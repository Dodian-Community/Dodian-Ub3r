package net.dodian.uber.net.builder.downstream

import io.netty.buffer.ByteBuf
import net.dodian.uber.protocol.packet.DownstreamPacket

private const val VARIABLE_BYTE_LENGTH = -1
private const val VARIABLE_SHORT_LENGTH = -2

@DslMarker
private annotation class PacketBuilderDsl

@PacketBuilderDsl
class DownstreamPacketBuilder<T : DownstreamPacket> {
    private lateinit var encoder: (T, ByteBuf) -> Unit

    var opcode: Int = -1
    var length: Int = 0

    val variableByteLength: Int
        get() = VARIABLE_BYTE_LENGTH

    val variableShortLength: Int
        get() = VARIABLE_SHORT_LENGTH

    fun encode(encoder: (packet: T, buf: ByteBuf) -> Unit) {
        this.encoder = encoder
    }

    fun build(): DownstreamPacketStructure<T> {
        check(opcode != -1)
        check(::encoder.isInitialized)
        return DownstreamPacketStructure(opcode, length, encoder)
    }
}