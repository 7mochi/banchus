package pe.nanamochi.banchus.util

private val USERNAME_REGEX = Regex("^[\\w \\[\\]-]{2,15}$")
private val EMAIL_REGEX =
    Regex(
        "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@" +
            "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$"
    )

fun String?.isValidUsername(): Boolean {
    if (this.isNullOrBlank()) return false

    if (this.contains(" ") && this.contains("_")) return false

    return USERNAME_REGEX.matches(this)
}

fun String?.isValidEmail(): Boolean = this != null && !this.isBlank() && EMAIL_REGEX.matches(this)

fun String?.isValidPassword(): Boolean {
    val password = this ?: return false

    if (password.length !in 8..32) return false

    return password.chars().distinct().count() > 3
}
