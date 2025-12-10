---
sidebar_position: 1
---

# Backend Overview

The backend is a Spring Boot 4.0 application with Java 25.

## Directory Structure

```
backend/src/main/java/com/example/demo/
├── DemoApplication.java    # Main entry point
├── DataSeeder.java         # Fallback data seeder
├── controller/             # REST controllers
│   ├── CustomerController.java
│   ├── PlanController.java
│   ├── DeviceController.java
│   ├── UsageRecordController.java
│   ├── SupportTicketController.java
│   ├── DashboardController.java
│   └── HealthController.java
├── model/                  # JPA entities
│   ├── Customer.java
│   ├── Plan.java
│   ├── Device.java
│   ├── UsageRecord.java
│   └── SupportTicket.java
└── repository/             # Spring Data repositories
    ├── CustomerRepository.java
    ├── PlanRepository.java
    ├── DeviceRepository.java
    ├── UsageRecordRepository.java
    └── SupportTicketRepository.java
```

## Key Features

### REST Controllers

All controllers use `@RestController` with standard CRUD operations:

```java
@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    @GetMapping
    public List<CustomerResponse> getAllCustomers() { }
    
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) { }
    
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CustomerRequest request) { }
    
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id, @RequestBody CustomerRequest request) { }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) { }
}
```

### Structured Logging

All endpoints use SLF4J for logging:

```java
private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

@PostMapping
public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CustomerRequest request) {
    log.info("Creating customer: email={}", request.email());
    // ...
    log.info("Customer created: id={}", saved.getId());
}
```

### OpenAPI Documentation

SpringDoc generates API documentation automatically:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## Database

Uses SQLite with Hibernate:

```properties
# application.properties
spring.datasource.url=jdbc:sqlite:app.db
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
```

## Running

```bash
# Development
cd backend && ./gradlew bootRun

# Build JAR
./gradlew build

# Run JAR
java -jar build/libs/*.jar
```
