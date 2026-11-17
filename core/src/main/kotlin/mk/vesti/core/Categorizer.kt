package mk.vesti.core

/**
 * Picks the app category for an article. The outlet's own `<category>` labels decide when they are
 * specific ("Свет", "Спорт"). Generic labels ("Вести", "Политика") or no labels at all used to land
 * everything in Македонија, including foreign news, so then the title and summary are checked for
 * foreign places and people with nothing Macedonian beside them.
 */
object Categorizer {
    private val rules: List<Pair<Category, List<String>>> = listOf(
        Category.SPORT to listOf("спорт", "фудбал", "кошарка", "ракомет", "тенис", "sport"),
        Category.EKONOMIJA to listOf("економ", "бизнис", "пазар", "финанс", "berza", "business"),
        Category.SVET to listOf("свет", "балкан", "регион", "европа", "world", "intern"),
        Category.HRONIKA to listOf("хроника", "црна", "сообраќај", "полиција"),
        Category.KULTURA to listOf("култур", "уметност", "книж", "театар"),
        Category.TEHNOLOGIJA to listOf("техно", "наука", "it", "tech", "дигитал"),
        Category.ZABAVA to listOf("забава", "сцена", "шоу", "лајф", "магазин", "живот", "life", "estrada"),
        Category.MAKEDONIJA to listOf("македонија", "скопје", "домашни"),
    )

    fun categorize(labels: List<String>, title: String, summary: String): Category {
        fromLabels(labels)?.let { return it }
        return if (looksForeign("$title $summary")) Category.SVET else Category.MAKEDONIJA
    }

    /**
     * The most specific category any label names, whatever the label order, so "Вести, Свет" is Свет.
     * Generic labels alone give null.
     */
    fun fromLabels(labels: List<String>): Category? {
        val lower = labels.map { it.lowercase() }
        for ((category, keys) in rules) if (lower.any { l -> keys.any { l.contains(it) } }) return category
        return null
    }

    /** Foreign places or people in the text and nothing that ties the story to North Macedonia. */
    fun looksForeign(text: String): Boolean {
        val lower = " " + text.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ") + " "
        fun hits(stems: List<String>) = stems.any { lower.contains(" $it") }
        return hits(FOREIGN) && !hits(DOMESTIC)
    }

    /** Word starts (stems), matched at the beginning of a word. */
    private val FOREIGN = listOf(
        "сад ", "американ", "вашингтон", "трамп", "бајден", "белата куќа", "пентагон",
        "русија", "руск", "путин", "москва", "кремљ", "украин", "киев", "зеленски",
        "кина ", "кинес", "пекинг", "тајван", "јапон", "токио", "северна кореја", "индија", "пакистан",
        "израел", "газа", "хамас", "нетанјаху", "иран", "техеран", "ирак", "сирија", "либан", "египет", "јемен",
        "турција", "турск", "ердоган", "анкара", "истанбул",
        "германија", "германск", "берлин", "франција", "францус", "париз", "макрон",
        "британ", "лондон", "италија", "италијан", "шпанија", "шпанск", "мадрид", "полска", "унгарија", "орбан",
        "австрија", "виена", "швајцарија", "холандија", "белгија", "шведска", "норвешка", "данска",
        "србија", "српск", "белград", "вучиќ", "хрватска", "загреб", "босна", "сараево", "црна гора", "подгорица",
        "косово", "приштина", "албанија", "тирана", "рама ", "бугарија", "бугарск", "софија", "грција", "грчк", "атина",
        "романија", "букурешт", "словенија", "љубљана",
        "канада", "мексико", "бразил", "аргентина", "венецуела", "австралија", "африка",
    )

    private val DOMESTIC = listOf(
        "македониј", "македонск", "скопје", "скопск", "мицкоски", "сиљановска", "собрание", "вмро", "сдсм",
        "левица", "дуи ", "влада на рсм", "рсм ", "мвр", "нбрм", "ујп", "фзо", "денари",
        "битола", "куманово", "тетово", "охрид", "прилеп", "штип", "струмица", "велес", "гостивар", "кочани",
        "кавадарци", "струга", "кичево", "гевгелија", "дебар", "радовиш", "неготино", "ресен", "крива паланка",
    )
}
