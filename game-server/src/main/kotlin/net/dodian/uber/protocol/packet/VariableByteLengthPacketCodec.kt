package net.dodian.uber.protocol.packet

import io.netty.buffer.ByteBuf

private const val MAX_BYTE_LENGTH = 255
abstract class VariableByteLengthPacketCodec<T : Packet>(
    type: Class<T>,
    opcode: Int
) : PacketCodec<T>(type, opcode) {

    override fun isLengthReadable(buf: ByteBuf): Boolean {
        return buf.isReadable
    }

    override fun readLength(buf: ByteBuf): Int {
        return buf.readUnsignedByte().toInt()
    }

    override fun offsetLength(buf: ByteBuf) {
        buf.writeZero(1)
    }

    override fun setLength(buf: ByteBuf, offsetLengthWriterIndex: Int, length: Int) {
        require(length <= MAX_BYTE_LENGTH)
        buf.setByte(offsetLengthWriterIndex, length)
    }
}