# Getting Started with NettoApi

A Swedish tax calculation API built with Spring Boot. This guide covers how to set up and run the project locally.

## Prerequisites

- **Java 21** - Required to compile and run the application
- **Maven** - Included via wrapper (`mvnw.cmd`), no separate installation needed

### Verify Java Version

```bash
java -version
```

You should see output indicating Java 21 (e.g., `openjdk version "21.0.x"`).

## Project Setup

### 1. Place Configuration File

You should have received `application-local.properties` via email. Place this file in:

```
demo/src/main/resources/application-local.properties
```

This file contains the Supabase database credentials and is required for the application to connect to the database.

### 2. Run the Application

Open a terminal in the `demo/` folder and run:

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

The application will start on **http://localhost:8080**.

### 3. Verify the Application is Running

Visit the health endpoint to confirm everything is working:

```
http://localhost:8080/actuator/health
```

You should see:

```json
{
  "status": "UP"
}
```

## API Documentation

Once the application is running, you can explore all available endpoints via Swagger UI:

```
http://localhost:8080/api/v1/swagger-ui.html
```

## Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/tax/calculate` | Calculate net salary by municipality UUID |
| GET | `/api/v1/tax/calculate-by-code` | Calculate net salary by municipality code |
| GET | `/api/v1/municipalities` | List all Swedish municipalities |
| GET | `/api/v1/municipalities/regions` | List all Swedish regions |

### Example: Calculate Net Salary

```
GET http://localhost:8080/api/v1/tax/calculate-by-code?municipalityCode=0180&grossSalary=50000&year=2026
```

## Building a JAR

To build an executable JAR file:

```bash
mvnw.cmd clean package -DskipTests
```

The JAR will be created at `target/demo-0.0.1-SNAPSHOT.jar`.

Run it with:

```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## Troubleshooting

### Database Connection Issues

If you see database connection errors, verify that:
1. `application-local.properties` is in the correct location
2. The Supabase database is accessible (not paused due to inactivity)

### Port Already in Use

If port 8080 is occupied, you can specify a different port:

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```
