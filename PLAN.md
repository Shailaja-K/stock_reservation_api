# Stock Reservation API — Implementation Plan

## 1. Objective

Build a REST API using Java, Spring Boot, Spring Data JPA, and PostgreSQL to manage product inventory and stock reservations.

The application must support multi-product reservations, stock availability validation, reservation confirmation and cancellation, and safe inventory updates under concurrent requests.

## 2. Technology Stack

- **Language:** Java
- **Framework:** Spring Boot
- **API:** Spring Web MVC
- **Persistence:** Spring Data JPA and Hibernate
- **Database:** PostgreSQL
- **Build:** Maven Wrapper
- **Testing:** JUnit and Spring Boot Test

## 3. Functional Requirements

### Product Management

- Retrieve the list of products.
- Create products with a unique SKU, name, and stock quantity.
- Track total stock and reserved stock.
- Calculate available stock as:

  `availableStock = totalStock - reservedStock`

### Reservation Management

- Create reservations containing multiple products and quantities.
- Validate that all requested products exist.
- Reject zero or negative quantities.
- Verify sufficient available stock for every requested product.
- Retrieve reservation details and status.
- Confirm active reservations.
- Cancel active reservations.
- Reject invalid state transitions.

### Inventory Consistency

- Ensure a failed multi-product reservation does not retain partial stock changes.
- Prevent available stock from becoming negative.
- Ensure confirmation deducts stock only once.
- Ensure cancellation releases reserved stock only once.
- Preserve reservation and inventory consistency through database transactions.

## 4. Reservation Lifecycle

A reservation follows these state transitions:

```text
             Create
               |
               v
            ACTIVE
            /    \
           /      \
          v        v
    CONFIRMED    CANCELLED
```

Only `ACTIVE` reservations may transition to `CONFIRMED` or `CANCELLED`.

Confirmed and cancelled reservations are terminal states. Repeated or conflicting state transitions must be rejected without modifying inventory again.

## 5. Inventory Update Strategy

### Reservation Creation

1. Receive the requested product IDs and quantities.
2. Validate the request and ensure quantities are positive.
3. Retrieve and lock the requested product rows.
4. Acquire product locks in ascending ID order to reduce deadlock risk.
5. Validate that every product exists and has sufficient available stock.
6. Increase reserved stock for all requested products.
7. Create the reservation and its items.
8. Commit the transaction only if the complete operation succeeds.

If any validation or persistence operation fails, roll back the transaction so no partial reservation remains.

### Reservation Confirmation

1. Retrieve and lock the reservation.
2. Verify that its status is `ACTIVE`.
3. Lock the associated product rows in a consistent order.
4. Deduct the reserved quantities from total stock.
5. Decrease reserved stock by the same quantities.
6. Update the reservation status to `CONFIRMED`.
7. Commit the changes in a single transaction.

### Reservation Cancellation

1. Retrieve and lock the reservation.
2. Verify that its status is `ACTIVE`.
3. Lock the associated product rows in a consistent order.
4. Decrease reserved stock by the reservation quantities.
5. Leave total stock unchanged.
6. Update the reservation status to `CANCELLED`.
7. Commit the changes in a single transaction.

The implementation should ensure that reservation status changes and inventory updates cannot succeed independently.

## 6. Concurrency Strategy

Concurrent reservation requests can otherwise read the same available stock and reserve more units than exist.

The planned protection mechanism is pessimistic database locking combined with transactional stock validation.

- Lock product rows before validating and modifying inventory.
- Acquire multiple product locks in ascending product ID order.
- Perform stock validation and updates within the same transaction.
- Lock reservations before processing confirmation or cancellation.
- Enforce valid state transitions to prevent duplicate inventory adjustments.
- Use transaction rollback when a stock-sensitive operation fails.

The concurrency tests should verify that competing requests cannot reserve more stock than is available and that the final inventory values remain consistent.

## 7. Database Design

The persistence model includes the following conceptual entities.

### Product

Stores:

- Product ID
- SKU
- Product name
- Total stock
- Reserved stock

Available stock is calculated from total stock and reserved stock.

### Reservation

Stores:

- Reservation ID
- Reservation status
- Creation timestamp
- Last-updated timestamp

### Reservation Item

Stores the products and quantities associated with a reservation, linking reservations to products.

The database should enforce appropriate relationships and uniqueness constraints. Product and reservation records must persist across application restarts.

## 8. API Design

### Products

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/products` | List products and inventory quantities |
| `POST` | `/api/products` | Create a product |

### Reservations

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/reservations` | Create a multi-product reservation |
| `GET` | `/api/reservations/{id}` | Retrieve reservation details |
| `POST` | `/api/reservations/{id}/confirm` | Confirm an active reservation |
| `POST` | `/api/reservations/{id}/cancel` | Cancel an active reservation |

Expected error handling includes invalid requests, insufficient stock, missing resources, and invalid reservation state transitions. Exact HTTP statuses must match the implemented exception-handling logic.

## 9. Testing Strategy

### Functional Tests

- Product creation and inventory retrieval.
- Successful single-product and multi-product reservations.
- Rejection of insufficient-stock requests.
- Rejection of missing products.
- Rejection of zero and negative quantities.
- Atomic rollback when one item in a multi-product request fails.
- Reservation retrieval and status verification.

### State Transition Tests

- Successful confirmation of an active reservation.
- Successful cancellation of an active reservation.
- Rejection of repeated confirmation.
- Rejection of repeated cancellation.
- Rejection of confirmation after cancellation.
- Verification that inventory is adjusted only once.

### Concurrency Tests

- Run competing reservation requests against limited stock.
- Verify that successful reservations never exceed available inventory