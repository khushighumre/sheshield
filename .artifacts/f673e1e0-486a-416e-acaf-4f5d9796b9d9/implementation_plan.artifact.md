# OpenRouter API Integration for Route Safety Summaries

Integrate the OpenRouter API into SheShield's route planning feature to provide AI-powered qualitative safety summaries alongside the existing algorithmic safety scores.

## User Review Required

> [!IMPORTANT]
> The OpenRouter API key provided will be stored securely in `local.properties` and injected via `BuildConfig` to avoid hardcoding credentials directly into source code.

## Open Questions

- Should the AI summary be generated automatically for every route calculation, or upon tapping a "Get AI Safety Insights" button? (Proposed: Generate automatically when a route is analyzed).

## Proposed Changes

### Configuration
#### [MODIFY] [local.properties](file:///C:/Users/Khushi/Desktop/neww/SheShield/local.properties)
- Add `OPENROUTER_API_KEY=sk-or-v1-...`

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/Khushi/Desktop/neww/SheShield/app/build.gradle.kts)
- Configure `buildConfigField` to inject the OpenRouter API key into `BuildConfig`.

### Networking & Data Layer
#### [NEW] [OpenRouterClient.kt](file:///C:/Users/Khushi/Desktop/neww/SheShield/app/src/main/java/com/example/sheshield/data/OpenRouterClient.kt)
- Create a client helper to call OpenRouter's chat completion endpoint (`https://openrouter.ai/api/v1/chat/completions`) using OkHttp/Ktor or standard networking.

#### [MODIFY] [RouteAnalyzer.kt](file:///C:/Users/Khushi/Desktop/neww/SheShield/app/src/main/java/com/example/sheshield/data/RouteAnalyzer.kt)
- Integrate LLM prompt generation based on route safety metrics to fetch a qualitative summary.

## Verification Plan

### Automated Tests
- Run Gradle build (`./gradlew assembleDebug`) to verify compilation and BuildConfig injection.

### Manual Verification
- Test route planning on the emulator and verify that AI safety summaries load correctly.
