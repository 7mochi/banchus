package pe.nanamochi.banchus.multiplayer.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "matches")
class Match(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    var id: Long = 0,
    @Column(name = "name", nullable = false) var name: String,
    @Column(name = "private", nullable = false) var private: Boolean,
    @Column(name = "start_time", nullable = false) var startTime: Instant = Instant.now(),
    @Column(name = "end_time", nullable = true) var endTime: Instant? = null,
)
