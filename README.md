# Nuvex VPN

A lightweight, ad-free, open-source Android VPN client built with native Kotlin.

> **Development status:** early scaffold. The Android app and UI are being built, but no production VPN gateway is configured yet. Server locations shown in the UI are planned choices, not live servers. The app must not claim a secure connection until a real, audited VPN protocol and reachable gateways are integrated.

## Goals

- Native Android, Kotlin, minimal dependencies
- Dark navy and neon-blue interface
- Accessible connect/disconnect states and server selection
- No ads, trackers, or analytics by default
- Explicit Android VPN permission flow
- CI build checks and transparent release notes
- Target a small APK; final size depends on protocol and ABI choices

## Build

Requirements: JDK 17 and Android SDK Platform 35.

Open this repository in Android Studio and let Gradle sync, then build the `app` module. Or use:

```bash
gradle :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## VPN backend

A UI is not a VPN by itself. Before enabling production traffic, integrate a supported protocol and real gateways, validate certificates/keys, handle DNS and IPv4/IPv6 routes, kill-switch behavior, reconnects, and leak testing. Never ship fake endpoints or embed private server credentials in the APK.

## Planned locations

The initial selector lists 10 intended regions as placeholders. They become selectable live endpoints only after a verified provider/backend is configured.

## Privacy

Nuvex aims to collect no personal data. This scaffold contains no ad SDK or analytics SDK. Review dependencies and the eventual gateway operator's logging policy before a production release.

## License

MIT. See [LICENSE](LICENSE).
