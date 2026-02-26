package pe.nanamochi.banchus.service

import java.util.*
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.protocol.PacketWriter

@Service
class MatchBroadcastService(
    private val packetWriter: PacketWriter,
    private val packetBundleService: PacketBundleService,
    private val channelService: ChannelService,
) {}
