# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0] - 2026-11-27

First public release of **AdDroid Home**.

### Added
- **Server Connection & Security:**
  - Connect to self-hosted AdGuard Home instances over HTTPS or private-LAN HTTP (`RFC 1918` / link-local / `.local` enforced at the OkHttp layer).
  - Opt-in trust for self-signed homelab TLS certificates plus user-installed root CAs.
  - On-device AES-256-GCM credential encryption backed by Android Keystore.
  - Runtime **Local Network** (`ACCESS_LOCAL_NETWORK`) permission handling for Android 17 (API 37).
- **Interactive Dashboard & Charts:**
  - Live DNS protection master toggle and quick pause actions (`5 min`, `15 min`, `1 hour`) with countdown display.
  - Stats time-range selector (`Default`, `1h`, `6h`, `12h`, `24h`, `7d` capped to the server's configured retention window).
  - Clickable metric cards (**DNS Queries**, **Blocked**, **Avg Latency**, **Safe & Parental**) and clickable **Top Activity** lists (**Blocked**, **Queried**, **Clients**) that drill straight into the filtered Query Log.
  - Touch-interactive **Query Activity** chart with tap/drag scrubbing and **Upstreams** share & latency breakdown.
- **Query Log:**
  - Version-aware status filtering (`reason` / `response_status`), search, cursor-based pagination, query detail bottom sheet, and one-tap **Block / Unblock** rule creation.
- **Filters & Custom Rules:**
  - Global DNS filtering switch, blocklist & allowlist toggle/add/remove, upstream list refresh, and monospace custom filtering rules editor.
- **Blocked Services:**
  - Searchable service catalog with category chips, `Blocked` filter toggle, staged batch updates, and schedule-preserving `PUT /control/blocked_services/update`.
- **Settings & DNS Rewrites:**
  - Toggle **Safe Browsing**, **Parental Control**, and **Safe Search** (master switch plus individual engine chips for Google, Bing, YouTube, DuckDuckGo, Ecosia, Pixabay, and Yandex).
  - Full **DNS Rewrites** CRUD (add, edit, toggle, delete) with backward-compatible support for pre-schema-v31 servers.
  - Confirmation-guarded **Clear Query Log** and **Reset Statistics** maintenance actions.
- **Material 3 Design & Tooling:**
  - Flat Material 3 adaptive launcher icon with Android 13+ monochrome themed icon layer and dynamic color support.
  - Unit test suite covering DTO serialization, repository calls, ViewModel flows, custom rule updates, and LAN security policy.
  - GitHub Actions CI and tag-driven dual-APK release workflow (`addroidhome.apk`, `addroidhome-debug.apk`, `SHA256SUMS.txt`).

[Unreleased]: https://github.com/Syxhen/AdDroidHome/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Syxhen/AdDroidHome/releases/tag/v1.0.0
