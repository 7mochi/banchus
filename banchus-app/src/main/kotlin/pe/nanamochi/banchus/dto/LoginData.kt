package pe.nanamochi.banchus.dto

data class LoginData(
    val username: String,
    val passwordMd5: String,
    val osuVersion: String,
    val utcOffset: Int,
    val displayCity: Boolean,
    val pmPrivate: Boolean,
    val osuPathMd5: String,
    val adaptersStr: String,
    val adaptersMd5: String,
    val uninstallMd5: String,
    val diskSignatureMd5: String,
)
