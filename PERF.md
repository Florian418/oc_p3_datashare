# Performance — DataShare

1. [Test de charge backend (k6)](#1-test-de-charge-backend-k6)

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
