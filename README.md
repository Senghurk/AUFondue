# AU Fondue Android App

Mobile application for reporting and managing maintenance issues across university campuses.

## Features

- Photo-based issue reporting
- Real-time geolocation tagging
- Offline support
- Push notifications
- Dark/Light theme support

## Setup Requirements

- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 34
- Kotlin 1.9.0+
- Min Android API: 24

## Getting Started

1. Clone the repository
```bash
git clone https://github.com/your-organization/au-fondue-android.git
```

2. Add local.properties in project root:
```properties
MAPS_API_KEY=your_google_maps_api_key
BASE_API_URL=your_backend_api_url
```

3. Sync and build project in Android Studio

## Tech Stack

- UI: Jetpack Compose
- DI: Hilt
- Network: Retrofit
- Storage: Room
- Images: Coil
- Location: Google Play Services
- Camera: CameraX

## Building

Debug:
```bash
./gradlew assembleDebug
```

Release:
```bash
./gradlew assembleRelease
```

## Testing
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Project Structure
```
app/
├── data/         # Repositories, data sources
├── domain/       # Business logic
├── presentation/ # UI, ViewModels
├── di/           # Dependency injection
└── utils/        # Utilities
```

## Contributing

1. Fork repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## Contact

- Project Lead: [name@university.edu](mailto:name@university.edu)
- Team: [team@university.edu](mailto:team@university.edu)