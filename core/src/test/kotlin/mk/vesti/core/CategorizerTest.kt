package mk.vesti.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CategorizerTest {
    private fun cat(title: String, vararg labels: String) = Categorizer.categorize(labels.toList(), title, "")

    @Test
    fun foreignNewsUnderAGenericLabelGoesToSvet() {
        assertEquals(Category.SVET, cat("Трамп се сретна со Путин во Алјаска", "Вести"))
        assertEquals(Category.SVET, cat("Нови напади во Газа, Израел ја прошири операцијата"))
        assertEquals(Category.SVET, cat("Вучиќ: Србија нема да воведе санкции", "Политика"))
    }

    @Test
    fun foreignNamesBesideMacedoniaStayMakedonija() {
        assertEquals(Category.MAKEDONIJA, cat("Мицкоски во Берлин разговараше за евроинтеграциите"))
        assertEquals(Category.MAKEDONIJA, cat("Бугарија пак ги блокира преговорите на Македонија", "Вести"))
        assertEquals(Category.MAKEDONIJA, cat("Собранието ја донесе одлуката за буџетот"))
    }

    @Test
    fun specificLabelWinsWhateverItsPosition() {
        assertEquals(Category.SVET, cat("Избори во регионот", "Вести", "Свет"))
        assertEquals(Category.SPORT, cat("Вардар победи", "Најнови", "Фудбал"))
        // "сад" as a word start must not catch "сада" etc.; "Садовска" is not the USA.
        assertEquals(Category.MAKEDONIJA, cat("Садовска ја отвори изложбата"))
    }
}
