# E-commerce & Inventory Management Backend

A learning-focused REST API built with Java 21, Spring Boot, Maven, and
MySQL. The project is being developed incrementally with a
modular-monolith architecture and production-oriented practices.

> **Status:** Core domain foundation, DTO/mapping, initial services, and
> CRUD controllers are completed. Controller integration testing is
> next. Authentication, standardized errors, advanced stock workflows,
> and infrastructure features are planned.

## Tech Stack

-   Java 21
-   Spring Boot, Spring Web
-   Spring Data JPA / Hibernate
-   MySQL
-   Maven
-   Jakarta Bean Validation
-   Lombok
-   MapStruct
-   Postman for manual API testing

## Architecture

``` text
Client / Postman
      |
      v
Controller (HTTP/API)
      |
      v
Service (business operations)
      |
      v
Repository (Spring Data JPA)
      |
      v
MySQL
```

The application is organized by feature. Controllers accept requests and
return DTOs; services coordinate business logic; repositories handle
persistence; MapStruct converts between entities and DTOs.

## Completed So Far

### Project and database foundation

-   Created the Spring Boot Maven project and configured local MySQL.
-   Designed the initial domain model and relationships.
-   Created core entities and tables: `Role`, `User`, `Category`,
    `Product`, and `Inventory`.
-   Current entity identifiers use integer types (`int` / `Integer`).
-   Added entity timestamp lifecycle handling.
-   Added status enums for users, categories, and products.
-   Added an inventory `@Version` field as the foundation for optimistic
    locking.

### Repositories, DTOs, and mapping

-   Created Spring Data repositories for the core entities.
-   Added request/response DTOs for user, category, product, and
    inventory features.
-   Configured MapStruct with Lombok annotation processing and created
    the corresponding mapper interfaces.

### Services

Initial service interfaces and implementations are complete for: -
User - Category - Product - Inventory

Current service logic includes basic CRUD operations where applicable,
duplicate checks (email, category name, SKU, and inventory per product),
and resolving related entities such as a product's category in the
service layer.

### Controllers

Initial REST controllers are implemented for: - `CategoryController` -
`UserController` - `ProductController` - `InventoryController`

They delegate to services and return DTOs through `ResponseEntity`.

## Current API Endpoints

Local base URL: `http://localhost:8080`

  -----------------------------------------------------------------------------------------------
Feature           Method            Endpoint                                  Purpose
  ----------------- ----------------- ----------------------------------------- -----------------
Categories        POST              `/api/categories`                         Create category

Categories        GET               `/api/categories/{id}`                    Get category by
ID

Categories        GET               `/api/categories`                         List categories

Categories        PUT               `/api/categories/{id}`                    Update category

Categories        DELETE            `/api/categories/{id}`                    Delete category

Users             POST              `/api/users`                              Create user

Users             GET               `/api/users/{id}`                         Get user by ID

Users             GET               `/api/users`                              List users

Users             PUT               `/api/users/{id}`                         Update user
name/email

Users             DELETE            `/api/users/{id}`                         Delete user

Products          POST              `/api/products`                           Create product

Products          GET               `/api/products/{id}`                      Get product by ID

Products          GET               `/api/products`                           List products

Products          PUT               `/api/products/{id}`                      Update product

Products          DELETE            `/api/products/{id}`                      Delete product

Inventory         POST              `/api/inventory`                          Create inventory
for product

Inventory         GET               `/api/inventory/{id}`                     Get inventory by
inventory ID

Inventory         GET               `/api/inventory/product/{productId}`      Get inventory by
product ID

Inventory         GET               `/api/inventory`                          List inventory

Inventory         PUT               `/api/inventory/{id}/stock?quantity=20`   Set stock
quantity (basic
implementation)
  -----------------------------------------------------------------------------------------------

## Example Requests

### Create category

`POST /api/categories`

``` json
{
  "name": "Electronics",
  "description": "Electronic devices and accessories"
}
```

### Create product

Requires an existing category ID.

`POST /api/products`

``` json
{
  "name": "Mechanical Keyboard",
  "description": "RGB keyboard",
  "sku": "KB-001",
  "price": 4500,
  "categoryId": 1
}
```

### Create inventory

Requires an existing product ID.

`POST /api/inventory`

``` json
{
  "productId": 1,
  "quantity": 10
}
```

Example response:

``` json
{
  "id": 1,
  "productId": 1,
  "quantity": 10,
  "reservedQuantity": 0,
  "availableQuantity": 10,
  "version": 0
}
```

`availableQuantity` is derived as `quantity - reservedQuantity`, not
stored separately.

## Local Setup

### Requirements

-   JDK 21
-   Maven (or project Maven wrapper, if present)
-   MySQL Server
-   Postman or another REST client

### 1. Create database

``` sql
CREATE DATABASE ecommerce_db;
```

### 2. Configure `src/main/resources/application.properties`

``` properties
spring.datasource.url=jdbc:DB_URL
spring.datasource.username=DB_USERNAME
spring.datasource.password=DB_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Replace the placeholder with your local password. Do not commit real
credentials. Use environment variables or a secrets manager for
shared/deployed environments.

### 3. Run

From the project root:

``` bash
./mvnw spring-boot:run
```

Windows PowerShell:

``` powershell
.\mvnw.cmd spring-boot:run
```

If there is no Maven wrapper:

``` bash
mvn spring-boot:run
```

## Suggested Manual Test Order

1.  Ensure a role row exists in MySQL (there is not yet a role-creation
    controller in the completed API list).
2.  Create a user with that existing `roleId`.
3.  Create a category.
4.  Create a product using the existing `categoryId`.
5.  Create inventory using the existing `productId`.
6.  Test the GET, update, and delete endpoints.
7.  Test invalid inputs and missing related IDs.

## Current Limitations

This is an in-progress learning project, not yet production-ready: -
Authentication and authorization are not complete; do not expose the
current user endpoints publicly in a deployed environment. - Password
hashing/authentication is planned; use only dummy local credentials and
never submit a real password to the current development flow. - Some
service errors still use generic runtime exceptions; custom exceptions
and global error handling are planned. - Stock update currently sets an
absolute quantity; robust inventory operations, transaction boundaries,
concurrency conflict handling, and movement auditing are still
planned. - `@Version` is present as a foundation, but complete conflict
handling is not yet implemented. - Automated and full integration tests
are pending. - Search/filter/pagination, carts, orders, payment
idempotency, Redis, Kafka, Docker Compose, API docs, and deployment are
future milestones.

## Roadmap

1.  Review and integration-test the current controllers with Postman.
2.  Complete validation and edge-case handling.
3.  Add global exception handling and consistent error response bodies.
4.  Implement JWT authentication, refresh tokens, password hashing, and
    role-based authorization.
5.  Add product search, filtering, sorting, and pagination.
6.  Build robust inventory operations with transactions, optimistic
    locking, and inventory movement history.
7.  Implement carts, orders, and order status history.
8.  Add simulated payments and idempotency.
9.  Add JUnit/Mockito tests.
10. Introduce Redis caching and Kafka events.
11. Add Docker Compose, API documentation, and deployment setup.

## Development Principles

-   Keep controllers thin; put business rules in services.
-   Use DTOs as the public API contract, not JPA entities.
-   Resolve foreign-key relationships in the service layer.
-   Make stock operations transactional and auditable as the inventory
    module matures.
-   Complete and verify each phase before moving to the next.

------------------------------------------------------------------------
