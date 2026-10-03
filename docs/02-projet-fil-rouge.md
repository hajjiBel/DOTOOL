# Le projet fil rouge : `devops-demo`

Application Java Spring Boot 3.3.5 / JDK 21 : un **gestionnaire de tâches** (API REST + page web). Elle est volontairement simple pour que l'attention reste sur la chaîne de livraison.

| Point d'entrée | Rôle | Utilisé dans |
|---|---|---|
| `GET /` | Interface web (champ `#title`, bouton `#add-btn`, liste `#task-list`) | TP11 |
| `GET/POST /api/tasks`, `PUT /api/tasks/{id}/toggle`, `DELETE /api/tasks/{id}` | API REST | TP02, TP09 |
| `GET /api/info` | Nom, **version**, nom d'hôte | smoke tests, TP08 |
| `GET /api/simulate/error` | Exception Java → HTTP 500 | TP09, TP10 |
| `GET /api/simulate/slow?ms=1500` | Latence artificielle | TP10 |
| `/actuator/health`, `/actuator/prometheus` | Santé et métriques | TP04, TP08, TP10 |

> Limite à connaître : les tâches sont stockées **en mémoire** et disparaissent à chaque redémarrage. C'est utile pédagogiquement (on voit ce que « sans état persistant » implique avec Docker et Kubernetes), mais l'application n'a pas de base de données.

## Les 13 versions : ce qui change, et pourquoi

On ne montre **jamais** l'architecture finale au départ : chaque version ajoute **un seul** élément pour résoudre **un seul** problème.

| Version | TP | Ce qui change | Problème résolu (le POURQUOI) | Preuve de réussite |
|---|---|---|---|---|
| **V1** Application locale | Intro, TP01 | L'appli tourne avec `java -jar` sur une machine | – (point de départ : « ça marche chez moi ») | `curl localhost:9090/api/info` |
| **V2** Versionnée avec Git | TP01 | Dépôt Git, branches, Pull Request sur GitHub | Plus de « version finale_v2 », travail à plusieurs sans écrasement | Historique + PR fusionnée |
| **V3** Build Maven | TP02 | `mvn clean package` produit un JAR reproductible | Tout le monde compile pareil, sans IDE | `target/devops-demo.jar` |
| **V4** Tests automatisés | TP02 | Tests unitaires et d'intégration exécutés par Maven | Détecter une régression en secondes, pas en recette | Rapports Surefire |
| **V5** Intégration continue | TP03 | Jenkins compile et teste à chaque `push` | Personne ne « oublie » de lancer les tests | Build vert/rouge + historique |
| **V6** Conteneurisation | TP04 | Image Docker de l'application | Même exécutable partout, plus de « dépendances différentes » | `docker ps` : *healthy* |
| **V7** Publication de l'image | TP05 | Image poussée dans un registre, déployée avec Compose | Un artefact versionné et partageable | Tags dans le registre |
| **V8** Déploiement Ansible | TP06 | Ansible déploie le conteneur sur `target` | Un déploiement décrit en code, rejouable, sans SSH manuel | `changed` / `ok` |
| **V9** Pipeline CI/CD complet | TP07 | Jenkins enchaîne build → image → registre → déploiement → smoke test | De `git push` à l'application en ligne sans intervention | Version affichée = n° du build |
| **V10** Kubernetes | TP08 | Déploiement avec 2 réplicas, Service, mise à jour progressive | Auto-réparation, scaling, mise à jour sans coupure | `kubectl get pods` |
| **V11** Logs centralisés | TP09 | Filebeat → Logstash → Elasticsearch → Kibana | Retrouver une erreur sans se connecter aux serveurs | Dashboard Kibana |
| **V12** Monitoring | TP10 | Prometheus collecte, Grafana affiche, Alertmanager alerte | Voir les problèmes **avant** les utilisateurs | Dashboard + alerte *Firing* |
| **V13** Tests fonctionnels | TP11 | Selenium bloque la promotion d'une image défectueuse | Une régression d'interface ne part jamais en production | Pipeline rouge, `stable` inchangé |
| **Finale** | Projet final | Toute la chaîne, automatisée et supervisée | Livrer vite **et** en sécurité | Démonstration + exercice d'incident |

### Rappel pédagogique pour chaque version

Au début de chaque TP « fil rouge », le formateur écrit au tableau trois lignes : **Avant** (ce qui était douloureux), **Après** (ce qui change), **Preuve** (comment on le voit). Les participants les recopient dans leur journal de bord.

## La chaîne CI/CD, construite étape par étape

| # | Étape | Où elle est construite | Stage Jenkins |
|---|---|---|---|
| 1 | Developer → Git | TP01 | – |
| 2 | Git → Jenkins | TP03 | Checkout |
| 3 | Jenkins → Maven build | TP02 puis TP03 | Build Maven |
| 4 | Jenkins → Tests | TP02 puis TP03 | Tests unitaires |
| 5 | Jenkins → Package | TP03 | Package (JAR archivé) |
| 6 | Jenkins → Docker build | TP04 puis TP07 | Docker build |
| 7 | Jenkins → Docker Registry | TP05 puis TP07 | Registre |
| 8 | Jenkins → Déploiement | TP06 puis TP07 | Déploiement Ansible |
| 9 | Jenkins → Tests après déploiement | TP07 (smoke) puis TP11 (Selenium) | Smoke test, Selenium |
| 10 | Jenkins → Notification | TP07 | `post { always }` + rapport |

Le pipeline complet est `jenkins/Jenkinsfile.final`. Les `Jenkinsfile.tp1`, `.tp4`, `.tp8` sont les étapes intermédiaires de l'archive d'origine.

## Évolution du dépôt

```
devops-formation/
├── app/                 V1-V4  : code Java, pom.xml, tests ; V6 : Dockerfile ; V7 : docker-compose.yml
├── jenkins/             V5     : Jenkinsfile.tp1 → V9 : Jenkinsfile.tp4 → Jenkinsfile.final (+ .tp8, .rollback)
├── ansible/             V8     : inventory.ini, deploy-app.yml, site.yml, mini-tp/
├── k8s/                 V10    : deployment.yaml, service.yaml, configmap.yaml, secret.yaml
├── elk/                 V11    : stack ELK + Filebeat
├── monitoring/          V12    : Prometheus, Alertmanager, Grafana
├── selenium-tests/      V13    : tests fonctionnels
├── mini-tp/             Mini-TP indépendants (calculatrice, site-demo, compose-db, k8s-nginx, logs, selenium)
└── solutions/           Corrigés complémentaires
```

## Scénarios de tests (matrice)

| Niveau | Outil | Scénario | Commande ou classe | Résultat attendu |
|---|---|---|---|---|
| Manuel | `curl` | Ajouter une tâche | `curl -s -XPOST -H 'Content-Type: application/json' -d '{"title":"x"}' localhost:9090/api/tasks` | `201` + JSON |
| Manuel | `curl` | Titre vide | idem avec `{"title":""}` | `400` |
| Manuel | `curl` | Tâche inconnue | `curl -s -o /dev/null -w '%{http_code}' -XDELETE localhost:9090/api/tasks/9999` | `404` |
| Unitaire | JUnit | Ajout, titre vide, bascule, suppression | `TaskServiceTest` | 4 tests verts |
| Intégration | MockMvc | 201, 400, 404, 500, santé, page d'accueil | `TaskControllerTest` | 6 tests verts |
| API après déploiement | `curl` | Version déployée = n° du build | stage *Smoke test* | `grep` réussit |
| Fonctionnel | Selenium | Titre, ajout/terminaison, suppression, santé, `/api/info` | `AppUiTest` | 5 tests verts |
| Régression | Selenium | Renommer `id="page-title"` | TP11 | Pipeline rouge, pas de promotion |

## Pistes pour enrichir le projet (hors des 3 jours)

- Ajouter PostgreSQL : dépendances `spring-boot-starter-data-jpa` et `postgresql`, entité `Task`, `application-prod.properties`, service `db` dans Compose, `Secret` Kubernetes pour le mot de passe.
- Un second service (par exemple un service de notifications) pour travailler Compose et Kubernetes à plusieurs services.
- Helm, uniquement si la formation est rallongée.
