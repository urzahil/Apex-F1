# Apex-F1

Apex-F1 is an Android app for following Formula 1 sessions, live timing, championship standings, and historical session data in one place.

## Features

### Live timing
- Follow the current Formula 1 session with continuously refreshed timing data.
- View the running order, driver gaps, lap information, and session status.

### Championship standings
- View the current Drivers' Championship standings.
- View the Constructors' Championship standings.

### Race and session history
- Browse previous Formula 1 events.
- Open individual sessions to inspect historical timing and classification data.
- Supports race, qualifying, sprint, and practice session data.

## Architecture

Apex-F1 is written in Kotlin using modern Android components, including:

- Jetpack Compose for the UI.
- ViewModels and coroutines for UI state and asynchronous work.
- Retrofit for communication with the F1 data API.
- Moshi for JSON parsing.
- Room for local API caching.
- Gradle Kotlin DSL for the build configuration.

The application separates the UI, ViewModel, repository, API, and data-model layers so that network and parsing concerns are kept out of the Compose UI.

## Data source

The app consumes data from an F1 data API exposed through the project's configured API endpoint. The API provides live session status and timing, event calendars, historical session data, and championship standings.

The API key is supplied through the `F1_API_KEY` environment/Gradle configuration and should never be committed to the repository.

> **Security note:** an API key used directly by a mobile application can potentially be extracted from a distributed APK. Keeping the key in GitHub Actions secrets prevents it from being committed to source control, but does not make a client-side key a true secret. For a public distribution architecture, the preferred approach is to keep the upstream API key on a server-side backend.

## Building

### Requirements

- Android Studio with a compatible Android SDK.
- JDK 17.
- Gradle (the project uses the Gradle wrapper).

### Configuration

Create a local `.env` file containing the API key:

```text
F1_API_KEY=your-api-key
```

Do not commit `.env` or any real API key.

The GitHub Actions workflow uses the repository's `F1_API_KEY` secret when building in CI.

### Build from the command line

Run the unit tests:

```bash
./gradlew testDebugUnitTest
```

Build a debug APK:

```bash
./gradlew assembleDebug
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Project structure

```text
app/
└── src/
    ├── main/
    │   └── java/com/example/
    │       ├── data/
    │       │   ├── api/          # Retrofit API client and JSON adapters
    │       │   ├── model/        # API/data models
    │       │   └── repository/   # Data access and caching
    │       └── ui/
    │           ├── live/         # Live timing
    │           ├── standings/    # Championship standings
    │           └── history/      # Historical sessions
    └── test/                     # Unit and Robolectric tests
```

## Status

Apex-F1 is an actively developed personal Android project. API availability and the exact data exposed by historical sessions depend on the configured F1 data backend.

## License

No open-source license has currently been specified for this repository.
