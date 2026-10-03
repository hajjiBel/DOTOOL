# TP 2 — Maven et tests automatisés (V3 → V4)

**Durée : 75 min** · théorie 10 · activité 15 · mini-TP 20 · fil rouge 20 · validation 10 · **VM** : `ci` · **Format** : individuel

#### 1. Objectif pédagogique
Comprendre qu'un build **reproductible** (Maven) et des **tests exécutables par une machine** sont les deux conditions préalables à toute intégration continue. Savoir lancer `mvn test`, lire un rapport, et écrire un test.

#### 2. Prérequis
TP01 (dépôt Git de l'application). Lire quelques lignes de Java.

#### 3. Concept DevOps abordé
**Build reproductible + feedback rapide.** Le `pom.xml` décrit dépendances et étapes (compiler, tester, empaqueter) ; la commande est la même sur le poste, sur Jenkins et chez le voisin. Les tests transforment « je crois que ça marche » en « une machine me le prouve en quelques secondes ». La pyramide : beaucoup de tests unitaires (rapides), quelques tests d'intégration, peu de tests d'interface.

#### 4. Problème réel à résoudre
« Une application évolue : 5 fonctionnalités aujourd'hui, 50 dans six mois. À chaque livraison, quelqu'un doit tout retester à la main. Les régressions passent entre les mailles. Comment vérifier tout l'existant en quelques secondes ? »

#### 5. Activité pédagogique de découverte **[N1 Découverte]** (15 min) — « Testons à la main »
Sur `ci`, construisez l'application (boîte noire pour l'instant) et lancez-la :
```bash
cd ~/devops-formation/app && mvn -B -q -DskipTests package
java -jar target/devops-demo.jar --server.port=9090 > /tmp/app.log 2>&1 &
sleep 20
```
Chronomètre en main, **exécutez cette check-list manuelle** et cochez ce qui est conforme :

| # | Action | Attendu |
|---|---|---|
| 1 | `curl -s -o /dev/null -w '%{http_code}\n' localhost:9090/api/tasks` | 200 |
| 2 | `curl -s -XPOST -H 'Content-Type: application/json' -d '{"title":"A"}' localhost:9090/api/tasks` | 201 |
| 3 | idem avec `{"title":""}` | 400 |
| 4 | `curl -s -XPUT localhost:9090/api/tasks/1/toggle` | `done: true` |
| 5 | `curl -s -XDELETE -o /dev/null -w '%{http_code}\n' localhost:9090/api/tasks/9999` | 404 |
| 6 | `curl -s localhost:9090/api/simulate/error -o /dev/null -w '%{http_code}\n'` | 500 |

```bash
kill %1
```

**Questions** : combien de temps pour 6 vérifications ? Pour 60 ? Combien de fois par semaine ? Qui le fait quand le développeur est en congés ?

> 🎤 **Formateur** : notez le temps du plus rapide (≈ 3 min). Extrapolez au tableau : 60 cas × 20 livraisons = plusieurs jours par mois. Conclusion : « ce qu'on fait toujours pareil, une machine doit le faire. »

#### 6. Mini-TP **[N2 Application]** (20 min) — « La calculatrice »
- **Contexte** : mini-projet Maven autonome `mini-tp/calculatrice` (calcul de prix TTC).
- **Objectif** : lancer, casser, réparer un test.
- **Architecture** : 1 classe, 1 classe de test, 1 `pom.xml`.
- **Fichiers** (fournis dans `depot-tp/mini-tp/calculatrice/`) :

`pom.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>tn.formation</groupId>
  <artifactId>calculatrice</artifactId>
  <version>1.0</version>
  <packaging>jar</packaging>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>5.10.3</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.2.5</version>
      </plugin>
    </plugins>
  </build>
</project>
```

`src/main/java/tn/formation/calc/Calculator.java`
```java
package tn.formation.calc;

public class Calculator {

    /** Prix TTC arrondi au centime. taux = 0.19 pour 19 %. */
    public double prixTtc(double ht, double taux) {
        if (ht < 0) {
            throw new IllegalArgumentException("Le prix HT doit être positif");
        }
        return Math.round(ht * (1 + taux) * 100.0) / 100.0;
    }

    public double diviser(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Division par zéro");
        }
        return a / b;
    }
}
```

`src/test/java/tn/formation/calc/CalculatorTest.java`
```java
package tn.formation.calc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CalculatorTest {

    private final Calculator calc = new Calculator();

    @Test
    void ttcSimple() {
        assertEquals(119.0, calc.prixTtc(100, 0.19));
    }

    @Test
    void ttcArrondiAuCentime() {
        assertEquals(11.89, calc.prixTtc(9.99, 0.19));
    }

    @Test
    void ttcRefuseUnPrixNegatif() {
        assertThrows(IllegalArgumentException.class, () -> calc.prixTtc(-1, 0.19));
    }

    @Test
    void divisionNormale() {
        assertEquals(2.5, calc.diviser(5, 2));
    }

    @Test
    void divisionParZero() {
        assertThrows(ArithmeticException.class, () -> calc.diviser(1, 0));
    }
}
```

**Étapes et commandes**
```bash
cd ~/devops-formation/mini-tp/calculatrice
mvn -B test                       # 5 tests verts (le 1er lancement télécharge les dépendances)
ls target/surefire-reports/       # les rapports

# 1) Cassez le code : remplacez Math.round(...) par une troncature
sed -i 's|Math.round(ht \* (1 + taux) \* 100.0) / 100.0|((int) (ht * (1 + taux) * 100)) / 100.0|' src/main/java/tn/formation/calc/Calculator.java
mvn -B test                       # ROUGE : ttcArrondiAuCentime échoue (11.88 au lieu de 11.89)

# 2) Réparez
git checkout src/main/java/tn/formation/calc/Calculator.java
mvn -B test                       # VERT

# 3) Produisez l'artefact
mvn -B -DskipTests package && ls target/*.jar
```

**Résultat attendu** : `Tests run: 5, Failures: 0` ; après la casse, au moins un échec, dont `ttcArrondiAuCentime` (`expected: <11.89> but was: <11.88>`) ; d'autres cas peuvent aussi échouer à cause des arrondis flottants, ce qui est une bonne discussion ; le JAR `calculatrice-1.0.jar` existe.

**Erreurs fréquentes** : lancer `mvn` hors du dossier du `pom.xml` (« no POM in this directory ») ; `git checkout` qui échoue si le dépôt n'a pas été commité au TP01 (utilisez alors la version du formateur) ; réseau lent au premier lancement.

**À faire ensuite** : ajoutez un test `ttcAvecTauxZero` qui vérifie que `prixTtc(50, 0)` vaut `50.0`.

**Solution** : 
```java
@Test
void ttcAvecTauxZero() {
    assertEquals(50.0, calc.prixTtc(50, 0));
}
```

#### 7. Retour pédagogique
- **Appris** : `mvn test` = compiler + exécuter tous les tests ; un test rouge localise précisément la régression ; le `pom.xml` rend le build identique pour tous.
- **Pourquoi en DevOps** : Jenkins n'a pas d'IDE ; il ne sait qu'exécuter une commande. Sans commande de build et de test déterministe, pas d'intégration continue.
- **Problème résolu** : recette manuelle coûteuse et non reproductible.
- **Limites** : des tests verts ne prouvent pas l'absence de bugs ; des tests mal écrits donnent une fausse confiance ; les tests lents découragent de les lancer.

#### 8. Retour au projet fil rouge **[N3 Intégration]** (20 min) — V3 puis V4
1. **V3 – build** : ouvrez `app/pom.xml` : parent Spring Boot 3.3.5, `java.version` 21, starters (web, actuator, test), plugin Spring Boot. Lancez :
   ```bash
   cd ~/devops-formation/app
   mvn -B clean package -DskipTests && ls -lh target/devops-demo.jar
   ```
2. **V4 – tests** : exécutez les 10 tests existants (4 unitaires `TaskServiceTest`, 6 d'intégration `TaskControllerTest`) :
   ```bash
   mvn -B clean verify
   grep -h "Tests run" target/surefire-reports/*.txt
   ```
3. **Ajoutez un test unitaire** dans `TaskServiceTest` :
   ```java
   @Test
   void toggleUnknownIdThrows() {
       assertThrows(java.util.NoSuchElementException.class, () -> service.toggle(9999));
   }
   ```
   puis `mvn -B test`.
4. Commitez sur une branche et poussez : `git switch -c feature/test-toggle && git commit -am "Test toggle inconnu" && git push -u origin feature/test-toggle`.

**Résultat attendu** : `BUILD SUCCESS`, 11 tests, un JAR `devops-demo.jar` ; vous reliez chaque ligne de la check-list manuelle de l'activité à un test automatique.

**Pourquoi V3/V4 ?** Avant : on compile dans l'IDE, on vérifie à la main. Après : une commande unique construit et prouve. Preuve : rapports Surefire.

#### 9. Validation
1. Quelle commande compile, teste et empaquette ?
2. Où trouver la liste des dépendances d'un projet Maven ?
3. Différence entre test unitaire (`TaskServiceTest`) et test d'intégration (`TaskControllerTest`) ?
4. Tâche : cassez volontairement un test de l'application et montrez le message d'erreur Maven.
5. Quel test automatique remplace la ligne 5 de la check-list manuelle ? (`unknownTaskReturns404`)

#### 10. Extension / challenge
- Ajoutez le test d'intégration « `PUT /api/tasks/9999/toggle` renvoie 404 ».
- Ajoutez le plugin JaCoCo et lisez le rapport de couverture (`target/site/jacoco/index.html`).
