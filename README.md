# Virtual Office Backend

A microservices-based backend system for a Virtual Office application.

## 🚀 Services & Ports

| Service | Port | Description |
| :--- | :--- | :--- |
| [**Gateway API**](./gateway-api) | `8080` | Entry point for all client requests |
| [**User Service**](./user-service) | `8081` | User identity and authentication |
| [**Notification Service**](./notification-service) | `8082` | Email and push notifications |
| [**Calendar Service**](./calendar-service) | `8083` | Event scheduling and management |
| [**Chat Service**](./chat-service) | `8084` | Real-time messaging |
| [**Tasks Service**](./tasks-service) | `8085` | Task tracking and assignment |
| [**Room Service**](./room-service) | `8086` | Virtual meeting rooms |
| [**Workspace Service**](./workspace-service) | `8087` | Organization and workspace settings |
| [**Desk Service**](./desk-service) | `8088` | Desk management and assignment |
| [**Shared Library**](./shared-library) | N/A | Common utilities and DTOs (Library) |

## 🛠️ Build & Run

### Prerequisites
- Java 21
- Maven

### Build All Services
```bash
./mvnw clean install
```
