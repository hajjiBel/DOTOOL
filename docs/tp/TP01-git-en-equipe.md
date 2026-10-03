# TP 1 — Git : travailler à plusieurs sans s'écraser (V1 → V2)

**Durée : 90 min** · théorie 10 · activité 10 · mini-TP 25 · fil rouge 35 · validation 10 · **VM** : `ci` · **Format** : binômes

#### 1. Objectif pédagogique
À la fin du TP, le participant sait : créer un dépôt, enregistrer des modifications (commit), travailler sur une branche, fusionner, résoudre un conflit, puis collaborer via GitHub avec une Pull Request. Il comprend **pourquoi** un historique partagé remplace l'envoi de fichiers.

#### 2. Prérequis
Savoir ouvrir un terminal et éditer un fichier texte. Un compte GitHub et un *Personal Access Token*.

#### 3. Concept DevOps abordé
**Gestion de version = source de vérité unique.** Tout (code, pipeline, infrastructure) vit dans Git. Chaque modification est tracée, réversible et relue avant d'être intégrée. C'est le point de départ de la chaîne : sans Git, pas d'automatisation fiable.

#### 4. Problème réel à résoudre
« Deux développeurs corrigent le même fichier en même temps. L'un envoie sa version par mail, l'autre la copie sur un partage. Le lundi, personne ne sait quelle version est la bonne, et une correction a disparu. Comment travailler à plusieurs sur le même code ? »

#### 5. Activité pédagogique de découverte **[N1 Découverte]** (10 min)
**Sans Git, simulez l'équipe.** Chaque binôme fait ceci :

```bash
mkdir -p ~/decouverte && cd ~/decouverte && mkdir alice bob
printf 'Titre: Omelette\nIngredients: oeufs\nCuisson: 3 min\n' > alice/recette.txt
cp alice/recette.txt bob/recette.txt
sed -i 's/^Cuisson.*/Cuisson: 2 min/' alice/recette.txt      # Alice modifie
sed -i 's/^Cuisson.*/Cuisson: 4 min/' bob/recette.txt        # Bob modifie la même ligne
diff -u alice/recette.txt bob/recette.txt
```

**Questions** : qui a raison ? Si Bob copie son fichier sur celui d'Alice, que perd-on ? Comment saurait-on *qui* a changé *quoi* et *quand* ?

> 🎤 **Formateur** : laissez 3 minutes de débat, puis notez au tableau les besoins exprimés (« historique », « fusion », « qui/quand/pourquoi », « revenir en arrière »). Annoncez : « Git répond à chacun de ces besoins. »

#### 6. Mini-TP **[N2 Application]** (25 min) — « Le dépôt de recettes »
- **Contexte** : un dépôt Git local, sans lien avec le projet fil rouge.
- **Objectif** : enchaîner commit → branche → conflit → fusion.
- **Architecture** : un dossier local, un seul fichier.
- **Fichiers à créer** : `recette.txt` (3 lignes).

**Étapes et commandes**

```bash
git config --global user.name  "Votre Nom"
git config --global user.email "vous@exemple.fr"

mkdir -p ~/tp-git && cd ~/tp-git
git init -b main
printf 'Titre: Omelette\nIngredients: oeufs\nCuisson: 3 min\n' > recette.txt
git status                       # fichier non suivi
git add recette.txt
git commit -m "Recette initiale"

git switch -c feature/cuisson-courte
sed -i 's/^Cuisson.*/Cuisson: 2 min/' recette.txt
git commit -am "Cuisson plus courte"

git switch main
sed -i 's/^Cuisson.*/Cuisson: 4 min/' recette.txt
git commit -am "Cuisson plus longue"

git merge feature/cuisson-courte       # CONFLIT
git status
cat recette.txt                        # marqueurs <<<<<<< ======= >>>>>>>
```

Résolvez le conflit : éditez `recette.txt` pour ne garder qu'une ligne `Cuisson: 3 min`, puis :

```bash
git add recette.txt
git commit -m "Fusion : cuisson 3 min"
git log --oneline --graph --all
```

**Résultat attendu** : un graphe avec deux branches qui se rejoignent ; `recette.txt` contient `Cuisson: 3 min` sans marqueurs.

**Erreurs fréquentes**
- Oublier `git add` avant `git commit` : le commit est vide ou incomplet.
- Laisser les marqueurs `<<<<<<<` dans le fichier avant de valider.
- `git commit` sans `-m` : un éditeur s'ouvre (`Ctrl+X` pour quitter nano).
- `fatal: unable to auto-detect email address` : refaire les `git config`.

**Solution** : voir ci-dessus ; le fichier final doit être exactement :
```
Titre: Omelette
Ingredients: oeufs
Cuisson: 3 min
```

#### 7. Retour pédagogique
- **Appris** : un commit est une photographie datée et signée ; une branche isole un travail ; un conflit n'est pas une erreur mais une question posée à l'humain.
- **Pourquoi en DevOps** : Jenkins, Ansible, Kubernetes lisent tous leur configuration dans Git ; le dépôt est le déclencheur de la chaîne.
- **Problème résolu** : traçabilité, travail parallèle, retour arrière.
- **Limites** : Git ne remplace pas la communication (conflits fréquents = équipe qui ne se parle pas) ; les gros fichiers binaires et les secrets n'y ont pas leur place.

#### 8. Retour au projet fil rouge **[N3 Intégration]** (35 min) — V1 puis V2
**V1 (démonstration formateur, 5 min)** : montrer l'application qui tourne localement sur la VM `ci` pour définir le point de départ.
```bash
cd ~/devops-formation/app
mvn -B -q -DskipTests package
java -jar target/devops-demo.jar --server.port=9090 &
curl -s http://localhost:9090/api/info ; kill %1
```

**V2 (participants, 30 min)** — simulation d'une équipe de deux développeurs :

1. **Alice** crée un dépôt GitHub vide `devops-formation` et y ajoute **Bob** comme collaborateur.
2. Sur la VM d'Alice (`~/devops-formation`) :
   ```bash
   git init -b main              # ignorer si c'est déjà un dépôt
   cat .gitignore                # target/, .shared/ ... : pourquoi ?
   git add . && git commit -m "V1 : application initiale"
   git remote add origin https://github.com/<alice>/devops-formation.git
   git push -u origin main       # mot de passe = Personal Access Token
   ```
3. **Bob** clone (`git clone https://github.com/<alice>/devops-formation.git`).
4. Chacun crée sa branche et modifie **un fichier différent** :
   - Alice : `git switch -c feature/titre` → change le titre `<h1>` dans `app/src/main/resources/templates/index.html`.
   - Bob : `git switch -c feature/doc` → ajoute une ligne dans `README.md`.
5. Chacun pousse sa branche (`git push -u origin feature/...`) et ouvre une **Pull Request** vers `main` sur GitHub.
6. Relecture croisée : chacun commente puis approuve la PR de l'autre ; Alice fusionne les deux PR.
7. Chacun fait `git switch main && git pull` et vérifie `git log --oneline --graph`.

**Résultat attendu** : `main` contient les deux modifications ; l'historique montre deux branches fusionnées ; aucune manipulation de fichiers par mail.

> 🎤 **Formateur** : insistez sur la règle de l'équipe — *on ne pousse jamais directement sur `main`*. Dans les paramètres GitHub (Branches), activez « Require a pull request » si le temps le permet. Cette règle servira au TP03 : Jenkins vérifiera chaque branche.

**Pourquoi V2 ?** Avant : un dossier qui circule. Après : un historique partagé et relu. Preuve : l'URL du dépôt et le graphe `git log`.

#### 9. Validation
1. Quelle commande montre l'historique en graphe ? (`git log --oneline --graph --all`)
2. Quelle différence entre `git merge` et `git push` ?
3. Pourquoi `target/` est-il dans `.gitignore` ?
4. Tâche : montrez que votre branche `feature/...` est bien fusionnée dans `main`.
5. Que fait Git quand deux branches modifient la même ligne ?

#### 10. Extension / challenge
- Créez un tag `v1.0.0` sur `main` et poussez-le (`git tag v1.0.0 && git push --tags`).
- Annulez proprement un commit déjà poussé avec `git revert`, sans réécrire l'historique.
- Réécrivez l'historique d'une branche locale avec `git rebase main` et comparez le graphe avec un `merge`.
