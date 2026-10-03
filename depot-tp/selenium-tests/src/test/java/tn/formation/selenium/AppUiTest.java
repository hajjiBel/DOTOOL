package tn.formation.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Tests fonctionnels de l'interface web.
 * Exécution : mvn test -Dapp.url=http://192.168.56.11:8080
 */
class AppUiTest {

    private static final String BASE_URL = System.getProperty("app.url", "http://localhost:8080");
    private static WebDriver driver;
    private static WebDriverWait wait;

    @BeforeAll
    static void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1280,800");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterAll
    static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void homePageDisplaysTitle() {
        driver.get(BASE_URL);
        String heading = driver.findElement(By.id("page-title")).getText();
        assertTrue(heading.contains("DevOps Demo"), "Titre inattendu : " + heading);
        assertTrue(driver.getTitle().contains("DevOps Demo"));
    }

    @Test
    void userCanAddAndCompleteATask() {
        String title = "Tâche Selenium " + UUID.randomUUID().toString().substring(0, 8);
        driver.get(BASE_URL);

        driver.findElement(By.id("title")).sendKeys(title);
        driver.findElement(By.id("add-btn")).click();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("task-list"), title));

        driver.findElement(By.xpath("//li[contains(., '" + title + "')]//button[@class='toggle-btn']")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//li[contains(@class,'done')][contains(., '" + title + "')]")));
    }

    @Test
    void userCanDeleteATask() {
        String title = "À supprimer " + UUID.randomUUID().toString().substring(0, 8);
        driver.get(BASE_URL);
        driver.findElement(By.id("title")).sendKeys(title);
        driver.findElement(By.id("add-btn")).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("task-list"), title));

        driver.findElement(By.xpath("//li[contains(., '" + title + "')]//button[@class='delete-btn']")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.xpath("//li[contains(., '" + title + "')]")));
    }

    @Test
    void healthEndpointIsUp() {
        driver.get(BASE_URL + "/actuator/health");
        assertTrue(driver.getPageSource().contains("UP"));
    }

    @Test
    void apiInfoReturnsApplicationName() {
        driver.get(BASE_URL + "/api/info");
        assertTrue(driver.getPageSource().contains("devops-demo"));
        assertEquals(BASE_URL + "/api/info", driver.getCurrentUrl());
    }
}
