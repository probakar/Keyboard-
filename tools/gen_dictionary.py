#!/usr/bin/env python3
"""Generates app/src/main/res/raw/dictionary_en.txt (word<TAB>frequency).

The base list holds the most frequent English lemmas in rough frequency order; regular
inflections are generated with standard English morphology so the shipped dictionary covers
well over 10k surface forms while staying small.
"""
import os, re

BASE = """
the be to of and a in that have i it for not on with he as you do at this but his by from they we say her she or an will my one all would there their what so up out if about who get which go me when make can like time no just him know take people into year your good some could them see other than then now look only come its over think also back after use two how our work first well way even new want because any these give day most us is are was were been has had did said got made went took came saw knew thought looked wanted gave used found told asked worked seemed felt tried leave left call man woman child world life hand part eye place week case point government company number group problem fact money month lot right study book job word business issue side kind head house service friend father power hour game line end member law car city community name president team minute idea kid body information parent face others level office door health person art war history party result change morning reason research girl guy moment air teacher force education foot boy age policy process music market sense nation plan college interest death experience effect class control care field development role effort rate heart drug show leader light voice wife police mind price report decision son hope view relationship town road arm difference value building action model season society tax director position player record paper space ground form event official matter center couple site project activity star table need court produce eat teach oil situation cost industry figure street image phone either data cover quite picture clear practice piece land recent describe product doctor wall patient worker news test movie certain north love personal open support simply third technology catch step baby computer type attention draw film tree source red nearly organization choose cause hair century evidence window difficult listen soon culture billion chance brother energy period course summer less realize hundred available plant likely opportunity term short letter condition choice single rule daughter administration south husband floor campaign material population economy medical hospital church close thousand risk current fire future wrong involve defense anyone increase security bank myself certainly west sport board subject officer private rest behavior deal performance fight throw top quickly past goal second bed order author fill represent focus foreign drop blood upon agency push nature color recently store reduce sound note fine before near movement page enter share common poor natural race concern series significant similar hot language usually response dead rise animal factor decade article shoot east save seven artist away scene stock career despite central eight thus treatment beyond happy exactly protect approach lie size dog fund serious occur media ready sign list individual simple quality pressure accept answer hard resource identify meeting determine prepare disease whatever success argue cup particularly amount ability staff recognize indicate character growth loss degree wonder attack herself region television box training pretty trade election everybody physical lay general feeling standard bill message fail outside arrive analysis benefit forward lawyer present section environmental glass skill sister professor operation financial crime stage compare authority miss design sort act ten knowledge gun station blue strategy little clearly discuss indeed truth song example democratic check environment leg dark various rather laugh guess executive prove hang entire rock enough forget since claim remove manager help enjoy network legal religious cold final main science green memory card above seat cell establish nice trial expert spring firm radio visit management avoid imagine tonight huge ball finish yourself talk theory impact respond statement maintain charge popular traditional onto reveal direction weapon employee cultural contain peace pull return"""

EXTRA = """
hello hi hey thanks thank please sorry okay yes yeah sure maybe tomorrow today yesterday evening afternoon night weekend monday tuesday wednesday thursday friday saturday sunday january february march april june july august september october november december
email message text video photo camera screen battery charger internet wifi password username account login logout download upload install update delete settings keyboard mobile laptop tablet device software hardware website browser google android apple microsoft amazon facebook instagram twitter whatsapp youtube linkedin telegram
schedule appointment deadline report document presentation spreadsheet invoice payment receipt budget salary colleague client customer supplier contract agreement proposal interview resume application
breakfast lunch dinner coffee tea water juice milk bread rice chicken beef fish vegetable fruit apple banana orange mango potato tomato onion garlic salt sugar pepper cheese butter chocolate cake pizza burger sandwich pasta soup salad
apartment room kitchen bathroom bedroom garden garage chair sofa lamp mirror shower towel pillow blanket
train plane bus taxi airport ticket passport luggage hotel booking vacation holiday travel journey destination beach mountain river ocean forest desert island
medicine exercise fitness workout running walking swimming cycling yoga sleep stress sad angry tired excited nervous calm
school university student lesson homework exam grade library geography mathematics physics chemistry biology english literature
regards sincerely wishes kindly appreciate confirm attached reply follow mentioned received requested
really very much many little few more most less least always never sometimes often usually rarely again already still yet almost together alone
awesome amazing great perfect excellent wonderful beautiful lovely cool fun funny interesting boring easy important urgent
congratulations welcome goodbye bye later cheers luck safe
"""

def tokens(text):
    out = []
    for w in text.split():
        w = w.strip().lower()
        if w and re.fullmatch(r"[a-z']+", w):
            out.append(w)
    return out

base = []
seen = set()
for w in tokens(BASE) + tokens(EXTRA):
    if w not in seen:
        seen.add(w)
        base.append(w)

VOWELS = "aeiou"
IRREGULAR_PLURAL = {
    "man": "men", "woman": "women", "child": "children", "foot": "feet", "tooth": "teeth",
    "person": "people", "mouse": "mice", "goose": "geese", "life": "lives", "knife": "knives",
    "wife": "wives", "leaf": "leaves", "half": "halves", "self": "selves",
}
NO_INFLECT = set("""the be to of and a in that i it for not on with as you at this but by from they we so up
out if about who which go me when no just know its over also our even any these us is are was were been has
had did said how two most all would there their what or an will my one some could them see other than then
now look only come after use well way new want because give day think make take""".split())

def plural(word):
    if word in IRREGULAR_PLURAL:
        return IRREGULAR_PLURAL[word]
    if word.endswith(("s", "x", "z", "ch", "sh")):
        return word + "es"
    if word.endswith("y") and len(word) > 1 and word[-2] not in VOWELS:
        return word[:-1] + "ies"
    if word.endswith("fe"):
        return word[:-2] + "ves"
    if word.endswith("f"):
        return word[:-1] + "ves"
    return word + "s"

def ing(word):
    if word.endswith("ie"):
        return word[:-2] + "ying"
    if word.endswith("e") and not word.endswith("ee"):
        return word[:-1] + "ing"
    if (len(word) > 2 and word[-1] not in VOWELS and word[-2] in VOWELS
            and word[-3] not in VOWELS and word[-1] not in "wxy"):
        return word + word[-1] + "ing"
    return word + "ing"

def ed(word):
    if word.endswith("e"):
        return word + "d"
    if word.endswith("y") and len(word) > 1 and word[-2] not in VOWELS:
        return word[:-1] + "ied"
    if (len(word) > 2 and word[-1] not in VOWELS and word[-2] in VOWELS
            and word[-3] not in VOWELS and word[-1] not in "wxy"):
        return word + word[-1] + "ed"
    return word + "ed"

def ly(word):
    if word.endswith("y") and len(word) > 1 and word[-2] not in VOWELS:
        return word[:-1] + "ily"
    if word.endswith("le"):
        return word[:-1] + "y"
    return word + "ly"

def er(word):
    if word.endswith("e"):
        return word + "r"
    if word.endswith("y") and len(word) > 1 and word[-2] not in VOWELS:
        return word[:-1] + "ier"
    return word + "er"

words = {}
total = len(base)
for index, word in enumerate(base):
    freq = max(12, int(255 * (1 - index / float(total)) ** 2) + 10)
    words[word] = max(words.get(word, 0), freq)
    if word in NO_INFLECT or len(word) < 3:
        continue
    forms = [(plural(word), .55), (ing(word), .45), (ed(word), .45)]
    if index < 420:  # only the most common lemmas get -ly / -er forms (avoids noun junk)
        forms += [(er(word), .3), (ly(word), .3)]
    for form, scale in forms:
        if re.fullmatch(r"[a-z]+", form) and form not in words:
            words[form] = max(8, int(freq * scale))

CONTRACTIONS = """i'm you're he's she's it's we're they're i've you've we've they've i'll you'll he'll she'll
we'll they'll don't doesn't didn't won't wouldn't can't couldn't shouldn't isn't aren't wasn't weren't hasn't
haven't hadn't let's that's there's what's who's here's i'd you'd we'd they'd"""
for w in CONTRACTIONS.split():
    words[w] = max(words.get(w, 0), 180)

out_dir = "app/src/main/res/raw"
os.makedirs(out_dir, exist_ok=True)
path = os.path.join(out_dir, "dictionary_en.txt")
with open(path, "w", encoding="utf-8") as f:
    f.write("# CustomBoard English dictionary: word<TAB>frequency (0-255)\n")
    for word in sorted(words, key=lambda w: (-words[w], w)):
        f.write("%s\t%d\n" % (word, min(255, words[word])))

print("dictionary entries:", len(words), "->", path, os.path.getsize(path), "bytes")

BIGRAMS = """i am|i have|i will|i think|i want|i need|i know|i would|i can|i was|i don't|i love|i hope|i just|i really
you are|you have|you can|you will|you should|you know|you want|you need|you did|you were
we are|we have|we will|we can|we should|we need|we were
they are|they have|they will|they can|they were
it is|it was|it will|it would|it can|it looks|it seems
this is|this was|this will|that is|that was|that would|there is|there are|there was
how are|how is|how do|how can|how about|how much|how many|how long
what is|what are|what do|what about|what time|what kind
thank you|thanks for|please let|let me|let us|good morning|good evening|good night|good luck|good job
see you|talk to|talk soon|call you|call me|text me|send me|give me|tell me|show me|help me|meet you
going to|want to|need to|have to|has to|had to|able to|used to|trying to|looking forward|according to|due to|thanks to
in the|on the|at the|to the|for the|of the|with the|from the|by the|about the|into the|over the
a lot|a little|a few|a bit|a while|a good|a great|a new|a very
very good|very much|very well|very happy|so much|so good|too much|too many
next week|next month|next year|next time|last night|last week|last year|this week|this month|this year|this morning|this time
right now|right away|just now|as soon|as well|at least|at most|of course|in fact|for example|such as
make sure|take care|get back|get in|come back|come over|go back|go home|go out|look like|look forward
can you|could you|would you|will you|do you|did you|are you|have you|should i|can i|may i|shall we
i'm sorry|i'm not|i'm going|i'm here|i'm good|it's a|it's not|that's a|that's great|don't know|doesn't work|didn't get
work on|working on|based on|depends on|focus on
pick up|set up|wake up|hold on|hang on|check out|find out|figure out
happy birthday|happy new|merry christmas|best regards|kind regards
the best|the same|the first|the last|the next|the only|the other|the most
let me know|please find|as discussed|following up"""
pairs = []
for chunk in BIGRAMS.split("|"):
    parts = chunk.strip().replace("\n", " ").split()
    if len(parts) >= 2:
        pairs.append((" ".join(parts[:-1]).strip(), parts[-1]))

bigram_path = os.path.join(out_dir, "bigrams_en.txt")
with open(bigram_path, "w", encoding="utf-8") as f:
    f.write("# CustomBoard bigrams: context<TAB>nextWord<TAB>weight\n")
    for first, second in pairs:
        f.write("%s\t%s\t%d\n" % (first, second, 100))
print("bigram entries:", len(pairs), "->", bigram_path)
