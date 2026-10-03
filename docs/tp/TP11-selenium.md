# TP 11 — Tests fonctionnels Selenium et barrière qualité (V13)

**Durée : 45 min** · théorie 5 · activité 5 · mini-TP 10 · fil rouge 20 · validation 5 · **VM** : `ci`, `target` · **Format** : individuel
*(Si le groupe a du retard : mini-TP en démonstration formateur, fil rouge conservé. Progression test manuel → unitaire → intégration → API → fonctionnel, vue dans les TP02, TP07 et ici.)*

#### 1. Objectif pédagogique
Comprendre qu'un test fonctionnel simule un utilisateur dans un navigateur ; l'intégrer au pipeline pour **bloquer la promotion** d'une version qui casse l'interface.

#### 2. Prérequis
TP07 (pipeline `Jenkinsfile.final`, application déployée sur `target`).

#### 3. Concept DevOps abordé
**Qualité comme porte de la livraison.** Unitaires et d'intégration vérifient le code ; le test fonctionnel vérifie **ce que voit l'utilisateur**, sur l'application réellement déployée. Il est plus lent et plus fragile : on en écrit peu, sur les parcours critiques.

#### 4. Problème réel à résoudre
« Un développeur renomme un champ de l'interface. Les tests unitaires passent, l'image se construit, le déploiement réussit… mais la page n'a plus de bouton qui fonctionne. L'utilisateur le découvre avant l'équipe. »

#### 5. Activité pédagogique de découverte **[N1 Découverte]** (5 min)
Ouvrez http://192.168.56.11:8080 et, **à la main**, vérifiez : le titre s'affiche, ajout d'une tâche, terminer la tâche, supprimer la tâche (4 parcours). Chronométrez. Multipliez par 20 livraisons par semaine et 3 navigateurs.

> 🎤 **Formateur** : « Répétitif, ennuyeux, donc oublié. Selenium sait faire ces 4 parcours pour nous. »

#### 6. Mini-TP **[N2 Application]** (10 min) — « Un robot devant le site »
- **Contexte** : le site `site-demo` (TP04), sans l'application.
- **Objectif** : exécuter un test Selenium puis le faire échouer par une régression.
- **Fichier** `mini-tp/selenium/src/test/java/tn/formation/selenium/SiteDemoTest.java` :

```java
package tn.formation.selenium;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/** Mini-TP Selenium : mvn test -Dsite.url=http://localhost:8089 */
class SiteDemoTest {

    private static final String URL = System.getProperty("site.url", "http://localhost:8089");
    private static WebDriver driver;

    @BeforeAll
    static void setUp() {
        ChromeOptions o = new ChromeOptions();
        o.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(o);
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) driver.quit();
    }

    @Test
    void lePageAUnTitre() {
        driver.get(URL);
        assertTrue(driver.findElement(By.id("titre")).getText().contains("Site démo"));
    }

    @Test
    void leBoutonAfficheUnMessage() {
        driver.get(URL);
        driver.findElement(By.id("btn")).click();
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(
                ExpectedConditions.textToBe(By.id("message"), "Bonjour DevOps !"));
    }
}
```

**Étapes**
```bash
docker rm -f site 2>/dev/null; docker run -d --name site -p 8089:80 site-demo:2   # ou site-demo:1
cd ~/devops-formation/mini-tp/selenium
mvn -B test -Dsite.url=http://localhost:8089                 # 2 tests verts

# Régression : le développeur renomme l'identifiant du bouton
cd ../site-demo && sed -i 's/id="btn"/id="bouton"/' index.html
docker build -t site-demo:3 . && docker rm -f site && docker run -d --name site -p 8089:80 site-demo:3
cd ../selenium && mvn -B test -Dsite.url=http://localhost:8089   # ROUGE : NoSuchElementException (id btn)
# Restaurer : git checkout ../site-demo/index.html
```
**Résultat attendu** : 2 tests verts, puis `leBoutonAfficheUnMessage` en erreur (`no such element: Unable to locate element: {"method":"css selector","selector":"[id="btn"]"}`).

**Erreurs fréquentes** : `session not created` (Chrome absent ou driver non téléchargé : `google-chrome --version`, accès Internet) ; mauvais port ; ancien conteneur toujours actif.

**Solution** : le test est fourni ; l'échec prouve que le test détecte la régression.

#### 7. Retour pédagogique
- **Appris** : localisateurs (`By.id`), attentes explicites (`WebDriverWait`), mode *headless*.
- **Pourquoi en DevOps** : dernière barrière avant la promotion ; l'automatisation protège contre les régressions d'interface.
- **Problème résolu** : régressions visibles seulement par l'utilisateur.
- **Limites** : lent, fragile (un identifiant modifié casse tout), coûteux à maintenir ; ne remplace pas les tests unitaires (pyramide des tests).

#### 8. Retour au projet fil rouge **[N3 Intégration]** (20 min) — V13
1. Lisez `selenium-tests/src/test/java/tn/formation/selenium/AppUiTest.java` : 5 tests (titre, ajout + terminaison, suppression, santé, `/api/info`).
2. Exécutez contre l'environnement de test :
   ```bash
   cd ~/devops-formation/selenium-tests
   mvn -B test -Dapp.url=http://192.168.56.11:8080
   ```
3. **Écrivez un test** : « ajouter une tâche vide ne crée aucune ligne » (compter les `<li>` de `#task-list` avant/après).
4. **Dans le pipeline** : lancez `devops-final` avec **`RUN_SELENIUM` coché** : le stage *9b* s'exécute après le smoke test et **avant** la promotion `latest`/`stable`.
5. **Démonstrez la barrière qualité** :
   - Dans `app/src/main/resources/templates/index.html`, renommez `id="page-title"` en `id="titre"`, poussez.
   - Le pipeline échoue à *9b* ; le stage *Promotion* n'est **pas** exécuté ; `stable` ne bouge pas :
     `curl -s http://192.168.56.10:5000/v2/devops-demo/tags/list`
   - Corrigez : le pipeline repasse au vert et `stable` avance.

**Pipeline obtenu** : `… → Déploiement → Smoke test → Selenium → Promotion (latest/stable)`.
**Résultat attendu** : une régression d'interface bloque la promotion ; la correction la débloque.

**Pourquoi V13 ?** Avant : une régression visuelle arrive jusqu'à `stable`. Après : elle est arrêtée par la machine. Preuve : tag `stable` inchangé après un build rouge.

#### 9. Validation
1. Quelle différence entre un test d'intégration (MockMvc) et un test Selenium ?
2. Pourquoi Selenium s'exécute-t-il **après** le déploiement ?
3. À quoi sert `WebDriverWait` ?
4. Tâche : montrez qu'un build rouge Selenium n'a pas déplacé le tag `stable`.

#### 10. Extension / challenge
- Capturez une copie d'écran en cas d'échec (`TakesScreenshot`) et archivez-la dans Jenkins.
- Exécutez les tests sur plusieurs navigateurs (Selenium Grid) ou en parallèle.
