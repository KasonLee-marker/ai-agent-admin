# Model Service Constraints

## Architecture
- Layered architecture: domain -> service -> api
- Use Lombok for boilerplate reduction
- Use MapStruct for DTO mapping

## Security
- API Keys must be encrypted using Jasypt
- Never log decrypted API keys
- Use environment variables for sensitive config

## Database
- H2 for development
- PostgreSQL for production
- JPA for ORM

## Testing
- Unit test coverage > 70%
- Mock external dependencies
- Use @WebMvcTest for controller tests

## No-Go
- No Nacos
- No Redis
- No distributed caching
