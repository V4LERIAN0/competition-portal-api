# Competition Portal API — Implementation Status

Last reviewed: 2026-07-16

## Status legend

- **Implemented**: working source exists with controllers/services/entities as applicable.
- **Partial**: some supporting source exists, but the complete workflow does not.
- **Scaffold only**: package/file structure exists, but implementation files are empty.
- **Planned**: defined in the product specification but not started.

## Module status

| Module | Status | Notes |
|---|---|---|
| Project configuration | Implemented | Java 21, Spring Boot, MySQL, dev profile on port 8081. |
| Authentication | Implemented | JWT issued in an HTTP-only cookie; login, logout, and current-user endpoints. |
| Authorization | Implemented | Admin and athlete route protection is configured. Judge is modeled for future use. |
| User accounts | Partial | Entity and repository exist; full credential-management workflow is not implemented. |
| Competitions | Implemented | Admin CRUD, archival behavior, public lookup by slug. |
| Landing content | Partial | Entity/repository/DTO exist; dedicated management workflow is incomplete. |
| Categories | Implemented | Admin CRUD and public listing, dynamically scoped to a competition. |
| Athletes | Implemented | Admin CRUD, public/private DTO separation, category association. |
| Athlete dashboard | Scaffold only | Response shape exists, but the dashboard controller/workflow is empty. |
| Events/workouts | Implemented | Admin CRUD and public listing with score type and ranking direction. |
| Heats | Scaffold only | DTO, entity, repository, controller, and service files exist but are empty. |
| Athlete check-in | Scaffold only | Planned files exist but contain no implementation. |
| Scores | Scaffold only | Score lifecycle, normalization, validation, and batch entry are not implemented. |
| Event leaderboard | Scaffold only | Ranking DTO/service/controller files exist but are empty. |
| Overall leaderboard | Scaffold only | Placement-point aggregation and tie-break logic are not implemented. |
| Announcements | Scaffold only | Module structure exists without implementation. |
| Sponsors | Scaffold only | Module structure exists without implementation. |
| Media/gallery | Scaffold only | Module structure exists without implementation. |
| File uploads | Scaffold only | Storage/controller interfaces are not implemented. |
| Online registration | Planned (Phase 2) | Registration status exists on Competition; applications are not modeled yet. |
| Payments | Planned (Phase 2) | No payment provider or transaction model exists. |
| Automated testing | Minimal | Only an application-context smoke test currently exists. |

## Implemented domain entities

- `Competition`
- `CompetitionLandingContent`
- `CompetitionCategory`
- `CompetitionAthlete`
- `CompetitionEvent`
- `UserAccount`

## Important domain distinctions

Competition status describes lifecycle:

```text
DRAFT → PUBLISHED → LIVE → FINISHED → ARCHIVED
```

Visibility describes whether the public may see the competition:

```text
PRIVATE | PUBLIC
```

Registration status describes whether applications may be accepted:

```text
OPEN | CLOSED | WAITLIST | FULL
```

These dimensions are intentionally separate. Backend rules should eventually prevent invalid combinations.

## Current public workflow

1. The frontend requests a competition using its configured slug.
2. Only public competition data is returned.
3. Categories, athletes, and published/visible events are requested independently.
4. Private athlete contact information remains excluded from public DTOs.

## Recommended development order

1. Complete frontend administration for categories.
2. Complete frontend administration for athletes.
3. Complete frontend administration for events.
4. Add integration tests for the stable CRUD modules.
5. Implement heat entities, assignments, and manual management.
6. Implement random heat generation.
7. Implement score entry and normalization.
8. Implement event leaderboards.
9. Implement overall placement-point leaderboards and tie-breaks.
10. Generate later heats from current standings.
11. Implement athlete credentials, dashboard, and check-in.
12. Add announcements and sponsors.
13. Add publishing/locking and audit behavior.
14. Add registration and payments only after competition operations are stable.

## Production readiness gaps

- Replace development administrator seeding and credentials.
- Add comprehensive tests for authorization and business rules.
- Decide and implement the final CSRF/cookie security strategy.
- Use environment-specific allowed origins and secure cookies.
- Introduce versioned database migrations before storing production competition data.
- Add score and ranking auditability.
- Add monitoring, backups, and deployment documentation.