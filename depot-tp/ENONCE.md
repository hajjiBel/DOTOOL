# Formation DevOps — De l'idée à la supervision en 2 jours

> **Durée** : 2 jours (2 × 7 h) · **Niveau** : débutant à avancé · **Format** : 8 travaux pratiques autour d'**un seul projet fil rouge**

---

## 1. Objectifs pédagogiques

À l'issue de la formation, vous saurez :

- automatiser la compilation et les tests d'une application Java avec **Jenkins**, **Maven** et **JUnit** ;
- configurer des serveurs Linux de façon reproductible avec **Ansible** ;
- conteneuriser une application avec **Docker** et la publier dans un registre ;
- construire un **pipeline CI/CD** complet déclenché à chaque `push` ;
- centraliser et analyser les logs avec la pile **ELK** ;
- superviser serveurs et application avec **Prometheus** et **Grafana**, et définir des alertes ;
- déployer sur **Kubernetes** (Deployment, Service, scaling, mise à jour progressive) ;
- bloquer une livraison défectueuse grâce aux tests fonctionnels **Selenium**.

## 2. Le projet fil rouge

Une application Java (**Spring Boot**, gestionnaire de tâches) que vous allez faire évoluer de bout en bout :

```
 Développeur ──push──▶ GitHub ──▶ Jenkins ──▶ Registre Docker ──▶ Déploiement ──▶ Serveur de test / K8s
                                  │ Maven build                    (Ansible)            │
                                  │ JUnit                                               ├──▶ Prometheus ─▶ Grafana (métriques, alertes)
                                  │ Selenium (barrière qualité)                         └──▶ Filebeat ─▶ Logstash ─▶ Elasticsearch ─▶ Kibana
```

| Point d'entrée | Rôle |
|---|---|
| `GET /` | Interface web (cible des tests Selenium) |
| `GET/POST /api/tasks`, `PUT /api/tasks/{id}/toggle`, `DELETE /api/tasks/{id}` | API REST |
| `GET /api/info` | Version et nom d'hôte (utile pour voir le load-balancing) |
| `GET /api/simulate/error` | Provoque une exception Java → HTTP 500 (TP ELK, alertes) |
| `GET /api/simulate/slow?ms=1500` | Provoque de la latence (TP Grafana) |
| `GET /actuator/health`, `/actuator/prometheus` | Santé et métriques |

## 3. Planning

| | Horaire | Contenu |
|---|---|---|
| **Jour 1** | 09:00 – 09:30 | Introduction DevOps, présentation du projet, démarrage des VM |
| | 09:30 – 11:00 | **TP1** — Intégration continue avec Jenkins |
| | 11:15 – 12:30 | **TP2** — Configuration de serveurs avec Ansible |
| | 13:30 – 14:45 | **TP3** — Conteneurisation avec Docker |
| | 15:00 – 17:00 | **TP4** — Pipeline CI/CD complet |
| **Jour 2** | 09:00 – 10:30 | **TP5** — Centralisation des logs avec ELK |
| | 10:45 – 12:15 | **TP6** — Supervision avec Prometheus et Grafana |
| | 13:15 – 14:30 | **TP7** — Déploiement sur Kubernetes |
| | 14:45 – 16:15 | **TP8** — Tests Selenium et barrière qualité |
| | 16:15 – 17:00 | Synthèse, bonnes pratiques, évaluation |

---

## 4. Mise en place de l'environnement

### 4.1 Prérequis sur votre poste

- **VirtualBox** 7.x et **Vagrant** 2.4+
- **16 Go de RAM** conseillés (8 Go suffisent le jour 1 : seules `ci` et `target` démarrent)
- ~25 Go d'espace disque, accès Internet
- Un compte **GitHub** (et un *Personal Access Token* si votre dépôt est privé)
- Un éditeur de code (VS Code, IntelliJ…)

### 4.2 Les machines virtuelles

| VM | IP | Rôle | Utilisée dans |
|---|---|---|---|
| `ci` | 192.168.56.10 | Jenkins, JDK 21, Maven, Git, Docker, registre privé, Ansible, Chrome | TP1–4, TP8 |
| `target` | 192.168.56.11 | Serveur cible (Docker, SSH ; Nginx et Java seront installés par **vous**, via Ansible) | TP2–5, TP8 |
| `monitor` | 192.168.56.12 | Prometheus, Alertmanager, Grafana | TP6 |
| `elk` | 192.168.56.13 | Elasticsearch, Logstash, Kibana | TP5 |
| `k8s` | 192.168.56.14 | Kubernetes mono-nœud (k3s) | TP7 |

### 4.3 Démarrage

```bash
# Jour 1 : ci + target (premier démarrage : 15 à 25 min, le provisionnement installe tous les outils)
vagrant up

# Jour 2 (à lancer en début de TP pour gagner du temps)
vagrant up elk          # avant le TP5
vagrant up monitor      # avant le TP6
vagrant up k8s          # avant le TP7

vagrant status          # état des VM
vagrant ssh ci          # se connecter
vagrant provision ci    # rejouer le provisionnement en cas d'échec
```

> 💡 Machine limitée en RAM ? Réduisez les VM : `CI_RAM=3072 ELK_RAM=3584 vagrant up elk`.
> Tout démarrer d'un coup : `LAB_ALL=1 vagrant up`.

### 4.4 Adresses et comptes

| Service | URL | Identifiants |
|---|---|---|
| Jenkins | http://192.168.56.10:8080 | `admin` / `admin` |
| Registre Docker | http://192.168.56.10:5000/v2/_catalog | — |
| Application (conteneur, TP3-4) | http://192.168.56.11:8080 | — |
| Application via Nginx (TP2) | http://192.168.56.11 | — |
| Kibana | http://192.168.56.13:5601 | — |
| Elasticsearch | http://192.168.56.13:9200 | — |
| Prometheus | http://192.168.56.12:9090 | — |
| Alertmanager | http://192.168.56.12:9093 | — |
| Grafana | http://192.168.56.12:3000 | `admin` / `admin` |
| Application sur Kubernetes | http://192.168.56.14:30080 | — |
| SSH sur `target` | `vagrant@192.168.56.11` | mot de passe `vagrant` |

### 4.5 Votre espace de travail

Sur la VM `ci`, le dépôt de TP est copié dans **`~/devops-formation`**. Vous y travaillez en ligne de commande ; le dossier du poste hôte est aussi visible dans `/vagrant` (pratique pour éditer avec votre IDE, mais **ne lancez pas Ansible depuis `/vagrant`**).

```
devops-formation/
├── app/              Application Java (pom.xml, Dockerfile, docker-compose.yml, tests JUnit)
├── ansible/          Inventaire, playbooks (site.yml, deploy-app.yml), templates
├── jenkins/          Jenkinsfile.tp1, Jenkinsfile.tp4, Jenkinsfile.tp8
├── monitoring/       Prometheus, Alertmanager, Grafana
├── elk/              Elasticsearch/Logstash/Kibana + Filebeat
├── k8s/              Manifestes Kubernetes
└── selenium-tests/   Tests fonctionnels Selenium
```

---

## TP1 — Intégration continue avec Jenkins

**Durée** 1 h 30 · **Niveau** débutant → intermédiaire · **VM** `ci`

**Objectif** : à chaque modification du code, compiler, tester et produire un rapport automatiquement.

### Étapes

1. **Publier le projet sur GitHub**
   - Créez un dépôt GitHub vide `devops-formation` (idéalement **public** pour simplifier Jenkins).
   - Sur `ci` :
     ```bash
     cd ~/devops-formation
     git init -b main          # ignorez si le dossier est déjà un dépôt Git
     git add . && git commit -m "Projet initial"
     git remote add origin https://github.com/<votre-compte>/devops-formation.git
     git push -u origin main   # mot de passe = Personal Access Token
     ```

2. **Builder à la main** (comprendre avant d'automatiser)
   ```bash
   cd ~/devops-formation/app
   mvn clean test            # compile + exécute les tests JUnit
   mvn -DskipTests package   # produit target/devops-demo.jar
   java -jar target/devops-demo.jar --server.port=9090   # 8080 est occupé par Jenkins
   ```
   Ouvrez http://192.168.56.10:9090 puis arrêtez l'application (`Ctrl+C`).

3. **Créer le job Jenkins**
   - Connectez-vous à Jenkins → **Nouveau Item** → nom `tp1-ci` → **Pipeline**.
   - *Pipeline script from SCM* → Git → URL de votre dépôt → branche `*/main`.
   - **Script Path** : `jenkins/Jenkinsfile.tp1`.
   - Enregistrez, puis **Lancer un build**.

4. **Lire le Jenkinsfile** : identifiez chaque *stage* (Checkout, Compilation, Tests, Package) et observez la *Stage View* ainsi que les **Résultats des tests**.

5. **Faire échouer un test volontairement**
   - Dans `app/src/test/java/.../TaskServiceTest.java`, changez une assertion (ex. `assertEquals(1, ...)` → `assertEquals(2, ...)`).
   - `git commit -am "Test cassé" && git push`.
   - Attendez le déclenchement automatique (*Poll SCM* toutes les ~2 min) ou cliquez sur **Lancer**. Le build passe au rouge : retrouvez le test fautif dans le rapport.
   - Corrigez, poussez, constatez le retour au vert.

### ✅ Validation

- [ ] Le build `tp1-ci` est vert et affiche un graphique de tests.
- [ ] Le JAR est archivé dans les artefacts du build.
- [ ] Un test cassé fait échouer le build, sa correction le rétablit.

### 🚀 Pour aller plus loin

- Remplacer le *polling* par un **webhook GitHub** (nécessite une URL publique : `ngrok` ou `smee.io` + déclencheur `githubPush()`).
- Ajouter un stage qui publie un rapport de couverture (JaCoCo).

---

## TP2 — Configuration automatique de serveurs avec Ansible

**Durée** 1 h 15 · **Niveau** débutant → intermédiaire · **VM** `ci` (contrôleur) → `target` (géré)

**Objectif** : préparer un serveur Linux « from scratch » (Java, Nginx, utilisateurs, service) sans taper une seule commande sur ce serveur.

### Étapes

1. **Tester la connexion**
   ```bash
   cd ~/devops-formation/ansible
   ansible all -m ping
   ```
   > Si le ping échoue (clé non autorisée) : `ssh-copy-id vagrant@192.168.56.11` (mot de passe `vagrant`).

2. **Commandes ad hoc**
   ```bash
   ansible webservers -m setup -a "filter=ansible_distribution*"
   ansible webservers -b -m apt -a "name=htop state=present"
   ansible webservers -m command -a "df -h /"
   ```

3. **Construire le JAR à déployer**
   ```bash
   cd ~/devops-formation/app && mvn -B -DskipTests package && cd ../ansible
   ```

4. **Étudier puis exécuter le playbook** `site.yml` : repérez les modules (`apt`, `user`, `copy`, `template`, `service`, `uri`), les variables, les *handlers* et les tags.
   ```bash
   ansible-playbook site.yml --check --diff   # simulation
   ansible-playbook site.yml                  # exécution réelle
   ansible-playbook site.yml                  # 2ᵉ passage : changed=0 → idempotence !
   ```

5. **Vérifier**
   ```bash
   curl http://192.168.56.11/api/info          # via Nginx (port 80 → 8081)
   ssh vagrant@192.168.56.11 "systemctl status nginx devops-demo --no-pager"
   ansible-playbook site.yml --tags verify     # contrôles seuls
   ```

6. **Modifications à réaliser** (une à la fois, puis rejouer le playbook)
   - Ajouter un utilisateur opérateur `charlie` dans la variable `operators`.
   - Changer `app_port` en `8082` : observez que seuls les fichiers concernés sont modifiés et les services redémarrés.
   - **Réparation automatique** : `ssh vagrant@192.168.56.11 "sudo systemctl stop nginx"` puis rejouez le playbook.

### ✅ Validation

- [ ] `ansible all -m ping` renvoie `pong`.
- [ ] http://192.168.56.11 affiche la réponse de l'application via Nginx.
- [ ] Le second passage du playbook indique `changed=0`.
- [ ] Vous avez ajouté un utilisateur et changé le port via les variables.

### 🚀 Pour aller plus loin

- Transformer `site.yml` en **rôles** (`common`, `nginx`, `java_app`).
- Chiffrer une variable avec **Ansible Vault**.

---

## TP3 — Conteneurisation avec Docker

**Durée** 1 h 15 · **Niveau** intermédiaire · **VM** `ci`, puis `target`

**Objectif** : packager l'application dans une image reproductible, la publier et la déployer avec Docker Compose.

### Étapes

1. **Construire l'image**
   ```bash
   cd ~/devops-formation/app
   mvn -B -DskipTests package
   cat Dockerfile                                   # lisez chaque instruction
   docker build -t devops-demo:1.0 --build-arg APP_VERSION=1.0 .
   docker images devops-demo
   docker history devops-demo:1.0                   # couches de l'image
   ```

2. **Exécuter et inspecter**
   ```bash
   docker run -d --name demo -p 9090:8080 devops-demo:1.0
   curl http://localhost:9090/api/info
   docker logs -f demo                              # Ctrl+C pour quitter
   docker exec -it demo sh -c "whoami && ls /app"   # utilisateur non-root
   docker ps                                         # STATUS passe à (healthy)
   docker rm -f demo
   ```

3. **Publier dans le registre privé**
   ```bash
   docker tag devops-demo:1.0 192.168.56.10:5000/devops-demo:1.0
   docker push 192.168.56.10:5000/devops-demo:1.0
   curl http://192.168.56.10:5000/v2/_catalog
   curl http://192.168.56.10:5000/v2/devops-demo/tags/list
   ```

4. **Déployer avec Docker Compose sur `target`**
   ```bash
   ssh vagrant@192.168.56.11
   cd /vagrant/app
   TAG=1.0 docker compose up -d      # l'image est téléchargée depuis le registre de ci
   docker compose ps
   exit
   ```
   Ouvrez http://192.168.56.11:8080 .

5. **Nettoyer avant le TP4**
   ```bash
   ssh vagrant@192.168.56.11 "cd /vagrant/app && docker compose down"
   ```

### ✅ Validation

- [ ] L'image se construit et le conteneur passe à `healthy`.
- [ ] `devops-demo:1.0` apparaît dans le catalogue du registre.
- [ ] L'application répond sur http://192.168.56.11:8080 après `docker compose up`.

### ❓ Questions

- Quelle est la taille de l'image ? Quelles instructions créent le plus de couches ?
- Pourquoi exécuter le processus avec un utilisateur non-root ?
- 🚀 Réécrivez le `Dockerfile` en **multi-stage** (build Maven dans l'image) et comparez.

---

## TP4 — Pipeline CI/CD complet

**Durée** 2 h · **Niveau** intermédiaire → avancé · **VM** `ci` + `target`

**Objectif** : du `git push` jusqu'à l'application déployée, sans intervention manuelle.

```
push ─▶ checkout ─▶ mvn verify (JUnit) ─▶ docker build ─▶ docker push ─▶ ansible deploy ─▶ smoke test
```

### Étapes

1. Lisez `jenkins/Jenkinsfile.tp4` : variables d'environnement, déclencheurs, enchaînement des stages.
2. Dans Jenkins, créez un job Pipeline **`devops-cicd`** (Pipeline from SCM, **Script Path** `jenkins/Jenkinsfile.tp4`).
3. Lancez-le. À la fin :
   - l'image `devops-demo:<n° de build>` est dans le registre (`curl http://192.168.56.10:5000/v2/devops-demo/tags/list`) ;
   - l'application tourne sur http://192.168.56.11:8080 et affiche **Version \<n° de build\>**.
4. **Cycle complet de livraison**
   - Modifiez le titre dans `app/src/main/resources/templates/index.html`.
   - `git commit -am "Nouveau titre" && git push`.
   - Observez Jenkins se déclencher seul, puis rechargez la page : la nouvelle version est en ligne.
5. **Scénario d'échec** : cassez un test, poussez. Vérifiez que le pipeline s'arrête **avant** la construction de l'image et que la version en ligne ne change pas.
6. **Analyse** : ouvrez la sortie console d'un build, identifiez la durée de chaque stage et le stage le plus lent.

### ✅ Validation

- [ ] Un `push` déclenche automatiquement le pipeline (webhook ou polling).
- [ ] Chaque build produit une image taguée avec son numéro.
- [ ] La page en ligne affiche le numéro du dernier build réussi.
- [ ] Un test en échec empêche tout déploiement.

### 🚀 Pour aller plus loin

- Ajouter un stage qui effectue un **rollback** (redéploiement du tag précédent) en cas d'échec du smoke test.
- Paramétrer le pipeline (`parameters { string(name: 'TAG') }`) pour redéployer une version donnée.

---

## TP5 — Centralisation des logs avec ELK

**Durée** 1 h 30 · **Niveau** intermédiaire → avancé · **VM** `elk` + `target`

**Objectif** : collecter les logs de l'application et de Nginx, les indexer, et repérer erreurs HTTP et exceptions Java.

```
conteneur devops-demo ─┐
                       ├─▶ Filebeat (target) ─▶ Logstash :5044 (grok) ─▶ Elasticsearch ─▶ Kibana
Nginx access.log ──────┘
```

### Étapes

1. **Démarrer la pile** (hôte) : `vagrant up elk`, puis vérifier `curl http://192.168.56.13:9200` et ouvrir Kibana (patientez 2–3 min).
2. **Lire la configuration** : `elk/logstash/pipeline/logstash.conf` (entrée Beats, filtre `grok`, sortie `devops-logs-*`) et `elk/filebeat/filebeat.yml` (regroupement des *stack traces* Java en un seul événement).
3. **Démarrer Filebeat sur `target`**
   ```bash
   ssh vagrant@192.168.56.11
   cd /vagrant/elk/filebeat && docker compose up -d
   docker logs filebeat --tail 20
   ```
   > L'application doit tourner (TP4) ; les logs Nginx n'existent que si le TP2 a été réalisé.
4. **Générer du trafic** (depuis `ci`)
   ```bash
   for i in $(seq 1 40); do
     curl -s http://192.168.56.11:8080/api/info > /dev/null
     curl -s http://192.168.56.11:8080/api/simulate/error > /dev/null
     curl -s http://192.168.56.11/introuvable > /dev/null
     curl -s -X POST -H 'Content-Type: application/json' -d '{"title":""}' http://192.168.56.11:8080/api/tasks > /dev/null
   done
   ```
5. **Explorer dans Kibana**
   - *Stack Management → Data Views* : créez `devops-logs-*` (champ temps `@timestamp`).
   - *Discover* : filtrez `level : "ERROR"`, puis `tags : "java_exception"`, puis `response >= 400`.
6. **Construire un tableau de bord** (*Dashboard → Create*) avec au minimum :
   1. volume de logs dans le temps, ventilé par `level` ;
   2. répartition des codes HTTP (`response`) — anneau ;
   3. tableau des messages d'erreur les plus fréquents (`log_message.keyword`).
   Enregistrez-le sous le nom **« Santé de l'application »**.

### ✅ Validation

- [ ] L'index `devops-logs-*` contient des documents.
- [ ] Une exception Java apparaît dans **un seul** document avec sa stack trace complète.
- [ ] Vous distinguez les erreurs 4xx/5xx de Nginx.
- [ ] Le tableau de bord contient les 3 visualisations.

### 🚀 Pour aller plus loin

- Créer une **règle d'alerte** Kibana (> 10 erreurs en 5 min).
- Ajouter dans Logstash un champ `severity_num` calculé à partir de `level`.

---

## TP6 — Supervision avec Prometheus et Grafana

**Durée** 1 h 30 · **Niveau** intermédiaire → avancé · **VM** `monitor` (+ `ci`, `target`)

**Objectif** : surveiller CPU, mémoire, disque et performances de l'application, puis alerter sur des seuils.

### Étapes

1. `vagrant up monitor`. Dans Prometheus, ouvrez **Status → Targets** : `node` (3 VM) et `app` doivent être **UP**.
2. **Requêtes PromQL** (onglet *Graph*) — comprenez-les puis testez-les :
   ```promql
   100 - (avg by (host) (rate(node_cpu_seconds_total{mode="idle"}[2m])) * 100)
   (1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100
   100 - (node_filesystem_avail_bytes{mountpoint="/",fstype!="rootfs"} / node_filesystem_size_bytes{mountpoint="/",fstype!="rootfs"} * 100)
   sum(rate(http_server_requests_seconds_count{job="app"}[1m])) by (uri)
   jvm_memory_used_bytes{job="app",area="heap"}
   ```
3. **Grafana** (admin/admin) :
   - la source de données *Prometheus* est déjà configurée ;
   - **Dashboards → New → Import** : ID `1860` (Node Exporter Full) puis `4701` (JVM Micrometer), source *Prometheus* ;
   - créez votre propre dashboard avec 4 panneaux : CPU par VM, mémoire, requêtes/s de l'application, latence p95
     (`histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="app"}[1m])))`).
4. **Alertes** : lisez `monitoring/prometheus/alert.rules.yml`, puis déclenchez-les :
   ```bash
   # Charge CPU sur target (> 2 min pour passer en "firing")
   ssh vagrant@192.168.56.11 "sudo apt-get install -y stress-ng && stress-ng --cpu 2 --timeout 300s"

   # Taux d'erreurs 5xx
   while true; do curl -s http://192.168.56.11:8080/api/simulate/error > /dev/null; sleep 0.5; done

   # Panne de l'application
   ssh vagrant@192.168.56.11 "docker stop devops-demo"
   ```
   Suivez l'évolution *Inactive → Pending → Firing* dans **Alerts** (Prometheus) et dans Alertmanager (:9093). Redémarrez ensuite le conteneur (`docker start devops-demo`).
5. **Votre règle** : ajoutez une alerte `SlowRequests` (p95 > 1 s pendant 1 min), rechargez Prometheus (`curl -X POST http://192.168.56.12:9090/-/reload`) et testez-la avec `/api/simulate/slow?ms=2000`.

### ✅ Validation

- [ ] Toutes les cibles sont **UP**.
- [ ] Le dashboard personnalisé affiche 4 panneaux avec des données.
- [ ] `HighCpuUsage` et `AppDown` sont passées à *Firing* puis ont disparu après correction.
- [ ] Votre règle `SlowRequests` est chargée.

### 🚀 Pour aller plus loin

- Configurer un récepteur **webhook** dans `alertmanager.yml`.
- Exporter votre dashboard en JSON et le provisionner automatiquement.

---

## TP7 — Déploiement sur Kubernetes

**Durée** 1 h 15 · **Niveau** intermédiaire · **VM** `k8s`

**Objectif** : exécuter l'application sur un cluster, la mettre à l'échelle et la mettre à jour sans interruption.

### Étapes

1. `vagrant up k8s` puis `vagrant ssh k8s` ; `kubectl get nodes` (alias `k`).
2. **Déployer**
   ```bash
   cat /vagrant/k8s/deployment.yaml /vagrant/k8s/service.yaml     # lisez-les
   kubectl apply -f /vagrant/k8s/
   kubectl get pods -o wide -w
   kubectl get svc devops-demo
   ```
   Ouvrez http://192.168.56.14:30080 — **rafraîchissez plusieurs fois** : le nom d'hôte affiché alterne entre les pods.
3. **Mise à l'échelle**
   ```bash
   kubectl scale deployment devops-demo --replicas=4
   kubectl get pods
   kubectl top pods
   ```
4. **Mise à jour progressive** (utilisez un tag construit au TP4, par ex. `12`)
   ```bash
   kubectl set image deployment/devops-demo app=192.168.56.10:5000/devops-demo:12
   kubectl rollout status deployment/devops-demo
   kubectl rollout history deployment/devops-demo
   ```
   Pendant la mise à jour, lancez dans un autre terminal : `while true; do curl -s http://192.168.56.14:30080/api/info; echo; sleep 0.5; done` — aucune requête ne doit échouer.
5. **Retour arrière** : `kubectl rollout undo deployment/devops-demo`.
6. **Auto-réparation** : `kubectl delete pod <un-pod>` → un nouveau pod est recréé immédiatement. Expliquez le rôle des sondes `readinessProbe` / `livenessProbe`.
7. **Diagnostic** : provoquez une erreur (`image: …:inexistant`), observez `ImagePullBackOff` avec `kubectl describe pod`, puis corrigez.

### ✅ Validation

- [ ] 2 pods `Running` joignables via le NodePort 30080.
- [ ] Passage à 4 réplicas puis retour à 2.
- [ ] Mise à jour sans erreur côté client, puis `rollout undo` réussi.

### 🚀 Pour aller plus loin

- Ajouter un **HorizontalPodAutoscaler** (`kubectl autoscale deployment devops-demo --cpu-percent=50 --min=2 --max=5`).
- Ajouter le job `app-k8s` dans Prometheus (déjà préparé en commentaire dans `prometheus.yml`).

---

## TP8 — Tests automatisés avec Selenium

**Durée** 1 h 30 · **Niveau** intermédiaire → avancé · **VM** `ci`

**Objectif** : exécuter des tests fonctionnels de l'interface web dans le pipeline et **bloquer la livraison** en cas d'échec.

### Étapes

1. **Lire** `selenium-tests/src/test/java/.../AppUiTest.java` : Chrome en mode *headless*, localisateurs (`By.id`, XPath), attentes explicites (`WebDriverWait`).
2. **Exécuter à la main** (contre l'environnement de test)
   ```bash
   cd ~/devops-formation/selenium-tests
   mvn -B test -Dapp.url=http://192.168.56.11:8080
   ```
3. **Écrire un nouveau test** : « ajouter une tâche vide ne crée aucune ligne ».
   Indice : compter les `<li>` de `#task-list` avant/après la soumission.
4. **Intégrer au pipeline** : créez le job `devops-cicd-gated` avec `jenkins/Jenkinsfile.tp8`. Comparez-le au Jenkinsfile du TP4 : où se situent Selenium et la **promotion** (tags `latest` / `stable`) ?
5. **Démontrer la barrière qualité**
   - Dans `index.html`, renommez le titre `id="page-title"` en `id="titre"` (régression d'interface) puis poussez.
   - Le pipeline échoue au stage *Tests fonctionnels Selenium* ; le stage *Promotion* n'est **pas** exécuté et le tag `stable` reste inchangé :
     `curl http://192.168.56.10:5000/v2/devops-demo/tags/list`
   - Corrigez : le pipeline repasse au vert et `stable` avance.
6. **Option Kubernetes** : lancez le job avec le paramètre `DEPLOY_K8S=true` (VM `k8s` démarrée, TP7 réalisé).

### ✅ Validation

- [ ] Les 5 tests Selenium passent contre l'environnement de test.
- [ ] Votre test supplémentaire est intégré et exécuté par Jenkins.
- [ ] Une régression d'interface bloque la promotion de l'image.

### 🚀 Pour aller plus loin

- Capturer une copie d'écran en cas d'échec (`TakesScreenshot`) et l'archiver dans Jenkins.
- Exécuter les tests en parallèle ou sur plusieurs navigateurs (Selenium Grid).

---

## 5. Synthèse et évaluation

Chaîne obtenue en fin de formation :

| Étape | Outil | Preuve |
|---|---|---|
| Code & versions | Git / GitHub | Dépôt avec historique |
| Build & tests | Maven, JUnit, Jenkins | Rapports de tests |
| Qualité fonctionnelle | Selenium | Barrière avant promotion |
| Packaging | Docker, registre | Tags `n`, `latest`, `stable` |
| Provisionnement & déploiement | Ansible, Compose, Kubernetes | Serveur reproductible, rollout |
| Logs | Filebeat → Logstash → Elasticsearch → Kibana | Tableau de bord |
| Métriques & alertes | Prometheus, Alertmanager, Grafana | Dashboards, règles |

**Livrables à rendre** : l'URL de votre dépôt GitHub, une capture du pipeline vert (Stage View), une capture du dashboard Kibana, une capture du dashboard Grafana avec une alerte *Firing*, et la sortie de `kubectl get pods,svc`.

| Critère | Points |
|---|---|
| CI Jenkins fonctionnelle (TP1) | 3 |
| Playbook Ansible idempotent (TP2) | 3 |
| Image Docker + registre + Compose (TP3) | 3 |
| Pipeline CI/CD automatique (TP4) | 4 |
| Tableau de bord ELK (TP5) | 2 |
| Supervision + 1 alerte personnalisée (TP6) | 2 |
| Déploiement Kubernetes + rollback (TP7) | 2 |
| Selenium + barrière qualité (TP8) | 1 |
| **Total** | **20** |

---

## Annexe — Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| `vagrant up` : erreur de dossier partagé (`vboxsf`) | Guest Additions absentes | `vagrant plugin install vagrant-vbguest` puis `vagrant reload` |
| `vagrant up` : plage réseau refusée | VirtualBox n'autorise pas 192.168.56.x | Créez `/etc/vbox/networks.conf` avec `* 192.168.56.0/21` (Linux/macOS) |
| Provisionnement interrompu (réseau) | Téléchargement échoué | `vagrant provision <vm>` (le script est rejouable) |
| Jenkins inaccessible juste après `vagrant up` | Démarrage lent / plugins | Attendre 2–3 min ; `sudo journalctl -u jenkins -n 50` |
| `permission denied` sur `/var/run/docker.sock` | Groupe `docker` non pris en compte | Se reconnecter : `exit` puis `vagrant ssh ci` |
| `ansible all -m ping` : `Permission denied` | Clé non copiée | `ssh-copy-id vagrant@192.168.56.11` |
| `ansible.cfg ignored` (world writable) | Playbook lancé depuis `/vagrant` | Travailler dans `~/devops-formation/ansible` |
| `docker push` : `http: server gave HTTP response to HTTPS client` | Registre non déclaré comme *insecure* | Déjà configuré par Vagrant ; sinon éditer `/etc/docker/daemon.json` puis `sudo systemctl restart docker` |
| Elasticsearch redémarre en boucle | Mémoire insuffisante / `vm.max_map_count` | Augmenter `ELK_RAM`, `vagrant provision elk` |
| Aucun log dans Kibana | Filebeat arrêté ou Logstash pas prêt | `docker logs filebeat` sur `target`, `docker logs logstash` sur `elk` |
| Cible Prometheus `DOWN` | VM éteinte ou appli non déployée | Démarrer la VM / relancer le job Jenkins |
| Pod en `ImagePullBackOff` | Tag inexistant dans le registre | `curl http://192.168.56.10:5000/v2/devops-demo/tags/list` |
| Selenium : `session not created` | Chrome/driver indisponible | Vérifier `google-chrome --version`, accès Internet (téléchargement du driver) |
