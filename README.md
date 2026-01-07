# FluxPay Orchestration Engine

[![Build Status](https://github.com/serhatsoysal/fluxpay-orchestration-engine/workflows/CI%20Pipeline/badge.svg)](https://github.com/serhatsoysal/fluxpay-orchestration-engine/actions)
[![CodeQL](https://github.com/serhatsoysal/fluxpay-orchestration-engine/workflows/CodeQL%20Security%20Scan/badge.svg)](https://github.com/serhatsoysal/fluxpay-orchestration-engine/security/code-scanning)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=serhatsoysal_fluxpay-orchestration-engine&metric=alert_status)](https://sonarcloud.io/dashboard?id=serhatsoysal_fluxpay-orchestration-engine)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=serhatsoysal_fluxpay-orchestration-engine&metric=coverage)](https://sonarcloud.io/dashboard?id=serhatsoysal_fluxpay-orchestration-engine)
[![codecov](https://codecov.io/gh/serhatsoysal/fluxpay-orchestration-engine/branch/master/graph/badge.svg)](https://codecov.io/gh/serhatsoysal/fluxpay-orchestration-engine)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Enterprise-grade SaaS subscription management and billing orchestration platform built with Java 21 and Spring Boot 3.4.5.

## Features

- Multi-tenant architecture with tenant isolation
- JWT-based authentication and authorization
- Product catalog with flexible pricing models (flat rate, per-unit, tiered)
- Subscription lifecycle management (trial, active, canceled, paused)
- Invoice generation and payment processing
- PostgreSQL database with JPA/Hibernate
- RESTful API with comprehensive error handling
- Security scanning with CodeQL and Trivy
- Code quality monitoring with SonarCloud

## Technology Stack

- **Java 21** - Virtual threads for improved concurrency
- **Spring Boot 3.4.5** - Application framework
- **PostgreSQL 16+** - Primary database
- **Redis** - Caching and session management
- **Maven** - Build and dependency management
- **JUnit 5** - Testing framework
- **Docker** - Containerization

## Prerequisites

- Java 21 or higher
- Maven 3.8+
- PostgreSQL 16+
- Redis 7+
- Docker (optional)

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/serhatsoysal/fluxpay-orchestration-engine.git
cd fluxpay-orchestration-engine
```

### 2. Configure Environment

Copy the example environment file and configure your settings:

```bash
cp .env.example .env
```

**IMPORTANT: Generate a secure JWT secret before running the application**

#### Linux/Mac:
```bash
openssl rand -base64 64
```

#### Windows PowerShell:
```powershell
$bytes = New-Object byte[] 64
$rng = New-Object Security.Cryptography.RNGCryptoServiceProvider
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Update the `JWT_SECRET` value in your `.env` file with the generated key.

### 3. Set up Database

Create a PostgreSQL database:

```sql
CREATE DATABASE fluxpay;
```

Update database credentials in `.env` file.

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

```bash
cd fluxpay-api
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login

### Tenant Management
- `POST /api/tenants/register` - Register new tenant
- `GET /api/tenants/{id}` - Get tenant details
- `PUT /api/tenants/{id}` - Update tenant

### Products
- `POST /api/products` - Create product
- `GET /api/products` - List products
- `GET /api/products/{id}` - Get product details
- `POST /api/products/{id}/prices` - Add price to product

### Subscriptions
- `POST /api/subscriptions` - Create subscription
- `GET /api/subscriptions/{id}` - Get subscription details
- `POST /api/subscriptions/{id}/cancel` - Cancel subscription
- `POST /api/subscriptions/{id}/pause` - Pause subscription
- `POST /api/subscriptions/{id}/resume` - Resume subscription

### Invoices
- `GET /api/invoices/{id}` - Get invoice details
- `GET /api/invoices/customer/{customerId}` - List customer invoices
- `POST /api/invoices/{id}/finalize` - Finalize invoice
- `POST /api/invoices/{id}/void` - Void invoice

## Project Structure

```
fluxpay-orchestration-engine/
├── fluxpay-common/          # Shared utilities, DTOs, exceptions
├── fluxpay-security/        # JWT authentication, tenant context
├── fluxpay-tenant/          # Tenant and user management
├── fluxpay-product/         # Product catalog and pricing
├── fluxpay-subscription/    # Subscription lifecycle
├── fluxpay-billing/         # Invoicing and payments
└── fluxpay-api/            # REST controllers and main application
```

## Testing

Run all tests:

```bash
mvn test
```

Generate coverage report:

```bash
mvn clean test jacoco:report
```

## Code Quality

### Run Checkstyle

```bash
mvn checkstyle:check
```

### Run SonarCloud Analysis

First, add `SONAR_TOKEN` to GitHub Secrets, then manually trigger the SonarCloud workflow from the Actions tab.

## Security

- JWT tokens with HS512 algorithm
- BCrypt password hashing
- SQL injection protection with JPA
- CORS configuration
- Security scanning with CodeQL and Trivy

**Never commit your `.env` file or production secrets to version control.**

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

Serhat - [@serhatsoysal](https://github.com/serhatsoysal)

Project Link: [https://github.com/serhatsoysal/fluxpay-orchestration-engine](https://github.com/serhatsoysal/fluxpay-orchestration-engine)
