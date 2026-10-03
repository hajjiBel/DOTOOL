# TP 7 — Pipeline CI/CD complet (V9)

**Durée : 150 min** · théorie 15 · activité 20 · mini-TP 35 · fil rouge 70 · validation 10 · **VM** : `ci` + `target` · **Format** : binômes
*(Étapes 6 à 10 de la chaîne : Docker build, registre, déploiement, tests après déploiement, notification, puis reconstruction du pipeline complet.)*

#### 1. Objectif pédagogique
Construire un pipeline qui mène un `git push` jusqu'à l'application déployée, **identifier à quelle étape il s'arrête quand quelque chose casse**, et savoir revenir à une version saine.

#### 2. Prérequis
TP01 à TP06 : Git, Maven/tests, Jenkins CI, image Docker, registre, déploiement Ansible.

#### 3. Concept DevOps abordé
**Livraison continue (CD).** Chaque étape est une porte (*gate*) : une version défectueuse s'arrête au plus tôt. L'artefact (image taguée par numéro de build) est construit **une seule fois** et déployé tel quel ; le smoke test vérifie que la bonne version tourne ; une notification ferme la boucle.

#### 4. Problème réel à résoudre
« Chaque mise en production demande de lancer à la main une dizaine d'opérations, dans l'ordre. Une étape oubliée et le serveur sert l'ancienne version sans que personne ne s'en rende compte. Comment livrer à la demande, sans erreur, et pouvoir revenir en arrière ? »

#### 5. Activité pédagogique de découverte **[N1 Découverte]** (20 min) — « Le jeu des cartes »
Chaque binôme reçoit 12 cartes (une par étape) : *Récupérer le code · Compiler · Tests unitaires · Empaqueter le JAR · Construire l'image · Pousser au registre · Déployer · Smoke test · Tests Selenium · Promouvoir `stable` · Notifier · Déployer en production*.
1. **Remettez-les dans l'ordre** (5 min). Chaque carte indique ce qu'elle produit (un artefact) et ce qu'elle exige.
2. **Placez les portes** : quelles cartes bloquent la suite en cas d'échec ?
3. **Injectez 3 pannes** (tirage au sort) : *test unitaire rouge*, *déploiement impossible*, *mauvaise version affichée*. À quelle carte le pipeline doit-il s'arrêter ? Que doit-il dire ? Que faire du serveur ?

> 🎤 **Formateur** : comparez les ordres proposés au pipeline de référence (`02-projet-fil-rouge.md`). Faites exprimer la règle : « on ne promeut que ce qui a passé toutes les portes ; on ne reconstruit jamais l'image entre deux étapes. »

#### 6. Mini-TP **[N2 Application]** (35 min) — « Livrer un site, avec retour arrière automatique »
- **Contexte** : le site statique `site-demo` (TP04), sans l'application Java.
- **Objectif** : build → push → déploiement → smoke test → **rollback** automatique en cas d'échec.
- **Architecture** : Jenkins (`ci`) construit l'image, la pousse dans le registre, l'exécute sur le port 8088 de `ci`, vérifie `version.txt`.
- **Fichier** `mini-tp/site-demo/Jenkinsfile` :

```groovy
// Mini-TP CI/CD : build -> push -> deploy -> smoke test -> rollback automatique
pipeline {
    agent any
    parameters {
        booleanParam(name: 'BREAK_SMOKE', defaultValue: false, description: 'Simule une version défectueuse')
    }
    environment {
        REGISTRY = '192.168.56.10:5000'
        IMAGE    = "${REGISTRY}/site-demo"
        TAG      = "${env.BUILD_NUMBER}"
        LAST     = "${JENKINS_HOME}/site-demo.last_good"
    }
    stages {
        stage('Checkout') { steps { checkout scm } }
        stage('Build image') {
            steps { dir('mini-tp/site-demo') { sh 'docker build --build-arg VERSION=$TAG -t $IMAGE:$TAG .' } }
        }
        stage('Push registre') { steps { sh 'docker push $IMAGE:$TAG' } }
        stage('Deploy') {
            steps { sh 'docker rm -f site-demo || true; docker run -d --name site-demo -p 8088:80 $IMAGE:$TAG; sleep 3' }
        }
        stage('Smoke test') {
            steps {
                script {
                    def attendu = params.BREAK_SMOKE ? 'version-inexistante' : env.BUILD_NUMBER
                    sh "curl -fs http://localhost:8088/version.txt | grep -qx '${attendu}'"
                }
            }
        }
    }
    post {
        success { sh 'echo $TAG > $LAST' }
        failure {
            script {
                if (fileExists(env.LAST)) {
                    def prev = readFile(env.LAST).trim()
                    echo "ROLLBACK vers la version ${prev}"
                    sh "docker rm -f site-demo || true; docker run -d --name site-demo -p 8088:80 ${IMAGE}:${prev}"
                } else {
                    echo 'Aucune version précédente connue : pas de rollback possible'
                }
            }
        }
    }
}
```

**Étapes**
1. Job Pipeline **`site-demo-cd`** (Pipeline from SCM, **Script Path** `mini-tp/site-demo/Jenkinsfile`). Lancez **deux builds** (bouton *Build with Parameters* ; `BREAK_SMOKE` décoché). Après chaque :
   ```bash
   curl -s localhost:8088/version.txt          # le numéro du dernier build
   curl -s http://192.168.56.10:5000/v2/site-demo/tags/list
   ```
2. Lancez un 3ᵉ build avec **`BREAK_SMOKE` coché** : le smoke test échoue, la section `post { failure }` redéploie la version précédente.
3. Vérifiez : `curl -s localhost:8088/version.txt` renvoie le numéro du **build 2**, pas du 3.

**Résultat attendu** : builds 1 et 2 verts ; build 3 rouge avec « ROLLBACK vers la version 2 » ; le site sert toujours la version 2 ; l'image `:3` existe dans le registre mais n'est plus déployée.

**Erreurs fréquentes**
- Premier build rouge sans rollback possible : normal si aucune version n'a réussi avant.
- Port 8088 occupé : `docker rm -f site-demo`.
- `docker: permission denied` pour l'utilisateur `jenkins` : le groupe `docker` n'est pas pris en compte (`sudo systemctl restart jenkins`).

**Solution** : le `Jenkinsfile` ci-dessus. Idée clé : la dernière version **saine** est mémorisée dans un fichier (`site-demo.last_good`) mis à jour uniquement après un smoke test réussi.

#### 7. Retour pédagogique
- **Appris** : un smoke test doit vérifier *la bonne version*, pas seulement « ça répond » ; le rollback est possible parce que chaque version est une image taguée immuable.
- **Pourquoi en DevOps** : livraisons fréquentes et sûres ; l'erreur est détectée par la machine, pas par l'utilisateur.
- **Problème résolu** : mise en production manuelle, peur du changement, retour arrière improvisé.
- **Limites** : sans tests solides, la CD automatise aussi les erreurs ; un rollback ne répare pas les données (migrations) ; une seule cible de déploiement ici, pas de bascule progressive.

#### 8. Retour au projet fil rouge **[N3 Intégration]** (70 min) — V9
**Rappel des étapes déjà construites** : 1 Git (TP01) · 2 Jenkins (TP03) · 3 Maven (TP02/03) · 4 Tests · 5 Package. Il reste les étapes 6 à 10.

**Partie A — le pipeline de l'archive (25 min)**
1. Lisez `jenkins/Jenkinsfile.tp4` ligne par ligne. Qu'est-ce qui change à chaque build ? (`TAG = BUILD_NUMBER`)
2. Job **`devops-cicd`** → Script Path `jenkins/Jenkinsfile.tp4`. Lancez.
3. Vérifiez :
   ```bash
   curl -s http://192.168.56.10:5000/v2/devops-demo/tags/list
   curl -s http://192.168.56.11:8080/api/info          # "version":"<n° du build>"
   ```
4. **Cycle complet** : modifiez le titre dans `app/src/main/resources/templates/index.html`, branche + PR + fusion ; observez le déclenchement automatique, puis rechargez http://192.168.56.11:8080 : la nouvelle version est en ligne.

**Partie B — le pipeline final, étape par étape (25 min)**
5. Job **`devops-final`** → Script Path `jenkins/Jenkinsfile.final`. Comparez avec `.tp4` et complétez le tableau :

   | Étape | Stage | Preuve visible |
   |---|---|---|
   | 6 Docker build | ? | image `devops-demo:<n>` |
   | 7 Registre | ? | tag dans le catalogue |
   | 8 Déploiement | ? | version qui change |
   | 9 Tests après déploiement | ? | stage vert + `grep` de la version |
   | 10 Notification | ? | `build-report.txt` archivé |

6. Lancez le job avec les paramètres par défaut : `RUN_SELENIUM` et `DEPLOY_K8S` décochés (activés aux TP11 et TP08).
7. Téléchargez `build-report.txt` depuis les artefacts.

**Partie C — le pipeline qui s'arrête au bon endroit (15 min)**
8. **Panne 1** : cassez un test, poussez. Le pipeline s'arrête-t-il **avant** la construction de l'image ?
9. **Panne 2** : sur `target`, `ssh vagrant@192.168.56.11 "docker stop devops-demo"` pendant un build entre *Déploiement* et *Smoke test* (ou changez `TARGET_HOST` pour une IP erronée dans une branche). Que montre le smoke test ?
10. **Reconstruisez le pipeline complet au tableau** avec le formateur : un schéma à 10 cases, où chaque case porte son *stage*, sa *preuve* et sa *porte* (bloque/ne bloque pas).

**Pipeline obtenu** :
```
push → Checkout → Build+Tests → Package → Docker build → Push registre → Ansible deploy → Smoke test (version) → [Selenium] → Promotion latest/stable → [K8s] → Notification
```
**Résultat attendu** : page et `/api/info` affichent le numéro du dernier build réussi ; un test rouge empêche l'image d'exister ; le rapport de build est archivé.

**Pourquoi V9 ?** Avant : des briques isolées (Jenkins, Docker, Ansible). Après : une chaîne unique déclenchée par `git push`. Preuve : la version en ligne suit le numéro de build.

#### 9. Validation
1. Pourquoi tagguer l'image avec `BUILD_NUMBER` plutôt que seulement `latest` ?
2. À quelle étape un test unitaire cassé arrête-t-il la chaîne ?
3. Que vérifie exactement le smoke test de `Jenkinsfile.final` ?
4. Tâche : prouvez qu'un build échoué n'a pas changé la version en ligne.
5. Où retrouve-t-on le résultat d'un build sans ouvrir Jenkins ? (`build-report.txt`, webhook de notification)

#### 10. Extension / challenge
- Renseignez `NOTIFY_URL` avec un récepteur de test (par exemple un `nc -l` sur `ci`) et vérifiez la requête.
- Ajoutez un stage **manuel** (`input`) avant la promotion pour valider une version.
- Ajoutez le stage de rollback automatique du mini-TP au pipeline du fil rouge.
