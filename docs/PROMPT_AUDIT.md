# Prompt compliance and delivery audit

**Reviewed:** 2026-10-03  
**Branch:** `arena/01a10043-keyboard`
**Source baseline:** published commit `b6b1dd7` plus the current, not-yet-CI-verified worktree changes.
**Status:** the requested features are implemented in source; compile, unit-test, release-shrink and device validation are still pending for this revision.

## Requested work added in this revision

| Requirement | Source status | Notes |
|---|---|---|
| Finger-drawn handwriting | Implemented; CI pending | A touch/stylus canvas feeds ML Kit Digital Ink **19.0.0**. The user chooses among English, Urdu (`ur-PK`), Hindi, Arabic, French, German and Spanish; the language model is downloaded on first use and can then recognize offline. Model data is not bundled in the APK. |
| Current Digital Ink/toolchain | Configured; CI pending | Digital Ink remains **19.0.0** (no compatibility downgrade); Kotlin/KSP are upgraded to `2.1.0` / `2.1.0-1.0.29` to read the dependency metadata. |
| GIF provider | Replaced in source | The Tenor request/parser has been replaced by GIPHY v1 search/trending. Users add their own encrypted GIPHY key in Settings; a direct developer-dashboard link, tap-through setup message, and “Powered by GIPHY” credit are included. No provider key is bundled. |
| Theme customization | Implemented in source | Theme studio has live keyboard preview, palette/gradient controls and JSON import. Gallery photos are copied, downscaled and stored locally as keyboard backgrounds. |
| Online theme catalog | Implemented in source | A GitHub-hosted catalog offers install/apply for four themes and falls back to bundled copies offline. Online catalog/theme downloads require network; installed palettes are editable as the custom theme. |
| Email/URL layouts | Implemented in source | Editor-aware bottom rows add `@`, `.com`, `www.` and `/` shortcuts while preserving the active language layout. |
| Toolbar actions | Implemented in source | Date/time insert using the device locale; the shortcut guide opens an in-keyboard help panel. These items are available in toolbar customization. |
| APK-size work | Configured; measurement pending | Release uses R8 and resource shrinking; ABI-specific APKs and a universal APK are configured; translated resources are limited to the shipped English/Urdu/Hindi/Arabic locales; unused ConstraintLayout, ViewPager2 and Lifecycle ViewModel/LiveData dependencies were removed. Release fallback signing uses the standard debug key for testing only. |

## Existing app audit snapshot

| Area | Status | Remaining caveat |
|---|---|---|
| Core typing, layouts and gestures | Partial | Core editing, glide typing, email/URL and number/phone adaptations are present. Some items from the original 200-feature wish list still need device-level behavior testing. |
| Autocorrect and prediction | Partial | The bundled English dictionary contains about **3,962 words** and English-only bundled n-gram data; it does not meet the original 50,000-word target or prove multilingual autocorrection. Text expansion is now wired in source but its tests await CI. |
| Emoji | Partial | Search, categories, recents, variants and stickers are present; the bundled emoji table has **1,052 entries**, not complete Unicode coverage. |
| Clipboard | Mostly present | Room history, pinning, categories, retention and password/incognito protections are present. Device and lifecycle coverage still needs testing. |
| Search and translation | Partial | The search panel can display DuckDuckGo Instant Answer cards; it is not a complete inline search-results page. Translation and provider links exist. |
| Settings/privacy | Mostly present | Settings search, JSON backup/restore, biometric settings lock, encrypted API credentials, incognito and password-field protections have code paths. Restore/lock behavior still needs device testing. |
| Toolbar, theme and keyboard modes | Implemented in source | Reordering/hiding, expanded tools, custom themes, floating/one-handed/split and resize controls have code paths; visual parity and accessibility need device review. |
| Offline/network boundary | Partial by design | Handwriting models work offline after download. Gemini, GIPHY, theme catalog and initial ML Kit model downloads are optional network features, so the app is not strictly “no server/no network” when those features are used. |
| Runtime/performance | Not verified | No emulator/physical-device run or measured startup, memory, battery or frame-time results are available yet. The release APK-size measurement is also pending. |

## Verification plan/results

Local Android build tools are unavailable in this sandbox (no Java/JDK, Gradle installation or Android SDK). Before CI, the dependency-free checks pass:

- `python3 tools/check_resources.py` — **0 unresolved references**.
- `python3 tools/check_kotlin_refs.py` — **0 problems**.
- XML resources/manifest parse successfully; version catalog and theme JSON parse successfully.

GitHub Actions is configured to build the debug APK, run unit tests and lint, then assemble/upload optimized release APKs. Replace this section with the actual run ID, test/build results and measured APK sizes after CI completes. No device testing should be inferred from a successful CI build.

## Bottom line

This is a feature-rich keyboard implementation, not a verified reproduction of every item in the original 200-feature prompt. The requested handwriting, current Digital Ink version, GIPHY migration, theme catalog/gallery import, toolbar actions and APK-size measures are implemented/configured here, but this revision must pass CI before it can be called build-verified. Dictionary/emoji coverage and full inline web search remain known scope gaps; runtime behavior and size/performance measurements require actual build/device results.
