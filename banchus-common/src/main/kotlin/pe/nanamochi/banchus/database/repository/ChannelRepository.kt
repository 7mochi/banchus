package pe.nanamochi.banchus.database.repository

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.Channel

@Repository
interface ChannelRepository : JpaRepository<Channel, UUID> {
    fun findByName(name: String): Channel?
}
