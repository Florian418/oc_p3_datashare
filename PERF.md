# Performance — DataShare

1. [Test de charge backend (k6)](#1-test-de-charge-backend-k6)
2. [Journalisation des métriques clés](#2-journalisation-des-métriques-clés)

## 1. Test de charge backend (k6)

**Outil** : [k6](https://k6.io/) (Grafana Labs) — un seul script JS suffit pour scripter un scénario multi-étapes (upload multipart, authentification, téléchargement), sans configuration lourde.

**Cible** : le parcours complet d'un fichier protégé par mot de passe — inscription, connexion, upload authentifié (US01), authentification du partage (US02/US09), téléchargement réel, puis suppression (US06). Pas seulement l'upload isolé : le download protégé exerce un chemin différent (vérification BCrypt + émission JWT), volontairement plus coûteux en CPU qu'une simple lecture, donc pertinent à mesurer séparément.

**Scénario** (`perf/upload-download.js`) : un compte de test est créé une seule fois (`setup()`, exécuté avant la montée en charge), puis chaque itération de chaque utilisateur virtuel enchaîne upload (fichier PNG synthétique de 5 Mo, même principe que le buffer minimal de `e2e/tests/upload.spec.ts`) → authentification du partage → téléchargement (contenu vérifié complet) → suppression. Montée progressive à 10 utilisateurs virtuels sur 10s, palier de 30s à 10 VUs, puis redescente :

```
./gradlew bootRun            # backend + Garage + Postgres démarrés (docker compose up -d)
k6 run perf/upload-download.js
```

**Résultats (2026-07-15, poste de dev local)** :

```
  █ THRESHOLDS

    http_req_duration
    ✓ 'p(95)<2000' p(95)=219.91ms

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%

  █ TOTAL RESULTS

    checks_succeeded...: 100.00% 1754 out of 1754

    ✓ register status is 201
    ✓ login status is 200
    ✓ upload status is 201
    ✓ authenticate status is 200
    ✓ download status is 200
    ✓ download has full content
    ✓ list status is 200
    ✓ delete status is 204

    HTTP
    http_req_duration..............: avg=79.58ms min=2.63ms med=67.55ms max=352.87ms p(90)=196.64ms p(95)=219.91ms
    http_req_failed................: 0.00%  0 out of 1462
    http_reqs......................: 1462   28.825899/s

    EXECUTION
    iterations.....................: 292    5.757293/s

    NETWORK
    data_sent......................: 1.5 GB 30 MB/s
    data_received..................: 1.5 GB 30 MB/s
```

**Interprétation** :
- **0% d'échec sur 1754 vérifications** (292 parcours complets — upload, authentification, téléchargement avec vérification du contenu intégral, suppression — répétés en continu par 10 utilisateurs virtuels simultanés) : aucune erreur, aucun timeout, aucune incohérence de contenu téléchargé.
- **p95 à 220ms, moyenne à 80ms** tous types de requêtes confondus — largement sous le seuil fixé (2s), malgré le mélange de requêtes légères (JSON) et lourdes (upload/download de 5 Mo).
- **~1,5 Go envoyés et 1,5 Go reçus** en 50s (upload et download comptent chacun pour leur propre volume, contrairement au test précédent qui ne mesurait que l'envoi) — aucune dégradation visible du temps de réponse entre le début et la fin du palier de charge.
- Test réalisé sur un poste de développement local (Garage single-node, Postgres et backend sur la même machine) — les chiffres ne reflètent pas un environnement de production avec latence réseau réelle entre les composants, mais valident l'absence de goulot d'étranglement évident dans le code applicatif (pas de blocage, pas de fuite de connexions, BCrypt/JWT ne dégradent pas le temps de réponse à cette échelle) sur l'ensemble du cycle de vie d'un fichier.

## 2. Journalisation des métriques clés

Un log générique existe déjà (`RequestLoggingFilter`) : chaque requête HTTP est journalisée en JSON structuré (format [ECS](https://www.elastic.co/guide/en/ecs/current/index.html), natif Spring Boot, sans dépendance) avec méthode, chemin, statut et durée — mais sans détail métier. `FileShareService.upload()` et `ShareService.download()` ajoutent maintenant un log dédié avec les champs qui manquaient : taille du fichier, type MIME, protection par mot de passe, authentification.

**Exemple réel** (upload d'un fichier de 16 octets, testé en local le 2026-07-15) — deux lignes distinctes pour la même requête, à croiser par timestamp/thread :

```json
{"@timestamp":"2026-07-15T15:51:19.183561900Z","log":{"level":"INFO","logger":"fr.euflow.backend.fileshare.FileShareService"},"message":"file_uploaded","sizeBytes":16,"mimeType":"image/png","expiresInDays":7,"passwordProtected":false,"authenticated":false,"ecs":{"version":"8.11"}}
{"@timestamp":"2026-07-15T15:51:19.219712800Z","log":{"level":"INFO","logger":"fr.euflow.backend.logging.RequestLoggingFilter"},"message":"http_request","method":"POST","path":"/api/v1/files","status":201,"durationMs":383,"ecs":{"version":"8.11"}}
```

Et au téléchargement :

```json
{"@timestamp":"2026-07-15T15:52:16.115808600Z","log":{"level":"INFO","logger":"fr.euflow.backend.fileshare.ShareService"},"message":"file_downloaded","sizeBytes":16,"mimeType":"image/png","passwordProtected":false,"ecs":{"version":"8.11"}}
{"@timestamp":"2026-07-15T15:52:16.148256600Z","log":{"level":"INFO","logger":"fr.euflow.backend.logging.RequestLoggingFilter"},"message":"http_request","method":"GET","path":"/api/v1/shares/.../download","status":200,"durationMs":312,"ecs":{"version":"8.11"}}
```

Sortie en stdout uniquement (pas de fichier de log), format JSON directement exploitable par un agrégateur de logs (ELK, Grafana Loki...) sans parsing de texte libre — permet en théorie des requêtes du type "taille moyenne des fichiers uploadés", "% de partages protégés par mot de passe", ou de corréler taille et durée sur un historique réel, au-delà du seul test de charge synthétique de la section 1.
