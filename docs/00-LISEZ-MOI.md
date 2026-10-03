# Formation DevOps — 3 jours, 100 % projet fil rouge

Dossier formateur construit à partir de votre archive `devops-formation.zip` (2 jours, 8 TP). Il la transforme en formation de **3 jours (21 h)** : séquences *concept → activité simple → mini-TP → projet fil rouge → validation*, 11 TP et un projet final.

## Ce que contient ce dossier

| Fichier | Contenu |
|---|---|
| `01-programme-et-planning.md` | Programme, planning minute par minute (théorie / activité / mini-TP / fil rouge / validation / pauses) |
| `02-projet-fil-rouge.md` | Les 13 versions du projet : ce qui change et pourquoi, chaîne CI/CD en 10 étapes, scénarios de tests |
| `03-environnement-lab.md` | Laboratoire VirtualBox/Vagrant, VM actives par séance, plan B si peu de RAM, aide-mémoire Linux |
| `tp/TP01 … TP11` | Les 11 fiches TP (10 rubriques imposées + script formateur + corrigé) |
| `04-scenarios-projet-final-evaluation.md` | Scénario d'entreprise, projet final, QCM, grille d'évaluation /20 |
| `05-catalogue-modules-optionnels.md` | Les séries complètes demandées (Git 8 TP, Docker 8, Ansible 10, K8s 10, ELK 8, monitoring 7…) avec leur statut dans les 3 jours et ce qu'il faut ajouter pour une version longue |
| `../depot-tp/` | Dépôt de TP : code de l'application, corrigés (Jenkinsfile, Dockerfile, Ansible, K8s, ELK, Prometheus), **plus les nouveaux fichiers** (dossier `mini-tp/`, `Jenkinsfile.final`, `Jenkinsfile.rollback`, `k8s/configmap.yaml`, `k8s/secret.yaml`, `ansible/mini-tp/`) |

## Choix à connaître avant de vous lancer

1. **21 h ne permettent pas de faire toutes les séries de TP du cahier des charges.** Les séries complètes (Ansible 10 TP, Kubernetes 10 TP, ELK 8 TP…) représentent plusieurs dizaines d'heures. Plutôt que de survoler tout, j'ai :
   - gardé **un TP par grand concept** (11 TP), où chaque TP regroupe plusieurs étapes de la série (par exemple le TP Docker couvre les « TP Docker 1 à 4 ») ;
   - rangé les autres étapes dans le **catalogue** (`05`), avec objectif, statut et durée de la version longue, pour que vous puissiez rallonger la formation sans repartir de zéro.
2. **Temps de théorie très court** (≈ 11 % du temps, détail dans le planning), bien en dessous de votre plafond de 30-40 %. Le surplus est de la pratique guidée. Si vous préférez plus de cours, allongez les blocs « théorie » de 5 min par TP.
3. **Base de données** : l'application de l'archive stocke les tâches **en mémoire** (pas de PostgreSQL). Je n'ai pas modifié le code Java : la base est travaillée en mini-TP (PostgreSQL + Adminer, TP05) et proposée en challenge pour le projet fil rouge. Dites-moi si vous voulez que j'ajoute JPA/PostgreSQL à l'application et aux tests.
4. **Scénarios ELK** : l'application ne produit que des erreurs 500, 400 et 404 et des exceptions Java. « Utilisateur inconnu » devient « tâche inconnue » (404). « Connexion à la base » est traité avec les événements de démonstration du mini-TP. « Temps de réponse élevé » n'est volontairement **pas** visible dans les logs : c'est la transition vers le TP10 (métriques).
5. **Rien de ce qui est fourni n'a pu être exécuté** : je n'ai ni VirtualBox, ni Maven, ni Docker dans mon environnement (seule la lecture de votre archive a été vérifiée). Les nouveaux fichiers sont écrits avec soin mais **à valider par un `vagrant up` complet et un déroulé de chaque TP avant la session**. Les points les plus à risque sont : les `Jenkinsfile` (échappement des guillemets dans `grep`), le test Selenium du mini-TP et l'accès de k3s au registre privé.

## Comment utiliser les fiches

Chaque fiche suit les 10 rubriques demandées. Les encadrés **🎤 Formateur** donnent ce qu'il faut dire et ce qu'il faut observer ; les niveaux sont marqués **[N1 Découverte]**, **[N2 Application]**, **[N3 Intégration]**. Les corrigés sont dans la rubrique « Solution » du mini-TP et dans le dépôt.

> Mode « exercice » : avant distribution, retirez du dépôt les corrigés (`jenkins/`, `Dockerfile`, `ansible/*.yml`, `k8s/`, `monitoring/`, `elk/`, `solutions/`) comme l'indique le README d'origine, et laissez les stagiaires les écrire à partir des fiches.
