# Nuvex backend (early scaffold)

Kotlin/Ktor service intended to keep provider master keys off Android devices.

## Current functionality

- `GET /api/v1/health` returns service health and whether provider environment variables are present.
- `GET /api/v1/servers` fails closed with `PROVIDER_NOT_CONFIGURED` unless provider settings are present. Even when environment variables exist, it returns `PROVIDER_ADAPTER_PENDING` until a reviewed adapter is implemented.
- PostgreSQL migration `db/migrations/V001__initial_schema.sql` defines initial data tables.

This is **not a production VPN backend** yet. Authentication, authorization, persistent database access, provider adapter calls, quotas, rate limiting, audit controls, session APIs, and deployment hardening still need implementation and tests. Do not expose it publicly as-is.

## Local build

Requirements: JDK 17 and Gradle 8.9.

```bash
gradle -p backend test
gradle -p backend build
```

## Environment

Copy `.env.example` to a local untracked `.env` file and set values for your own environment. The current scaffold does not yet load dotenv files automatically; export environment variables in the shell/container. Never commit real provider keys or database passwords.

## PostgreSQL

Create a dedicated database and least-privilege application role, then apply SQL migrations using your migration runner of choice. The app does not yet open a database connection; schema creation is groundwork only.

## Provider configuration

No provider is selected or approved yet. Review [the provider evaluation](../docs/PROVIDER_EVALUATION.md). Do not set `NUVEX_PROVIDER_API_KEY` until a provider contract and credentials are in place.
