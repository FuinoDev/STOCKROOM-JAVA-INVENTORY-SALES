# Stockroom user guide

Stockroom is a Java desktop inventory and cash-sales application. Its full project title is **Java-Based Inventory and Sales Management System for Small Businesses**.

## Start and sign in

Double-click **Start Stockroom.cmd** in the main project folder. It starts the project's local PostgreSQL server if needed and opens the application.

On the first launch, enter a display name, a username, and a password of 10–128 characters. Confirm the password. This creates the administrator account. There is no default password.

The optional **Start with 12 sample products** checkbox loads a small practice catalog with opening quantities. Sample products are real records in your local database, clearly identified by their opening-stock references. Leave the checkbox clear for an empty business catalog. Sample transactions are not inserted into your business database.

On later launches, use your username and password. Usernames are case-insensitive. Five unsuccessful attempts for a username temporarily block further attempts for 10 minutes in that running application. Sessions expire after eight hours.

## Overview

The overview shows active products, stock units, products needing attention, inventory value, today's sales, a seven-day sales chart, and low-stock alerts.

Inventory value uses **current selling price × current quantity**. This is a retail valuation, not purchase cost, profit, or accounting asset value. Sales revenue is the total collected for products; it is not profit.

The inventory counts are store-wide. Admin sees sales for all users. Staff sees only their own sales totals. All business dates use the configured timezone, which defaults to Asia/Manila.

## Products

Admin can add a product with its category, price, opening quantity, and minimum-stock threshold. Product names must be unique without regard to letter case. A category can be added using **+ Category**.

Search by any part of the name and filter by category. Search and category filters work together. **Export catalog** exports the rows currently visible.

Use **Edit product** to update details. Stock is deliberately changed through inventory actions, so every quantity change has a record. If another operation changes a product while its form is open, refresh and reopen the form.

**Archive product** is the safe delete operation. First bring stock to zero through a documented adjustment, then archive. Archived products disappear from the active catalog but remain in historical receipts and stock movements. Archived names remain reserved.

Staff can view, search, filter and export the catalog. Staff cannot change product details, categories or quantities.

## Inventory

**Receive stock** adds a positive quantity. Enter a delivery reference or reason.

**Adjust count** sets the actual counted quantity. Enter a reason such as damage, loss, or physical count. If stock changed after you opened the form, refresh and count again before adjusting.

**View product history** shows movements for the selected product. **Show all movements** restores the complete recent history. History shows the latest 500 matching movements, newest first, including archived products. CSV exports reflect the currently loaded history.

Statuses are distinct: **Out of stock** means zero; **Low stock** means a positive quantity at or below the minimum; **In stock** means above the minimum. Overview alerts include both low and out-of-stock products.

## New sale

1. Search or filter the catalog and select a product.
2. Enter a whole-number quantity and choose **Add to cart**, or double-click a product.
3. Add more products. Adding the same product again combines quantities.
4. Use **Change quantity** or **Remove** as needed.
5. Enter cash received. Change updates immediately.
6. Choose **Complete sale**.

The application rechecks prices and stock in PostgreSQL before saving. If a price changed, remove the item and add it again to accept the new price. If stock is insufficient, reduce the quantity.

Payment must cover the total. Sales cannot be empty. Quantities must be positive. The receipt, all sale lines, stock deductions, and inventory movements commit together, or none of them do.

If the connection is interrupted during checkout, **retry the unchanged cart**. The same request reference is reused to recover a completed receipt without creating a duplicate sale. Check Transactions before clearing or changing an uncertain sale.

The cart stays available when you switch screens. Signing out or closing asks for confirmation and warns about an unsaved cart. Carts are not saved across application restarts.

## Transactions and receipts

Choose a start and end date using YYYY-MM-DD, then **Apply dates**. Both dates are inclusive in the business timezone.

The table shows at most the newest 500 matching transactions. Reports aggregate every sale in the selected period, even if the transaction table reaches this display limit.

Double-click a transaction or choose **View receipt**. Save a UTF-8 text receipt or print it using the system print dialog. Receipts retain product names and prices from the time of sale, even after products are edited or archived.

Staff sees only their own receipts, including when accessing a transaction by ID.

Receipts are internal sales records, **not tax invoices**. This project does not implement tax accreditation, VAT calculations, refunds, discounts, purchase costs, or non-cash payments.

## Reports (Admin)

Choose a date range or use Today, Last 7 days, or This month. Choose **Generate report**.

Daily sales show transactions, units sold, and revenue per date with recorded sales. Best sellers show up to ten products by units sold. A product renamed between sales may appear under separate historical names.

The inventory tab is a **current snapshot**, not historical stock as of the sales end date. Changing the sales period does not reconstruct past inventory. The captions identify the loaded period and current quantities.

Export daily sales and inventory to CSV. The loaded report is exported even if you subsequently edit a date field without generating again. CSV fields are quoted, UTF-8 encoded, and protected against spreadsheet formula injection.

## Users and passwords (Admin)

**Add user** creates Staff or Admin accounts. Staff is the default choice.

Deactivate accounts that should no longer sign in. Their past transactions remain intact. You cannot deactivate your own account, and at least one active administrator must remain.

**Reset password** invalidates that user's existing sessions. Share the replacement securely yourself. The app does not email passwords.

Use **Change password** in the sidebar for your own account. Enter your current password and confirm the replacement. Other sessions are invalidated; your current session remains active.

## Backups

Use the backup script described in README.md to create a PostgreSQL dump. Store backups securely outside this computer. A copy of the source code or runnable JAR does not include the business database.

Do not delete .local/postgres or config/local.properties as a way to reset the application: they contain your local database and connection settings.

## Troubleshooting

- Cannot connect: launch using Start Stockroom.cmd and confirm PostgreSQL is installed. Check .local/postgres.log and config/local.properties.
- Wrong credentials: check your username, password and account activation with an admin. Passwords cannot be recovered from the database.
- Duplicate product: search the catalog, including whether that name was archived.
- Stock changed while editing: refresh, reopen the product or stock form, and re-enter the change.
- Blank report: confirm the date range and timezone. Unsaved carts are not transactions.
- Session ended: sign out and sign in again.
- A very long product name may be visually shortened in a table; receipts and exports retain the full name.
