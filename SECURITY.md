# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| Latest  | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability within Kaspa Live Widget, please send an email to the repository owner or create a private security advisory on GitHub.

**Please do not report security vulnerabilities through public GitHub issues.**

### What to Include

- Type of vulnerability
- Full paths of source file(s) related to the vulnerability
- Location of the affected source code (tag/branch/commit or direct URL)
- Any special configuration required to reproduce the issue
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit it

### Response Timeline

- We will acknowledge your email within 48 hours
- We will provide a detailed response within 7 days
- We will work on a fix and keep you updated on progress
- Once fixed, we will release a security advisory

## Security Best Practices

This app follows these security principles:

- ✅ Read-only API access
- ✅ No personal data collection
- ✅ No wallet integration
- ✅ No private key handling
- ✅ All API calls use HTTPS
- ✅ No local storage of sensitive data

## Permissions

The app only requires:
- `INTERNET` - To fetch Kaspa network data
- No other permissions are requested

## Third-Party Services

The app connects to:
- CoinGecko API (price data)
- KuCoin API (fallback price data)
- Gate.io API (fallback price data)
- Kaspa API (network data)

All connections use HTTPS and are read-only.
