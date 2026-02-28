package pe.nanamochi.banchus.protocol

import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.packets.client.CantSpectatePacket
import pe.nanamochi.banchus.packets.client.ChannelJoinPacket
import pe.nanamochi.banchus.packets.client.ChannelLeavePacket
import pe.nanamochi.banchus.packets.client.ExitPacket
import pe.nanamochi.banchus.packets.client.LobbyJoinPacket
import pe.nanamochi.banchus.packets.client.LobbyPartPacket
import pe.nanamochi.banchus.packets.client.MatchChangeModsPacket
import pe.nanamochi.banchus.packets.client.MatchChangePasswordPacket
import pe.nanamochi.banchus.packets.client.MatchChangeSettingsPacket
import pe.nanamochi.banchus.packets.client.MatchChangeSlotPacket
import pe.nanamochi.banchus.packets.client.MatchCompletePacket
import pe.nanamochi.banchus.packets.client.MatchCreatePacket
import pe.nanamochi.banchus.packets.client.MatchFailedPacket
import pe.nanamochi.banchus.packets.client.MatchHasBeatmapPacket
import pe.nanamochi.banchus.packets.client.MatchJoinPacket
import pe.nanamochi.banchus.packets.client.MatchLoadCompletePacket
import pe.nanamochi.banchus.packets.client.MatchLockPacket
import pe.nanamochi.banchus.packets.client.MatchNoBeatmapPacket
import pe.nanamochi.banchus.packets.client.MatchNotReadyPacket
import pe.nanamochi.banchus.packets.client.MatchPartPacket
import pe.nanamochi.banchus.packets.client.MatchReadyPacket
import pe.nanamochi.banchus.packets.client.MatchScoreUpdatePacket
import pe.nanamochi.banchus.packets.client.MatchSkipRequestPacket
import pe.nanamochi.banchus.packets.client.MatchStartPacket
import pe.nanamochi.banchus.packets.client.MessagePacket
import pe.nanamochi.banchus.packets.client.PongPacket
import pe.nanamochi.banchus.packets.client.PrivateMessagePacket
import pe.nanamochi.banchus.packets.client.SpectateFramesPacket
import pe.nanamochi.banchus.packets.client.StartSpectatingPacket
import pe.nanamochi.banchus.packets.client.StatusUpdateRequestPacket
import pe.nanamochi.banchus.packets.client.StopSpectatingPacket
import pe.nanamochi.banchus.packets.client.UserStatsRequestPacket
import pe.nanamochi.banchus.packets.client.UserStatusPacket

object PacketRegistry {
    private val factories = mutableMapOf<PacketType, () -> BanchoPacket.Client>()

    init {
        register(PacketType.OSU_CANT_SPECTATE, ::CantSpectatePacket)
        register(PacketType.OSU_CHANNEL_JOIN, ::ChannelJoinPacket)
        register(PacketType.OSU_CHANNEL_LEAVE, ::ChannelLeavePacket)
        register(PacketType.OSU_EXIT, ::ExitPacket)
        register(PacketType.OSU_LOBBY_JOIN, ::LobbyJoinPacket)
        register(PacketType.OSU_LOBBY_PART, ::LobbyPartPacket)
        register(PacketType.OSU_MATCH_CHANGE_MODS, ::MatchChangeModsPacket)
        register(PacketType.OSU_MATCH_CHANGE_PASSWORD, ::MatchChangePasswordPacket)
        register(PacketType.OSU_MATCH_CHANGE_SETTINGS, ::MatchChangeSettingsPacket)
        register(PacketType.OSU_MATCH_CHANGE_SLOT, ::MatchChangeSlotPacket)
        register(PacketType.OSU_MATCH_COMPLETE, ::MatchCompletePacket)
        register(PacketType.OSU_MATCH_CREATE, ::MatchCreatePacket)
        register(PacketType.OSU_MATCH_FAILED, ::MatchFailedPacket)
        register(PacketType.OSU_MATCH_HAS_BEATMAP, ::MatchHasBeatmapPacket)
        register(PacketType.OSU_MATCH_JOIN, ::MatchJoinPacket)
        register(PacketType.OSU_MATCH_LOAD_COMPLETE, ::MatchLoadCompletePacket)
        register(PacketType.OSU_MATCH_LOCK, ::MatchLockPacket)
        register(PacketType.OSU_MATCH_NO_BEATMAP, ::MatchNoBeatmapPacket)
        register(PacketType.OSU_MATCH_NOT_READY, ::MatchNotReadyPacket)
        register(PacketType.OSU_MATCH_PART, ::MatchPartPacket)
        register(PacketType.OSU_MATCH_READY, ::MatchReadyPacket)
        register(PacketType.OSU_MATCH_SCORE_UPDATE, ::MatchScoreUpdatePacket)
        register(PacketType.OSU_MATCH_SKIP_REQUEST, ::MatchSkipRequestPacket)
        register(PacketType.OSU_MATCH_START, ::MatchStartPacket)
        register(PacketType.OSU_MESSAGE, ::MessagePacket)
        register(PacketType.OSU_PONG, ::PongPacket)
        register(PacketType.OSU_PRIVATE_MESSAGE, ::PrivateMessagePacket)
        register(PacketType.OSU_SPECTATE_FRAMES, ::SpectateFramesPacket)
        register(PacketType.OSU_START_SPECTATING, ::StartSpectatingPacket)
        register(PacketType.OSU_STATUS_UPDATE_REQUEST, ::StatusUpdateRequestPacket)
        register(PacketType.OSU_STOP_SPECTATING, ::StopSpectatingPacket)
        register(PacketType.OSU_USER_STATS_REQUEST, ::UserStatsRequestPacket)
        register(PacketType.OSU_USER_STATUS, ::UserStatusPacket)
    }

    private fun register(type: PacketType, factory: () -> BanchoPacket.Client) {
        factories[type] = factory
    }

    fun getFactories(): Map<PacketType, () -> BanchoPacket.Client> = factories
}
