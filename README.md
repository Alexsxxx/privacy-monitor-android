# Privacy Monitor

Android privacy monitoring application that alerts users when their camera or microphone is being accessed by other applications.

## Features

- **Real-time monitoring** of camera and microphone usage
- **Privacy notifications** when apps access your camera/microphone
- **Foreground service** for continuous protection
- **No data collection** - everything runs locally on your device
- **Material Design** UI with custom security-themed icons

## Screenshots

<img src="app/src/main/res/drawable/ic_security_gradient.xml" alt="Privacy Monitor Icon" width="80"/>

## Permissions Required

- **Camera** - To detect when other apps access your camera (no data captured)
- **Microphone** - To detect when other apps access your microphone (no audio recorded)
- **Notifications** - To display privacy alerts
- **Foreground Service** - For continuous monitoring in background

## Privacy Policy

This app does not collect, store, or transmit any personal data. All monitoring happens locally on your device. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for complete details.

## Installation

1. Download the latest `app-release.aab` from the [releases](app/release/) directory
2. Install via Google Play Store (recommended) or sideload the AAB file

## Development

### Requirements
- Android Studio
- Android SDK API 24+
- Kotlin support

### Build Instructions

```bash
# Clone the repository
git clone https://github.com/Alexsxxx/privacy-monitor-android.git
cd privacy-monitor-android

# Build debug APK
./gradlew assembleDebug

# Build release AAB
./gradlew bundleRelease
```

### Project Structure

```
app/
├── src/main/java/com/privacy/monitor/
│   ├── MainActivity.kt           # Main UI and permission handling
│   └── PrivacyMonitorService.kt  # Background monitoring service
├── src/main/res/
│   ├── layout/activity_main.xml  # Main UI layout
│   └── drawable/                 # Icons and graphics
└── release/
    └── app-release.aab          # Production build
```

## Technical Details

- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: Single Activity + Foreground Service
- **No external dependencies** for privacy monitoring

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is open source. Please respect user privacy when contributing.

## Support

For issues or questions, please create an issue in this repository.

---

**Privacy Monitor** - Protecting your digital privacy, one notification at a time. 🛡️