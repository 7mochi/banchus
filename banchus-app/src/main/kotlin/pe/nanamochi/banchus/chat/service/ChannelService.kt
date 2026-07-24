package pe.nanamochi.banchus.chat.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toResultOr
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.auth.entity.Session
import pe.nanamochi.banchus.chat.entity.Channel
import pe.nanamochi.banchus.chat.entity.ChannelName
import pe.nanamochi.banchus.chat.repository.ChannelRedisRepository
import pe.nanamochi.banchus.chat.repository.ChannelRepository
import pe.nanamochi.banchus.core.StreamName
import pe.nanamochi.banchus.core.error.ChannelIsUnauthorized
import pe.nanamochi.banchus.core.error.ChannelNotFound
import pe.nanamochi.banchus.core.error.ChannelUserAlreadyIn
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.NotInMatch
import pe.nanamochi.banchus.core.service.StreamService
import pe.nanamochi.banchus.core.util.runDatabaseCatching
import pe.nanamochi.banchus.multiplayer.service.MultiplayerService
import pe.nanamochi.banchus.packets.server.ChannelAvailablePacket
import pe.nanamochi.banchus.protocol.PacketWriter
import pe.nanamochi.banchus.spectator.service.SpectatorService

@Service
class ChannelService(
    private val streamService: StreamService,
    @Lazy private val spectatorService: SpectatorService,
    @Lazy private val multiplayerService: MultiplayerService,
    private val channelRepository: ChannelRepository,
    private val channelRedisRepository: ChannelRedisRepository,
    private val packetWriter: PacketWriter,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getChannelName(session: Session, channelName: String): Result<ChannelName, DomainMessage> =
        binding {
            when (channelName) {
                "#spectator" -> {
                    val hostSessionId =
                        spectatorService.fetchSpectating(session.sessionId) ?: session.sessionId
                    ChannelName.Spectator(hostSessionId)
                }
                "#multiplayer" -> {
                    val matchId =
                        multiplayerService
                            .fetchSessionMatchId(session.sessionId)
                            .toResultOr { NotInMatch }
                            .bind()

                    ChannelName.Multiplayer(matchId)
                }
                else -> ChannelName.from(channelName)
            }
        }

    fun fetchOne(channelName: ChannelName): Result<Channel, DomainMessage> =
        when (channelName) {
            is ChannelName.Spectator -> Ok(Channel.spectator())
            is ChannelName.Multiplayer -> Ok(Channel.multiplayer())
            is ChannelName.Chat -> {
                channelRepository.findByName(channelName.name).toResultOr { ChannelNotFound }
            }
        }

    fun fetchAll(): Result<List<Channel>, DomainMessage> = runDatabaseCatching {
        channelRepository.findAll()
    }

    fun join(
        session: Session,
        channelName: ChannelName,
    ): Result<Pair<Channel, Long>, DomainMessage> = binding {
        val channel = fetchOne(channelName).bind()
        if (!channel.canRead(session.privileges)) {
            Err(ChannelIsUnauthorized).bind()
        }

        val existingChannels = channelRedisRepository.fetchSessionChannels(session.sessionId)
        if (channelName.resolve() in existingChannels) {
            Err(ChannelUserAlreadyIn).bind()
        }

        streamService.join(session.sessionId, channelName.getMessageStream())
        val memberCount = channelRedisRepository.join(session.sessionId, channelName)

        log.info(
            "User {} joined channel {}. Members: {}",
            session.sessionId,
            channelName.resolve(),
            memberCount,
        )

        broadcastChannelInfoUpdate(channelName, channel, memberCount.toInt())

        Pair(channel, memberCount)
    }

    fun leave(
        sessionId: UUID,
        channelName: ChannelName,
    ): Result<Pair<Channel, Long>, DomainMessage> = binding {
        val channel = fetchOne(channelName).bind()
        streamService.leave(sessionId, channelName.getMessageStream())

        val memberCount = channelRedisRepository.leave(sessionId, channelName)
        log.info(
            "User member {} left channel {}. Members: {}",
            sessionId,
            channelName.resolve(),
            memberCount,
        )

        broadcastChannelInfoUpdate(channelName, channel, memberCount.toInt())

        Pair(channel, memberCount)
    }

    fun leaveAll(sessionId: UUID): Result<Unit, DomainMessage> = binding {
        val channels = channelRedisRepository.fetchSessionChannels(sessionId)
        channels.forEach { channel -> leave(sessionId, ChannelName.from(channel)).bind() }
    }

    fun memberCount(channelName: ChannelName): Long =
        channelRedisRepository.memberCount(channelName)

    fun close(channelName: ChannelName): Result<Unit, DomainMessage> = binding {
        val memberIds = channelRedisRepository.fetchChannelMembers(channelName)
        memberIds.forEach { sessionId -> leave(sessionId, channelName).bind() }
    }

    fun broadcastChannelInfoUpdate(channelName: ChannelName, channel: Channel, memberCount: Int) {
        val updateStream = channelName.getUpdateStream()
        val privRule =
            when (updateStream) {
                StreamName.Main -> channel.readPrivileges
                else -> null
            }

        streamService.broadcastData(
            updateStream,
            packetWriter.serialize(
                ChannelAvailablePacket(
                    realName = channel.name,
                    topic = channel.description,
                    userCount = memberCount,
                )
            ),
            null,
            privRule,
        )
    }
}
