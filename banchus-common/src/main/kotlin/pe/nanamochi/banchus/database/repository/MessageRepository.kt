package pe.nanamochi.banchus.database.repository

import jakarta.transaction.Transactional
import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.database.entity.Message

@Repository
interface MessageRepository : JpaRepository<Message, Int> {
    fun findAllByTargetIdAndDeletedAtIsNullAndReadAtIsNull(targetId: Int): List<Message>

    @Modifying
    @Transactional
    @Query(
        "UPDATE Message m SET m.readAt = CURRENT_TIMESTAMP WHERE m.targetId = :targetId AND m.readAt IS NULL"
    )
    fun markAllAsRead(targetId: Int)

    fun countBySenderIdAndCreatedAtAfter(senderId: Int, deltaSeconds: Instant): Long

    @Modifying
    @Transactional
    @Query(
        "UPDATE Message m SET m.deletedAt = CURRENT_TIMESTAMP WHERE m.senderId = :senderId AND m.createdAt > :deltaSeconds"
    )
    fun softDeleteRecent(senderId: Int, deltaSeconds: Instant)

    fun senderName(senderName: String): MutableList<Message>

    fun targetId(targetId: Int): MutableList<Message>
}
