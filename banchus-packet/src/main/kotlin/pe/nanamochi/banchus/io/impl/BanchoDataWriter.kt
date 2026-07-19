package pe.nanamochi.banchus.io.impl

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import pe.nanamochi.banchus.io.DataWriter

class BanchoDataWriter : DataWriter {
    private fun OutputStream.writeBuffer(size: Int, block: ByteBuffer.() -> Unit) {
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.block()
        this.write(buffer.array())
    }

    override fun writeUint64(out: OutputStream, value: ULong) =
        out.writeBuffer(8) { putLong(value.toLong()) }

    override fun writeInt64(out: OutputStream, value: Long) = writeUint64(out, value.toULong())

    override fun writeUint32(out: OutputStream, value: UInt) =
        out.writeBuffer(4) { putInt(value.toInt()) }

    override fun writeInt32(out: OutputStream, value: Int) = out.writeBuffer(4) { putInt(value) }

    override fun writeUint16(out: OutputStream, value: UShort) =
        out.writeBuffer(2) { putShort(value.toShort()) }

    override fun writeInt16(out: OutputStream, value: Short) =
        out.writeBuffer(2) { putShort(value) }

    override fun writeUint8(out: OutputStream, value: UByte) = out.write(value.toInt())

    override fun writeInt8(out: OutputStream, value: Byte) = writeUint8(out, value.toUByte())

    override fun writeBoolean(out: OutputStream, value: Boolean) = out.write(if (value) 1 else 0)

    override fun writeFloat32(out: OutputStream, value: Float) =
        out.writeBuffer(4) { putFloat(value) }

    override fun writeFloat64(out: OutputStream, value: Double) =
        out.writeBuffer(8) { putDouble(value) }

    override fun writeIntList16(out: OutputStream, values: List<Int>) {
        writeUint16(out, values.size.toUShort())
        values.forEach { writeInt32(out, it) }
    }

    override fun writeIntList32(out: OutputStream, values: List<Int>) {
        writeUint32(out, values.size.toUInt())
        values.forEach { writeInt32(out, it) }
    }

    override fun writeBoolList(out: OutputStream, values: List<Boolean>) {
        require(values.isNotEmpty() && values.size <= 8) { "bool list size must be 1-8" }

        var result = 0
        values.forEachIndexed { i, b ->
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
