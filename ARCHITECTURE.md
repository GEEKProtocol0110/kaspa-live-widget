# Architecture

Android 8+ native home-screen widgets, Kotlin, RemoteViews and WorkManager. Both providers use one renderer; the launcher advances the TextClock independently of network calls. The app does not access wallets or submit transactions.

## Components

| Component | Responsibility |
| --- | --- |
| `SettingsActivity` | Widget pin requests, manual installation help, shared preferences, current reading/chart preview and release page |
| `WidgetSettings` | Validated USD/EUR/GBP, 15/30/60-minute interval, dark/light theme and chart preference |
| `WorkScheduler` | Unique periodic work with UPDATE policy; manual KEEP work; settings changes append a refresh so an unfinished old-currency request cannot suppress the new choice |
| `WidgetUpdateWorker` | Mutex around cache read/fetch/merge/save, parallel market/network calls, progress/result status and widget rendering |
| `KaspaApiService` | Public REST network and hashrate calls |
| `PublicNodeFallback` | Short validated mainnet JSON-wRPC session for missing REST feeds |
| `MarketDataSource` / `MarketParser` | Selected-currency quote, real 24-hour change/provider timestamp and validated historical prices |
| `NetworkDataMerger` / `MarketDataMerger` | Retain failed feeds without renewing their timestamps; reject old/cross-currency quotes |
| `DataCache` / `MarketCache` | SharedPreferences snapshots; separate currency keys and safe legacy USD migration |
| `RefreshStatusCache` | Queued/running/complete/partial/unavailable/interrupted status |
| `WidgetRenderer` / `MarketDisplay` / `PriceChart` | Both sizes, responsive chart visibility, honest unavailable/stale labels and Canvas drawing of recorded points |

## Refresh and cache rules

Manual taps share unfinished work. Settings changes append work rather than cancelling an active fetch. Periodic interval changes use UPDATE. A process-local mutex prevents periodic/manual refreshes from overwriting each other’s cache snapshots. Failed attempts finish instead of occupying manual KEEP work through a retry backoff. Removing the last widget cancels periodic and manual work; the app can still request an explicit refresh.

Prices, network counts and hashrate have separate last-good timestamps. Market quotes keep the provider’s `last_updated_at`; USD exchange quotes use their fetch time. A currency switch uses only that currency’s cache. Exchange fallback never populates EUR/GBP. A price-only quote clears a previous change percentage. Network source/node changes reset observed BPS so counts from different nodes are not subtracted. RPC block counts are node-local and may fall during pruning; a decreasing count produces no BPS reading.

Price/network freshness is 30 minutes. A chart is stale when its last point is older than an hour. Successful history fetches are reused for one hour; missing history can be retried during later refreshes. Chart parsing requires positive finite prices, ordered/deduplicated timestamps, a recent final point and at least 22 hours of coverage. It never synthesizes a missing price series. The expanded widget shows the chart only at a minimum height of 380dp; the app always offers the enabled chart preview.

## Public data sources

CoinGecko `/api/v3/simple/price` requests the selected currency, `include_24hr_change=true` and `include_last_updated_at=true`. `/coins/kaspa/market_chart?vs_currency=...&days=1` supplies historical `[Unix milliseconds, price]` pairs. USD-only fallbacks are public KuCoin and Gate KAS/USDT tickers; the UI identifies these as an approximation. All calls have timeouts and failures preserve the last good data.

Primary network endpoints are `https://api.kaspa.org/info/blockdag` and `/info/hashrate` (TH/s). Missing feeds are filled without replacing a healthy primary reading. The fallback uses official public resolver groups (`kaspa.stream`, `kaspa.red`, `kaspa.blue`) and requires an allowlisted WSS host and exact mainnet proxy path. Resolver records currently expose Borsh paths; the documented proxy layout also exposes the corresponding `/wrpc/json` path. That convention is not a protocol guarantee: unavailable JSON endpoints fail safely and another resolver can be tried.

Before accepting a node, `getServerInfo` must identify a synced mainnet server with RPC API version 1. Only `getBlockDagInfo` and `estimateNetworkHashesPerSecond` follow. RPC hashrate is H/s, converted to the existing TH/s formatter. Requests correlate IDs/methods, responses are bounded and the socket closes on completion/cancellation. The fallback has a 24-second overall coroutine budget, 10-second node budget and 6-second HTTP call timeout. Blocking HTTP cancellation can finish at its call timeout. No authentication, subscriptions or wallet calls are used.

Upstream references:

- JSON proxy paths: https://kaspa.aspectron.org/rpc/proxies.html
- Resolver configuration/protocol: https://github.com/kaspanet/rusty-kaspa/tree/master/rpc/wrpc/client
- RPC messages and units: https://github.com/kaspanet/rusty-kaspa/blob/master/rpc/core/src/model/message.rs
- CoinGecko quote/history: https://docs.coingecko.com/reference/simple-price and https://docs.coingecko.com/reference/coins-id-market-chart

## Verification and releases

`./gradlew build` assembles debug/release, runs unit tests for both variants and Android lint. Robolectric uses native graphics on API 26 and 34 to check clocks, layout bounds, currency/theme/progress/chart rendering and the app setup screen. MockWebServer covers the actual wRPC response envelope and rejects testnet before additional calls. Market tests cover rate-limit fallback, currency isolation, provider ages and invalid/short/stale history.

Release signing uses a persistent owner-held keystore supplied through Actions secrets. Tagged main commits must match the app version, pass the full build, match the pinned certificate and, after the first public release, use the same certificate and a larger version code. Assets use a stable APK filename and checksum. See [SIGNED_RELEASES.md](SIGNED_RELEASES.md). No production signing key is stored in this repository.
