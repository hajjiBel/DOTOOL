package tn.formation.devops;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de simulation de pannes, utilisés pour les TP ELK (exceptions Java,
 * erreurs HTTP 500) et Prometheus/Grafana (latence, taux d'erreur).
 */
@RestController
@RequestMapping("/api/simulate")
public class SimulationController {

    @GetMapping("/error")
    public Map<String, String> error() {
        throw new IllegalStateException("Erreur simulée pour la démonstration ELK / alerting");
    }

    @GetMapping("/slow")
    public Map<String, Long> slow(@RequestParam(defaultValue = "1000") long ms) throws InterruptedException {
        long bounded = Math.min(Math.max(ms, 0), 10_000);
        Thread.sleep(bounded);
        return Map.of("sleptMs", bounded);
    }
}
