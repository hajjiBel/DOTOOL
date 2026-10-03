# TP 8 — Kubernetes : exploiter plusieurs conteneurs (V10)

**Durée : 105 min** · théorie 12 · activité 15 · mini-TP 33 · fil rouge 35 · validation 10 · **VM** : `ci`, `k8s` · **Format** : individuel
*(Couvre les « TP Kubernetes 1 à 9 » : Pod, Deployment, Service, exposition, scaling, rolling update, configuration, secrets, déploiement de l'application. Le 10ᵉ — déploiement depuis Jenkins — se fait avec `Jenkinsfile.final`.)*

#### 1. Objectif pédagogique
Comprendre **pourquoi** les conteneurs seuls deviennent difficiles à gérer ; manipuler Pod, Deployment, Service ; mettre à l'échelle, mettre à jour sans coupure, revenir en arrière ; déployer l'application fil rouge.

#### 2. Prérequis
TP04-05 (images, registre). Notion de port et de réplication.

#### 3. Concept DevOps abordé
**Orchestration déclarative.** On déclare l'état voulu (« 3 réplicas de cette image, joignables sur ce port ») ; Kubernetes réconcilie en permanence l'état réel avec cet état : il relance, redistribue, remplace progressivement.
Vocabulaire minimal : **Pod** (un ou plusieurs conteneurs), **Deployment** (garantit N pods et gère les mises à jour), **Service** (adresse stable devant des pods changeants), **scaling** (changer N), **rolling update** (remplacer les pods un par un).

#### 4. Problème réel à résoudre
« L'application tourne dans 10 conteneurs sur 3 serveurs. Un conteneur plante la nuit : personne ne le relance. On déploie une nouvelle version : une minute d'indisponibilité. Le jour de la promotion, le trafic triple et il faut ajouter des instances à la main. »

#### 5. Activité pédagogique de découverte **[N1 Découverte]** (15 min) — « Le chaos des conteneurs »
Sur `ci` (pas encore Kubernetes) :
```bash
for i in 1 2 3; do docker run -d --name web$i -p 809$i:80 nginx:1.27-alpine; done
docker ps --format '{{.Names}} {{.Status}}'
docker kill web2
sleep 5 && docker ps --format '{{.Names}} {{.Status}}'      # web2 ne revient pas
curl -s -o /dev/null -w '%{http_code}\n' localhost:8092     # connexion refusée
```
**Questions** : qui a relancé `web2` ? Comment répartir les requêtes entre 3 ports ? Comment passer de `nginx:1.27` à `1.28` sans coupure ? Que se passe-t-il si le serveur entier tombe ?
Nettoyage : `docker rm -f web1 web2 web3`.

> 🎤 **Formateur** : écrivez les 4 besoins : *redémarrage automatique*, *répartition de charge*, *mise à jour progressive*, *montée en charge*. Annoncez : « Kubernetes est un robot qui garantit cela pour vous, à partir d'un fichier. »

#### 6. Mini-TP **[N2 Application]** (33 min) — « Nginx sur Kubernetes »
- **Contexte** : trois réplicas de Nginx derrière un Service, sans lien avec l'application Java.
- **Objectif** : voir l'auto-réparation, le scaling et la mise à jour progressive.
- **Architecture** : VM `k8s` (k3s mono-nœud) ; Service NodePort 30090.
- **Fichier** `mini-tp/k8s-nginx/web.yaml` :

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web
spec:
  replicas: 3
  selector:
    matchLabels: { app: web }
  template:
    metadata:
      labels: { app: web }
    spec:
      containers:
        - name: nginx
          image: nginx:1.25-alpine
          ports:
            - containerPort: 80
---
apiVersion: v1
kind: Service
metadata:
  name: web
spec:
  type: NodePort
  selector: { app: web }
  ports:
    - port: 80
      targetPort: 80
      nodePort: 30090
```

**Étapes et commandes** (sur `k8s` : `vagrant ssh k8s`)
```bash
kubectl get nodes
kubectl apply -f /vagrant/mini-tp/k8s-nginx/web.yaml
kubectl get pods -o wide -w                 # Ctrl+C quand 3 pods sont Running
kubectl get svc web
curl -s -o /dev/null -w '%{http_code}\n' http://192.168.56.14:30090

# 1. Auto-réparation
kubectl delete pod $(kubectl get pods -l app=web -o name | head -1)
kubectl get pods                            # un nouveau pod est déjà créé

# 2. Scaling
kubectl scale deployment web --replicas=5
kubectl get pods

# 3. Mise à jour progressive (terminal 2 : while true; do curl -s -o /dev/null -w '%{http_code}\n' http://192.168.56.14:30090; sleep 0.5; done)
kubectl set image deployment/web nginx=nginx:1.27-alpine
kubectl rollout status deployment/web
kubectl rollout history deployment/web

# 4. Retour arrière
kubectl rollout undo deployment/web

# 5. Diagnostic d'une erreur
kubectl set image deployment/web nginx=nginx:inexistant
kubectl get pods                            # ErrImagePull / ImagePullBackOff
kubectl describe pod $(kubectl get pods -l app=web -o name | grep -v Running | head -1) | tail -8
kubectl rollout undo deployment/web

kubectl delete -f /vagrant/mini-tp/k8s-nginx/web.yaml
```
**Résultat attendu** : pod supprimé → recréé ; 5 pods après `scale` ; pendant la mise à jour la boucle `curl` n'affiche que des `200` ; le tag inexistant bloque seulement les **nouveaux** pods : les anciens continuent de servir.

**Erreurs fréquentes** : `kubectl: command not found` (il faut être sur la VM `k8s`) ; confondre `nodePort` (30090, externe) et `port` (80, interne au cluster) ; attendre `Running` sans lire `kubectl describe` en cas d'erreur.

**Solution** : voir les commandes ; le diagnostic s'appuie sur `kubectl describe pod` (section *Events*).

#### 7. Retour pédagogique
- **Appris** : on décrit un état, Kubernetes le maintient ; un Service masque les pods qui naissent et meurent ; une mise à jour progressive protège la disponibilité.
- **Pourquoi en DevOps** : exploitation fiable à grande échelle, déploiements sans interruption.
- **Problème résolu** : relance manuelle, coupure lors des mises à jour, capacité fixe.
- **Limites** : complexité réelle (réseau, stockage, sécurité) ; surdimensionné pour une petite application ; courbe d'apprentissage ; on ne l'utilise ici que dans sa version la plus simple (pas de Helm).

#### 8. Retour au projet fil rouge **[N3 Intégration]** (35 min) — V10
1. Lisez `k8s/deployment.yaml` : 2 réplicas, `RollingUpdate` (`maxUnavailable: 0`), `requests/limits`, **`readinessProbe`** et **`livenessProbe`** (sondes de l'actuator). Pourquoi la sonde de disponibilité est-elle essentielle pendant une mise à jour ?
2. Déployez :
   ```bash
   kubectl apply -f /vagrant/k8s/deployment.yaml -f /vagrant/k8s/service.yaml
   kubectl get pods -o wide -w
   ```
   Ouvrez http://192.168.56.14:30080 et **rafraîchissez** : le nom d'hôte change (répartition entre pods).
3. **Configuration et secret** (`k8s/configmap.yaml`, `k8s/secret.yaml`) :
   ```bash
   kubectl apply -f /vagrant/k8s/configmap.yaml -f /vagrant/k8s/secret.yaml
   kubectl set env deployment/devops-demo --from=configmap/devops-demo-config
   kubectl set env deployment/devops-demo --from=secret/devops-demo-secret
   kubectl rollout status deployment/devops-demo
   kubectl exec deploy/devops-demo -- sh -c 'echo $JAVA_OPTS; [ -n "$DB_PASSWORD" ] && echo "secret présent"'
   ```
   Le `Secret` est **encodé, pas chiffré** : `kubectl get secret devops-demo-secret -o jsonpath='{.data.DB_PASSWORD}' | base64 -d`.
4. **Mise à jour depuis le pipeline** : dans Jenkins, lancez `devops-final` avec `DEPLOY_K8S` **coché** ; observez le stage *Déploiement Kubernetes* (`kubectl set image` + `rollout status`) et, en boucle :
   ```bash
   while true; do curl -s http://192.168.56.14:30080/api/info; echo; sleep 0.5; done
   ```
   Aucune requête ne doit échouer pendant la mise à jour.
5. **Incident** : `kubectl rollout undo deployment/devops-demo`, puis un tag inexistant (`kubectl set image deployment/devops-demo app=192.168.56.10:5000/devops-demo:999`) : constatez que l'ancienne version continue de servir.

**Résultat attendu** : 2 pods `Running` joignables sur 30080 ; `JAVA_OPTS` visible dans le pod ; version mise à jour par Jenkins sans erreur côté client.

**Pourquoi V10 ?** Avant : un conteneur sur un serveur, relancé à la main. Après : un état déclaré, maintenu et mis à jour progressivement. Preuve : `kubectl get pods`, et l'absence d'erreur pendant la mise à jour.

#### 9. Validation
1. Quelle est la différence entre un Pod et un Deployment ?
2. Pourquoi la boucle `curl` ne voit-elle aucune erreur pendant un *rolling update* ?
3. À quoi servent `readinessProbe` et `livenessProbe` ?
4. Tâche : passez le déploiement à 4 réplicas puis revenez à 2.
5. Le `Secret` est-il chiffré ? (non, simplement encodé en base64)

#### 10. Extension / challenge
Ajoutez un **HorizontalPodAutoscaler** : `kubectl autoscale deployment devops-demo --cpu-percent=50 --min=2 --max=5`, générez de la charge (`/api/simulate/slow` en boucle) et observez `kubectl get hpa -w`.
