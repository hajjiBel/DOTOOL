# TP 3 — Intégration continue avec Jenkins (V5)

**Durée : 90 min** · théorie 10 · activité 15 · mini-TP 25 · fil rouge 30 · validation 10 · **VM** : `ci` · **Format** : individuel

#### 1. Objectif pédagogique
Concevoir une solution d'**intégration continue** : à chaque modification du code, la compilation et les tests s'exécutent automatiquement et le résultat est visible de toute l'équipe. Savoir lire un `Jenkinsfile` et diagnostiquer un build rouge.

#### 2. Prérequis
TP01 (dépôt GitHub avec le projet) et TP02 (`mvn test`).

#### 3. Concept DevOps abordé
**Intégration continue (CI).** Chaque `push` déclenche un pipeline : récupérer le code → compiler → tester → empaqueter. Un build rouge est un signal collectif : on le répare avant de continuer. Le pipeline est lui-même du code (`Jenkinsfile`), versionné avec l'application (*pipeline as code*).

#### 4. Problème réel à résoudre
« Un développeur modifie le code mais l'équipe doit compiler et tester manuellement l'application à chaque modification. On découvre les casses le vendredi soir. Concevoir une solution CI avec Jenkins. »

#### 5. Activité pédagogique de découverte **[N1 Découverte]** (15 min) — « Le script manuel »
Dans `mini-tp/calculatrice`, **par deux** : l'un joue le « développeur », l'autre « le garant de la qualité ».
1. Le développeur modifie `Calculator.java` (par exemple change un message d'erreur) et annonce « c'est prêt ».
2. Le garant exécute à la main : `mvn -B test`, puis `mvn -B -DskipTests package`, puis note le résultat sur une feuille.
3. Répétez 3 fois, en cassant un test la 2ᵉ fois sans le dire.

**Questions** : combien d'étapes répétitives ? Que se passe-t-il si le garant est absent ? Si le développeur oublie d'annoncer ? Qu'est-ce qui pourrait être automatisé ?

> 🎤 **Formateur** : dessinez au tableau la boucle « modifier → lancer → regarder → corriger » et entourez ce qui est mécanique. Terminez par : « Jenkins sera ce garant infatigable : il n'oublie jamais et prévient tout de suite. »

#### 6. Mini-TP **[N2 Application]** (25 min) — « Un garant pour la calculatrice »
- **Contexte** : le mini-projet `calculatrice` du TP02, déjà dans votre dépôt GitHub.
- **Objectif** : mettre en place un job Jenkins qui teste automatiquement à chaque push.
- **Architecture** : GitHub → Jenkins (VM `ci`) → rapport de tests.
- **Fichier** : `mini-tp/calculatrice/Jenkinsfile`

```groovy
pipeline {
    agent any
    triggers { pollSCM('H/2 * * * *') }
    stages {
        stage('Checkout') { steps { checkout scm } }
        stage('Tests') {
            steps { dir('mini-tp/calculatrice') { sh 'mvn -B test' } }
            post { always { junit 'mini-tp/calculatrice/target/surefire-reports/*.xml' } }
        }
        stage('Package') {
            steps {
                dir('mini-tp/calculatrice') { sh 'mvn -B -DskipTests package' }
                archiveArtifacts artifacts: 'mini-tp/calculatrice/target/*.jar'
            }
        }
    }
}
```

**Étapes**
1. Vérifiez que le dépôt GitHub contient `mini-tp/` (`git push` si besoin ; dépôt public de préférence).
2. Jenkins (http://192.168.56.10:8080, `admin`/`admin`) → **Nouveau Item** → `calc-ci` → **Pipeline**.
3. *Pipeline script from SCM* → Git → URL de votre dépôt → branche `*/main` → **Script Path** `mini-tp/calculatrice/Jenkinsfile` → Enregistrer → **Lancer un build**.
4. Observez la *Stage View* et les **Résultats des tests**.
5. **Cassez un test** : dans `CalculatorTest`, changez `119.0` en `120.0` ; `git commit -am "casse" && git push`. Attendez le polling (≈ 2 min) ou relancez.
6. Constatez le build rouge, repérez le test fautif dans le rapport, corrigez, poussez, constatez le retour au vert.

**Résultat attendu** : build #1 vert avec graphique de tests (5 tests) ; build #2 rouge (stage *Tests*, 1 échec) ; build #3 vert ; le JAR est archivé dans les artefacts du build vert.

**Erreurs fréquentes**
- *Script Path* faux (« Unable to find Jenkinsfile ») : le chemin est relatif à la racine du dépôt.
- Dépôt privé sans identifiants Jenkins : « Authentication failed » → rendez-le public ou ajoutez des identifiants.
- `mvn: command not found` : la VM `ci` n'a pas fini son provisionnement (`vagrant provision ci`).
- Attendre sans comprendre : le déclenchement est un *polling* toutes les 2 min (le webhook GitHub exige une URL publique).

**Solution** : le `Jenkinsfile` ci-dessus ; la correction est simplement de remettre `119.0`.

#### 7. Retour pédagogique
- **Appris** : un *pipeline* est une suite de *stages* ; le résultat (rouge/vert) est le contrat de qualité ; le rapport de tests évite de chercher dans la console.
- **Pourquoi en DevOps** : la CI donne un retour en minutes et rend l'intégration banale au lieu de redoutée.
- **Problème résolu** : oublis, casses découvertes tard, « ça marche chez moi ».
- **Limites** : la CI ne vaut que par la qualité des tests ; un build lent décourage ; un job configuré uniquement dans l'interface ne se reproduit pas (d'où le `Jenkinsfile`).

#### 8. Retour au projet fil rouge **[N3 Intégration]** (30 min) — V5
1. Lisez `jenkins/Jenkinsfile.tp1` : *Checkout*, *Compilation*, *Tests unitaires* (avec `junit`), *Package* (avec `archiveArtifacts`).
2. Créez le job `devops-ci` : **Pipeline from SCM**, **Script Path** `jenkins/Jenkinsfile.tp1`. Lancez.
3. **Travail d'équipe (binômes)** : Bob pousse une branche `feature/casse` avec un test rouge et ouvre une PR ; Alice, sans fusionner, observe qui a cassé le build et sur quelle ligne ; Bob corrige.
4. **Règle d'équipe à écrire au tableau** : « Une PR ne se fusionne que si le build est vert. »

```bash
git switch -c feature/casse
# changez une assertion dans app/src/test/java/tn/formation/devops/TaskServiceTest.java
git commit -am "Test cassé (exercice)" && git push -u origin feature/casse
```

**Pipeline obtenu** : `push → Jenkins → Checkout → mvn compile → mvn test (rapport JUnit) → package (JAR archivé)`.
**Résultat attendu** : build vert sur `main`, rouge sur la branche cassée, graphique de tendance des tests, JAR dans les artefacts.

**Pourquoi V5 ?** Avant : les tests dépendent de la discipline de chacun. Après : ils s'exécutent à chaque push. Preuve : historique des builds.

#### 9. Validation
1. Quel fichier décrit le pipeline et où est-il stocké ?
2. Pourquoi le stage *Tests* est-il avant *Package* ?
3. Comment retrouver quel test a échoué sans lire toute la console ?
4. Tâche : montrez un build rouge puis son retour au vert dans la *Stage View*.
5. Pourquoi le déclenchement par *polling* n'est-il qu'un pis-aller ? (latence, charge, et le webhook est instantané)

#### 10. Extension / challenge
- Remplacez le polling par un **webhook GitHub** (`smee.io` ou `ngrok` + déclencheur `githubPush()`).
- Ajoutez un stage qui publie un rapport de couverture JaCoCo.
- Transformez le job en **Multibranch Pipeline** pour construire automatiquement chaque branche et PR.
