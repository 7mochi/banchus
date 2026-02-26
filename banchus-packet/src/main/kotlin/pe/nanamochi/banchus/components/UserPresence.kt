package pe.nanamochi.banchus.components

data class UserPresence(
    var utcOffset: UByte = 0u,
    var country: UByte = 0u,
    var permissions: UByte = 0u,
    var latitude: Float = 0f,
    var longitude: Float = 0f,
)
