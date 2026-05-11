# Frontend Changes Required

> **Instructions for subagents:** After completing your task, fill in the section corresponding to your domain. If your changes have no frontend impact, write "No frontend changes required." under your section. Do NOT leave sections blank.

---

## [Agent A] Security Changes — API Contract Impacts

### 1. `POST /api/users/create-reset-token` — response body changed (BREAKING)

**Before:** `200 OK` with `{ "token": "<raw_jwt_token>" }` when user exists; `404 Not Found` when user does not exist.

**After:** Always returns `200 OK` with `{ "message": "Password reset email sent" }` regardless of whether the user exists.

**Frontend action required:** Do not read or display the `token` field from this response. The reset link is now generated server-side and delivered via email only. Remove any UI logic that branches on 404 from this endpoint or that extracts the `token` field.

---

### 2. `POST /api/users/forgot-password` — 404 eliminated (BREAKING)

**Before:** `404 Not Found` with `{ "error": "No se encontró un usuario con este email" }` when the email was not registered.

**After:** Always returns `200 OK` with `{ "message": "If the email is registered, a reset link will be sent" }` regardless of whether the email exists.

**Frontend action required:** Remove any logic that shows "email not found" based on a 404 from this endpoint. Display a generic success message for all 200 responses (e.g., "If your email is registered you will receive a reset link shortly"). The only remaining non-200 response is `400 Bad Request` for malformed request payloads.

---

### 3. File upload — stricter document validation (potentially BREAKING)

**Before:** `CloudinaryService.isValidDocument` accepted a file if its content-type **OR** its extension matched the allowlist (OR logic).

**After:** Both content-type **AND** extension must match the allowlist (AND logic). A file with a valid extension but wrong MIME type (or vice versa) is now rejected.

**Affected endpoints:** Any endpoint that accepts document uploads (PDF, Word, Excel, PowerPoint, plain text).

**Frontend action required:** Ensure file pickers enforce both the correct extension and the correct MIME type simultaneously. Files previously accepted because only one of the two checks passed will now be rejected with `400 Bad Request`. Valid combinations:

| Extension | Required Content-Type |
|-----------|----------------------|
| `.pdf` | `application/pdf` |
| `.doc` | `application/msword` |
| `.docx` | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |
| `.txt` | `text/plain` |
| `.xls` | `application/vnd.ms-excel` |
| `.xlsx` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| `.ppt` | `application/vnd.ms-powerpoint` |
| `.pptx` | `application/vnd.openxmlformats-officedocument.presentationml.presentation` |

---

## [Agent B] Tech Debt & Data Integrity — API Contract Impacts

### FetchType.LAZY on Patient.doctors and AppUser.roles

Both `Patient.doctors` (`@ManyToMany`) and `AppUser.roles` (`@ManyToMany`) were changed from `FetchType.EAGER` to `FetchType.LAZY`. This is **not a breaking API contract change** — the database schema and endpoint URLs are unchanged. However, teams consuming these endpoints should verify:

- Any response DTO that serializes the `doctors` list on a Patient or the `roles` set on an AppUser must be loaded within an active Hibernate session (i.e., inside a `@Transactional` boundary or via a DTO projection). If serialization happens outside the session a `LazyInitializationException` will be thrown. Check all controller methods that return `Patient` or `AppUser` directly to ensure DTOs properly access these collections within the transaction scope.
- No frontend payload changes are expected if the backend DTOs already eagerly project the needed fields.

### DataInitializer: seed credential changes

All developer personal email addresses (`alejandro.rolandelli@gmail.com`, `dorado.tomas@gmail.com`, `noreplyharmos@gmail.com`) have been replaced with placeholder addresses (`admin1@harmos.example.com`, `admin2@harmos.example.com`, `superuser@harmos.example.com`). Doctor seed accounts use `doctor1@harmos.example.com` through `doctor30@harmos.example.com`. The default seed password has changed from `"password"` to `"dev-password-change-me"`.

**Action required for developers:** When running with `app.database.repopulate=true` on a fresh database, update any saved Postman collections, integration test fixtures, or local login bookmarks to use the new placeholder credentials.

### Notifications seeding: missing saveAll fix

`initializeNotifications()` previously built a list of `Notification` objects but never persisted them (missing `notificationRepository.saveAll()`). This has been fixed. On the next fresh database repopulation (`app.database.repopulate=true`), notifications will now appear for all seeded users and announcements. **No frontend contract change** — the notifications API endpoints are unchanged. Developers who previously saw an empty notifications feed during local testing should now see seed data.

### Schedule seeding: JDBC workaround removed

`initializeSchedules()` was inserting schedules via raw `JdbcTemplate` SQL instead of the JPA repository. This has been replaced with `scheduleRepository.saveAll()`. No API contract change; the Schedule entity and database schema are unchanged.

---

## [Agent C] Authorization & Exception Handling — API Contract Impacts

### PUT /announcements/{id} and DELETE /announcements/{id}
- **Before:** Any authenticated user could edit or delete any announcement.
- **After:** Only the announcement owner (`createdBy.id == currentUser.id`) or an ADMINISTRATOR may call these endpoints. Non-owners/non-admins now receive `403 Forbidden` with body `{"error": "Forbidden: you do not own this announcement"}`.
- **Frontend work required:**
  - Hide the Edit and Delete buttons/actions for announcements when the logged-in user is neither the owner nor an ADMINISTRATOR.
  - Handle `403` responses gracefully (e.g., show "You do not have permission to perform this action").

### POST /patients
- **Before:** Any authenticated user could create a patient.
- **After:** Only ADMINISTRATOR users may create patients. Non-admins now receive `403 Forbidden` with body `{"error": "Forbidden: only administrators can create patients"}`.
- **Frontend work required:**
  - Hide the "Create Patient" button/form for non-ADMINISTRATOR users.
  - Handle `403` responses gracefully on the patient creation flow.

### PUT /patients/{id} and PUT /announcements/{id} — not-found handling
- **Before:** If the entity did not exist, the service threw `RuntimeException` which propagated as `500 Internal Server Error`.
- **After:** Both endpoints now catch that `RuntimeException` and return `404 Not Found` with body `{"error": "Patient not found"}` or `{"error": "Announcement not found"}`.
- **Frontend work required:**
  - Update error handling for PUT calls to these endpoints: treat `404` as "record no longer exists" (e.g., refresh the list and show a toast/message) rather than treating it as an unexpected server error.

### Global Exception Handler
- **New behavior:** A `@ControllerAdvice` (`GlobalExceptionHandler`) now catches unhandled `RuntimeException`, `NoSuchElementException`, and `MethodArgumentNotValidException` across the entire API.
  - Unhandled `RuntimeException` → `500` with `{"error": "An unexpected error occurred"}`
  - `NoSuchElementException` → `404` with `{"error": "Resource not found"}`
  - `MethodArgumentNotValidException` → `400` with `{"errors": {"fieldName": "message", ...}}`
- **Frontend work required:**
  - Parse the `error` field (string) on non-2xx responses for human-readable error messages.
  - For `400` validation errors, parse the `errors` field (object) to display per-field validation feedback.

---

## [Agent D] Code Quality & Report Refactoring — API Contract Impacts

### sortBy / sortDirection now functional

Both `GET /reports` and `GET /reports/all` previously accepted `sortBy` and `sortDirection` query params but silently ignored them (always returning results ordered by date descending). These params are now wired through to the database query.

- **`sortBy`** — allowed values: `id`, `title`, `date`. Any other value falls back to `date`.
- **`sortDirection`** — allowed values: `asc`, `desc` (case-insensitive). Any other value falls back to `desc`.

**Frontend action recommended:** Verify existing calls that omit `sortBy`/`sortDirection` still behave as expected — the default is `date DESC`, which is the same as the previous implicit behaviour. Consider surfacing sort controls in the reports list UI to let users sort by title or date.

### No breaking contract changes

All endpoint signatures (`GET /reports`, `GET /reports/all`, `GET /reports/{id}`, `POST /reports`, `PUT /reports/{id}`, `DELETE /reports/{id}`, `GET /reports/{id}/file`) are unchanged. Request and response shapes are identical. The combinatorial `when` chain in the service was replaced with composable JPA Specifications; all four original filter dimensions (`patientId`, `specialtyId`, `doctorId`, `title`) are fully preserved and behave identically.

---

## [Agent E] Validation & Entity Completeness — API Contract Impacts

### Bean Validation Now Active

All request DTOs now carry JSR-303 (`javax.validation`) annotations. Because every controller is already annotated with `@Validated` at the class level, validation triggers automatically once `@Valid` is added to each `@RequestBody` parameter. When validation fails, Spring returns **HTTP 400** with a structured error body — for example:

```json
{
  "timestamp": "...",
  "status": 400,
  "errors": {
    "email": "Email must be a valid email address",
    "password": "Password must be at least 8 characters"
  }
}
```

#### Affected endpoints and validated fields

| Endpoint | DTO | Validated fields |
|----------|-----|-----------------|
| `POST /announcements` | `CreateAnnouncementRequest` | `title` (NotBlank, max 255), `content` (NotBlank) |
| `PUT /announcements/{id}` | `EditAnnouncementRequest` | `title` (max 255 if provided) |
| `POST /users` | `CreateAppUserRequest` | `email` (NotBlank, Email, max 255), `password` (NotBlank, min 8), `firstName` (NotBlank, max 255), `lastName` (NotBlank, max 255), `phone` (NotBlank, max 50) |
| `PUT /users/{id}` | `EditAppUserRequest` | `firstName` (max 255), `lastName` (max 255), `phone` (max 50) |
| `POST /users/forgot-password` | `ForgotPasswordRequest` | `email` (NotBlank, Email) |
| `POST /patients` | `CreatePatientRequest` | `firstName` (NotBlank, max 255), `lastName` (NotBlank, max 255), `phone` (NotBlank, max 50), `status` (NotNull) |
| `PUT /patients/{id}` | `EditPatientRequest` | `firstName` (max 255), `lastName` (max 255), `phone` (max 50) |
| `POST /specialties` | `CreateSpecialtyRequest` | `name` (NotBlank, max 255) |
| `PUT /specialties/{id}` | `EditSpecialtyRequest` | `name` (max 255 if provided) |
| `POST /schedules` | `CreateScheduleRequest` | `dayOfWeek` (NotNull), `hourFrom` (0–23), `minuteFrom` (0–59), `hourTo` (0–23), `minuteTo` (0–59), `durationMinutes` (Positive), `doctorUserId` (NotNull, Positive), `patientId` (NotNull, Positive) |

Note: `POST /reports` and `PUT /reports/{id}` use `@RequestParam` multipart fields instead of a JSON body DTO — the controller already performs manual validation, so no DTO-level `@Valid` applies there.

### New audit timestamp fields on entities

The following entities now expose `createdAt` and `updatedAt` fields in any JSON response that serializes them:

- **Announcement** — `createdAt` (ISO-8601, set on creation, never updated), `updatedAt` (ISO-8601, updated on every save)
- **Report** — same as above
- **Notification** — same as above

This is an **additive, non-breaking change**: existing response consumers can safely ignore the new fields. No request payload changes are required.

### Comment entity status

`Comment.kt` exists as a JPA entity (`@Table(name = "Comments")`) but has **no repository, service, or controller** wired up. The entity is inert — no API endpoints exist for it. Do not build any frontend UI for comments until the feature is fully implemented.

---

## Summary Table

| Domain | Breaking Change? | Frontend Work Required |
|--------|-----------------|----------------------|
| Security (Agent A) | Yes | Remove token from create-reset-token response; remove 404 branch on forgot-password; align file-upload MIME-type + extension validation |
| Tech Debt & Data Integrity (Agent B) | No | Verify DTOs including Patient.doctors and AppUser.roles load within transaction scope; update local dev login credentials to `admin1@harmos.example.com` / `dev-password-change-me` |
| Authorization & Exception Handling (Agent C) | Yes | Hide edit/delete buttons for announcements for non-owners; hide create-patient button for non-admins; handle 403 responses; update 500→404 error handling on PUT endpoints; parse `error`/`errors` fields on non-2xx responses |
| Code Quality & Report Refactoring (Agent D) | No | `sortBy` and `sortDirection` on `GET /reports` and `GET /reports/all` now take effect — verify existing calls; consider adding sort UI. No endpoint or payload changes. |
| Validation & Entity Completeness (Agent E) | No | POST/PUT endpoints now return 400 with `{"errors":{...}}` for invalid input (field-level messages); Announcement, Report, Notification responses gain new `createdAt`/`updatedAt` fields (additive); Comment feature has no endpoints — do not build UI for it |
