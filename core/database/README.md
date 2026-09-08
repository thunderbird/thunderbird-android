# Core Database

`core:database` provides the domain-neutral database foundation used by application database assembly. Feature schemas,
Room entities, DAOs, and repository implementations do not belong here.

## Modules

- `:core:database:api` contains backend-neutral contribution metadata, validation, deterministic ordering, and the
  transaction contract.
- `:core:database:internal` contains the Room 3 bootstrap, coordinated Room migration assembly, and transaction adapter.
  Only application composition modules may depend on it.

## Room bootstrap

The application database assembly owns the composed Room `@Database` class and feature migration implementations. It:

1. creates a `DefaultDatabaseContributionRegistry` from all enabled feature contributions;
2. associates each concrete Room migration with its contribution using `RoomDatabaseMigration`;
3. calls `RoomMigrationAssembler.assemble()` to validate and combine migration steps deterministically;
4. creates a platform builder with `createRoomDatabaseBuilder()`; and
5. opens it with `RoomDatabaseBootstrap`.

On Android, `createRoomDatabaseBuilder(context, name)` places the database in the application's private database
directory. On JVM desktop, `createRoomDatabaseBuilder(databaseFile)` uses the supplied file and creates its parent
directory when needed. Both paths use AndroidX `BundledSQLiteDriver` and `Dispatchers.IO` by default.

After opening, application composition exposes `createRoomDatabaseTransactionRunner(database)` as the
backend-neutral `DatabaseTransactionRunner` used by repositories.
