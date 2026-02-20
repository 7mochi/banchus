package pe.nanamochi.banchus.database.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.nanamochi.banchus.database.entity.Channel;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {
  List<Channel> findByAutoJoin(boolean autoJoin);

  Optional<Channel> findByName(String name);
}
