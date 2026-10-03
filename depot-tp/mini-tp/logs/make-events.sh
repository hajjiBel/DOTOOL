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
