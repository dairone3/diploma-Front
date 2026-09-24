# HTTP API reference — response shapes

Base URL (local dev): `http://localhost:8787/api`
(The router also accepts paths without the `/api` prefix, but use `/api` to be safe.)

- **Content type:** all bodies are JSON (`Content-Type: application/json`).
- **Auth:** every route except `POST /api/auth/register` and `POST /api/auth/login`
  requires a header: `Authorization: Bearer <token>`.
- **Error shape (all failures):** `{ "error": "<message>" }` with the matching
  HTTP status (400/401/403/404/409).
- **Timestamps:** every `createdAt` is an **ISO 8601 string** (e.g. `"2026-07-20T12:00:00.000Z"`).
- **IDs:** every `id` / `userId` is an **integer**.

---

## AUTH

### `POST /api/auth/register`  (public)
Create a user account.

Request:
```json
{ "login": "alice", "password": "secret", "name": "Alice" }
```
Response `201`:
```json
{ "id": 1, "login": "alice", "name": "Alice", "createdAt": "2026-07-20T12:00:00.000Z" }
```
- No password is ever returned. **No token is returned here** — call login next.
- `409` if the login is taken, `400` if login/password missing.
- ⚠️ Note: the code comment in `register.post.ts` mentions a `tripId` field, but
  the actual response (the `mapUser` shaper) does **not** include it. The real
  shape is exactly the four fields above.

### `POST /api/auth/login`  (public)
Exchange credentials for a JWT.

Request:
```json
{ "login": "alice", "password": "secret" }
```
Response `200`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.<...>.<...>",
  "user": { "id": 1, "login": "alice", "name": "Alice", "createdAt": "2026-07-20T12:00:00.000Z" }
}
```
- `token` is a **JWT string** (it internally encodes `userId`, expires in 1 hour).
- `401` on wrong credentials (same message whether the user or password is wrong).

### How auth is sent on every other request
Put the token in the header — **not** in the body:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.<...>
```
The server verifies the token and derives the caller's `userId` from it. You
**never send `userId` or `created_by` yourself** — for creates (e.g. a trip),
the server stamps `created_by` from the token automatically. `created_by` is an
internal column and is **not** returned in any response.

---

## TRIPS

### `GET /api/trips`  — list the caller's trips
Yes, it exists. Returns **only** trips the caller created or is a member of,
newest first. Each item is a **summary with counts — no coordinates**.

Response `200`:
```json
[
  {
    "id": 7,
    "name": "Euro Tour",
    "startDate": "2026-07-15T00:00:00.000Z",
    "endDate": "2026-07-20T00:00:00.000Z",
    "destinationsCount": 2,
    "usersCount": 1,
    "createdAt": "2026-07-20T12:00:00.000Z"
  }
]
```
- `startDate` / `endDate` are ISO strings or `null`.
- **No lat/lng here** — only `destinationsCount` and `usersCount`. To get
  coordinates, fetch the single trip below.

### `GET /api/trips/{tripId}`  — one full trip
Returns a **single merged object** (NOT `{ trip, points }`). Destinations,
members and points are nested arrays on the trip.

Response `200`:
```json
{
  "id": 7,
  "name": "Euro Tour",
  "startDate": "2026-07-15T00:00:00.000Z",
  "endDate": "2026-07-20T00:00:00.000Z",
  "destination": [
    { "id": 3, "name": "Paris", "latitude": 48.8566, "longitude": 2.3522, "order": 0, "createdAt": "2026-07-20T12:00:00.000Z" },
    { "id": 4, "name": "Rome",  "latitude": 41.9028, "longitude": 12.4964, "order": 1, "createdAt": "2026-07-20T12:00:00.000Z" }
  ],
  "users": [
    { "userId": 1 }
  ],
  "points": [
    { "id": 10, "name": "Eiffel Tower", "latitude": 48.8584, "longitude": 2.2945, "createdAt": "2026-07-20T12:00:00.000Z" }
  ],
  "createdAt": "2026-07-20T12:00:00.000Z"
}
```

**Where do lat/lng come from?** Two separate places:
- **`destination[]`** — the trip's ordered stops (table `trips_cities`); each has
  `latitude`, `longitude`, and an `order` (0-based sequence).
- **`points[]`** — arbitrary map pins (table `points`); each has `latitude`,
  `longitude`, but **no `order`**.

The field is named `destination` (singular) but it's an **array**. `users` is an
array of `{ userId }` (the member ids; the creator is included as a member).
Errors: `400` bad id, `403` not your trip, `404` missing.

> Other trip routes (for completeness): `POST /api/trips` (create),
> `PATCH /api/trips/{tripId}` (update name/dates), `DELETE /api/trips/{tripId}` (→ `204`).
> `POST` and `PATCH` return the same full merged object as `GET /api/trips/{tripId}`.

---

## POINTS

All point routes are nested under a trip and require trip access.
**The point JSON does NOT include `trip_id`** — the trip is identified by the URL.
(`trip_id` is an integer column in the DB, but it is not returned in responses.)
`id` is an integer, `name` is a string (defaults to `""` if omitted on create).

### `GET /api/trips/{tripId}/points`  — list (newest first)
Response `200`:
```json
[
  { "id": 10, "name": "Eiffel Tower", "latitude": 48.8584, "longitude": 2.2945, "createdAt": "2026-07-20T12:00:00.000Z" }
]
```

### `GET /api/trips/{tripId}/points/{pointId}`  — one point
Response `200`:
```json
{ "id": 10, "name": "Eiffel Tower", "latitude": 48.8584, "longitude": 2.2945, "createdAt": "2026-07-20T12:00:00.000Z" }
```
`404` if the point isn't in that trip.

### `POST /api/trips/{tripId}/points`  — create
Request (`name` optional; `latitude`/`longitude` required numbers):
```json
{ "name": "Eiffel Tower", "latitude": 48.8584, "longitude": 2.2945 }
```
Response `201`:
```json
{ "id": 10, "name": "Eiffel Tower", "latitude": 48.8584, "longitude": 2.2945, "createdAt": "2026-07-20T12:00:00.000Z" }
```
`400` invalid coords/id, `404` trip missing.

### `PATCH /api/trips/{tripId}/points/{pointId}`  — partial update
Request (all optional): `{ "name"?, "latitude"?, "longitude"? }`
Response `200`: the full updated point (same shape as GET one).
`400` if no updatable fields / bad types, `404` if not in this trip.

### `DELETE /api/trips/{tripId}/points/{pointId}`
Response `204` (empty body). `404` if not in this trip.

---

## Quick answers to the four questions

1. **`GET /api/trips` (list)** — exists. Returns an array of trip **summaries**:
   `{ id, name, startDate, endDate, destinationsCount, usersCount, createdAt }`.
   **No coordinates** — only counts.

2. **`GET /api/trips/{tripId}`** — a **single merged object** (not
   `{ trip, points }`). Coordinates come from two nested arrays: `destination[]`
   (lat/lng/order, from `trips_cities`) and `points[]` (lat/lng, from `points`).

3. **Points** — paths `GET/POST/PATCH` `…/points[/id]` return
   `{ id, name, latitude, longitude, createdAt }`; `DELETE` returns `204`.
   `id` is an integer, `name` a string. `trip_id` is an integer **in the DB** but
   is **not** included in the JSON (it's in the URL).

4. **Auth** — the routes are `POST /api/auth/register` and `POST /api/auth/login`
   (note the `/auth` segment). Register returns the user
   `{ id, login, name, createdAt }` (no token). Login returns
   `{ token, user }` where `token` is a **JWT string** and `user.id` is the numeric
   user id. Send the token on every other request as the header
   `Authorization: Bearer <token>`. You never send `created_by` — the server sets
   it from the token, and it is not exposed in responses.

