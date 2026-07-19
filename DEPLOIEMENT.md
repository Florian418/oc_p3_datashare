# Déploiement — DataShare

## Objectif

Ce projet ne définit pas de pipeline CI/CD. Le fichier ci-dessous est un `docker-compose.yml` **représentatif**, utilisé pour héberger une démonstration live de l'application sur une infrastructure personnelle distincte de ce dépôt — il illustre comment les 4 composants (backend, frontend, PostgreSQL, stockage objet S3) s'assemblent en production, mais n'est ni exécuté ni testé depuis ce repo.

Il complète `backend/compose.yaml` (utilisé en développement, PostgreSQL + Garage uniquement) avec deux différences propres à un déploiement en production :
- les images `back`/`front` sont construites (`Dockerfile` dédiés, non présents dans ce repo) et publiées sur un registre privé plutôt que lancées via `bootRun`/`ng serve` ;
- le backend est activé avec le profil Spring `prod` (`SPRING_PROFILES_ACTIVE=prod`, voir `backend/src/main/resources/application-prod.properties`) — sans lui, le jar packagé n'a aucune information de connexion PostgreSQL (`spring-boot-docker-compose`, qui gère cela en développement, est en scope `developmentOnly` et absent du jar).

```
services:
  back:
    image: registry.example.com/datashare-back:latest
    container_name: datashare-back
    restart: unless-stopped
    depends_on:
      - postgres
      - garage
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: postgres
      DB_PORT: "5432"
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      GARAGE_S3_ENDPOINT: http://garage:3900
      GARAGE_BUCKET: ${GARAGE_BUCKET}
      GARAGE_ACCESS_KEY: ${GARAGE_ACCESS_KEY}
      GARAGE_SECRET_KEY: ${GARAGE_SECRET_KEY}
      JWT_SECRET: ${JWT_SECRET}
      CORS_ALLOWED_ORIGIN: https://datashare.example.com
      FRONTEND_BASE_URL: https://datashare.example.com

  front:
    image: registry.example.com/datashare-front:latest
    container_name: datashare-front
    restart: unless-stopped
    depends_on:
      - back
    ports:
      - "9098:80"

  postgres:
    image: postgres:18.4-alpine3.23
    container_name: datashare-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - datashare_postgres:/var/lib/postgresql

  garage:
    image: dxflrs/garage:v2.3.0
    container_name: datashare-garage
    command: ["/garage", "server", "--single-node", "--default-bucket"]
    restart: unless-stopped
    environment:
      GARAGE_DEFAULT_ACCESS_KEY: ${GARAGE_ACCESS_KEY}
      GARAGE_DEFAULT_SECRET_KEY: ${GARAGE_SECRET_KEY}
      GARAGE_DEFAULT_BUCKET: ${GARAGE_BUCKET}
    volumes:
      - ./garage.toml:/etc/garage.toml:ro
      - datashare_garage_meta:/var/lib/garage/meta
      - datashare_garage_data:/var/lib/garage/data

volumes:
  datashare_postgres:
  datashare_garage_meta:
  datashare_garage_data:
```

Le frontend sert de reverse-proxy (nginx) vers le backend pour `/api/`, évitant toute configuration CORS en production — le navigateur ne voit qu'un seul domaine.
