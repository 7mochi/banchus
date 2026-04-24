package pe.nanamochi.banchus.core

import java.io.InputStream
import java.io.OutputStream
import pe.nanamochi.banchus.io.DataReader
import pe.nanamochi.banchus.io.DataWriter

abstract class BanchoPacket {
    abstract val type: PacketType
}

abstract class ClientPacket : BanchoPacket() {
    abstract fun read(reader: DataReader, ins: InputStream)
}

abstract class ServerPacket : BanchoPacket() {
    abstract fun write(writer: DataWriter, out: OutputStream)
}
