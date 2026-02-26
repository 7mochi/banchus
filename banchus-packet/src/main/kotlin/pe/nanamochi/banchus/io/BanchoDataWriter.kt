package pe.nanamochi.banchus.io

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class BanchoDataWriter : DataWriter {
    private fun OutputStream.writeBuffer(size: Int, block: ByteBuffer.() -> Unit) {
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.block()
        this.write(buffer.array())
    }

    override fun writeUint64(out: OutputStream, v: ULong) =
        out.writeBuffer(8) { putLong(v.toLong()) }

    override fun writeInt64(out: OutputStream, v: Long) = writeUint64(out, v.toULong())

    override fun writeUint32(out: OutputStream, v: UInt) = out.writeBuffer(4) { putInt(v.toInt()) }

    override fun writeInt32(out: OutputStream, v: Int) = out.writeBuffer(4) { putInt(v) }

    override fun writeUint16(out: OutputStream, v: UShort) =
        out.writeBuffer(2) { putShort(v.toShort()) }

    override fun writeInt16(out: OutputStream, v: Short) = out.writeBuffer(2) { putShort(v) }

    override fun writeUint8(out: OutputStream, v: UByte) = out.write(v.toInt())

    override fun writeInt8(out: OutputStream, v: Byte) = writeUint8(out, v.toUByte())

    override fun writeBoolean(out: OutputStream, v: Boolean) = out.write(if (v) 1 else 0)

    override fun writeFloat32(out: OutputStream, v: Float) = out.writeBuffer(4) { putFloat(v) }

    override fun writeFloat64(out: OutputStream, v: Double) = out.writeBuffer(8) { putDouble(v) }

    override fun writeIntList16(out: OutputStream, list: List<Int>) {
        writeUint16(out, list.size.toUShort())
        list.forEach { writeInt32(out, it) }
    }

    override fun writeIntList32(out: OutputStream, list: List<Int>) {
        writeUint32(out, list.size.toUInt())
        list.forEach { writeInt32(out, it) }
    }

    override fun writeBoolList(out: OutputStream, bools: List<Boolean>) {
        require(bools.isNotEmpty() && bools.size <= 8) { "bool list size must be 1-8" }

        var result = 0
        bools.forEachIndexed { i, b ->
            if (b) {
                result = result or (1 shl i)
            }
        }
        writeUint8(out, result.toUByte())
    }

    override fun writeString(out: OutputStream, value: String?) {
        if (value.isNullOrEmpty()) {
            writeUint8(out, 0x00u)
            return
        }
        writeUint8(out, 0x0bu)
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        out.write(uleb128Encode(bytes.size))
        out.write(bytes)
    }

    private fun uleb128Encode(value: Int): ByteArray =
        ByteArrayOutputStream()
            .apply {
                var v = value
                do {
                    var b = (v and 0x7F)
                    v = v ushr 7
                    if (v != 0) b = b or 0x80
                    write(b)
                } while (v != 0)
            }
            .toByteArray()
}
