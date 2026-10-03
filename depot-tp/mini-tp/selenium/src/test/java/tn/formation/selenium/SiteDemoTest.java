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
