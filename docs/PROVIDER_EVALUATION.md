# VPN provider evaluation

Research date: 2026-11-11. This is a preliminary public-documentation review, not approval to use any provider.

## Summary

| Candidate | Public integration evidence | Commercial status | Preliminary result |
| --- | --- | --- | --- |
| Vox VPN | Partner REST API documents locations, user lifecycle, fresh WireGuard/AmneziaWG configs, usage and traffic limits. https://vox-vpn.com/partner-api | Partner API key required; pricing is quoted per active account. | Best technical pilot candidate because its documented config format matches the current WireGuard engine. Not approved/configured until partner terms, price, and keys are obtained. |
| KeepSolid VPN Unlimited OEM | Official OEM API and Android SDK documentation; server discovery and account/session APIs. https://oem.vpnunlimited.com/sdk-api | Requires OEM application credentials and commercial arrangement; pricing depends on program. | Viable alternative if Android SDK/API terms and protocol integration meet requirements. No credentials or approval currently configured. |
| WLVPN | Official reseller API and Android SDK; API access requires reseller account and API key. https://docs.wlvpn.com/ and https://wlvpn.com/wlvpn-faqs/ | Reseller agreement, NDA/staging access, and commercial pricing/minimums may apply. | Viable alternative subject to agreement, SDK access, pricing and free-tier economics. |

## Recommendation

Request a small pilot quote from Vox first, while requesting written quotes from KeepSolid OEM and WLVPN for comparison. The free-to-users business model does not mean provider infrastructure is free. Do not promise unlimited or permanently free service until the cost per active user and bandwidth terms are confirmed.

## Required partner questions

1. Is a fully free, ad-free Android app allowed under the agreement?
2. What is the monthly cost per active account, data allowance, and overage price?
3. Are Singapore and India available for the pilot, and what are the real supported protocols?
4. Does the agreement permit automatic account creation, suspend/resume, and config retrieval through our backend?
5. Can the returned WireGuard config be used with the official WireGuard Android tunnel library?
6. What user/device identifiers and personal data must be supplied?
7. What are the API rate limits, SLA, support response times, and data retention rules?
8. Are there minimum commitments, company registration, tax, or payment requirements?

## Security boundary

Provider API keys belong only in backend environment secrets. Android should call the Nuvex backend over HTTPS. The backend must validate user/device eligibility and request fresh credentials from the provider immediately before connection. Never commit real keys or generated private keys.

## Current implementation status

- Provider-neutral Android interfaces: scaffolded.
- Ktor backend health route: scaffolded.
- PostgreSQL initial schema: added.
- Real provider adapter: not implemented; requires provider selection, written authorization, API credentials and reviewed API contract.
- Session/authentication/quota enforcement, persistent database integration, production deployment and tunnel verification: not complete.
