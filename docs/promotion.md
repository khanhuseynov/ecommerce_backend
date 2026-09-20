# Promotions and coupons

`promotion` supports percentage and fixed-amount discounts. All monetary values use the same currency as product prices (the existing application has no multi-currency support).

## Admin API

All `/api/v1/promotions` endpoints require `ROLE_ADMIN`.

| Method | Path | Behavior |
| --- | --- | --- |
| POST | `/api/v1/promotions` | Create a campaign (201) |
| GET | `/api/v1/promotions` | Paginated campaigns, including inactive campaigns |
| GET | `/api/v1/promotions/{id}` | Read campaign and usage count |
| PUT | `/api/v1/promotions/{id}` | Replace campaign rules; usage history is retained |
| DELETE | `/api/v1/promotions/{id}` | Deactivate without deleting usage history (204) |

Example creation body:

```json
{
  "name": "Summer 20",
  "code": "SUMMER20",
  "discount_type": "PERCENTAGE",
  "discount_value": 20.00,
  "minimum_order_amount": 50.00,
  "maximum_discount_amount": 30.00,
  "starts_at": "2026-06-01T00:00:00",
  "ends_at": "2026-10-01T00:00:00",
  "active": true,
  "usage_limit": 100,
  "per_user_limit": 1,
  "product_id": null,
  "category_id": null
}
```

- Omit `code` or set it to `null` for an automatic campaign. Codes contain ASCII letters, numbers, `_` or `-`, and are stored uppercase. Empty codes are rejected on creation.
- Use `FIXED_AMOUNT` for a fixed discount. Percentage values must be greater than zero and at most 100. Money accepts up to two decimal places.
- `minimum_order_amount` is required (use zero for no minimum). The threshold uses the entire order's original total.
- Optional `product_id` or `category_id` restricts the discount to matching line subtotals. Both cannot be supplied together. A category scope matches that exact category.
- Optional limits and maximum discount use `null` for unlimited. Usage limits must be positive.
- Dates use the application's local time, consistent with existing order timestamps. Start is inclusive; end is exclusive.
- Updating a campaign never resets usage. Deactivated codes remain reserved; use PUT to reactivate.
- Pagination supports sorting by `id`, `name`, `starts_at`, and `ends_at`.

## Checkout

```http
POST /api/v1/orders/checkout
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{"shipping_address": "Baku, Nizami street", "coupon_code": "SUMMER20"}
```

Checkout trims and uppercases the coupon. Blank or omitted codes select the eligible automatic campaign offering the greatest saving. Ties prefer the lowest campaign ID. An explicit coupon is applied alone; it never stacks with automatic discounts. An invalid or ineligible explicit coupon fails checkout with 409 rather than silently falling back.

The response includes `original_total_price`, `discount_amount`, final `total_price`, `coupon_code`, and `promotion_name`. Item prices and subtotals remain before discount. Historical order values remain unchanged when campaign rules change. Fixed discounts and percentage caps never reduce the payable total below zero; percentage calculations round half up to two decimals.

Usage is consumed only by successful checkout, not by placing items in the cart. The cart, stock, order, and usage commit in one transaction. Checkout locks the cart, products in ID order, and campaigns before checking limits. Campaign locking serializes usage checks, including per-user limits. Automatic campaigns are locked in ID order. Failed checkout rolls back all changes.

Refunds/cancellations do not restore usage: the existing application has no cancellation/refund workflow. If one is added, its usage-restoration policy must be specified explicitly. No external payment provider is required.

## Database and tests

Liquibase `v6-promotion.sql` creates campaigns and usage history, adds order snapshots, and backfills existing orders with zero discount and their original total. Foreign keys preserve history; DELETE deactivates campaigns rather than removing them.

Run unit specifications:

```bash
./gradlew test --tests '*Spec'
```

Integration tests require a **disposable PostgreSQL database** and create test data in it:

```bash
PROMOTION_TEST_DB_URL=jdbc:postgresql://localhost:55439/postgres \
PROMOTION_TEST_DB_USER=khan \
PROMOTION_TEST_DB_PASSWORD='' \
./gradlew test --tests '*PromotionIntegrationTest'
```

Without `PROMOTION_TEST_DB_URL`, these integration tests are skipped. They verify migrations and entity mappings, HTTP authorization and validation, committed snapshots, rollback, concurrent global limits, and duplicate checkout protection.
