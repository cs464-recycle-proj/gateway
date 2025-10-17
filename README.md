# Gateway Service

**Spring Cloud Gateway** that routes requests to downstream microservices, validates JWTs, and enriches requests with user information.

---

## Ports

| Service        | Port |
| -------------- | ---- |
| Gateway        | 8080 |
| Auth Service   | 8081 |
| Future Service | TBD  |

---

## Description

The Gateway service acts as the entry point for API requests. Its main responsibilities:

* Decodes cookies and extracts JWT tokens.
* Validates JWTs using the configured secret.
* Adds the following headers to requests for downstream services:

  * `X-User-ID` — the user identifier from the JWT.
  * `X-User-Role` — the user role from the JWT.
* Routes requests to the appropriate service (currently Auth Service).

This ensures that downstream services do not need to handle authentication or extract user info — it’s centralized in the gateway.

---

## Getting Started

### Prerequisites

* Docker
* Docker Compose
* `.env` file with the relevant variables (refer to notion)

---

### Running the Gateway with Docker Compose

To build and start the services (Gateway + Auth Service):

```bash
docker-compose up --build
```

Or, if images are already built:

```bash
docker-compose up
```

---

### Shutting Down

```bash
docker-compose down
```

This stops and removes the Gateway and Auth Service containers.

---

### Notes

* Future services can be added to the gateway routes in `GatewayConfig.java`.
