# Architecture

This document explains how CustomBoard is put together, why the pieces are split the way they
are, and where to look when you want to change something.

## 1. The service is the only glue

`service/CustomBoardIME.kt` is the single `InputMethodService`. It implements **every** panel
listener interface plus `KeyboardView.KeyboardListener`, which means all state transitions
("emoji key pressed → open the emoji panel → a GIF was picked → commit content → close the
panel") are visible in one file instead of being spread over a dozen controllers.

Responsibilities:

| Area | Collaborator |
|---|---|
| Drawing and touch | `widgets/KeyboardView` |
| Text manipulation | `service/InputLogic` |
| Selection and cursor | `service/CursorController` |
| Which layout to show | `keyboard/KeyboardLayoutManager` + `layouts/LayoutFactory` |
| Suggestions | `autocorrect/AutoCorrectionEngine` |
| Everything else | the managers in `keyboard/`, `clipboard/`, `ai/`, `voice/`, … |

The service never touches an `InputConnection` directly: `InputLogic` and `CursorController` both
receive a `() -> InputConnection?` supplier, so they stay testable and never hold a stale
connection.

## 2. Rendering

`KeyboardView` draws every key itself:

* the layout is measured once per size/layout change into a list of `KeyPlacement`
  (`Key` + `RectF` + centre), reused for hit testing, popups and glide typing;
* all `Paint` objects are allocated in the constructor — `onDraw` performs no allocation;
* pressed keys, the glide trail and popup previews are drawn in the same pass;
* split mode simply inserts a gap in the placement maths — no second view, no second layout.

`keyboard/KeyboardRenderer` reuses the same drawing code to produce the miniature bitmaps shown
in the theme gallery, so a preview can never drift from the real thing.

## 3. Input pipeline

```
touch ──► KeyboardView ──► KeyboardListener ──► CustomBoardIME.handleKey
                                                      │
                     ┌────────────────────────────────┼────────────────────┐
                     ▼                                ▼                    ▼
              InputLogic (text)            CursorController       panel / mode actions
                     │
                     ├─ composing buffer, auto-space, smart punctuation
                     ├─ auto-correct on separator
                     ├─ snapshot undo/redo stack
                     └─ suggestions() ──► AutoCorrectionEngine
                                               ├─ LanguageModelManager (unigrams + bigrams)
                                               ├─ SpellChecker → EditDistance
                                               ├─ WordPredictionEngine
                                               ├─ TextCompletionManager
                                               └─ PersonalDictionary
```

Suggestions are produced in a coroutine on `Dispatchers.Default`. The previous job is cancelled
on every keystroke, so a slow lookup can never overwrite a newer result.

## 4. Theming

`ThemeColors` is an immutable data class with every colour the keyboard needs.
`ThemeManager.current` resolves it from the preferences, taking into account:

1. the chosen preset (`ThemePresets.ALL`, 15 entries);
2. Material You dynamic colour (`DynamicColorExtractor`, Android 12+);
3. a background image's palette (`BackgroundImageManager` + `DynamicColorExtractor.fromBitmap`);
4. the custom theme built in settings (`CustomThemeCreator`);
5. opacity, high-contrast and night-mode overrides.

Views never read preferences for colours: they receive a `ThemeColors` through `applyTheme()`.

## 5. Clipboard

Room with a single entity:

```sql
CREATE TABLE clipboard_items (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    content   TEXT    NOT NULL,
    timestamp INTEGER NOT NULL,
    is_pinned INTEGER NOT NULL DEFAULT 0,
    category  TEXT    NOT NULL DEFAULT 'text',
    preview   TEXT    NOT NULL
);
```

`ClipboardManager` listens to `OnPrimaryClipChangedListener`, classifies the clip (link, email,
phone, number, code, address, text), and writes it unless incognito mode or a password field is
active. `ClipboardRetentionManager` trims by age and by count; pinned clips are never removed by
either rule.

## 6. AI

`ai/AiWritingAssistant` is the only entry point the UI knows about. It decides, per action,
whether the work can be done on-device:

* **Smart Reply** and **language identification** always run on-device (ML Kit).
* **Translation** tries ML Kit first and only falls back to Gemini when the language pack is not
  available and the user has not enabled offline-only mode.
* Everything else builds a prompt (`ai/PromptLibrary`) and calls `ai/GeminiClient`, an OkHttp
  client with a 45-second timeout that maps every HTTP status to a localised error string.

Results are returned as `AiResult.Success` / `AiResult.Error(message, needsKey)` so the panel can
offer a "set up your key" shortcut instead of a dead end.

## 7. Preferences

`settings/PreferencesManager` is a singleton exposing typed properties over `SharedPreferences`;
keys live in `utils/Prefs` and defaults in `utils/Defaults`. The IME registers a change listener
and funnels everything through `applyAllPreferences()`, so there is exactly one code path that
turns settings into visible behaviour.

## 8. Threading

| Work | Dispatcher |
|---|---|
| Dictionary and emoji loading | `Default`, started from `CustomBoardApplication` |
| Suggestions and glide recognition | `Default`, cancellable |
| Room queries | Room's own executor via `suspend` DAO functions |
| Gemini / Tenor requests | `IO` through OkHttp |
| Everything touching a view | `Main` |

## 9. What is deliberately absent

* No dependency injection framework — the graph is small and explicit.
* No image-loading library: GIF thumbnails are fetched with OkHttp and cached in an `LruCache`.
* No analytics, no crash reporter, no advertising SDK.
