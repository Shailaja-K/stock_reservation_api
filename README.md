
# Stock Reservation API

A REST API built with Java, Spring Boot, Spring Data JPA, and PostgreSQL to manage product inventory and stock reservations.

The application supports creating reservations for multiple products, confirming or cancelling reservations, validating stock availability, and protecting inventory updates through database transactions and locking.

## Features

- List products with total, reserved, and available stock.
- Create products.
- Create reservations containing multiple products and quantities.
- Validate product existence and requested quantities.
- Reject reservations when sufficient stock is unavailable.
- Prevent partial stock reservations when a multi-product request fails.
- Confirm reservations and deduct stock.
- Cancel reservations and release reserved stock.
- Prevent repeated confirmation or cancellation from modifying stock again.
- Retrieve reservation details and status.
- Use transactions and database locking for stock-sensitive operations.
- Return appropriate HTTP error responses for supported failure scenarios.

## Technology Stack

- Java
- Spring Boot
- Spring Web MVC
- Spring Data JPA and Hibernate
- PostgreSQL
- Maven Wrapper
- JUnit and Spring Boot Test

## Prerequisites

- A compatible Java JDK
- PostgreSQL
- PowerShell on Windows, or an equivalent terminal on another operating system

## Database Configuration

Create a PostgreSQL database named `stock_reservation`.

The application reads the following environment variables:

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/stock_reservation` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | No default |

The relevant configuration in `application.properties` is:

```properties
spring.application.name=stock-reservation

spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/stock_reservation}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Set the database password in your PowerShell session without adding it to the repository:

```powershell
$env:DB_PASSWORD = "YOUR_LOCAL_PASSWORD"
$env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD
```

Replace `YOUR_LOCAL_PASSWORD` with your local PostgreSQL password.

## Running the Application

Open PowerShell in the project directory:

```powershell
$env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD
.\mvnw.cmd spring-boot:run
```

The API is normally available at:

`http://localhost:8080`

## Sample Product Data

The project includes `src/main/resources/seed.sql`, a manual SQL script that inserts sample products.

| Product ID | SKU | Name | Total Stock |
|---|---|---|---:|
| 1 | NOTE-001 | Notebook | 10 |
| 2 | PEN-001 | Pen | 5 |
| 3 | BAG-001 | Backpack | 2 |

The seed script is not automatically executed when the application starts. Execute it manually if you want to use these sample records. Product IDs may differ if the database already contains data.

## API Endpoints

### Products

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | List products and stock availability |
| POST | `/api/products` | Create a product |

Product responses include `id`, `sku`, `name`, `totalStock`, `reservedStock`, and `availableStock`.

Available stock is calculated as:

`availableStock = totalStock - reservedStock`

### Reservations

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/reservations` | Create a reservation |
| GET | `/api/reservations/{id}` | Retrieve reservation details |
| POST | `/api/reservations/{id}/confirm` | Confirm an active reservation |
| POST | `/api/reservations/{id}/cancel` | Cancel an active reservation |

## Example Usage

### 1. Create a Reservation

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

This requests 3 notebooks and 2 pens using the sample product IDs.

A successful request creates an `ACTIVE` reservation and increases the reserved quantities for the requested products.

The response contains the reservation details, including its ID, status, timestamps, and items. Use the returned reservation ID for subsequent requests.

### 2. Retrieve a Reservation

```http
GET /api/reservations/{id}
```

Returns the reservation details for the specified ID.

### 3. Confirm a Reservation

```http
POST /api/reservations/{id}/confirm
```

Confirmation deducts the reserved quantities from total stock, decreases reserved stock, and changes the reservation status to `CONFIRMED`.

### 4. Cancel a Reservation

```http
POST /api/reservations/{id}/cancel
```

Cancellation releases the reserved quantities without deducting total stock and changes the reservation status to `CANCELLED`.

## Reservation Rules

- Requested quantities must be positive.
- All requested products must exist.
- Sufficient available stock must exist for every requested product.
- A failed multi-product reservation must not retain partial stock changes.
- Only `ACTIVE` reservations can be confirmed or cancelled.
- Confirming a reservation deducts stock once.
- Cancelling a reservation releases reserved stock once.
- A cancelled reservation cannot be confirmed.
- Reservation state and inventory changes are handled transactionally.

## Error Handling

The application handles common failure scenarios, including:

| HTTP Status | Typical Scenario |
|---|---|
| `400 Bad Request` | Invalid quantity, insufficient stock, or malformed JSON |
| `404 Not Found` | Product or reservation does not exist |
| `409 Conflict` | Invalid reservation state transition |

The exact response depends on the exception and validation path. The implementation has been manually checked for selected success and error scenarios.

## Transaction and Concurrency Handling

Stock-sensitive operations use database transactions and pessimistic locking.

- Products are locked while stock is checked and updated.
- Product locks are acquired in ascending ID order to reduce deadlock risk.
- Reservation creation validates requested products and quantities within a transaction.
- Confirmation and cancellation lock the reservation before changing its state.
- Transactional rollback prevents partial inventory changes when an operation fails.

Automated concurrency tests exercise the implemented reservation behavior. These tests provide evidence for the scenarios tested, rather than a guarantee for every possible production workload.

## Running Tests

Run the automated test suite from the project root:

```powershell
$env:SPRING_DATASOURCE_PASSWORD = $env:DB_PASSWORD
.\mvnw.cmd clean test
```

The test suite covers:

- Reservation creation and reserved-stock updates.
- Insufficient-stock rejection.
- Atomic failure for multi-product reservations.
- Confirmation and stock deduction.
- Cancellation and stock release.
- Repeated confirmation and cancellation.
- Missing products and reservations.
- Zero and negative quantities.
- Confirmation of cancelled reservations.
- Reservations containing multiple products.
- Concurrent reservation behavior.
- Application context startup.

Selected HTTP error scenarios, including malformed JSON and invalid reservation state transitions, were also checked manually.

The suite does not automate every HTTP error scenario. Database persistence across an application restart has not been separately verified.

## Project Structure

```text
stock-reservation/
├── src/
│   ├── main/
│   │   ├── java/com/example/stock_reservation/
│   │   │   ├── controller/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── StockReservationApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── seed.sql
│   └── test/
│       └── java/com/example/stock_reservation/
├── .gitignore
├── AI_USAGE.md
├── HELP.md
├── PLAN.md
├── pom.xml
└── mvnw.cmd
```

The exact source files and package organization should be checked against the repository.

## AI Usage Disclosure

AI assistance was used during development for discussing implementation decisions, reviewing edge cases, supporting test development, and preparing documentation.

See [`AI_USAGE.md`](AI_USAGE.md) for the disclosure.

## Additional Documentation

- [`PLAN.md`](PLAN.md) — implementation plan, design decisions, and verification status.
- [`HELP.md`](HELP.md) — setup instructions and reference documentation.
- [`AI_USAGE.md`](AI_USAGE.md) — AI assistance disclosure.
