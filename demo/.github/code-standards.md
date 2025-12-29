# Code Quality Standards

> **Reference document** - linked from main copilot-instructions.md  
> Follow these standards for all code contributions

---

## Naming Conventions

### Java / Spring Boot

| **Type** | **Convention** | **Example** |
|----------|---------------|-------------|
| Classes | `PascalCase` | `UserService`, `OrderRepository` |
| Interfaces | `PascalCase` | `UserService`, `OrderRepository` |
| Methods | `camelCase` | `getUserById`, `processOrder` |
| Fields | `camelCase` | `userRepository`, `orderService` |
| Local Variables | `camelCase` | `userId`, `orderItems` |
| Parameters | `camelCase` | `userId`, `includeDeleted` |
| Constants | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` |
| Enums | `PascalCase` | `OrderStatus`, `UserRole` |
| Enum Values | `UPPER_SNAKE_CASE` | `OrderStatus.PENDING`, `UserRole.ADMINISTRATOR` |
| Packages | `lowercase` | `com.example.demo.service` |

### General Naming Principles

✅ **DO:**
- Use intention-revealing names
- Prefer longer and clear over short and cryptic
- Use pronounceable names
- Use searchable names
- Avoid encodings (Hungarian notation)

❌ **DON'T:**
- Use single-letter names (except loop counters)
- Use abbreviations unless universally understood
- Use misleading names
- Use names that differ only in casing

**Examples:**

```java
// ❌ BAD
int d; // What is 'd'?
String ymdstr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
User u = getU(1); // What is 'u'?

// ✅ GOOD
int elapsedTimeInDays;
String currentDateFormatted = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
User user = getUserById(1);
```

---

## Structure & Responsibilities

### Single Responsibility Principle

**Each class should have ONE reason to change:**

```java
// ❌ BAD - Multiple responsibilities
public class UserManager {
    public void createUser(User user) { }
    public void sendWelcomeEmail(User user) { }
    public void generateUserReport() { }
    public void validateUserData(User user) { }
}

// ✅ GOOD - Separated responsibilities
@Service
public class UserService {
    public void createUser(User user) { }
}

@Service
public class EmailService {
    public void sendWelcomeEmail(User user) { }
}

@Service
public class ReportService {
    public void generateUserReport() { }
}

@Component
public class UserValidator {
    public ValidationResult validate(User user) { }
}
```

### Method Size

**Keep methods short and focused:**

- Target: 5-15 lines
- Warning: 15-30 lines (consider refactoring)
- Red flag: 30+ lines (must refactor)

**Refactoring strategy:**

```java
// ❌ BAD - Long method doing too much
public void processOrder(Order order) {
    // 50 lines of validation
    // 30 lines of calculation
    // 20 lines of database operations
    // 15 lines of email sending
}

// ✅ GOOD - Extracted to focused methods
public void processOrder(Order order) {
    validateOrder(order);
    BigDecimal total = calculateOrderTotal(order);
    saveOrderToDatabase(order, total);
    sendOrderConfirmationEmail(order);
}

private void validateOrder(Order order) { }
private BigDecimal calculateOrderTotal(Order order) { }
private void saveOrderToDatabase(Order order, BigDecimal total) { }
private void sendOrderConfirmationEmail(Order order) { }
```

---

## Dependency Injection - CRITICAL RULE

### The Golden Rule

**NEVER use `new` for services, repositories, or external clients.**

✅ **Allowed to use `new`:**
- DTOs (Data Transfer Objects)
- Value objects
- Domain entities
- Simple data structures (ArrayList, HashMap, etc.)
- Framework types (StringBuilder, LocalDateTime, etc.)

❌ **FORBIDDEN to use `new`:**
- Services (@Service)
- Repositories (@Repository)
- External API clients
- EntityManager
- Anything managed by Spring

### Why This Matters

Direct instantiation with `new`:
- ❌ Creates tight coupling
- ❌ Makes testing difficult (can't inject mocks)
- ❌ Breaks Dependency Inversion Principle
- ❌ Hides dependencies
- ❌ Prevents Spring lifecycle management

### Examples

```java
// ❌ WRONG - Direct instantiation
@Service
public class OrderService {
    public void processOrder(Long orderId) {
        OrderRepository repository = new OrderRepository(); // WRONG
        EmailService emailService = new EmailService();      // WRONG
        
        Order order = repository.findById(orderId);
        // ...
    }
}

// ✅ CORRECT - Constructor injection (preferred)
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    
    public void processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        // ...
    }
}

// ✅ CORRECT - Using 'new' for DTOs and value objects
public OrderDto mapToDto(Order order) {
    return new OrderDto(  // OK - DTO
        order.getId(),
        order.getItems().stream()
            .map(i -> new OrderItemDto(i.getProductId(), i.getQuantity()))  // OK - DTO
            .toList()
    );
}
```

---

## Things to Avoid

### Magic Values

❌ **Bad:**
```java
if (user.getRole().equals("admin"))
if (order.getStatus() == 1)
if (retry < 3)
```

✅ **Good:**
```java
if (user.getRole() == UserRole.ADMINISTRATOR)
if (order.getStatus() == OrderStatus.PENDING)
if (retry < MAX_RETRY_COUNT)
```

### Fat Interfaces

❌ **Bad:**
```java
public interface UserService {
    User getUser(Long id);
    void createUser(User user);
    void deleteUser(Long id);
    void sendEmail(Long userId);
    void generateReport(Long userId);
    void exportData(Long userId);
    void importData(String data);
    // 20 more methods...
}
```

✅ **Good:**
```java
public interface UserService {
    User getUser(Long id);
    void createUser(User user);
    void deleteUser(Long id);
}

public interface EmailService {
    void sendEmail(Long userId);
}

public interface ReportService {
    void generateReport(Long userId);
}
```

### Static State and Singletons

❌ **Avoid:**
```java
public class UserCache {
    private static Map<Long, User> cache = new HashMap<>();
    
    public static void add(Long id, User user) {
        cache.put(id, user);
    }
}
```

✅ **Prefer:**
```java
public interface UserCache {
    void add(Long id, User user);
    Optional<User> get(Long id);
}

@Component
public class UserCacheImpl implements UserCache {
    private final Map<Long, User> cache = new ConcurrentHashMap<>();
    
    @Override
    public void add(Long id, User user) {
        cache.put(id, user);
    }
    
    @Override
    public Optional<User> get(Long id) {
        return Optional.ofNullable(cache.get(id));
    }
}
```

### Layer Mixing

❌ **Bad:**
```java
// Business logic in controller
@PostMapping
public ResponseEntity<?> createOrder(@RequestBody OrderDto dto) {
    // Validation logic
    if (dto.getItems().isEmpty()) return ResponseEntity.badRequest().build();
    
    // Business logic
    BigDecimal total = dto.getItems().stream()
        .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal discount = total.compareTo(BigDecimal.valueOf(100)) > 0 
        ? total.multiply(BigDecimal.valueOf(0.1)) 
        : BigDecimal.ZERO;
    
    // Data access
    Order order = new Order();
    order.setTotal(total.subtract(discount));
    orderRepository.save(order);
    
    return ResponseEntity.ok().build();
}

// Infrastructure details in domain
@Entity
public class Order {
    public void save() {
        // Direct database access from entity - WRONG
    }
}
```

✅ **Good:**
```java
// Thin controller
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@Valid @RequestBody CreateOrderDto dto) {
        OrderDto result = orderService.createOrder(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}

// Business logic in service
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    
    @Transactional
    public OrderDto createOrder(CreateOrderDto dto) {
        Order order = orderMapper.toEntity(dto);
        order.calculateTotal();
        Order saved = orderRepository.save(order);
        return orderMapper.toDto(saved);
    }
}
```

---

## Comments & Documentation

### When to Comment

✅ **Good reasons to comment:**
- Explain **why** a decision was made
- Document edge cases or non-obvious behavior
- Clarify complex algorithms
- Note future improvements or known limitations
- Explain workarounds for bugs in external libraries

❌ **Bad reasons to comment:**
- Explain **what** the code does (code should be self-explanatory)
- Restate the code
- Leave commented-out code
- Add noise comments

### Comment Rules

- Must be in English
- Short (1-2 lines) and focused
- Explain decisions or context, not syntax
- Use Javadoc for public APIs

### Examples

```java
// ❌ BAD - Restating the code
// Get the user by ID
User user = getUserById(id);

// ❌ BAD - Commented-out code
// Order result = oldMethod();
Order result = newMethod();

// ✅ GOOD - Explaining why
// Using ThreadLocal to avoid allocation in hot path
private static final ThreadLocal<StringBuilder> BUILDER = 
    ThreadLocal.withInitial(StringBuilder::new);

// ✅ GOOD - Documenting edge case
// Returns empty Optional when user is soft-deleted to maintain backward compatibility
public Optional<User> getUserById(Long id) {
    return userRepository.findByIdAndDeletedFalse(id);
}
```

### Javadoc Documentation

Document all public APIs:

```java
/**
 * Retrieves a user by their unique identifier.
 *
 * @param id the unique identifier of the user
 * @return the user if found
 * @throws UserNotFoundException if no user exists with the given id
 * @throws IllegalArgumentException if id is null or less than 1
 */
public User getUserById(Long id) {
    if (id == null || id < 1) {
        throw new IllegalArgumentException("Id must be a positive number");
    }
    return userRepository.findById(id)
        .orElseThrow(() -> new UserNotFoundException(id));
}
```

---

## Controllers & APIs

### REST Principles

```java
// ✅ GOOD - RESTful design
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    @GetMapping
    public List<UserDto> getAll() { }
    
    @GetMapping("/{id}")
    public UserDto getById(@PathVariable Long id) { }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserDto dto) { }
    
    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserDto dto) { }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { }
}
```

### Keep Controllers Thin

Controllers should only:
- Validate input (via @Valid)
- Delegate to services
- Map results to HTTP responses

```java
// ✅ GOOD - Thin controller
@PostMapping
public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserDto dto) {
    UserDto result = userService.createUser(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
}

// ❌ BAD - Business logic in controller
@PostMapping
public ResponseEntity<?> createUser(@RequestBody CreateUserDto dto) {
    if (dto.getEmail() == null || dto.getEmail().isEmpty()) {
        return ResponseEntity.badRequest().body("Email required");
    }
    if (!dto.getEmail().contains("@")) {
        return ResponseEntity.badRequest().body("Invalid email");
    }
    
    Optional<User> existing = userRepository.findByEmail(dto.getEmail());
    if (existing.isPresent()) {
        return ResponseEntity.badRequest().body("Email exists");
    }
    
    User user = new User();
    user.setEmail(dto.getEmail());
    user.setName(dto.getName());
    user.setCreatedAt(LocalDateTime.now());
    
    userRepository.save(user);
    
    emailService.sendWelcomeEmail(user.getEmail());
    
    return ResponseEntity.ok(user);
}
```

---

## Testing

### Testability Requirements

- All new code must be testable
- Avoid static dependencies
- Use interfaces for external dependencies
- Keep business logic separate from infrastructure

### Test Types

**Unit Tests:**
- Test business logic in isolation
- Mock all dependencies
- Fast execution

**Integration Tests:**
- Test data access and external services
- Use @SpringBootTest or @DataJpaTest
- Slower but verify actual integration

### Example

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void createOrder_validInput_returnsSuccess() {
        // Arrange
        CreateOrderDto dto = new CreateOrderDto(/* ... */);
        Order savedOrder = new Order(/* ... */);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        
        // Act
        OrderDto result = orderService.createOrder(dto);
        
        // Assert
        assertThat(result).isNotNull();
        verify(orderRepository, times(1)).save(any(Order.class));
    }
}
```

---

## Logging

### Best Practices

```java
// ✅ GOOD - Structured logging with context
log.info("Processing order {} for user {}", orderId, userId);

// ✅ GOOD - Log objects (with proper toString or use MDC)
log.debug("Order details: {}", order);

// ❌ BAD - String concatenation
log.info("Processing order " + orderId + " for user " + userId);

// ❌ BAD - No context
log.info("Processing order");
```

### When to Log

- **INFO**: Key business events (order created, user logged in)
- **WARN**: Recoverable errors (retry successful, fallback used)
- **ERROR**: Exceptions and failures
- **DEBUG**: Detailed diagnostic information (only in development)

### Do Not Add Debug Logging

Unless explicitly requested, do not add debug logging when making changes to files.

---

## Exception Handling

### Use Custom Exceptions

```java
// ✅ GOOD - Custom exception with context
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long orderId) {
        super("Order not found with id: " + orderId);
    }
}

// ✅ GOOD - Global exception handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("Validation failed", errors));
    }
}
```

---

*Last Updated: 2025-12-29*
