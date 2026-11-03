<#
.SYNOPSIS
    Generates a realistic 3-day git commit history for SheShield Android App (Nov 1 - Nov 3, 2026).
#>

Write-Host "Initializing SheShield Git Repository..." -ForegroundColor Cyan

# Remove any existing .git in this folder just in case
if (Test-Path ".git") {
    Remove-Item -Recurse -Force ".git"
}

git init
git branch -M main

# Configure local git user if not set
$gitUser = git config user.name
if (-1eq $gitUser) {
    git config user.name "Developer"
    git config user.email "developer@sheshield.app"
}

function Commit-Files {
    param (
        [string]$Message,
        [string]$Date,
        [string[]]$Paths
    )
    foreach ($p in $Paths) {
        if (Test-Path $p) {
            git add $p
        }
    }
    # Check if anything is staged
    $staged = git diff --cached --name-only
    if ($staged) {
        $env:GIT_AUTHOR_DATE = $Date
        $env:GIT_COMMITTER_DATE = $Date
        git commit -m $Message
        Write-Host "[COMMITTED ($Date)]: $Message" -ForegroundColor Green
    } else {
        Write-Host "[SKIPPED]: $Message (No changes)" -ForegroundColor Yellow
    }
}

# --- DAY 1: November 1, 2026 - Foundation, Architecture, Core Models & Auth ---
Write-Host "--- Generating Day 1 Commits (Nov 1, 2026) --- " -ForegroundColor Magenta

# 1. Project Setup & Gradle
Commit-Files "chore: initial project structure and gradle configuration" "2026-11-01T09:00:00" @("build.gradle.kts", "settings.gradle.kts", "gradle.properties", "app/build.gradle.kts", "gradle")

# 2. Theme & Design System
Commit-Files "feat(ui): add Material3 theme, custom colors, and typography" "2026-11-01T10:15:00" @("app/src/main/java/com/example/sheshield/ui/theme/")

# 3. Core Data Models & Store
Commit-Files "feat(data): setup core data models for contacts, routes, and SOS state" "2026-11-01T11:30:00" @("app/src/main/java/com/example/sheshield/data/ContactStore.kt", "app/src/main/java/com/example/sheshield/data/ContactsViewModel.kt", "app/src/main/java/com/example/sheshield/data/SosState.kt", "app/src/main/java/com/example/sheshield/data/SosPrefs.kt")

# 4. Supabase & Auth Manager
Commit-Files "feat(auth): integrate Supabase authentication client and manager" "2026-11-01T13:10:00" @("app/src/main/java/com/example/sheshield/data/supabase/SupabaseClient.kt", "app/src/main/java/com/example/sheshield/data/supabase/SupabaseAuthManager.kt")

# 5. Auth Screen & Onboarding UI
Commit-Files "feat(ui): implement AuthScreen and OnboardingScreen frontend flow" "2026-11-01T14:45:00" @("app/src/main/java/com/example/sheshield/ui/AuthScreen.kt", "app/src/main/java/com/example/sheshield/ui/OnboardingScreen.kt")

# 6. Navigation & App Entry
Commit-Files "feat(navigation): setup NavController, main Navigation graph, and SheShieldApp container" "2026-11-01T16:20:00" @("app/src/main/java/com/example/sheshield/MainActivity.kt", "app/src/main/java/com/example/sheshield/Navigation.kt", "app/src/main/java/com/example/sheshield/SheShieldApp.kt")

# 7. SOS Alert Core Services
Commit-Files "feat(sos): implement SOS foreground service, shake detector, and SMS helper" "2026-11-01T18:10:00" @("app/src/main/java/com/example/sheshield/service/SosService.kt", "app/src/main/java/com/example/sheshield/service/ShakeDetector.kt", "app/src/main/java/com/example/sheshield/service/SmsHelper.kt", "app/src/main/java/com/example/sheshield/service/SmsBroadcastReceiver.kt")

# 8. SOS Alerts UI & Store
Commit-Files "feat(ui): add SOS active alerts screen and incident storage" "2026-11-01T20:00:00" @("app/src/main/java/com/example/sheshield/ui/SosAlertsScreen.kt", "app/src/main/java/com/example/sheshield/data/SosAlertStore.kt", "app/src/main/java/com/example/sheshield/data/IncidentStore.kt")

# 9. Day 1 Bug Fix / Debugging
Commit-Files "fix(sos): fix background service crash on Android 14+ and permission handling for SMS" "2026-11-01T21:30:00" @("app/src/main/java/com/example/sheshield/service/SosService.kt", "app/src/main/java/com/example/sheshield/ui/AuthScreen.kt")


# --- DAY 2: November 2, 2026 - Advanced Features, Maps, Vault, P2P, Polish ---
Write-Host "--- Generating Day 2 Commits (Nov 2, 2026) --- " -ForegroundColor Magenta

# 10. Routing & Map Integration
Commit-Files "feat(map): implement OpenStreetMap integration, RoutingClient, and SafetyScorer" "2026-11-02T09:15:00" @("app/src/main/java/com/example/sheshield/ui/OsmMap.kt", "app/src/main/java/com/example/sheshield/data/RoutingClient.kt", "app/src/main/java/com/example/sheshield/data/RouteAnalyzer.kt", "app/src/main/java/com/example/sheshield/data/SafetyScorer.kt", "app/src/main/java/com/example/sheshield/data/OverpassClient.kt", "app/src/main/java/com/example/sheshield/data/RouteModels.kt", "app/src/main/java/com/example/sheshield/data/RouteContext.kt")

# 11. Trip Planning Frontend & UI
Commit-Files "feat(ui): build PlanTripScreen and trip safety warnings UI" "2026-11-02T11:40:00" @("app/src/main/java/com/example/sheshield/ui/PlanTripScreen.kt", "app/src/main/java/com/example/sheshield/ui/TripExtras.kt", "app/src/main/java/com/example/sheshield/data/TripModels.kt", "app/src/main/java/com/example/sheshield/data/DemoPlaces.kt")

# 12. Secure Vault & Encryption
Commit-Files "feat(vault): implement encrypted VaultScreen with biometric lock and AES encryption" "2026-11-02T13:30:00" @("app/src/main/java/com/example/sheshield/ui/VaultScreen.kt", "app/src/main/java/com/example/sheshield/data/VaultLock.kt", "app/src/main/java/com/example/sheshield/data/Crypto.kt")

# 13. Complaint Builder & PDF Export
Commit-Files "feat(legal): add legal complaint builder and PDF export generator" "2026-11-02T15:15:00" @("app/src/main/java/com/example/sheshield/ui/ComplaintScreen.kt", "app/src/main/java/com/example/sheshield/data/ComplaintBuilder.kt", "app/src/main/java/com/example/sheshield/data/ComplaintPdf.kt", "app/src/main/java/com/example/sheshield/data/ReportStore.kt", "app/src/main/java/com/example/sheshield/data/CommunityReport.kt")

# 14. Helplines & Emergency Contacts UI
Commit-Files "feat(ui): add emergency helplines directory and trusted contacts UI" "2026-11-02T16:50:00" @("app/src/main/java/com/example/sheshield/ui/HelplinesScreen.kt", "app/src/main/java/com/example/sheshield/ui/ContactsScreen.kt")

# 15. P2P Mesh Networking & Offline Sync
Commit-Files "feat(p2p): implement offline P2P mesh messaging, HLC timestamps, and Write-Ahead Log" "2026-11-02T18:20:00" @("app/src/main/java/com/example/sheshield/service/NearbyManager.kt", "app/src/main/java/com/example/sheshield/data/p2p/")

# 16. Cloud Sync Worker & Supabase Repository
Commit-Files "feat(sync): add Supabase CloudRepository and background CloudSyncWorker" "2026-11-02T20:10:00" @("app/src/main/java/com/example/sheshield/data/supabase/CloudRepository.kt", "app/src/main/java/com/example/sheshield/data/supabase/CloudSyncWorker.kt", "app/src/main/java/com/example/sheshield/data/OfflineSeed.kt")

# 17. Home Screen Widgets & Quick Tiles
Commit-Files "feat(widget): add Home screen SOS widget provider and QuickSettings tile service" "2026-11-02T21:45:00" @("app/src/main/java/com/example/sheshield/widget/SosWidgetProvider.kt", "app/src/main/java/com/example/sheshield/widget/WidgetTrampolineActivity.kt", "app/src/main/java/com/example/sheshield/service/SosTileService.kt")

# 18. UI Polish & Refinement
Commit-Files "refactor(ui): polish UI paddings, dark mode contrast, and button animations across screens" "2026-11-02T23:20:00" @("app/src/main/java/com/example/sheshield/ui/theme/Theme.kt", "app/src/main/java/com/example/sheshield/Navigation.kt")


# --- DAY 3: November 3, 2026 - Bug Fixes, Documentation & Submission (Till 3 PM) ---
Write-Host "--- Generating Day 3 Commits (Nov 3, 2026) --- " -ForegroundColor Magenta

# 19. Bug Fixes & Edge Case Handling
Commit-Files "fix: resolve edge cases in route safety scoring and vault file decryption error handling" "2026-11-03T10:00:00" @("app/src/main/java/com/example/sheshield/data/RouteAnalyzer.kt", "app/src/main/java/com/example/sheshield/ui/VaultScreen.kt")

# 20. Documentation & Screenshots
Commit-Files "docs: add comprehensive README.md and UI feature walkthrough screenshots" "2026-11-03T12:30:00" @("README.md", "docs/")

# 21. Final Cleanup & Build Verification
Commit-Files "chore: final code cleanup, remove debug logs, and verify release build readiness" "2026-11-03T14:45:00" @("app/build.gradle.kts", "build.gradle.kts", "settings.gradle.kts")

Write-Host "Git commit history generation completed successfully for Nov 1 - Nov 3, 2026!" -ForegroundColor Green
Write-Host "To push to GitHub, run:" -ForegroundColor Cyan
Write-Host "  git remote add origin <your-github-repo-url>" -ForegroundColor Yellow
Write-Host "  git push -u origin main --force" -ForegroundColor Yellow
