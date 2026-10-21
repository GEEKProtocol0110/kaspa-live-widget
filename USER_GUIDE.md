# Kaspa Live Widget - User Guide

## Installation

1. Install the APK on your Android device (minimum Android 8.0)
2. Open **Kaspa Live Widget** in your app drawer for installation help and settings

## Preview and save your widget

1. Choose **Compact** or **Expanded** in the app's preview size menu.
2. Choose appearance, currency, refresh interval and chart visibility. The preview uses saved readings for that currency; unavailable data stays unavailable.
3. Tap **Apply settings** to save for all widgets, or **Save & add compact/expanded** to save and ask your launcher to add it.
4. **Refresh saved settings** refreshes the saved currency. The button waits while a request is queued/running. Unsaved preview choices do not request another currency's feed.

Your unsaved choices survive rotation. **Install help & data sources** explains launcher fallback and feed sources.

## Adding the Widget to Your Home Screen

### For 2x2 Widget (Compact View)
1. Long-press on an empty area of your home screen
2. Tap on "Widgets"
3. Scroll to find "Kaspa 2x2"
4. Long-press and drag it to your desired location
5. Release to place

### For 4x4 Widget (Extended View)
1. Long-press on an empty area of your home screen
2. Tap on "Widgets"
3. Scroll to find "Kaspa 4x4"
4. Long-press and drag it to your desired location
5. Release to place

## Settings, refresh and chart

Open the app or tap **Setup** on a widget. Choose currency, refresh interval, dark/light appearance and an optional chart. **Chart time range** offers **1H, 24H, 7D and 30D**, then tap **Apply settings**. Use **Save & add compact** or **Save & add expanded** to save choices and request a widget, or use your launcher’s Widgets menu. Choices apply to all your Kaspa widgets.

Tap the widget or **Refresh now** to see queued/running/completed status. Partial and failed updates keep cached readings with their original times. Switching currency shows that currency’s own cache or a placeholder while loading; it never relabels a dollar price as euros.

The chart uses actual CoinGecko observations for the selected range. The 1H range selects the past hour from the daily feed; 7D and 30D load their own series. Chart range and the 15/30/60-minute background refresh are independent. The percentage beside the price always shows the 24-hour change. Switching ranges keeps a separate saved history for each currency and range; a range you have not loaded yet shows unavailable until a successful refresh. Its neon line appears behind the frosted-glass panels on both widget sizes, with a separate preview in the app. Unavailable series show a message. Cached charts older than an hour show stale. Price/network readings older than 30 minutes show stale even when you select a 60-minute refresh interval.

Use **App updates** to check for a new version. In the signed direct-download app, tap **Update now**, wait for verification, then **Install update**. Android asks you to approve installation and may require a one-time Allow from this source permission. Daily checks can be turned off on that screen. The updater never downloads or installs a release just because it detects one. Existing older apps need one installation of v1.6.0 to gain this feature.

Use **Downloads and release notes** to open GitHub Releases. Public signed releases require the owner’s one-time setup in [SIGNED_RELEASES.md](SIGNED_RELEASES.md). After the first signed release, newer release APKs update the existing app and preserve settings. Debug CI builds are a separate test app.

## Widget Information

### 2x2 Widget Displays:
- Current time (updates independently of network refreshes and follows your phone’s 12/24-hour preference)
- KAS price in your selected USD/EUR/GBP currency, plus real 24-hour change
- Current DAA score

### 4x4 Widget Displays:
- Current time
- KAS price in your selected USD/EUR/GBP currency, plus real 24-hour change
- Current DAA score
- BPS (Blocks Per Second)
- Network hashrate

## Update Frequency

- Network refreshes are scheduled at your selected **15, 30 or 60 minute** interval; Android may defer them to conserve battery
- Tap the widget to request an update; repeated taps share the unfinished request
- If an attempt cannot fetch data, it keeps cached readings and finishes; the next tap can try again immediately
- The clock continues independently of network refreshes
- Each feed keeps its last good value and successful-fetch timestamp
- A fresh price cannot make an old network reading look fresh, or vice versa
- On each widget update, readings older than 30 minutes are labelled stale; hashrate has its own freshness label on the large widget
- Network connectivity is required for updates

## Data Sources

The widget fetches data from public, read-only APIs:

- **Price Data**: CoinGecko USD/EUR/GBP. KuCoin and Gate KAS/USDT provide a USD-only approximation during outages. A source without percentage data displays “24h change unavailable”.
- **Network Data**: Kaspa REST (https://api.kaspa.org), with a synced-mainnet public node fallback for missing feeds.

## Troubleshooting

### Widget Not Updating
- Check your internet connection
- Tap the widget to request a refresh; previously loaded readings remain visible during outages
- Check the freshness labels before relying on a reading
- If necessary, remove and re-add the widget
- Restart your device

### Widget Shows "--" or "N/A"
- This indicates no data is available yet
- Wait a few minutes for the first update
- Check your internet connection

### Battery Optimization
- The widget uses WorkManager which is battery-efficient
- Updates are scheduled intelligently by Android
- No manual battery optimization needed

## Privacy

- **No personal data collected**: The widget only fetches public Kaspa network data
- **No wallet access**: Does not interact with any Kaspa wallets
- **No private keys**: Does not store or transmit any sensitive information
- **Read-only**: Only reads public data, never writes or modifies anything

## Permissions

The widget requires only:
- **Internet**: To fetch Kaspa network data
- **Network State**: To check if internet is available

## Removing the Widget

1. Long-press on the widget
2. Drag to "Remove" or tap the remove icon
3. Release to remove

When all widgets are removed, automatic updates are stopped to save battery.

## Support

For issues or questions, please visit:
https://github.com/GEEKProtocol0110/kaspa-live-widget
