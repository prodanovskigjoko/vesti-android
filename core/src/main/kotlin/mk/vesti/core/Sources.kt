package mk.vesti.core

/**
 * Default feeds. The URLs follow each outlet's usual RSS location but have not all been
 * checked from a device yet; a feed that fails is skipped, so a wrong URL only drops that outlet.
 */
val DefaultSources: List<FeedSource> = listOf(
    FeedSource("А1он", "https://a1on.mk/feed/"),
    FeedSource("Телма", "https://telma.com.mk/feed/"),
    FeedSource("Сител", "https://sitel.com.mk/rss.xml"),
    FeedSource("МИА", "https://mia.mk/feed"),
    FeedSource("Нова Македонија", "https://novamakedonija.com.mk/feed/"),
    FeedSource("Слободен печат", "https://www.slobodenpecat.mk/feed/"),
    FeedSource("Плусинфо", "https://plusinfo.mk/feed/"),
    FeedSource("Мета", "https://meta.mk/feed/"),
    FeedSource("Фактор", "https://faktor.mk/feed"),
    FeedSource("Макфакс", "https://makfax.com.mk/feed/"),
    FeedSource("360 степени", "https://360stepeni.mk/feed/"),
    FeedSource("Курир", "https://kurir.mk/feed/"),
    FeedSource("Фокус", "https://fokus.mk/feed/"),
    FeedSource("Вечер", "https://vecer.press/feed/"),
    FeedSource("Нетпрес", "https://netpress.com.mk/feed/"),
    FeedSource("Либертас", "https://www.libertas.mk/feed/"),
    FeedSource("Радио МОФ", "https://www.radiomof.mk/feed/"),
    FeedSource("Спорт1", "https://sport1.mk/feed/", Category.SPORT),
)
