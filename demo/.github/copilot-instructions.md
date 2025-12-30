# GitHub Copilot Instructions

> **Streamlined version** - detailed standards are in separate reference documents  
> Last Updated: 2025-12-29

---

## 📋 Quick Navigation

**Primary Documentation:**
- 📖 [`docs/system-overview.md`](../docs/system-overview.md) - System architecture and components
- 🏗️ [Architecture & Design Principles](architecture-principles.md) - SOLID, Clean Architecture, patterns
- 📐 [Code Quality Standards](code-standards.md) - Naming, structure, best practices
- 🐛 [Debugging Guide](debugging-guide.md) - Systematic troubleshooting approach

---

## ⚠️ CRITICAL RULES - Always Apply

### 1. Always Re-read Files
- **Never assume** that a previously shown file is still up to date
- **When modifying a file**: re-read it first, then show the new version with changes
- **Don't rely on memory** or earlier context for file contents

### 2. Think Before Claiming Correctness
- **If told you are wrong**: re-evaluate your answer critically
- **Check** against code, documentation, and established patterns
- **Respond** with facts and reasoning, not assumptions
- **Acknowledge mistakes** when wrong; explain reasoning when correct

### 3. Compare Solutions
- **Analyze** your own answers vs. previous solutions
- **If the old solution is better**: explicitly suggest reverting to it
- **Don't assume** newer is always better

### 4. Dependency Injection - NO EXCEPTIONS
**NEVER use `new` for services, repositories, or external clients.**

✅ **Allowed**: DTOs, value objects, domain entities, simple data structures  
❌ **FORBIDDEN**: Services, repositories, API clients, EntityManager

```java
// ❌ WRONG
OrderService service = new OrderService();

// ✅ CORRECT
@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
}
```

---

## 🎯 Core Principles (Quick Reference)

### Architecture
- **Clean Architecture**: Dependencies point inward (see architecture-principles.md)
- **Layered Architecture**: Controller → Service → Repository → Database
- **Full details**: See [architecture-principles.md](architecture-principles.md)

### Code Quality
- **Single Responsibility**: One reason to change per class
- **Intention-Revealing Names**: Clear over clever
- **Short Methods**: Target 5-15 lines, max 30 lines
- **Full standards**: See [code-standards.md](code-standards.md)

### Debugging
- **Top-Down Approach**: Configuration → Dependencies → Services → Implementation
- **Never skip levels**: Root cause often in configuration, not implementation
- **Full guide**: See [debugging-guide.md](debugging-guide.md)

---

## 🤖 AI Behavior Rules

### General Behavior
- **Explain why** for architectural choices, not only how
- **Challenge violations** of SOLID principles politely
- **Ask for clarification** instead of guessing when requirements are ambiguous
- **Stay focused**: Implement only what has been requested

### Working with Files
- **For large changes**: Describe what changed and why
- **For refactoring**: Explain before/after and reasoning
- **For big changes**: Explain differences clearly

### What NOT to Do (Unless Asked)
- ❌ Extra refactorings
- ❌ Additional logging
- ❌ "Nice to have" improvements
- ❌ Unrelated fixes

---

## 📚 Documentation Standards

### System Documentation Reference
**Primary Documentation**: [`docs/system-overview.md`](../docs/system-overview.md)  
*(Single source of truth for system architecture, components, data flows, and technical stack)*

### When to Update Documentation
| **Priority** | **Trigger** |
|--------------|-------------|
| **MUST** | New components, architecture changes, external integrations, data flows, tech stack changes |
| **SHOULD** | Database schema changes, significant refactoring, deployment changes |

### Documentation Awareness
- **For architectural questions**: Use `docs/system-overview.md` as primary reference
- **Propose updates**: If your suggestions change or extend the architecture
- **Follow patterns**: Established in the documentation unless strong reason to deviate

---

## 🔧 Code Standards (Quick Reference)

### Naming Conventions
```java
// Java / Spring Boot
UserService                    // Classes: PascalCase
getUserById()                  // Methods: camelCase
userRepository                 // Fields: camelCase
MAX_RETRY_COUNT               // Constants: UPPER_SNAKE_CASE
OrderStatus                    // Enums: PascalCase
PENDING                        // Enum values: UPPER_SNAKE_CASE
```

### Essential Rules
- **Comments**: English only, 1-2 lines, explain "why" not "what"
- **Controllers**: Thin - validation + delegation only, RESTful naming
- **Testing**: All new code must be testable
- **Logging**: Use SLF4J logger, include context (IDs, parameters)

**Full standards**: See [code-standards.md](code-standards.md)

---

## 🐛 Debugging (Quick Reference)

### Before Diving Into Code - Check:
1. ✅ Are beans registered correctly in Spring context?
2. ✅ Is configuration correct for the current profile?
3. ✅ Are filters and interceptors ordered correctly?
4. ✅ Are bean scopes (Singleton/Prototype/Request) appropriate?
5. ✅ Are there circular dependencies causing issues?

### Hierarchy (Top → Down)
```
1. Configuration Level (Application class, @Configuration, application.properties)
2. Dependencies Level (Bean scopes, Spring context, @Autowired)
3. Services Level (Business logic, service boundaries)
4. Implementation Details (Individual methods, algorithms)
```

**Full guide**: See [debugging-guide.md](debugging-guide.md)

---

## 📝 Change Management

### For Every Code Change
- Implement only what was explicitly requested
- For large changes: explain what, why, and potential side effects
- For architectural changes: reference relevant principles and propose doc updates

### Code Review Mindset
- Challenge violations of layered architecture and SOLID
- If old solution is better: say so clearly and suggest reverting
- Avoid "rewrite everything" unless absolutely necessary

---

## Version Information

**Copilot Instructions Version**: `1.0`  
**Last Updated**: `2025-12-29`  
**Technology Stack**: Java 21 + Spring Boot 4.x

---

*This instruction file must be followed by both developers and AI assistants. It is a central tool for maintaining architecture, code quality, and living documentation in this project.*
