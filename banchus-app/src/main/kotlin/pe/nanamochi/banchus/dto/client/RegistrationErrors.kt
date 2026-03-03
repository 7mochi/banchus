package pe.nanamochi.banchus.dto.client

typealias RegistrationErrors = Map<String, List<String>>

fun MutableMap<String, MutableList<String>>.addError(field: String, message: String) {
    this.computeIfAbsent(field) { mutableListOf() }.add(message)
}
