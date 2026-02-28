package pe.nanamochi.banchus.io

import java.io.OutputStream

interface DataWriter {
    fun writeUint64(out: OutputStream, v: ULong)

    fun writeInt64(out: OutputStream, v: Long)

    fun writeUint32(out: OutputStream, v: UInt)

    fun writeInt32(out: OutputStream, v: Int)

    fun writeUint16(out: OutputStream, v: UShort)

    fun writeInt16(out: OutputStream, v: Short)

    fun writeUint8(out: OutputStream, v: UByte)

    fun writeInt8(out: OutputStream, v: Byte)

    fun writeBoolean(out: OutputStream, v: Boolean)

    fun writeFloat32(out: OutputStream, v: Float)

    fun writeFloat64(out: OutputStream, v: Double)

    fun writeIntList16(out: OutputStream, list: List<Int>)

    fun writeIntList32(out: OutputStream, list: List<Int>)

    fun writeBoolList(out: OutputStream, bools: List<Boolean>)

    fun writeString(out: OutputStream, value: String?)
}
