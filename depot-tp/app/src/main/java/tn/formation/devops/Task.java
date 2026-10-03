package tn.formation.devops;

/** Une tâche du gestionnaire de tâches (objet immuable). */
public record Task(long id, String title, boolean done) {
}
