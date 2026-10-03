package tn.formation.devops;

import java.net.InetAddress;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Interface web (Thymeleaf), cible des tests Selenium. */
@Controller
public class WebController {

    private final TaskService service;

    @Value("${app.version}")
    private String version;

    public WebController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("tasks", service.findAll());
        model.addAttribute("version", version);
        model.addAttribute("host", hostname());
        return "index";
    }

    @PostMapping("/tasks")
    public String add(@RequestParam String title) {
        try {
            service.add(title);
        } catch (IllegalArgumentException ignored) {
            // titre vide : on réaffiche simplement la page
        }
        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/toggle")
    public String toggle(@PathVariable long id) {
        service.toggle(id);
        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/delete")
    public String delete(@PathVariable long id) {
        service.delete(id);
        return "redirect:/";
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "inconnu";
        }
    }
}
