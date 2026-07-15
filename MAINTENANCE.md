# Maintenance — DataShare

Procédures de mise à jour des dépendances, fréquence, risques à surveiller, et sauvegarde de la base de données.

1. [Mise à jour des dépendances](#1-mise-à-jour-des-dépendances)
2. [Fréquence](#2-fréquence)
3. [Risques à surveiller](#3-risques-à-surveiller)
4. [Sauvegarde et restauration de la base de données](#4-sauvegarde-et-restauration-de-la-base-de-données)

## 1. Mise à jour des dépendances

**Backend (Gradle)** : la plupart des versions sont gérées par la BOM Spring Boot (`platform(SpringBootPlugin.BOM_COORDINATES)` dans `backend/build.gradle`) — bumper `id 'org.springframework.boot' version 'X'` met à jour l'essentiel d'un coup, de façon cohérente (versions testées ensemble par l'équipe Spring). Les dépendances hors BOM (`springdoc-openapi`, `jjwt`, `tika-core`, SDK AWS...) se vérifient une par une sur Maven Central. Après toute mise à jour : relancer `./gradlew test` puis `./gradlew dependencyCheckAnalyze` (voir `SECURITY.md`) pour confirmer qu'aucune régression de sécurité n'a été introduite.

**Frontend (npm)** : `ng update` (pas `npm update` seul) pour Angular et ses paquets liés — l'outil applique aussi les migrations de code nécessaires, pas juste le numéro de version. `npm outdated` pour repérer le reste. `npm audit` après coup.

## 2. Fréquence

- **Sécurité** : les alertes [Dependabot](https://docs.github.com/en/code-security/dependabot) de GitHub tournent en continu (activées dans les réglages du repo) — elles détectent une nouvelle CVE sur une dépendance déjà en place sans attendre un scan manuel. `dependencyCheckAnalyze`/`npm audit` (voir `SECURITY.md`) restent lancés manuellement avant chaque mise en production, en complément.
- **Mises à jour de routine** (hors sécurité) : objectif trimestriel — assez fréquent pour éviter d'accumuler une dette de versions trop importante d'un coup, assez espacé pour ne pas passer plus de temps à mettre à jour qu'à développer. Reste manuel pour l'instant (pas de PR automatique façon "Dependabot version updates", volontairement désactivé — trop de bruit pour un projet à cette échelle).
- **Montées de version majeures** (Spring Boot, Angular) : pas de fréquence fixe — à évaluer au cas par cas, en lisant le guide de migration avant de bumper, avec du temps réservé pour re-tester derrière.

## 3. Risques à surveiller

- **Versions forcées via `constraints { }`** (`backend/build.gradle`, voir `SECURITY.md` §2) : correctifs de CVE appliqués au-dessus de ce que recommande la BOM Spring Boot. À retirer dès qu'une version plus récente de la BOM les inclut nativement — sinon elles peuvent devenir orphelines (oubliées, jamais retirées) ou entrer en conflit avec une future version de la BOM.
- **Suppressions dans `backend/dependency-check-suppressions.xml`** : basées sur le SHA1 exact du binaire (voir `SECURITY.md` §4). Une suppression ne s'applique donc plus automatiquement dès que la dépendance change de version — comportement voulu (évite qu'un faux positif masque un vrai futur problème), mais à revérifier à chaque montée de version concernée : le rapprochement CVE erroné est-il toujours d'actualité sur la nouvelle version ?
- **Version de l'outillage lui-même** (pas les dépendances applicatives) : le plugin PIT/Pitest est forcé en `1.25.7` (indépendamment de la version par défaut du plugin Gradle) pour le support du bytecode Java 25 — à repasser en version par défaut une fois que le plugin la suit nativement, pour ne pas traîner un override devenu inutile.
- **Migrations de schéma Flyway** : toute mise à jour de dépendance qui toucherait indirectly à Hibernate/JPA doit rester compatible avec `ddl-auto=validate` (Hibernate ne doit jamais modifier le schéma lui-même) — un changement de comportement de validation entre deux versions casserait le démarrage de l'application plutôt que la base de données, donc détectable immédiatement via `./gradlew test`.

## 4. Sauvegarde et restauration de la base de données

Le conteneur PostgreSQL (`backend/compose.yaml`) est le seul composant à état durable géré directement par le projet — les fichiers uploadés vivent dans Garage (stockage objet, sauvegarde hors scope de ce document). Identifiants dans `backend/.env` (`POSTGRES_DB`, `POSTGRES_USER`).

**Sauvegarde** (depuis `backend/`, conteneur démarré) :

```
docker compose exec postgres pg_dump -U datashare -d datashare > datashare-backup.sql
```

**Restauration** (écrase les données existantes de la base ciblée) :

```
docker compose exec -T postgres psql -U datashare -d datashare < datashare-backup.sql
```

`-T` désactive le pseudo-terminal de `docker compose exec` — nécessaire pour que la redirection d'entrée (`<`) transmette le fichier correctement à `psql`, pas seulement pour la sauvegarde en sortie.
