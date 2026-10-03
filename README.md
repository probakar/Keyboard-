# CustomBoard

**A complete, production-quality Android keyboard — Gboard's polish, Samsung Keyboard's feature
depth, and a full suite of Google-powered AI writing tools.**

CustomBoard is a native `InputMethodService` written entirely in Kotlin with a custom
`Canvas`-based renderer (no deprecated framework `KeyboardView`), a Material Design 3 settings
app, Room-backed clipboard history, glide typing, 9 language layouts, 15 themes, and an AI panel
built on Gemini and on-device ML Kit.

![Android](https://img.shields.io/badge/Android-7.0%20--%2014-3DDC84)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF)
![minSdk](https://img.shields.io/badge/minSdk-24-blue)
![License](https://img.shields.io/badge/license-Apache%202.0-green)

---

## Table of contents

1. [Highlights](#highlights)
2. [AI features (Google APIs)](#ai-features-google-apis)
3. [Full feature list](#full-feature-list)
4. [Screens and panels](#screens-and-panels)
5. [Building](#building)
6. [Project structure](#project-structure)
7. [Architecture](#architecture)
8. [Privacy](#privacy)
9. [Configuration](#configuration)
10. [Development tools](#development-tools)
11. [License](#license)

---

## Highlights

| | |
|---|---|
| **131 Kotlin files, ~16 500 lines** | No stubs, no `TODO`s, no placeholder implementations |
| **Custom renderer** | Every key is drawn on a `Canvas`; `android.inputmethodservice.KeyboardView` is never used |
| **Glide typing** | Shape-matching recogniser with bigram re-ranking and a live gesture trail |
| **Autocorrect** | Keyboard-aware Damerau-Levenshtein distance, 3 962-word dictionary, 196 bigrams, learned vocabulary |
| **Clipboard** | Room database, pinning, categories, 1 h → forever retention, 500-item cap |
| **AI panel** | 18 writing tools: proofread, rewrite, 8 tones, translate, smart reply, compose, summarise … |
| **Themes** | 15 presets + Material You + live custom studio + online catalog + gallery photo backgrounds |
| **Handwriting** | On-device Digital Ink 19.0.0; model downloads on demand, including Urdu and multiple scripts |
| **GIF search** | GIPHY v1 API, user-owned encrypted key, GIPHY attribution |
| **Languages** | English (US/UK), Urdu, Arabic, Hindi, French, German, Spanish, Dvorak — RTL aware |
| **Editor-aware layouts** | Dedicated email and URL rows with `@`, `.com`, `www.` and `/` shortcuts |
| **APK size** | R8 code/resource shrinking, language-resource filtering and per-ABI APK splits |
| **Modes** | One-handed, floating (draggable), split, resizable, number row |
| **Privacy** | No analytics, no tracking, no telemetry. Incognito mode + Quick Settings tile |

---

## AI features (Google APIs)

CustomBoard combines **on-device Google ML Kit** models (free, offline, private) with the
**Gemini API** (optional, user-supplied key) so that the common cases never leave the phone.

### On-device (ML Kit — no key, no network, no cost)

| Feature | Model |
|---|---|
| **Smart Reply** — three contextual reply chips above the keyboard | `ml-kit:smart-reply` |
| **Language identification** — detects what you are reading or writing | `ml-kit:language-id` |
| **Offline translation** — downloads a language pack once, then translates with no network | `ml-kit:translate` |

### Cloud (Gemini — bring your own free key)

| Tool | What it does |
|---|---|
| **Proofread** | Fixes grammar, spelling and punctuation while keeping your voice |
| **Rewrite** | Rephrases the selection, keeping the meaning |
| **Change tone** | professional, friendly, formal, casual, confident, polite, funny, poetic |
| **Summarise** | Condenses long text to its essentials |
| **Expand** | Turns notes into full prose |
| **Shorten** | Tightens the text without losing meaning |
| **Bullet list** | Converts prose into scannable bullets |
| **Translate** | 16 target languages; falls back to ML Kit when offline |
| **Continue writing** | Smart Compose: predicts the rest of your sentence |
| **Compose** | Writes a message from a one-line instruction |
| **Emojify** | Adds tasteful emoji to your text |
| **Explain** | Explains the selected text in plain words |
| **Ask AI** | Free-form question, answer inserted at the cursor |
| **Hashtags** | Generates relevant hashtags for social posts |
| **Custom prompts** | Your own instructions, one per line in settings |

Model, temperature and default tone are configurable (`gemini-2.5-flash` by default; Flash-Lite,
Pro, 2.0 Flash and 1.5 Flash are also offered). The key is stored **AES-256-GCM encrypted in the
Android Keystore** and is only ever sent to `generativelanguage.googleapis.com`. An
**offline-only switch** blocks every cloud call outright.

Get a free key at <https://aistudio.google.com/app/apikey> (the settings screen links to it).

---

## Full feature list

<details>
<summary><strong>1. Core typing (1–15)</strong></summary>

QWERTY/AZERTY/QWERTZ/Dvorak layouts · symbols pages 1 and 2 · numeric pad · phone pad · optional
number row · shift / caps-lock with double-tap · long-press alternates with a slide-to-pick popup
· key preview bubbles · key repeat with configurable delay · auto-capitalisation · double-space
period · smart punctuation and smart quotes · auto-spacing after punctuation · per-app layout
memory.
</details>

<details>
<summary><strong>2. Gesture typing (16–28)</strong></summary>

Shape-based glide recognition · live gesture trail · bigram re-ranking of glide candidates ·
space-bar cursor control with acceleration · swipe-left-on-backspace deletes a word · swipe down
to hide · swipe up on a key for its secondary character · two-finger gestures · fully rebindable
gesture map (`CustomGestureMapper`) · configurable distance thresholds and long-press timing.
</details>

<details>
<summary><strong>3. Autocorrect and prediction (29–45)</strong></summary>

Keyboard-aware Damerau-Levenshtein distance (neighbouring-key typos cost 0.6, transpositions 0.8)
· 3 962-word frequency dictionary · 196 bigrams · learned unigrams and bigrams · next-word
prediction · word completion · personal dictionary · blocked words · text shortcuts/expansion ·
offensive-word filter · grammar checker · spell-checker service exposed to other apps · inline
calculator (`12*8+4` → `100`) · contact-name suggestions · clipboard suggestion chip · emoji
suggestion from the word you are typing · undo/redo of auto-corrections.
</details>

<details>
<summary><strong>4. Clipboard manager (46–58)</strong></summary>

Room database `clipboard_items(id, content, timestamp, is_pinned, category, preview)` · automatic
capture · pinning · categories (text, link, email, phone, number, code, address) · search ·
retention 1 h / 6 h / 12 h / 24 h / 3 d / 7 d / 30 d / forever (24 h default) · 50/100/250/500 item
cap · survives reboots · one-tap paste · delete and clear-all · never records anything in
incognito mode or in password fields.
</details>

<details>
<summary><strong>5. Emoji, stickers and GIFs (59–75)</strong></summary>

1 052-entry emoji table · 9 categories with a tab strip · search by name and keyword · recents and
frequency ranking · skin-tone variant picker · kaomoji library · sticker packs · GIPHY GIF search
with an in-memory `LruCache` · GIFs are shared through a `FileProvider` content URI
(`InputConnectionCompat.commitContent`) with a plain-URL fallback for apps that refuse rich
content.
</details>

<details>
<summary><strong>6. Themes and appearance (76–100)</strong></summary>

15 presets (light, dark, AMOLED, Material You, blue, green, purple, red, ocean, sunset, forest,
candy, mono, midnight, custom) · Material You dynamic colour on Android 12+ · live custom-theme
studio with HSV colour controls and keyboard preview · community theme catalog with offline fallback
· JSON theme import · downscaled gallery photo backgrounds · gradients · key shapes
(rounded, pill, square, circle, flat) · key borders · 7 fonts · adjustable key font size, corner
radius, keyboard height, bottom padding and opacity · automatic light/dark switching.
</details>

<details>
<summary><strong>7. Sound and haptics (101–108)</strong></summary>

5 sound profiles (system, soft, mechanical, typewriter, bubble) · per-key-type sounds · volume
slider · haptic feedback with a strength slider · respects silent mode · key-press animations ·
optional key preview popups.
</details>

<details>
<summary><strong>8. Languages (109–122)</strong></summary>

English (US), English (UK), Urdu, Arabic, Hindi, French, German, Spanish and Dvorak · full RTL
rendering and cursor handling · per-language space-bar label · language key with long-press IME
picker · multilingual typing · 8 IME subtypes declared in `method.xml`.
</details>

<details>
<summary><strong>9. Keyboard modes (123–134)</strong></summary>

One-handed left/right with a scale slider · floating keyboard with a drag handle and remembered
position · split keyboard for tablets and landscape · resize panel (height + bottom padding) ·
landscape-specific dimensions · number-row toggle · hide-keyboard key.
</details>

<details>
<summary><strong>10. Toolbar (135–145)</strong></summary>

Scrollable toolbar with 25 actions · drag-and-drop reordering with switches to hide items ·
active-state highlighting · one-tap access to AI, clipboard, emoji, voice, GIF, stickers,
translate, search, themes, text tools, cursor control, select-all/copy/cut/paste, undo/redo,
one-handed, floating, split, resize, number row, incognito, contacts and settings.
</details>

<details>
<summary><strong>11. Voice input (146–152)</strong></summary>

`SpeechRecognizer` integration with partial results · animated volume ring · offline preference ·
voice commands ("new line", "delete that", "select all", "undo", "send", "stop") · graceful
permission handling through a transparent helper activity.
</details>

<details>
<summary><strong>12. Search and translate (153–160)</strong></summary>

In-keyboard search panel with Google, Images, Maps, YouTube, Translate and Define providers ·
contact search with insert · share the selection · quick translate using ML Kit first and Gemini
as a fallback.
</details>

<details>
<summary><strong>13. Text tools (161–176)</strong></summary>

15 transformations (UPPERCASE, lowercase, Title Case, Sentence case, tOGGLE, camelCase,
snake_case, kebab-case, reverse, trim spaces, remove line breaks, sort lines, remove duplicates,
add quotes, URL-encode) · 16 Unicode stylisers (bold, italic, script, fraktur, double-struck,
monospace, circled, squared, fullwidth, small caps, upside-down, strikethrough, underline, spaced,
wavy) · live word/character/sentence counter · cursor-control pad with word jumps and a selection
mode.
</details>

<details>
<summary><strong>14. Privacy (177–184)</strong></summary>

Incognito mode (nothing learned, nothing stored) with a visual tint · Quick Settings tile ·
automatic disable in password fields · biometric app lock for settings · encrypted key storage ·
no analytics, no tracking, no network calls other than the AI/GIF features you trigger yourself.
</details>

<details>
<summary><strong>15. Settings app (185–195)</strong></summary>

Material 3 settings with 11 screens · five-step setup wizard that checks the real system state ·
theme gallery with live miniature previews rendered by the keyboard renderer itself · toolbar
editor · personal-dictionary manager · API-key dialogs · reset-to-defaults · about screen.
</details>

<details>
<summary><strong>16. Polish and performance (196–200)</strong></summary>

Object pooling and pre-allocated `Paint`s in the renderer · no allocations in `onDraw` ·
coroutine-based suggestion pipeline that cancels stale work · lazily created panels · dictionaries
loaded off the main thread at application start · `RecyclerView` everywhere a list appears ·
full content descriptions and TalkBack announcements · high-contrast and large-key accessibility
options.
</details>

---

## Screens and panels

| Surface | Implementation |
|---|---|
| Keyboard | `widgets/KeyboardView.kt` — custom `Canvas` rendering, multi-touch, popups, glide trail |
| Suggestion strip | `widgets/CandidateView.kt` |
| Toolbar | `widgets/ToolbarView.kt` |
| Emoji / kaomoji / stickers | `widgets/EmojiPanelView.kt`, `widgets/MediaPanelView.kt` |
| Clipboard | `widgets/ClipboardPanelView.kt` |
| AI | `widgets/AiPanelView.kt` |
| Text tools | `widgets/TextToolsPanelView.kt` |
| Cursor pad | `widgets/CursorControlPanelView.kt` |
| Voice | `widgets/VoiceInputView.kt` |
| Search | `widgets/SearchPanelView.kt` |
| Resize | `widgets/ResizePanelView.kt` |
| Expanded suggestions | `widgets/SuggestionsPanelView.kt` |

Panels are built programmatically (no per-panel XML), swapped into a single `FrameLayout` by the
IME, and each one exposes `applyTheme(ThemeColors)` plus a small `Listener` interface. Panels that
contain a text field implement `TextInputTarget`, so the physical keys keep working inside them.

---

## Building

### Requirements

* JDK 17
* Android SDK 34 (compile and target), minSdk 24
* Android Studio Koala or newer (or Gradle 8.7 on the command line)

### Android Studio

1. **File → Open** and select this folder.
2. Let the IDE download the Gradle distribution and SDK packages.
3. Run the `app` configuration.

### Command line

```bash
# Generate the Gradle wrapper JAR once (it is intentionally not committed)
gradle wrapper --gradle-version 8.7

./gradlew assembleDebug          # debug APK -> app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug              # Android Lint
./gradlew assembleRelease        # R8/resource-shrunk ABI APKs plus a universal APK
```

> `gradle/wrapper/gradle-wrapper.jar` is a binary and is not stored in the repository. `gradlew`
> downloads it automatically on first run, or you can regenerate it with the command above.

### Signing a release

For production distribution, create `keystore.properties` in the project root (it is git-ignored):

```properties
storeFile=/absolute/path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

If no private keystore is configured, `assembleRelease` uses the standard debug signing key so the
optimized APK remains installable for testing. Do not use that fallback for a public release.

---

## Project structure

```
app/src/main/java/com/customboard/keyboard/
├── CustomBoardApplication.kt     Warm-up of dictionaries, emoji and clipboard
├── service/                      CustomBoardIME, InputLogic, CursorController
├── keyboard/                     Renderer helpers, popups, sounds, modes, glide engine
├── layouts/                      14 layout definitions + LayoutFactory
├── widgets/                      KeyboardView, CandidateView, Toolbar and every panel
├── autocorrect/                  Dictionary, spell checker, prediction, grammar, personal words
├── clipboard/                    Room entity, DAO, database, manager, retention, pinning
├── emoji/                        Emoji table, search, recents, kaomoji, stickers, GIPHY GIFs
├── ai/                           Gemini client, prompts, writing assistant, ML Kit wrappers
├── theme/                        Theme model, presets, dynamic colour, studio, photo backgrounds, online store
├── textprocessing/               Transformers, Unicode stylisers, translator, calculator
├── gesture/                      Gesture detector, swipe handler, space-bar handler, mapper
├── voice/                        SpeechRecognizer manager and voice commands
├── search/                       Web search providers and contact lookup
├── toolbar/                      Toolbar items and ordering
├── privacy/                      Incognito manager, Quick Settings tile, encrypted storage
├── accessibility/                TalkBack announcements and accessibility preferences
├── settings/                     Settings app, wizard, theme studio/store, toolbar editor, dictionary
└── utils/                        Key codes, preference keys, defaults, constants, extensions
```

---

## Architecture

```
            ┌──────────────────────────────────────────┐
            │              CustomBoardIME              │
            │  (InputMethodService — the only glue)    │
            └───────┬───────────────┬──────────────────┘
                    │               │
      ┌─────────────▼──────┐   ┌────▼──────────────────┐
      │ KeyboardView       │   │ InputLogic            │
      │ Canvas renderer    │   │ composing buffer,     │
      │ touch + gestures   │   │ autocorrect, undo     │
      └─────────┬──────────┘   └────┬──────────────────┘
                │                   │
       ┌────────▼────────┐    ┌─────▼───────────────────┐
       │ LayoutFactory   │    │ AutoCorrectionEngine    │
       │ 14 layouts      │    │ + LanguageModelManager  │
       └─────────────────┘    └─────────────────────────┘
```

* **One service, many panels.** `CustomBoardIME` implements every panel `Listener`, so all state
  transitions live in one readable file instead of being scattered across callbacks.
* **Immutable key model.** `Key`, `KeyRow` and `KeyboardLayout` are data classes; every piece of
  drawing state belongs to the view.
* **Coroutines, not threads.** Suggestions, AI calls and GIF downloads run on `Dispatchers.Default`
  / `IO` and are cancelled whenever the input context changes.
* **Preferences as the single source of truth.** `PreferencesManager` exposes typed properties, and
  the IME re-applies everything through one `applyAllPreferences()` call when anything changes.

---

## Privacy

* **No analytics, no crash reporting, no tracking, no advertising IDs** — the dependency list
  contains nothing that could phone home.
* The only network calls are the ones you trigger: a Gemini request, a GIPHY GIF search, a theme
  catalog/theme download, or an ML Kit model download. ML Kit translation runs on-device after its
  first model download.
* **Incognito mode** disables learning, clipboard capture and suggestions, and tints the keyboard
  so the state is always visible. A Quick Settings tile toggles it from anywhere.
* Password and no-suggestion fields automatically disable learning and the clipboard.
* API keys are encrypted with AES-256-GCM using a key that lives in the Android Keystore, and they
  are excluded from cloud backups (`backup_rules.xml`, `data_extraction_rules.xml`).

---

## Configuration

| Setting | Where |
|---|---|
| Gemini API key | Settings → AI writing tools → Gemini API key |
| GIPHY API key | Settings → AI writing tools → GIFs and stickers (user-supplied; encrypted on-device) |
| Theme catalog | Theme and appearance → Browse themes → Theme store (falls back to bundled themes offline) |
| Photo background | Theme and appearance → Browse themes → Use gallery photo |
| Default build-time keys | `app/build.gradle.kts` → `DEFAULT_GEMINI_API_KEY`, `DEFAULT_GIPHY_API_KEY` (empty by default) |
| Optimized APK | `assembleRelease` creates ABI-specific APKs plus a universal APK; release fallback uses the debug signing key |
| Clipboard retention | Settings → Clipboard → Keep clips for |
| Gesture bindings | `CustomGestureMapper` (stored as JSON in preferences) |
| Toolbar order | Settings → Layout and modes → Customise toolbar |

---

## Development tools

Two dependency-free Python checkers run without an Android SDK and are useful in review:

```bash
python3 tools/check_resources.py    # every R.* / @type/name reference resolves
python3 tools/check_kotlin_refs.py  # imports, ViewBinding fields, and no TODO/stub markers
```

Data generators (re-runnable, output committed under `app/src/main/res/raw/`):

```bash
python3 tools/gen_dictionary.py     # dictionary_en.txt + bigrams_en.txt
python3 tools/gen_emoji.py          # emoji_data.txt
```

---

## License

```
Copyright 2025 CustomBoard contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

See [LICENSE](LICENSE) for the full text.
