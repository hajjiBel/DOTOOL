# Formation sur les Outils DevOps 

Onze TP qui mènent un `git push` jusqu'à une application déployée, testée et supervisée, sur une petite application Java fil rouge (gestionnaire de tâches, Spring Boot 3.3.5, Java 21).

## Parcours

| TP | Thème | Durée |
|---|---|---|
| 01 | Git en équipe | 
| 02 | Maven et tests | 
| 03 | Intégration continue avec Jenkins | 
| 04 | Docker | 
| 05 | Docker Compose et registre | 
| 06 | Ansible | 
| 07 | Pipeline CI/CD complet |
| 08 | Kubernetes | 
| 09 | Logs avec ELK | 
| 10 | Prometheus et Grafana | 
| 11 | Selenium | 

Chaque TP suit trois niveaux : découverte, mini-TP, puis intégration dans le fil rouge.

## Prérequis

- VirtualBox et Vagrant
- Un compte GitHub
- Environ 8 Go de RAM libres pour lancer les VM utiles au TP du jour

## Démarrage

```bash
vagrant up ci            # démarre la VM principale
vagrant ssh ci           # s'y connecter
cd ~/devops-formation
```

Les autres VM (`target`, `monitor`, `elk`, `k8s`) se démarrent au besoin avec `vagrant up <nom>`.

## Infrastructure

| VM | Adresse | Rôle |
|---|---|---|
| ci | 192.168.56.10 | Jenkins (:8080), registre (:5000), Ansible |
| target | 192.168.56.11 | Application (:8080) |
| monitor | 192.168.56.12 | Prometheus (:9090), Grafana (:3000) |
| elk | 192.168.56.13 | Elasticsearch (:9200), Kibana (:5601) |
| k8s | 192.168.56.14 | k3s, NodePort 30080 |

Jenkins : http://192.168.56.10:8080 (`admin` / `admin`, à usage pédagogique uniquement).

## Structure du dépôt

```text
app/              application fil rouge (code, Dockerfile, docker-compose.yml)
mini-tp/          mini-TP autonomes (calculatrice, site-demo, compose-db, ...)
jenkins/          Jenkinsfile de chaque étape
ansible/          inventaire, playbooks (deploy-app.yml, site.yml)
k8s/              manifestes Kubernetes
elk/              configuration Logstash et Filebeat
monitoring/       configuration Prometheus et alertes
selenium-tests/   tests d'interface
```

## Pipeline de référence

```text
push → Checkout → Build + tests → Package → Docker build → Push registre
     → Ansible deploy → Smoke test → [Selenium] → Promotion latest/stable → [K8s] → Notification
```

Si une étape échoue, la chaîne s'arrête. Les tags d'image portent le numéro de build.
