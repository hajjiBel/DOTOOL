package tn.formation.devops;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public List<Task> findAll() {
        return tasks.values().stream().sorted(Comparator.comparingLong(Task::id)).toList();
    }

    public Task add(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Le titre ne doit pas être vide");
        }
        Task task = new Task(sequence.incrementAndGet(), title.trim(), false);
        tasks.put(task.id(), task);
        log.info("Tâche créée id={} titre='{}'", task.id(), task.title());
        return task;
    }

    public Task toggle(long id) {
        Task updated = tasks.computeIfPresent(id, (k, t) -> new Task(k, t.title(), !t.done()));
        if (updated == null) {
            throw new NoSuchElementException("Tâche introuvable : " + id);
        }
        log.info("Tâche modifiée id={} done={}", updated.id(), updated.done());
        return updated;
    }

    public void delete(long id) {
        if (tasks.remove(id) == null) {
            throw new NoSuchElementException("Tâche introuvable : " + id);
        }
        log.info("Tâche supprimée id={}", id);
    }
}
