# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Settings screen for customization
- Multiple theme options
- Additional widget sizes (3x2, 4x4)
- Configurable update intervals
- Price alerts and notifications
- Multi-currency support (EUR, GBP, etc.)

## [1.0.0] - 2026-01-25

### Added
- Initial release of Kaspa Live Widget
- Two widget sizes: 2x2 (compact) and 4x2 (extended)
- Live KAS/USD price display from CoinGecko
- Fallback price sources (KuCoin, Gate.io)
- Network statistics: block height, BPS, hashrate
- Current time display on widget
- Auto-updates every 15 minutes via WorkManager
- Smart caching system to reduce API calls
- Modern dark theme with Kaspa brand colors
- Comprehensive documentation
- CI/CD with GitHub Actions
- Automated release workflow

### Technical Details
- Kotlin-based Android application
- Min SDK: Android 8.0 (API 26)
- Target SDK: Android 14 (API 34)
- Gradle 8.10.2
- Clean architecture with abstracted data sources
- Resilient API handling with automatic fallback

### Security
- Read-only API access
- No personal data collection
- No wallet or private key handling
- HTTPS-only connections

[Unreleased]: https://github.com/GEEKProtocol0110/kaspa-live-widget/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/GEEKProtocol0110/kaspa-live-widget/releases/tag/v1.0.0
