# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

Malmo is an AI-powered relationship counseling app backend built with Spring Boot. It provides attachment style diagnosis, AI conflict counseling, and daily couple questions. The app targets couples who want to understand each other better through attachment theory-based analysis.

## Build and Test Commands

```bash
# Build the project (generates QueryDSL Q-classes)
./gradlew build

# Run tests (uses H2 in-memory database and requires Redis on localhost:6379)
./gradlew test

# Run a specific test class
./gradlew test --tests "makeus.cmc.malmo.integration_test.MemberIntegrationTest"

# Run a specific test method
./gradlew test --tests "makeus.cmc.malmo.integration_test.MemberIntegrationTest.getMemberInfo*"

# Build JAR without tests
./gradlew bootJar

# Run application locally (requires environment variables)
./gradlew bootRun

# Clean build (removes QueryDSL generated classes)
./gradlew clean build
```

## Architecture

The project follows **Hexagonal Architecture (Ports and Adapters)** with clear separation of concerns:

```
makeus.cmc.malmo/
├── adaptor/                    # Infrastructure layer
│   ├── in/                     # Driving adapters (incoming)
│   │   ├── web/controller/     # REST controllers
│   │   ├── web/dto/            # Request/Response DTOs
│   │   ├── web/security/       # Spring Security configuration
│   │   ├── web/filter/         # HTTP filters
│   │   ├── aop/                # AOP annotations (@CheckValidMember, @CheckCoupleMember)
│   │   └── exception/          # Error handling
│   ├── out/                    # Driven adapters (outgoing)
│   │   ├── persistence/        # JPA repositories and entities
│   │   ├── oidc/               # Apple/Kakao OIDC authentication
│   │   ├── oauth/              # OAuth token management
│   │   ├── jwt/                # JWT token generation
│   │   ├── redis/              # Redis operations
│   │   └── amplitude/          # Analytics tracking
│   └── message/                # Message handling
├── application/                # Application layer
│   ├── port/in/                # Input ports (use cases)
│   ├── port/out/               # Output ports (interfaces for driven adapters)
│   ├── service/                # Use case implementations
│   ├── helper/                 # Reusable query/command helpers
│   └── exception/              # Application-level exceptions
├── domain/                     # Domain layer (pure business logic)
│   ├── model/                  # Domain entities and aggregates
│   ├── value/                  # Value objects (IDs, states, types)
│   └── service/                # Domain services
├── config/                     # Spring configuration
└── util/                       # Utility classes
```

### Key Architectural Patterns

1. **Use Case Pattern**: Each business operation is defined as a `*UseCase` interface in `port/in/` and implemented in `service/`. Controllers depend only on use case interfaces.

2. **Helper Pattern**: The `application/helper/` package contains reusable `*QueryHelper` and `*CommandHelper` classes that encapsulate common database operations, reducing duplication in services.

3. **Mapper Pattern**: `adaptor/out/persistence/mapper/` contains mappers for converting between domain models and JPA entities.

4. **AOP Validation**: Custom annotations `@CheckValidMember` and `@CheckCoupleMember` in `adaptor/in/aop/` validate member state before method execution.

## Domain Concepts

- **Member**: App user with attachment type diagnosis
- **Couple**: Two linked members who can share questions and chat
- **LoveType**: Attachment style based on ECR assessment (anxiety/avoidance rates)
- **ChatRoom**: AI counseling session with message history
- **CoupleQuestion**: Daily questions for couples to strengthen communication

## Testing

Tests use H2 in-memory database with test data from `src/main/resources/data-test.sql`. Integration tests extend patterns in `src/test/java/makeus/cmc/malmo/integration_test/` using:

- `@SpringBootTest` with `@Transactional` for rollback
- `MockMvc` for HTTP testing
- DTO factories in `dto_factory/` for test data creation

## Configuration

- Profiles: `test` (default), `dev`, `qa`, `prod`
- QueryDSL Q-classes generated to `build/generated/querydsl`
- Redis required for session/SSE management
- External APIs: OpenAI (chat), Kakao/Apple (auth), Amplitude (analytics)

## Deployment

Blue-green deployment via GitHub Actions. Commit messages trigger deployment:
- `release-green`: Deploys to port 8080
- `release-blue`: Deploys to port 8081
