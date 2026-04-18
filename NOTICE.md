# Warpdroid — Attribution and License Notice

Warpdroid is a Warpnet Android client. It is a fork of the Tusky Mastodon
client (Copyright (C) the Tusky Contributors, licensed under GNU GPL-3.0-or-later)
with the Mastodon networking layer replaced by a Warpnet peer-to-peer transport.

## Combined license: AGPL-3.0-or-later

The combined work is distributed under the GNU Affero General Public License,
version 3 or later. This is the stronger of the two upstream licenses and is
compatible with both:

- GPL-3.0-or-later (Tusky upstream) — compatible with AGPL-3.0 per GPL-3 §13.
- AGPL-3.0-or-later (Warpnet node, android-binding).

See `LICENSE.md` for the full text.

## Upstream attribution

### Tusky

- Source: https://github.com/tuskyapp/Tusky
- License: GPL-3.0-or-later
- Copyright (C) 2017-2025 the Tusky Contributors.

Substantial portions of this codebase — including the timeline UI, compose
flow, notification UI, theme system, resource files, and most of the
non-network Kotlin — originate from Tusky. Their copyright headers and
license notices are retained in the source files they introduce.

### Warpnet

- Source: https://github.com/Warp-net/warpnet
- License: AGPL-3.0-or-later
- Copyright (C) 2025 Vadim Filin.

The Warpnet node supplies the protocol and DTO definitions that Warpdroid
targets.

### Warpnet Android binding

- Source: https://github.com/Warp-net/android-binding
- License: AGPL-3.0-or-later
- Copyright (C) 2025 Vadim Filin.

The `warpnet.aar` shipped with Warpdroid is produced from this project via
`gomobile bind`. See `docs/warpnet-protocol.md` and `app/build.gradle` for
integration details.

## Changes from upstream Tusky

Warpdroid's key divergences from Tusky:

- Mastodon REST/OAuth networking replaced with Warpnet libp2p stream RPC.
- All server-derived persistence (Room timeline, account, notification,
  instance, and draft caches) removed; data lives only in the user's
  Warpnet desktop node.
- Instance picker and login flow stubbed.
- Federated/local timeline tabs removed.
- Deep-link handlers for Mastodon URL schemes removed.

## Source availability

Because this application reaches the AGPL-3.0 distribution trigger when it
communicates with end users, its complete corresponding source code must be
offered to every user who interacts with a deployed copy. The source is
available at:

    https://github.com/Warp-net/warpdroid
