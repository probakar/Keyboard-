package com.customboard.keyboard.autocorrect

/**
 * Lightweight, rule-based grammar help that runs on every keystroke without any network.
 * Deeper rewriting is handled by the Gemini powered proofreader in the AI module.
 */
object GrammarChecker {

    data class Issue(val original: String, val replacement: String, val message: String)

    private val CONTRACTIONS = mapOf(
        "dont" to "don't", "cant" to "can't", "wont" to "won't", "isnt" to "isn't",
        "arent" to "aren't", "wasnt" to "wasn't", "werent" to "weren't", "doesnt" to "doesn't",
        "didnt" to "didn't", "hasnt" to "hasn't", "havent" to "haven't", "hadnt" to "hadn't",
        "couldnt" to "couldn't", "wouldnt" to "wouldn't", "shouldnt" to "shouldn't",
        "im" to "I'm", "ive" to "I've", "ill" to "I'll", "id" to "I'd", "youre" to "you're",
        "youve" to "you've", "theyre" to "they're", "theyve" to "they've", "its" to "it's",
        "thats" to "that's", "whats" to "what's", "lets" to "let's", "hes" to "he's",
        "shes" to "she's", "weve" to "we've", "were" to "we're"
    )

    private val COMMON_TYPOS = mapOf(
        "teh" to "the", "adn" to "and", "recieve" to "receive", "seperate" to "separate",
        "definately" to "definitely", "occured" to "occurred", "untill" to "until",
        "wich" to "which", "thier" to "their", "alot" to "a lot", "becuase" to "because",
        "goverment" to "government", "tommorow" to "tomorrow", "accomodate" to "accommodate",
        "neccessary" to "necessary", "beleive" to "believe", "acheive" to "achieve"
    )

    private val VOWEL_SOUND = charArrayOf('a', 'e', 'i', 'o', 'u')

    /** Checks the last sentence of [text] and returns the issues found. */
    fun check(text: CharSequence): List<Issue> {
        val issues = mutableListOf<Issue>()
        val sentence = text.toString().takeLast(300)
        val words = sentence.split(Regex("\\s+")).filter { it.isNotBlank() }

        words.forEachIndexed { index, raw ->
            val word = raw.trim { !it.isLetterOrDigit() && it != '\'' }
            val lower = word.lowercase()
            if (word.isEmpty()) return@forEachIndexed

            // lone "i" must be capitalised
            if (word == "i") {
                issues += Issue(word, "I", "Capitalise \"I\"")
            }
            CONTRACTIONS[lower]?.let { fixed ->
                if (lower != "were" && lower != "its") {
                    issues += Issue(word, fixed, "Missing apostrophe")
                }
            }
            COMMON_TYPOS[lower]?.let { fixed ->
                issues += Issue(word, fixed, "Common misspelling")
            }
            // a/an agreement
            if ((lower == "a" || lower == "an") && index + 1 < words.size) {
                val next = words[index + 1].lowercase().trim { !it.isLetter() }
                if (next.isNotEmpty()) {
                    val startsWithVowel = VOWEL_SOUND.contains(next[0])
                    if (lower == "a" && startsWithVowel) {
                        issues += Issue("a $next", "an $next", "Use \"an\" before a vowel sound")
                    } else if (lower == "an" && !startsWithVowel) {
                        issues += Issue("an $next", "a $next", "Use \"a\" before a consonant sound")
                    }
                }
            }
            // repeated word
            if (index > 0 && words[index - 1].equals(word, ignoreCase = true) && word.length > 2) {
                issues += Issue("$word $word", word, "Repeated word")
            }
        }
        return issues.distinctBy { it.original + it.replacement }.take(3)
    }

    /** Applies every rule to the whole text (used by the "fix grammar" toolbar action). */
    fun autoFix(text: String): String {
        var result = text
        check(text).forEach { issue ->
            result = result.replace(
                Regex("\\b" + Regex.escape(issue.original) + "\\b"), issue.replacement
            )
        }
        // sentence capitalisation
        val builder = StringBuilder(result)
        var capitaliseNext = true
        for (i in builder.indices) {
            val c = builder[i]
            if (capitaliseNext && c.isLetter()) {
                builder[i] = c.uppercaseChar()
                capitaliseNext = false
            }
            if (c == '.' || c == '!' || c == '?') capitaliseNext = true
        }
        return builder.toString()
    }
}
