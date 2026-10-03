# `Prompt.md` compliance audit

**Reviewed:** 2026-10-03  
**Source reviewed:** application source at `d3f41cf`; workflow fixes on `arena/01a10043-keyboard`  
**Result:** **Not fully compliant.** This is a substantial native Kotlin keyboard implementation, but the prompt's “all 200 features, production-ready” checklist is not met end-to-end. Several specific feature gaps are verifiable in the source, and runtime/performance claims still need device testing.

## Build failure and current verification

The previous GitHub Actions run (`37099895403`, on `main`) failed at **Set up Android SDK**. Gradle setup, compilation, tests, and APK upload were all skipped, so that run did not establish whether the Kotlin project compiles. The workflow used `android-actions/setup-android@v3`; its default package list requests the retired Android SDK `tools` package.

The first workflow fix (`8a58e17`) successfully got past SDK and Gradle setup, confirming that the environment problem is repaired. Its build run (`37100568079`) then failed in **Build debug APK**; tests and APK upload were skipped. The workflow was updated to capture `build.log` and expose diagnostics as GitHub check annotations. Run `37101020362` identified an AAPT blocker: `style/CustomBoard.Toolbar` was inferred as the parent of an unparented dotted style name, but that parent did not exist. That style has been renamed, and `tools/check_resources.py` now checks implicit style parents. Run `37101235163` exposed mismatched custom preference attributes (`app:defaultColor`, `app:valueUnit`, `app:min`); these are now declared consistently in `attrs.xml`, and the seek-bar preference applies its configured minimum. The checker now also verifies app-namespaced custom attributes. Run `37101430436` passed resource linking and reached `:app:compileDebugKotlin`, but Kotlin compilation failed. Run `37101603386` surfaced three source errors: `CustomKeyView` called the wrong `pressScale` signature, `AiPanelView` tried to assign a read-only `aiTone`, and `KeyboardUtils.isPhoneField` used an early return in an expression-body function. Those were corrected. Run `37101791679` successfully assembled the debug APK but failed while compiling unit tests. Run `37102021043` again assembled the debug APK and uploaded the `CustomBoard-debug-apk` artifact, but unit-test compilation still fails. The unit-test failure reporter now emits the last 150 lines of the Gradle test log as a check annotation, so the next run should expose the exact compiler error. The APK artifact is available from [GitHub Actions run 37102021043](https://github.com/probakar/Keyboard-/actions/runs/37102021043). The workspace could not download GitHub's Azure-backed artifact (the download request returned EOF), so the APK is presently available through that Actions artifact link rather than as a local file.

Local Android compilation was not available during this audit: this workspace has no Java/JDK, Gradle installation, or Android SDK, and the Gradle distribution could not be reached from the local shell. The repository's two dependency-free checks did pass:

- `python3 tools/check_resources.py` — **0 problems**.
- `python3 tools/check_kotlin_refs.py` — **0 unresolved references**.

There are 121 Kotlin source files and six unit-test source files. Those static checks do not substitute for a Gradle compile, unit-test run, or install/device test.

## Requirement-by-requirement summary

Legend: **Mostly present** = substantial source implementation found; **Partial** = implementation exists but a stated requirement is missing or narrower; **Not verified** = requires a built APK/device/measurement.

| Prompt category | Status | Audit notes |
|---|---|---|
| 1. Core typing | **Partial** | QWERTY, symbols, number/phone layouts, alternates, double-space period, caps lock, auto-capitalisation, adaptive enter, cursor/selection actions, undo/redo, number row and per-app layout memory are present. A Tab key path is not wired (the `TAB` constant is unused). Long-pressing space opens Android's IME picker rather than reliably switching CustomBoard's active language. The candidate strip is integrated into the input view rather than returned from `onCreateCandidatesView()`. |
| 2. Gesture typing | **Mostly present** | Glide recognition/trail, swipe actions, space-bar cursor movement, alternate characters and a configurable gesture mapper are present. Space-bar swipes move the cursor; they do not switch languages as the prompt separately requests. |
| 3. Autocorrect and prediction | **Partial** | Suggestions, correction, learning, personal words, grammar checks, and contact suggestions exist. The bundled English dictionary has **3,962 words**, not the requested 50,000; bundled unigram/bigram data is English-only. Long-pressing a suggestion adds it to the personal dictionary rather than removing/blocking it. Multi-language autocorrection/automatic language detection is not established by the source. |
| 4. Clipboard | **Mostly present** | Room persistence, retention settings, pinning, categories, search, item operations and a 500-item cap are implemented. “Store all copied text” is broader than the current IME clipboard-listener lifecycle can guarantee; the optional sync notification is not present. |
| 5. Emoji and stickers | **Partial** | Search, categories, recents/frequency, skin-tone/gender variants and built-in stickers are present; Tenor integration needs the user to supply a key. The bundled table has **1,052 entries**, not all Unicode 15.0 emoji. |
| 6. Themes and appearance | **Mostly present** | Presets, dynamic colour, custom themes, key styling and appearance controls have implementation. Actual visual parity with Gboard is subjective and was not verified on-device. |
| 7. Sound and haptics | **Partial** | Platform key sounds, per-key-type effects, volume and haptic controls are implemented. Sound “profiles” scale platform effects rather than supplying distinct custom timbres; custom vibration patterns are not evident. |
| 8. Languages | **Partial** | English US/UK, Urdu, Arabic, Hindi, French, German, Spanish and Dvorak layouts exist, including RTL support and language controls. Autocorrection data remains English-only; space-bar swiping is cursor control, not language switching. |
| 9. One-handed and accessibility | **Mostly present** | One-handed/floating/split/resizing modes, TalkBack announcements, high contrast and large keys have code paths. The advertised settings lock is not enforced: a preference and biometric dependency exist, but no `BiometricPrompt`/authentication flow was found. |
| 10. Toolbar | **Partial** | A 25-action horizontally scrolling toolbar can be reordered and hidden. No expand/collapse interaction was found. |
| 11. Voice input | **Partial** | Android `SpeechRecognizer`, partial results, volume feedback and an offline preference are present. There is no explicit auto-punctuation option in the recognition intent; availability/offline behavior depends on the device's speech service. |
| 12. Search and translate | **Partial** | Contact lookup and translation integrations exist. Web-provider buttons launch an external browser through `ACTION_VIEW`; they do not show web results inline inside the keyboard as requested. |
| 13. Text tools | **Partial** | Text transformations and Unicode stylizers are present. Text-expansion shortcuts have preference fields/strings but no expansion manager or typing-path use was found. |
| 14. Privacy and security | **Partial** | Incognito/password-field protections and explicit user-triggered cloud features are present. The literal “fully offline/no server” requirement conflicts with the same prompt's Gemini/GIF API requirements; this app can make optional Gemini/Tenor network calls. The settings-lock implementation is also missing. |
| 15. Settings app | **Partial** | Material settings screens, setup wizard, theme picker, dictionary management and reset are present. Backup/restore labels and a preferences snapshot exist, but no file export/import handlers were found. A settings-search string exists but no search UI/logic was found. |
| 16. UI/UX polish | **Partial / not verified** | Custom Canvas rendering, popups, previews, landscape resources and adaptive controls exist. Exact Gboard/Samsung parity, edge-to-edge behavior and compatibility across all apps/devices cannot be claimed from static review. |
| 17. Performance and quality | **Not verified** | The debug APK assembles in CI, but unit-test compilation is still failing and there has been no install/device run. APK size, startup-under-200ms, frame time, RAM, battery use, Android 7–14 coverage and crash-free operation need measurement on real/emulated devices. |
| 18. Additional smart features | **Partial** | Number/phone editor adaptation, enter actions, calculator and Quick Settings tile are present. Email/URL-specific key layouts, date/time insertion, a shortcut cheat sheet, What's New UI and rate-app prompt were not found. State fields for first launch/rating/version alone do not implement those prompts. |

## Other prompt-level gaps and conflicts

- **Dictionary size:** `app/src/main/res/raw/dictionary_en.txt` contains 3,963 lines including the header (3,962 entries), versus the prompt's explicit 50,000-word requirement.
- **Emoji coverage:** `emoji_data.txt` contains 1,053 lines including the header (1,052 entries), not the complete Unicode 15.0 set.
- **README checklist:** the README has feature/build/architecture/privacy information, but it does not include the requested screenshots section, install-and-enable walkthrough, settings guide, or contribution guidelines.
- **Workflow-file rule:** `Prompt.md` says not to put any `.yml` workflow in the repository, while this repository contains `.github/workflows/build.yml`. It is being retained because it is the user's APK-build action; deleting it would remove that build path. This is a deliberate conflict, not an accidental claim of compliance.
- **Exact project tree:** several named prompt files/classes are implemented under different names or consolidated (for example, `InputLogic` and `CursorController`); the source tree is not an exact copy of the proposed tree.
- **“No TODO/stubs”:** the supplied Kotlin-reference checker passes and a source scan found no TODO/FIXME/`NotImplementedError` markers. That is a useful signal, not proof that all 200 behaviors work.

## Bottom line

The codebase is a serious, feature-rich starting point, but it should **not** be described as satisfying every item in `Prompt.md` or as production-validated yet. The highest-confidence unmet items are the dictionary/emoji coverage targets, Tab/text-expansion paths, language-aware autocorrect, inline web results, app lock, settings backup/restore/search, and several smart-feature prompts. The debug APK now assembles and is available as a GitHub Actions artifact, but unit-test compilation still fails; fix and rerun that check before treating the project as fully validated.
