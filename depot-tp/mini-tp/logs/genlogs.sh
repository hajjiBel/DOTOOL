#!/usr/bin/env bash
# Génère deux fichiers de logs (web1.log / web2.log) pour l'activité "retrouver une erreur"
for srv in web1 web2; do
  : > "$srv.log"
  for i in $(seq 1 300); do
    ts=$(date -u -d "-$((300-i)) seconds" +%Y-%m-%dT%H:%M:%S.000+00:00)
    r=$((RANDOM % 100))
    if   [ $r -lt 90 ]; then echo "$ts INFO  [http-nio-8080-exec-1] t.f.d.TaskService - Tâche créée id=$i" >> "$srv.log"
    elif [ $r -lt 97 ]; then echo "$ts WARN  [http-nio-8080-exec-2] t.f.d.GlobalExceptionHandler - Requête invalide : titre vide" >> "$srv.log"
    else                     echo "$ts ERROR [http-nio-8080-exec-3] t.f.d.GlobalExceptionHandler - Erreur interne req=$i" >> "$srv.log"
    fi
  done
done
echo "web1.log et web2.log générés ($(wc -l < web1.log) lignes chacun)"
