package pe.nanamochi.banchus.mapper

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.springframework.stereotype.Component
import pe.nanamochi.banchus.components.Mode
import pe.nanamochi.banchus.components.Mods
import pe.nanamochi.banchus.components.User
import pe.nanamochi.banchus.components.UserPresence
import pe.nanamochi.banchus.components.UserStats
import pe.nanamochi.banchus.components.UserStatus
import pe.nanamochi.banchus.database.entity.Session
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.domain.errors.DomainMessage
import pe.nanamochi.banchus.domain.errors.UserNotFound
import pe.nanamochi.banchus.service.RankingService
import pe.nanamochi.banchus.util.toClientPrivileges

@Component
class BanchoUserMapper(private val rankingService: RankingService) {
    fun toPacketUser(
        session: Session,
        stats: Stat,
        forcedRank: Int? = 0,
    ): Result<User, DomainMessage> {
        val dbUser = session.user ?: return Err(UserNotFound)
        val globalRank =
            forcedRank ?: rankingService.getGlobalRank(session.gamemode, dbUser).toInt()

        return Ok(
            User(
                id = dbUser.id,
                username = dbUser.username,
                globalRank = globalRank,
                presence =
                    UserPresence(
                        utcOffset = (session.utcOffset + 24).toUByte(),
                        country = session.country.id.toUByte(),
                        permissions = dbUser.privileges.toClientPrivileges().toUByte(),
                        latitude = session.latitude,
                        longitude = session.longitude,
                    ),
                stats =
                    UserStats(
                        globalRank = globalRank.toUInt(),
                        rankedScore = stats.rankedScore.toULong(),
                        totalScore = stats.totalScore.toULong(),
                        accuracy = stats.accuracy.toFloat(),
                        playCount = stats.playCount.toUInt(),
                        performancePoints = stats.performancePoints.toUShort(),
                    ),
                status =
                    UserStatus(
                        action = session.action.toUByte(),
                        infoText = session.infoText,
                        beatmapMd5 = session.beatmapMd5,
                        mods = Mods.fromBitmask(session.mods.toUInt()),
                        gamemode = Mode.fromValue(session.gamemode.value),
                    ),
            )
        )
    }
}
