# Model Service

AI Model Management Service with Spring AI integration.

## Features

- Multi-provider support (OpenAI, DashScope, DeepSeek, Ollama)
- API Key encryption using Jasypt (AES-256)
- Dynamic model switching at runtime
- Health check for model connectivity
- RESTful API with OpenAPI documentation

## API Endpoints

### Model CRUD
- `GET /api/v1/models` - List all models with filters
- `GET /api/v1/models/{id}` - Get model details
- `POST /api/v1/models` - Create new model
- `PUT /api/v1/models/{id}` - Update model
- `DELETE /api/v1/models/{id}` - Delete model

### Model Operations
- `POST /api/v1/models/{id}/test` - Health check
- `POST /api/v1/models/{id}/default` - Set as default
- `GET /api/v1/models/default` - Get default model
- `GET /api/v1/models/active` - Get active model
- `POST /api/v1/models/switch` - Switch model at runtime

### Providers
- `GET /api/v1/models/providers` - List all providers
- `GET /api/v1/models/providers/{provider}/builtin` - Get built-in models

## Configuration

### Environment Variables
```bash
# Required for production
export JASYPT_PASSWORD=your-secure-password

# Database (production)
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=modeldb
export DB_USER=modeluser
export DB_PASSWORD=your-db-password
```

### Application Properties
```yaml
spring:
  profiles:
    active: dev  # or prod
```

## Running Tests

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw jacoco:report

# Run specific test
./mvnw test -Dtest=ModelControllerTest
```

## Building

```bash
# Build JAR
./mvnw clean package

# Build Docker image
./mvnw spring-boot:build-image
```

## API Documentation

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

Access OpenAPI JSON at: `http://localhost:8080/api-docs`
