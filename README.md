# SecureBank API

API REST bancaire sécurisée développée avec Spring Boot, conteneurisée avec Docker, dans une démarche DevSecOps complète.

## Stack technique

- **Backend** : Spring Boot 3.2, Spring Security, Spring Data JPA
- **Base de données** : PostgreSQL + Flyway
- **Authentification** : JWT (Access Token + Refresh Token)
- **Conteneurisation** : Docker, Docker Compose
- **Documentation API** : Swagger / OpenAPI 3.0

## Fonctionnalités

- Authentification JWT (register, login, refresh token)
- Gestion des utilisateurs (profil, administration)
- Gestion des comptes bancaires (courant, épargne)
- Transactions (dépôt, retrait, virement)
- Génération de reçus PDF
- RBAC (USER / ADMIN)

## Lancer le projet en local

### Avec Docker (recommandé)

\`\`\`bash
docker compose up --build
\`\`\`

L'application sera accessible sur `http://localhost:8080/api/v1`

### Documentation API

Une fois l'application lancée :
\`\`\`
http://localhost:8080/api/v1/swagger-ui/index.html
\`\`\`

## Architecture

Architecture monolithique modulaire, microservices-ready, suivant les principes DevSecOps :
sécurité intégrée à chaque étape (code, dépendances, image Docker, infrastructure).