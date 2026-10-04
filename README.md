# JARVIS Mobile

A Kotlin Multiplatform and Compose Multiplatform companion app for OpenJarvis. The phone is a thin client: the agent, its tools, the persona, memory and model routing all keep running on the Mac.

## Getting started

On the Mac (OpenJarvis repo):

```bash
uv sync --extra mobile                       # QR, FCM, APNs dependencies
jarvis mobile enable                         # add --public-url for use outside home
# restart JARVIS (the desktop app then binds 0.0.0.0)
```

To pair, open **Settings → Teléfono** in the desktop app (or run `jarvis mobile pair` in a terminal). In the phone app, tap **Escanear QR**. The app stores every address in the QR: at home it uses the LAN, and away from home it switches to the public one (Tailscale or tunnel).

```bash
./gradlew :androidApp:installDebug                       # Android
cd iosApp && xcodegen generate && open iosApp.xcodeproj  # iOS (set your team)
./gradlew :shared:testDebugUnitTest                      # common tests
```

### Outside your home network

Install [Tailscale](https://tailscale.com) on the Mac and on the phone, signed in to the same account. Then run `jarvis mobile enable --public-url http://<mac>.<tailnet>.ts.net:8000` and pair again.

### Push notifications (optional)

- **Android (FCM):** create a Firebase project and put its `google-services.json` in `androidApp/`. On the Mac, run `jarvis mobile enable --fcm <service-account.json>`.
- **iOS (APNs, no Firebase):** create an Apple Developer key with APNs and download its `.p8`. On the Mac, run `jarvis mobile enable --apns-key <file.p8> --apns-key-id <id> --apns-team-id <team>`. In Xcode, set `DEVELOPMENT_TEAM` (Push Notifications capability).

Without these, the app still gets every event live while it is open.

## Protocol (`PROTOCOL_VERSION = 1`)

| What | Where |
|---|---|
| Version handshake (public) | `GET /v1/mobile/hello` |
| Pairing | `POST /v1/mobile/pair/complete` `{code, name, platform, protocol}` → `{token, device}` |
| Chat | `POST /v1/chat/completions` (SSE), with the same agent and tools as the desktop |
| Live events + control | WebSocket `/v1/mobile/ws`, JSON-RPC style: `hello`, `ping`, `sessions.live`, `sessions.events`, `sessions.answer`, `sessions.message`, `push.register` |
| Server events | `session_event` (Claude finished / asks permission / summary), `agent_event` (tool calls, inference) |

Every request sends `Authorization: Bearer oj_dev_…`. Direct loopback requests (the desktop app) need no token. Requests relayed by a local proxy (cloudflared, Tailscale Serve) are never treated as loopback.

## Layout

```
shared/src/commonMain/kotlin/com/felipechaux/jarvis/
├── core/        network (Ktor, SSE, RPC WS, address failover), storage (Keystore/Keychain),
│                audio (VAD ported from the desktop, PCM, WAV), push, database (Room)
├── feature/
│   ├── pairing/   QR payload → device token
│   ├── chat/      SSE streaming, tool calls, presence, Room history, voice
│   ├── voice/     transcription and TTS against the Mac
│   └── sessions/  live Claude Code sessions, answer permission prompts, activity feed
├── di/          Koin (platform module per target)
└── App.kt       pairing until credentials exist, then Chat / Sessions tabs
androidApp/      Android entry point
iosApp/          SwiftUI host + APNs (XcodeGen: project.yml)
```

## Pending

- **On-device wake word** ("Jarvis" without touching the screen). This needs a choice of engine: Porcupine (Picovoice key) or openWakeWord (custom model). Today the mic is tap-to-talk with automatic stop (VAD).
