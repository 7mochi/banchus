package pe.nanamochi.banchus.domain.enums

enum class ScoreSubmissionErrors(val value: String) {
    HANDLE_PASSWORD_RESET("reset"),
    REQUIRE_VERIFICATION("verify"),
    NO_SUCH_USER("nouser"),
    NEEDS_AUTHENTICATION("pass"),
    ACCOUNT_INACTIVE("inactive"),
    ACCOUNT_BANNED("ban"),
    BEATMAP_UNRANKED("beatmap"),
    MODE_OR_MODS_DISABLED("disabled"),
    OLD_OSU_VERSION("oldver"),
    NO("no");

    override fun toString(): String = value
}
