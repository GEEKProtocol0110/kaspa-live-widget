# Geek Kaspa Widget for iPhone

A free Scriptable Home Screen widget with the Kaspa media-kit logo, neon glass surface, real 24-hour USD chart, price/change, DAA score, observed average BPS, and hashrate. Small, medium, and large layouts; mint, cyan, violet, and xray themes. No Geek account, wallet, API key, or Apple Developer membership required.

This is a JavaScript script hosted by Scriptable. It is separate from the native iPhone app and does not require TestFlight. **First version: automated data/runtime checks are complete; installation and layout still need a physical iPhone trial.**

## Install on an iPhone

1. Install [Scriptable from the App Store](https://apps.apple.com/us/app/scriptable/id1405459188).
2. Open [Geek-Kaspa-Widget.js](Geek-Kaspa-Widget.js). Use GitHub's **Raw** button and copy the entire script. If downloading fails in an in-app browser, open this page in Safari and copy the raw text instead.
3. Open Scriptable, tap **+**, paste the script, and name it **Geek Kaspa Widget**. Tap the play/run button and choose **Preview medium**. Cancel closes the preview menu without fetching data.
4. Touch and hold an empty area of the iPhone Home Screen. Choose **Edit → Add Widget** (or the **+** button on older iOS), search for **Scriptable**, select the **medium** size, and tap **Add Widget**.
5. Touch and hold the new widget, choose **Edit Widget**, and select **Geek Kaspa Widget** in the **Script** field. Set **When Interacting** to **Run Script**. Close the edit panel.

The same script supports small and large Home Screen widgets. Lock Screen and accessory widgets are not supported. The small layout prioritizes price, DAA score, and BPS; medium and large also show hashrate. The medium/large header shows the time of the last widget render, following the phone's time format; it is not a continuously ticking clock.

To refresh manually, run the script in Scriptable. Tapping the widget with **Run Script** selected opens the preview menu; choose a size to fetch again. iOS controls when the Home Screen widget receives the result, so a successful preview does not guarantee an immediate Home Screen refresh.

## Choose a look

In **Edit Widget → Parameter**, enter a theme name:

| Parameter | Look |
| --- | --- |
| `mint` | Kaspa mint on dark graphite; default |
| `cyan` | Electric blue |
| `violet` | Neon purple |
| `xray` | White on black |

For more options, use semicolon-separated values, for example:

```text
theme=violet;chart=off;refresh=15
```

`chart=on` or `chart=off` controls the background line. `refresh=15`, `30`, or `60` requests the earliest next refresh in minutes. Invalid options fall back safely. Set these separately for each widget. In-app previews use the defaults at the top of the script; change `OPTIONS` there to preview another theme.

## Data and freshness

- CoinGecko supplies the USD price, 24-hour change, and actual price history. KuCoin is a price fallback and is explicitly labeled **KAS/USDT ≈ USD**. Its USDT quote is an approximation, and it supplies no replacement chart.
- Kaspa's public REST API supplies mainnet DAA score, block count, and hashrate. DAA counters are kept as decimal strings to preserve uint64 precision.
- BPS is the average change in observed block count between samples at least 30 seconds and at most six hours apart. It starts at **Sampling…**. A counter rollback or unusually large rate starts sampling again. It is not a hard-coded target or an instantaneous rate.
- Each feed retains its own last successful reading and source timestamp. Readings older than 30 minutes show **Stale** with their age. A feed outage does not relabel cached data as current. These ages are evaluated when the widget script renders, not continuously between iOS refreshes.
- Valid chart history is fetched at most once an hour. After an outage, the last real series may remain for up to 24 hours after its fetch; it is labeled **cached** if its final point is more than one hour old. Missing/expired history shows **unavailable**, and no invented line is drawn.
- iOS can defer the requested refresh, especially with low battery or infrequent widget use. This follows [Scriptable's refresh behavior](https://docs.scriptable.app/listwidget/#refreshafterdate).

Only public read-only HTTPS market/network endpoints are contacted. The logo is embedded. The script stores readings in `geek-kaspa-cache-v1.json` inside Scriptable's local documents; it does not read wallets, photos, contacts, or location. To reset readings, delete that cache file in Scriptable's file browser and run again. BPS will begin sampling again.

## Troubleshooting

- **Blank or “Select script” widget:** edit it and select the script. Ensure you copied the entire file, including the final `await run();` line.
- **Download never finishes:** use Safari, GitHub's Raw view, and copy/paste instead of downloading a file.
- **Unavailable/stale data:** public feeds may be offline or rate-limited. Wait and run again; cached readings remain labeled. Multiple widgets share the data cache.
- **Sampling… BPS:** run once, wait at least 30 seconds, then run again. A six-hour gap or a counter rollback requires a new pair of samples.
- **Wrong theme in the preview:** the Home Screen parameter applies to that widget; the in-app preview uses `OPTIONS`.
- **Text looks cramped:** start with medium size. Home Screen tinting and larger accessibility text still need physical-device verification; choose normal/full-color widget appearance for the intended neon colors.

## Validation

Run `node --test scriptable/widget.test.cjs` from the repository root. No npm dependencies are needed. Tests exercise feed validation, uint64 counters, sample intervals, partial/offline caches, malformed responses, all layouts/themes, and the actual script entry point through a Scriptable API adapter. The adapter checks calls and data flow; it cannot prove native iOS typography, widget placement, or refresh scheduling. Complete [DEVICE_TRIAL.md](DEVICE_TRIAL.md) on a real iPhone before advertising this as device-verified.

CI also runs `node scriptable/check-live-feeds.cjs` to record actual public-feed compatibility in a downloadable JSON artifact. Read its per-feed `accepted` values; a green test job alone does not prove every external feed was reachable. Public-feed outages are reported separately from deterministic regression failures.

See [Scriptable's widget docs](https://docs.scriptable.app/listwidget/) for platform behavior and [BRAND_ASSETS.md](../BRAND_ASSETS.md) for the Kaspa mark attribution. Code is licensed under the repository's MIT license.
