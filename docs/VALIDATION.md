# Validation record

Verified locally on August 31, 2026.

## Environment

Windows 11, JDK 26.0.2.1, Maven 3.9.15, PostgreSQL 18, Java source compatibility 17.

## Automated checks

The full local suite passed **24 tests with zero failures, zero errors and zero skips**:

- 16 real PostgreSQL integration tests.
- 7 unit tests.
- 1 desktop Swing workflow/rendering test.

After the full run, the Swing test was rerun following layout refinements and passed again. The final rendering check asserts that previews are not blank, the category selector stays inside the compact layout, and the cart can display at least two rows at the minimum tested window size.

Integration coverage includes first-admin bootstrapping, salted password storage, login throttling, role restrictions, account/session invalidation across application instances, own-password changes, product CRUD and stale edits, inventory audit history, atomic checkout, rejected sales without side effects, price changes, forced database failure after a sale header is inserted, duplicate checkout keys, concurrent overselling, concurrent duplicate requests, historical receipts, staff transaction isolation, report boundaries at Manila midnight, and optional sample data.

Swing checks instantiate all screens using an isolated test schema, validate catalog and transaction rows, exercise the cart controls, verify payment/change display, and confirm that staff has no Users or Reports page.

## Visual inspection

Offscreen previews were generated for first-run setup, login, overview, products, inventory, empty checkout, populated checkout, transactions, reports, users, compact checkout and the staff overview.

Manual inspection identified compact-window filter clipping and an oversized payment section. These were corrected; table widths, brand and navigation icons were also refined.

Selected previews are included in docs/screenshots. They contain generated test fixtures, not the user's business data.

## Packaging and startup

The Maven build produces target/stockroom.jar with PostgreSQL JDBC, FlatLaf and Checker Qual included. Third-party licenses are retained in named resource folders. Repeated packaging recreates the base JAR before shading to avoid merging a previously shaded JAR again.

The packaged application's --check-db command successfully connected to inventory_sales, initialized its schema and reported that the first administrator was still required. No account or sample product was created in the user's application database by verification.

The Windows setup, start, stop, test, backup and package scripts are included. The release ZIP excludes local credentials, database data and dependency caches.

## Practical limitations

Offscreen rendering checks validate Swing controls and layout but are not a substitute for every manual interaction on all operating systems and display scaling settings. Printer output needs a configured physical or PDF printer and was not physically printed. The GitHub workflow is supplied separately; its remote status should be checked on the repository.

The application is an educational small-business desktop system, not an audited accounting package or accredited tax-invoicing system.
