package pe.nanamochi.banchus.io

import java.io.InputStream

interface DataReader {
    fun readUint64(ins: InputStream): ULong

    fun readInt64(ins: InputStream): Long

    fun readUint32(ins: InputStream): UInt

    fun readInt32(ins: InputStream): Int

    fun readUint16(ins: InputStream): UShort

    fun readInt16(ins: InputStream): Short

    fun readUint8(ins: InputStream): UByte

    fun readInt8(ins: InputStream): Byte

    fun readBoolean(ins: InputStream): Boolean

    fun readFloat32(ins: InputStream): Float

    fun readFloat64(ins: InputStream): Double

    fun readIntList16(ins: InputStream): IntArray

    fun readIntList32(ins: InputStream): IntArray

    fun readBoolList(ins: InputStream): List<Boolean>

    fun readString(ins: InputStream): String
}
