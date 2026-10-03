# AI features

CustomBoard splits its intelligence in two: what can run **on the device** does, and only the
genuinely generative work is sent to Google's **Gemini API** — and only when you ask for it.

## On-device (Google ML Kit)

| Capability | Dependency | Notes |
|---|---|---|
| Smart Reply | `com.google.mlkit:smart-reply` | Three contextual replies from the last messages in the field. Never leaves the device. |
| Language identification | `com.google.mlkit:language-id` | Detects the language you are reading or writing, used to pick a translation source. |
| Translation | `com.google.mlkit:translate` | Downloads a ~30 MB language pack once, then translates offline. |

These features need no API key, cost nothing, and work in aeroplane mode once the models are
downloaded.

## Cloud (Gemini)

| Action | Prompt intent |
|---|---|
| `PROOFREAD` | Fix grammar, spelling and punctuation; keep the author's voice |
| `REWRITE` | Rephrase while preserving meaning |
| `TONE` | Rewrite in one of eight tones |
| `SUMMARIZE` | Condense to the essentials |
| `EXPAND` | Turn notes into prose |
| `SHORTEN` | Tighten without losing meaning |
| `BULLETS` | Convert prose into bullets |
| `TRANSLATE` | Translate to the chosen target language |
| `CONTINUE` | Smart Compose: finish the sentence or paragraph |
| `COMPOSE` | Write a message from an instruction |
| `EMOJIFY` | Add tasteful emoji |
| `EXPLAIN` | Explain the selection in plain words |
| `ASK` | Answer a free-form question |
| `HASHTAGS` | Generate relevant hashtags |
| `CUSTOM` | Run one of the user's own saved prompts |

### Configuration

| Setting | Default |
|---|---|
| Model | `gemini-2.5-flash` (Flash-Lite, Pro, 2.0 Flash, 1.5 Flash also available) |
| Temperature | 40 % |
| Default tone | professional |
| Translation target | Spanish (16 languages offered) |
| Offline-only | off — turn it on to block every cloud call |
| Keep AI history | on |

### Getting a key

1. Open <https://aistudio.google.com/app/apikey> and create a free key.
2. In CustomBoard: **Settings → AI writing tools → Gemini API key**.
3. Paste and save. The key is encrypted with AES-256-GCM; the encryption key itself is generated
   inside the Android Keystore and never leaves it.

A key can also be baked into a build through `DEFAULT_GEMINI_API_KEY` in `app/build.gradle.kts`
(empty by default, so a stock build ships without any credentials).

### Request shape

`ai/GeminiClient` posts to

```
POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
```

with the system instruction from `ai/PromptLibrary`, the user's text, the configured temperature
and a 45-second timeout. Input longer than 6 000 characters is trimmed before sending. Every HTTP
status maps to a localised message (invalid key, rate limited, blocked by the safety filter,
server error, offline…), and errors that are fixable by the user carry a `needsKey` flag so the
panel can jump straight to the key dialog.

### What is never sent

* Nothing is sent unless you tap an AI action.
* Nothing is sent while incognito mode is on, or from password fields.
* Clipboard contents, the personal dictionary and learned words never leave the device.
