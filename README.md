# Stockroom — Java Inventory & Sales

**Java-Based Inventory and Sales Management System for Small Businesses**

A complete Java desktop application following the supplied Programming (Java) NC III project brief. Built with **Java Swing + OOP + JDBC + PostgreSQL**. The interface uses FlatLaf; it is a native desktop application, not a website.

## Start here on this computer

Double-click **Start Stockroom.cmd** in this folder.

The local database setup and runnable JAR are provided. On first launch, create your administrator account. There is **no default password**. You can choose whether to load twelve sample products.

Open this folder's **pom.xml** in IntelliJ IDEA. Do not open the separate INVENTORY-AND-SALES-MANAGEMENT-SYSTEM Spring Boot starter when working on Stockroom; that folder is independent and was left untouched.

## What is included

- Login, first-administrator setup, password changes and secure password hashing.
- Admin/Staff roles, server-independent service authorization and session invalidation.
- Overview with five metrics, seven-day sales chart and low-stock alerts.
- Product create, read, edit and archive; search, categories and stock statuses.
- Stock receiving, counted adjustments, before/after history and audit references.
- Multi-product cart, quantity changes, cash payment, exact totals and change.
- Atomic checkout, stock locking and protection against duplicate checkout retries.
- Receipts, text export, printing and date-filtered transaction history.
- Daily sales, best sellers, current inventory snapshots and CSV exports.
- User creation, activation/deactivation and administrator password resets.
- Six-table PostgreSQL schema, isolated local setup, backup and release scripts.
- Unit, real PostgreSQL integration and offscreen Swing verification.
- User manual, design diagrams, use-case scenarios and demonstration notes.

Admin can manage the store and team. Staff can view products and inventory, process sales, and see only their own transactions. Restricted actions are checked in the service layer as well as hidden in the UI.

## Requirements

- JDK **17 or newer**; the source targets Java 17.
- PostgreSQL **14 or newer** with command-line tools.
- Maven **3.9 or newer** to build from source. The bundled release JAR already contains its runtime libraries.
- Windows for the supplied PowerShell launch/setup scripts. The Java application itself can run on other desktop platforms with an existing PostgreSQL database.

Development verification uses the locally installed JDK 26, Maven 3.9.15 and PostgreSQL 18. Dependencies are pinned in pom.xml: PostgreSQL JDBC 42.7.13, FlatLaf 3.7, and JUnit Jupiter 6.0.3 for tests.

## Set up from source on another Windows computer

In a terminal opened in this folder:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/setup-local.ps1
mvn "-Dmaven.repo.local=$PWD\.m2" -B -ntp package
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/run.ps1
```

The setup script creates an isolated PostgreSQL cluster at **.local/postgres**, listening only on **127.0.0.1:55432**, with SCRAM authentication. It generates random database credentials and creates **inventory_sales** for the application and **inventory_sales_test** for tests. It does not change an existing PostgreSQL installation's databases or Windows service.

Local credentials are stored in **config/local.properties** and **.local/db-*.secret**. Both locations are ignored by Git. Keep these files private. The bootstrap database administrator is separate from the non-superuser application database account.

For another local port, use setup-local.ps1 -Port 55433 before the first setup and update your test connection accordingly. The standard test script assumes port 55432. If you already have local configuration, setup does not overwrite it.

To stop only this project's local database after closing the app:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/stop-local.ps1
```

## Use an existing PostgreSQL server

Create a database and login role with a password using your database administration tool. The application role needs CONNECT, schema creation, and ownership privileges for its own tables. It does not need superuser access.

Copy config/application.example.properties to config/local.properties and edit the URL, username and password. The application creates its six tables on first connection. No default administrator is inserted.

Environment variables override configuration: **DB_URL**, **DB_USERNAME**, **DB_PASSWORD**, **BUSINESS_NAME**, and **BUSINESS_TIMEZONE**. Set a different configuration file with the Java property **-Dstockroom.config=/path/to/file.properties**. The default file path is relative to the launch working directory.

For a remote server use PostgreSQL TLS settings appropriate to that environment, such as sslmode=verify-full, and do not expose the local development database directly to the internet.

Run from this project directory:

```text
java -jar target/stockroom.jar
java -jar target/stockroom.jar --check-db
```

The check command verifies the connection, initializes the schema if needed, and reports whether an administrator must be created. It does not create a user or sample data.

## IntelliJ IDEA

Open **pom.xml** as a Maven project. Choose a JDK of version 17 or later. Let Maven import dependencies. Set the run configuration's main class to **ph.stockroom.Main**, with this project root as the working directory. Run the database setup script once before starting.

No IntelliJ-specific files are required or committed.

## Build and test

```powershell
mvn "-Dmaven.repo.local=$PWD\.m2" -B -ntp package
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1
```

A plain Maven build runs unit tests. Database and Swing tests are explicitly skipped when TEST_DB_URL is absent; a unit-only build is not a substitute for the full test script.

The full script provides the dedicated test connection and runs **mvn verify**, producing **target/stockroom.jar**. Integration tests create unique schemas only in databases whose names end in **_test**. They do not read the production configuration or alter inventory_sales. Test schemas are dropped after use.

For a custom test server set TEST_DB_URL, TEST_DB_USERNAME and TEST_DB_PASSWORD, then run Maven verify directly. TEST_DB_URL must point to a separate database ending in _test. Swing screenshots require a desktop-capable Java environment and are skipped in headless CI.

Tests cover permissions, login throttling, session revocation, product uniqueness, stale edits, stock audit trails, invalid payments, price changes, forced mid-checkout database failure, concurrent overselling, duplicate request recovery, historical receipts, staff transaction isolation, exact reports and Manila midnight boundaries. The Swing check renders every screen, verifies staff navigation, and checks the actual cart controls and change display.

Test reports are in **target/surefire-reports**. Generated UI previews are in **target/screenshots** and use isolated test data, not your business database.

## Release package

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/package.ps1
```

This produces **dist/Stockroom-1.0.0.zip**, containing the executable JAR, launcher, setup/backup scripts and documentation. It intentionally excludes database contents, passwords, dependency caches and development test fixtures. The recipient still needs Java and PostgreSQL installed. Source builds require the full source repository, not just the runtime ZIP.

## Backup and restore

Create a PostgreSQL custom-format backup:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/backup.ps1
```

By default it is saved under .local/backups with a timestamp. Use -Destination for a specific new filename. Copy the backup securely to another device; a backup on the same drive is not sufficient protection against drive failure.

A database administrator can restore a dump into a **new empty database** using pg_restore --no-owner --no-privileges. Do not restore over your only working database. Configure the app to use the restored database, verify products, users, sales and movements, and only then decide whether to switch permanently.

The setup secrets and app configuration are separate from the data dump. Restoring to another machine requires the database administrator to create a role and supply fresh connection credentials. Source code and JAR backups do not include transaction data.

## Source map

```text
src/main/java/ph/stockroom/
  Main.java              Startup and database check
  config/                Environment and local configuration
  model/                 Products, roles, categories, cart and receipts
  dao/                   JDBC queries and row mapping
  service/               Authorization and business rules
  database/              Connections, transactions, schema initialization
  view/                  Swing frames, panels and dialogs
  util/                  Receipt, currency/date and CSV formatting
src/main/resources/database/schema.sql
src/test/java/ph/stockroom/
scripts/                  Setup, launch, test, backup and packaging
docs/                     User guide, design and validation notes
```

Read **docs/USER-GUIDE.md** for operating instructions and **docs/PROJECT-DESIGN.md** for use cases, OOP examples, class/ER/sequence diagrams and presentation notes.

## Operational boundaries

Prices and payments support two decimal places. Products have a maximum price of 999,999,999.99 PHP; quantities and minimum stock are bounded at 1,000,000. A sale supports at most 200 distinct products. Sale/payment totals must fit the database's NUMERIC(14,2) range.

Inventory valuation is at current selling prices. Revenue is not profit. Inventory reports show current quantities, even when a past sales period is selected. Receipts are internal sales records, not tax invoices.

Products are archived, not physically removed, to preserve references. Product names remain unique even after archival. The UI shows at most 500 recent transactions or movements; report aggregates cover all matching sales.

The system does not implement refunds, taxes, discounts, card payments, purchasing, multiple branches, password recovery emails, or automatic scheduled backups. It is designed for a trusted small-store desktop environment. Database credentials on a user's machine are not a security boundary against that machine's owner.

## Version control and GitHub

The main folder is the Stockroom project boundary. Do not stage the parent C: drive or the separate Spring starter. The ignore file excludes private settings, PostgreSQL files, exports, caches, IDE settings and builds.

See docs/GITHUB.md for a private-repository publishing checklist and CI configuration. Publishing requires access to the user's chosen GitHub repository; local source files alone are not a GitHub backup.

## Technology references

- [Oracle: JDBC transactions](https://docs.oracle.com/javase/tutorial/jdbc/basics/transactions.html)
- [PostgreSQL JDBC driver](https://jdbc.postgresql.org/)
- [PostgreSQL documentation](https://www.postgresql.org/docs/)
- [FlatLaf](https://www.formdev.com/flatlaf/)
- [JUnit](https://junit.org/)

Third-party license notices are listed in THIRD-PARTY-NOTICES.md and included in the application resources.
