package pe.nanamochi.banchus.service;

import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.redis.repository.SpectatorRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpectatorService {
  private final SpectatorRepository spectatorRepository;

  public void addSpectator(UUID hostSessionId, UUID spectatorSessionId) {
    log.debug("Adding spectator {} to host {}", spectatorSessionId, hostSessionId);
    spectatorRepository.add(hostSessionId, spectatorSessionId);
  }

  public void removeSpectator(UUID hostSessionId, UUID spectatorSessionId) {
    log.debug("Removing spectator {} from host {}", spectatorSessionId, hostSessionId);
    spectatorRepository.remove(hostSessionId, spectatorSessionId);
  }

  public Set<UUID> getSpectators(UUID hostSessionId) {
    return spectatorRepository.getMembers(hostSessionId);
  }

  public boolean hasSpectators(UUID hostSessionId) {
    Set<UUID> members = spectatorRepository.getMembers(hostSessionId);
    return !members.isEmpty();
  }

  public void removeAllSpectators(UUID hostSessionId) {
    log.info("Removing all spectators for host {}", hostSessionId);
    Set<UUID> members = spectatorRepository.getMembers(hostSessionId);
    members.forEach(spectatorId -> spectatorRepository.remove(hostSessionId, spectatorId));
  }
}
