package pe.nanamochi.banchus.database.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.database.entity.Stat;
import pe.nanamochi.banchus.database.entity.User;
import pe.nanamochi.banchus.domain.enums.Mode;

@Repository
public interface StatRepository extends JpaRepository<Stat, Integer> {
  Optional<Stat> findByUserAndGamemode(User user, Mode gamemode);
}
