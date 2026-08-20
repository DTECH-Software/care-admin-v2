# Previous-version database upgrade

The existing production schema is treated as Flyway baseline version `1`. The
ordered upgrades are under `src/main/resources/db/migration`, starting at `V2`.

## Before deployment

1. Stop writes to the old application and take a verified database backup.
2. Use a database restored from the previous application version. Do not use a
   database where some files from `docs/sql` were already run manually.
3. Run the migration from one Care Admin instance first.

## Run the upgrade

Set this environment variable on the migration instance:

```text
WECARE_DB_MIGRATION_ENABLED=true
```

Start Care Admin. Flyway creates `flyway_schema_history`, baselines the existing
schema at version `1`, and then applies `V2` through the latest migration in
order. Application startup fails if any migration fails.

## Verify

```sql
SELECT installed_rank,
       version,
       description,
       installed_on,
       success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Confirm that every row has `success = 1` and that the latest version matches the
latest SQL filename. Then complete application smoke tests before reopening
traffic.

## Important

Flyway versioned files must not be edited after they have been applied. Add the
next change as a new `V<number>__description.sql` file.
