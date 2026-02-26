package pe.nanamochi.banchus.redis.entity

import java.time.Instant

class PacketBundle(val data: ByteArray, val createdAt: Instant = Instant.now())
