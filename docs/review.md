# Product reviews and ratings

Reviews support integer ratings from 1 to 5 and an optional comment of up to 2,000 characters. Comments are trimmed; blank comments become `null`. Responses expose only the author's ID and first name, never email or password. Comments are plain text; clients should render them as text, not HTML.

## Eligibility and ownership

- Only an authenticated user with a non-cancelled order containing the exact product can create a review. `PLACED`, `PROCESSING`, `SHIPPED`, and `DELIVERED` qualify. This verifies an order, not payment or delivery.
- A user can have one review per product, enforced by both application logic and a database unique constraint.
- Only the author may edit a review. The author or an admin may delete it. Admins cannot rewrite another person's review.
- Updates and deletion do not require the purchase to remain eligible. Cancellation does not automatically remove existing reviews. After deletion, the user may create a new review if still eligible.
- Product-level write locks serialize review mutations. This prevents simultaneous create requests from both passing the duplicate check.

## API

| Method | Endpoint | Access |
| --- | --- | --- |
| GET | `/api/v1/products/{productId}/reviews` | Public, paginated |
| GET | `/api/v1/products/{productId}/reviews/{reviewId}` | Public |
| POST | `/api/v1/products/{productId}/reviews` | Authenticated buyer |
| PUT | `/api/v1/products/{productId}/reviews/{reviewId}` | Author |
| DELETE | `/api/v1/products/{productId}/reviews/{reviewId}` | Author or admin |

Create and update body:

```json
{
  "rating": 5,
  "comment": "Məhsul keyfiyyətlidir."
}
```

`PUT` replaces the rating and comment. Omit `comment` or send null to remove it. Product ID comes from the URL; user ID comes from the authenticated principal, not the request body.

Response:

```json
{
  "id": 12,
  "product_id": 3,
  "user_id": 7,
  "reviewer_name": "Ali",
  "rating": 5,
  "comment": "Məhsul keyfiyyətlidir.",
  "created_at": "2026-09-20T17:00:00",
  "updated_at": "2026-09-20T17:00:00"
}
```

List example: `/api/v1/products/3/reviews?page=0&size=10&sort=rating,desc`.

Allowed sort fields: `id`, `rating`, `created_at`/`createdAt`, `updated_at`/`updatedAt`. Default ordering is newest first; ID breaks ties for stable pagination. Maximum page size follows the application's existing 100-item limit.

- Create: 201; get/update: 200; delete: 204.
- Invalid rating/comment/sort: 400.
- No eligible purchase or duplicate review: 409.
- Missing resource, wrong product/review combination, or another user's write attempt: 404.
- Unauthenticated writes: 403 under the existing security configuration.
- Review writes are available to buyers; product management endpoints remain admin-only.

## Product statistics

Product detail and search responses now include `average_rating` and `review_count`. An unrated product returns zero for both. Average rating is rounded to two decimal places.

The values are read-only Hibernate formulas computed by PostgreSQL from the reviews table in the product SELECT. There are no stored counters to become inconsistent after update/delete, and no separate query per product from Java. Review indexes support these lookups. Existing product filters and allowed sorting fields are unchanged.

Liquibase `v7-review.sql` adds the reviews table, the rating check, foreign keys, a unique `(user_id, product_id)` constraint, and indexes for product/date and product/rating queries. Existing products need no rating backfill. Deleting a product cascades to its reviews, subject to the other existing product foreign-key restrictions. User deletion is restricted while reviews reference the user.

## Postman walkthrough

1. Restart the backend so Liquibase runs and the new endpoints are loaded.
2. Log in with a buyer and set Authorization to Bearer Token using `accessToken`.
3. Add the desired product to the cart and complete `/api/v1/orders/checkout`.
4. POST the body above to `/api/v1/products/<purchased-product-id>/reviews`.
5. GET the review list and product details without a token to inspect the review and statistics.
6. PUT the review with another rating and verify the product's average changes.
7. Try the same create again: expect 409. Try editing/deleting as another buyer: expect 404.
8. DELETE as the author or admin, then verify the review count decreases.

## Tests

Unit and login tests:

```bash
./gradlew test --tests '*Spec' --tests '*LoginSecurityTest'
```

Database integration tests need a disposable PostgreSQL database and leave test records in it. Never point them at production:

```bash
REVIEW_TEST_DB_URL=jdbc:postgresql://localhost:55439/postgres \
REVIEW_TEST_DB_USER=khan \
REVIEW_TEST_DB_PASSWORD='' \
./gradlew test --tests '*ReviewIntegrationTest'
```

Without `REVIEW_TEST_DB_URL`, integration tests are skipped. They check migrations/entity mappings, order eligibility, public reads, ownership/admin restrictions, request validation, pagination, statistics after edits/deletion, and concurrent duplicate submissions.
