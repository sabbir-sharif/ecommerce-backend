# E-commerce & Inventory Management Backend

A production-oriented REST API built with **Java 21, Spring Boot, Spring Security, Maven, and MySQL**.

This project is being developed incrementally using a **modular-monolith architecture** and practical backend engineering principles. It covers authentication, product management, inventory control, shopping carts, order processing, payment simulation, and inventory movement auditing.

> **Project Status:** Core backend features (Phases 1–3) have been implemented. Phase 4 — Testing & Verification — is the current development milestone. Redis caching and event-driven architecture are planned for subsequent phases.

---

## Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA / Hibernate
- Spring Security
- JWT Authentication
- MySQL
- Maven
- Jakarta Bean Validation
- Lombok
- MapStruct
- JUnit 5
- Mockito
- Postman

---

## Architecture

The application follows a feature-based modular-monolith architecture.

```text
Client / Postman
       |
       v
Spring Security / JWT
       |
       v
Controller Layer
       |
       v
Service Layer
       |
       v
Repository Layer
       |
       v
MySQL Database
```

### Layer Responsibilities

- **Security:** Authenticates users and enforces role-based access.
- **Controller:** Handles HTTP requests, validation and API responses.
- **Service:** Contains business rules and coordinates operations.
- **Repository:** Handles database access using Spring Data JPA.
- **Entity:** Represents persistent domain objects.
- **DTO:** Defines API request and response structures.
- **Mapper:** Uses MapStruct to convert between entities and DTOs.

The project uses feature-based packages to keep related functionality organized.

---

## Implemented Features

### 1. Authentication and Authorization

- User registration and login.
- Password hashing.
- JWT-based authentication.
- Role-based access control.
- USER and ADMIN roles.
- Protected API endpoints.

### 2. User Management

- User creation and retrieval.
- User information management.
- Email uniqueness validation.
- User status management.

### 3. Category Management

- Create, retrieve, update and delete categories.
- Category-product relationship.
- Duplicate category validation.

### 4. Product Management

- Product CRUD operations.
- SKU uniqueness validation.
- Product-category association.
- Product status management.
- Search and filtering.
- Pagination and sorting.
- Product DTO mapping.

### 5. Inventory Management

- Inventory creation and retrieval.
- Stock quantity management.
- Reserved stock tracking.
- Available quantity calculation.
- Optimistic locking using JPA `@Version`.
- Inventory consistency checks.

The available quantity is calculated as:

```text
availableQuantity = quantity - reservedQuantity
```

Physical stock and reserved stock are tracked separately.

### 6. Shopping Cart

- Add products to cart.
- Update item quantities.
- Remove cart items.
- Retrieve the authenticated user's cart.
- Validate product availability.
- Validate requested quantities.

### 7. Order Management

- Place orders from the shopping cart.
- Generate order information.
- Store order item snapshots.
- Preserve product name, SKU and unit price at purchase time.
- Retrieve customer order history.
- Retrieve individual orders.
- Filter orders by status.
- Paginate order results.
- Admin order search.

### 8. Inventory Reservation and Fulfillment

- Reserve inventory when an order is placed.
- Release reserved stock when an eligible order is cancelled.
- Confirm pending orders.
- Fulfill confirmed orders after successful payment.
- Deduct physical inventory during fulfillment.
- Use transaction boundaries to maintain consistency.
- Use pessimistic locking for critical order operations.

### 9. Payment Simulation and Idempotency

- Simulated payment success and failure.
- Payment records associated with orders.
- Idempotency key support.
- Protection against duplicate payment processing.
- Payment verification before fulfillment.

This is a simulated payment workflow, not a real payment gateway integration.

### 10. Audit Trails and Inventory Movement History

- Record inventory-changing operations.
- Track stock before and after changes.
- Track reserved quantity before and after changes.
- Record movement types and reasons.
- Associate movements with relevant references and users where available.
- Retrieve movement history with pagination.

Movement types include:

- `STOCK_ADDED`
- `STOCK_REMOVED`
- `STOCK_ADJUSTED`
- `STOCK_RESERVED`
- `STOCK_RELEASED`
- `STOCK_FULFILLED`

---

## Order Lifecycle

The current order lifecycle is:

```text
        PENDING
           |
           v
       CONFIRMED
           |
           v
       FULFILLED
```

A pending order can also be cancelled:

```text
        PENDING
           |
           v
       CANCELLED
```

Important inventory behavior:

| Operation | Physical Quantity | Reserved Quantity |
|---|---:|---:|
| Order placement | Unchanged | Increases |
| Order cancellation | Unchanged | Decreases |
| Order fulfillment | Decreases | Decreases |

Fulfillment requires a confirmed order and successful payment.

---

## API Overview

Local base URL:

```text
http://localhost:8080
```

The following are the main API routes currently established in the project. Refer to the controllers and Postman collection for exact request and response details.

| Feature | Method | Endpoint |
|---|---|---|
| Register | POST | `/api/auth/register` |
| Login | POST | `/api/auth/login` |
| Products | GET | `/api/products` |
| Product details | GET | `/api/products/{id}` |
| Create product | POST | `/api/products` |
| Update product | PUT | `/api/products/{id}` |
| Categories | GET | `/api/categories` |
| Create category | POST | `/api/categories` |
| Inventory | GET | `/api/inventory` |
| Create inventory | POST | `/api/inventory` |
| Cart | GET | `/api/cart` |
| Place order | POST | `/api/orders` |
| Customer order history | GET | `/api/orders` |
| Order details | GET | `/api/orders/{orderId}` |
| Confirm order | PATCH | `/api/orders/{orderId}/confirm` |
| Fulfill order | PATCH | `/api/orders/{orderId}/fulfill` |
| Simulate payment | POST | `/api/payments/orders/{orderId}` |
| Inventory movements | GET | `/api/inventory/movements` |
| Inventory movement history | GET | `/api/inventory/movements/inventory/{inventoryId}` |
| Movement by reference | GET | `/api/inventory/movements/reference/{referenceType}/{referenceId}` |

Some features have additional routes for updates, deletion, filtering, pagination and administration.

---

## Standard API Response

The application uses a common `ApiResponse` wrapper for standardized API responses.

Conceptual example:

```json
{
  "status": "SUCCESS",
  "message": "Operation completed successfully",
  "body": {},
  "timestamp": "2026-10-30T12:00:00"
}
```

Actual status values and timestamp formatting depend on the implementation.

---

## Project Structure

The application uses a feature-based package structure.

```text
com.ecommerce
│
├── auth
├── user
├── role
├── category
├── product
├── inventory
├── cart
├── order
├── payment
├── common
│   ├── config
│   ├── exception
│   └── response
│
└── EcommerceApplication.java
```

Each feature can contain its own:

```text
feature/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
└── mapper/
```

The exact package organization may vary slightly by feature.

---

## Local Setup

### Requirements

- JDK 21
- Maven or Maven Wrapper
- MySQL Server
- Postman

### 1. Clone the repository

```bash
git clone <YOUR_REPOSITORY_URL>
cd ecommerce-backend
```

### 2. Create the database

```sql
CREATE DATABASE ecommerce_db;
```

### 3. Configure database credentials

Configure `src/main/resources/application.properties`:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/ecommerce_db}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD}

spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Set the appropriate environment variables for your local environment.

Do not commit actual database credentials, JWT secrets, or other sensitive configuration.

### 4. Run the application

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Without Maven Wrapper:

```bash
mvn spring-boot:run
```

---

## Postman Collection

The project includes a Postman collection under:

```text
docs/postman/
└── Postman_Collection_Import_v1.json
```

Import the collection into Postman and configure the appropriate environment variables, including the local base URL and authentication token.

---

## Development Roadmap

The project is developed in phases.

| Phase | Description | Status |
|---|---|---|
| Phase 1 | Project and domain foundation | Implemented |
| Phase 2 | Authentication and security | Implemented |
| Phase 3 | Core e-commerce backend features (3.1–3.10) | Implemented; verification pending |
| Phase 4 | Testing and verification | Current |
| Phase 5 | Redis caching | Planned |
| Phase 6 | Event-driven architecture with Kafka | Planned |

### Phase 4 — Testing and Verification

The next milestone focuses on verifying the complete Phase 3 implementation.

Planned testing includes:

- JUnit 5 and Mockito unit testing.
- Spring Boot integration testing.
- Authentication and authorization testing.
- Product, category and inventory testing.
- Cart and order workflow testing.
- Inventory reservation and fulfillment testing.
- Payment and idempotency testing.
- Inventory movement and audit testing.
- Transaction rollback and concurrency testing.
- End-to-end API testing using Postman.

### Phase 5 — Redis Caching

Planned improvements:

- Redis integration.
- Product read caching.
- Cache expiration and invalidation.
- Cache consistency during product updates.

### Phase 6 — Event-Driven Architecture

Planned improvements:

- Apache Kafka integration.
- Event publishing and consumption.
- Asynchronous order processing.
- Event-driven inventory and notification workflows.
- Reliable event handling and failure recovery.

---

## Current Project Scope and Limitations

This is a learning-focused backend project using production-oriented design principles. It is not yet a fully production-deployed application.

- Payment processing is simulated; no real payment provider is integrated.
- Phase 4 automated and end-to-end verification is in progress.
- Redis caching and Kafka-based event processing have not yet been implemented.
- Docker-based deployment and production infrastructure are future improvements.
- Performance, load testing, monitoring and deployment hardening remain future work.

The implementation and testing status of individual features should be distinguished; implemented functionality is not automatically considered fully verified.

---

## Project Goal

The goal is to develop a maintainable, secure and production-oriented e-commerce backend while demonstrating practical knowledge of:

- Java and Spring Boot.
- REST API design.
- Database modeling and persistence.
- Authentication and authorization.
- Transaction management.
- Inventory consistency and concurrency control.
- Payment idempotency.
- Automated testing.
- Caching.
- Event-driven architecture.

This project is developed incrementally, with emphasis on understanding architectural decisions and being able to explain and defend the implementation.

## Roadmap

1. ✅ Review and integration-test the current controllers with Postman.
2. ✅ Complete validation and edge-case handling.
3. ✅ Add global exception handling and consistent error response bodies.
4. ✅ Implement JWT authentication, refresh tokens, password hashing, and
   role-based authorization.
5. ✅ Build robust inventory operations with transactions, optimistic 
   locking, and inventory movement history.
6. ✅ Add product search, filtering, sorting, and pagination.
   locking, and inventory movement history.
7. ✅ Implement carts, orders, and order status history.
8. ✅ Add simulated payments and idempotency.
9. 🔄 Add JUnit/Mockito tests.
10. ⬜ Introduce Redis caching and Kafka events.
11. ⬜ Add Docker Compose, API documentation, and deployment setup.

[//]: # (1.  Review and integration-test the current controllers with Postman.)

[//]: # (2.  Complete validation and edge-case handling.)

[//]: # (3.  Add global exception handling and consistent error response bodies.)

[//]: # (4.  Implement JWT authentication, refresh tokens, password hashing, and)

[//]: # (    role-based authorization.)

[//]: # (5.  Add product search, filtering, sorting, and pagination.)

[//]: # (6.  Build robust inventory operations with transactions, optimistic)

[//]: # (    locking, and inventory movement history.)

[//]: # (7.  Implement carts, orders, and order status history.)

[//]: # (8.  Add simulated payments and idempotency.)

[//]: # (9.  Add JUnit/Mockito tests.)

[//]: # (10. Introduce Redis caching and Kafka events.)

[//]: # (11. Add Docker Compose, API documentation, and deployment setup.)

[//]: # (## Development Principles)

[//]: # ()
[//]: # (-   Keep controllers thin; put business rules in services.)

[//]: # (-   Use DTOs as the public API contract, not JPA entities.)

[//]: # (-   Resolve foreign-key relationships in the service layer.)

[//]: # (-   Make stock operations transactional and auditable as the inventory)

[//]: # (    module matures.)

[//]: # (-   Complete and verify each phase before moving to the next.)

------------------------------------------------------------------------
