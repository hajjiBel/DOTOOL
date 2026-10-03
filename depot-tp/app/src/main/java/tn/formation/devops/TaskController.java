package tn.formation.devops;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** API REST : /api/tasks, /api/info */
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService service;

    @Value("${app.version}")
    private String version;

    public TaskController(TaskService service) {
        this.service = service;
    }

    public record NewTask(String title) {
    }

    @GetMapping("/tasks")
    public List<Task> list() {
        return service.findAll();
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@RequestBody NewTask body) {
        return service.add(body.title());
    }

    @PutMapping("/tasks/{id}/toggle")
    public Task toggle(@PathVariable long id) {
        return service.toggle(id);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }

    @GetMapping("/info")
    public Map<String, String> info() throws Exception {
        return Map.of("application", "devops-demo",
                "version", version,
                "host", InetAddress.getLocalHost().getHostName());
    }
}
