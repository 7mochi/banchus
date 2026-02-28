package pe.nanamochi.banchus.protocol

import java.io.ByteArrayInputStream
import java.io.InputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.core.PacketType
import pe.nanamochi.banchus.io.BanchoDataReader
import pe.nanamochi.banchus.io.DataReader

class PacketReader {
    private val reader: DataReader = BanchoDataReader()
    private val packetFactories = PacketRegistry.getFactories()

    fun readPackets(data: ByteArray): List<BanchoPacket> {
        val bis = ByteArrayInputStream(data)
        val packets = mutableListOf<BanchoPacket>()

        while (bis.available() > 0) {
            val packet = readSinglePacket(bis)
            if (packet != null) packets.add(packet)
        }
        return packets
    }

    private fun readSinglePacket(ins: InputStream): BanchoPacket? {
        val packetId = reader.readUint16(ins)
        reader.readUint8(ins) // padding
        val length = reader.readInt32(ins)
        val data = ins.readNBytes(length)

        val type = PacketType.fromId(packetId) ?: return null
        val factory = packetFactories[type] ?: return null

        val packet = factory()
        packet.read(reader, ByteArrayInputStream(data))

        return packet as? BanchoPacket
    }
}
