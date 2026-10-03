# Environnement technique

Le laboratoire est celui de l'archive (`Vagrantfile`, 5 VM Ubuntu 22.04, provisionnement complet). Il est réalisable avec VirtualBox sur les postes des participants.

## Machine hôte recommandée

| Ressource | Minimum | Confortable |
|---|---|---|
| RAM | 12 Go | 16 Go |
| CPU | 4 cœurs avec virtualisation (VT-x/AMD-V) activée | 6-8 cœurs |
| Disque | 30 Go libres (SSD) | 40 Go |
| Logiciels | VirtualBox 7.x, Vagrant 2.4+, Git, éditeur de code | idem |
| Réseau | Accès Internet, réseau hôte `192.168.56.0/24` autorisé | idem |

## Les VM

| VM | IP | OS | vCPU | RAM par défaut | Disque | Rôle | Variable |
|---|---|---|---|---|---|---|---|
| `ci` | 192.168.56.10 | Ubuntu 22.04 | 2 | 4096 Mo | ≈ 10 Go | Jenkins, JDK 21, Maven, Git, Docker, registre :5000, Ansible, Chrome | `CI_RAM` |
| `target` | 192.168.56.11 | Ubuntu 22.04 | 1-2 | 2048 Mo | ≈ 5 Go | Serveur cible (Docker, SSH) | `TARGET_RAM` |
| `monitor` | 192.168.56.12 | Ubuntu 22.04 | 1-2 | 2048 Mo | ≈ 5 Go | Prometheus, Alertmanager, Grafana | `MONITOR_RAM` |
| `elk` | 192.168.56.13 | Ubuntu 22.04 | 2 | 4608 Mo | ≈ 8 Go | Elasticsearch, Logstash, Kibana | `ELK_RAM` |
| `k8s` | 192.168.56.14 | Ubuntu 22.04 | 2 | 3072 Mo | ≈ 8 Go | Kubernetes mono-nœud (k3s) | `K8S_RAM` |

Réseau : adaptateur NAT (Internet) + réseau privé hôte `192.168.56.0/24`.

Versions : JDK 21, Spring Boot 3.3.5, Jenkins LTS, ansible-core 2.16/2.17, Elastic 8.13.4, Prometheus 2.53.2, Alertmanager 0.27.0, Grafana 11.2.0, k3s dernière version stable, Selenium 4.25.0, Postgres 16 (mini-TP), Nginx 1.25/1.27 (mini-TP).

## Quelles VM allumer, et quand

L'organisation en 3 jours permet de **ne jamais dépasser 3 VM en même temps** (≈ 10 Go au pic) :

| Séance | VM allumées | RAM cumulée |
|---|---|---|
| Jour 1 (TP01 → TP04) | `ci` | 4 Go |
| Jour 2 (TP05 → scénario) | `ci`, `target` | 6 Go |
| TP08 Kubernetes | `ci`, `k8s` (arrêter `target` si besoin) | 7 Go |
| TP09 ELK | `ci`, `target`, `elk` (arrêter `k8s`) | 10,5 Go |
| TP10 Monitoring | `ci`, `target`, `monitor` (arrêter `elk`) | 8 Go |
| TP11 et projet final | `ci`, `target` (+ `k8s`, `monitor` si le projet final les utilise) | 6 à 11 Go |

```bash
vagrant up ci                 # jour 1 (15 à 25 min la première fois)
vagrant up target             # jour 2 : démarrer ci AVANT target (clé SSH partagée)
vagrant halt target && vagrant up k8s       # TP08
vagrant halt k8s && vagrant up target elk   # TP09
vagrant halt elk && vagrant up monitor      # TP10
```

## Avant la formation (indispensable)

1. Faire un `vagrant up` complet sur **chaque** poste (ou distribuer les boxes exportées) : le premier provisionnement télécharge plusieurs Go.
2. Vérifier l'accès à GitHub et à Docker Hub, ou prévoir un miroir.
3. Faire créer un compte GitHub et un *Personal Access Token* (droits `repo`) à chaque participant **avant** le jour 1.
4. Linux/macOS : si VirtualBox refuse le réseau, créer `/etc/vbox/networks.conf` avec `* 192.168.56.0/21`.
5. Dérouler **une fois** tous les TP vous-même : ce dossier n'a pas pu être exécuté pendant sa conception.

## Plan B si les ressources sont limitées

| Situation | Solution |
|---|---|
| Poste de 8 Go | Une seule VM à la fois, réduite : `CI_RAM=3072 vagrant up ci`. Le jour 3, le formateur montre ELK, Prometheus et Kubernetes en démonstration à partir de **sa** machine ; les participants font les mini-TP (qui ne nécessitent pas le fil rouge complet) sur `ci`. |
| Postes très faibles ou non virtualisables | Un serveur central (32 Go, ou VM cloud) héberge les 5 VM ; les participants utilisent leur navigateur et un SSH vers `ci`. Prévoir des plages de ports ou une machine par binôme. |
| Pas d'Internet fiable | Précharger les images (`docker save` / `docker load`) et les plugins Jenkins, utiliser un dépôt Git local. |
| Kubernetes impossible | Remplacer le TP08 par un terrain de jeu en ligne (Killercoda, Play with Kubernetes) pour le mini-TP ; le fil rouge reste en démonstration. |
| ELK trop gourmand | `ELK_RAM=3584` (5 Go déconseillé en dessous), ou démonstration formateur et mini-TP `_bulk` sur la machine du formateur. |

## Adresses et comptes

| Service | URL | Identifiants |
|---|---|---|
| Jenkins | http://192.168.56.10:8080 | `admin` / `admin` |
| Registre Docker | http://192.168.56.10:5000/v2/_catalog | – |
| Appli en conteneur (target) | http://192.168.56.11:8080 | – |
| Kibana / Elasticsearch | http://192.168.56.13:5601 / :9200 | – |
| Prometheus / Alertmanager / Grafana | http://192.168.56.12:9090 / :9093 / :3000 | Grafana `admin` / `admin` |
| Appli sur Kubernetes | http://192.168.56.14:30080 | – |
| SSH `target` | `vagrant@192.168.56.11` | mot de passe `vagrant` |

> Sur la VM `ci`, le port 8080 est occupé par Jenkins : les conteneurs de test y utilisent 9090, 8081, 8088 ou 8089.

## Aide-mémoire Linux et outils

```bash
# Fichiers et texte
ls -la · cd · pwd · cat f · less f · head -n 5 f · tail -f f · grep -c ERROR f · grep -rn "mot" . · wc -l f
sed -i 's/ancien/nouveau/' f        # remplacer dans un fichier
diff -u a b                         # comparer deux fichiers
# Réseau et HTTP
curl -s URL · curl -i URL · curl -s -o /dev/null -w '%{http_code}\n' URL
ss -tlnp                            # ports en écoute
# Processus et ressources
ps aux | grep java · top · free -m · df -h · stress-ng --cpu 2 --timeout 120s
# Services
systemctl status nginx · sudo systemctl restart nginx · journalctl -u jenkins -n 50
# SSH
ssh vagrant@192.168.56.11 "commande" · ssh-copy-id vagrant@192.168.56.11
# Git
git status · git add . · git commit -m "msg" · git switch -c feature/x · git merge x · git log --oneline --graph
# Maven
mvn clean test · mvn -B clean verify · mvn -DskipTests package
# Docker
docker build -t nom:tag . · docker run -d -p 9090:8080 nom:tag · docker ps · docker logs -f c · docker exec -it c sh · docker rm -f c
docker compose up -d · docker compose ps · docker compose down [-v]
# Ansible
ansible all -m ping · ansible-playbook p.yml --check --diff · ansible-playbook p.yml -e var=val
# Kubernetes
kubectl get pods,svc -o wide · kubectl apply -f . · kubectl describe pod p · kubectl logs p
kubectl scale deployment d --replicas=4 · kubectl set image deployment/d c=img:tag
kubectl rollout status|history|undo deployment/d
```

## Dépannage

Voir l'annexe « Dépannage » de l'énoncé d'origine (`depot-tp/ENONCE.md`, fin du fichier) : elle couvre `vboxsf`, plage réseau, Jenkins lent, `docker.sock`, Ansible, registre non sécurisé, Elasticsearch, `ImagePullBackOff` et Selenium.
