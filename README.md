# Nuvex VPN

<p align="center">
  <strong>A calmer, cleaner approach to online privacy.</strong><br>
  A lightweight, ad-free Android VPN project built with Kotlin and WireGuard.
</p>

<p align="center">
  <a href="https://github.com/Aasimabdul/Nuvex-VPN/actions/workflows/android.yml"><img alt="Android CI" src="https://github.com/Aasimabdul/Nuvex-VPN/actions/workflows/android.yml/badge.svg"></a>
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-39A7FF.svg"></a>
  <img alt="Platform: Android" src="https://img.shields.io/badge/Platform-Android-101D36.svg">
  <img alt="Language: Kotlin" src="https://img.shields.io/badge/Language-Kotlin-7F52FF.svg">
</p>

---

## What is Nuvex?

Nuvex VPN is an open-source Android project with a dark navy and neon-blue interface and a WireGuard-based tunnel engine. The app can import a WireGuard `.conf` profile, request Android's VPN consent, and attempt to establish a real tunnel through the profile's configured peer.

> **Important:** This is an early development build, not a ready-to-use consumer VPN service. It does not include built-in free gateways or a server-discovery service. You must import a valid profile from a VPN provider you trust. The countries displayed in the interface are planned locations, not live servers. Do not rely on this build to protect sensitive traffic.

## APK size

The latest successful CI build produced ABI-specific debug APKs below the 5 MiB target:

| APK | Measured size |
| --- | ---: |
| `app-arm64-v8a-debug.apk` | 3.52 MiB (3,694,619 bytes) |
| `app-armeabi-v7a-debug.apk` | 3.30 MiB (3,463,389 bytes) |

These are separate APKs for different CPU architectures, not one universal APK. Install only the APK matching your device. The workflow now fails if any generated APK exceeds 5 MiB. These are **debug/test APKs**, not a production release.

## Current status

| Area | Status |
| --- | --- |
| Kotlin Android app and dark UI | Implemented |
| WireGuard tunnel library | Integrated |
| Import WireGuard `.conf` profile | Implemented in app flow |
| Android VPN consent request | Implemented in app flow |
| Connect / disconnect via imported profile | Implemented; requires a valid reachable peer |
| Built-in free country gateways | Not available |
| Managed gateway discovery and profile delivery | Not available |
| DNS / IPv4 / IPv6 leak and network-change testing | Not yet verified |
| 5 MiB per-ABI debug APK budget | Verified by latest CI; enforced on future builds |
| Production release | Not ready |

## Connect using a trusted profile

1. Obtain a WireGuard configuration file (`.conf`) from a VPN provider you trust or from your own WireGuard server.
2. Install the Nuvex development APK.
3. Tap **Import WireGuard profile** and choose the configuration file.
4. Approve Android's VPN permission prompt.
5. The app will show connected only after the tunnel backend reports that the tunnel is up. A valid profile alone does not guarantee that the remote server is online.

Never share your WireGuard configuration: it contains a private key. Do not send the profile to anyone or commit it to this repository.

## Build from source

Requirements: Android SDK Platform 35, JDK 17 and Gradle 8.9.

```bash
gradle --no-daemon :app:assembleDebug
```

Per-ABI debug APKs are generated under `app/build/outputs/apk/debug/`. CI uploads APKs and SHA-256 checksums under the [latest workflow run](https://github.com/Aasimabdul/Nuvex-VPN/actions). Download the artifact named `nuvex-vpn-android-debug-apks`, then extract the ZIP to get the APKs and `SHA256SUMS.txt`.

## Security and privacy

- No ad SDK or analytics SDK is included in this project.
- The app does not embed private gateway keys or pretend that placeholder countries are working servers.
- The security and logging policy of the gateway provider is separate from the Nuvex source code.
- DNS handling, IPv4/IPv6 leak resistance, kill-switch behavior, reconnect after network changes, and long-duration stability still need independent testing.
- Until those checks and managed gateway operations exist, this app is not suitable for sensitive traffic.

## Roadmap

1. Verify connection lifecycle, error handling and app/process recovery.
2. Add automated tunnel, routing, DNS and leak tests.
3. Establish a transparent, sustainable gateway provider/infrastructure plan before advertising built-in country servers.
4. Measure APK sizes, optimize where possible, and publish checksums for any release.

## Contributing

Bug reports and pull requests are welcome. Do not include private keys, VPN profiles, credentials or sensitive network logs in issues.

## License

MIT — see [LICENSE](LICENSE).

<p align="center"><strong>Built in the open. Privacy claims earned through testing.</strong></p>
