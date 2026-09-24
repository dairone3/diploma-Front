# Database Implementation Plan (Cloudflare D1 via REST)

How the Android app moves from local‑file storage to the Cloudflare backend
described in `API.md`, with three hard requirements:

1. **The database is the only storage.** There is no local/offline fallback in the
   end state — `FileAppStorage` and the file repositories are **removed** once the
   remote path works. Files are used only as a temporary scaffold so the build stays
   green mid‑migration.
2. **Singletons** — one shared `HttpClient`, `SessionUserStorage`, `BaseServerApi`, and
   `AppStorage`, created once and injected.
3. **Convert everything to our own domain types at the boundary** — the app's
   UI/logic only ever sees `Trip` / `MapPoint`. Wire models (DTOs) never leak past
   the data layer.

---

## 0. Guiding principles

- **One direction of dependency:** `UI → AppStorage (interface) → RemoteAppStorage → ServerApi → HttpClient → backend`.
- **DTO in, domain out:** every network response is deserialized into a DTO, then
  **mapped to a domain model** before returning. Every request maps domain → DTO.
- **The screens don't change** thanks to the `AppStorage` interface — only its
  implementation and (unavoidably) its *async signatures* change.
- **Build green via phases, DB‑only at the end.** `FileAppStorage` is kept
  compiling only until `RemoteAppStorage` is wired; then it is deleted. No runtime
  toggle between file and remote.

---

## 1. Reconcile the contract (`API.md` is the source of truth)

The current `Dtos.kt` is stale. The real backend differs in ways that force
conversion at the boundary:

| Concern | Backend (`API.md`) | App domain type | Conversion rule |
| --- | --- | --- | --- |
| Trip id | `Int` (autoincrement) | `Trip.id: Int` | direct; new/unsaved trip = `0`, storage/backend assigns the real id |
| Point id | `Int` | `MapPoint.id: Int` | direct; new/unsaved point = `0`, assigned on save |
| Dates | ISO‑8601 string | `Long?` epoch millis | parse ISO → millis; format millis → ISO |
| `createdAt` | ISO‑8601 string | `Long` epoch millis | parse ISO → millis |
| Point label | `name` | `MapPoint.title` | `name ↔ title` |
| Trip destinations | `destination[]` (ordered, `trips_cities`) | `Trip.destinations: List<Destination>` | map each element → `Destination(name, lat, lng, order)`; keep `order` |
| Auth | JWT `Authorization: Bearer` | — | held in `SessionUserStorage`, added by client |

**Ids are `Int` (resolved).** Domain models use `Int` ids matching the backend's
autoincrement integers. A **new/unsaved** entity has id `0`; the storage layer (and
later the backend) assigns the real id. To support this, `upsert` **returns the
stored entity** (with its assigned id) so callers can use it (e.g. navigation).

**No legacy/back-compat.** The app is not released, so old on-disk formats are not
supported. The `FileTripRepository` reads only the current `destinations` shape (the
flat `latitude`/`longitude` fallback is removed); file storage is temporary anyway
and is deleted in Phase E.

**A trip can have multiple destinations.** The domain model changes:
- Add a domain type `Destination(name: String, latitude: Double, longitude: Double,
  order: Int)` (id/dates optional — add only if the UI needs them).
- `Trip` gains `destinations: List<Destination>` (ordered by `order`). The trip's
  map camera centers on the **first** destination (`order = 0`); the rest are shown
  as additional stops.
- This is a **domain change with UI ripple** (not just a mapper detail):
  - `CreateTripScreen` must allow **adding/removing/reordering multiple
    destinations** (today it has a single Destination field).
  - `TripScreen` should reflect all destinations (e.g., markers/list), not just one.
  - `Trip`’s old single `latitude/longitude` are replaced by `destinations` (or kept
    as a computed convenience from `destinations.first()` for map centering).

---

## 2. Class design (from your sketch, translated to Kotlin)

Your pseudo‑code maps almost 1:1 onto the Kotlin layer. Naming follows your sketch:

| Your sketch | Kotlin equivalent | Role |
| --- | --- | --- |
| `PostTripDTO` | `PostTripDto` | request body for create/update |
| `ResponseTripDTO` | `ResponseTripDto` | response shape from the server |
| `ServerApi(hostUrl)` | `ServerApi(baseUrl, client)` | wraps `HttpClient`, one method per endpoint |
| `UserAuthSingleton.getInstance().getAccessToken()` | `SessionUserStorage` (singleton) | holds the JWT, provides the `Bearer` token |
| `httpClient` | Ktor `HttpClient` (one shared) | actual network engine |

**Request vs response DTOs (your `PostTripDTO` / `ResponseTripDTO` split):**
- `PostTripDto` — what the app sends (name, dates, destination, …). No id/createdAt.
- `ResponseTripDto` — what the server returns (adds `id`, `createdAt`, nested
  `destination[]` / `points[]`). This is the one that gets **mapped to `Trip`**.
- Same split for points: `PostPointDto` → `ResponsePointDto`.
- **Rule:** `ResponseTripDto`/`ResponsePointDto` are converted to `Trip`/`MapPoint`
  via `toDomain()` before leaving the data layer. The UI never sees a DTO.

**`BaseServerApi` (your class), sketched in Kotlin:**
```kotlin
class ServerApi(
    private val baseUrl: String = BackendConfig.baseUrl,
    private val client: HttpClient = AppHttp.client,   // one shared client
) {
    suspend fun createTrip(body: PostTripDto): ResponseTripDto = /* POST /trips → 201 */
    suspend fun getTrips(): List<ResponseTripSummaryDto> = /* GET /trips (auth) */
    suspend fun getTrip(tripId: Int): ResponseTripDto =    /* GET /trips/{id} */
    suspend fun deleteTrip(tripId: Int)                     /* DELETE → 204 */
    // + points endpoints
}
```
The `Authorization: Bearer <token>` header is added **once** by the shared client
(reading `SessionUserStorage`), so `getTrips`/`getTrip`/`createTrip`/`deleteTrip` don't each
repeat it — exactly your `UserAuthSingleton.getInstance().getAccessToken()` idea,
just centralized.

**Singleton for auth (your `UserAuthSingleton`):**
```kotlin
// Kotlin's `object` IS a singleton (no getInstance boilerplate needed).
object UserAuth {
    @Volatile var accessToken: String? = null   // thread-safe field (your `static`)
    @Volatile var userId: Int? = null
    fun isLoggedIn() = accessToken != null
    // backed by DataStore so the token survives process death
}
```
- **Two caveats to keep it correct** (else the classic bug bites):
  1. **Persist** the token (DataStore) — an in‑memory‑only singleton "logs out" on
     process death.
  2. **Thread‑safe** field access (`@Volatile`) — many coroutines read it.

**Wiring the singletons:**
```
Application (DimaApp)
  └─ AppContainer           ← creates the shared instances (app context only)
       ├─ AppHttp.client    ← ONE Ktor client (OkHttp + JSON + Bearer via UserAuth)
       ├─ UserAuth          ← the auth singleton (token/userId, persisted)
       ├─ ServerApi         ← ONE instance (uses the shared client)
       └─ AppStorage        ← ONE RemoteAppStorage(ServerApi)  (FileAppStorage kept as fallback)
```
- `AppStorage`/`BaseServerApi` are **created once and injected** into `AppNavigation`
  (still testable). `SessionUserStorage` is a true global singleton because the token is
  genuinely cross‑cutting — matching your sketch.

### Status‑code corrections vs your sketch (from `API.md`)
- **Create** trip/point → **201** 
- **Delete** → **204** 
- **GET / PATCH** → **200**.
- Ktor throws/So we check via `response.status`; failures surface as an app error
  state (Phase D), not a raw exception in the UI.

---

## 3. Phased plan (each phase builds & is reviewable)

### Phase A — Rewrite DTOs + mappers to match `API.md`  *(no UI impact)*
- Replace `Dtos.kt` with request/response shapes (your `PostTripDTO` / `ResponseTripDTO` split):
  - `ResponseTripSummaryDto` (`id, name, startDate, endDate, destinationsCount, usersCount, createdAt`).
  - `ResponseTripDto` (`id, name, startDate, endDate, destination[], users[], points[], createdAt`).
  - `DestinationDto` (`id, name, latitude, longitude, order, createdAt`).
  - `ResponsePointDto` (`id, name, latitude, longitude, createdAt`).
  - `PostTripDto`, `PostPointDto` (request bodies — no id/createdAt).
  - `AuthUserDto`, `LoginResponseDto`.
- Add an **ISO‑8601 ↔ epoch‑millis** helper (`minSdk 24`‑safe).
- Add **mappers** `toDomain()` producing `Trip` / `MapPoint` (apply every rule from
  §1: id→String, name→title, ISO→millis, `destination[]` → `List<Destination>`
  ordered by `order`). The UI never sees a DTO.
- **Verify:** unit tests for mappers. `./gradlew :app:testDebugUnitTest`.

### Phase B — Singleton auth + shared client  *(no UI impact)*
- Implement **`SessionUserStorage`** (your `UserAuthSingleton`): `object` with `@Volatile`
  `accessToken`/`userId`, backed by **DataStore** (`load()` on startup, `save()`, `clear()`).
- Build the **single `HttpClient`** (`AppHttp.client`) with:
  - `ContentNegotiation` (JSON, `ignoreUnknownKeys = true`),
  - **`Auth` bearer plugin** reading `UserAuth.accessToken` → adds the header to
    every request automatically (register/login don't need it).
- **Verify:** app builds; a temporary debug login logs a token.

### Phase C — Full `BaseServerApi` per `API.md`  *(no UI impact)*
- Implement all endpoints on **`BaseServerApi`**, returning **DTOs**:
  - Auth: `register`, `login` (stores token/userId in `SessionUserStorage`).
  - Trips: `getTrips` (summaries), `getTrip`, `createTrip`, `updateTrip`, `deleteTrip`.
  - Points: `getPoints`, `getPoint`, `createPoint`, `updatePoint`, `deletePoint`.
- Check the correct status per `API.md` (create → 201, delete → 204, else 200).
- **Verify:** hit each route from `backend-requests.http`; adjust DTOs if shapes differ.

### Phase D — Make `AppStorage` async  *(UI ripples — the big one)*
- Convert the interface to **suspend** (network can't be synchronous):
  ```kotlin
  interface AppStorage {
      suspend fun getTrips(): List<Trip>
      suspend fun getTrip(tripId: String): Trip?
      suspend fun upsertTrip(trip: Trip): Trip      // returns server-assigned trip
      suspend fun deleteTrip(tripId: String)
      suspend fun getPoints(tripId: String): List<MapPoint>
      suspend fun upsertPoint(tripId: String, point: MapPoint): MapPoint
      suspend fun deletePoint(tripId: String, pointId: String)
  }
  ```
- Update **both** implementations:
  - `FileAppStorage` — wrap existing sync repos in `suspend` **temporarily**, only to
    keep the build green during this phase (it is deleted in Phase E).
  - `RemoteAppStorage(api: ServerApi)` — calls `BaseServerApi`, **maps DTO → domain**,
    and returns only domain types.
- Update state holders/screens to call from coroutines:
  - `MyTripsState.load()` already runs in `LaunchedEffect` → make it `suspend`.
  - `TripScreen` loads points/trip in `LaunchedEffect(tripId)`; writes
    (add/rename/delete point) launch in the screen's coroutine scope.
  - Add simple **loading / error** states (a spinner + a retry/snackbar) since
    calls can now fail or be slow.
- **Verify:** app builds and runs through the async interface.

### Phase E — Switch to the backend & remove file storage  *(DB‑only)*
- In `AppContainer`, provide **`RemoteAppStorage`** as the `AppStorage` singleton.
- **Delete `FileAppStorage`** and the file repositories/factories (`tripRepository`,
  `FileTripRepository`, `cityPointRepository`, `FilePointRepository`,
  `deleteTripStorage`) plus their JSON files — the DB is the only storage.
- Add a minimal **login/registration screen** (backend requires real users). On
  success, `SessionUserStorage` holds the token; `getTrips()` returns the caller's trips.
  - Interim (dev only): an auto `register`+`login` of a `demo` account may stand in
    briefly, but the real deliverable is a proper auth screen.
- **Verify end‑to‑end:**
  ```bash
  npx wrangler dev --ip 0.0.0.0 --port 8787
  ```
  Create a trip in the app → confirm the row:
  ```bash
  npx wrangler d1 execute dimadyplom2-db --local --command "SELECT * FROM trips;"
  ```

### Phase F — Polish
- Error/empty/loading UX; clear messaging when the network/backend is unreachable
  (since there is no offline fallback).
- (Later) real‑time via WebSocket (Durable Objects) feeding a reactive `Flow`
  version of `AppStorage`.
- (Optional, only if a genuine offline need appears later) reintroduce a local
  **read‑through cache** (e.g., Room) — not part of this DB‑only migration.

---

## 4. Files to add / change

**New**
- `app/.../DimaApp.kt` — `Application` subclass creating `AppContainer` (register in `AndroidManifest.xml`).
- `app/.../data/AppContainer.kt` — builds & holds the singletons.
- `app/.../data/remote/AppHttp.kt` — the ONE shared Ktor `HttpClient` (JSON + Bearer).
- `app/.../data/remote/UserAuth.kt` — your `UserAuthSingleton`: token/userId, DataStore‑backed.
- `app/.../data/remote/ServerApi.kt` — your `BaseServerApi`: endpoint methods returning DTOs.
- `app/.../data/RemoteAppStorage.kt` — `AppStorage` over `BaseServerApi`, DTO→domain only.
- `app/.../data/remote/IsoDate.kt` — ISO‑8601 ↔ millis helpers.
- `app/.../ui/auth/LoginScreen.kt` — login/register (Phase E).

**Changed**
- `trip/Trip.kt` — replace single `latitude/longitude` with `destinations: List<Destination>` (+ new `trip/Destination.kt`).
- `ui/CreateTripScreen.kt` — support one‑or‑more destinations (per the decision in §6).
- `ui/TripScreen.kt` — center on first destination; reflect all destinations.
- `data/remote/Dtos.kt` — rewrite to `API.md` shapes (`PostTripDto`/`ResponseTripDto`, points) + mappers.
- `data/remote/Requests.kt` — fold into the Post DTOs (auth adds user server‑side); remove stale bodies.
- `data/remote/TripApi.kt` — replaced by `ServerApi.kt` (full endpoint set + shared client + bearer).
- `data/remote/AuthStore.kt` — replaced by `UserAuth.kt` (or fill this empty file).
- `data/AppStorage.kt` — async interface (`FileAppStorage` kept only until Phase E).
- `ui/MyTripsState.kt`, map component — collect async data + loading/error.
- `navigation/AppNavigation.kt` — pull `AppStorage` from `AppContainer`; add auth gate.

**Removed (Phase E — DB is the only storage)**
- `data/AppStorage.kt` → `FileAppStorage` class.
- `trip/FileTripRepository.kt`, `trip/TripRepository.kt` factory (`tripRepository`).
- `map/repository/FilePointRepository.kt` (`cityPointRepository`, `deleteTripStorage`).
- On‑device JSON files (`trips/index.json`, `trips/<id>/points.json`).

---

## 5. Testing checklist

- [ ] Mapper unit tests (DTO → domain) — ids, dates, `name↔title`, destination→lat/lng.
- [ ] `SessionUserStorage` persists token across process restart.
- [ ] `BaseServerApi` calls succeed against `wrangler dev` (verified via `.http` file).
- [ ] Create trip in app → row appears in D1.
- [ ] Points create/rename/delete round‑trip to D1.
- [ ] App builds/runs through the async `AppStorage` (Phases D–E).

---

## 6. Open decisions (confirm before Phase D/E)

1. **Auth UX:** build a proper **login/registration screen** now (Phase E), or keep
   the temporary dev auto‑login a bit longer?
2. **Multiple destinations UI:** how much destination editing do you want now?
   - Minimum: store/display the full ordered list, but keep Create with **one**
     destination for now (list still supported in the model), or
   - Full: add/remove/reorder multiple destinations in `CreateTripScreen`.

> Resolved: **DB‑only** — no offline/file fallback; `FileAppStorage` is removed in
> Phase E.
> Resolved: **a trip has multiple destinations** — `Trip.destinations: List<Destination>`.
> Resolved: **ids are `Int`** — domain `Trip.id`/`MapPoint.id` are `Int` (0 = unsaved,
> assigned on save; `upsert` returns the stored entity).
> Resolved: **no legacy** — old on-disk formats are not supported.












