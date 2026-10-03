# TP 10 — Superviser avec Prometheus et Grafana (V12)

**Durée : 75 min** · théorie 8 · activité 10 · mini-TP 20 · fil rouge 30 · validation 7 · **VM** : `ci`, `target`, `monitor` · **Format** : individuel
*(Couvre les « TP Monitoring 1 à 7 » : observer CPU/RAM, exporter des métriques, Prometheus, Grafana, dashboard, alertes, monitoring du projet.)*

#### 1. Objectif pédagogique
Distinguer **log, métrique, trace, alerte** ; interroger des métriques avec PromQL ; construire un dashboard ; déclencher et comprendre une alerte ; superviser l'application fil rouge.

#### 2. Prérequis
TP07 (application déployée sur `target`), TP09 (logs).

#### 3. Concept DevOps abordé
**Observabilité par les métriques et alerte proactive.**

| Notion | Question à laquelle elle répond | Exemple |
|---|---|---|
| **Log** | Que s'est-il passé ? | « Erreur interne … stack trace » |
| **Métrique** | Combien ? À quelle vitesse ? | 12 requêtes/s, CPU 83 % |
| **Trace** | Par où est passée *cette* requête ? | service A → B → base *(hors périmètre ici)* |
| **Alerte** | Dois-je agir maintenant ? | « CPU > 80 % depuis 2 min » |

Prometheus **interroge** (*scrape*) des cibles qui exposent `/metrics` ; Grafana affiche ; Alertmanager notifie.

#### 4. Problème réel à résoudre
« L'application répond lentement depuis ce matin. Personne ne le sait : ce sont les utilisateurs qui téléphonent. Les logs ne montrent aucune erreur. Comment voir les problèmes **avant** les utilisateurs ? »

#### 5. Activité pédagogique de découverte **[N1 Découverte]** (10 min)
Sur `target` (`ssh vagrant@192.168.56.11`) :
```bash
top -bn1 | head -5 ; free -m ; df -h /
curl -s http://localhost:8080/actuator/prometheus | grep -E "^(http_server_requests_seconds_count|jvm_memory_used_bytes)" | head -6
sudo apt-get install -y stress-ng >/dev/null && stress-ng --cpu 2 --timeout 40s &
sleep 5 && top -bn1 | head -5
```
**Questions** : où voit-on la charge monter ? Qui regarde `top` à 3 h du matin ? Que contient le format `/actuator/prometheus` ? Comment garder l'historique ?

> 🎤 **Formateur** : « On a des chiffres instantanés ; il manque l'historique et le regard permanent. »

#### 6. Mini-TP **[N2 Application]** (20 min) — « La machine sous stress »
- **Contexte** : supervision de **machines**, sans l'application.
- **Objectif** : lire des métriques système dans Prometheus et Grafana, provoquer une charge.
- **Architecture** : `node_exporter` (installé sur chaque VM) → Prometheus (`monitor:9090`) → Grafana (`monitor:3000`).

**Étapes** (VM `monitor` démarrée)
1. Prometheus → **Status → Targets** : les cibles `node` doivent être **UP**.
2. Dans l'onglet *Graph*, exécutez :
   ```promql
   up
   100 - (avg by (host) (rate(node_cpu_seconds_total{mode="idle"}[2m])) * 100)
   (1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100
   ```
3. Lancez de la charge sur `target` : `ssh vagrant@192.168.56.11 "stress-ng --cpu 2 --timeout 240s"` et regardez la courbe CPU de `target` monter.
4. Grafana (`admin`/`admin`) → **Dashboards → New → Import** : ID `1860` (Node Exporter Full), source *Prometheus*. Repérez le panneau CPU de `target`.

**Résultat attendu** : `up` vaut 1 pour toutes les cibles ; la courbe CPU de `target` monte vers 100 % pendant 4 min puis retombe ; le dashboard 1860 affiche CPU, mémoire, disque, réseau.

**Erreurs fréquentes** : cible `DOWN` (VM éteinte ou `node_exporter` arrêté) ; plage de temps de Grafana trop large pour voir le pic ; import du dashboard sans choisir la source de données.

**Solution** : voir requêtes ci-dessus.

#### 7. Retour pédagogique
- **Appris** : un compteur seul ne sert à rien ; ce qui compte, c'est son **taux** (`rate`) et sa **tendance** ; Prometheus stocke des séries temporelles ; Grafana les rend lisibles.
- **Pourquoi en DevOps** : détecter, comprendre et prévoir avant la panne ; fournir des preuves après un incident.
- **Problème résolu** : pannes découvertes par les utilisateurs, absence d'historique.
- **Limites** : mauvaise alerte = fatigue d'alerte ; une métrique ne donne pas la cause (il faut revenir aux logs) ; la cardinalité excessive coûte cher.

#### 8. Retour au projet fil rouge **[N3 Intégration]** (30 min) — V12
1. Prometheus → Targets : `app` (appli sur `target:8080`) doit être **UP**.
2. **Requêtes de l'application** :
   ```promql
   sum(rate(http_server_requests_seconds_count{job="app"}[1m])) by (uri)
   histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="app"}[1m])))
   jvm_memory_used_bytes{job="app",area="heap"}
   ```
3. **Dashboard « Application »** (Grafana, nouveau dashboard) avec 4 panneaux : CPU par VM, mémoire, requêtes/s de l'application, latence p95. (Option : importer aussi le dashboard JVM `4701`.)
4. **Alertes** : lisez `monitoring/prometheus/alert.rules.yml` (InstanceDown, HighCpuUsage, HighMemoryUsage, LowDiskSpace, AppDown, AppHighErrorRate), puis **ajoutez** `SlowRequests` (latence p95 > 1 s pendant 1 min), à placer dans le groupe `application` :

   ```yaml
      # À ajouter dans le groupe "application" de monitoring/prometheus/alert.rules.yml
      - alert: SlowRequests
        expr: histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="app"}[1m]))) > 1
        for: 1m
        labels: { severity: warning }
        annotations:
          summary: "Latence p95 > 1 s sur l'application"
```

   Rechargez : `curl -X POST http://192.168.56.12:9090/-/reload` (si la VM `monitor` utilise `/vagrant/monitoring`, éditez le fichier côté hôte).
5. **Scénarios** (chacun suivi dans *Alerts* : *Inactive → Pending → Firing*) :

   | Scénario | Déclenchement | Alerte attendue |
   |---|---|---|
   | CPU élevé | `ssh vagrant@192.168.56.11 "stress-ng --cpu 2 --timeout 300s"` | `HighCpuUsage` |
   | Mémoire élevée | `ssh vagrant@192.168.56.11 "stress-ng --vm 1 --vm-bytes 92% --timeout 240s"` *(risque : arrêt du processus par le noyau, sans danger)* | `HighMemoryUsage` |
   | Application indisponible | `ssh vagrant@192.168.56.11 "docker stop devops-demo"` | `AppDown` (30 s) |
   | Nombre de requêtes élevé | `while true; do curl -s http://192.168.56.11:8080/api/info >/dev/null; done` (plusieurs terminaux) | pic sur le panneau « requêtes/s » |
   | Erreurs 5xx | `while true; do curl -s http://192.168.56.11:8080/api/simulate/error >/dev/null; sleep 0.5; done` | `AppHighErrorRate` |
   | Temps de réponse élevé | `while true; do curl -s "http://192.168.56.11:8080/api/simulate/slow?ms=2000" >/dev/null; done` | `SlowRequests` |

   Après chaque scénario : `docker start devops-demo` si nécessaire.
6. **Pont avec le TP09** : le scénario « temps de réponse élevé », invisible dans les logs, est maintenant visible.

**Résultat attendu** : dashboard à 4 panneaux avec données ; `AppDown` et `HighCpuUsage` passent à *Firing* puis disparaissent après correction ; `SlowRequests` est chargée.

**Pourquoi V12 ?** Avant : on découvre les pannes par les utilisateurs. Après : on les voit venir. Preuve : une alerte *Firing* avant tout appel utilisateur.

#### 9. Validation
1. Citez une question à laquelle répond un log et une à laquelle répond une métrique.
2. Que fait `rate(...[1m])` ?
3. Pourquoi `for: 1m` dans une règle d'alerte ?
4. Tâche : provoquez `AppDown` puis montrez-la en *Firing* dans Prometheus.
5. Quelle donnée utilisez-vous pour trouver la **cause** d'une alerte 5xx ? (les logs, dans Kibana)

#### 10. Extension / challenge
- Configurez un récepteur **webhook** dans `monitoring/prometheus/alertmanager.yml`.
- Exportez votre dashboard en JSON et provisionnez-le automatiquement dans `grafana/provisioning`.
- Ajoutez le job `app-k8s` (préparé en commentaire dans `prometheus.yml`) pour superviser l'application sur Kubernetes.
