# Catalogue des séries de TP et modules optionnels

Votre cahier des charges décrit des séries détaillées (Git 8 TP, Docker 8, Ansible 10, Kubernetes 10, ELK 8, monitoring 7…). En 3 jours, chaque série est **regroupée dans un TP**. Ce catalogue liste *toutes* les étapes, indique où elles sont traitées et ce qu'il faut développer pour une version longue.

**Légende** : ✅ traité dans les 3 jours · 🟡 traité en partie (condensé ou en démonstration) · ⬜ non traité (module optionnel).

Les durées « version longue » sont des **estimations** à valider à l'usage.

## Git (série de 8 TP) — traité dans TP01 (+ TP03)

| # | Étape | Statut | Où | Objectif du module optionnel |
|---|---|---|---|---|
| 1 | Git local | ✅ | TP01 mini-TP | – |
| 2 | Commit | ✅ | TP01 | – |
| 3 | Branch | ✅ | TP01 | – |
| 4 | Merge (conflit) | ✅ | TP01 | – |
| 5 | Pull Request | ✅ | TP01 fil rouge | Règles de protection de branche, revue de code structurée |
| 6 | GitHub | ✅ | TP01 fil rouge | – |
| 7 | Workflow d'équipe | 🟡 | TP01 fil rouge (2 personnes) | Simuler une équipe de 4 : *trunk-based* vs *git flow*, `rebase`, `cherry-pick`, `revert` |
| 8 | Intégration avec Jenkins | ✅ | TP03 | Multibranch Pipeline, webhook, statut de PR |

**Version longue : +1 h 30.**

## Tests automatisés — TP02, TP07, TP11

| Étape | Statut | Où |
|---|---|---|
| Test manuel (et pourquoi il ne passe pas à l'échelle) | ✅ | TP02 activité, TP11 activité |
| Test unitaire | ✅ | TP02 |
| Test d'intégration | ✅ | TP02 (`TaskControllerTest`) |
| Test API | 🟡 | TP02 (curl), TP07 (smoke test) ; module optionnel : collection Postman/Newman ou REST Assured |
| Test fonctionnel / Selenium | ✅ | TP11 |
| Tests dans Jenkins | ✅ | TP03, TP07, TP11 |

**Version longue : +2 h** (tests d'API automatisés, couverture JaCoCo, tests de performance simples).

## CI/CD (10 étapes) — TP03 et TP07

| Étape | Statut | Où |
|---|---|---|
| 1 Developer → Git | ✅ | TP01 |
| 2 Git → Jenkins | ✅ | TP03 |
| 3 Maven build | ✅ | TP02, TP03 |
| 4 Tests | ✅ | TP02, TP03 |
| 5 Package | ✅ | TP03 |
| 6 Docker build | ✅ | TP07 |
| 7 Registre | ✅ | TP05, TP07 |
| 8 Déploiement | ✅ | TP06, TP07 |
| 9 Tests après déploiement | ✅ | TP07 (smoke), TP11 (Selenium) |
| 10 Notification | 🟡 | TP07 (rapport + webhook) ; module optionnel : e-mail ou Slack réels |

**Version longue : +3 h** (une activité et un mini-TP dédiés à chaque étape, environnements dev/recette, approbation manuelle, paramètres de pipeline avancés).

## Docker (série de 8 TP) — TP04, TP05, TP07

| # | Étape | Statut | Où |
|---|---|---|---|
| 1 | Image / conteneur | ✅ | TP04 activité |
| 2 | Lancer un serveur web | ✅ | TP04 mini-TP A |
| 3 | Créer un Dockerfile | ✅ | TP04 mini-TP B |
| 4 | Conteneuriser l'application Java | ✅ | TP04 fil rouge |
| 5 | Plusieurs services avec Compose | ✅ | TP05 |
| 6 | Ajouter une base de données | 🟡 | TP05 mini-TP (hors application, qui est en mémoire) |
| 7 | Environnement reproductible | ✅ | TP05 fil rouge |
| 8 | Docker dans Jenkins | ✅ | TP07 |

**Version longue : +4 h** (intégration réelle de PostgreSQL dans l'application, multi-stage et optimisation d'image, réseaux et volumes, scan de vulnérabilités).

## Ansible (série de 10 TP) — TP06

| # | Étape | Statut | Où | Module optionnel |
|---|---|---|---|---|
| 1 | Problème de la configuration manuelle | ✅ | TP06 activité | – |
| 2 | Inventaire | ✅ | TP06 mini-TP | Groupes, variables d'hôte |
| 3 | Premier playbook | ✅ | TP06 mini-TP | – |
| 4 | Installation d'un serveur web | ✅ | TP06 mini-TP | – |
| 5 | Variables | ✅ | TP06 mini-TP | `group_vars`, précédence |
| 6 | Templates | ✅ | TP06 mini-TP | Filtres Jinja2 |
| 7 | Handlers | ✅ | TP06 mini-TP | – |
| 8 | Rôles | ⬜ | challenge TP06 | `ansible-galaxy init`, refactorer `site.yml` en rôles `common`, `nginx`, `java_app` |
| 9 | Déploiement de l'application Java | ✅ | TP06 fil rouge (`deploy-app.yml`, `site.yml`) | – |
| 10 | Intégration dans Jenkins | ✅ | TP07 (`ansible-playbook` dans le pipeline) | Identifiants Jenkins, Vault |

**Version longue : +5 h.**

## Kubernetes (série de 10 TP) — TP08

| # | Étape | Statut | Où |
|---|---|---|---|
| 1 | Premier Pod | 🟡 | TP08 (le Pod est vu à travers le Deployment ; un Pod nu en démonstration) |
| 2 | Deployment | ✅ | TP08 |
| 3 | Service | ✅ | TP08 |
| 4 | Exposition de l'application | ✅ | TP08 (NodePort) |
| 5 | Scaling | ✅ | TP08 |
| 6 | Rolling update | ✅ | TP08 |
| 7 | Configuration | ✅ | TP08 fil rouge (ConfigMap) |
| 8 | Secrets | ✅ | TP08 fil rouge |
| 9 | Déploiement de l'application fil rouge | ✅ | TP08 fil rouge |
| 10 | Déploiement depuis Jenkins | ✅ | TP08 fil rouge (`DEPLOY_K8S`) |

Introduction de l'activité « pourquoi Kubernetes ? » : ✅ (TP08 activité). Helm et concepts avancés : volontairement absents. **Version longue : +5 h** (Pod nu, labels, namespaces, Ingress, volumes persistants, HPA, `kubectl debug`).

## ELK (série de 8 TP) — TP09

| # | Étape | Statut | Où |
|---|---|---|---|
| 1 | Comprendre les logs | ✅ | TP09 activité (`grep`) |
| 2 | Collecter un fichier de logs | 🟡 | TP09 fil rouge (Filebeat sur les conteneurs) |
| 3 | Logstash / Filebeat | 🟡 | TP09 fil rouge (lecture de la configuration, pas d'écriture) |
| 4 | Elasticsearch | ✅ | TP09 mini-TP (`_bulk`, recherche) |
| 5 | Kibana | ✅ | TP09 |
| 6 | Recherche d'erreurs | ✅ | TP09 scénarios |
| 7 | Dashboard | ✅ | TP09 fil rouge |
| 8 | Connecter les logs du projet | ✅ | TP09 fil rouge |

Scénarios : 500, exception Java, 400/404 : ✅ ; base de données : mini-TP seulement ; temps de réponse : → TP10. **Version longue : +3 h 30** (écrire un `grok` soi-même, tests de pipeline Logstash, index lifecycle, alertes Kibana, logs applicatifs au format JSON).

## Monitoring (série de 7 TP) — TP10

| # | Étape | Statut | Où |
|---|---|---|---|
| 1 | Observer CPU / RAM | ✅ | TP10 activité |
| 2 | Exporter des métriques | 🟡 | TP10 (on **consomme** `node_exporter` et `/actuator/prometheus` ; ajouter un compteur métier custom = module optionnel) |
| 3 | Prometheus | ✅ | TP10 mini-TP |
| 4 | Grafana | ✅ | TP10 mini-TP |
| 5 | Dashboard | ✅ | TP10 fil rouge |
| 6 | Alertes | ✅ | TP10 fil rouge (dont `SlowRequests`) |
| 7 | Monitoring du projet fil rouge | ✅ | TP10 fil rouge |

Scénarios : CPU, mémoire, indisponibilité, requêtes élevées, latence : ✅. **Version longue : +3 h** (métriques personnalisées avec Micrometer, Alertmanager vers un vrai récepteur, SLO simples, traces avec OpenTelemetry).

## Récapitulatif pour une version longue

| Option | Durée totale approximative | Ajouts principaux |
|---|---|---|
| **3 jours (ce dossier)** | 21 h | 11 TP + projet final |
| **5 jours** | ≈ 35 h | Ajouter rôles Ansible, Pod nu et Ingress, tests API automatisés, PostgreSQL dans l'application, alertes Kibana, plus de temps projet final |
| **7 à 8 jours** | ≈ 50 h | Toutes les séries complètes avec une activité et un mini-TP par étape de chaque série |

Les ≈ 27 h supplémentaires des tableaux ci-dessus mènent à environ 48 h, soit 7 jours : c'est l'ordre de grandeur réaliste pour traiter **chaque** étape du cahier des charges avec activité, mini-TP et intégration au fil rouge.
