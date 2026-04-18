# Warpnet Protocol (client perspective)

This document describes the subset of the Warpnet node's libp2p stream protocol
that the Warpdroid Android client speaks. It is derived from the upstream
Warpnet Go source — see the [canonical
repository](https://github.com/Warp-net/warpnet) for the authoritative definitions.

Sources used:
- `warpnet/event/paths.go` — protocol IDs
- `warpnet/event/event.go` — request/response envelope and event payload structs
- `warpnet/domain/warpnet.go` — core DTOs (Tweet, User, Chat, ChatMessage, Notification, etc.)
- `warpnet/core/middleware/middleware.go` — signature verification and auth
- `warpnet/core/handler/*.go` — per-protocol handlers
- `warpnet/core/stream/stream.go` — wire framing

## Transport

- libp2p streams over TCP with Noise encryption.
- A pre-shared key (PSK) scopes the private libp2p network. Without the correct
  PSK the TCP handshake fails.
- One request per stream. The client opens a stream on a specific protocol ID,
  writes the full request, closes the write half, and reads the response to
  EOF. No length framing, no multiplexing over a single stream.

## Wire format

All payloads are JSON. The client wraps each payload in a signed envelope:

```json
{
  "body":        <raw JSON of event payload>,
  "message_id":  "<uuid>",
  "node_id":     "<libp2p peer ID of the client>",
  "path":        "<protocol ID, e.g. /private/get/timeline/0.0.0>",
  "timestamp":   "<RFC3339Nano>",
  "version":     "0.0.0",
  "signature":   "<base64 Ed25519 signature of body>"
}
```

`Message` is defined in `warpnet/event/event.go`. The server:
1. Reads the envelope.
2. Derives the sender's Ed25519 public key from `node_id` (the libp2p peer ID).
3. Verifies `signature` against `body`.
4. If the protocol ID contains `/private/`, requires that the sender's peer ID
   is in the paired-peers map.
5. Dispatches `body` to the handler for that protocol ID.

The response is the raw JSON of the handler's return value, or a
`ResponseError { code, message }` on failure.

## Pairing and authentication

- There is no session token. Every request is authenticated by the Ed25519
  signature + peer-ID lookup.
- Pairing uses `PRIVATE_POST_PAIR` (`/private/post/admin/pair/0.0.0`) with an
  `AuthNodeInfo` payload. Once paired, the peer ID is recorded on the node.
- Warpdroid currently stubs pairing — see `docs/decisions.md` — and relies on
  the fact that the peer ID is derived from the client's Ed25519 key, which
  must match the key used to sign the envelope.

## Protocol IDs

From `warpnet/event/paths.go`. Items marked **private** require pairing.

| Protocol ID | Purpose | Request body | Response body |
|---|---|---|---|
| `/public/get/info/0.0.0` | Node metadata | empty | `warpnet.NodeInfo` |
| `/public/get/user/0.0.0` | Get user profile | `GetUserEvent { user_id }` | `domain.User` |
| `/public/get/users/0.0.0` | List users | `GetAllUsersEvent { user_id, cursor, limit }` | `UsersResponse` |
| `/public/get/whotofollow/0.0.0` | Suggestions | `GetAllUsersEvent` | `UsersResponse` |
| `/public/get/tweets/0.0.0` | User timeline | `GetAllTweetsEvent { user_id, cursor, limit }` | `TweetsResponse` |
| `/public/get/tweet/0.0.0` | Single tweet | `GetTweetEvent { tweet_id, user_id }` | `domain.Tweet` |
| `/public/get/tweetstats/0.0.0` | Engagement counts | `GetTweetStatsEvent` | `TweetStatsResponse` |
| `/public/get/replies/0.0.0` | Replies to a root | `GetAllRepliesEvent { root_id, parent_id, cursor, limit }` | `RepliesResponse` |
| `/public/get/reply/0.0.0` | Single reply | `GetReplyEvent { reply_id, root_id, user_id }` | `domain.Tweet` |
| `/public/post/reply/0.0.0` | Post reply | `NewReplyEvent` | `domain.Tweet` |
| `/public/delete/reply/0.0.0` | Delete reply | `DeleteReplyEvent` | ack |
| `/public/post/like/0.0.0` | Like a tweet | `LikeEvent` | `LikesCountResponse` |
| `/public/post/unlike/0.0.0` | Remove like | `LikeEvent` | `LikesCountResponse` |
| `/public/post/retweet/0.0.0` | Retweet | `domain.Tweet` | `domain.Tweet` |
| `/public/post/unretweet/0.0.0` | Undo retweet | `UnretweetEvent` | ack |
| `/public/post/follow/0.0.0` | Follow user | `NewFollowEvent` | ack |
| `/public/post/unfollow/0.0.0` | Unfollow | `NewUnfollowEvent` | ack |
| `/public/get/followers/0.0.0` | List followers | `GetFollowersEvent` | `FollowersResponse` |
| `/public/get/followings/0.0.0` | List followings | `GetFollowingsEvent` | `FollowingsResponse` |
| `/public/post/isfollowing/0.0.0` | Relationship probe | `GetIsFollowingEvent` | `IsFollowingResponse` |
| `/public/post/isfollower/0.0.0` | Relationship probe | `GetIsFollowerEvent` | `IsFollowerResponse` |
| `/public/post/chat/0.0.0` | Create chat | `NewChatEvent` | `domain.Chat` |
| `/public/post/message/0.0.0` | Send message | `domain.ChatMessage` | `domain.ChatMessage` |
| `/public/get/image/0.0.0` | Fetch image | `GetImageEvent { user_id, key }` | `GetImageResponse { file }` |
| `/public/post/admin/challenge/0.0.0` | Proof-of-work | `ChallengeEvent` | `ChallengeResponse` |
| `/public/post/moderate/result/0.0.0` | Moderation callback | `ModerationResultEvent` | ack |
| **private** `/private/post/admin/pair/0.0.0` | Pair client | `AuthNodeInfo` | `AuthNodeInfo` |
| **private** `/private/get/admin/stats/0.0.0` | Node stats | empty | stats |
| **private** `/private/post/login/0.0.0` | Owner login | `LoginEvent` | `AuthNodeInfo` |
| **private** `/private/post/logout/0.0.0` | Owner logout | `LogoutEvent` | ack |
| **private** `/private/get/timeline/0.0.0` | Aggregated home feed | `GetAllTweetsEvent` | `TweetsResponse` |
| **private** `/private/get/notifications/0.0.0` | Notifications | `GetNotificationsEvent` | `GetNotificationsResponse` |
| **private** `/private/post/tweet/0.0.0` | Publish tweet | `domain.Tweet` | `domain.Tweet` |
| **private** `/private/delete/tweet/0.0.0` | Delete tweet | `DeleteTweetEvent` | ack |
| **private** `/private/post/user/0.0.0` | Update profile | `domain.User` | `domain.User` |
| **private** `/private/post/image/0.0.0` | Upload image(s) | `UploadImageEvent { image1..4 }` | `UploadImageResponse { key1..4 }` |
| **private** `/private/get/chat/0.0.0` | Get single chat | `GetChatEvent` | `domain.Chat` |
| **private** `/private/get/chats/0.0.0` | List chats | `GetAllChatsEvent` | `ChatsResponse` |
| **private** `/private/delete/chat/0.0.0` | Delete chat | `DeleteChatEvent` | ack |
| **private** `/private/get/message/0.0.0` | Single message | `GetMessageEvent` | `domain.ChatMessage` |
| **private** `/private/get/messages/0.0.0` | List messages | `GetAllMessagesEvent` | `ChatMessagesResponse` |
| **private** `/private/delete/message/0.0.0` | Delete message | `DeleteMessageEvent` | ack |

## Core DTOs

From `warpnet/domain/warpnet.go`. Field names are the JSON wire names.

### Tweet
```
id          string
created_at  time.Time
updated_at  time.Time?
parent_id   string?
retweeted_by string?
root_id     string
text        string
user_id     string
username    string
image_keys  string[]?
network     string
moderation  TweetModeration?
```

### User
```
id                     string
username               string
bio                    string
birthdate              string
avatar_key             string?       (mime;base64)
background_image_key   string        (mime;base64)
created_at             time.Time
updated_at             time.Time?
followings_count       int64
followers_count        int64
tweets_count           int64
isOffline              bool
node_id                string
network                string
rtt                    int64
website                string?
moderation             UserModeration?
metadata               map<string,string>
```

### Chat, ChatMessage, Like, Notification

See `warpnet/domain/warpnet.go` for full definitions. All fields use `snake_case`
JSON tags.

## Mastodon bridge

The node ships a read-mostly Mastodon federation bridge at
`warpnet/core/mastodon/client.go`. It exposes a pseudo-peer whose peer ID is
`12D3KooWDfpE8bR2iBjEMMe7gTVwEiahF9duFxETLHq3N6en9hsG` and routes a subset of
the protocol IDs above to Mastodon REST calls. The Warpdroid mapper treats
that bridge as an internal detail of the node — it just sees
`domain.Tweet` / `domain.User`.

## Client-side implications for Warpdroid

1. **The Android app signs its own envelopes.** The current
   `warp-net/android-binding` (`client.go:122-168`) writes raw bytes to the
   libp2p stream. It does **not** wrap in `event.Message` or sign. The Kotlin
   transport layer must build the envelope, sign the body with the node's
   Ed25519 key, and compute the `node_id` from the libp2p peer ID.

2. **Key export is blocked.** `newClient` (`android-binding/client.go:41`)
   generates a fresh Ed25519 key on every call and does not export it. To sign
   envelopes from Kotlin, the binding needs a new exported function such as
   `Sign(body []byte) (sig []byte, err error)` or must be taught to wrap
   payloads itself inside `Stream()`. This is a Phase 2 blocker — raised as an
   issue against `warp-net/android-binding`.

3. **Peer identity is ephemeral.** Because the binding regenerates the key
   every `Initialize()`, the peer ID changes on every app cold start. This
   defeats persistent pairing with the desktop node. Fix in `android-binding`:
   persist the private key to Android-managed secure storage (EncryptedFile /
   Keystore) and re-use it across sessions.

4. **No callbacks.** The binding exposes only synchronous request/response.
   Real-time features (live notifications, typing indicators) will need a
   separate long-lived stream protocol, which is out of scope for the current
   milestones.

5. **Images are inline base64.** `UploadImageEvent` carries up to 4 images as
   `mime;base64` strings, and `GetImageResponse` returns the same. No separate
   blob endpoint. This is expensive for large media and will need revisiting
   if video/large-photo upload becomes a requirement.
