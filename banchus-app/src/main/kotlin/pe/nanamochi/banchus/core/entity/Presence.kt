package pe.nanamochi.banchus.core.entity

import pe.nanamochi.banchus.core.enums.CountryCode
import pe.nanamochi.banchus.core.enums.ServerPrivileges

data class Presence(
    var userId: Int = 0,
    var username: String = "",
    var privileges: Int = 0,
    var action: UByte = 0u,
    var infoText: String = "",
    var beatmapMd5: String = "",
    var beatmapId: Int = 0,
    var mods: Int = 0,
    var mode: UByte = 0u,
    var rankedScore: ULong = 0u,
    var totalScore: ULong = 0u,
    var accuracy: Double = 0.0,
    var playcount: UInt = 0u,
    var performancePoints: UInt = 0u,
    var globalRank: UInt = 0u,
    var countryCode: CountryCode = CountryCode.XX,
    var latitude: Float = 0.0f,
    var longitude: Float = 0.0f,
    var utcOffset: Int = 0,
) {
    val isRestricted: Boolean
        get() = !ServerPrivileges.fromBitmask(privileges).contains(ServerPrivileges.UNRESTRICTED)

    companion object {
        const val BOT_ID = 1
        const val BOT_NAME = "BanchoBot"

        fun botPresence(): Presence =
            Presence(
                userId = BOT_ID,
                username = BOT_NAME,
                utcOffset = 0,
                countryCode = CountryCode.fromCode(CountryCode.XX.code),
                privileges = 3,
                latitude = 69.69f,
                longitude = 133.7f,
                action = 12u,
                infoText = "some stuff",
                beatmapMd5 = "",
                beatmapId = 0,
                mods = 0,
                mode = 0u,
                rankedScore = 0u,
                totalScore = 1337420691337uL * 24uL,
                accuracy = 4.2 * 100.0,
                playcount = 1337u,
                performancePoints = 1337u,
                globalRank = Int.MAX_VALUE.toUInt(),
            )
    }
}
