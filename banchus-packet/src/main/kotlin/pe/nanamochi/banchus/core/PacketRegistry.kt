package pe.nanamochi.banchus.core

import pe.nanamochi.banchus.packets.client.CantSpectatePacket
import pe.nanamochi.banchus.packets.client.ChangeStatusPacket
import pe.nanamochi.banchus.packets.client.ChannelJoinPacket
import pe.nanamochi.banchus.packets.client.ChannelLeavePacket
import pe.nanamochi.banchus.packets.client.CreateMatchPacket
import pe.nanamochi.banchus.packets.client.ExitPacket
import pe.nanamochi.banchus.packets.client.JoinLobbyPacket
import pe.nanamochi.banchus.packets.client.JoinMatchPacket
import pe.nanamochi.banchus.packets.client.LeaveMatchPacket
import pe.nanamochi.banchus.packets.client.MatchChangeModsPacket
import pe.nanamochi.banchus.packets.client.MatchChangePasswordPacket
import pe.nanamochi.banchus.packets.client.MatchChangeSettingsPacket
import pe.nanamochi.banchus.packets.client.MatchChangeSlotPacket
import pe.nanamochi.banchus.packets.client.MatchChangeTeamPacket
import pe.nanamochi.banchus.packets.client.MatchCompletePacket
import pe.nanamochi.banchus.packets.client.MatchFailedPacket
import pe.nanamochi.banchus.packets.client.MatchHasBeatmapPacket
import pe.nanamochi.banchus.packets.client.MatchInvitePacket
import pe.nanamochi.banchus.packets.client.MatchLoadCompletePacket
import pe.nanamochi.banchus.packets.client.MatchLockPacket
import pe.nanamochi.banchus.packets.client.MatchNoBeatmapPacket
import pe.nanamochi.banchus.packets.client.MatchNotReadyPacket
import pe.nanamochi.banchus.packets.client.MatchReadyPacket
import pe.nanamochi.banchus.packets.client.MatchScoreUpdatePacket
import pe.nanamochi.banchus.packets.client.MatchSkipPacket
import pe.nanamochi.banchus.packets.client.MatchStartPacket
import pe.nanamochi.banchus.packets.client.MatchTransferHostPacket
import pe.nanamochi.banchus.packets.client.MessagePacket
import pe.nanamochi.banchus.packets.client.PartLobbyPacket
import pe.nanamochi.banchus.packets.client.PongPacket
import pe.nanamochi.banchus.packets.client.PresenceRequestAllPacket
import pe.nanamochi.banchus.packets.client.PresenceRequestPacket
import pe.nanamochi.banchus.packets.client.PrivateMessagePacket
import pe.nanamochi.banchus.packets.client.RequestStatusPacket
import pe.nanamochi.banchus.packets.client.SpectateFramesPacket
import pe.nanamochi.banchus.packets.client.StartSpectatingPacket
import pe.nanamochi.banchus.packets.client.StopSpectatingPacket
import pe.nanamochi.banchus.packets.client.TournamentJoinMatchChannelPacket
import pe.nanamochi.banchus.packets.client.TournamentLeaveMatchChannelPacket
import pe.nanamochi.banchus.packets.client.TournamentMatchInfoPacket
import pe.nanamochi.banchus.packets.client.UserStatsRequestPacket

object PacketRegistry {
    private val factories = mutableMapOf<PacketType, () -> ClientPacket>()

    init {
        register(PacketType.OSU_CANT_SPECTATE, ::CantSpectatePacket)
        register(PacketType.OSU_CHANNEL_JOIN, ::ChannelJoinPacket)
        register(PacketType.OSU_CHANNEL_LEAVE, ::ChannelLeavePacket)
        register(PacketType.OSU_EXIT, ::ExitPacket)
        register(PacketType.OSU_JOIN_LOBBY, ::JoinLobbyPacket)
        register(PacketType.OSU_PART_LOBBY, ::PartLobbyPacket)
        register(PacketType.OSU_MATCH_CHANGE_MODS, ::MatchChangeModsPacket)
        register(PacketType.OSU_MATCH_CHANGE_PASSWORD, ::MatchChangePasswordPacket)
        register(PacketType.OSU_MATCH_CHANGE_SETTINGS, ::MatchChangeSettingsPacket)
        register(PacketType.OSU_MATCH_CHANGE_SLOT, ::MatchChangeSlotPacket)
        register(PacketType.OSU_MATCH_CHANGE_TEAM, ::MatchChangeTeamPacket)
        register(PacketType.OSU_MATCH_COMPLETE, ::MatchCompletePacket)
        register(PacketType.OSU_CREATE_MATCH, ::CreateMatchPacket)
        register(PacketType.OSU_MATCH_FAILED, ::MatchFailedPacket)
        register(PacketType.OSU_MATCH_HAS_BEATMAP, ::MatchHasBeatmapPacket)
        register(PacketType.OSU_JOIN_MATCH, ::JoinMatchPacket)
        register(PacketType.OSU_MATCH_LOAD_COMPLETE, ::MatchLoadCompletePacket)
        register(PacketType.OSU_MATCH_LOCK, ::MatchLockPacket)
        register(PacketType.OSU_MATCH_NO_BEATMAP, ::MatchNoBeatmapPacket)
        register(PacketType.OSU_MATCH_NOT_READY, ::MatchNotReadyPacket)
        register(PacketType.OSU_LEAVE_MATCH, ::LeaveMatchPacket)
        register(PacketType.OSU_MATCH_INVITE, ::MatchInvitePacket)
        register(PacketType.OSU_MATCH_READY, ::MatchReadyPacket)
        register(PacketType.OSU_MATCH_SCORE_UPDATE, ::MatchScoreUpdatePacket)
        register(PacketType.OSU_MATCH_SKIP, ::MatchSkipPacket)
        register(PacketType.OSU_MATCH_START, ::MatchStartPacket)
        register(PacketType.OSU_MATCH_TRANSFER_HOST, ::MatchTransferHostPacket)
        register(PacketType.OSU_MESSAGE, ::MessagePacket)
        register(PacketType.OSU_PONG, ::PongPacket)
        register(PacketType.OSU_PRESENCE_REQUEST, ::PresenceRequestPacket)
        register(PacketType.OSU_PRESENCE_REQUEST_ALL, ::PresenceRequestAllPacket)
        register(PacketType.OSU_PRIVATE_MESSAGE, ::PrivateMessagePacket)
        register(PacketType.OSU_SPECTATE_FRAMES, ::SpectateFramesPacket)
        register(PacketType.OSU_START_SPECTATING, ::StartSpectatingPacket)
        register(PacketType.OSU_REQUEST_STATUS, ::RequestStatusPacket)
        register(PacketType.OSU_STOP_SPECTATING, ::StopSpectatingPacket)
        register(PacketType.OSU_USER_STATS_REQUEST, ::UserStatsRequestPacket)
        register(PacketType.OSU_CHANGE_STATUS, ::ChangeStatusPacket)
        register(PacketType.OSU_TOURNAMENT_JOIN_MATCH_CHANNEL, ::TournamentJoinMatchChannelPacket)
        register(PacketType.OSU_TOURNAMENT_LEAVE_MATCH_CHANNEL, ::TournamentLeaveMatchChannelPacket)
        register(PacketType.OSU_TOURNAMENT_MATCH_INFO, ::TournamentMatchInfoPacket)
    }

    private fun register(type: PacketType, factory: () -> ClientPacket) {
        factories[type] = factory
    }

    fun getFactories(): Map<PacketType, () -> ClientPacket> = factories
}
