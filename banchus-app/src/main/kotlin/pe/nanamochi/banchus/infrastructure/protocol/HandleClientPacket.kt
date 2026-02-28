package pe.nanamochi.banchus.infrastructure.protocol

import org.springframework.stereotype.Component
import pe.nanamochi.banchus.core.PacketType

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Component
annotation class HandleClientPacket(val type: PacketType, val checkForRestriction: Boolean = false)
