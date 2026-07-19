# Maintenance — DataShare

## Objectif

Procédures de mise à jour des dépendances, fréquence des interventions, risques associés, et sauvegarde de la base de données.

## Procédures de mise à jour

### Front-end (Angular)

Vérifier les dépendances obsolètes, depuis `frontend/` :

```
npm outdated
```

Mettre à jour Angular et les paquets liés (`ng update` applique aussi les migrations de code nécessaires, pas juste le numéro de version), depuis `frontend/` :

```
ng update
```

### Back-end (Spring Boot/Gradle)

La majorité des versions sont gérées par la BOM Spring Boot (`platform(SpringBootPlugin.BOM_COORDINATES)` dans `backend/build.gradle`) — bumper `id 'org.springframework.boot' version 'X'` met à jour l'essentiel d'un coup, avec des versions testées ensemble par l'équipe Spring. Les dépendances hors BOM (`springdoc-openapi`, `jjwt`, `tika-core`, SDK AWS...) se vérifient une par une sur Maven Central — pas d'outil de détection automatique équivalent à `npm outdated` configuré côté Gradle.

Après toute mise à jour, depuis `backend/` :

```
./gradlew test
./gradlew dependencyCheckAnalyze
```

(confirme qu'aucune régression fonctionnelle ni de sécurité n'a été introduite — voir `SECURITY.md`)

### Base de données (PostgreSQL)

Les changements de schéma passent par une migration Flyway versionnée (`backend/src/main/resources/db/migration/VX__nom.sql`), appliquée automatiquement au démarrage de l'application (`ddl-auto=validate` — Hibernate ne modifie jamais le schéma lui-même) : pas de script manuel à lancer.

Le conteneur PostgreSQL (`backend/compose.yaml`) est le seul composant à état durable géré directement par le projet — les fichiers uploadés vivent dans Garage (stockage objet, sauvegarde hors scope de ce document). Identifiants dans `backend/.env` (`POSTGRES_DB`, `POSTGRES_USER`).

Sauvegarde, depuis `backend/` (conteneur démarré) :

```
docker compose exec postgres pg_dump -U datashare -d datashare > datashare-backup.sql
```

Restauration (écrase les données existantes de la base ciblée) :

```
docker compose exec -T postgres psql -U datashare -d datashare < datashare-backup.sql
```

`-T` désactive le pseudo-terminal de `docker compose exec` — nécessaire pour que la redirection d'entrée (`<`) transmette le fichier correctement à `psql`, pas seulement pour la sauvegarde en sortie.

## Fréquence de mise à jour des dépendances

[Dependabot](https://docs.github.com/en/code-security/dependabot) est activé sur le repo pour les alertes de sécurité — détection en continu d'une nouvelle CVE sur une dépendance déjà en place, sans attendre un scan manuel. `dependencyCheckAnalyze`/`npm audit` (voir `SECURITY.md`) restent lancés manuellement avant chaque mise en production, en complément.

Dependabot version updates (pull requests automatiques pour les mises à jour de routine) n'est pas activé — trop de bruit pour un projet de cette taille.

En dehors des alertes de sécurité (traitées dès qu'elles arrivent), objectif trimestriel pour les mises à jour de routine — assez fréquent pour ne pas accumuler une dette de version trop importante d'un coup, assez espacé pour ne pas y passer plus de temps qu'à développer. Les montées de version majeures (Spring Boot, Angular) n'ont pas de fréquence fixe — évaluées au cas par cas, en lisant le guide de migration avant de bumper, avec du temps réservé pour re-tester derrière.

## Risques de mise à jour des dépendances

- Comportement inattendu, régression, incompatibilité entre dépendances — risques génériques à toute mise à jour, d'où les tests relancés systématiquement après (`./gradlew test`, suite e2e complète).
- **Versions forcées via `constraints { }`** (`backend/build.gradle`, voir `SECURITY.md` §2) : correctifs de CVE appliqués au-dessus de ce que recommande la BOM Spring Boot. À retirer dès qu'une version plus récente de la BOM les inclut nativement — sinon elles peuvent devenir orphelines (oubliées, jamais retirées) ou entrer en conflit avec une future version de la BOM.
- **Suppressions dans `backend/dependency-check-suppressions.xml`** : basées sur le SHA1 exact du binaire (voir `SECURITY.md` §4). Une suppression ne s'applique donc plus automatiquement dès que la dépendance change de version — comportement voulu (évite qu'un faux positif masque un vrai futur problème), mais à revérifier à chaque montée de version concernée.
- **Version de l'outillage lui-même** (pas les dépendances applicatives) : le plugin PIT/Pitest est forcé en `1.25.7` (indépendamment de la version par défaut du plugin Gradle) pour le support du bytecode Java 25 — à repasser en version par défaut une fois que le plugin la suit nativement, pour ne pas traîner un override devenu inutile.
- **Migrations de schéma Flyway** : toute mise à jour de dépendance qui toucherait indirectement à Hibernate/JPA doit rester compatible avec `ddl-auto=validate` — un changement de comportement de validation entre deux versions casserait le démarrage de l'application plutôt que la base de données, donc détectable immédiatement via `./gradlew test`.
