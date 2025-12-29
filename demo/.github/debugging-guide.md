# Systematic Debugging Guide

> **Reference document** - linked from main copilot-instructions.md  
> Use this when troubleshooting unexpected behavior or bugs

---

## Core Philosophy

> **Always start from the highest architectural level and move downward.**  
> Root causes are often in configuration or shared dependencies, not low-level implementation.

**The Debugging Mantra:**
```
Configuration → Dependencies → Services → Implementation
```

**Never skip levels.** A "quick fix" at the implementation level often masks a configuration-level root cause.

---

## Quick Reference Checklist

Before diving into code, ask yourself:

1. ✅ Are beans registered correctly in the Spring context?
2. ✅ Is configuration correct for the current profile (dev/prod)?
3. ✅ Are filters and interceptors ordered correctly?
4. ✅ Are bean scopes (Singleton/Prototype/Request) appropriate?
5. ✅ Are there circular dependencies causing issues?

**Only after checking these** should you move to specific implementations.

---

## The Debugging Hierarchy

### Level 1: Configuration (Check FIRST)

**What to inspect:**
- Entry point (`DemoApplication.java`, `@SpringBootApplication`)
- Configuration classes (`@Configuration`, `@Bean`)
- Configuration files (`application.properties`, `application.yml`)
- Profile-specific configs (`application-dev.properties`, etc.)
- Environment variables

**Questions to ask:**
- Are the correct beans registered?
- Is the correct profile active?
- Are properties loaded correctly?
- Are connection strings valid for this environment?

**When to investigate this level:**
- Multiple components fail with similar symptoms
- Behavior differs between environments (dev vs. prod)
- `NoSuchBeanDefinitionException` or `BeanCreationException`
- "Could not autowire" errors

**Common issues:**
- Missing `@Component`, `@Service`, or `@Repository` annotation
- Wrong `application-{profile}.properties` loaded
- Missing `@EnableJpaRepositories` or `@EntityScan`
- Property placeholder `${property}` not resolved

---

### Level 2: Dependencies

**What to inspect:**
- Bean registrations in Spring context
- Bean scopes (Singleton / Prototype / Request)
- Constructor dependencies
- Third-party clients (RestTemplate, WebClient, SDK initialization)

**Questions to ask:**
- Is an incorrect scope causing stale state?
- Are there circular dependencies?
- Are mocks/fakes configured correctly in tests?
- Are third-party services initialized with correct credentials?

**When to investigate this level:**
- Beans have stale or unexpected state across requests
- Dependencies are null despite being registered
- Integration with external systems fails inconsistently
- Memory leaks or unexpected object retention

**Common issues:**
- Singleton bean with mutable state
- Request-scoped bean injected into Singleton
- Circular dependency between beans
- Missing `@Lazy` for optional dependencies

---

### Level 3: Services

**What to inspect:**
- Business services (@Service)
- Application services
- External API clients
- Transaction boundaries

**Questions to ask:**
- Does data flow correctly through the service?
- Are errors handled consistently?
- Do services enforce domain rules as documented?
- Are service boundaries clear and respected?
- Is @Transactional used correctly?

**When to investigate this level:**
- Business logic produces incorrect results
- Service method behavior is inconsistent
- Cross-service communication fails
- Transaction boundaries are unclear

**Common issues:**
- Service not properly handling edge cases
- Missing validation before calling repositories
- Transaction not committed/rolled back correctly
- Service mixing multiple responsibilities
- @Transactional on private method (doesn't work)

---

### Level 4: Implementation Details (Check LAST)

**What to inspect:**
- Individual methods
- Algorithms
- Repository implementations
- JPA queries

**Questions to ask:**
- Are edge cases and failure modes handled?
- Does the implementation match the intended logic?
- Are there off-by-one errors or incorrect calculations?
- Is data transformation correct?

**When to investigate this level:**
- Higher levels are confirmed correct
- Issue is isolated to a specific method or class
- Bug is reproducible with specific input values
- Logic error in algorithm

**Common issues:**
- Off-by-one errors in loops
- Incorrect null handling (use Optional properly)
- Wrong comparison operators
- Improper exception handling
- N+1 query problems in JPA

---

## Follow the Data Flow

Always trace the entire path:

```
HTTP Request → Controller → Service → Repository → Database → Response
```

**Example for a failing API endpoint:**

1. **Request**: Is the request reaching the controller? (logging, debugger)
2. **Configuration**: Is the route mapped? Security filter blocking?
3. **Bean Registration**: Is the service injected correctly?
4. **Business Logic**: Does the service method execute? What values?
5. **Data Access**: Does the repository query execute? Results correct?
6. **Response**: Is the response serialized correctly?

**Do not skip levels** - the root cause is often earlier in the chain than you think.

---

## Common Issue Patterns

| **Symptom** | **Likely Level** | **What to Check** |
|-------------|------------------|-------------------|
| Multiple components fail similarly | Configuration | Bean registration, component scan, profiles |
| Database queries fail unexpectedly | Configuration → Dependencies | DataSource config, JPA settings, connection pool |
| Auth/authz not working | Configuration | Security filter chain, @EnableMethodSecurity |
| Beans return null on inject | Dependencies | @Component missing, wrong package, circular deps |
| Inconsistent behavior across requests | Dependencies | Singleton scope with mutable state |
| Business logic gives wrong results | Services → Implementation | Service logic, domain rules, transactions |
| Specific method fails with certain input | Implementation | Algorithm, edge cases, data validation |

---

## Spring Boot Specific Debugging Paths

### Database/JPA Issues
```
1. DataSource configuration (application.properties)
2. Entity mappings (@Entity, @Table, relationships)
3. Repository interfaces (extends JpaRepository?)
4. @EnableJpaRepositories and @EntityScan
5. Query logic in services
```

### Dependency Injection Issues
```
1. @Component/@Service/@Repository present?
2. Package in component scan path?
3. Bean scopes correct?
4. Circular dependencies? (use @Lazy)
5. Constructor parameters available?
```

### Security Issues
```
1. SecurityFilterChain configuration
2. Filter order (@Order annotation)
3. @PreAuthorize / @Secured annotations
4. Principal/Authentication available?
5. CORS configuration
```

### Transaction Issues
```
1. @Transactional on public method?
2. Called from same class? (proxy issue)
3. RuntimeException for rollback?
4. propagation and isolation correct?
5. Connection pool exhaustion?
```

---

## Documentation Template

For each major bug, document:

```markdown
## Bug: [Brief Description]

### Symptoms
- What was observed?
- When did it occur?

### Investigation Path
1. **Configuration Level**: What was checked?
2. **Dependencies Level**: What was inspected?
3. **Services Level**: What was analyzed?
4. **Implementation Level**: What was examined?

### Root Cause
- What was the actual problem?
- At which level was it found?

### Fix Applied
- What was changed?
- Why does this fix the root cause?

### Lessons Learned
- What will prevent this in the future?
- What could have been caught earlier?
```

This creates institutional knowledge and prevents repeating the same debugging journey.

---

## Warning: Avoid "Quick Fixes"

❌ **Bad approach:**
```java
// Adding null checks everywhere because service is sometimes null
if (myService == null) {
    myService = new MyService(); // Creating instance manually - WRONG
}
```

✅ **Good approach:**
```
1. Investigate why the service is null
2. Find the bean registration issue
3. Fix the root cause (missing annotation or circular dependency)
4. Remove workarounds
```

**Band-aid solutions create technical debt.** Always address root causes.

---

## Red Flags - Stop and Check Configuration

🚩 **Immediate red flags that point to configuration issues:**
- Multiple unrelated features failing simultaneously
- Works locally but fails in other environments
- Intermittent failures with no clear pattern
- Beans that are "sometimes null"
- Different behavior after application restart
- Errors mentioning "No qualifying bean"

**Don't waste time debugging implementation when configuration is wrong.**

---

## Tools and Techniques

### Logging Strategy
```java
// Log at each level to trace the data flow
log.info("Processing order {}", orderId);
log.debug("Order details: {}", order);
log.warn("Validation failed for order {}: {}", orderId, errors);
```

### Actuator Endpoints
```properties
# Enable useful actuator endpoints
management.endpoints.web.exposure.include=health,info,beans,env,mappings
```

- `/actuator/beans` - See all registered beans
- `/actuator/env` - Check environment and properties
- `/actuator/mappings` - See all request mappings

### Debugger Breakpoints
- Set breakpoints at layer boundaries
- Check values entering and leaving each service
- Verify dependencies are not null

### Configuration Inspection
```java
// Log configuration on startup to verify
@PostConstruct
public void logConfig() {
    log.info("Database URL: {}", 
        dataSourceUrl.substring(0, Math.min(30, dataSourceUrl.length())) + "...");
}
```

### Bean Dependency Visualization
- Use Spring Boot Actuator `/actuator/beans`
- Check for circular dependencies in startup logs
- Verify bean scopes

---

*Last Updated: 2025-12-29*
