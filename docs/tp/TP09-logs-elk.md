# TP 9 — Centraliser les logs avec ELK (V11)

**Durée : 75 min** · théorie 8 · activité 10 · mini-TP 20 · fil rouge 30 · validation 7 · **VM** : `ci`, `target`, `elk` · **Format** : individuel
*(Couvre les « TP ELK 1 à 8 » : comprendre les logs, collecter, Filebeat/Logstash, Elasticsearch, Kibana, recherche, dashboard, logs du projet.)*

#### 1. Objectif pédagogique
Comprendre qu'on ne se connecte pas aux serveurs pour lire des logs ; indexer des événements, les rechercher dans Kibana, diagnostiquer des incidents réalistes et construire un tableau de bord.

#### 2. Prérequis
TP05 à TP07 (application déployée sur `target`, en conteneur).

#### 3. Concept DevOps abordé
**Observabilité par les logs.** Chaque événement est collecté (Filebeat), analysé (Logstash : champs `level`, `thread`…), stocké et indexé (Elasticsearch), puis exploré (Kibana). Un log est un **événement daté** : il dit *ce qui s'est passé*.

#### 4. Problème réel à résoudre
« Une application fonctionne mais les erreurs sont réparties sur plusieurs serveurs. Un client signale un problème survenu à 14 h 32. Il faut ouvrir plusieurs consoles, chercher à la main dans des fichiers énormes, et on ne sait pas si l'incident est isolé ou répété. Comment retrouver rapidement une erreur ? »

#### 5. Activité pédagogique de découverte **[N1 Découverte]** (10 min) — « Chercher dans deux fichiers »
```bash
cd ~/devops-formation/mini-tp/logs && ./genlogs.sh
grep -c ERROR web1.log web2.log
grep ERROR web1.log | head -3
```
**Questions** (chronométrées) : quelle minute concentre le plus d'erreurs, sur quel serveur ? Combien de lignes `WARN` ? Combien de temps pour 2 fichiers de 300 lignes ? Pour 20 serveurs ?

> 🎤 **Formateur** : « 1 minute pour 2 petits fichiers. Avec des millions de lignes sur 20 serveurs, impossible. Il nous faut un moteur de recherche pour les logs. »

#### 6. Mini-TP **[N2 Application]** (20 min) — « Indexer et chercher »
- **Contexte** : événements fabriqués, **sans** l'application ni Filebeat.
- **Objectif** : envoyer des documents dans Elasticsearch et les interroger dans Kibana.
- **Architecture** : `ci` → API HTTP → Elasticsearch (`elk`) → Kibana.
- **Fichier** `mini-tp/logs/make-events.sh` :

```bash
#!/usr/bin/env bash
# Produit des événements au format NDJSON (API _bulk d'Elasticsearch), horodatés "maintenant"
now() { date -u -d "-$1 minutes" +%Y-%m-%dT%H:%M:%SZ; }
emit() { printf '{"index":{"_index":"mini-logs"}}\n{"@timestamp":"%s","service":"%s","level":"%s","message":"%s"}\n' "$(now $1)" "$2" "$3" "$4"; }
emit 9 web1 INFO  "Tache creee id=1"
emit 8 web1 INFO  "Tache creee id=2"
emit 7 web2 WARN  "Requete invalide : titre vide"
emit 6 web2 ERROR "Erreur interne : connexion base de donnees refusee"
emit 5 web1 INFO  "Tache supprimee id=1"
emit 4 web2 ERROR "Erreur interne : timeout base de donnees"
emit 3 web1 WARN  "Ressource absente : tache 999"
emit 2 web2 INFO  "Tache creee id=3"
emit 1 web1 ERROR "Erreur interne : NullPointerException"
```

**Étapes** (VM `elk` démarrée ; patientez 2-3 min après le démarrage)
```bash
curl -s http://192.168.56.13:9200 | head -5                      # Elasticsearch répond
cd ~/devops-formation/mini-tp/logs
./make-events.sh > events.ndjson
curl -s -H 'Content-Type: application/x-ndjson' -XPOST http://192.168.56.13:9200/_bulk --data-binary @events.ndjson | head -c 200
curl -s 'http://192.168.56.13:9200/mini-logs/_search?q=level:ERROR&pretty' | head -40
```
Dans **Kibana** (http://192.168.56.13:5601) : *Stack Management → Data Views → Create* : nom et pattern `mini-logs`, champ temps `@timestamp`. Puis *Discover* (intervalle : dernières 15 minutes) et testez les requêtes KQL :
- `level : "ERROR"`
- `level : "ERROR" and service : "web2"`
- `message : *base*`

**Résultat attendu** : 9 documents ; 3 erreurs ; 2 erreurs sur `web2` (connexion refusée, timeout base de données) ; la recherche `*base*` isole les problèmes de base de données.

**Erreurs fréquentes** : « No results » car la plage de temps est trop courte ; oublier l'en-tête `Content-Type: application/x-ndjson` ; Elasticsearch pas encore prêt (`curl` échoue).

**Solution** : les 3 requêtes KQL ci-dessus ; la requête `q=level:ERROR` renvoie 3 résultats.

#### 7. Retour pédagogique
- **Appris** : un log devient un document avec des champs ; une requête remplace des heures de `grep` ; l'horodatage permet de corréler plusieurs serveurs.
- **Pourquoi en DevOps** : le diagnostic ne dépend plus de l'accès aux serveurs ; on voit des tendances, pas seulement des lignes.
- **Problème résolu** : recherche dispersée, délai de diagnostic.
- **Limites** : un log ne dit pas *combien* ni *à quelle vitesse* (métriques, TP10) ; stocker beaucoup coûte cher ; un format de log instable casse l'analyse ; ELK est gourmand en mémoire.

#### 8. Retour au projet fil rouge **[N3 Intégration]** (30 min) — V11
1. Lisez `elk/logstash/pipeline/logstash.conf` (entrée Beats, filtre `grok`, tag `java_exception`, sortie `devops-logs-*`) et `elk/filebeat/filebeat.yml` (collecte des conteneurs, regroupement des *stack traces* en un seul événement).
2. Sur `target`, démarrez Filebeat (l'application doit tourner, TP07) :
   ```bash
   ssh vagrant@192.168.56.11
   cd /vagrant/elk/filebeat && docker compose up -d
   docker logs filebeat --tail 20
   exit
   ```
3. **Scénarios d'incident** (depuis `ci`) :

   | Scénario | Déclencheur | Ce qu'on cherche dans Kibana |
   |---|---|---|
   | HTTP 500 | `curl http://192.168.56.11:8080/api/simulate/error` | `level : "ERROR"` |
   | Exception Java | idem | `tags : "java_exception"` — **une seule** entrée avec la stack trace complète |
   | Requête invalide (400) | `curl -XPOST -H 'Content-Type: application/json' -d '{"title":""}' http://192.168.56.11:8080/api/tasks` | `level : "WARN"` et `Requête invalide` |
   | Ressource inconnue (404) *(équivalent « utilisateur inconnu »)* | `curl -XDELETE http://192.168.56.11:8080/api/tasks/9999` | `log_message : *introuvable*` |
   | Problème de base de données | (pas de BDD dans l'application) | rejouer la recherche `*base*` du mini-TP |
   | Temps de réponse élevé | `curl http://192.168.56.11:8080/api/simulate/slow?ms=3000` | **Introuvable dans les logs** : rien n'est écrit ; c'est exactement ce que le TP10 résoudra |

   Générez du trafic : `for i in $(seq 1 40); do curl -s http://192.168.56.11:8080/api/info >/dev/null; curl -s http://192.168.56.11:8080/api/simulate/error >/dev/null; done`
4. Dans Kibana, créez la *data view* `devops-logs-*` (`@timestamp`), puis un **tableau de bord « Santé de l'application »** avec : volume de logs par `level` dans le temps ; répartition des niveaux (anneau) ; messages d'erreur les plus fréquents (`log_message.keyword`).

**Résultat attendu** : l'index `devops-logs-*` contient des documents ; l'exception Java tient dans un seul document ; le dashboard contient 3 visualisations.

**Pourquoi V11 ?** Avant : les logs restent dans les conteneurs. Après : un point de recherche unique. Preuve : l'erreur du 500 retrouvée en quelques secondes dans Kibana.

#### 9. Validation
1. Quel composant collecte les logs ? Lequel les analyse ? Lequel les stocke ? Lequel les affiche ?
2. Pourquoi regrouper les lignes d'une *stack trace* en un seul événement ?
3. Quelle requête KQL isole les exceptions Java ?
4. Tâche : montrez le nombre d'erreurs 500 sur les 15 dernières minutes.
5. Pourquoi l'incident de lenteur n'apparaît-il pas ? Que faudrait-il pour le voir ?

#### 10. Extension / challenge
- Créez dans Kibana une **règle d'alerte** : plus de 10 erreurs en 5 minutes.
- Ajoutez dans Logstash un champ `severity_num` calculé à partir de `level`.
- Ajoutez le temps de réponse dans les logs Nginx (`$request_time`) et exploitez-le.
