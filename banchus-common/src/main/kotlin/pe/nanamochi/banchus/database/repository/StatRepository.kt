package pe.nanamochi.banchus.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.Stat
import pe.nanamochi.banchus.database.entity.User
import pe.nanamochi.banchus.domain.enums.Mode

@Repository
interface StatRepository : JpaRepository<Stat, Int> {
    fun findByUserAndGamemode(user: User, gamemode: Mode): Stat?
}
