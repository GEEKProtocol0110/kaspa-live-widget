# Kaspa Live Widget - Technical Architecture

## Overview

Kaspa Live Widget is an Android home screen widget application that displays live Kaspa network data including price, block height, BPS (blocks per second), and network hashrate.

## Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Android System                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Home Screen (Launcher)                    │  │
│  │  ┌──────────────┐         ┌──────────────┐            │  │
│  │  │  2x2 Widget  │         │  4x2 Widget  │            │  │
│  │  └──────┬───────┘         └──────┬───────┘            │  │
│  └─────────┼────────────────────────┼────────────────────┘  │
│            │                        │                        │
│  ┌─────────┴────────────────────────┴────────────────────┐  │
│  │           Widget Providers Layer                       │  │
│  │  ┌──────────────────────┐  ┌──────────────────────┐   │  │
│  │  │ KaspaWidgetProvider  │  │KaspaWidgetLarge...   │   │  │
│  │  └──────────┬───────────┘  └──────────┬───────────┘   │  │
│  └─────────────┼───────────────────────────┼──────────────┘  │
│                │                           │                 │
│  ┌─────────────┴───────────────────────────┴──────────────┐  │
│  │              Business Logic Layer                       │  │
│  │  ┌──────────────────┐  ┌──────────────────┐            │  │
│  │  │  DataCache       │  │  WorkScheduler   │            │  │
│  │  └──────────────────┘  └────────┬─────────┘            │  │
│  │                                 │                       │  │
│  │                      ┌──────────┴─────────┐             │  │
│  │                      │ WidgetUpdateWorker │             │  │
│  │                      └──────────┬─────────┘             │  │
│  └─────────────────────────────────┼──────────────────────┘  │
│                                    │                         │
│  ┌─────────────────────────────────┴──────────────────────┐  │
│  │                Data Access Layer                        │  │
│  │  ┌──────────────────────────────────────────────────┐   │  │
│  │  │            KaspaApiService                       │   │  │
│  │  └──────────────────────────────────────────────────┘   │  │
│  └─────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ HTTPS
                         ▼
┌────────────────────────────────────────────────────────────┐
│                    External APIs                           │
│  ┌──────────────────┐          ┌──────────────────┐        │
│  │  CoinGecko API   │          │   Kaspa API      │        │
│  │  (Price Data)    │          │  (Network Data)  │        │
│  └──────────────────┘          └──────────────────┘        │
└────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. Widget Providers

#### KaspaWidgetProvider (2x2)
- **Purpose**: Manages the compact 2x2 widget
- **Displays**: Time, Price, Block Height
- **Lifecycle**:
  - `onUpdate()`: Called when widget needs updating
  - `onEnabled()`: First widget added - schedules updates
  - `onDisabled()`: Last widget removed - cancels updates

#### KaspaWidgetLargeProvider (4x2)
- **Purpose**: Manages the extended 4x2 widget
- **Displays**: Time, Price, Block Height, BPS, Hashrate
- **Lifecycle**: Same as 2x2 widget

### 2. Background Processing

#### WidgetUpdateWorker
- **Type**: CoroutineWorker (WorkManager)
- **Schedule**: Periodic, every 15 minutes
- **Tasks**:
  1. Fetch latest data from APIs
  2. Save to cache
  3. Update all widget instances
- **Error Handling**: Retries on failure

#### WorkScheduler
- **Purpose**: Manages WorkManager scheduling
- **Methods**:
  - `scheduleWidgetUpdates()`: Enqueues periodic work
  - `cancelWidgetUpdates()`: Cancels scheduled work
- **Policy**: KEEP - maintains existing schedule

### 3. Data Layer

#### PriceDataSource Interface
- **Purpose**: Abstraction for price data providers
- **Implementations**:
  - `CoinGeckoPriceDataSource`: Primary source (CoinGecko API)
  - `KaspaExchangePriceDataSource`: Fallback source
- **Fallback Strategy**: Tries each source in order until one succeeds
- **Benefits**: 
  - Resilience against rate limiting
  - No hard dependency on single provider
  - Easy to add new sources

#### KaspaApiService
- **Purpose**: Fetches data from public APIs with fallback handling
- **APIs Used**:
  - Price: Multiple sources via PriceDataSource interface
    - Primary: CoinGecko `https://api.coingecko.com/api/v3/simple/price`
    - Fallback: Kaspa exchange APIs
  - Kaspa Network: `https://api.kaspa.org/info/blockdag`
  - Kaspa Hashrate: `https://api.kaspa.org/info/hashrate`
- **Technology**: OkHttp + Gson
- **Error Handling**: 
  - Try-catch with null returns
  - Automatic fallback to secondary price sources
  - Rate limit detection (HTTP 429)

#### DataCache
- **Purpose**: Caches network data locally
- **Storage**: SharedPreferences
- **Cache Validity**: 14 minutes
- **Methods**:
  - `saveData()`: Stores network data
  - `getCachedData()`: Retrieves cached data
  - `isCacheValid()`: Checks if cache is still fresh
  - `getValidCachedData()`: Returns data only if valid

### 4. Data Models

#### KaspaNetworkData
```kotlin
data class KaspaNetworkData(
    val price: Double = 0.0,
    val blockHeight: Long = 0L,
    val bps: Double = 0.0,
    val hashrate: String = "N/A",
    val timestamp: Long = System.currentTimeMillis()
)
```

## Update Flow

### Initial Widget Addition

```
User adds widget to home screen
    ↓
onEnabled() called
    ↓
WorkScheduler.scheduleWidgetUpdates()
    ↓
onUpdate() called immediately
    ↓
Check cache for data
    ↓
Display data (or defaults if none)
    ↓
Wait for WorkManager to trigger first update
```

### Periodic Updates

```
WorkManager triggers (every 15 min)
    ↓
WidgetUpdateWorker.doWork()
    ↓
KaspaApiService.fetchAllData()
    ├── fetchPrice() → CoinGecko API
    ├── fetchNetworkInfo() → Kaspa API
    └── fetchHashrate() → Kaspa API
    ↓
DataCache.saveData()
    ↓
Update all widget instances
    ├── KaspaWidgetProvider.updateAllWidgets()
    └── KaspaWidgetLargeProvider.updateAllWidgets()
    ↓
RemoteViews updated
    ↓
AppWidgetManager.updateAppWidget()
    ↓
Widget display refreshed on home screen
```

## Threading Model

### Main Thread
- Widget UI updates (RemoteViews)
- AppWidgetProvider callbacks

### Background Threads
- Network requests (Dispatchers.IO)
- WorkManager workers
- Cache operations

### Coroutines
- API calls use `suspend` functions
- `withContext(Dispatchers.IO)` for network operations
- Structured concurrency for safe cancellation

## Data Format & Display

### Price Display
- Format: `$X.XXXX` (4 decimal places)
- Fallback: `$--` (no data)
- Color: #00D4AA (Kaspa green)

### Block Height Display
- Format: `XXX,XXX` (comma-separated)
- Fallback: `--` (no data)
- Source: virtualDaaScore from network info

### BPS Display
- Format: `X.X BPS`
- Default: 1.0 (Kaspa's average)
- Note: Can be enhanced with historical calculation

### Hashrate Display
- Format: Auto-scaled (PH/s, TH/s, GH/s, MH/s)
- Fallback: `N/A` (no data)
- Example: `156.23 PH/s`

## Caching Strategy

### Why Cache?
1. Reduce API calls (rate limiting)
2. Improve responsiveness
3. Offline fallback
4. Battery optimization

### Cache Invalidation
- Time-based: 14 minutes
- Shorter than update interval (15 min)
- Ensures data freshness

### Cache Miss Handling
- Display default values
- Don't block UI
- Wait for next update

## Battery Optimization

### WorkManager Benefits
- System-managed scheduling
- Doze mode compatible
- Battery-aware execution
- Automatic retry with backoff

### Update Frequency
- 15 minutes: Good balance
- Not too frequent (battery drain)
- Not too infrequent (stale data)

### Network Efficiency
- Single batch API call
- Connection pooling (OkHttp)
- Timeout limits (10 seconds)
- No polling or long-running connections

## Error Handling

### Network Errors
- Try-catch in all API calls
- Return null on failure
- Display cached data
- Retry on next schedule

### API Errors
- HTTP status checks
- JSON parsing errors
- Fallback to default values

### Widget Lifecycle
- Handle missing context
- Null safety throughout
- Default values for missing data

## Security Considerations

### API Security
- ✅ HTTPS only
- ✅ Read-only endpoints
- ✅ No authentication required
- ✅ Public data only

### Data Security
- ✅ No sensitive data stored
- ✅ No private keys
- ✅ No wallet functionality
- ✅ No user tracking

### Permissions
- ✅ Internet: Required for API calls
- ✅ Network State: Check connectivity
- ✅ No other permissions needed

## Testing Strategy

### Manual Testing
1. Add widget to home screen
2. Verify initial display
3. Wait 15 minutes for update
4. Check data accuracy
5. Test network error scenarios
6. Verify battery usage

### Device Testing
- Multiple Android versions (8.0+)
- Various screen densities
- Different launchers
- Battery optimization settings

## Future Enhancements

### Potential Features
1. **User Configuration**
   - Adjustable update interval
   - Custom color themes
   - Data source selection

2. **Additional Data**
   - Price change percentage
   - 24h high/low
   - Market cap
   - Circulating supply

3. **Advanced Caching**
   - Historical block data for BPS calculation
   - Price trend tracking
   - Offline mode improvements

4. **Widget Variants**
   - 1x1 minimal widget
   - 4x4 detailed widget
   - Lock screen widget (Android 13+)

5. **Performance**
   - GraphQL for optimized queries
   - WebSocket for near real-time updates
   - Image caching for icons

## Dependencies

### Production
- `androidx.core:core-ktx:1.12.0` - Kotlin extensions
- `androidx.appcompat:appcompat:1.6.1` - Backward compatibility
- `androidx.work:work-runtime-ktx:2.9.0` - Background scheduling
- `com.squareup.okhttp3:okhttp:4.11.0` - HTTP client
- `com.google.code.gson:gson:2.10.1` - JSON parsing
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3` - Async operations

### Build
- `com.android.tools.build:gradle:8.1.0` - Android Gradle plugin
- `org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0` - Kotlin plugin

## Performance Metrics

### Network Usage
- ~5-10 KB per update
- 96 updates per day (every 15 min)
- ~480 KB - 1 MB daily

### Battery Impact
- Minimal (WorkManager optimized)
- Background work: <1% daily
- No foreground services
- No wakelocks

### Storage
- APK Size: ~500 KB - 1 MB
- Cache Size: <10 KB
- Total: <2 MB

## Conclusion

The Kaspa Live Widget is designed with simplicity, efficiency, and user privacy in mind. It provides essential Kaspa network information at a glance while maintaining minimal resource usage and respecting user privacy.
