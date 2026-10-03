package com.customboard.keyboard.emoji

/** Japanese style text emoticons. */
object KaomojiManager {

    val HAPPY = listOf(
        "(◕‿◕)", "(^_^)", "(｡◕‿◕｡)", "(✿◠‿◠)", "ヽ(•‿•)ノ", "(〃＾▽＾〃)", "(＾▽＾)",
        "(•‿•)", "(¬‿¬)", "( ͡° ͜ʖ ͡°)", "(｡♥‿♥｡)", "٩(◕‿◕)۶"
    )

    val SAD = listOf(
        "(︶︹︶)", "(╥_╥)", "(T_T)", "(ಥ﹏ಥ)", "(´;ω;`)", "(っ- ‸ - ς)", "(－‸ლ)", "(◞‸◟)"
    )

    val ANGRY = listOf(
        "(╯°□°）╯︵ ┻━┻", "(ノಠ益ಠ)ノ彡┻━┻", "(¬_¬)", "(ง'̀-'́)ง", "凸(¬‿¬)凸", "(҂⌣̀_⌣́)"
    )

    val LOVE = listOf(
        "(♡°▽°♡)", "(づ￣ ³￣)づ", "(っ˘▽˘)っ", "♡(ӦｖӦ｡)", "(´,,•ω•,,)♡", "(≧◡≦) ♡"
    )

    val MISC = listOf(
        "¯\\_(ツ)_/¯", "ಠ_ಠ", "(ಠ_ಠ)", "┬─┬ノ( º _ ºノ)", "(づ｡◕‿‿◕｡)づ", "ლ(╹◡╹ლ)",
        "(•_•) ( •_•)>⌐■-■ (⌐■_■)", "☜(ﾟヮﾟ☜)", "(☞ﾟヮﾟ)☞", "⊂(◉‿◉)つ", "(~˘▾˘)~"
    )

    val ALL: List<String> = HAPPY + LOVE + SAD + ANGRY + MISC

    fun asEmojiItems(): List<EmojiItem> = ALL.map {
        EmojiItem(it, EmojiManager.CATEGORY_KAOMOJI, "kaomoji emoticon text face")
    }
}
