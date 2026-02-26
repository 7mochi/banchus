package pe.nanamochi.banchus.core

import java.io.InputStream
import java.io.OutputStream
import pe.nanamochi.banchus.io.DataReader
import pe.nanamochi.banchus.io.DataWriter

abstract class BanchoPacket {
    abstract val type: PacketType

    interface Client {
        val type: PacketType

        fun read(reader: DataReader, ins: InputStream)
    }

    interface Server {
        val type: PacketType

        fun write(writer: DataWriter, out: OutputStream)
    }
}
