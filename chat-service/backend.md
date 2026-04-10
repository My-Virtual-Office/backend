# Chat Service — Backend Summary

## Overview

A Spring Boot microservice handling real-time messaging, channels, threads, and read receipts. Runs on port **8084** with MongoDB for persistence and Redis for caching (read receipts + WS tickets).

## Tech Stack

| Component | Tech |
|---|---|
| Framework | Spring Boot 4.0.5, Java 21 |
| Database | MongoDB 7 (via Docker) |
| Cache | Redis 7 (via Docker) |
| Realtime | STOMP over WebSocket |
| Build | Maven (wrapper included) |

## Architecture

This service runs behind **Nginx** in a zero-trust setup. Nginx validates JWTs against the User Service and forwards `X-User-Id` and `X-User-Role` headers. The chat service never touches JWTs directly.

```
Client → Nginx (JWT validation) → Chat Service (trusts headers)
```

## Project Structure

```
src/main/java/com/khalwsh/chat_service/
├── config/              # mongo, redis, websocket, async, error handling
├── controller/          # REST + STOMP endpoints
├── dto/
│   ├── request/         # incoming payloads
│   ├── response/        # outgoing payloads
│   └── mapper/          # entity → DTO conversion
├── model/               # MongoDB documents (Channel, Message, ChatThread)
├── repository/          # Spring Data MongoDB repos
├── service/             # interfaces
│   └── impl/            # business logic
└── util/                # UserContext (header extraction)
```

## Data Model

### Channel
- `id` (ObjectId) — auto-generated
- `name` — channel name (null for DMs)
- `type` — `GROUP` or `DIRECT`
- `workspaceId` — integer (null for DMs)
- `members` — list of user IDs
- `dmKey` — deterministic key for DMs (e.g. `"5_42"`), unique index
- `createdBy`, `createdAt`, `updatedAt`

### Message
- `id` (ObjectId) — auto-generated, time-sortable
- `channelId` — which channel this belongs to
- `senderId` — who sent it
- `senderRole` — stored at send time for admin-vs-admin delete checks
- `content` — message text (null after soft-delete)
- `type` — `TEXT` or `SYSTEM`
- `threadId` — if set, this msg is inside a thread (excluded from main channel feed)
- `replyToId` — inline reply reference
- `mentions` — list of mentioned user IDs
- `clientMessageId` — client-generated UUID for idempotent sends
- `deleted`, `deletedAt` — soft delete fields
- `createdAt`, `updatedAt`

**Indexes:**
- `(channelId, createdAt)` — channel message queries
- `(threadId, createdAt)` — thread message queries
- `(senderId, clientMessageId)` — partial unique index for idempotency (only applied when clientMessageId exists)

### ChatThread
- `id` (ObjectId) — auto-generated
- `channelId` — parent channel
- `rootMessageId` — the message this thread branches from (unique)
- `name` — thread name
- `createdBy` — thread creator
- `creatorRole` — stored for admin-vs-admin deletion rules
- `deleted`, `createdAt`, `updatedAt`

## Key Features

### Channel Management
- Create GROUP channels (require workspaceId + members)
- Join/leave group channels (idempotent join, can't join DMs)
- Create DMs (idempotent via `dmKey` unique index, race-condition safe with `DuplicateKeyException` catch)
- Self-DMs blocked

### Messaging
- Send messages to channels or threads
- Page-based and cursor-based pagination
- Thread messages excluded from main channel feed
- Idempotent sends via `clientMessageId`
- Inline replies via `replyToId`
- User mentions

### Edit / Delete Authorization
- **Edit**: only the sender can edit their own message. Admins can't edit others'
- **Delete (soft)**: sender can delete their own. Admins can delete non-admin messages. Admins can NOT delete other admins' messages
- Same rules apply to thread deletion

### Threading
- One thread per root message (enforced by unique index)
- Root message must exist, belong to the channel, and not already be inside a thread
- Thread deletion is soft + triggers async cleanup of all thread messages

### Read Receipts
- Channel and thread tracked separately in Redis
- Keys: `read:{channelId}:{userId}` and `read:thread:{threadId}:{userId}`
- Forward-only cursor — older marks can't overwrite newer ones

### WebSocket
- STOMP over WebSocket at `/api/chat/connect`
- Ticket-based auth: client gets a one-time ticket via REST, passes it as `?ticket=` query param during WS handshake
- Subscription authorization: user must be a channel member to subscribe. Thread access checked via parent channel
- Standardized event envelope: `{ action, payload }`
- Typing indicators (broadcast only, no DB)

### Error Handling
- `GlobalExceptionHandler` catches MongoDB/Redis failures (503), validation errors (400), bad IDs (400), and generic exceptions (500)
- STOMP errors sent to `/user/queue/errors` with structured error codes

## Tests

**95 unit tests** covering all service implementations:

| Test Class | Tests | What it covers |
|---|---|---|
| `ChannelServiceImplTest` | 25 | create, get, join, leave, DM idempotency, DM race condition, self-DM block, isMember |
| `MessageServiceImplTest` | 28 | send (membership, idempotency, mentions, threads), pagination, edit auth, delete auth (admin rules) |
| `ThreadServiceImplTest` | 19 | create (all validations), delete (creator, admin-vs-admin, async cleanup) |
| `ReadReceiptServiceImplTest` | 10 | channel + thread marks, forward-only cursor, unread counts |
| `WebSocketTicketServiceImplTest` | 9 | create (uniqueness, TTL), validate (one-time use, null safety) |
| `ThreadCleanupServiceImplTest` | 4 | batch soft-delete, skip already-deleted, empty thread, error resilience |

Run tests:
```bash
# Linux / Mac
./mvnw test

# Windows
mvnw.cmd test
```

## How to Run

```bash
# set JAVA_HOME first
export JAVA_HOME=/path/to/your/jdk          # Linux / Mac
$env:JAVA_HOME = "C:\Users\Two Star\.jdks\openjdk-24.0.2"       # Windows (PowerShell)

# start mongo + redis
docker compose up -d

# run the service
# Linux / Mac
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Service starts on `http://localhost:8084`. Docker containers are auto-managed by Spring Boot's Docker Compose integration.
