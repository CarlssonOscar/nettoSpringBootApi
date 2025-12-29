# Architecture & Design Principles

> **Reference document** - linked from main copilot-instructions.md  
> Consult this when making architectural decisions or designing new components

---

## Architectural Patterns

### Layered Architecture (Spring Boot Standard)

**Core Principles:**
- Clear separation between presentation, business logic, and data access
- Each layer has specific responsibilities
- Dependencies flow downward (Controller → Service → Repository)
- Lower layers should not depend on higher layers

```
┌─────────────────────────────────────┐
│      Controller Layer (REST API)    │  ← HTTP requests/responses
├─────────────────────────────────────┤
│         Service Layer               │  ← Business logic
├─────────────────────────────────────┤
│        Repository Layer             │  ← Data access (JPA)
├─────────────────────────────────────┤
│          Entity Layer               │  ← Domain model
└─────────────────────────────────────┘
```

**Benefits:**
- Testability: Business logic can be tested without HTTP or database
- Flexibility: Easy to swap implementations
- Maintainability: Clear separation of concerns

### Object-Oriented Programming

- Use encapsulation, inheritance, and polymorphism where they clarify the model
- Prefer interfaces + composition over deep inheritance hierarchies
- Keep inheritance trees shallow (max 2-3 levels)

---

## SOLID Principles

### Single Responsibility Principle (SRP)
- Each class and method should have one clear reason to change
- If you can describe a class with "AND", it probably violates SRP
- Extract multiple responsibilities into separate classes

### Open/Closed Principle (OCP)
- Open for extension, closed for modification
- Prefer strategies, policies, and events over sprawling conditionals
- Use interfaces and dependency injection to enable extension points

### Liskov Substitution Principle (LSP)
- Subtypes should be usable wherever the base type is expected
- No surprising behavior when substituting implementations
- Maintain contracts defined by base types/interfaces

### Interface Segregation Principle (ISP)
- Many small, focused interfaces rather than one large "god interface"
- Clients should not depend on interfaces they don't use
- Split large interfaces into role-based smaller ones

### Dependency Inversion Principle (DIP)
- Depend on abstractions (interfaces), not concretions
- High-level modules should not depend on low-level modules
- Both should depend on abstractions

---

## Other Design Principles

### DRY (Don't Repeat Yourself)
- Avoid duplicated logic; extract shared behavior
- **Exception**: See pragmatic API validation sharing below

### KISS (Keep It Simple, Stupid)
- Choose the simplest solution that solves the problem well
- Avoid over-engineering
- Prefer clear and simple over clever and complex

### YAGNI (You Aren't Gonna Need It)
- Do not implement things "just in case"
- Build what is needed now, not what might be needed later
- Refactor when actual needs emerge

### Composition over Inheritance
- Especially for behavior sharing
- Use interfaces and composition for flexibility
- Reserve inheritance for true "is-a" relationships

---

## Pragmatic API Design

> **Philosophy**: Security and correctness trump convenience for business logic.

### Backend as Source of Truth

**Security-Critical:**
- All authentication and authorization decisions
- All data validation before persistence
- All business rule enforcement

**Data-Critical:**
- Calculations that affect data integrity
- Transactional operations
- Audit logging

### API Design Principles

```
Correctness and Consistency > Convenience
```
- Never duplicate core business rules across services
- Expose via clear API contracts
- Single source of truth in services

---

## Layer Responsibilities

### Controller Layer (Presentation)
- REST endpoints
- Request/response mapping
- Input validation (via @Valid)
- Delegates to Service layer
- **Must be thin**: no business logic

### Service Layer (Business Logic)
- Business rules and orchestration
- Transaction management (@Transactional)
- Calls to repositories
- DTO mapping
- **Core of the application**

### Repository Layer (Data Access)
- JPA repository interfaces
- Custom queries (JPQL, native SQL)
- No business logic
- **Only data access**

### Entity Layer (Domain Model)
- JPA entities
- Domain value objects
- Basic domain validation
- Relationships between entities

### DTO Layer (Data Transfer)
- Request/Response objects
- Validation annotations
- Mapping between layers
- API contracts

---

## Spring Boot Specific Patterns

### Constructor Injection (Preferred)

```java
// ✅ PREFERRED - Constructor injection with Lombok
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
}

// ✅ ALSO OK - Explicit constructor
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// ❌ AVOID - Field injection
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
}
```

### Transaction Management

```java
// ✅ GOOD - Transaction at service level
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    
    @Transactional
    public Order createOrder(CreateOrderDto dto) {
        Order order = new Order(dto);
        orderRepository.save(order);
        paymentService.processPayment(order);
        return order;
    }
    
    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
```

### Configuration Classes

```java
// ✅ GOOD - Separate configuration
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Security configuration
    }
}

@Configuration
public class JpaConfig {
    // JPA/Hibernate configuration
}
```

---

## Anti-Patterns to Avoid

❌ **God Classes**: Classes that know/do too much  
❌ **Anemic Domain Model**: Entities with no behavior, only getters/setters  
❌ **Service Locator**: Hidden dependencies (use constructor injection)  
❌ **Circular Dependencies**: Between services or layers  
❌ **Leaky Abstractions**: Infrastructure details exposed through interfaces  
❌ **Fat Interfaces**: Interfaces with too many methods  
❌ **Magic Values**: Hard-coded strings, numbers without constants  
❌ **Layer Mixing**: Business logic in controllers, data access in services  
❌ **Field Injection**: Use constructor injection instead  

---

## Package Structure

### Recommended Structure

```
com.example.demo/
├── DemoApplication.java
├── config/                    # @Configuration classes
│   ├── SecurityConfig.java
│   └── JpaConfig.java
├── controller/                # REST controllers
│   └── UserController.java
├── service/                   # Business logic
│   ├── UserService.java
│   └── impl/
│       └── UserServiceImpl.java
├── repository/                # JPA repositories
│   └── UserRepository.java
├── entity/                    # JPA entities
│   └── User.java
├── dto/                       # Data Transfer Objects
│   ├── request/
│   │   └── CreateUserDto.java
│   └── response/
│       └── UserDto.java
├── mapper/                    # Entity ↔ DTO mapping
│   └── UserMapper.java
├── exception/                 # Custom exceptions
│   ├── UserNotFoundException.java
│   └── handler/
│       └── GlobalExceptionHandler.java
└── util/                      # Utility classes
    └── DateUtils.java
```

---

## When to Deviate

These principles are guidelines, not laws. Deviate when:
- **Performance is critical** and the abstraction cost is measurable
- **Simple solutions** don't benefit from added complexity
- **Pragmatic trade-offs** favor a different approach

**But:**
- Document why you're deviating
- Get team consensus for architectural deviations
- Revisit the decision if circumstances change

---

*Last Updated: 2025-12-29*
