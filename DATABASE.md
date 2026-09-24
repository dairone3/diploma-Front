# How the app connects to the database

This backend runs on **Cloudflare Workers** and uses **Cloudflare D1** (a
serverless SQLite database). There is **no connection string and no connection
pool** to manage — Cloudflare injects the database into the Worker as a
**binding** named `DB`, available at runtime as `env.DB`.

The "connection" is therefore three things wired together:

```
wrangler.jsonc  ──▶  Env.DB type  ──▶  env.DB.prepare(...) in handlers
 (declares the       (worker-           (actual queries)
  binding)            configuration.d.ts)
```

---

## 1. Declare the binding — `wrangler.jsonc`

This is where the app is told *which* database to connect to. The `binding`
name (`DB`) becomes the property on `env`.

```jsonc
{
  "d1_databases": [
    {
      "binding": "DB",                                   // -> env.DB in code
      "database_name": "dimadyplom2-db",                 // human-friendly name (used by the CLI)
      "database_id": "31b10bb2-6b36-494f-a1c6-5b505677b3c5", // the real Cloudflare D1 database
      "migrations_dir": "migrations"                     // where schema migrations live
    }
  ]
}
```

- **Local dev / tests** use a local SQLite file under `.wrangler/state/v3/d1`
  (Miniflare emulates D1). Nothing hits the network.
- **Deployed** (`wrangler deploy`), the same `env.DB` talks to the real D1
  database identified by `database_id`.

The same code works in both places — only the binding target changes.

## 2. The type — `worker-configuration.d.ts`

`wrangler types` generates the `Env` interface so `env.DB` is typed as a
`D1Database`:

```ts
interface Env {
  DB: D1Database;
  // ...other bindings
}
```

Regenerate it any time you change bindings:

```bash
npm run cf-typegen      # = wrangler types
```

## 3. The entry point passes `env` in — `src/index.ts`

Cloudflare calls `fetch(request, env)` for every request. `env` (containing
`DB`) is placed on the request context and handed to each route handler:

```ts
export default {
  async fetch(request, env): Promise<Response> {
    return await route(request, env);   // env carries env.DB
  },
} satisfies ExportedHandler<Env>;
```

`env` flows into the `RouteContext` (`src/lib/router.ts`), so every handler and
middleware can reach the database via `ctx.env.DB`.

## 4. Run queries — any handler

Queries use the D1 prepared-statement API: `prepare()` → `bind()` →
`first()` / `all()` / `run()`. Real example from `src/routes/trips/trips.get.ts`:

```ts
export async function getTrip({ env, params }: RouteContext): Promise<Response> {
  // one row (or null)
  const trip = await env.DB
    .prepare(`SELECT * FROM trips WHERE id = ?`)
    .bind(Number(params.tripId))
    .first();

  // many rows -> { results: [...] }
  const points = await env.DB
    .prepare(`SELECT * FROM points WHERE trip_id = ? ORDER BY created_at DESC`)
    .bind(Number(params.tripId))
    .all();

  // ...
}
```

Writes look the same but end with `.run()`:

```ts
const insert = await env.DB
  .prepare(`INSERT INTO trips (name, created_by) VALUES (?, ?)`)
  .bind(name, userId)
  .run();

const newId = insert.meta.last_row_id;   // the auto-generated id
```

**Always use `.bind(...)` for values** (parameterized queries) — never string
concatenation — to stay safe from SQL injection.

### D1 result methods at a glance

| Method     | Returns                              | Use for                    |
| ---------- | ------------------------------------ | -------------------------- |
| `.first()` | the first row object, or `null`      | single-row reads / lookups |
| `.all()`   | `{ results: [...], meta }`           | multi-row reads (lists)    |
| `.run()`   | `{ meta: { changes, last_row_id } }` | INSERT / UPDATE / DELETE   |

---

## 5. Create / update the schema — migrations

The schema lives in `migrations/*.sql` and is applied with wrangler. Handy npm
scripts (see `package.json`):

```bash
npm run db:migrate         # apply migrations to the LOCAL database
npm run db:tables          # list tables
npm run db:schema          # print CREATE statements
```

Apply to the **remote** (deployed) database:

```bash
npx wrangler d1 migrations apply dimadyplom2-db --remote
```

Run ad-hoc SQL against the local database:

```bash
npx wrangler d1 execute dimadyplom2-db --local --command "SELECT * FROM users;"
```

---

## TL;DR

- The database connection **is** the `DB` binding in `wrangler.jsonc`.
- It surfaces in code as **`env.DB`** (typed `D1Database`).
- `env` is passed by the runtime into `fetch(request, env)` and threaded to
  handlers via `ctx.env`.
- Query with **`env.DB.prepare(sql).bind(...).first()/.all()/.run()`**.
- Local dev/tests use an emulated SQLite file; deploy uses the real D1 — same code.

