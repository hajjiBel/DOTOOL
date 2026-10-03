# Scénarios d'entreprise, projet final et évaluation

## 1. Scénario d'entreprise du jour 2 (75 min) — « Déploiement manuel, mises en production de plusieurs heures »

### Situation
> Vous êtes une équipe DevOps de 4 personnes dans une entreprise de logistique. L'application Java `devops-demo` est déployée **à la main** : un technicien se connecte en SSH, copie un JAR, redémarre le service, vérifie à l'œil. Une mise en production prend 3 heures, a lieu le samedi, et échoue une fois sur cinq. Il n'existe aucun moyen simple de revenir en arrière.

### Partie 1 — atelier de conception (30 min, binômes, jour 2 à 12:00)
Chaque binôme remplit **une page** :

| Rubrique | Question guide |
|---|---|
| **Organisation** | Qui écrit, qui relit, qui valide, qui est d'astreinte ? |
| **Outils** | Quels outils de la formation, pour quelle étape ? |
| **Workflow Git** | Branches, PR, règles de fusion ? |
| **Pipeline** | Étapes, portes, déclencheurs ? |
| **Automatisation** | Qu'est-ce qui reste manuel, et pourquoi ? |
| **Monitoring** | Quels indicateurs, quelles alertes ? |
| **Gestion des incidents** | Que fait-on à 3 h du matin ? Qui décide du retour arrière ? |

> 🎤 **Formateur** : circulez, challengez : « qui peut déployer ? », « comment sait-on que la version déployée est la bonne ? », « que se passe-t-il si le déploiement échoue à moitié ? ».

### Partie 2 — réalisation technique (45 min, jour 2 à 16:15)
**Réaliser le retour arrière** du scénario : un job Jenkins paramétré qui redéploie une version connue.
1. Lisez `jenkins/Jenkinsfile.rollback` (paramètre `TAG`, contrôle, redéploiement Ansible, vérification de version).
2. Créez le job `devops-rollback` (Pipeline from SCM, **Script Path** `jenkins/Jenkinsfile.rollback`).
3. **Exercice d'incident** :
   - Déployez la version N via `devops-final`.
   - Cassez volontairement l'application (changez un titre pour une valeur erronée) et déployez N+1 en désactivant un test (simule une version défectueuse).
   - Constatez le problème, lancez `devops-rollback` avec `TAG=<N>`.
   - Vérifiez : `curl -s http://192.168.56.11:8080/api/info`.
4. **Mesurez** : temps total du retour arrière, à comparer aux « 3 heures » du scénario.

**Livrable** : la page de conception + une capture du job `devops-rollback` vert + le temps mesuré.

## 2. Projet final (jour 3, 90 min : 75 min de réalisation + 15 min de validation)

### Énoncé
> Votre équipe (binôme) reçoit une demande métier : **ajouter une nouvelle fonctionnalité à `devops-demo` et la livrer en production à travers toute la chaîne**. Un incident vous sera ensuite injecté par le formateur.

Fonctionnalité au choix (exemples) : `GET /api/tasks/stats` (nombre total / terminées), filtrage `GET /api/tasks?done=true`, ou ajout d'un champ à l'interface.

### Exigences (progressives)

| Niveau | Contenu | Points forts évalués |
|---|---|---|
| **Bronze** *(obligatoire)* | Branche + PR relue · test unitaire pour la fonctionnalité · pipeline `devops-final` vert · version déployée sur `target` (visible dans `/api/info`) | Git, tests, CI/CD |
| **Argent** | + test Selenium ou test d'intégration sur la nouvelle fonctionnalité · déploiement Kubernetes (`DEPLOY_K8S`) · nouvelle ligne de log visible dans Kibana **ou** panneau Grafana dédié | Qualité, orchestration, observabilité |
| **Or** | + **exercice d'incident** : le formateur casse une chose parmi (test rouge · image avec mauvais tag · application arrêtée · erreurs 500 injectées). Le binôme **diagnostique avec les dashboards et les logs**, **corrige ou revient en arrière**, et explique la cause | Incident, analyse, retour arrière |

### Livrables
1. URL du dépôt GitHub (PR fusionnée, historique lisible).
2. Capture du pipeline vert (Stage View) et de `build-report.txt`.
3. Capture du dashboard (Kibana ou Grafana) montrant la fonctionnalité en action.
4. Pour l'exercice d'incident : 5 lignes — symptôme, outil utilisé, cause, action, temps de résolution.
5. Démonstration orale de 5 minutes.

> Si le temps manque : le projet final peut se terminer en travail personnel (jusqu'à une demi-journée) ; la démonstration se fait alors le lendemain ou à distance.

### Aide formateur pour l'exercice d'incident
Préparez 4 « pannes » prêtes à injecter (une par binôme à tour de rôle) :
1. Un test cassé poussé sur `main` (la porte doit arrêter le pipeline).
2. `docker stop devops-demo` sur `target` (alerte `AppDown`).
3. Boucle `curl /api/simulate/error` (alerte `AppHighErrorRate`, logs 500 dans Kibana).
4. `kubectl set image … :999` (`ImagePullBackOff`, ancien pod toujours actif).

## 3. Évaluation

| Forme | Où | Détail |
|---|---|---|
| **Exercices individuels** | « Validation » de chaque TP | 3 à 5 questions ou petites tâches, contrôlées par le formateur |
| **Exercices en binôme** | TP01, TP07, scénario du jour 2 | PR croisées, pipeline, retour arrière |
| **Challenges** | Rubrique « Extension » de chaque TP | Facultatifs, valorisés |
| **QCM de compréhension** | Fin du jour 3 (15 min) | Ci-dessous |
| **Mini-projets** | Scénario du jour 2 | Conception + retour arrière |
| **Projet final** | Jour 3 | Grille ci-dessous |

### QCM (corrigé entre parenthèses)
1. Quelle commande Git montre l'historique en graphe ? *(`git log --oneline --graph --all`)*
2. Pourquoi ne pousse-t-on pas directement sur `main` ? *(relecture par PR, build vert requis)*
3. Que fait `mvn clean verify` ? *(compile, teste, empaquette)*
4. Quel fichier décrit un pipeline Jenkins « as code » ? *(`Jenkinsfile`)*
5. Image ou conteneur : lequel est jetable ? *(le conteneur)*
6. Pourquoi tagguer une image avec le numéro de build ? *(traçabilité et retour arrière)*
7. Que signifie « idempotent » ? *(rejouer sans effet s'il n'y a rien à changer)*
8. Quand un *handler* Ansible s'exécute-t-il ? *(uniquement si une tâche notifiante a changé quelque chose)*
9. Quelle ressource Kubernetes garantit N pods et gère les mises à jour ? *(le Deployment)*
10. Que protège la `readinessProbe` pendant un rolling update ? *(l'envoi de trafic vers un pod pas encore prêt)*
11. Quel composant ELK collecte les logs ? Analyse ? Stocke ? Affiche ? *(Filebeat ; Logstash ; Elasticsearch ; Kibana)*
12. Log ou métrique : lequel dit « 12 requêtes par seconde » ? *(la métrique)*
13. Pourquoi une alerte a-t-elle un `for:` ? *(éviter les fausses alertes sur un pic bref)*
14. À quel endroit du pipeline se place Selenium ? *(après le déploiement de test, avant la promotion)*
15. Le `Secret` Kubernetes est-il chiffré ? *(non, encodé en base64 ; il faut limiter les droits)*

### Grille d'évaluation du projet final (/20)

| Critère | Insuffisant (0) | Acquis | Excellent | Points |
|---|---|---|---|---|
| **Git et collaboration** | Commits directs sur `main`, historique illisible | PR relue, branche dédiée | + messages clairs, relecture utile | /3 |
| **Tests et qualité** | Aucun test nouveau | Test unitaire de la fonctionnalité | + test d'intégration/Selenium, test du cas limite | /3 |
| **Pipeline CI/CD** | Étapes manuelles | Pipeline vert, version déployée visible | + portes correctes, rapport, paramètres bien utilisés | /5 |
| **Conteneur et déploiement** | Image non publiée | Image taguée, déploiement sur `target` | + Kubernetes, mise à jour sans coupure | /3 |
| **Observabilité** | Rien de visible | Une trace de la fonctionnalité dans Kibana **ou** Grafana | + les deux, alerte pertinente | /3 |
| **Incident et retour arrière** | Non traité | Diagnostic avec les outils | + cause expliquée, retour arrière, temps maîtrisé | /2 |
| **Démonstration et communication** | Confuse | Claire, tient en 5 min | + répond aux questions, explique les choix | /1 |
| **Total** | | | | **/20** |

**Barème des mini-évaluations** (indicatif) : exercices de validation 20 %, QCM 15 %, scénario du jour 2 15 %, projet final 50 %.

### Compétences visées (à rendre explicites en fin de formation)
- Travailler avec Git en équipe (branches, PR).
- Automatiser build et tests (Maven, Jenkins).
- Conteneuriser et publier une application (Docker, registre).
- Décrire et rejouer un déploiement (Ansible).
- Construire et exploiter un pipeline CI/CD avec portes qualité.
- Déployer et mettre à jour sur Kubernetes.
- Diagnostiquer avec logs et métriques ; définir des alertes.
- Mener un retour arrière lors d'un incident.
