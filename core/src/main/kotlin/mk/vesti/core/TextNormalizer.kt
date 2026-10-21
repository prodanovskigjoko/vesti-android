package mk.vesti.core

/** Turns headline text into comparable tokens for Macedonian (Cyrillic or Latin-transliterated). */
object TextNormalizer {
    private const val STEM_LENGTH = 6

    private val latinToCyrillic = listOf(
        "dzh" to "џ", "dž" to "џ", "gj" to "ѓ", "kj" to "ќ", "lj" to "љ", "nj" to "њ", "zh" to "ж",
        "ch" to "ч", "sh" to "ш", "dz" to "ѕ", "ž" to "ж", "č" to "ч", "š" to "ш", "ć" to "ќ", "đ" to "ѓ",
        "a" to "а", "b" to "б", "v" to "в", "g" to "г", "d" to "д", "e" to "е", "z" to "з", "i" to "и",
        "j" to "ј", "k" to "к", "l" to "л", "m" to "м", "n" to "н", "o" to "о", "p" to "п", "r" to "р",
        "s" to "с", "t" to "т", "u" to "у", "f" to "ф", "h" to "х", "c" to "ц",
    )

    private val stopWords = setOf(
        "и", "или", "на", "во", "од", "со", "за", "да", "се", "не", "ќе", "е", "ги", "го", "ја", "ни",
        "му", "им", "ли", "но", "а", "дека", "што", "кој", "која", "кое", "кои", "до", "по", "при",
        "низ", "пред", "меѓу", "сите", "сè", "ова", "тоа", "оваа", "овој", "тој", "таа", "тие", "биде",
        "бил", "била", "биле", "има", "нема", "како", "кога", "каде", "веќе", "уште", "само", "сега",
        "него", "неа", "нив", "тука", "таму", "без", "над", "под", "против", "околу", "според", "денес",
        "вчера", "утре", "година", "години", "нов", "нова", "ново", "нови", "видео", "фото",
    )

    fun tokens(text: String): List<String> {
        val lower = transliterate(text.lowercase())
        return lower.split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 1 && it !in stopWords }
            .map { stem(it) }
    }

    /** Crude stemmer: Macedonian inflects mostly at word endings, so a fixed prefix groups word forms. */
    private fun stem(word: String): String =
        if (word.length > STEM_LENGTH && word.any { !it.isDigit() }) word.take(STEM_LENGTH) else word

    private fun transliterate(text: String): String {
        if (text.none { it in 'a'..'z' || it in "žčšćđ" }) return text
        var out = text
        for ((latin, cyr) in latinToCyrillic) out = out.replace(latin, cyr)
        return out
    }
}
