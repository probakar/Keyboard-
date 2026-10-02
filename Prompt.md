# 🎯 Complete AI Agent Prompt for Custom Android Keyboard Project

Neeche aapko **complete prompt file** diya ja raha hai jo aap apne AI Agent (e.g., GitHub Copilot Agent, Devin, or any CI/CD based agent) ko de sakte hain. Yeh prompt **copy-paste ready** hai:

---

## 📋 MASTER PROMPT FILE

```text
==============================================================================
PROJECT: CustomBoard - Advanced Android Keyboard Application
AGENT TASK: Build a FULLY WORKING Android Keyboard APK from scratch
REPOSITORY: Connect to my GitHub account and push all code there
LANGUAGE: Kotlin + XML (Native Android)
MIN SDK: 24 (Android 7.0)
TARGET SDK: 34 (Android 14)
==============================================================================

IMPORTANT RULES:
- Do NOT just create placeholder files. Every feature must be 100% FUNCTIONAL.
- Do NOT push the .yml workflow file inside the project. I will handle CI/CD myself.
- The APK must compile and install without ANY errors.
- Every single feature listed below MUST be implemented with real working code.
- Analyze every top keyboard app (Gboard, Samsung Keyboard, SwiftKey, Fleksy,
  Grammarly Keyboard, Chrooma) and include ALL features found in them.
- The UI must look EXACTLY like Google Gboard (Material Design 3).
- The features/functionality must match Samsung Galaxy Keyboard.
- This is a PRODUCTION-QUALITY project, not a demo or prototype.

==============================================================================
PROJECT STRUCTURE (Create exactly this):
==============================================================================

CustomBoard/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/customboard/keyboard/
│   │   │   │   ├── CustomBoardApplication.kt
│   │   │   │   ├── service/
│   │   │   │   │   ├── CustomBoardIME.kt              (Main InputMethodService)
│   │   │   │   │   ├── InputMethodServiceHelper.kt     (Helper utilities)
│   │   │   │   │   └── KeyboardActionHandler.kt        (Key press handling)
│   │   │   │   ├── keyboard/
│   │   │   │   │   ├── KeyboardLayoutManager.kt        (Layout switching logic)
│   │   │   │   │   ├── KeyboardRenderer.kt             (Custom drawing/rendering)
│   │   │   │   │   ├── KeyPopupManager.kt              (Long press popup keys)
│   │   │   │   │   ├── KeySoundManager.kt              (Sound + haptic feedback)
│   │   │   │   │   ├── GestureTypingEngine.kt          (Swipe/glide typing)
│   │   │   │   │   ├── OneHandedModeManager.kt         (One-handed keyboard)
│   │   │   │   │   ├── FloatingKeyboardManager.kt      (Floating/resizable mode)
│   │   │   │   │   ├── SplitKeyboardManager.kt         (Split keyboard for tablets)
│   │   │   │   │   └── NumberRowManager.kt             (Dedicated number row)
│   │   │   │   ├── layouts/
│   │   │   │   │   ├── QwertyLayout.kt
│   │   │   │   │   ├── SymbolLayout.kt
│   │   │   │   │   ├── SymbolPage2Layout.kt
│   │   │   │   │   ├── NumpadLayout.kt
│   │   │   │   │   ├── PhonepadLayout.kt
│   │   │   │   │   ├── EmojiLayout.kt
│   │   │   │   │   ├── DvorakLayout.kt
│   │   │   │   │   ├── AzertyLayout.kt
│   │   │   │   │   ├── QwertzLayout.kt
│   │   │   │   │   ├── UrduLayout.kt
│   │   │   │   │   ├── ArabicLayout.kt
│   │   │   │   │   ├── HindiLayout.kt
│   │   │   │   │   └── LayoutFactory.kt                (Factory pattern for layouts)
│   │   │   │   ├── emoji/
│   │   │   │   │   ├── EmojiManager.kt                 (All emoji categories)
│   │   │   │   │   ├── EmojiSearchEngine.kt            (Search emojis by name)
│   │   │   │   │   ├── EmojiVariantSelector.kt         (Skin tone selection)
│   │   │   │   │   ├── RecentEmojiTracker.kt           (Recently used emojis)
│   │   │   │   │   ├── EmojiCategoryAdapter.kt
│   │   │   │   │   ├── KaomojiManager.kt               (Japanese text emoticons)
│   │   │   │   │   └── StickerManager.kt               (Sticker packs support)
│   │   │   │   ├── autocorrect/
│   │   │   │   │   ├── AutoCorrectionEngine.kt         (Real autocorrect logic)
│   │   │   │   │   ├── SpellChecker.kt                 (Spell checking)
│   │   │   │   │   ├── WordPredictionEngine.kt         (Next word prediction)
│   │   │   │   │   ├── PersonalDictionary.kt           (User's custom words)
│   │   │   │   │   ├── TextCompletionManager.kt        (Smart completion)
│   │   │   │   │   ├── GrammarChecker.kt               (Basic grammar checking)
│   │   │   │   │   └── LanguageModelManager.kt         (N-gram language model)
│   │   │   │   ├── clipboard/
│   │   │   │   │   ├── ClipboardManager.kt             (Main clipboard logic)
│   │   │   │   │   ├── ClipboardDatabase.kt            (Room DB for persistence)
│   │   │   │   │   ├── ClipboardDao.kt                 (Data access object)
│   │   │   │   │   ├── ClipboardEntity.kt              (Database entity)
│   │   │   │   │   ├── ClipboardAdapter.kt             (RecyclerView adapter)
│   │   │   │   │   ├── ClipboardRetentionManager.kt    (Auto-delete after time)
│   │   │   │   │   └── PinnedClipsManager.kt           (Pin important clips)
│   │   │   │   ├── theme/
│   │   │   │   │   ├── ThemeManager.kt                 (Theme switching)
│   │   │   │   │   ├── ThemeColors.kt                  (Color definitions)
│   │   │   │   │   ├── DynamicColorExtractor.kt        (Wallpaper-based theme)
│   │   │   │   │   ├── CustomThemeCreator.kt           (User custom themes)
│   │   │   │   │   ├── KeyBorderManager.kt             (Key border styles)
│   │   │   │   │   ├── FontManager.kt                  (Custom fonts)
│   │   │   │   │   ├── BackgroundImageManager.kt       (Custom background image)
│   │   │   │   │   └── ThemePresets.kt                  (Pre-built themes)
│   │   │   │   ├── settings/
│   │   │   │   │   ├── SettingsActivity.kt             (Main settings screen)
│   │   │   │   │   ├── SettingsFragment.kt
│   │   │   │   │   ├── AppearanceSettingsFragment.kt   (Theme/look settings)
│   │   │   │   │   ├── TypingSettingsFragment.kt       (Autocorrect/prediction)
│   │   │   │   │   ├── SoundHapticSettingsFragment.kt  (Sound & vibration)
│   │   │   │   │   ├── ClipboardSettingsFragment.kt    (Clipboard retention time)
│   │   │   │   │   ├── LanguageSettingsFragment.kt     (Add/remove languages)
│   │   │   │   │   ├── GestureSettingsFragment.kt      (Gesture configuration)
│   │   │   │   │   ├── AdvancedSettingsFragment.kt     (Advanced options)
│   │   │   │   │   ├── BackupRestoreManager.kt         (Settings backup/restore)
│   │   │   │   │   └── PreferencesManager.kt           (SharedPreferences wrapper)
│   │   │   │   ├── gesture/
│   │   │   │   │   ├── GestureDetector.kt              (Custom gesture detection)
│   │   │   │   │   ├── SwipeGestureHandler.kt          (Swipe actions)
│   │   │   │   │   ├── SpacebarGestureHandler.kt       (Swipe spacebar = move cursor)
│   │   │   │   │   └── CustomGestureMapper.kt          (User-defined gestures)
│   │   │   │   ├── voice/
│   │   │   │   │   ├── VoiceInputManager.kt            (Speech-to-text)
│   │   │   │   │   └── VoiceInputUI.kt                 (Voice input interface)
│   │   │   │   ├── toolbar/
│   │   │   │   │   ├── ToolbarManager.kt               (Top toolbar management)
│   │   │   │   │   ├── ToolbarActions.kt               (All toolbar actions)
│   │   │   │   │   └── ToolbarCustomizer.kt            (Rearrange toolbar items)
│   │   │   │   ├── textprocessing/
│   │   │   │   │   ├── TextTransformer.kt              (UPPERCASE, lowercase, etc.)
│   │   │   │   │   ├── TranslatorManager.kt            (Quick translate)
│   │   │   │   │   ├── UnicodeTextGenerator.kt         (Fancy/stylish text)
│   │   │   │   │   └── TextShortcutManager.kt          (Text expansion shortcuts)
│   │   │   │   ├── search/
│   │   │   │   │   ├── InKeyboardSearchManager.kt      (Search without leaving KB)
│   │   │   │   │   ├── GifSearchManager.kt             (GIF search + insert)
│   │   │   │   │   └── ContactSearchManager.kt         (Search contacts)
│   │   │   │   ├── privacy/
│   │   │   │   │   ├── IncognitoModeManager.kt         (Incognito mode)
│   │   │   │   │   ├── PrivacyManager.kt               (No data collection)
│   │   │   │   │   └── SecureInputDetector.kt          (Detect password fields)
│   │   │   │   ├── accessibility/
│   │   │   │   │   ├── AccessibilityManager.kt         (TalkBack support)
│   │   │   │   │   └── HighContrastMode.kt             (High contrast theme)
│   │   │   │   ├── utils/
│   │   │   │   │   ├── Constants.kt
│   │   │   │   │   ├── Extensions.kt                   (Kotlin extensions)
│   │   │   │   │   ├── DeviceUtils.kt                  (Screen size detection)
│   │   │   │   │   ├── KeyboardUtils.kt
│   │   │   │   │   └── AnimationUtils.kt               (Key press animations)
│   │   │   │   └── widgets/
│   │   │   │       ├── CustomKeyView.kt                (Individual key widget)
│   │   │   │       ├── KeyboardView.kt                 (Main keyboard custom view)
│   │   │   │       ├── CandidateView.kt                (Suggestion strip)
│   │   │   │       ├── EmojiView.kt                    (Emoji picker view)
│   │   │   │       ├── ClipboardView.kt                (Clipboard panel view)
│   │   │   │       └── ToolbarView.kt                  (Toolbar custom view)
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── keyboard_main.xml               (Main keyboard layout)
│   │   │   │   │   ├── keyboard_qwerty.xml
│   │   │   │   │   ├── keyboard_symbols.xml
│   │   │   │   │   ├── keyboard_symbols_2.xml
│   │   │   │   │   ├── keyboard_numpad.xml
│   │   │   │   │   ├── keyboard_emoji.xml
│   │   │   │   │   ├── key_preview_popup.xml
│   │   │   │   │   ├── key_long_press_popup.xml
│   │   │   │   │   ├── candidate_strip.xml
│   │   │   │   │   ├── clipboard_panel.xml
│   │   │   │   │   ├── clipboard_item.xml
│   │   │   │   │   ├── toolbar_layout.xml
│   │   │   │   │   ├── emoji_category_tab.xml
│   │   │   │   │   ├── emoji_grid_item.xml
│   │   │   │   │   ├── voice_input_layout.xml
│   │   │   │   │   ├── one_handed_layout.xml
│   │   │   │   │   ├── floating_keyboard_layout.xml
│   │   │   │   │   ├── search_bar_layout.xml
│   │   │   │   │   ├── activity_settings.xml
│   │   │   │   │   ├── fragment_settings_main.xml
│   │   │   │   │   ├── fragment_appearance.xml
│   │   │   │   │   ├── fragment_typing.xml
│   │   │   │   │   ├── fragment_clipboard_settings.xml
│   │   │   │   │   ├── fragment_languages.xml
│   │   │   │   │   ├── theme_preview_item.xml
│   │   │   │   │   └── dialog_custom_theme.xml
│   │   │   │   ├── layout-land/
│   │   │   │   │   ├── keyboard_main.xml               (Landscape layout)
│   │   │   │   │   └── keyboard_qwerty.xml
│   │   │   │   ├── xml/
│   │   │   │   │   ├── method.xml                      (Input method config)
│   │   │   │   │   ├── prefs_main.xml
│   │   │   │   │   ├── prefs_appearance.xml
│   │   │   │   │   ├── prefs_typing.xml
│   │   │   │   │   ├── prefs_sound_haptic.xml
│   │   │   │   │   ├── prefs_clipboard.xml
│   │   │   │   │   ├── prefs_languages.xml
│   │   │   │   │   ├── prefs_gestures.xml
│   │   │   │   │   └── prefs_advanced.xml
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml                     (All strings)
│   │   │   │   │   ├── colors.xml                      (Color palette)
│   │   │   │   │   ├── dimens.xml                      (Dimensions)
│   │   │   │   │   ├── styles.xml                      (Styles)
│   │   │   │   │   ├── themes.xml                      (App themes)
│   │   │   │   │   ├── attrs.xml                       (Custom attributes)
│   │   │   │   │   └── arrays.xml                      (String arrays)
│   │   │   │   ├── values-night/
│   │   │   │   │   ├── colors.xml                      (Dark mode colors)
│   │   │   │   │   └── themes.xml                      (Dark themes)
│   │   │   │   ├── values-ur/
│   │   │   │   │   └── strings.xml                     (Urdu translations)
│   │   │   │   ├── values-ar/
│   │   │   │   │   └── strings.xml                     (Arabic translations)
│   │   │   │   ├── values-hi/
│   │   │   │   │   └── strings.xml                     (Hindi translations)
│   │   │   │   ├── drawable/
│   │   │   │   │   ├── key_background_normal.xml
│   │   │   │   │   ├── key_background_pressed.xml
│   │   │   │   │   ├── key_background_special.xml
│   │   │   │   │   ├── spacebar_background.xml
│   │   │   │   │   ├── enter_key_background.xml
│   │   │   │   │   ├── keyboard_background.xml
│   │   │   │   │   ├── candidate_divider.xml
│   │   │   │   │   ├── rounded_button.xml
│   │   │   │   │   ├── clipboard_item_bg.xml
│   │   │   │   │   ├── toolbar_icon_bg.xml
│   │   │   │   │   ├── popup_background.xml
│   │   │   │   │   ├── ic_backspace.xml
│   │   │   │   │   ├── ic_shift.xml
│   │   │   │   │   ├── ic_shift_locked.xml
│   │   │   │   │   ├── ic_enter.xml
│   │   │   │   │   ├── ic_space.xml
│   │   │   │   │   ├── ic_emoji.xml
│   │   │   │   │   ├── ic_mic.xml
│   │   │   │   │   ├── ic_settings.xml
│   │   │   │   │   ├── ic_clipboard.xml
│   │   │   │   │   ├── ic_one_handed.xml
│   │   │   │   │   ├── ic_theme.xml
│   │   │   │   │   ├── ic_search.xml
│   │   │   │   │   ├── ic_translate.xml
│   │   │   │   │   ├── ic_gif.xml
│   │   │   │   │   ├── ic_sticker.xml
│   │   │   │   │   ├── ic_incognito.xml
│   │   │   │   │   ├── ic_pin.xml
│   │   │   │   │   ├── ic_delete.xml
│   │   │   │   │   ├── ic_copy.xml
│   │   │   │   │   ├── ic_select_all.xml
│   │   │   │   │   ├── ic_undo.xml
│   │   │   │   │   ├── ic_redo.xml
│   │   │   │   │   ├── ic_keyboard_hide.xml
│   │   │   │   │   ├── ic_language.xml
│   │   │   │   │   ├── ic_cursor_left.xml
│   │   │   │   │   ├── ic_cursor_right.xml
│   │   │   │   │   └── ic_number_row.xml
│   │   │   │   ├── drawable-night/
│   │   │   │   │   ├── key_background_normal.xml
│   │   │   │   │   ├── key_background_pressed.xml
│   │   │   │   │   └── keyboard_background.xml
│   │   │   │   ├── anim/
│   │   │   │   │   ├── key_press_scale.xml
│   │   │   │   │   ├── popup_enter.xml
│   │   │   │   │   ├── popup_exit.xml
│   │   │   │   │   ├── slide_up.xml
│   │   │   │   │   ├── slide_down.xml
│   │   │   │   │   ├── fade_in.xml
│   │   │   │   │   └── fade_out.xml
│   │   │   │   ├── font/
│   │   │   │   │   └── (Include Roboto or default system font references)
│   │   │   │   ├── raw/
│   │   │   │   │   ├── key_press_sound.ogg
│   │   │   │   │   ├── key_special_sound.ogg
│   │   │   │   │   ├── key_delete_sound.ogg
│   │   │   │   │   ├── key_return_sound.ogg
│   │   │   │   │   └── key_spacebar_sound.ogg
│   │   │   │   ├── mipmap-mdpi/
│   │   │   │   │   └── ic_launcher.png
│   │   │   │   ├── mipmap-hdpi/
│   │   │   │   │   └── ic_launcher.png
│   │   │   │   ├── mipmap-xhdpi/
│   │   │   │   │   └── ic_launcher.png
│   │   │   │   ├── mipmap-xxhdpi/
│   │   │   │   │   └── ic_launcher.png
│   │   │   │   ├── mipmap-xxxhdpi/
│   │   │   │   │   └── ic_launcher.png
│   │   │   │   └── mipmap-anydpi-v26/
│   │   │   │       ├── ic_launcher.xml
│   │   │   │       └── ic_launcher_round.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │       └── java/com/customboard/keyboard/
│   │           ├── ClipboardManagerTest.kt
│   │           ├── AutoCorrectionTest.kt
│   │           └── ThemeManagerTest.kt
│   ├── build.gradle.kts                                (App-level build file)
│   └── proguard-rules.pro
├── build.gradle.kts                                    (Project-level build file)
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── README.md
├── LICENSE
└── .gitignore

==============================================================================
COMPLETE FEATURE LIST (ALL MUST BE IMPLEMENTED AND WORKING):
==============================================================================

────────────────────────────────────────────
CATEGORY 1: CORE TYPING FEATURES
────────────────────────────────────────────
1.  Full QWERTY keyboard layout (English)
2.  Symbols layout (page 1 - common symbols)
3.  Symbols layout (page 2 - additional symbols)
4.  Number pad layout (calculator style)
5.  Phone dialer pad layout
6.  Long press on keys to show alternate characters (popup)
7.  Long press on period key to show common punctuation
8.  Double-tap spacebar to insert period + space
9.  Double-tap shift for CAPS LOCK
10. Auto-capitalization at start of sentences
11. Auto-capitalization after period
12. Smart spacing after punctuation
13. Backspace key with repeat-delete on long press
14. Enter/Return key that adapts (Search, Go, Send, Next, Done)
15. Tab key support where applicable
16. Cursor movement keys (left/right arrows in toolbar)
17. Select all, cut, copy, paste buttons in toolbar
18. Undo and Redo text operations
19. Text selection mode (shift + arrow keys)
20. Long press spacebar to switch language
21. Dedicated number row (toggleable from settings)
22. Key press animation (scale/highlight effect)

────────────────────────────────────────────
CATEGORY 2: GESTURE / SWIPE TYPING
────────────────────────────────────────────
23. Swipe/Glide typing (draw path through letters to type words)
24. Gesture trail visualization (visible path while swiping)
25. Swipe left on backspace to delete entire word
26. Swipe spacebar left/right to move cursor
27. Swipe down on keyboard to minimize/hide
28. Swipe up on keys for alternate characters
29. Customizable swipe gestures in settings

────────────────────────────────────────────
CATEGORY 3: AUTOCORRECT & PREDICTION
────────────────────────────────────────────
30. Real-time autocorrection with suggestion strip
31. Next-word prediction (based on frequency/n-gram model)
32. Suggestion strip showing 3 suggestions (tap to select)
33. Bold the middle suggestion (auto-correct candidate)
34. Long press suggestion to remove it / block the word
35. Personal dictionary (add custom words)
36. Learn from user typing patterns over time
37. Auto-correct toggle ON/OFF in settings
38. Prediction toggle ON/OFF in settings
39. Block offensive words option
40. Multi-language autocorrect
41. Smart punctuation prediction
42. Contact name suggestions (with permission)
43. App-specific prediction (URL bar = no autocorrect)
44. Basic grammar suggestions

────────────────────────────────────────────
CATEGORY 4: CLIPBOARD MANAGER (CRITICAL FEATURE)
────────────────────────────────────────────
45. Clipboard history panel (accessible from toolbar)
46. Store ALL copied text persistently (Room Database)
47. Default retention: 24 hours (auto-delete after that)
48. User can change retention time in settings:
    - 1 hour
    - 6 hours
    - 12 hours
    - 24 hours (default)
    - 3 days
    - 7 days
    - 30 days
    - Forever (never delete)
49. Pin important clipboard items (pinned items never auto-delete)
50. Delete individual clipboard items manually
51. Clear all clipboard history button
52. Tap clipboard item to paste it directly
53. Long press clipboard item for options (pin/delete/edit)
54. Search within clipboard history
55. Clipboard items show timestamp
56. Clipboard items show preview (truncated text)
57. Clipboard works even after phone restart (persistent storage)
58. Organize clips by category (text, links, numbers, etc.)
59. Maximum 500 clipboard items storage
60. Clipboard sync notification (optional)

────────────────────────────────────────────
CATEGORY 5: EMOJI & STICKERS
────────────────────────────────────────────
61. Full emoji keyboard with ALL Unicode 15.0 emojis
62. Emoji categories: Smileys, People, Animals, Food, Travel,
    Activities, Objects, Symbols, Flags
63. Recently used emojis tab (auto-tracked)
64. Frequently used emojis section
65. Emoji search by keyword
66. Skin tone variants (long press on people emojis)
67. Gender variants for people emojis
68. Emoji combinations/suggestions
69. Kaomoji support (Japanese text emoticons) ¯\_(ツ)_/¯
70. Sticker support (basic built-in sticker packs)
71. GIF search and insert (using Tenor/Giphy API - include API integration)
72. Smooth emoji grid with fast scrolling
73. Emoji keyboard remembers last used category

────────────────────────────────────────────
CATEGORY 6: THEMES & APPEARANCE
────────────────────────────────────────────
74. Light theme (Gboard-style white/gray)
75. Dark theme (Gboard-style dark gray/black)
76. AMOLED Black theme (pure black background)
77. Material You / Dynamic Color theme (Android 12+)
78. Blue theme preset
79. Green theme preset
80. Purple theme preset
81. Red theme preset
82. Ocean theme preset
83. Sunset gradient theme preset
84. Custom theme creator:
    - Choose background color
    - Choose key color
    - Choose key text color
    - Choose accent color
    - Choose key border ON/OFF
    - Choose key border color
    - Choose key border radius
    - Choose font
85. Custom background image for keyboard
86. Key border styles: None, Rounded, Square, Pill
87. Key shape options: Rounded Rectangle, Circle, Square
88. Adjustable key height (small, medium, large, extra large)
89. Adjustable keyboard height (compact, normal, tall)
90. Font size adjustment for keys
91. Transparency/opacity slider for keyboard background
92. Preview theme before applying
93. Follow system dark/light mode
94. Gradient background support

────────────────────────────────────────────
CATEGORY 7: SOUND & HAPTIC FEEDBACK
────────────────────────────────────────────
95.  Key press sound (toggleable)
96.  Different sounds for: regular keys, space, backspace, enter, special
97.  Key press haptic/vibration feedback (toggleable)
98.  Vibration intensity slider (weak to strong)
99.  Sound volume slider
100. Visual key press feedback (key highlight animation)
101. Sound and haptic respect system silent mode
102. Custom vibration patterns option

────────────────────────────────────────────
CATEGORY 8: MULTI-LANGUAGE SUPPORT
────────────────────────────────────────────
103. English (US) - QWERTY layout
104. English (UK) - QWERTY layout
105. Urdu - full Urdu keyboard layout (RTL support)
106. Arabic - full Arabic keyboard layout (RTL support)
107. Hindi - Devanagari keyboard layout
108. French - AZERTY layout
109. German - QWERTZ layout
110. Spanish - QWERTY with Ñ
111. Dvorak layout option
112. Switch between languages by swiping spacebar
113. Switch via globe/language key
114. Active language indicator on spacebar
115. Multi-language autocorrect (detect language automatically)
116. Add/remove languages from settings
117. RTL text support for Urdu/Arabic

────────────────────────────────────────────
CATEGORY 9: ONE-HANDED & ACCESSIBILITY
────────────────────────────────────────────
118. One-handed mode (keyboard shifts left or right)
119. Toggle between left-handed and right-handed
120. Resize keyboard in one-handed mode
121. Floating keyboard mode (drag anywhere on screen)
122. Resize floating keyboard
123. Split keyboard for tablets
124. Keyboard height adjustment
125. Long press delay adjustment
126. TalkBack/screen reader support
127. High contrast mode for visually impaired
128. Large key mode

────────────────────────────────────────────
CATEGORY 10: TOOLBAR FEATURES
────────────────────────────────────────────
129. Toolbar strip above keyboard with quick actions
130. Toolbar items:
     - Clipboard
     - Settings
     - Theme switcher
     - One-handed mode toggle
     - Voice input
     - Emoji
     - GIF
     - Translate
     - Floating keyboard
     - Cursor control
     - Select All
     - Copy
     - Cut
     - Paste
     - Undo
     - Redo
     - Text formatting (if supported by app)
     - Search
     - Number row toggle
     - Incognito mode toggle
131. Toolbar is scrollable horizontally
132. Rearrange toolbar items in settings
133. Expand/collapse toolbar

────────────────────────────────────────────
CATEGORY 11: VOICE INPUT
────────────────────────────────────────────
134. Voice typing using Android SpeechRecognizer API
135. Mic button on keyboard (near spacebar or toolbar)
136. Real-time speech to text
137. Voice input language selection
138. Visual feedback during voice recording (waveform animation)
139. Auto-punctuation during voice input

────────────────────────────────────────────
CATEGORY 12: SEARCH & TRANSLATE
────────────────────────────────────────────
140. In-keyboard web search (type query, see results inline)
141. In-keyboard translate (type text, translate to another language)
142. GIF search (powered by Tenor API)
143. Contact search (search phone contacts by name)

────────────────────────────────────────────
CATEGORY 13: TEXT TOOLS
────────────────────────────────────────────
144. Text transformer:
     - UPPERCASE
     - lowercase
     - Title Case
     - Sentence case
     - tOGGLE cASE
     - Reverse text
145. Unicode/stylish text generator:
     - 𝐁𝐨𝐥𝐝
     - 𝘐𝘵𝘢𝘭𝘪𝘤
     - 𝗕𝗼𝗹𝗱 𝗜𝘁𝗮𝗹𝗶𝗰
     - U̲n̲d̲e̲r̲l̲i̲n̲e̲
     - S̶t̶r̶i̶k̶e̶t̶h̶r̶o̶u̶g̶h̶
     - 🅱🅾🆇🅴🅳
     - Ⓒⓘⓡⓒⓛⓔⓓ
     - ꜱᴍᴀʟʟ ᴄᴀᴘꜱ
     - 𝕆𝕦𝕥𝕝𝕚𝕟𝕖𝕕
     - 𝒮𝒸𝓇𝒾𝓅𝓉
     - ɯoɹɹᴉW/Flipped
     - Monospace
     - Wide text (Ｗ ｉ ｄ ｅ)
146. Text expansion/shortcuts (e.g., "addr" → full address)
147. Auto-space after word
148. Smart quotes (" " instead of " ")

────────────────────────────────────────────
CATEGORY 14: PRIVACY & SECURITY
────────────────────────────────────────────
149. Incognito mode (no learning, no clipboard save, no history)
150. Incognito indicator visible when active
151. Auto-detect password fields → disable prediction
152. No data sent to any server (fully offline)
153. No analytics or tracking
154. Option to clear all learned data
155. Option to clear all clipboard data
156. App lock for settings (optional fingerprint/PIN)

────────────────────────────────────────────
CATEGORY 15: SETTINGS APP
────────────────────────────────────────────
157. Beautiful Material Design 3 settings activity
158. Settings categories:
     - Languages & Input
     - Appearance (themes, key style, height, font)
     - Typing (autocorrect, prediction, gestures)
     - Sound & Haptic
     - Clipboard (retention time, max items)
     - Toolbar (customize toolbar items)
     - Gestures (configure swipe actions)
     - Privacy (incognito, clear data)
     - Advanced (backup/restore, reset)
     - About (version, developer info)
159. Backup settings to file
160. Restore settings from file
161. Reset to defaults option
162. Setup wizard on first launch (enable keyboard, set as default)
163. Settings search functionality

────────────────────────────────────────────
CATEGORY 16: UI/UX POLISH
────────────────────────────────────────────
164. Smooth animations on keyboard open/close
165. Key press ripple effect
166. Popup key preview on press (shows enlarged key)
167. Long press popup with extra characters
168. Smooth transition between layouts (letters → symbols → emoji)
169. Keyboard remembers last used layout per app
170. Smooth scrolling in emoji picker
171. Pull-down handle to resize keyboard
172. Material Design 3 components throughout
173. Adaptive layout for different screen sizes
174. Landscape mode support with wider layout
175. Tablet support with larger keys + split option
176. Edge-to-edge keyboard (follows system navigation)
177. Rounded corners on keyboard container
178. Shadow/elevation on keyboard panel
179. Smooth candidate strip with horizontal scroll

────────────────────────────────────────────
CATEGORY 17: PERFORMANCE & QUALITY
────────────────────────────────────────────
180. App size under 15MB
181. Keyboard appears in under 200ms
182. No lag during typing (< 16ms frame time)
183. Efficient memory usage (< 50MB RAM)
184. Battery efficient (minimal background processing)
185. No crashes - comprehensive error handling
186. Works on Android 7.0 to Android 14
187. ProGuard/R8 optimization for release build
188. Proper lifecycle management
189. Handles configuration changes (rotation, split screen)
190. Works with ALL apps (messaging, browsers, social media, etc.)

────────────────────────────────────────────
CATEGORY 18: ADDITIONAL SMART FEATURES
────────────────────────────────────────────
191. Auto-detect input type (email, URL, phone, text)
192. Keyboard adapts to input type:
     - Email field: show @ and .com on main layout
     - URL field: show / and .com
     - Phone field: show numpad
     - Search field: show search icon on enter
193. Smart compose suggestions
194. Date and time quick insert
195. Calculator mode (type expression, see result)
196. Color code picker (for developers)
197. Quick settings toggle from notification
198. Keyboard shortcut cheat sheet
199. What's New dialog after update
200. Rate app prompt (after X days of use)

==============================================================================
ANDROIDMANIFEST.XML REQUIREMENTS:
==============================================================================
- Declare InputMethodService properly
- Declare SettingsActivity
- Request permissions:
  - VIBRATE
  - RECORD_AUDIO (for voice input)
  - READ_CONTACTS (for contact suggestions, optional with runtime request)
  - INTERNET (for GIF search, translate)
  - FOREGROUND_SERVICE (if needed)
- Proper intent filters for IME
- Proper meta-data for input method
- Application theme: Material3

==============================================================================
BUILD.GRADLE DEPENDENCIES (use latest stable versions):
==============================================================================
- AndroidX Core KTX
- AndroidX AppCompat
- Material Design 3 (com.google.android.material)
- AndroidX ConstraintLayout
- AndroidX RecyclerView
- AndroidX ViewPager2
- AndroidX Preference KTX
- AndroidX Room (Runtime + Compiler for KSP) - for clipboard database
- AndroidX Lifecycle (ViewModel + LiveData)
- Kotlin Coroutines (core + android)
- AndroidX Fragment KTX
- AndroidX Navigation (if needed)
- Retrofit2 + OkHttp (for GIF/translate API calls)
- Gson converter
- AndroidX Biometric (for app lock)
- ViewBinding enabled
- KSP plugin for Room

==============================================================================
GRADLE CONFIGURATION:
==============================================================================
- Use Kotlin DSL (.kts) for all gradle files
- Use version catalogs OR direct dependency declarations
- Enable ViewBinding
- Enable KSP for Room annotation processing
- ProGuard rules for release build
- Signing config placeholder for release builds
- Compile with Java 17

==============================================================================
GIT CONFIGURATION:
==============================================================================

.gitignore must include:
*.iml
.gradle/
/local.properties
/.idea/
.DS_Store
/build/
/captures
.externalNativeBuild
.cxx
*.apk
*.aab
*.jks
*.keystore
/app/release/

==============================================================================
README.md CONTENT:
==============================================================================
Include comprehensive README with:
- Project name and description
- Screenshots section (placeholder)
- Complete feature list
- Build instructions
- Tech stack
- Minimum requirements
- How to install and enable the keyboard
- Settings guide
- Contributing guidelines
- License (Apache 2.0)

==============================================================================
CRITICAL IMPLEMENTATION NOTES:
==============================================================================

1. The InputMethodService (CustomBoardIME.kt) is the HEART of the app.
   It must properly implement:
   - onCreateInputView()
   - onStartInput()
   - onFinishInput()
   - onCreateCandidatesView()
   - onUpdateSelection()
   - onKey handling
   - commitText()
   - sendKeyEvent()
   - Properly handle EditorInfo for different input types

2. The keyboard rendering must be CUSTOM (not using deprecated android.inputmethodservice.KeyboardView).
   Build a custom View that draws keys using Canvas or uses RecyclerView/ConstraintLayout.

3. Clipboard database schema:
   CREATE TABLE clipboard_items (
       id INTEGER PRIMARY KEY AUTOINCREMENT,
       content TEXT NOT NULL,
       timestamp LONG NOT NULL,
       is_pinned BOOLEAN DEFAULT 0,
       category TEXT DEFAULT 'text',
       preview TEXT
   )

4. The autocorrect engine should use a pre-built word frequency dictionary.
   Include a basic English dictionary (top 50,000 words) as a raw resource
   or build from a text file.

5. For gesture typing, implement a basic path-matching algorithm that
   compares the swipe path against key positions to find matching words.

6. All themes must be applied in real-time without restarting the keyboard.

7. Sound files: Generate simple key press sounds programmatically using
   ToneGenerator if .ogg files cannot be included, OR include small
   sound files in res/raw/.

8. The settings app must have a polished, professional look matching
   Google's settings design language.

9. Include a first-launch setup screen that guides users to:
   a. Enable the keyboard in system settings
   b. Set it as the default keyboard
   c. Choose initial theme
   d. Select languages

10. Every feature MUST work. Do not leave TODO comments or placeholder
    implementations. If a feature requires an API key (like GIF search),
    include the integration code and use a placeholder key with clear
    instructions on how to replace it.

==============================================================================
DO NOT:
==============================================================================
- Do NOT create empty files or stub implementations
- Do NOT use deprecated KeyboardView from Android framework
- Do NOT use Java (use Kotlin only)
- Do NOT skip any feature listed above
- Do NOT push .yml workflow files
- Do NOT include any malware, tracking, or data collection code
- Do NOT use experimental/alpha dependencies
- Do NOT hardcode strings (use strings.xml)
- Do NOT ignore accessibility

==============================================================================
FINAL CHECKLIST BEFORE PUSHING:
==============================================================================
[ ] All 200 features implemented
[ ] Project compiles without errors
[ ] APK installs on Android 7.0+
[ ] Keyboard appears in system keyboard list
[ ] Can be set as default keyboard
[ ] All layouts render correctly
[ ] Autocorrect works
[ ] Clipboard saves and persists
[ ] Clipboard auto-deletes based on retention setting
[ ] Emoji keyboard works with all categories
[ ] Themes apply correctly
[ ] Settings all functional
[ ] Sound and haptic feedback works
[ ] Voice input works
[ ] Gesture typing works
[ ] One-handed mode works
[ ] Landscape mode works
[ ] No crashes in any scenario
[ ] Memory usage is reasonable
[ ] README.md is complete
[ ] .gitignore is proper
[ ] No .yml files in the repository

==============================================================================
END OF PROMPT
==============================================================================
📝 YML File (Separately — Do NOT Push to Repo)
Yeh file aap apne paas rakhein, repo mein push NAHI karni:

YAML

# ============================================
# FILE: build-customboard.yml
# PURPOSE: GitHub Actions CI/CD for building APK
# NOTE: Do NOT push this file to the repository
# ============================================

name: Build CustomBoard APK

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    name: Build Debug & Release APK
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission to gradlew
        run: chmod +x gradlew

      - name: Cache Gradle packages
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle.kts') }}
          restore-keys: |
            ${{ runner.os }}-gradle-

      - name: Run Lint Check
        run: ./gradlew lint
        continue-on-error: true

      - name: Run Unit Tests
        run: ./gradlew test
        continue-on-error: true

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Build Release APK
        run: ./gradlew assembleRelease

      - name: Upload Debug APK
        uses: actions/upload-artifact@v4
        with:
          name: CustomBoard-Debug-APK
          path: app/build/outputs/apk/debug/app-debug.apk
          retention-days: 30

      - name: Upload Release APK
        uses: actions/upload-artifact@v4
        with:
          name: CustomBoard-Release-APK
          path: app/build/outputs/apk/release/app-release-unsigned.apk
          retention-days: 30

      - name: Upload Lint Report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: Lint-Report
          path: app/build/reports/lint-results-debug.html
          retention-days: 14

      - name: Upload Test Report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: Test-Report
          path: app/build/reports/tests/
          retention-days: 14

      - name: Build Summary
        if: success()
        run: |
          echo "## ✅ Build Successful!" >> $GITHUB_STEP_SUMMARY
          echo "- Debug APK: Ready for download" >> $GITHUB_STEP_SUMMARY
          echo "- Release APK: Ready for download" >> $GITHUB_STEP_SUMMARY
          echo "- Build Time: $(date)" >> $GITHUB_STEP_SUMMARY
🚀 Kaise Use Karein
Step	Action
1	Upar diya gaya MASTER PROMPT copy karein
2	Apne AI Agent (Copilot Workspace / Devin / Custom Agent) mein paste karein
3	Agent ko bolen: "Build this complete project and push to my GitHub repo"
4	Agent project banayega aur push karega
5	YML file aap khud locally rakhein ya manually CI/CD setup karein
6	APK build hone ke baad install karein aur test karein
