<div align="center">

# 🔔 NotifyHub

**A real-time notification system built for scale.**

Spring Boot · Redis Pub/Sub · WebSocket · PostgreSQL

[Getting Started](#-getting-started) · [API Docs](#-api-reference) · [Architecture](#-architecture) · [Design Decisions](#-design-decisions)

</div>

---

## 📋 Overview

NotifyHub is a real-time notification delivery system that pushes messages to users the instant they're created — no polling, no delays. It combines **Redis Pub/Sub** for scalable message fan-out with **WebSocket (STOMP)** for persistent client connections, backed by **PostgreSQL** for durable storage.

I built this to explore a common distributed systems pattern: decoupling producers from consumers using a message broker, while maintaining both real-time delivery *and* persistence. It's the same architectural pattern used by Slack, Discord, and GitHub Notifications — scaled down to be understandable but designed with production concerns in mind.

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT (Browser)                            │
│                                                                     │
│   ┌─────────────┐                          ┌──────────────────┐    │
│   │  REST calls  │  HTTP                   │  SockJS + STOMP  │    │
│   │  (fetch API) ├──────────┐    ┌─────────┤  (live updates)  │    │
│   └─────────────┘          │    │         └──────────────────┘    │
└────────────────────────────┼────┼─────────────────────────────────┘
                             │    │
                             ▼    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      SPRING BOOT APPLICATION                        │
│                                                                     │
│   ┌────────────────┐    ┌─────────────────┐    ┌────────────────┐  │
│   │  Notification   │    │  Notification    │    │  STOMP         │  │
│   │  Controller     │───▶│  Service         │    │  Messaging     │  │
│   │  (REST API)     │    │  (orchestrator)  │    │  Template      │  │
│   └────────────────┘    └────────┬─────────┘    └───────▲────────┘  │
│                                  │                       │           │
│                          ┌───────┴───────┐               │           │
│                          │               │               │           │
│                          ▼               ▼               │           │
│                 ┌──────────────┐  ┌─────────────┐        │           │
│                 │ JPA          │  │ Redis       │        │           │
│                 │ Repository   │  │ Publisher   │        │           │
│                 └──────┬───────┘  └──────┬──────┘        │           │
│                        │                 │               │           │
│                        │                 │   ┌───────────┴────────┐  │
│                        │                 │   │ Redis Subscriber   │  │
│                        │                 │   │ (MessageListener)  │  │
│                        │                 │   └───────────▲────────┘  │
└────────────────────────┼─────────────────┼───────────────┼──────────┘
                         │                 │               │
                         ▼                 ▼               │
                  ┌─────────────┐   ┌─────────────┐        │
                  │ PostgreSQL  │   │    Redis     │────────┘
                  │ (durable    │   │  Pub/Sub     │
                  │  storage)   │   │ (fan-out)    │
                  └─────────────┘   └─────────────┘
```

### The Flow

1. A user sends a notification via `POST /api/notifications`
2. **NotificationService** persists it in PostgreSQL (durability) and publishes it to a Redis Pub/Sub channel (distribution)
3. **RedisMessageSubscriber** receives the published message on every application instance
4. The subscriber routes the notification to the recipient's WebSocket topic via STOMP: `/topic/notifications/{userId}`
5. The recipient's browser receives the notification instantly through its active WebSocket connection

**Why two data stores?** PostgreSQL handles durability — notifications survive restarts, users can fetch history, and we get full query capabilities. Redis handles distribution — when we scale to multiple app instances behind a load balancer, Redis ensures *every* instance receives the message, not just the one that processed the API call.

---

## ✨ Features

- **Real-time delivery** — Notifications appear in the browser within milliseconds via WebSocket (STOMP over SockJS)
- **Scalable distribution** — Redis Pub/Sub ensures message delivery across multiple application instances
- **Persistent storage** — All notifications are durably stored in PostgreSQL with full CRUD support
- **REST API** — Clean, versioned API with structured JSON responses and input validation
- **Unread tracking** — Server-side read/unread state with badge counts and bulk mark-as-read
- **Multi-user simulation** — Switch between users in the UI to test real-time delivery across recipients
- **Responsive frontend** — Dark-themed dashboard with glassmorphism design, toast notifications, and smooth animations
- **Structured error handling** — Global exception handler with field-level validation errors
- **Dockerized infrastructure** — One-command setup with Docker Compose for Redis and PostgreSQL

---

## 🛠 Tech Stack

| Layer | Technology | Why |
|---|---|---|
| **Framework** | Spring Boot 4.1 | Production-grade foundation with auto-configuration, embedded server, and mature ecosystem |
| **Real-time** | WebSocket (STOMP + SockJS) | Full-duplex communication with automatic fallback for browsers without native WebSocket support |
| **Message Broker** | Redis 7 (Pub/Sub) | Sub-millisecond message fan-out that scales horizontally without complex setup |
| **Database** | PostgreSQL 16 | ACID-compliant storage for notification history with rich querying via JPA |
| **ORM** | Spring Data JPA + Hibernate | Declarative queries, type-safe repository pattern, automatic DDL management |
| **Validation** | Jakarta Bean Validation | Annotation-based input validation with structured error responses |
| **Frontend** | Vanilla HTML/CSS/JS | Zero build step, no framework lock-in, demonstrates core web fundamentals |
| **Infrastructure** | Docker Compose | Reproducible dev environment — one command for all dependencies |

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+** — [Download](https://adoptium.net/)
- **Docker** and **Docker Compose** — [Install](https://docs.docker.com/get-docker/)
- **Maven** (optional — the project includes a `mvnw` wrapper)

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/NotifyHub.git
cd NotifyHub
```

### 2. Start the infrastructure

```bash
docker-compose up -d
```

This starts two containers:

| Container | Service | Port |
|---|---|---|
| `notifyhub-postgres` | PostgreSQL 16 | 5432 |
| `notifyhub-redis` | Redis 7 | 6379 |

Verify they're healthy:
```bash
docker ps
```

### 3. Run the application

**Linux/macOS:**
```bash
./mvnw spring-boot:run
```

**Windows:**
```powershell
.\mvnw.cmd spring-boot:run
```

### 4. Open the dashboard

Navigate to **[http://localhost:8080](http://localhost:8080)** in your browser.

> **Note:** If Docker is running in WSL while the app runs on Windows, `localhost` should work out of the box with WSL2's port forwarding. If not, replace `localhost` in `application.yml` with your WSL IP (run `hostname -I` inside WSL).

---

## 📡 API Reference

All endpoints return a consistent response wrapper:

```json
{
  "success": true,
  "message": "Success",
  "data": { ... },
  "timestamp": "2025-01-15T10:30:00"
}
```

### Send Notification

```
POST /api/notifications
```

```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "senderId": "alice",
    "recipientId": "bob",
    "title": "Deployment Complete",
    "message": "The v2.1 release has been deployed to production.",
    "type": "SUCCESS"
  }'
```

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `senderId` | string | No | `"system"` | Who sent the notification |
| `recipientId` | string | **Yes** | — | Target user |
| `title` | string | **Yes** | — | Short headline (max 255 chars) |
| `message` | string | **Yes** | — | Body text (max 1000 chars) |
| `type` | enum | No | `INFO` | `INFO`, `SUCCESS`, `WARNING`, `ERROR` |

### Get All Notifications

```
GET /api/notifications/{userId}
```

```bash
curl -H "Accept: application/json" http://localhost:8080/api/notifications/bob
```

### Get Unread Notifications

```
GET /api/notifications/{userId}/unread
```

### Get Unread Count

```
GET /api/notifications/{userId}/unread/count
```

### Mark as Read

```
PATCH /api/notifications/{notificationId}/read
```

### Mark All as Read

```
PATCH /api/notifications/{userId}/read-all
```

---

## 🔌 WebSocket

The application uses **STOMP over SockJS** for real-time communication.

### Connection

| Detail | Value |
|---|---|
| Handshake endpoint | `/ws` (SockJS) |
| Subscribe topic | `/topic/notifications/{userId}` |
| Protocol | STOMP 1.1 |

### Client Example

```javascript
const socket = new SockJS('/ws');
const client = Stomp.over(socket);

client.connect({}, function () {
    client.subscribe('/topic/notifications/bob', function (frame) {
        const notification = JSON.parse(frame.body);
        console.log('New notification:', notification.title);
    });
});
```

Each user subscribes to their own topic. When a notification is sent to them through the REST API, it's pushed to their topic in real-time — no polling required.

---

## 🎯 Usage Example

### Step 1: Open two browser tabs

Both pointing to `http://localhost:8080`.

### Step 2: Set up users

- **Tab 1:** Select **"Bob"** from the "Viewing as" dropdown (this is the *recipient*)
- **Tab 2:** Select **"Alice"** from the dropdown (this is the *sender*)

### Step 3: Send a notification

In Tab 2 (Alice), fill out the form:
- **Recipient:** Bob
- **Title:** Server Alert
- **Message:** CPU usage exceeded 90% on prod-web-03
- **Type:** Warning

Click **Send Notification**.

### Step 4: See it arrive in real-time

Tab 1 (Bob) will instantly show:
- A **toast popup** sliding in from the right
- The notification appearing at the top of the feed with an amber "WARNING" badge
- The **unread counter** incrementing on the badge

### Step 5: Interact

- Click **"Mark read"** on the notification to clear its unread state
- Use the **"Unread"** filter to show only unread notifications
- Click the **checkmark button** to mark all as read at once

---

## 🧠 Design Decisions

### Why Redis Pub/Sub over alternatives?

| Option | Considered | Decision |
|---|---|---|
| **Redis Pub/Sub** | ✅ Chosen | Fire-and-forget, sub-millisecond latency, zero configuration, perfect for ephemeral real-time delivery |
| **RabbitMQ** | Considered | Overkill for this use case — adds operational complexity (exchanges, bindings, queues) without proportional benefit |
| **Kafka** | Considered | Designed for durable event streaming, not transient notification delivery. Would need consumer groups, offset management, and compaction policies |
| **Database polling** | Rejected | Adds latency (polling interval), unnecessary database load, and doesn't scale well with concurrent users |

**The key insight:** Redis Pub/Sub is "fire-and-forget" by design — if no subscriber is listening, the message is lost. That's *fine* for us because we persist to PostgreSQL first. Redis's only job is distribution across app instances, not durability.

### Why WebSocket (STOMP) over Server-Sent Events?

**WebSocket** gives us full-duplex communication — the client can send messages back to the server if needed (e.g., typing indicators, presence status). SSE is one-directional and would require a separate mechanism for client-to-server communication.

**STOMP** adds topic-based routing on top of raw WebSocket. Without it, we'd need to build our own message routing logic. With STOMP, we just `convertAndSend("/topic/notifications/{userId}", payload)` and Spring handles the rest.

**SockJS** provides automatic fallback for environments where WebSocket connections are blocked (corporate proxies, older browsers). It transparently degrades to long-polling without code changes.

### Why PostgreSQL + Redis (dual storage)?

This separates **concerns** and **access patterns**:

- **PostgreSQL** answers: *"What notifications did Bob receive last week?"* — historical queries, pagination, filtering
- **Redis** answers: *"Bob just got a notification — tell every app server immediately"* — real-time fan-out

Neither replaces the other. You could build a simpler system with just PostgreSQL (poll for new rows), but it wouldn't be real-time. You could build with just Redis (store + pubsub), but you'd lose durability and queryability.

### Scalability model

```
                    ┌─── App Instance 1 ──── WS clients (users A, B)
Load Balancer ──────┼─── App Instance 2 ──── WS clients (users C, D)
                    └─── App Instance 3 ──── WS clients (users E, F)
                              │
                              ▼
                         Redis Pub/Sub
                    (all instances subscribe)
```

When User A sends a notification to User D:
1. Instance 1 receives the API call and publishes to Redis
2. **All three instances** receive the Redis message
3. Instance 2 finds User D's WebSocket session and delivers it
4. Instances 1 and 3 check, find no matching session, and discard — no harm done

This is horizontally scalable with zero session affinity requirements.

---

## ⚡ Performance Considerations

### Concurrent users

- Each WebSocket connection is a lightweight, persistent TCP connection — Spring Boot can handle **thousands** on a single instance
- STOMP's simple broker uses an in-memory map for topic subscriptions — O(1) lookup per delivery
- Notifications are written to PostgreSQL asynchronously from the user's perspective (the API returns after persist + publish, delivery happens via the subscriber)

### Redis memory

- Pub/Sub messages are **not stored** in Redis — they're delivered and forgotten. Zero memory growth from message volume
- The only Redis memory usage is the connection overhead for subscriber channels
- Suitable for high-throughput scenarios (Redis Pub/Sub benchmarks at ~1M messages/second on a single node)

### WebSocket connection limits

- Default Spring Boot configuration handles up to **~10,000** concurrent WebSocket connections per instance
- SockJS fallback ensures connectivity even when WebSocket is blocked
- Connection heartbeats (built into STOMP) automatically clean up dead sessions

### Database optimization

- Indexed queries on `recipientId` and `recipientId + read` for fast lookups
- Bulk `UPDATE` for mark-all-as-read (single query instead of N updates)
- `@Transactional(readOnly = true)` on read operations for JPA query optimization

---

## 📁 Project Structure

```
src/main/java/com/notifyhub/
│
├── NotifyHubApplication.java          # Application entry point
│
├── config/
│   ├── RedisConfig.java               # Redis template, Pub/Sub listener, ObjectMapper with JSR310
│   ├── WebSocketConfig.java           # STOMP broker config, /ws endpoint with SockJS
│   └── SecurityConfig.java            # Permit-all security (demo mode)
│
├── model/
│   ├── Notification.java              # JPA entity — UUID PK, indexed recipientId
│   └── NotificationType.java          # Enum: INFO, SUCCESS, WARNING, ERROR
│
├── dto/
│   ├── NotificationRequest.java       # Validated inbound payload
│   ├── NotificationResponse.java      # Outbound payload with computed timeAgo
│   └── ApiResponse.java               # Generic REST wrapper with static factories
│
├── repository/
│   └── NotificationRepository.java    # Spring Data JPA with derived + custom queries
│
├── service/
│   ├── NotificationService.java       # Business logic — persist, publish, query, mark-read
│   └── RedisMessagePublisher.java     # Serializes notifications and publishes to Redis channel
│
├── listener/
│   └── RedisMessageSubscriber.java    # Redis → WebSocket bridge via SimpMessagingTemplate
│
├── controller/
│   └── NotificationController.java    # REST endpoints — 6 operations
│
└── exception/
    ├── ResourceNotFoundException.java # Custom 404 for missing notifications
    └── GlobalExceptionHandler.java    # Centralized error handling with validation details
```

```
src/main/resources/
├── application.yml                    # Datasource, Redis, JPA, logging config
└── static/
    ├── index.html                     # Dashboard — send form + notification feed
    ├── css/style.css                  # Dark theme, glassmorphism, animations
    └── js/app.js                      # SockJS/STOMP client, REST calls, real-time UI
```

---

## 🔮 Future Improvements

- [ ] **Authentication & Authorization** — JWT-based auth with Spring Security, per-user topic access control
- [ ] **Notification channels** — Extend beyond WebSocket to email (SendGrid), SMS (Twilio), and push notifications (FCM)
- [ ] **Priority levels** — Urgent vs. normal notifications with different delivery guarantees and UI treatment
- [ ] **Rate limiting** — Per-user send limits to prevent notification spam
- [ ] **Pagination** — Cursor-based pagination for notification history (currently returns all)
- [ ] **Redis Streams migration** — Replace Pub/Sub with Redis Streams for message persistence and consumer groups, enabling guaranteed delivery
- [ ] **Admin dashboard** — Analytics on notification volume, delivery latency, and read rates
- [ ] **Batch notifications** — Group-based delivery to notify all members of a team or channel
- [ ] **Read receipts via WebSocket** — Push read-state changes back to senders in real-time

---

## 🤝 Contributing

Contributions are welcome! If you'd like to improve NotifyHub:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/notification-priorities`)
3. Commit your changes with clear messages
4. Push to your fork and open a Pull Request

Please follow the existing code style — conventional Java naming, Lombok for boilerplate, and structured logging.

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

<div align="center">

Built by [Atharva](https://github.com/yourusername) · ⭐ Star this repo if you found it useful!

</div>