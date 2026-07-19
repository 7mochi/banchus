package pe.nanamochi.banchus.io.impl

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import pe.nanamochi.banchus.io.DataReader

class BanchoDataReader : DataReader {
    private fun InputStream.readBuffer(size: Int): ByteBuffer {
        val bytes = readNBytes(size)
        if (bytes.size != size) throw RuntimeException("Failed to read $size bytes")
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    }

    override fun readUint64(ins: InputStream): ULong = ins.readBuffer(8).long.toULong()

    override fun readInt64(ins: InputStream): Long = ins.readBuffer(8).long

    override fun readUint32(ins: InputStream): UInt = ins.readBuffer(4).int.toUInt()

    override fun readInt32(ins: InputStream): Int = ins.readBuffer(4).int

    override fun readUint16(ins: InputStream): UShort = ins.readBuffer(2).short.toUShort()

    override fun readInt16(ins: InputStream): Short = ins.readBuffer(2).short

    override fun readUint8(ins: InputStream): UByte {
        val byte = ins.read()
        if (byte == -1) throw RuntimeException("Unexpected end of stream")
        return byte.toUByte()
    }

    override fun readInt8(ins: InputStream): Byte = readUint8(ins).toByte()

    override fun readBoolean(ins: InputStream): Boolean = readUint8(ins).toInt() != 0

    override fun readFloat32(ins: InputStream): Float = ins.readBuffer(4).float

    override fun readFloat64(ins: InputStream): Double = ins.readBuffer(8).double

    override fun readIntList16(ins: InputStream): IntArray =
        IntArray(readUint16(ins).toInt()) { readInt32(ins) }

    override fun readIntList32(ins: InputStream): IntArray =
        IntArray(readUint32(ins).toInt()) { readInt32(ins) }

    override fun readBoolList(ins: InputStream): List<Boolean> {
        val input = readUint8(ins).toInt()

        return List(8) { i -> ((input shr i) and 1) != 0 }
    }

    override fun readString(ins: InputStream): String {
        if (readUint8(ins).toInt() != 0x0b) return ""
        val length = uleb128Decode(ins)
        if (length == 0) return ""

        return String(ins.readNBytes(length), StandardCharsets.UTF_8)
    }

    private fun uleb128Decode(ins: InputStream): Int {
        var value = 0
        var shift = 0
        var byte: Int
        do {
            byte = readUint8(ins).toInt()
            value = value or ((byte and 0x7F) shl shift)
            shift += 7
        } while ((byte and 0x80) != 0)
        return value
    }
}
