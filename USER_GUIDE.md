# Kaspa Live Widget - User Guide

## Installation

1. Install the APK on your Android device (minimum Android 8.0)
2. The app will not show up in your app drawer as it's a widget-only app

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

## Widget Information

### 2x2 Widget Displays:
- Current time (updates independently of network refreshes and follows your phone’s 12/24-hour preference)
- KAS/USD price
- Current DAA score

### 4x4 Widget Displays:
- Current time
- KAS/USD price
- Current DAA score
- BPS (Blocks Per Second)
- Network hashrate

## Update Frequency

- Network refreshes are scheduled approximately every **15 minutes**; Android may defer them to conserve battery
- Tap the widget to request an update; repeated taps share the unfinished request
- If an attempt cannot fetch data, it keeps cached readings and finishes; the next tap can try again immediately
- The clock continues independently of network refreshes
- Each feed keeps its last good value and successful-fetch timestamp
- A fresh price cannot make an old network reading look fresh, or vice versa
- On each widget update, readings older than 30 minutes are labelled stale; hashrate has its own freshness label on the large widget
- Network connectivity is required for updates

## Data Sources

The widget fetches data from public, read-only APIs:

- **Price Data**: CoinGecko KAS/USD, with KuCoin and Gate KAS/USDT fallback (a USD approximation)
- **Network Data**: Kaspa.org API (https://api.kaspa.org)

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
