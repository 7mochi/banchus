package pe.nanamochi.banchus.io

import java.io.OutputStream

interface DataWriter {
    fun writeUint64(out: OutputStream, value: ULong)

    fun writeInt64(out: OutputStream, value: Long)

    fun writeUint32(out: OutputStream, value: UInt)

    fun writeInt32(out: OutputStream, value: Int)

    fun writeUint16(out: OutputStream, value: UShort)

    fun writeInt16(out: OutputStream, value: Short)

    fun writeUint8(out: OutputStream, value: UByte)

    fun writeInt8(out: OutputStream, value: Byte)

    fun writeBoolean(out: OutputStream, value: Boolean)

    fun writeFloat32(out: OutputStream, value: Float)

    fun writeFloat64(out: OutputStream, value: Double)

    fun writeIntList16(out: OutputStream, values: List<Int>)

    fun writeIntList32(out: OutputStream, values: List<Int>)

    fun writeBoolList(out: OutputStream, values: List<Boolean>)

    fun writeString(out: OutputStream, value: String?)
}
