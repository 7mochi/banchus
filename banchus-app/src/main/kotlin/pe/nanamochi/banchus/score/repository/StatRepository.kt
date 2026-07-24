package pe.nanamochi.banchus.score.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.identity.entity.User
import pe.nanamochi.banchus.score.entity.Stat
import pe.nanamochi.banchus.core.enums.Mode

@Repository
interface StatRepository : JpaRepository<Stat, Int> {
    fun findByUserIdAndMode(userId: Int, mode: Mode): Stat?

    fun findByUser(user: User): List<Stat>
}
