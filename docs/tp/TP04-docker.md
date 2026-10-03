# TP 4 — Docker : image, conteneur, Dockerfile (V6)

**Durée : 105 min** · théorie 15 · activité 15 · mini-TP 35 · fil rouge 30 · validation 10 · **VM** : `ci` · **Format** : individuel
*(Couvre les « TP Docker 1 à 4 » du cahier des charges : image/conteneur, serveur web, Dockerfile, conteneurisation de l'application Java.)*

#### 1. Objectif pédagogique
Comprendre **pourquoi** on empaquette une application avec son environnement ; faire la différence entre image et conteneur ; écrire un `Dockerfile` ; conteneuriser l'application Java.

#### 2. Prérequis
TP02 (JAR produit par Maven). Notions de port et de processus.

#### 3. Concept DevOps abordé
**Immuabilité et reproductibilité de l'artefact.** Une *image* est un modèle figé (système minimal + runtime + application) ; un *conteneur* est une instance en exécution, jetable. On ne « configure » pas un serveur à la main : on construit une image, on la déploie telle quelle partout.

#### 4. Problème réel à résoudre
« L'application tourne chez le développeur (Java 21) mais plante en recette (Java 11) et en production (autre variable d'environnement). Chaque serveur est un cas particulier. Comment livrer exactement le même environnement partout ? »

#### 5. Activité pédagogique de découverte **[N1 Découverte]** (15 min)
```bash
cat /etc/os-release | head -2                                   # l'OS de la VM
docker run --rm alpine cat /etc/os-release | head -2            # un autre OS, en 1 s
time docker run --rm alpine echo "bonjour"                      # démarrage éclair
docker run -d --name jetable alpine sleep 600
docker exec jetable sh -c "apk add --no-cache curl && curl --version | head -1"
docker rm -f jetable
docker run --rm alpine curl --version                           # curl a disparu !
```

**Questions** : quel OS voit-on dans le conteneur ? En combien de temps démarre-t-il comparé à une VM ? Où est passé `curl` après `docker rm` ? Qu'en déduire sur la façon d'installer des logiciels ?

> 🎤 **Formateur** : montrez deux terminaux côte à côte. Message clé : « Un conteneur est jetable ; ce qu'on veut garder doit être dans l'**image** (le Dockerfile), pas installé à la main dedans. »

#### 6. Mini-TP **[N2 Application]** (35 min) — « Un site web dans une image »
- **Contexte** : une page HTML statique à servir avec Nginx, sans installer Nginx sur la VM.
- **Objectif** : lancer un serveur web (Docker 2), écrire un Dockerfile (Docker 3), versionner l'image.
- **Architecture** : image `site-demo:1` → conteneur → port 8089 de la VM.
- **Fichiers** (`depot-tp/mini-tp/site-demo/`) :

`index.html`
```html
<!doctype html>
<html lang="fr">
<head><meta charset="utf-8"><title>Site démo</title></head>
<body>
  <h1 id="titre">Site démo DevOps</h1>
  <p id="version">Version @@VERSION@@</p>
  <button id="btn" onclick="document.getElementById('message').textContent='Bonjour DevOps !'">Dire bonjour</button>
  <p id="message"></p>
</body>
</html>
```

`Dockerfile`
```dockerfile
FROM nginx:1.27-alpine
ARG VERSION=dev
COPY index.html /usr/share/nginx/html/index.html
RUN sed -i "s/@@VERSION@@/${VERSION}/" /usr/share/nginx/html/index.html \
 && echo "${VERSION}" > /usr/share/nginx/html/version.txt
HEALTHCHECK --interval=15s --timeout=3s CMD wget -qO- http://localhost/ >/dev/null || exit 1
```

**Étapes et commandes**
```bash
# A. Lancer un serveur web sans rien construire (10 min)
docker run -d --name web -p 8089:80 nginx:1.27-alpine
curl -s localhost:8089 | head -4         # page par défaut de Nginx
docker ps && docker logs web | tail -3
docker rm -f web

# B. Construire notre image (15 min)
cd ~/devops-formation/mini-tp/site-demo
docker build --build-arg VERSION=1 -t site-demo:1 .
docker images site-demo
docker history site-demo:1               # les couches
docker run -d --name site -p 8089:80 site-demo:1
curl -s localhost:8089 | grep Version    # Version 1
cat <(curl -s localhost:8089/version.txt)

# C. Modifier et reconstruire (10 min)
sed -i 's/Site démo DevOps/Site démo DevOps - v2/' index.html
docker build --build-arg VERSION=2 -t site-demo:2 .   # observez "CACHED" sur les étapes inchangées
docker rm -f site && docker run -d --name site -p 8089:80 site-demo:2
curl -s localhost:8089 | grep -E "Version|v2"
```

**Résultat attendu** : étape A : page « Welcome to nginx! » ; étape B : `Version 1` et `1` dans `version.txt` ; étape C : `Version 2`, et la ligne `COPY` est reconstruite alors que la couche `FROM` est en cache.

**Erreurs fréquentes**
- `port is already allocated` : un ancien conteneur occupe 8089 (`docker rm -f`).
- `permission denied … docker.sock` : se reconnecter (`exit` puis `vagrant ssh ci`) pour prendre en compte le groupe `docker`.
- Oublier le `.` à la fin de `docker build`.
- Modifier `index.html` mais relancer l'ancien conteneur sans reconstruire.

**Solution** : voir les commandes ; le Dockerfile est celui fourni. Nettoyage : `docker rm -f site`.

#### 7. Retour pédagogique
- **Appris** : image ≠ conteneur ; chaque instruction du Dockerfile crée une couche mise en cache ; une image se versionne avec un tag (`:1`, `:2`).
- **Pourquoi en DevOps** : l'image est l'artefact livrable ; ce qui a été testé est ce qui sera déployé.
- **Problème résolu** : écarts d'environnement, installations manuelles.
- **Limites** : un conteneur seul ne gère ni haute disponibilité ni orchestration (TP08) ; l'image grossit si on y met n'importe quoi ; elle n'est pas un coffre-fort (jamais de secrets dedans).

#### 8. Retour au projet fil rouge **[N3 Intégration]** (30 min) — V6
1. Lisez `app/Dockerfile` : image de base `eclipse-temurin:21-jre-jammy`, `ARG APP_VERSION`, utilisateur **non-root** `appuser`, `HEALTHCHECK`, `ENTRYPOINT` avec `JAVA_OPTS`. Pour chaque instruction, dites **pourquoi**.
2. Construire et exécuter :
   ```bash
   cd ~/devops-formation/app
   mvn -B -DskipTests package
   docker build -t devops-demo:1.0 --build-arg APP_VERSION=1.0 .
   docker images devops-demo
   docker run -d --name demo -p 9090:8080 devops-demo:1.0
   sleep 45 && docker ps                    # STATUS : (healthy)
   curl -s localhost:9090/api/info
   docker exec demo sh -c "whoami && ls /app"
   docker logs demo | tail -5
   docker rm -f demo
   ```
3. **Pourquoi `-p 9090:8080` ?** Jenkins occupe déjà 8080 sur `ci`.

**Résultat attendu** : `healthy`, `/api/info` renvoie `"version":"1.0"`, `whoami` affiche `appuser`.

**Pourquoi V6 ?** Avant : l'application dépend du JDK et de la configuration du serveur. Après : une image portable. Preuve : `docker ps` en `healthy`.

#### 9. Validation
1. Différence entre `docker run` et `docker build` ?
2. Pourquoi exécuter le processus avec un utilisateur non-root ?
3. Quelle instruction du Dockerfile fixe la version de l'application dans l'image ?
4. Tâche : modifiez l'`ARG` et prouvez, avec `curl`, que la version a changé.
5. Que perd-on si l'on supprime un conteneur ? (son système de fichiers modifié ; pas l'image)

#### 10. Extension / challenge
Réécrivez le Dockerfile en **multi-stage** (le build Maven a lieu dans l'image) et comparez taille et temps de build :
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
ARG APP_VERSION=1.0.0
ENV APP_VERSION=${APP_VERSION} JAVA_OPTS="-Xms128m -Xmx384m"
RUN useradd --system --create-home appuser
WORKDIR /app
COPY --from=build /src/target/devops-demo.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
```
(à valider avant la séance : non exécuté lors de la conception).
