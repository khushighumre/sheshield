# Task Checklist - Stage 1: Security and Authentication Hardening

- `[x]` Part A: Supabase Authentication
  - `[x]` Create `SupabaseAuthManager.kt` for GoTrue login, signup, refresh, and logout
  - `[x]` Create `AuthScreen.kt` for Jetpack Compose login/signup flow
  - `[x]` Update `Navigation.kt` to integrate auth routes and account management
  - `[x]` Update `CloudRepository.kt` to use active and refreshed JWT tokens
- `[x]` Part B: P2P Message Authenticity & Key Management
  - `[x]` Update `Crypto.kt` to support Android KeyStore signing and verification
  - `[x]` Update `P2pMessage.kt` to include public key and digital signature
  - `[x]` Update `NearbyManager.kt` to sign outgoing messages and verify incoming signatures
- `[x]` Part C: WAL Performance & WakeLock
  - `[x]` Update `WalLog.kt` and `MessageQueueManager.kt` for asynchronous WAL recovery off main thread
  - `[x]` Update `SosService.kt` for robust wake lock lifecycle management
- `[x]` Part D: Automated Tests
  - `[x]` Create `P2pSecurityTest.kt` covering HLC, TTL, hopCount, signature verification, and WAL recovery
- `[x]` Part E: Build & Verification
  - `[x]` Run Gradle tests and assembleDebug
