#!/usr/bin/env python3
"""Generates app/src/main/res/raw/emoji_data.txt from the Unicode database shipped with Python.

Format (one per line):  emoji \t category \t searchable keywords
Categories match the Unicode emoji groups used by the picker.
"""
import os, unicodedata

RANGES = [
    # (start, end, category)
    (0x1F600, 0x1F64F, "smileys"),
    (0x1F910, 0x1F97A, "smileys"),
    (0x1F9D0, 0x1F9DF, "people"),
    (0x1F466, 0x1F487, "people"),
    (0x1F574, 0x1F57A, "people"),
    (0x1F645, 0x1F64F, "people"),
    (0x1F6B4, 0x1F6B6, "people"),
    (0x1F400, 0x1F43F, "animals"),
    (0x1F980, 0x1F9AE, "animals"),
    (0x1F330, 0x1F37F, "food"),
    (0x1F950, 0x1F96F, "food"),
    (0x1F32D, 0x1F32F, "food"),
    (0x1F680, 0x1F6A3, "travel"),
    (0x1F3D4, 0x1F3F0, "travel"),
    (0x1F300, 0x1F32C, "travel"),
    (0x1F3A0, 0x1F3CA, "activities"),
    (0x1F93A, 0x1F94F, "activities"),
    (0x1F380, 0x1F39F, "activities"),
    (0x1F4A0, 0x1F4FF, "objects"),
    (0x1F500, 0x1F53D, "symbols"),
    (0x1F5A5, 0x1F5FF, "objects"),
    (0x1F550, 0x1F567, "symbols"),
    (0x1F1E6, 0x1F1FF, "flags"),
]

# Curated legacy-block symbols that are rendered in colour by every Android emoji font.
LEGACY = ("☀☁☂☃☄★☆☎☑☕☘☝☠☢☣☦☪☮☯☸☹☺♈♉♊♋♌♍♎♏♐♑♒♓♟♠♣♥♦♨♻♾♿⚒⚓⚔⚕⚖⚗⚙⚛⚜⚠⚡⚪⚫⚰⚱⚽⚾⛄⛅⛈⛎⛏⛑⛓⛔⛩⛪⛰⛱⛲⛳⛴⛵⛷⛸⛹⛺⛽"
          "✂✅✈✉✊✋✌✍✏✒✔✖✝✡✨✳✴❄❇❌❎❓❔❕❗❣❤➕➖➗➡➰➿⬅⬆⬇⬛⬜⭐⭕")

MANUAL = [
    ("❤️", "symbols", "red heart love like favourite"),
    ("💔", "symbols", "broken heart sad breakup"),
    ("👍", "people", "thumbs up ok good yes like approve"),
    ("👎", "people", "thumbs down bad no dislike"),
    ("🙏", "people", "folded hands please thanks pray namaste"),
    ("👏", "people", "clapping hands applause bravo"),
    ("🔥", "travel", "fire hot lit flame"),
    ("✨", "activities", "sparkles shiny magic ai"),
    ("🎉", "activities", "party popper celebration congrats"),
    ("🥳", "smileys", "partying face celebration birthday"),
    ("😂", "smileys", "face with tears of joy laughing lol funny"),
    ("🤣", "smileys", "rolling on the floor laughing rofl"),
    ("😍", "smileys", "smiling face with heart eyes love adore"),
    ("🥰", "smileys", "smiling face with hearts love affection"),
    ("😊", "smileys", "smiling face with smiling eyes happy blush"),
    ("😁", "smileys", "beaming face grin happy"),
    ("😎", "smileys", "smiling face with sunglasses cool"),
    ("🤔", "smileys", "thinking face hmm consider"),
    ("😢", "smileys", "crying face sad tear"),
    ("😭", "smileys", "loudly crying face sob sad"),
    ("😡", "smileys", "pouting face angry mad"),
    ("🙌", "people", "raising hands celebration hooray"),
    ("💪", "people", "flexed biceps strong muscle"),
    ("🤝", "people", "handshake deal agreement"),
    ("🫶", "people", "heart hands love"),
    ("👋", "people", "waving hand hello bye hi"),
    ("🫡", "smileys", "saluting face respect yes sir"),
    ("💯", "symbols", "hundred points perfect score"),
    ("✅", "symbols", "check mark done yes complete"),
    ("❌", "symbols", "cross mark no wrong cancel"),
    ("⭐", "symbols", "star favourite rating"),
    ("🚀", "travel", "rocket launch fast startup"),
    ("☕", "food", "hot beverage coffee tea"),
    ("🍕", "food", "pizza slice italian"),
    ("🎂", "food", "birthday cake celebration"),
    ("🌹", "animals", "rose flower love"),
    ("🐶", "animals", "dog face puppy pet"),
    ("🐱", "animals", "cat face kitten pet"),
    ("🇵🇰", "flags", "flag pakistan"),
    ("🇮🇳", "flags", "flag india"),
    ("🇺🇸", "flags", "flag united states usa america"),
    ("🇬🇧", "flags", "flag united kingdom uk britain"),
    ("🇸🇦", "flags", "flag saudi arabia"),
    ("🇦🇪", "flags", "flag united arab emirates uae"),
    ("🇨🇦", "flags", "flag canada"),
    ("🇦🇺", "flags", "flag australia"),
    ("🇩🇪", "flags", "flag germany"),
    ("🇫🇷", "flags", "flag france"),
    ("🇪🇸", "flags", "flag spain"),
    ("🇮🇹", "flags", "flag italy"),
    ("🇯🇵", "flags", "flag japan"),
    ("🇨🇳", "flags", "flag china"),
    ("🇰🇷", "flags", "flag south korea"),
    ("🇧🇷", "flags", "flag brazil"),
    ("🇹🇷", "flags", "flag turkey"),
    ("🇷🇺", "flags", "flag russia"),
    ("🇿🇦", "flags", "flag south africa"),
    ("🇧🇩", "flags", "flag bangladesh"),
    ("🇮🇩", "flags", "flag indonesia"),
    ("🇲🇾", "flags", "flag malaysia"),
    ("🇪🇬", "flags", "flag egypt"),
    ("🇳🇬", "flags", "flag nigeria"),
]

SKIP_WORDS = ("VARIATION", "TAG", "ZERO WIDTH", "REGIONAL INDICATOR")

entries = {}
for start, end, category in RANGES:
    for cp in range(start, end + 1):
        ch = chr(cp)
        try:
            name = unicodedata.name(ch)
        except ValueError:
            continue
        if any(skip in name for skip in SKIP_WORDS):
            continue
        keywords = name.lower().replace("-", " ")
        entries[ch] = (category, keywords)

for ch in LEGACY:
    try:
        name = unicodedata.name(ch)
    except ValueError:
        continue
    entries[ch] = ("symbols", name.lower().replace("-", " "))

for ch, category, keywords in MANUAL:
    entries[ch] = (category, keywords)

ORDER = ["smileys", "people", "animals", "food", "travel", "activities", "objects", "symbols", "flags"]
out_dir = "app/src/main/res/raw"
os.makedirs(out_dir, exist_ok=True)
path = os.path.join(out_dir, "emoji_data.txt")
with open(path, "w", encoding="utf-8") as f:
    f.write("# emoji\tcategory\tkeywords\n")
    for category in ORDER:
        for ch, (cat, keywords) in entries.items():
            if cat == category:
                f.write("%s\t%s\t%s\n" % (ch, cat, keywords))
print("emoji entries:", len(entries), "->", path, os.path.getsize(path), "bytes")
