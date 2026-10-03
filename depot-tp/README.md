# Formation DevOps (2 jours) — Dépôt de TP

Projet fil rouge : application Java Spring Boot → CI Jenkins → image Docker → déploiement Ansible / Compose / Kubernetes → supervision Prometheus/Grafana + logs ELK.

- **Énoncé stagiaires** : [`ENONCE.md`](ENONCE.md)
- **Infrastructure** : [`Vagrantfile`](Vagrantfile) (5 VM Ubuntu 22.04 + provisionnement complet)

## Démarrage rapide

```bash
vagrant up                    # ci + target (jour 1)
vagrant up elk monitor k8s    # jour 2
```

| VM | IP | RAM par défaut | Variable |
|---|---|---|---|
| ci | 192.168.56.10 | 4096 | `CI_RAM` |
| target | 192.168.56.11 | 2048 | `TARGET_RAM` |
| monitor | 192.168.56.12 | 2048 | `MONITOR_RAM` |
| elk | 192.168.56.13 | 4608 | `ELK_RAM` |
| k8s | 192.168.56.14 | 3072 | `K8S_RAM` |

Total si tout tourne : ~16 Go. Jour 1 seul : ~6 Go.

## Notes pour le formateur

- **À faire AVANT la formation** : lancer `vagrant up` une fois sur chaque poste (ou exporter les boxes) — le premier provisionnement télécharge plusieurs Go (images Docker, plugins Jenkins, k3s, Chrome).
- Les VM `ci` et `target` partagent une clé SSH via `/vagrant/.shared/ci_key.pub` : démarrer `ci` **avant** `target` (sinon `vagrant provision target`).
- Les fichiers `Dockerfile`, `Jenkinsfile.*`, `ansible/*.yml`, `monitoring/`, `elk/`, `k8s/` constituent le **corrigé**. Pour un format « exercice », retirez-les avant distribution et laissez les stagiaires les écrire à partir de l'énoncé.
- Jenkins est préconfiguré (admin/admin, plugins installés, utilisateur `jenkins` membre du groupe `docker`, clé SSH vers `target`/`k8s`). Les jobs sont créés par les stagiaires (c'est un objectif du TP1).
- Le webhook GitHub exige une URL publique ; le `pollSCM` (2 min) sert de repli.
- Versions principales : JDK 21, Spring Boot 3.3.5, Jenkins LTS, Ansible-core 2.16/2.17, ELK 8.13.4, Prometheus 2.53, Grafana 11.2, k3s (dernière stable), Selenium 4.25.
- Réinitialiser : `vagrant destroy -f` (et supprimer `.shared/`).
- Non testé dans l'environnement de génération (pas de VirtualBox) : faites un `vagrant up` complet avant la session pour valider votre réseau et vos miroirs d'images.

---

## Ajouts pour la version « 3 jours »

Voir `../docs/00-LISEZ-MOI.md`. Nouveaux fichiers : `mini-tp/` (calculatrice, site-demo, compose-db, k8s-nginx, logs, selenium), `ansible/mini-tp/`, `jenkins/Jenkinsfile.final`, `jenkins/Jenkinsfile.rollback`, `k8s/configmap.yaml`, `k8s/secret.yaml`, `solutions/alert-slowrequests.yml`. Ces fichiers n'ont pas pu être exécutés lors de leur conception : faites un déroulé complet avant la session.
