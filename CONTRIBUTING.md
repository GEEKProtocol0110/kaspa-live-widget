# Contributing to Kaspa Live Widget

Thank you for your interest in contributing to Kaspa Live Widget!

## Development Setup

1. **Fork the repository**
2. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/kaspa-live-widget.git
   cd kaspa-live-widget
   ```
3. **Open in Android Studio**
4. **Sync Gradle dependencies**
5. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Project Structure

```
kaspa-live-widget/
├── app/
│   ├── src/main/
│   │   ├── java/com/kaspa/livewidget/
│   │   │   ├── api/           # API services
│   │   │   ├── data/          # Data models
│   │   │   ├── utils/         # Utilities and helpers
│   │   │   ├── widget/        # Widget providers
│   │   │   └── worker/        # Background workers
│   │   ├── res/
│   │   │   ├── drawable/      # Graphics and backgrounds
│   │   │   ├── layout/        # Widget layouts
│   │   │   ├── values/        # Strings and styles
│   │   │   └── xml/           # Widget metadata
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Code Style

- **Language**: Kotlin
- **Naming**:
  - Classes: PascalCase
  - Functions: camelCase
  - Constants: UPPER_SNAKE_CASE
- **Formatting**: Use Android Studio's default Kotlin formatter
- **Documentation**: Add KDoc comments for public APIs

## Making Changes

### Adding New Features

1. Create an issue describing the feature
2. Wait for approval from maintainers
3. Implement the feature
4. Add tests if applicable
5. Update documentation
6. Submit a pull request

### Fixing Bugs

1. Check if an issue exists; create one if not
2. Reference the issue in your commit messages
3. Include steps to reproduce in the PR description

## API Guidelines

### When Adding New Data Sources

- **Use public, read-only APIs only**
- **No authentication required**
- **Respect rate limits**
- **Handle errors gracefully**
- **Cache responses appropriately**

### Example API Integration

```kotlin
suspend fun fetchNewData(): DataType? = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder()
            .url("https://api.example.com/endpoint")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.string()?.let { body ->
                    gson.fromJson(body, DataType::class.java)
                }
            } else null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
```

## Widget Development

### Layout Guidelines

- Support both light and dark themes
- Use sp for text sizes
- Use dp for dimensions
- Test on multiple screen densities
- Keep widgets lightweight

### Update Frequency

- Default: 15 minutes
- Use WorkManager for scheduling
- Respect battery optimization
- Cache data to reduce API calls

## Testing

### Manual Testing

1. Install on device/emulator
2. Add widget to home screen
3. Verify data updates
4. Test network connectivity edge cases
5. Check battery usage

### Test Checklist

- [ ] Widget displays correctly on 2x2
- [ ] Widget displays correctly on 4x2
- [ ] Data updates automatically
- [ ] Cached data is used when appropriate
- [ ] No crashes on network errors
- [ ] Works on API 26+
- [ ] Battery usage is minimal

## Submitting Changes

### Pull Request Process

1. **Update documentation** for any changed behavior
2. **Add or update tests** if applicable
3. **Run code formatter**: `Code > Reformat Code`
4. **Check for lint warnings**: `Analyze > Inspect Code`
5. **Test thoroughly** on device/emulator
6. **Commit changes**:
   ```bash
   git add .
   git commit -m "Brief description of changes"
   ```
7. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```
8. **Create pull request** on GitHub

### Commit Messages

Follow conventional commits:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code formatting
- `refactor:` Code refactoring
- `test:` Adding tests
- `chore:` Maintenance tasks

Examples:
```
feat: add support for network difficulty display
fix: handle null price response gracefully
docs: update README with new widget size
```

## Code Review

- Be respectful and constructive
- Address all review comments
- Update PR based on feedback
- Keep discussions focused on code

## Security

### Reporting Vulnerabilities

**DO NOT** open public issues for security vulnerabilities.

Email security concerns to the repository maintainer.

### Security Guidelines

- Never commit API keys or secrets
- Use HTTPS for all API calls
- Validate and sanitize all external data
- Follow Android security best practices
- Keep dependencies updated

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (see LICENSE file).

## Questions?

- Open an issue for general questions
- Check existing issues and PRs first
- Be patient and respectful

Thank you for contributing! 🎉
