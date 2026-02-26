package pe.nanamochi.banchus.protocol

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import pe.nanamochi.banchus.core.BanchoPacket
import pe.nanamochi.banchus.io.BanchoDataWriter
import pe.nanamochi.banchus.io.DataWriter

class PacketWriter {
    private val writer: DataWriter = BanchoDataWriter()

    fun serialize(packet: BanchoPacket.Server): ByteArray =
        ByteArrayOutputStream().use { out ->
            writeSinglePacket(out, packet)
            out.toByteArray()
        }

    fun serializeAll(packets: List<BanchoPacket.Server>): ByteArray =
        ByteArrayOutputStream().use { out ->
            packets.forEach { writeSinglePacket(out, it) }
            out.toByteArray()
        }

    private fun writeSinglePacket(out: OutputStream, packet: BanchoPacket.Server) {
        val packetData =
            ByteArrayOutputStream().use { body ->
                packet.write(writer, body)
                body.toByteArray()
            }

        writer.writeUint16(out, packet.type.id)
        writer.writeUint8(out, 0u)
        writer.writeInt32(out, packetData.size)
        out.write(packetData)
    }
}
