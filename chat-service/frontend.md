# Chat Service — Frontend Integration Guide

## Prerequisites

- **Java 21+** (JDK)
- **Docker** (for MongoDB + Redis)

## How to Run

```bash
# 1. clone the repo and cd into the service
cd backend/chat-service

# 2. set JAVA_HOME (point to your JDK 21+ installation)
# Linux / Mac
export JAVA_HOME=/path/to/your/jdk

# Windows (PowerShell)
$env:JAVA_HOME = "C:\Users\Two Star\.jdks\openjdk-24.0.2"

# 3. start mongo + redis (docker-compose.yml is included)
docker compose up -d

# 4. run the service
# Linux / Mac
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

The service starts on **http://localhost:8084**.  
Health check: `GET /api/chat/health` → `"OK"`

---

## Authentication

This service does NOT handle JWT itself. It expects **Nginx** to validate the JWT and forward these headers:

| Header | Description |
|---|---|
| `X-User-Id` | integer user ID (e.g. `42`) |
| `X-User-Role` | `ADMIN` or `USER` |

Every REST request must include these headers. For local testing without Nginx, just set them manually.

---

## REST API

Base path: `/api/chat`

### Channels

#### Create a group channel
```
POST /api/chat/channels
```
**Request body:**
```json
{
  "name": "general",
  "workspaceId": 100,
  "members": [1, 2, 3]
}
```
All fields required. The creator is auto-added to members.

**Response** `201`:
```json
{
  "id": "64f1a2b3...",
  "name": "general",
  "type": "GROUP",
  "workspaceId": 100,
  "members": [1, 2, 3],
  "createdBy": 1,
  "createdAt": "2026-04-10T14:00:00Z",
  "updatedAt": "2026-04-10T14:00:00Z"
}
```

#### List workspace channels
```
GET /api/chat/channels?workspaceId=100&page=1&limit=20
```
Returns only channels the requesting user is a member of.

**Response** `200`:
```json
{
  "content": [ /* ChannelResponse[] */ ],
  "totalPages": 1,
  "totalElements": 3,
  "currentPage": 1
}
```

#### Get a channel
```
GET /api/chat/channels/{id}
```

#### Join a channel
```
POST /api/chat/channels/{id}/join
```
No body needed. Only works for GROUP channels. Idempotent (joining twice is fine).

#### Leave a channel
```
POST /api/chat/channels/{id}/leave
```
No body needed. Only GROUP channels. Returns 400 if not a member.

---

### Direct Messages (DM)

#### Create or get a DM
```
POST /api/chat/dm
```
**Request body:**
```json
{
  "targetUserId": 2
}
```
Idempotent — calling twice returns the same DM. Self-DMs are blocked (400).

**Response** `200`: same `ChannelResponse` shape, with `type: "DIRECT"` and `workspaceId: null`.

#### List DMs
```
GET /api/chat/dm?page=1&limit=20
```
Returns all DMs for the current user, sorted by most recently updated.

---

### Messages

#### Send a message
```
POST /api/chat/channels/{channelId}/messages
```
**Request body:**
```json
{
  "content": "Hello world!",
  "threadId": null,
  "replyToId": null,
  "mentions": [2, 5],
  "clientMessageId": "uuid-from-client"
}
```
Only `content` is required. Everything else is optional.

- `threadId` — if set, the message goes into that thread instead of the main channel feed
- `replyToId` — inline reply reference
- `mentions` — array of user IDs being mentioned
- `clientMessageId` — client-generated UUID for idempotent sends. If you retry with the same `clientMessageId`, you get the original message back instead of a duplicate.

**Response** `201`:
```json
{
  "id": "64f1a2b3...",
  "channelId": "64f1a000...",
  "senderId": 1,
  "content": "Hello world!",
  "type": "TEXT",
  "threadId": null,
  "replyToId": null,
  "mentions": [2, 5],
  "clientMessageId": "uuid-from-client",
  "deleted": false,
  "deletedAt": null,
  "createdAt": "2026-04-10T14:00:00Z",
  "updatedAt": "2026-04-10T14:00:00Z"
}
```

#### Get channel messages (page-based)
```
GET /api/chat/channels/{channelId}/messages?page=1&limit=50
```
Returns top-level messages only (thread replies are excluded). Sorted newest first.

**Response** `200`: `PaginatedResponse<MessageResponse>`

#### Get channel messages (cursor-based)
```
GET /api/chat/channels/{channelId}/messages?before={messageId}&limit=50
GET /api/chat/channels/{channelId}/messages?after={messageId}&limit=50
```
- `before` — load older messages (scroll up)
- `after` — load newer messages (catch up after reconnect)

Cursor-based takes priority over page-based if both are provided.

**Response** `200`: `MessageResponse[]` (flat array, no pagination wrapper)

#### Edit a message
```
PUT /api/chat/messages/{messageId}
```
**Request body:**
```json
{
  "content": "edited text"
}
```
Only the original sender can edit. Admins cannot edit anyone else's messages.

**Response** `200`: updated `MessageResponse`

#### Delete a message (soft)
```
DELETE /api/chat/messages/{messageId}
```
Sets `deleted: true` and `content: null`. The message stays in the DB.

**Rules:**
- Users can delete their own messages
- Admins can delete non-admin users' messages
- Admins CANNOT delete other admins' messages
- Already-deleted messages are silently skipped

---

### Threads

#### Create a thread
```
POST /api/chat/channels/{channelId}/threads
```
**Request body:**
```json
{
  "rootMessageId": "64f1a2b3...",
  "name": "discussion"
}
```
Both fields required.

**Validation rules:**
- Root message must exist
- Root message must belong to this channel
- Root message can't already be inside a thread
- Only one thread per root message (409 if duplicate)

**Response** `201`:
```json
{
  "id": "64f1b000...",
  "channelId": "64f1a000...",
  "rootMessageId": "64f1a2b3...",
  "name": "discussion",
  "createdBy": 1,
  "deleted": false,
  "createdAt": "2026-04-10T14:00:00Z",
  "updatedAt": "2026-04-10T14:00:00Z"
}
```

#### List threads in a channel
```
GET /api/chat/channels/{channelId}/threads?page=1&limit=20
```
Only returns non-deleted threads.

#### Get a thread
```
GET /api/chat/threads/{threadId}
```

#### Delete a thread (soft)
```
DELETE /api/chat/threads/{threadId}
```
Same admin rules as message deletion. Thread messages are cleaned up asynchronously.

#### Get thread messages
```
GET /api/chat/threads/{threadId}/messages?page=1&limit=50
GET /api/chat/threads/{threadId}/messages?before={messageId}&limit=50
GET /api/chat/threads/{threadId}/messages?after={messageId}&limit=50
```
Same pagination logic as channel messages.

---

### Read Receipts

Channel and thread read receipts are tracked separately.

#### Mark channel as read
```
POST /api/chat/channels/{channelId}/read
```
**Request body:**
```json
{
  "lastReadMessageId": "64f1a2b3..."
}
```
The cursor only moves forward — you can't mark older messages as "last read" if you've already read newer ones.

#### Get channel unread count
```
GET /api/chat/channels/{channelId}/unread
```
**Response** `200`:
```json
{
  "unreadCount": 5,
  "lastReadMessageId": "64f1a2b3..."
}
```
`lastReadMessageId` is `null` if nothing has been read yet.

#### Mark thread as read
```
POST /api/chat/threads/{threadId}/read
```
Same body as channel read.

#### Get thread unread count
```
GET /api/chat/threads/{threadId}/unread
```
Same response shape.

---

### WebSocket Ticket

#### Get a ticket
```
POST /api/chat/ws-ticket
```
No body. Returns:
```json
{
  "ticket": "e556486f-435d-4537-bea0-e7d1e1f0627b"
}
```
Ticket is valid for **60 seconds** and can only be used **once**.

---

## WebSocket (STOMP)

### Connecting

1. Call `POST /api/chat/ws-ticket` to get a ticket (via REST, with `X-User-Id` header)
2. Connect via STOMP to: `ws://localhost:8084/api/chat/connect?ticket={ticket}`

The ticket is validated during the handshake. If invalid or expired, the connection is rejected.

### Subscribing

Subscribe to topics to receive real-time updates:

| Topic | Events |
|---|---|
| `/topic/channel/{channelId}` | new messages, edits, deletes in that channel |
| `/topic/thread/{threadId}` | new messages, edits, deletes in that thread |
| `/topic/channel/{channelId}/typing` | typing indicators for channel |
| `/topic/thread/{threadId}/typing` | typing indicators for thread |

Subscription is **authorized** — you can only subscribe to channels you're a member of. Thread access is checked through the parent channel.

### Sending Messages (via STOMP)

Send to `/app/chat/send`:
```json
{
  "channelId": "64f1a000...",
  "content": "Hello!",
  "threadId": null,
  "replyToId": null,
  "mentions": [2],
  "clientMessageId": "uuid-from-client"
}
```
Only `channelId` and `content` are required.

### Typing Indicators

Send to `/app/chat/typing`:
```json
{
  "channelId": "64f1a000...",
  "threadId": null,
  "typing": true
}
```
No database write — just broadcasts to subscribers.

### Event Envelope

All WebSocket messages come wrapped in this format:

```json
{
  "action": "NEW_MESSAGE",
  "payload": { /* the actual data */ }
}
```

| Action | Payload | When |
|---|---|---|
| `NEW_MESSAGE` | `MessageResponse` | someone sent a message |
| `EDIT_MESSAGE` | `MessageResponse` | someone edited a message |
| `DELETE_MESSAGE` | `MessageResponse` | someone deleted a message |
| `TYPING` | `TypingNotification` | someone started/stopped typing |
| `THREAD_DELETED` | thread info | a thread was soft-deleted |

**TypingNotification:**
```json
{
  "userId": 2,
  "channelId": "64f1a000...",
  "threadId": null,
  "typing": true
}
```

### Error Handling (STOMP)

Errors are sent to `/user/queue/errors`:
```json
{
  "action": "ERROR",
  "payload": {
    "code": "NOT_A_MEMBER",
    "message": "you are not a member of this channel"
  }
}
```

| Error Code | Meaning |
|---|---|
| `NOT_A_MEMBER` | user isn't in the channel |
| `CHANNEL_NOT_FOUND` | channel/thread doesn't exist |
| `INVALID_PAYLOAD` | missing required fields |
| `INTERNAL_ERROR` | something unexpected broke |

---

## Error Responses (REST)

All REST errors follow this shape:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "workspaceId is required for group channels",
  "timestamp": "2026-04-10T14:00:00Z"
}
```

| Status | When |
|---|---|
| `400` | validation failures, bad input |
| `403` | not a member, can't edit/delete someone else's stuff |
| `404` | channel/message/thread not found |
| `409` | duplicate thread for the same root message |
| `503` | MongoDB or Redis is down |

---

## Data Types Quick Reference

- All IDs are **MongoDB ObjectId strings** (24-char hex, e.g. `"64f1a2b3c4d5e6f7a8b9c0d1"`)
- Timestamps are **ISO 8601 UTC** (e.g. `"2026-04-10T14:00:00Z"`)
- User IDs are **integers** (e.g. `42`)
- Channel types: `"GROUP"` or `"DIRECT"`
- Message types: `"TEXT"` or `"SYSTEM"`
- Pages are **1-based** (page=1 is the first page)
