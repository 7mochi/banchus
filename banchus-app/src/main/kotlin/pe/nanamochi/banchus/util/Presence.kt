package pe.nanamochi.banchus.util

import pe.nanamochi.banchus.components.Mode
import pe.nanamochi.banchus.components.Mods
import pe.nanamochi.banchus.components.User
import pe.nanamochi.banchus.components.UserPresence
import pe.nanamochi.banchus.components.UserStats
import pe.nanamochi.banchus.components.UserStatus
import pe.nanamochi.banchus.core.ServerPacket
import pe.nanamochi.banchus.packets.server.UserPresencePacket
import pe.nanamochi.banchus.packets.server.UserStatsPacket
import pe.nanamochi.banchus.redis.entity.Presence

fun Presence.userPanel(): List<ServerPacket> {
    return buildList {
        add(UserPresencePacket(toBanchoUser()))
        add(UserStatsPacket(toBanchoUser()))
    }
}

fun Presence.toBanchoUser(): User =
    User(
        id = userId,
        username = username,
        globalRank = globalRank.toInt(),
        presence =
            UserPresence(
                utcOffset = utcOffset.toUByte(),
                country = countryCode.id.toUByte(),
                permissions = ((privileges.toClientPrivileges()) or (mode.toInt() shl 5)).toUByte(),
                latitude = latitude,
                longitude = longitude,
            ),
        stats =
            UserStats(
                globalRank = globalRank,
                rankedScore = rankedScore,
                totalScore = totalScore,
                accuracy = accuracy.toFloat(),
                playCount = playcount,
                performancePoints = performancePoints.toUShort(),
            ),
        status =
            UserStatus(
                action = action,
                infoText = infoText,
                beatmapMd5 = beatmapMd5,
                mods = Mods.fromBitmask(mods.toUInt()),
                gamemode = Mode.fromValue(mode),
            ),
    )
