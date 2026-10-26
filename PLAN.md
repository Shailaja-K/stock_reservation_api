
# Implementation Plan — Stock Reservation API

## 1. Objective

Build a REST API using Java, Spring Boot, Spring Data JPA, and PostgreSQL to manage product inventory and reservations.

The API must support creating reservations for multiple products, confirming or cancelling reservations, validating stock availability, and maintaining consistent inventory during concurrent requests.

## 2. Acceptance Criteria

- [x] List products and their stock availability.
- [x] Create products.
- [x] Create reservations containing multiple products and quantities.
- [x] Validate that requested quantities are positive.
- [x] Reject reservations when requested stock is unavailable.
- [x] Prevent partial stock reservations when any requested product fails validation.
- [x] Confirm reservations and deduct stock.
- [x] Cancel reservations and release reserved stock.
- [x] Prevent repeated confirmation or cancellation from modifying stock again.
- [x] Reject operations involving nonexistent products or reservations.
- [x] Prevent confirmation of cancelled reservations.
- [x] Use database transactions and locking to protect inventory updates.
- [x] Include automated tests for core reservation behavior and concurrency.
- [x] Document setup, API endpoints, and test execution.

## 3. Assumptions and Scope

- Each product has a unique SKU.
- Product stock is represented by total stock and reserved stock.
- Available stock is calculated as `totalStock - reservedStock`.
- A newly created reservation has status `ACTIVE`.
- Confirmation changes the reservation status to `CONFIRMED`.
- Cancellation changes the reservation status to `CANCELLED`.
- Only active reservations can be confirmed or cancelled.
- A reservation can contain multiple products.
- Product IDs are used to identify products in reservation requests.

### Out of Scope

- User authentication and authorization.
- Payment processing.
- Reservation expiration and automatic cleanup.
- Frontend development.
- Distributed inventory management across multiple services.
- Production deployment and monitoring.

## 4. API Design

### Products

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | Retrieve products and stock availability |
| POST | `/api/products` | Create a product |

Product responses include:

- `id`
- `sku`
- `name`
- `totalStock`
- `reservedStock`
- `availableStock`

### Reservations

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/reservations` | Create a reservation |
| GET | `/api/reservations/{id}` | Retrieve reservation details |
| POST | `/api/reservations/{id}/confirm` | Confirm an active reservation |
| POST | `/api/reservations/{id}/cancel` | Cancel an active reservation |

### Example: Create a Reservation

Request:

```http
POST /api/reservations
Content-Type: application/json
```

```json
{
  "1": 3,
  "2": 2
}
```

This requests 3 units of product ID `1` and 2 units of product ID `2`.

The request body maps product IDs to requested quantities.

### Sample Products

The `src/main/resources/seed.sql` script contains the following sample products:

| Product ID | SKU | Name | Total Stock |
|---|---|---|---:|
| 1 | NOTE-001 | Notebook | 10 |
| 2 | PEN-001 | Pen | 5 |
| 3 | BAG-001 | Backpack | 2 |

These IDs correspond to the sample database state created by the seed script. Actual IDs may differ depending on the database contents.

The seed script must be executed manually; it is not automatically applied on application startup.

### Reservation Lifecycle

1. **Create:** Validate requested products, quantities, and available stock. Create an `ACTIVE` reservation and increase reserved stock.
2. **Confirm:** Deduct the reserved quantities from total stock, decrease reserved stock, and mark the reservation `CONFIRMED`.
3. **Cancel:** Decrease reserved stock without changing total stock and mark the reservation `CANCELLED`.

## 5. Database Design

### Product

Stores product and inventory information.

- `id` — primary key
- `sku` — unique product identifier
- `name` — product name
- `total_stock` — total inventory quantity
- `reserved_stock` — quantity currently reserved

### Reservation

Stores reservation information.

- `id` — primary key
- `status` — reservation status
- `created_at` — creation timestamp
- `updated_at` — last-update timestamp

### Reservation Item

Stores the products and quantities belonging to each reservation.

- `id` — primary key
- `reservation_id` — reference to a reservation
- `product_id` — reference to a product
- `quantity` — reserved quantity

A reservation can contain multiple reservation items, and each item references one product.

## 6. Application Components

- **ProductController:** Handles product listing and creation.
- **ReservationController:** Handles reservation creation, retrieval, confirmation, and cancellation.
- **ReservationService:** Implements reservation business rules and transaction boundaries.
- **ProductRepository:** Provides product persistence and pessimistic locking.
- **ReservationRepository:** Provides reservation persistence and locking.
- **ReservationItemRepository:** Retrieves reservation items.
- **GlobalExceptionHandler:** Converts supported exceptions into HTTP error responses.
- **ReservationResponse:** Represents reservation details returned by the API.

Spring Data JPA and Hibernate handle database persistence.

## 7. Transaction and Concurrency Strategy

Reservation operations use database transactions to keep related inventory changes atomic.

- Product rows are locked during stock-sensitive operations.
- Products are locked in ascending ID order to reduce deadlock risk when multiple products are involved.
- Available stock is checked while holding the relevant product lock.
- Reservation creation fails without retaining partial stock changes if a requested product cannot be reserved.
- Confirmation and cancellation require the reservation to remain `ACTIVE`.
- Reservation rows are locked when changing reservation state.

These measures protect the inventory updates exercised by the implementation and its tests. Broader production concurrency behavior would require testing under the intended deployment workload.

## 8. Implementation Status

- [x] Set up the Spring Boot project and dependencies.
- [x] Configure PostgreSQL and JPA.
- [x] Implement the product entity and repository.
- [x] Implement the reservation and reservation-item entities.
- [x] Implement product endpoints.
- [x] Implement reservation creation and stock validation.
- [x] Implement reservation confirmation and cancellation.
- [x] Add transactional behavior and database locking.
- [x] Implement exception handling.
- [x] Add automated tests.
- [x] Verify selected API success and error scenarios manually.
- [x] Prepare project documentation.

## 9. Test Plan and Verification

### Automated Tests

The automated test suite covers:

- [x] Application context startup.
- [x] Reservation creation and reserved-stock updates.
- [x] Insufficient-stock rejection.
- [x] Atomic failure for multi-product reservations.
- [x] Reservation confirmation and stock deduction.
- [x] Reservation cancellation and stock release.
- [x] Repeated cancellation without releasing stock twice.
- [x] Repeated confirmation without deducting stock twice.
- [x] Requests involving nonexistent products.
- [x] Zero and negative quantities.
- [x] Confirmation of a cancelled reservation.
- [x] Confirmation of a nonexistent reservation.
- [x] Retrieval of reservations containing multiple products.
- [x] Concurrent reservation behavior.

### Manual Verification

Selected API scenarios were checked manually:

- [x] Product listing.
- [x] Creating, confirming, and cancelling reservations.
- [x] Rejecting insufficient-stock requests without partial reservation.
- [x] Rejecting malformed JSON.
- [x] Returning an error for a nonexistent reservation.
- [x] Rejecting invalid reservation state transitions.

### Additional Verification

- [ ] Verify database state persists across an application restart.
- [ ] Automate additional HTTP-level error scenarios if required.

The unchecked items are additional verification work and should not be treated as completed.

## 10. Documentation and Delivery

- [x] `README.md` — project overview, configuration, endpoints, and usage.
- [x] `PLAN.md` — implementation plan, design, and verification status.
- [x] `HELP.md` — setup help and technical references.
- [x] `AI_USAGE.md` — disclosure of AI assistance.
- [x] Automated test suite.

Before submission:

- [ ] Review the final Git changes.
- [ ] Confirm all intended files are committed.

## Planning Note

The implementation work began before Git was initialized. Therefore, the repository history cannot establish that this plan was committed before implementation began. The plan documents the intended scope, design, and current verification status.
