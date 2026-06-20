# NotifyHub

A real-time notification system built with **Spring Boot**, **Redis Pub/Sub**, and **WebSocket (STOMP)**.

Users can send notifications to other users through a REST API. Notifications are persisted in PostgreSQL, published to Redis Pub/Sub for horizontal scaling, and delivered to recipients in real-time via WebSocket.

---

## Architecture

```
┌─────────────────┐         ┌──────────────────────────────────────────────┐
│   Browser (JS)  │         │              Spring Boot App                 │
│                 │  REST   │                                              │
│  ┌───────────┐  ├────────►│  ┌────────────┐    ┌──────────────────┐     │
│  │ fetch()   │  │         │  │ REST       │    │ Notification     │     │
│  └───────────┘  │         │  │ Controller │───►│ Service          │     │
│                 │         │  └────────────┘    └──────┬───────────┘     │
│  ┌───────────┐  │  WS     │                          │                  │
│  │ SockJS +  │◄─┼─────────┤  ┌────────────┐    ┌────▼───────────┐     │
│  │ STOMP     │  │         │  │ Redis      │◄───│ Redis          │     │
│  └───────────┘  │         │  │ Subscriber │    │ Publisher      │     │
│                 │         │  └─────┬──────┘    └────────────────┘     │
└─────────────────┘         │        │                    │              │
                            │        ▼                    │              │
                            │  ┌────────────┐     ┌──────▼──────┐      │
                            │  │ STOMP      │     │ JPA         │      │
                            │  │ Messaging  │     │ Repository  │      │
                            │  └────────────┘     └──────┬──────┘      │
                            └────────────────────────────┼──────────────┘
                                                         │
                            ┌────────────┐        ┌──────▼──────┐
                            │   Redis    │        │ PostgreSQL  │
                            │  Pub/Sub   │        │             │
                            └────────────┘        └─────────────┘
```

### Flow

1. Client sends `POST /api/notifications` with a JSON payload
2. `NotificationService` persists the notification in PostgreSQL
3. The notification is published to a Redis Pub/Sub channel (`notifications`)
4. All application instances subscribed to the channel receive the message
5. `RedisMessageSubscriber` routes it to the recipient's WebSocket topic (`/topic/notifications/{userId}`)
6. The browser receives the notification in real-time via its STOMP subscription

**Why Redis Pub/Sub?** It decouples the notification sender from the WebSocket delivery layer. In a multi-instance deployment (e.g., behind a load balancer), Redis ensures every instance receives the message and can deliver it to locally connected WebSocket clients.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend Framework | Spring Boot 4.1 |
| Real-time Communication | WebSocket (STOMP) + SockJS |
| Message Broker | Redis 7 (Pub/Sub) |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| Frontend | Vanilla HTML/CSS/JavaScript |
| Containerization | Docker Compose |

---

## Project Structure

```
src/main/java/com/notifyhub/
├── NotifyHubApplication.java       # Entry point
├── config/
│   ├── RedisConfig.java            # Redis template, listener container, topic
│   ├── WebSocketConfig.java        # STOMP broker, /ws endpoint
│   └── SecurityConfig.java         # Permit-all for demo
├── model/
│   ├── Notification.java           # JPA entity
│   └── NotificationType.java       # INFO, SUCCESS, WARNING, ERROR
├── dto/
│   ├── NotificationRequest.java    # Validated inbound payload
│   ├── NotificationResponse.java   # Outbound payload with timeAgo
│   └── ApiResponse.java            # Standard REST wrapper
├── repository/
│   └── NotificationRepository.java # JPA queries
├── service/
│   ├── NotificationService.java    # Business logic
│   └── RedisMessagePublisher.java  # Publishes to Redis channel
├── listener/
│   └── RedisMessageSubscriber.java # Redis → WebSocket bridge
├── controller/
│   └── NotificationController.java # REST endpoints
└── exception/
    ├── ResourceNotFoundException.java
    └── GlobalExceptionHandler.java
```

---

## Getting Started

### Prerequisites

- **Java 17+**
- **Docker** and **Docker Compose**
- **Maven** (or use the included `mvnw` wrapper)

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts PostgreSQL (port 5432) and Redis (port 6379).

### 2. Run the Application

```bash
./mvnw spring-boot:run
```

### 3. Open the Dashboard

Navigate to [http://localhost:8080](http://localhost:8080) in your browser.

---

## API Reference

### Send Notification

```bash
POST /api/notifications
Content-Type: application/json

{
  "senderId": "alice",
  "recipientId": "bob",
  "title": "Deployment Complete",
  "message": "The v2.1 release has been deployed to production.",
  "type": "SUCCESS"
}
```

**Response** (201 Created):
```json
{
  "success": true,
  "message": "Notification sent",
  "data": {
    "id": "a1b2c3d4-...",
    "senderId": "alice",
    "recipientId": "bob",
    "title": "Deployment Complete",
    "message": "The v2.1 release has been deployed to production.",
    "type": "SUCCESS",
    "read": false,
    "createdAt": "2025-01-15T10:30:00",
    "timeAgo": "just now"
  },
  "timestamp": "2025-01-15T10:30:00"
}
```

### Get User Notifications

```bash
GET /api/notifications/{userId}
```

### Get Unread Notifications

```bash
GET /api/notifications/{userId}/unread
```

### Get Unread Count

```bash
GET /api/notifications/{userId}/unread/count
```

### Mark as Read

```bash
PATCH /api/notifications/{notificationId}/read
```

### Mark All as Read

```bash
PATCH /api/notifications/{userId}/read-all
```

---

## WebSocket

The app uses **STOMP over SockJS** for real-time notification delivery.

### Connection

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function () {
    stompClient.subscribe('/topic/notifications/bob', function (message) {
        const notification = JSON.parse(message.body);
        console.log('New notification:', notification);
    });
});
```

Each user subscribes to `/topic/notifications/{userId}` and receives notifications published to their channel in real-time.

---

## Frontend

The dashboard is a single-page app served as a static resource:

- **Left panel**: Send notifications to any user with type selection
- **Right panel**: Real-time notification feed with filtering and mark-as-read
- **User switcher**: Simulate different users without authentication
- **Toast popups**: Visual alerts for incoming notifications
- **Dark theme**: Glassmorphism design with smooth animations

---

## Configuration

Key settings in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/notifyhub
    username: notifyhub
    password: notifyhub
  data:
    redis:
      host: localhost
      port: 6379
  jpa:
    hibernate:
      ddl-auto: update
```

---

## License

This project is for portfolio and educational purposes.
#   N o t i f y H u b  
 