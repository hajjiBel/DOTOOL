# Programme et planning — 3 jours

## Principe

Chaque grand concept suit la même boucle : **POURQUOI ?** (problème réel) → **COMMENT ?** (concept et activité simple) → **JE PRATIQUE** (mini-TP indépendant) → **J'INTÈGRE** (projet fil rouge) → **J'AUTOMATISE** (pipeline) → **JE SUPERVISE** (logs, métriques).

Journée type : 09:00 – 17:00, déjeuner 12:30 – 13:30, deux pauses de 15 min → **6 h 30 de contenu par jour** (390 min). Les pauses ne sont pas comptées dans les tableaux ci-dessous.

## Vue d'ensemble

| Jour | Thème | TP | Versions du fil rouge |
|---|---|---|---|
| **1** | Du code à l'image | Intro · TP01 Git · TP02 Maven & tests · TP03 CI Jenkins · TP04 Docker | V1 → V6 |
| **2** | Livrer automatiquement | TP05 Compose & registre · TP06 Ansible · TP07 Pipeline CI/CD · Scénario d'entreprise | V7 → V9 |
| **3** | Exploiter et superviser | TP08 Kubernetes · TP09 ELK · TP10 Prometheus/Grafana · TP11 Selenium · Projet final | V10 → V13 + finale |

## Jour 1 — du code à l'image (390 min)

| Horaire | Séance | Théorie | Activité | Mini-TP | Fil rouge | Valid. | Total |
|---|---|---|---|---|---|---|---|
| 09:00 | Introduction DevOps, présentation du projet, `vagrant up ci` | 20 | 10 | – | – | – | 30 |
| 09:30 | **TP01** Git en équipe (V1→V2) | 10 | 10 | 25 | 35 | 10 | 90 |
| 11:15 (pause 11:00) | **TP02** Maven & tests (V3→V4) | 10 | 15 | 20 | 20 | 10 | 75 |
| 13:30 | **TP03** Intégration continue Jenkins (V5) | 10 | 15 | 25 | 30 | 10 | 90 |
| 15:15 (pause 15:00) | **TP04** Docker (V6) | 15 | 15 | 35 | 30 | 10 | 105 |
| | **Total jour 1** | **65** | **65** | **105** | **115** | **40** | **390** |

## Jour 2 — livrer automatiquement (390 min)

| Horaire | Séance | Théorie | Activité | Mini-TP | Fil rouge | Valid. | Total |
|---|---|---|---|---|---|---|---|
| 09:00 | **TP05** Compose, registre, environnement reproductible (V7) | 8 | 10 | 20 | 15 | 7 | 60 |
| 10:00 | **TP06** Ansible (V8) | 12 | 15 | 35 | 33 | 10 | 105 |
| 11:45 | *Pause 15 min* | | | | | | |
| 12:00 | **Scénario d'entreprise, partie 1** : atelier de conception (organisation, workflow, pipeline) | – | 30 | – | – | – | 30 |
| 13:30 | **TP07** Pipeline CI/CD complet (V9) | 15 | 20 | 35 | 70 | 10 | 150 |
| 16:00 | *Pause 15 min* | | | | | | |
| 16:15 | **Scénario d'entreprise, partie 2** : réalisation technique (retour arrière) | – | – | – | 35 | 10 | 45 |
| | **Total jour 2** | **35** | **75** | **90** | **153** | **37** | **390** |

## Jour 3 — exploiter et superviser (390 min)

| Horaire | Séance | Théorie | Activité | Mini-TP | Fil rouge | Valid. | Total |
|---|---|---|---|---|---|---|---|
| 09:00 | **TP08** Kubernetes (V10) | 12 | 15 | 33 | 35 | 10 | 105 |
| 10:45 | *Pause 15 min* | | | | | | |
| 11:00 | **TP09** Logs avec ELK (V11) | 8 | 10 | 20 | 30 | 7 | 75 |
| 12:15 | *Pause 15 min : démarrer la VM `monitor`, arrêter `elk`* | | | | | | |
| 13:30 | **TP10** Monitoring Prometheus/Grafana (V12) | 8 | 10 | 20 | 30 | 7 | 75 |
| 14:45 | **TP11** Tests Selenium, barrière qualité (V13) | 5 | 5 | 10 | 20 | 5 | 45 |
| 15:30 | **Projet final** (chaîne complète + exercice d'incident) et évaluation | – | – | – | 75 | 15 | 90 |
| | **Total jour 3** | **33** | **40** | **83** | **190** | **44** | **390** |

L'après-midi du jour 3 n'a pas de pause formelle (5 minutes libres à la discrétion du formateur pendant le projet final).

## Bilan du temps

| | Théorie | Activité de découverte | Mini-TP | Fil rouge | Validation | Total |
|---|---|---|---|---|---|---|
| **Minutes** | 133 | 180 | 278 | 458 | 121 | 1170 |
| **Part** | 11 % | 15 % | 24 % | 39 % | 10 % | 100 % |

Comme demandé, la théorie reste sous 30-40 % ; la pratique (activité, mini-TP, fil rouge, validation) représente donc environ 89 %. La « mise en commun » de chaque activité est incluse dans le bloc « activité ». Si vous voulez un équilibre plus proche de 65/35, allongez les blocs théorie plutôt que de supprimer des TP.

## Points de vigilance du planning

- **Le jour 3 est serré** : si le groupe prend du retard, faites du TP11 (Selenium) une démonstration et donnez le temps libéré au projet final.
- **Le démarrage des VM** (`elk`, `monitor`, `k8s`) est à lancer **avant** la séance concernée, pendant la pause ou le déjeuner (voir `03-environnement-lab.md`).
- **Binômes** : TP01 (collaboration Git), TP07 et scénario d'entreprise se font en binôme ; le reste est individuel.
- **Mémoire des VM** : jamais plus de 3 VM allumées en même temps (voir `03-environnement-lab.md`).
