package pe.nanamochi.banchus.beatmap.enums

enum class OsuDirectQuery(val query: String, val sort: String) {
    NEWEST("Newest", "ranked_date:desc"),
    TOP_RATED("Top Rated", "favourite_count:desc"),
    MOST_PLAYED("Most Played", "play_count:desc");

    companion object {
        fun fromQuery(query: String?): OsuDirectQuery? {
            if (query == null) return null
            val normalizedQuery = query.trim().lowercase()
            return entries.find { it.query.lowercase() == normalizedQuery }
        }
    }
}
