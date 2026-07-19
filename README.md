# DataShare

Application de partage de fichiers (MVP) — projet OpenClassrooms "Pilotez le développement d'une solution informatique".

Backend Spring Boot (Gradle), frontend Angular, PostgreSQL, stockage objet S3 (Garage).

## Fonctionnalités

- Dépôt d'un fichier, avec ou sans compte, avec expiration configurable (1 à 7 jours) et mot de passe optionnel
- Téléchargement via lien unique, sans compte requis
- Création de compte, connexion (authentification JWT)
- Historique des fichiers déposés, filtrable par tag
- Gestion des tags sur un fichier
- Suppression manuelle d'un fichier
- Purge automatique des fichiers expirés

## Prérequis

- Java 25 (Temurin recommandé)
- Node.js ≥ 22.22.3, ≥ 24.15.0 ou ≥ 26.0.0 (exigé par Angular CLI 22)
- Docker Desktop (Docker Compose v2)

## Installation et lancement

### Backend (`backend/`)

1. Copier les fichiers d'environnement et renseigner les valeurs (secrets Garage notamment) :
   ```
   cp .env.example .env
   cp garage.toml.example garage.toml
   ```
2. Démarrer les services dépendants (PostgreSQL + Garage) :
   ```
   docker compose up -d
   ```
3. Lancer l'application :
   ```
   ./gradlew bootRun
   ```
   Le schéma de base de données est géré automatiquement par Flyway au démarrage — aucune commande SQL manuelle.
4. Vérification :
   - Healthcheck : `http://localhost:8080/actuator/health` → `{"status":"UP"}`
   - Documentation API interactive (générée automatiquement, springdoc-openapi) : `http://localhost:8080/swagger-ui.html`

### Frontend (`frontend/`)

```
npm install
npm start
```

Application accessible sur `http://localhost:4200`.

### Variables d'environnement (`backend/.env`, voir `backend/.env.example`)

| Variable | Rôle |
| :--- | :--- |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Connexion PostgreSQL |
| `GARAGE_ACCESS_KEY` / `GARAGE_SECRET_KEY` | Identifiants S3 pour le stockage Garage |
| `GARAGE_BUCKET` / `GARAGE_S3_ENDPOINT` | Bucket cible et endpoint S3 de Garage |
| `JWT_SECRET` | Secret HMAC (Base64) qui signe/vérifie les JWT — généré via `openssl rand -base64 32` |
| `NVD_API_KEY` | Clé API NVD, utilisée par le scan de sécurité OWASP Dependency-Check (voir `SECURITY.md`) |

## Tests

| Type | Commande | Répertoire |
| :--- | :--- | :--- |
| Backend (unitaires/intégration) | `./gradlew test` | `backend/` |
| Couverture backend (JaCoCo) | `./gradlew jacocoTestReport` | `backend/` |
| Frontend (unitaires) | `npx ng test` | `frontend/` |
| E2E (Playwright) | `npx playwright test` | `e2e/` |

## Documentation

- [`TESTING.md`](TESTING.md) — stratégie et couverture des tests
- [`SECURITY.md`](SECURITY.md) — scan de dépendances, CVE traitées
- [`PERF.md`](PERF.md) — tests de performance et budgets front
- [`MAINTENANCE.md`](MAINTENANCE.md) — mise à jour des dépendances, sauvegarde de la base
- [`DEPLOIEMENT.md`](DEPLOIEMENT.md) — exemple de déploiement en production
