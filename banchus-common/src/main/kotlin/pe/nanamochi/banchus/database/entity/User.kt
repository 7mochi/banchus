package pe.nanamochi.banchus.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import pe.nanamochi.banchus.domain.enums.CountryCode
import pe.nanamochi.banchus.domain.enums.ServerPrivileges

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    var id: Int = 0,
    @Column(name = "username", length = 32, nullable = false, unique = true)
    var username: String = "",
    @Column(name = "email", length = 64, nullable = false, unique = true) var email: String = "",
    @Column(name = "password_md5", length = 32, nullable = false) var passwordMd5: String = "",
    @Column(name = "country", length = 2, nullable = false)
    @Enumerated(EnumType.ORDINAL)
    var country: CountryCode = CountryCode.XX,
    @Column(name = "silence_end") var silenceEnd: Instant? = null,
    @Column(name = "privileges", nullable = false) var privileges: Int = 1,
) {
    val isSilenced: Boolean
        get() = silenceEnd?.isAfter(Instant.now()) ?: false

    val isRestricted: Boolean
        get() = !ServerPrivileges.fromBitmask(privileges).contains(ServerPrivileges.UNRESTRICTED)
}
