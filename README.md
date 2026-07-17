# Competition Portal API

Spring Boot REST API for operating reusable functional-fitness competitions. The first implementation is branded as **SIVARFEST**, while the domain model remains competition-agnostic for future events.

## Current scope

Implemented:

- JWT authentication using an HTTP-only cookie
- Admin and athlete roles
- Competition CRUD and public competition lookup by slug
- Dynamic category CRUD and public category listing
- Competition-athlete CRUD and public athlete listing
- Event/workout CRUD and public event listing
- Validation, DTO separation, exception handling, and development seed data

Planned but not implemented yet:

- Heats and lane/station assignments
- Athlete check-in
- Score entry, normalization, validation, publishing, and locking
- Event and overall leaderboards
- Athlete dashboard and credential management
- Announcements, sponsors, media, and uploads
- Online registration and payments (Phase 2)

See [docs/implementation-status.md](docs/implementation-status.md) for the detailed status.

## Technology

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Security
- Spring Data JPA
- MySQL 8
- JWT (`jjwt`)
- Maven
- Lombok

Flyway dependencies are present, but migrations are currently disabled while the schema is evolving. Development uses Hibernate schema updates.

## Related frontend

The Next.js client is maintained separately in [`sivarfest-web`](https://github.com/V4LERIAN0/sivarfest-web).

Development topology:

```text
Next.js frontend  http://localhost:3000
Spring Boot API   http://localhost:8081/api
MySQL database    competition_portal_db
```

## Local setup

### Requirements

- JDK 21 or newer
- MySQL 8
- Maven, or the included Maven Wrapper

### Database

Create the development database:

```sql
CREATE DATABASE competition_portal_db;
```

Copy:

```text
src/main/resources/application-dev.example.properties
```

to:

```text
src/main/resources/application-dev.properties
```

Then set your local database credentials and replace the example JWT secret with a long random value. The real development properties file is intentionally ignored by Git.

### Run

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

The API starts at `http://localhost:8081` under the active `dev` profile.

## Development account

The development profile creates an admin account if one does not exist. These credentials are for local development only and must be replaced or disabled before deployment.

```text
Email: admin@sivarfest.fit
Password: Admin123!
```

## Implemented endpoints

Authentication:

```text
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
```

Public:

```text
GET /api/public/competitions/{slug}
GET /api/public/competitions/{slug}/categories
GET /api/public/competitions/{slug}/athletes
GET /api/public/competitions/{slug}/events
```

Admin CRUD:

```text
/api/admin/competitions
/api/admin/competitions/{competitionId}/categories
/api/admin/competitions/{competitionId}/athletes
/api/admin/competitions/{competitionId}/events
```

## Configuration policy

Current development settings:

```properties
spring.jpa.hibernate.ddl-auto=update
spring.flyway.enabled=false
```

Before production, the intended direction is to introduce versioned migrations, enable Flyway, and change Hibernate to schema validation.

## Security notes

- Never commit `application-dev.properties`, `.env` files, passwords, or production JWT secrets.
- Development credentials must not be retained in production.
- Cookie security, CSRF strategy, allowed origins, and HTTPS settings must be reviewed before deployment.
- Real athlete, registration, and payment data must never be added as repository fixtures.

## Product direction

The backend is the source of truth for competition rules. Official score normalization, ranking, placement points, tie-breaking, heat generation, check-in rules, publishing, and locking belong here rather than in the frontend.