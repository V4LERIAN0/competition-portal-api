# Competition Portal API — implementation status

Reviewed: 2026-09-11. This describes source, not confirmation of a production deployment.

| Area | Current implementation |
|---|---|
| Competitions and categories | Admin management; public visibility and lifecycle rules. |
| Athletes and roster | Admin registrations; public roster and individual public profiles; optional profile fields and explicit height/weight publication. |
| Athlete accounts | Admin activation preview, editable usernames, collision detection, selected activation and explicit reset. No automatic account activation during deployment. |
| Authentication | Email or username login, cookie JWT, mandatory password change, versioned session revocation on change/reset; disabled accounts rejected. |
| Athlete dashboard | Own identity/profile, published standings/results, own visible heats, lane, schedule and check-in window. |
| Athlete photos | JPG/PNG upload, 5 MB/20 MP limits, resized/re-encoded JPEG with metadata removed; public lookup requires a visible roster entry. |
| Workouts | Shared events with category overrides; category-aware validation, repetitions and capped scores. |
| Heats | Manual management, category isolation, balanced random generation, standings seeding and final eligibility. |
| Heat check-in | Athlete-owned check-in within the configured window; explicit CHECK_IN_OPEN override; assigned-judge/admin attendance confirmation. |
| Judges | Accounts, heat-position assignments, category scoring context, submitted scores and attendance confirmation. New/reset judge passwords require a change. |
| Scoring | Validation/publication/locking/rejection/reopening and audit trail; effective category configuration respected. |
| Leaderboards | Published event results and overall category placement standings; public athlete results use these visibility rules. |
| Announcements | Admin publish/hide, public notices and separate authenticated athlete/judge audiences. No email or push delivery. |
| Sponsors | Public frontend assets/configuration already meet this competition's needs; backend CMS remains scaffold. |
| Tests | 23 tests pass, including 8 new integration workflows for authentication, account activation, profiles/privacy, check-in, judge scope, photos and announcement visibility. |

## Still deferred

- Admin visual redesign (explicitly deferred by the user).
- Automated email reminders, email recovery and verified contact addresses. Imported placeholder emails must not be used for delivery; recovery is currently an explicit admin reset.
- Browser push/PWA/offline score submission. Device drafts are not submitted scores; a working connection is required to submit.
- Sponsor/media gallery CMS and a separate historical gallery. The existing sponsor section remains functional.
- Online registration, payments, reusable SaaS onboarding and multi-competition public routing (later phases).
- Versioned production migrations: current deployments use the existing Hibernate `update` policy. Do not use `create` or `create-drop` on production.

## New API surfaces

| Access | Method/path | Purpose |
|---|---|---|
| Authenticated | POST `/api/auth/change-password` | Verify current password, change it, clear the requirement, invalidate older tokens and issue a fresh cookie. |
| Admin | GET/POST `/api/admin/competitions/{id}/athlete-access` | Review and activate selected accounts. |
| Admin | POST `/api/admin/athletes/{id}/reset-access` | Explicit recovery/reset; old tokens revoked. |
| Athlete | GET `/api/athlete/me` | Own dashboard. |
| Athlete | PUT `/api/athlete/me/profile` | Own optional profile fields; cannot change name, division, bib or another athlete. |
| Athlete | POST/DELETE `/api/athlete/me/photo` | Upload/remove own public profile image. |
| Public | GET `/api/public/competitions/{slug}/athletes/{id}` | Roster profile and published results; no email, phone, date of birth or credentials. |
| Athlete | POST `/api/athlete/me/heats/{assignmentId}/check-in` | Own attendance in an open window. |
| Judge | POST `/api/judge/assignments/{id}/check-in` | Confirm only an assigned athlete. |
| Admin | POST `/api/admin/heat-assignments/{id}/check-in` | Manual attendance confirmation. |
| Admin | GET/POST `/api/admin/competitions/{id}/announcements` | List/publish notices. |
| Admin | DELETE `/api/admin/announcements/{id}` | Hide a notice, preserving its record. |
| Public | GET `/api/public/competitions/{slug}/announcements` | Published public notices only. |
| Athlete/Judge | GET `/api/athlete/me/announcements`, `/api/judge/announcements` | Own competition's public and audience-specific notices. |

## Account configuration and rollout

Activation JSON accepts `athletes: [{athleteId, username}]` plus either `useNameBasedPassword: true` or `temporaryPassword` (4–72 characters, at most 72 UTF-8 bytes). The name-based option uses the first name in lowercase without accents plus `sf!`. A newly chosen personal password must have 12–72 characters and at most 72 UTF-8 bytes, and differ from the current password.

`must_change_password` is stored in the database. An unchanged initial credential remains restricted on every login, not merely the first login. The API blocks protected data/actions until the change succeeds. `token_version` prevents reuse of older tokens after a password change/reset.

A predictable initial password still permits first-login impersonation; this follows the user's accepted temporary rollout policy. Review compound names in the activation preview. Repeating bulk activation does not reset already configured accounts. Existing admin/judge accounts are preserved; resetting or creating a judge account activates the password-change requirement.

Schema additions: `user_accounts.username` (unique, nullable), `must_change_password` (false by default), `token_version` (0 by default), `competition_athletes.show_body_metrics` (false by default), and `competition_announcements`. Deploy both repositories and test one account before activating the roster.

Photos use `ATHLETE_PHOTO_DIRECTORY` or `./uploads/athlete-photos`. With the existing API service working directory, the default is `/opt/sivarfest/uploads/athlete-photos`. Include it in backups. Replaced/removed photo URLs are no longer served when no active public athlete references them; unreferenced files can be cleaned during later storage maintenance.

SIVARFEST service: `sivarfest-api`, JAR `/opt/sivarfest/releases/competition-portal-api.jar`, Java 21, port 8081. `wodnsivar` is a separate application and is not a deployment target for this repository.
