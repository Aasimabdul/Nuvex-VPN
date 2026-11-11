# Nuvex VPN

<p align="center">
  <strong>A calmer, cleaner approach to online privacy.</strong><br>
  A lightweight, ad-free Android VPN project built with Kotlin.
</p>

<p align="center">
  <a href="https://github.com/Aasimabdul/Nuvex-VPN/actions/workflows/android.yml"><img alt="Android CI" src="https://github.com/Aasimabdul/Nuvex-VPN/actions/workflows/android.yml/badge.svg"></a>
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-39A7FF.svg"></a>
  <img alt="Platform: Android" src="https://img.shields.io/badge/Platform-Android-101D36.svg">
  <img alt="Language: Kotlin" src="https://img.shields.io/badge/Language-Kotlin-7F52FF.svg">
</p>

---

## What is Nuvex?

Nuvex is an open-source Android VPN app prototype with a dark navy/neon-blue interface. The home screen has been redesigned to use vertical space more efficiently and no longer asks users to import a WireGuard configuration file.

> **Important:** Automatic VPN connection is not implemented yet. The Connect button currently explains that a verified provider integration and live VPN gateways are required. The country list is a visual preview only; these are not working servers. Do not rely on this development build to protect traffic.

## APK size

The latest previously verified CI build produced ABI-specific debug APKs below the 5 MiB target:

| APK | Measured size |
| --- | ---: |
| `app-arm64-v8a-debug.apk` | 3.52 MiB (3,694,619 bytes) |
| `app-armeabi-v7a-debug.apk` | 3.30 MiB (3,463,389 bytes) |

Those sizes refer to the previous build and must be re-verified for this UI update. These are separate architecture-specific debug APKs, not one universal APK or a production release.

## Current status

| Area | Status |
| --- | --- |
| Compact dark UI with reduced vertical padding | Implemented |
| Manual `.conf` import flow | Removed from the home screen |
| Automatic provider discovery | Not implemented |
| Real free country gateways | Not available |
| Connect button | Explains the missing provider/gateway dependency; does not fake a connection |
| DNS / IPv4 / IPv6 leak and network-change testing | Not verified |
| Production release | Not ready |

## How automatic connection must work

A Calyx/Bitmask-style experience requires a trusted provider that exposes a compatible configuration/discovery service and operates real VPN gateways. The client must fetch and validate provider configuration, select an available gateway, request Android VPN consent, establish the tunnel, and report success only after the tunnel is actually up.

Nuvex does not currently have a provider agreement, compatible provider API integration, or its own funded gateways. We must not invent server endpoints, reuse another provider's private infrastructure, or show placeholder locations as connected servers. The official [Bitmask Android source](https://github.com/leapcode/bitmask_android) documents the provider-based approach; it also uses additional native components and is GPL-licensed, so directly importing it would require a license and APK-size review.

## Build from source

Requirements: Android SDK Platform 35, JDK 17 and Gradle 8.9.

```bash
gradle --no-daemon :app:assembleDebug
```

Per-ABI debug APKs are generated under `app/build/outputs/apk/debug/`. CI uploads APKs and SHA-256 checksums under the [GitHub Actions workflow runs](https://github.com/Aasimabdul/Nuvex-VPN/actions).

## Security and privacy

- No ad SDK or analytics SDK is included.
- No fake gateway addresses or embedded provider secrets are used.
- DNS handling, IPv4/IPv6 leak resistance, kill-switch behavior, reconnects, and long-duration stability have not been verified.
- Until provider integration and security tests are complete, this app is not suitable for sensitive traffic.

## Roadmap

1. Choose a legitimate provider integration or operate Nuvex gateways.
2. Implement provider discovery and secure configuration delivery.
3. Add tunnel lifecycle, routing, DNS and leak tests.
4. Verify APK sizes and publish checksums for release builds.

## License

MIT — see [LICENSE](LICENSE).
