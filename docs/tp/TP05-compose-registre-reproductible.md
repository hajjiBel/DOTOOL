# TP 5 — Docker Compose, registre et environnement reproductible (V7)

**Durée : 60 min** · théorie 8 · activité 10 · mini-TP 20 · fil rouge 15 · validation 7 · **VM** : `ci`, `target` · **Format** : individuel
*(Couvre les « TP Docker 5 à 7 » : plusieurs services, base de données, environnement reproductible.)*

#### 1. Objectif pédagogique
Décrire un ensemble de services dans un fichier (`docker-compose.yml`), comprendre la **persistance** (volumes), publier une image dans un **registre** et la redéployer sur un autre serveur.

#### 2. Prérequis
TP04 (image, conteneur, Dockerfile).

#### 3. Concept DevOps abordé
**Infrastructure décrite en code et artefact versionné.** Compose décrit plusieurs services et leurs liens dans un fichier relu et versionné ; le registre stocke les images par tag, comme un dépôt Git pour les artefacts.

#### 4. Problème réel à résoudre
« Pour démarrer l'application et sa base de données, un nouveau collègue doit suivre une page de 12 commandes que personne ne tient à jour. Et l'image construite sur le poste du développeur n'existe nulle part ailleurs. »

#### 5. Activité pédagogique de découverte **[N1 Découverte]** (10 min)
Démarrez **à la main** deux conteneurs qui doivent se parler :
```bash
docker network create demo-net
docker run -d --name pg --network demo-net -e POSTGRES_USER=demo -e POSTGRES_PASSWORD=demo-pwd -e POSTGRES_DB=demo postgres:16-alpine
docker run -d --name adm --network demo-net -p 8081:8080 adminer:4
docker ps
```
Puis, **sans rien noter**, arrêtez tout : `docker rm -f adm pg && docker network rm demo-net`.

**Questions** : combien de commandes, d'options à retenir ? Que se passe-t-il si un collègue oublie `--network` ? Comment partager cette recette ?

> 🎤 **Formateur** : comptez les options au tableau (≈ 12). « Et la prod, c'est 10 services. Il nous faut un fichier. »

#### 6. Mini-TP **[N2 Application]** (20 min) — « Base de données + interface d'administration »
- **Contexte** : PostgreSQL et Adminer, sans lien avec l'application.
- **Objectif** : décrire 2 services, prouver la persistance.
- **Architecture** : `db` (volume `db_data`) ← `adminer` (port 8081).
- **Fichier** `mini-tp/compose-db/docker-compose.yml` :

```yaml
# Mini-TP : plusieurs services + persistance
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: demo
      POSTGRES_PASSWORD: demo-pwd
      POSTGRES_DB: demo
    volumes:
      - db_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U demo -d demo"]
      interval: 5s
      retries: 10

  adminer:
    image: adminer:4
    ports:
      - "8081:8080"      # 8080 est déjà pris par Jenkins sur la VM ci
    depends_on:
      db:
        condition: service_healthy

volumes:
  db_data:
```

**Étapes**
```bash
cd ~/devops-formation/mini-tp/compose-db
docker compose up -d
docker compose ps                      # db: healthy, adminer: running
docker compose exec db psql -U demo -d demo -c "CREATE TABLE notes(id serial, txt text); INSERT INTO notes(txt) VALUES ('bonjour');"
docker compose exec db psql -U demo -d demo -c "SELECT * FROM notes;"

docker compose down                    # supprime les conteneurs, PAS le volume
docker compose up -d
docker compose exec db psql -U demo -d demo -c "SELECT * FROM notes;"   # la ligne est toujours là

docker compose down -v                 # supprime aussi le volume
docker compose up -d
docker compose exec db psql -U demo -d demo -c "SELECT * FROM notes;"   # ERREUR : table inexistante
docker compose down -v
```
Ouvrez aussi http://192.168.56.10:8081 (Système : PostgreSQL, Serveur : `db`, utilisateur `demo`, mot de passe `demo-pwd`).

**Résultat attendu** : la ligne `bonjour` survit à `down` et disparaît avec `down -v`.

**Erreurs fréquentes** : `docker-compose` (ancienne syntaxe) au lieu de `docker compose` ; indentation YAML (espaces, pas de tabulations) ; se connecter à Adminer avec le serveur `localhost` au lieu du nom de service `db`.

**Solution** : le fichier fourni ; le service `db` n'est joignable depuis Adminer que par son **nom de service**.

#### 7. Retour pédagogique
- **Appris** : Compose crée un réseau commun et résout les noms de service ; un volume sépare la vie des données de celle des conteneurs ; `healthcheck` + `depends_on` ordonnent le démarrage.
- **Pourquoi en DevOps** : « démarrer l'environnement » devient une seule commande, identique pour tous.
- **Problème résolu** : documentation obsolète, environnements différents d'un poste à l'autre.
- **Limites** : Compose pilote **un seul hôte**, sans reprise automatique entre machines ni mise à jour progressive (voir TP08).

#### 8. Retour au projet fil rouge **[N3 Intégration]** (15 min) — V7
1. **Publier l'image** dans le registre privé de la VM `ci` (déjà configuré en *insecure registry*) :
   ```bash
   cd ~/devops-formation/app
   docker tag devops-demo:1.0 192.168.56.10:5000/devops-demo:1.0
   docker push 192.168.56.10:5000/devops-demo:1.0
   curl -s http://192.168.56.10:5000/v2/_catalog
   curl -s http://192.168.56.10:5000/v2/devops-demo/tags/list
   ```
2. **Déployer sur un autre serveur** (`target`) à partir du registre, sans construire quoi que ce soit :
   ```bash
   ssh vagrant@192.168.56.11
   cd /vagrant/app && cat docker-compose.yml
   TAG=1.0 docker compose up -d
   docker compose ps
   exit
   curl -s http://192.168.56.11:8080/api/info
   ```
3. **Reproductibilité** : sur `target`, `docker compose down` puis `TAG=1.0 docker compose up -d` : même résultat.
4. **Nettoyage avant le TP06** : `ssh vagrant@192.168.56.11 "cd /vagrant/app && docker compose down"` (le port 8080 doit être libre pour Ansible).

**Résultat attendu** : le tag `1.0` est visible dans le registre ; l'application répond sur `192.168.56.11:8080` sans qu'aucun JAR ni Maven n'existe sur `target`.

**Pourquoi V7 ?** Avant : l'image n'existe que sur le poste qui l'a construite. Après : un artefact versionné, récupérable partout. Preuve : le catalogue du registre.

#### 9. Validation
1. Quelle différence entre `docker compose down` et `docker compose down -v` ?
2. Pourquoi Adminer joint-il la base avec le nom `db` ?
3. Que contient l'URL `192.168.56.10:5000/devops-demo:1.0` (registre, nom, tag) ?
4. Tâche : poussez un tag `1.1` et listez les tags du registre.

#### 10. Extension / challenge
Ajoutez un service `db` (PostgreSQL) au `docker-compose.yml` de l'application avec un `Secret`-like via variable d'environnement, puis esquissez les modifications Java nécessaires (`spring-boot-starter-data-jpa`, driver PostgreSQL, entité `Task`). Attention : l'application actuelle stocke les tâches en mémoire.
