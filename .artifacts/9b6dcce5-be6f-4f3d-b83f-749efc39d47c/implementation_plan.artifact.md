# Implementation Plan - Stage 1: Security and Authentication Hardening

This plan outlines the implementation steps for hardening Supabase authentication, adding cryptographic P2P message signatures via Android KeyStore, optimizing WAL thread safety and wake-lock management, and adding robust unit tests.

## User Review Required

> [!IMPORTANT]
> - **Supabase Auth Integration**: We will implement email/password sign-up, login, session refresh, and token storage. A new Login/Signup screen will be added to the navigation flow.
> - **P2P Message Signatures**: Outgoing P2P messages will be signed using an Android KeyStore-backed EC/RSA keypair. Incoming messages from unknown peers will be verified against their public key and placed in an untrusted/pending verification queue if not pre-trusted.
> - **WAL Thread Safety**: WAL recovery will be moved off the main thread to prevent UI stutter/ANR during app startup.

## Proposed Changes

### Part A: Supabase Authentication
#### [NEW] [SupabaseAuthManager.kt](file:///C:/Users/Khushi/Desktop/All Projects/Hackathon/sheshield/app/src/main/java/com/example/sheshield/data/supabase/SupabaseAuthManager.kt)
- Manages GoTrue REST calls for sign-up, login (`/auth/v1/token?grant_type=password`), token refresh (`/auth/v1/token?grant_type=refresh_token`), and logout. Securely stores tokens using encrypted shared preferences / Keystore.
#### [NEW] [AuthScreen.kt](file:///C:/Users/Khushi/Desktop/All Projects/Hackathon/sheshield/app/src/main/java/com/example/sheshield/ui/AuthScreen.kt)
- Jetpack Compose UI for Login and Signup with validation and error handling.
#### [MODIFY] [Navigation.kt](file:///C:/Users/Khushi/Desktop/All Projects/Hackathon/sheshield/app/src/main/java/com/example/sheshield/Navigation.kt)
- Add login/signup routes and account button on HomeScreen.
#### [MODIFY] [CloudRepository.kt](file:///C:/Users/Khushi/Desktop/All Projects/Hackathon/sheshield/app/src/main/java/com/example/sheshield/data/supabase/CloudRepository.kt)
- Fetch active JWT token, refresh if expired, and include in Authorization headers.

### Part B: P2P Message Authenticity
#### [MODIFY] [Crypto.kt](file:///C:/Users/Khushi/Desktop/All Projects/Hackathon/sheshield/app/src/main/java/com/example/sheshield/data/Crypto.kt)
- Add Android KeyStore signing key generation, `sign(data)`, and `verify(data, signature, publicKey)`.
#### [MODIFY] [P2pMessage.kt](file:///C:/Users/Khushi/Desktop/All Projects/Hackathon/sheshield/app/src/main/java/com/example/sheshield/data/p2p/P2pMessage.kt)
- Add `senderPublicKey` and `signature` fields. Add canonical payload serialization for signing.
#### [NEW] [PeerTrustStore.kt](file:///C:/Users/Khushi/Desktop/All Projects/Hackathon/sheshield/app/src/main/java/com/example/sheshield/data/p2p/PeerTrustStore.kt)
- Manages trusted sender public keys and pending/untrusted incoming messages.
#### [MODIFY] [NearbyManager.kt](file:///C:/Users/Khushi/Desktop/All Projects/Hackathon/sheshield/app/src/main/java/com/example/sheshield/service/NearbyManager.kt)
- Sign outgoing messages and verify incoming message signatures before processing/forwarding.

### Part C: WAL Performance & WakeLock
#### [MODIFY] [WalLog.kt](file:///C:/Users/Khushi/Desktop/All Projects/Hackathon/sheshield/app/src/main/java/com/example/sheshield/data/p2p/WalLog.kt) & [MessageQueueManager.kt](file:///C:/Users/Khushi/Desktop/All Projects/Hackathon/sheshield/app/src/main/java/com/example/sheshield/data/p2p/MessageQueueManager.kt)
- Move recovery off main thread.
#### [MODIFY] [SosService.kt](file:///C:/Users/Khushi/Desktop/All Projects/Hackathon/sheshield/app/src/main/java/com/example/sheshield/service/SosService.kt)
- Ensure wake-lock is safely released across all exit paths.

### Part D: Automated Tests
#### [NEW] [P2pSecurityTest.kt](file:///C:/Users/Khushi/Desktop/All Projects/Hackathon/sheshield/app/src/test/java/com/example/sheshield/P2pSecurityTest.kt)
- Unit tests for HLC, TTL, hopCount, signature verification, duplicate detection, and WAL recovery.

## Verification Plan

### Automated Tests
- Run `./gradlew test` to execute all unit tests.
- Run `./gradlew assembleDebug` to verify project compilation.
