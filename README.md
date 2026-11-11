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

<p align="center">
  <a href="#project-status">Project status</a> ·
  <a href="#planned-features">Features</a> ·
  <a href="#build-from-source">Build</a> ·
  <a href="#privacy-and-security">Privacy & security</a> ·
  <a href="#contributing">Contribute</a>
</p>

---

## Meet Nuvex

Nuvex VPN is an open-source Android project focused on making privacy tools feel simple: a focused interface, a clear connection state, a choice of locations, and no advertising clutter.

The goal is a thoughtfully designed app that is transparent about how it works. Nuvex is being developed in public, and its source code is available for anyone to inspect, learn from, and improve.

> **Important — early development:** The official WireGuard Android tunnel library and an app-side adapter are now included, but no trusted server profiles or live VPN gateways are configured. The adapter is not yet connected to a production server-discovery flow or the UI. The ten locations shown in the current interface are placeholders, not working servers. Do not rely on this development build to protect internet traffic.

## Project status

| Area | Status |
| --- | --- |
| Native Kotlin Android project | Started |
| Dark navy + neon-blue interface | Initial implementation |
| Location selector UI | Initial implementation; placeholder locations |
| WireGuard tunnel library | Dependency added; adapter groundwork committed |
| Android VPN permission and connection UI flow | Not yet wired end-to-end |
| Live gateway discovery and server profiles | Not configured |
| Automated Android build workflow | Configured; check Actions for the latest result |
| DNS, IPv4/IPv6 leak and reconnect testing | Pending |
| Public production APK release | Not available yet |

## Design principles

- **Privacy first:** no ad SDK or analytics SDK is included in the current project.
- **Clear by default:** the app must never display “connected” unless a real tunnel has been established and verified.
- **Lightweight:** measure release APKs and optimize ABI packaging without removing necessary security components.
- **Open source:** make implementation choices and limitations visible.
- **User control:** clear connection states and an understandable location selector.

## Planned features

These are development goals, not claims that every feature is available today.

- [x] Native Kotlin project scaffold
- [x] Dark navy and neon-blue starter interface
- [x] Initial location selector UI
- [x] MIT license and automated build workflow
- [x] Add the official WireGuard tunnel dependency and initial engine adapter
- [ ] Connect Android VPN permission flow and UI to the engine
- [ ] Build a trusted server-discovery and profile-delivery backend
- [ ] Operate or integrate verified, maintained gateways in supported regions
- [ ] Reliable connect, disconnect, reconnect and failure states
- [ ] Validate DNS and IPv4/IPv6 routing, leak protection and network changes
- [ ] Review gateway ownership, privacy policy and operational security
- [ ] Release a tested APK with checksums and release notes
- [ ] Measure APK size and optimize ABI packaging without compromising security

### Planned locations

The initial UI illustrates ten intended regions: Singapore, India, Japan, Netherlands, Germany, United Kingdom, United States, Canada, France and Australia. They are **not live VPN endpoints**. Availability depends on verified infrastructure and operating costs; a country will only be advertised as available after its gateway has been configured and tested.

## Build from source

### Requirements

- Android Studio with Android SDK Platform 35
- JDK 17
- Gradle 8.9 or a compatible Gradle installation

### Build a debug APK

1. Clone or download this repository.
2. Open the project root in Android Studio and allow Gradle sync to finish.
3. Build the **app** module, or run the following from a terminal with Gradle installed:

   ```bash
   gradle --no-daemon :app:assembleDebug
   ```

4. Find the debug APK at:

   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

The debug APK is for development only. It is not a production VPN and should not be used as a privacy or security guarantee. Check the [Actions tab](https://github.com/Aasimabdul/Nuvex-VPN/actions) for the latest build result.

## Download

A public release APK will be linked here after the app has verified gateways and passes build, connection and security checks. Until then, use the source code to inspect the project or build the development scaffold yourself. **There is no production-ready VPN download yet.**

## Privacy and security

A VPN app is only as trustworthy as its implementation and the people operating its gateways. Before a production release, Nuvex must document its protocol, gateway operators, logging practices, DNS handling, routing behavior and limitations.

- Never enter credentials into unofficial or unverified builds.
- Never treat a location label or UI animation as proof of a secure tunnel.
- Do not ship private gateway keys or credentials inside the APK.
- Security claims will be made only after implementation and testing.
- The gateway provider's privacy policy and logging practices must be reviewed separately from this app's source code.

This project is not yet suitable for protecting sensitive traffic.

## Contributing

Issues, bug reports, design feedback and code contributions are welcome. Please include clear reproduction steps for bugs and avoid posting private keys, credentials, personal data or sensitive network logs.

1. Check existing issues before opening a duplicate.
2. Describe the expected behavior and what actually happened.
3. For security-sensitive findings, avoid publishing exploit details or secrets in a public issue; contact the maintainer privately where possible.

## Roadmap

The priority is correctness before extra features: connect the WireGuard engine to verified gateways, implement the Android connection lifecycle, test routing and leaks, then publish a release with transparent limitations. Visual polish and APK-size optimization come alongside those checks—not instead of them.

## License

Nuvex VPN is released under the [MIT License](LICENSE).

---

<p align="center">
  <strong>Nuvex VPN</strong><br>
  Built in the open. Privacy claims earned through testing.
</p>
