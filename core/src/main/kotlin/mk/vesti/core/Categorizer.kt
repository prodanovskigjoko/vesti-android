package mk.vesti.core

/** Maps an outlet's own category labels (from RSS `<category>`) onto the app's categories. */
object Categorizer {
    private val rules: List<Pair<Category, List<String>>> = listOf(
        Category.SPORT to listOf("спорт", "фудбал", "кошарка", "ракомет", "тенис", "sport"),
        Category.EKONOMIJA to listOf("економ", "бизнис", "пазар", "финанс", "berza", "business"),
        Category.SVET to listOf("свет", "балкан", "регион", "европа", "world", "intern"),
        Category.HRONIKA to listOf("хроника", "црна", "сообраќај", "полиција"),
        Category.KULTURA to listOf("култур", "уметност", "книж", "театар"),
        Category.TEHNOLOGIJA to listOf("техно", "наука", "it", "tech", "дигитал"),
        Category.ZABAVA to listOf("забава", "сцена", "шоу", "лајф", "магазин", "живот", "life", "estrada"),
        Category.MAKEDONIJA to listOf("македонија", "политика", "општество", "скопје", "вести", "домашни"),
    )

    fun fromLabels(labels: List<String>): Category? {
        for (label in labels) {
            val l = label.lowercase()
            for ((category, keys) in rules) if (keys.any { l.contains(it) }) return category
        }
        return null
    }
}
