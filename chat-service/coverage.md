# Test Coverage — Chat Service

**Total: 232 tests · 0 failures**

---

## Service Layer (98 tests)

### ChannelServiceImpl — 26 tests

#### Create Group Channel
| # | Scenario |
|---|----------|
| 1 | Create channel with valid data → saved with correct name, type, workspaceId |
| 2 | Creator is auto-added to the members list if not already there |
| 3 | Creator not duplicated if already in members list |
| 4 | Null workspaceId → 400 "workspaceId is required" |
| 5 | Duplicate channel name in same workspace → DuplicateKeyException bubbles up |

#### Get Workspace Channels
| # | Scenario |
|---|----------|
| 6 | Returns paginated channels for user in a workspace |
| 7 | Empty workspace → returns empty page with zero elements |

#### Get Channel
| # | Scenario |
|---|----------|
| 8 | Existing channel → returns details |
| 9 | Non-existent channel → 404 "channel not found" |

#### Join Channel
| # | Scenario |
|---|----------|
| 10 | User joins group channel → added to members list |
| 11 | Already a member → idempotent, no save |
| 12 | Joining a DM channel → 400 "cannot join a direct message channel" |
| 13 | Non-existent channel → 404 |

#### Leave Channel
| # | Scenario |
|---|----------|
| 14 | User leaves group channel → removed from members |
| 15 | Leaving a DM → 400 "cannot leave a direct message channel" |
| 16 | Not a member → 400 "not a member of this channel" |
| 17 | Non-existent channel → 404 |

#### Get or Create DM
| # | Scenario |
|---|----------|
| 18 | Existing DM → returned without saving |
| 19 | New DM → created with correct dmKey, members, null workspaceId |
| 20 | dmKey is deterministic → (20,10) and (10,20) produce same key "10_20" |
| 21 | Two concurrent creates (race condition) → second catches DuplicateKeyException, falls back to lookup |
| 22 | DM with self → 400 "cannot create a DM with yourself" |

#### Get Direct Messages
| # | Scenario |
|---|----------|
| 23 | Returns paginated DMs for user |

#### isMember
| # | Scenario |
|---|----------|
| 24 | User in members list → true |
| 25 | User not in members list → false |
| 26 | Channel doesn't exist → false (no exception) |

---

### MessageServiceImpl — 29 tests

#### Send Message
| # | Scenario |
|---|----------|
| 1 | Valid send → message saved with channelId, senderId, content, timestamps |
| 2 | Sender role is stored on the message |
| 3 | Null sender role defaults to "USER" |
| 4 | Non-member sender → 403 "not a member of this channel" |
| 5 | threadId and replyToId are passed through when set |
| 6 | Mentions list is stored on the message |
| 7 | Duplicate clientMessageId → returns existing message (idempotency) |
| 8 | New clientMessageId → saves new message |
| 9 | Blank/whitespace clientMessageId → normalized to null |

#### Get Messages (Channel)
| # | Scenario |
|---|----------|
| 10 | Page-based pagination returns correct page |
| 11 | Cursor-based "before" returns messages before the cursor |
| 12 | Cursor-based "after" returns messages after the cursor |

#### Get Messages (Thread)
| # | Scenario |
|---|----------|
| 13 | Thread page-based pagination |
| 14 | Thread cursor "before" |
| 15 | Thread cursor "after" |

#### Edit Message
| # | Scenario |
|---|----------|
| 16 | Owner edits own message → content updated |
| 17 | Non-sender tries to edit → 403 |
| 18 | Admin tries to edit someone's message → 403 (only sender can edit) |
| 19 | Editing a soft-deleted message → 400 "cannot edit a deleted message" |
| 20 | Message not found → 404 |
| 21 | Edit updates updatedAt timestamp |

#### Delete Message
| # | Scenario |
|---|----------|
| 22 | Owner soft-deletes own message → deleted flag set, content nulled |
| 23 | Admin deletes a normal user's message → allowed |
| 24 | Admin tries to delete another admin's message → 403 |
| 25 | Non-owner non-admin → 403 |
| 26 | Already-deleted message → returns null (no-op) |
| 27 | Message not found → 404 |
| 28 | Delete sets deletedAt timestamp |
| 29 | Admin deletes own message → allowed |

---

### ThreadServiceImpl — 19 tests

#### Create Thread
| # | Scenario |
|---|----------|
| 1 | Valid creation → thread saved with channelId, rootMessageId, name |
| 2 | Creator role stored on thread entity |
| 3 | Non-member of channel → 403 |
| 4 | Root message not found → 404 |
| 5 | Root message belongs to different channel → 404 |
| 6 | Root message is already inside a thread → 400 |
| 7 | Thread already exists for the root message → 409 Conflict |

#### Get Channel Threads
| # | Scenario |
|---|----------|
| 8 | Returns paginated threads for channel |
| 9 | No threads → returns empty page |

#### Get Thread
| # | Scenario |
|---|----------|
| 10 | Existing thread → returns details |
| 11 | Non-existent → 404 |

#### Delete Thread
| # | Scenario |
|---|----------|
| 12 | Creator soft-deletes own thread |
| 13 | Admin deletes a normal user's thread → allowed |
| 14 | Admin tries to delete another admin's thread → 403 |
| 15 | Non-creator non-admin → 403 |
| 16 | Already-deleted thread → no-op |
| 17 | Thread not found → 404 |
| 18 | Delete triggers async message cleanup |
| 19 | Admin deletes own thread → allowed |

---

### ReadReceiptServiceImpl — 9 tests

#### Channel Reads
| # | Scenario |
|---|----------|
| 1 | markAsRead calls Lua script with correct Redis key |
| 2 | Redis key format is "read:{channelId}:{userId}" |
| 3 | getUnreadCount returns count + lastReadMessageId when cursor exists |
| 4 | getUnreadCount counts all messages when user has never read anything |

#### Thread Reads
| # | Scenario |
|---|----------|
| 5 | markThreadAsRead calls Lua script with thread key |
| 6 | Thread key uses "read:thread:{threadId}:{userId}" prefix |
| 7 | Thread mark does not use channel key |
| 8 | getThreadUnreadCount returns correct count |
| 9 | getThreadUnreadCount counts all when never read |

---

### WebSocketTicketServiceImpl — 11 tests

#### Create Ticket
| # | Scenario |
|---|----------|
| 1 | Returns a non-null ticket string |
| 2 | Ticket is stored in Redis with a TTL |
| 3 | Two calls produce different ticket values |
| 4 | Redis value stores "userId:role" |
| 5 | Null role defaults to "USER" |

#### Validate & Consume Ticket
| # | Scenario |
|---|----------|
| 6 | Valid ticket → returns userId and role |
| 7 | Admin role is correctly extracted |
| 8 | Invalid/unknown ticket → returns null |
| 9 | Null ticket → returns null |
| 10 | Ticket is deleted from Redis after consumption (one-time use) |
| 11 | Second use of same ticket returns null |

---

### ThreadCleanupServiceImpl — 4 tests

| # | Scenario |
|---|----------|
| 1 | All thread messages are soft-deleted in batch |
| 2 | Already-deleted messages are skipped |
| 3 | Empty thread → no error, graceful handling |
| 4 | Repository error during cleanup → caught, does not propagate |

---

## Controller Layer (73 tests)

### ChannelController — 9 tests

| # | Scenario |
|---|----------|
| 1 | POST /channels → 201 Created with channel body |
| 2 | POST /channels with duplicate name → 409 Conflict |
| 3 | GET /channels → 200 with paginated channels |
| 4 | GET /channels/{id} as member → 200 |
| 5 | GET /channels/{id} as non-member → 403 |
| 6 | POST /channels/{id}/join → 200, service called |
| 7 | POST /channels/{id}/leave → 200, service called |
| 8 | POST /dm → 200 with DM channel |
| 9 | GET /dm → 200 with paginated DMs |

### MessageController — 11 tests

| # | Scenario |
|---|----------|
| 1 | POST /channels/{id}/messages → 201 Created |
| 2 | GET messages as non-member → 403 |
| 3 | GET messages with no cursor → page-based pagination |
| 4 | GET messages with "before" cursor → cursor-based |
| 5 | GET messages with "after" cursor → cursor-based |
| 6 | "before" takes priority over "after" when both provided |
| 7 | PUT /messages/{id} → 200, WS broadcast to /topic/channel/{channelId} |
| 8 | PUT /messages/{id} in thread → WS broadcast to /topic/thread/{threadId} |
| 9 | DELETE /messages/{id} → 200, WS broadcast to channel topic |
| 10 | DELETE /messages/{id} in thread → WS broadcast to thread topic |
| 11 | DELETE when service returns null → no WS broadcast |

### ThreadController — 11 tests

| # | Scenario |
|---|----------|
| 1 | POST /channels/{id}/threads → 201 Created |
| 2 | GET /channels/{id}/threads as member → 200 |
| 3 | GET /channels/{id}/threads as non-member → 403 |
| 4 | GET /threads/{id} as member of parent channel → 200 |
| 5 | GET /threads/{id} as non-member of parent channel → 403 |
| 6 | DELETE /threads/{id} → 200, WS broadcast to thread topic |
| 7 | GET /threads/{id}/messages as non-member → 403 |
| 8 | GET thread messages → page-based pagination |
| 9 | GET thread messages with "before" cursor |
| 10 | GET thread messages with "after" cursor |
| 11 | "before" takes priority over "after" |

### ReadReceiptController — 8 tests

| # | Scenario |
|---|----------|
| 1 | POST /channels/{id}/read as member → 200 |
| 2 | POST /channels/{id}/read as non-member → 403 |
| 3 | GET /channels/{id}/unread as member → 200 with count |
| 4 | GET /channels/{id}/unread as non-member → 403 |
| 5 | POST /threads/{id}/read as member of parent channel → 200 |
| 6 | POST /threads/{id}/read as non-member of parent → 403 |
| 7 | GET /threads/{id}/unread as member of parent → 200 |
| 8 | GET /threads/{id}/unread as non-member of parent → 403 |

### ChatStompController — 20 tests

#### handleSendMessage
| # | Scenario |
|---|----------|
| 1 | Valid send → message saved, broadcast to /topic/channel/{id} |
| 2 | Thread message → broadcast to /topic/thread/{id} |
| 3 | null channelId in payload → error sent to /user/queue/errors |
| 4 | Blank content → error sent to /user/queue/errors |
| 5 | Service throws 403 → error with "FORBIDDEN" forwarded |
| 6 | Service throws 404 → error with "NOT_FOUND" forwarded |
| 7 | Service throws 400 → error with "BAD_REQUEST" forwarded |
| 8 | Service throws unknown ResponseStatusException → generic error |
| 9 | Service throws non-ResponseStatusException → generic error |
| 10 | Missing userId in session → error to /user/queue/errors |

#### handleTyping
| # | Scenario |
|---|----------|
| 11 | Typing in channel → broadcast to /topic/channel/{id} |
| 12 | Typing in thread → broadcast to /topic/thread/{id} |
| 13 | Null channelId in typing payload → error to /user/queue/errors |

#### handleException
| # | Scenario |
|---|----------|
| 14 | Exception with message → error sent to /user/queue/errors |
| 15 | Exception with null message → default error text |

#### Session Edge Cases
| # | Scenario |
|---|----------|
| 16 | Message with null session attributes → error |
| 17 | Session with userId but no role → defaults to "USER" |
| 18 | Valid admin session → message sent with ADMIN role |
| 19 | Session with string userId → parsed correctly |
| 20 | Typing with valid session → broadcast includes userId |

### WebSocketTicketController — 3 tests

| # | Scenario |
|---|----------|
| 1 | GET /ws/ticket as USER → returns ticket string |
| 2 | GET /ws/ticket as ADMIN → returns ticket string |
| 3 | Role is correctly passed from headers to service |

### HealthController — 1 test

| # | Scenario |
|---|----------|
| 1 | GET /health → returns "OK" |

---

## Config Layer (29 tests)

### GlobalExceptionHandler — 10 tests

| # | Scenario |
|---|----------|
| 1 | MongoException → 503 "database unavailable" |
| 2 | RedisConnectionFailureException → 503 "cache unavailable" |
| 3 | MethodArgumentNotValidException with field errors → 400 with field details |
| 4 | MethodArgumentNotValidException without field errors → 400 with default |
| 5 | IllegalArgumentException → 400 with message |
| 6 | ResponseStatusException 401 → forwarded as 401 |
| 7 | ResponseStatusException 403 → forwarded as 403 |
| 8 | ResponseStatusException 404 → forwarded as 404 |
| 9 | Catch-all Exception → 500 "unexpected error" |
| 10 | ResponseStatusException preserves reason phrase |

### WebSocketHandshakeInterceptor — 7 tests

| # | Scenario |
|---|----------|
| 1 | Valid ticket → handshake accepted, userId+role stashed in session |
| 2 | Admin ticket → "ADMIN" role stored in session |
| 3 | Invalid/expired ticket → handshake rejected (returns false) |
| 4 | Null ticket parameter → handshake rejected |
| 5 | Non-servlet request (e.g. raw WS) → handshake rejected |
| 6 | afterHandshake with null exception → no-op |
| 7 | afterHandshake with exception → no-op, doesn't throw |

### WebSocketSubscriptionInterceptor — 12 tests

| # | Scenario |
|---|----------|
| 1 | Non-SUBSCRIBE frame → passed through unchanged |
| 2 | SUBSCRIBE with null destination → passed through |
| 3 | SUBSCRIBE to unrelated destination → passed through |
| 4 | SUBSCRIBE to /topic/channel/{id} as member → allowed |
| 5 | SUBSCRIBE to /topic/channel/{id}/typing as member → allowed |
| 6 | SUBSCRIBE to /topic/channel/{id} as non-member → 403 |
| 7 | SUBSCRIBE to /topic/channel/{id} for non-existent channel → error |
| 8 | SUBSCRIBE to /topic/thread/{id} as member of parent channel → allowed |
| 9 | SUBSCRIBE to /topic/thread/{id}/typing as member → allowed |
| 10 | SUBSCRIBE to /topic/thread/{id} for non-existent thread → error |
| 11 | SUBSCRIBE to /topic/thread/{id} as non-member of parent → error |
| 12 | Missing userId in session attributes → error thrown |

---

## Util & DTO Layer (35 tests)

### UserContext — 18 tests

#### Header Parsing
| # | Scenario |
|---|----------|
| 1 | Valid X-User-Id + X-User-Role → parsed correctly |
| 2 | ADMIN role parsed |
| 3 | Lowercase "user" role accepted |
| 4 | Mixed-case "Admin" accepted |

#### Validation Errors
| # | Scenario |
|---|----------|
| 5 | Missing X-User-Id (null) → 401 |
| 6 | Blank X-User-Id → 401 |
| 7 | Non-numeric X-User-Id → 400 |
| 8 | Float X-User-Id (e.g. "10.5") → 400 |
| 9 | Missing X-User-Role (null) → 400 |
| 10 | Invalid role string → 400 |
| 11 | Empty role string → 400 |

#### UserInfo Helpers
| # | Scenario |
|---|----------|
| 12 | isAdmin() returns true for ADMIN |
| 13 | isAdmin() returns true for lowercase admin |
| 14 | isAdmin() returns false for USER |
| 15 | isUser() returns true for USER |
| 16 | isUser() returns false for ADMIN |
| 17 | getUserId() returns correct integer |
| 18 | getRole() returns correct string |

### DtoMapper — 10 tests

| # | Scenario |
|---|----------|
| 1 | Channel → ChannelResponse (GROUP, all fields) |
| 2 | Channel → ChannelResponse (DM, null name/workspaceId) |
| 3 | Message → MessageResponse (normal message, all fields) |
| 4 | Message → MessageResponse (thread + inline reply) |
| 5 | Message → MessageResponse (deleted → content nulled) |
| 6 | Message → MessageResponse (system message → senderId null) |
| 7 | Message → MessageResponse (null mentions → empty list) |
| 8 | Thread → ThreadResponse (all fields) |
| 9 | Thread → ThreadResponse (deleted state) |
| 10 | Thread → ThreadResponse (null name) |

### WebSocketEvent — 7 tests

| # | Scenario |
|---|----------|
| 1 | factory method of(action, payload) → correct event |
| 2 | of() with Map payload |
| 3 | of() with null payload → no exception |
| 4 | Action constants match expected strings |
| 5 | Builder works correctly |
| 6 | No-args constructor |
| 7 | All-args constructor |

---

## LoggingAspect — 7 tests

| # | Scenario |
|---|----------|
| 1 | Service call → result passed through from proceed() |
| 2 | Service call throws → exception propagated |
| 3 | Null args array → handled without error |
| 4 | Empty args array → handled without error |
| 5 | Null element in args array → handled without error |
| 6 | Controller call → result passed through |
| 7 | Controller call throws → exception propagated |
