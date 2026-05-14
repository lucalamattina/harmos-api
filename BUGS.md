# BUGS.md — Bugs found during test writing

**Date:** 2026-05-13  
**Branch:** `fix/concerns-resolution`  
**Found while:** Generating unit tests for the CONCERNS resolution changes

---

## Bug 1 — `UserController.createResetToken` calls wrong service method (email never sent) ✓ FIXED

**Severity:** High  
**File:** [src/main/kotlin/ar/edu/itba/harmos/app/controller/UserController.kt](src/main/kotlin/ar/edu/itba/harmos/app/controller/UserController.kt#L159-L162)

### Description
`POST /users/create-reset-token` calls `appUserService.createPasswordResetToken(email)` instead of `appUserService.createPasswordResetTokenForUser(email)`.

- `createPasswordResetToken` only creates and persists a token, returning a nullable `String`. The return value is **silently discarded**.
- `createPasswordResetTokenForUser` creates the token AND sends the reset-link email.

As a result, calling this endpoint **never sends a password-reset email** to the user.

Additionally, the response always says `"Password reset email sent"` regardless of whether the user exists in the database — a misleading success message.

### Fix
```kotlin
// UserController.kt line 160 — replace:
appUserService.createPasswordResetToken(email)
// with:
appUserService.createPasswordResetTokenForUser(email)
```

---

## Bug 2 — `ReportService.createReportWithFile` notification condition is always false

**Severity:** Medium  
**File:** [src/main/kotlin/ar/edu/itba/harmos/services/ReportService.kt](src/main/kotlin/ar/edu/itba/harmos/services/ReportService.kt#L139)

### Description
After saving a report, the service attempts to notify the report's doctor if the requester is a different user:

```kotlin
if (savedReport.doctor.id != doctor.id) {
```

Here `doctor` is the `AppUser` passed into `createReportWithFile`, and `savedReport.doctor` is the same object (set directly from `doctor` during report construction on line 128–134). Because both references point to the same object, `savedReport.doctor.id == doctor.id` is **always true**, so the notification block is **never entered**.

In-app notifications for report creation are therefore **never created**.

### Fix
The condition should compare against the *report's original owner* (the doctor who will receive the notification) rather than the current actor. Clarify intent:
- If the goal is "notify the doctor when someone else creates a report for their patient," a different design is needed (e.g. notify patient's assigned doctors who are NOT the creator).
- At minimum, the dead `if` block should either be removed or replaced with the correct business rule.

---

## Bug 3 — `ForgotPasswordRequest.isValid()` and `getValidationError()` are inconsistent ✓ FIXED

**Severity:** Low (maintenance hazard, not a security issue)  
**File:** [src/main/kotlin/ar/edu/itba/harmos/dtos/requests/ForgotPasswordRequest.kt](src/main/kotlin/ar/edu/itba/harmos/dtos/requests/ForgotPasswordRequest.kt)

### Description
`isValid()` applies a regex that requires a TLD of at least 2 characters:

```
^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$
```

`getValidationError()` does NOT use the same regex — it performs only basic structural checks (presence of `@`, dot, etc.).

**Example:** `user@test.c`
- `getValidationError()` → `null` (no error detected)
- `isValid()` → `false` (fails TLD regex)

In `UserController.forgotPassword`, the `!isValid()` branch logs `getValidationError() ?: "Datos inválidos"`. For the `user@test.c` case this logs `"Datos inválidos"` even though `getValidationError()` returned null — the error label is wrong.

**Runtime impact:** Both branches return the same 200 generic response, so there is no security exposure. However the inconsistency can cause confusing log messages and maintenance mistakes.

### Fix
Unify validation logic: either have `getValidationError()` also apply the regex, or have `isValid()` delegate to `getValidationError()`:

```kotlin
fun isValid(): Boolean = getValidationError() == null
```

---

## Bug 4 — `@Valid` on `forgotPassword` defeats the anti-oracle behavior (FIXED)

**Severity:** Medium (security — information leakage)  
**File:** [src/main/kotlin/ar/edu/itba/harmos/app/controller/UserController.kt](src/main/kotlin/ar/edu/itba/harmos/app/controller/UserController.kt#L129)  
**Status:** Fixed in this branch

### Description
`POST /users/forgot-password` had `@Valid @RequestBody ForgotPasswordRequest` where `ForgotPasswordRequest` carries `@field:Email` Bean Validation.

When an invalid email like `"not-an-email"` is submitted, Spring's validation layer fires **before** the controller method runs and returns `400 Bad Request`. This means callers can distinguish:

- `400` → email format is invalid  
- `200` → email format is valid (whether or not the user exists)

This is an **email-format oracle**: an attacker can use the `/forgot-password` endpoint to test whether a string is a syntactically valid email address, leaking information.

The controller already has the correct anti-oracle logic at line 130 (`if (!forgotPasswordRequest.isValid())` → return generic 200), but `@Valid` prevented that code from ever being reached for invalid emails.

### Fix Applied
Removed `@Valid` from the `forgotPassword` handler parameter. The controller's own `isValid()` check now handles validation and always returns the generic 200 response.

```kotlin
// Before (leaks info):
fun forgotPassword(@Valid @RequestBody forgotPasswordRequest: ForgotPasswordRequest)

// After (no oracle):
fun forgotPassword(@RequestBody forgotPasswordRequest: ForgotPasswordRequest)
```

---

## Summary

| # | File | Severity | Status | Impact |
|---|------|----------|--------|--------|
| 1 | `UserController.kt:160` | High | **Fixed** | Password-reset emails never sent via `/create-reset-token` |
| 2 | `ReportService.kt:139` | Medium | Open (manual) | In-app notifications on report creation never fired — requires design decision |
| 3 | `ForgotPasswordRequest.kt` | Low | **Fixed** | Inconsistent validation — `isValid()` now delegates to `getValidationError()` |
| 4 | `UserController.kt:129` | Medium | **Fixed** | `@Valid` leaked email-format oracle on `/forgot-password` |
