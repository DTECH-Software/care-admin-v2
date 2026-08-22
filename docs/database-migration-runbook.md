# Previous-version database upgrade

The existing production schema is treated as Flyway baseline version `1`. The
ordered upgrades are under `src/main/resources/db/migration`, starting at `V2`.

## Before deployment

1. Stop writes to the old application and take a verified database backup.
2. Use a database restored from the previous application version. Do not use a
   database where some files from `docs/sql` were already run manually.
3. Run `docs/sql/production-migration-preflight.sql`. Its ACTIVE NIC duplicate
   query must return zero rows before continuing.
4. Run the migration from one Care Admin instance first.

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

## Recover a failed MySQL migration

1. Set `WECARE_DB_MIGRATION_ENABLED=false` and restore application health.
2. Keep the database backup. Inspect the failed version and any DDL it may have
   committed; MySQL DDL is not rolled back like ordinary transactional data.
3. Correct and deploy the migration only when that version has never succeeded
   in any target environment.
4. Delete only its failed history row (`success = 0`) after verifying the
   partial schema state. Never delete or change a successful history row.
5. Run the preflight again, enable migration on one instance, and verify the
   full history before enabling other instances.

## Important

Flyway versioned files must not be edited after they have been applied. Add the
next change as a new `V<number>__description.sql` file.
