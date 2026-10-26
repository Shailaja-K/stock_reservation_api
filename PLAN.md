# Implementation Plan — Stock Reservation API

## 1. Problem Statement

Build a REST API using Java, Spring Boot, and PostgreSQL to manage product inventory and reservations. The API must support creating products, reserving stock, retrieving reservation details, confirming reservations, and cancelling reservations.

Inventory must remain consistent when requests fail or multiple clients attempt to reserve the same stock concurrently.

## 2. Acceptance Criteria

- Products can be created and listed with total, reserved, and available stock.
- A reservation can contain multiple products and quantities.
- Only positive quantities and existing products can be reserved.
- Insufficient stock rejects the entire reservation without partially reserving inventory.
- Creating a reservation increases reserved stock without immediately reducing total stock.
- Confirming an active reservation reduces both total and reserved stock.
- Cancelling an active reservation releases reserved stock without reducing total stock.
- A reservation cannot be confirmed or cancelled more than once.
- Missing resources and invalid state transitions return appropriate HTTP status codes and readable JSON errors.
- Concurrent requests cannot reserve more stock than is available.
- Automated tests cover core business rules and concurrency behavior.

## 3. Assumptions, Questions, and Scope

### Assumptions

- PostgreSQL is the persistent source of truth for inventory.
- Product IDs identify products in reservation requests; SKU values are unique.
- A reservation is either ACTIVE, CONFIRMED, or CANCELLED.
- Product quantities are positive integers.
- Available stock is calculated as `totalStock - reservedStock`.
- Confirmation consumes stock that was previously reserved; cancellation releases the reservation.

### Questions and ambiguities

- Should reservations expire automatically, or remain active until confirmed or cancelled?
- Should authentication, authorization, and user ownership be required?
- Are there business-specific maximum quantities or reservation time limits?

These questions should be clarified through the assignment's invitation channel if it is available. Until clarified, automatic expiry, authentication, and user ownership are outside the initial scope.

### Intentionally excluded

- Payment processing.
- Automatic reservation expiry and background jobs.
- Authentication and authorization.
- Distributed inventory across multiple databases or services.
- Frontend development and deployment infrastructure.

## 4. Proposed API

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/products` | List products and stock availability |
| POST | `/api/products` | Create a product |
| POST | `/api/reservations` | Reserve quantities of multiple products |
| GET | `/api/reservations/{id}` | Retrieve reservation details |
| POST | `/api/reservations/{id}/confirm` | Confirm an active reservation |
| POST | `/api/reservations/{id}/cancel` | Cancel an active reservation |

### Representative requests

Create a product:

```json
{
  "sku": "NOTE-001",
  "name": "Notebook",
  "totalStock": 10
}
```

Create a reservation for two products:

```json
{
  "1": 2,
  "2": 3
}
```

The reservation request maps product IDs to requested quantities.

A successful reservation should return its ID, status, timestamps, and reserved items. A failed request should return a JSON error message.

### Expected error categories

| Status | Meaning |
|---|---|
| 400 Bad Request | Malformed JSON, invalid quantity, or insufficient stock |
| 404 Not Found | Requested product or reservation does not exist |
| 409 Conflict | Invalid reservation state transition |

Exact response bodies should be verified against the implementation.

## 5. Database Design

### `products`

- `id`: primary key.
- `sku`: required, unique.
- `name`: required.
- `total_stock`: total remaining physical stock.
- `reserved_stock`: stock currently held by active reservations.

### `reservations`

- `id`: primary key.
- `status`: reservation state.
- `created_at`: creation timestamp.
- `updated_at`: last-update timestamp.

### `reservation_items`

- `id`: primary key.
- `reservation_id`: foreign key to `reservations.id`.
- `product_id`: foreign key to `products.id`.
- `quantity`: reserved quantity.

Each reservation can contain multiple items. Each item refers to one product.

```text
products 1 -------- * reservation_items * -------- 1 reservations
```

Foreign keys maintain valid relationships. The SKU uniqueness constraint prevents duplicate product SKUs. Quantities must be positive, and stock values must remain consistent. Database constraints should reinforce these rules where practical.

## 6. Components and Request Flow

- **Controllers:** expose REST endpoints, accept requests, and return HTTP responses.
- **Services:** implement reservation rules and transaction boundaries.
- **Repositories:** perform database access through Spring Data JPA.
- **Entities:** represent products, reservations, and reservation items.
- **DTOs:** shape reservation responses.
- **Exception handler:** converts expected application and request errors into appropriate HTTP responses.

Request flow:

```text
HTTP request
    |
Controller
    |
Service (@Transactional)
    |
Repository / JPA
    |
PostgreSQL
    |
Response DTO
    |
HTTP response
```

## 7. Transaction Safety and Concurrency

Reservation creation must execute as one database transaction. If any requested product is invalid or has insufficient stock, all changes must roll back.

Product rows should be locked while checking availability and updating reserved stock. When multiple products are involved, acquire locks in a consistent product-ID order to reduce deadlock risk.

Confirmation and cancellation must lock the reservation row and verify that its status is ACTIVE. Product stock updates and the status transition must commit together.

Without locking, two requests could read the same available quantity and both reserve it, causing overselling. Pessimistic row locking serializes competing updates to the same product; the later request checks the stock after the earlier transaction completes.

## 8. Implementation Sequence

1. Set up the Spring Boot project, dependencies, and PostgreSQL connection.
2. Define product, reservation, and reservation-item entities and repositories.
3. Implement product creation and listing.
4. Implement transactional reservation creation and availability checks.
5. Implement reservation retrieval, confirmation, and cancellation.
6. Add request validation and consistent exception handling.
7. Add automated tests for business rules, failure cases, and concurrency.
8. Document setup, API usage, design decisions, and test commands.

## 9. Test Plan and Priorities

### Priority 1 — Core correctness

- Application context starts successfully.
- Product creation and listing work.
- Valid reservation updates reserved stock.
- Insufficient stock rejects the entire multi-product reservation.
- Confirmation consumes stock and clears the reservation hold.
- Cancellation releases reserved stock.
- Repeated confirmation or cancellation is rejected.

### Priority 2 — Failure handling

- Empty requests, malformed JSON, and non-positive quantities are rejected.
- Missing products and reservations return appropriate errors.
- Confirming or cancelling a missing reservation returns 404.
- Invalid state transitions return 409.
- Failed operations leave inventory unchanged.

### Priority 3 — Concurrency and regression

- Competing reservations cannot oversell inventory.
- Run the complete automated test suite after implementation changes.
- Verify the documented setup and API examples.

If time is limited, prioritize atomic inventory updates, correct reservation state transitions, concurrency protection, and tests for insufficient stock and cancellation before optional features.

## 10. Documentation and Delivery

- Maintain `README.md` with setup instructions, database configuration, endpoint examples, and test commands.
- Maintain `PLAN.md` as the implementation plan.
- Document AI assistance and the extent of its use in `AI_USAGE.md`.
- Run the full test suite before submission.
- Commit the plan before any subsequent implementation changes.

**Planning note:** This document records the intended design and current understanding. Because implementation work preceded this Git initialization, the repository cannot establish that this plan was committed before the original implementation.
