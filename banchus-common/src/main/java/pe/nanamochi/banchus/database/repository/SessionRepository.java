package pe.nanamochi.banchus.database.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.database.entity.Session;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
  Optional<Session> findByUser_IdAndPrimarySessionTrue(int userId);

  Optional<Session> findByUser_UsernameIgnoreCaseAndPrimarySessionTrue(String username);
}
