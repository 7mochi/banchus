package pe.nanamochi.banchus.database.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import pe.nanamochi.banchus.domain.enums.CountryCode;
import pe.nanamochi.banchus.domain.enums.ServerPrivileges;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "users")
public class User {
  @EqualsAndHashCode.Include
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, unique = true)
  private int id;

  @Column(name = "username", length = 32, nullable = false, unique = true)
  private String username;

  @Column(name = "email", length = 64, nullable = false, unique = true)
  private String email;

  @Column(name = "password_md5", length = 32, nullable = false)
  private String passwordMd5;

  @Column(name = "country", length = 2, nullable = false)
  private CountryCode country;

  @Column(name = "silence_end")
  private Instant silenceEnd;

  @Column(name = "privileges", nullable = false)
  private int privileges = 1;

  public boolean isSilenced() {
    return this.getSilenceEnd() != null && this.getSilenceEnd().isAfter(Instant.now());
  }

  public boolean isRestricted() {
    return !ServerPrivileges.fromBitmask(this.getPrivileges())
        .contains(ServerPrivileges.UNRESTRICTED);
  }
}
