package tn.formation.devops;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskServiceTest {

    private TaskService service;

    @BeforeEach
    void init() {
        service = new TaskService();
    }

    @Test
    void addCreatesAnOpenTask() {
        Task t = service.add("Écrire un Jenkinsfile");
        assertEquals("Écrire un Jenkinsfile", t.title());
        assertFalse(t.done());
        assertEquals(2, service.findAll().size());
    }

    @Test
    void addRejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class, () -> service.add("   "));
        assertThrows(IllegalArgumentException.class, () -> service.add(null));
    }

    @Test
    void toggleInvertsDoneFlag() {
        Task t = service.add("Tester");
        assertTrue(service.toggle(t.id()).done());
        assertFalse(service.toggle(t.id()).done());
    }

    @Test
    void deleteRemovesTask() {
        Task t = service.add("Supprimer moi");
        service.delete(t.id());
        assertTrue(service.findAll().isEmpty());
        assertThrows(NoSuchElementException.class, () -> service.delete(t.id()));
    }


    @Test
void toggleUnknownIdThrows() {
    assertThrows(java.util.NoSuchElementException.class, () -> service.toggle(9999));
}
}
