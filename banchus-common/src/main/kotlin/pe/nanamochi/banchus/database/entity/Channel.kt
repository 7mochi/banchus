package pe.nanamochi.banchus.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UuidGenerator
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "channels", indexes = [Index(name = "channels_name_idx", columnList = "name")])
@EntityListeners(AuditingEntityListener::class)
class Channel(
    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @UuidGenerator
    @Column(name = "id", nullable = false, length = 36, updatable = false)
    var id: UUID? = null,
    @Column(name = "name", nullable = false, length = 96, unique = true) var name: String = "",
    @Column(name = "topic", nullable = false, length = 256) var topic: String = "",
    @Column(name = "read_privileges", nullable = false) var readPrivileges: Int = 0,
    @Column(name = "write_privileges", nullable = false) var writePrivileges: Int = 0,
    @Column(name = "auto_join", nullable = false) var autoJoin: Boolean = false,
    @Column(name = "temporary", nullable = false) var temporary: Boolean = false,
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
