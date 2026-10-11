# Data-safety review notes — owner approval required in Play Console

Source review for v1.4.1 at `5bfe3ecc5f8f0e5fbccd43957aa2936ec1a93ce6`: the app manifest explicitly requests INTERNET and ACCESS_NETWORK_STATE; AndroidX WorkManager contributes background-work permissions to the merged manifest. The acceptance artifacts' `current-apk-metadata.txt` records permissions from the actual public APK, not just the source manifest. Dependencies include OkHttp public feed requests, WorkManager, preferences and market/network caches, with no declared advertising/analytics/account SDKs. Android backup is enabled. Privacy policy: https://www.geekprotocol.xyz/tools/kaspa-widget/privacy/

| Area | Observed behavior | Console review |
| --- | --- | --- |
| Accounts and app access | No login, wallet or restricted screens | No account deletion workflow is needed for this app; app storage can be cleared in Android |
| Settings/cache | Currency, theme, chart, refresh interval and public readings stay in app storage | Disclose Android backup in privacy policy; check backup configuration when answering collection questions |
| Public feed requests | HTTPS to CoinGecko, Kaspa REST, exchange fallbacks and public Kaspa nodes | Providers receive IP address and ordinary request metadata. Review provider retention and Google's collection/sharing definitions before declaring no data collected/shared |
| Location | No location APIs, permission or explicit location field | Do not claim no location collection by a provider unless its IP handling is verified |
| Device identifiers | No advertising ID/device ID is sent explicitly | Review transitive dependencies and provider practices for the final bundle |
| Analytics, advertising, tracking | No in-app analytics/advertising SDK or tracking endpoint | Contains ads: No, based on this version's source |
| Transport | All feed endpoints use HTTPS | Data in transit encrypted for those requests |
| Payments/trading | No purchases, wallet access or trading actions | Free Tools utility; prices are informational |

The developer must review the actual Play form against the final binary and third-party provider practices. Do not automatically select “No data collected” merely because this app has no developer-operated server. GitHub and HQ pages opened in a separate browser have separate policies. These notes are preparation, not legal declarations or a completed submission.
