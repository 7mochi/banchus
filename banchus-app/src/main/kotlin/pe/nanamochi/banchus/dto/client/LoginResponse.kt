package pe.nanamochi.banchus.dto.client

data class LoginResponse(val token: String, val payload: ByteArray, val success: Boolean) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoginResponse) return false
        if (token != other.token) return false
        if (!payload.contentEquals(other.payload)) return false
        if (success != other.success) return false
        return true
    }

    override fun hashCode(): Int {
        var result = success.hashCode()
        result = 31 * result + token.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
