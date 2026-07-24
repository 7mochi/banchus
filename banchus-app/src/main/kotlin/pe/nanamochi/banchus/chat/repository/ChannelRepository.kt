package pe.nanamochi.banchus.chat.repository

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.chat.entity.Channel

@Repository
interface ChannelRepository : JpaRepository<Channel, UUID> {
    fun findByName(name: String): Channel?
}
