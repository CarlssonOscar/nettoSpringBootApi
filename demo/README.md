# NettoApi - Swedish Salary & Tax Calculator

Backend API for calculating net salary based on Swedish tax rules.

## Quick Start

### Prerequisites
- Java 21
- Maven
- Supabase account (or PostgreSQL database)

### Database Setup

1. **Copy the template file:**
   ```bash
   cp src/main/resources/application-local.properties.template src/main/resources/application-local.properties
   ```

2. **Edit `application-local.properties`** with your Supabase credentials:
   ```properties
   spring.datasource.url=jdbc:postgresql://db.YOUR_PROJECT.supabase.co:5432/postgres
   spring.datasource.username=postgres
   spring.datasource.password=YOUR_PASSWORD
   ```

3. **Get credentials from Supabase:**
   - Go to [Supabase Dashboard](https://supabase.com/dashboard)
   - Select your project → Settings → Database
   - Copy the JDBC connection string

### Run the Application

```bash
./mvnw spring-boot:run
```

Or on Windows:
```cmd
mvnw.cmd spring-boot:run
```

## Project Structure

```
src/main/java/com/example/demo/
├── DemoApplication.java          # Entry point
├── controller/                   # REST endpoints
├── service/                      # Business logic
├── repository/                   # Data access
├── entity/                       # JPA entities
└── dto/                          # Request/Response objects
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/calculate` | Calculate net salary |
| GET | `/api/municipalities` | List municipalities |

## Configuration

| File | Purpose | Git Status |
|------|---------|------------|
| `application.properties` | Base config | ✅ Committed |
| `application-local.properties` | Secrets | ❌ Gitignored |
| `application-local.properties.template` | Template | ✅ Committed |

## Documentation

- [System Overview](docs/system-overview.md)
- [Architecture Principles](.github/architecture-principles.md)
- [Code Standards](.github/code-standards.md)
