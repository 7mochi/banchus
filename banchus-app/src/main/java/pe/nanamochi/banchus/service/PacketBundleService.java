package pe.nanamochi.banchus.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.redis.repository.PacketBundleRepository;

@Service
@RequiredArgsConstructor
public class PacketBundleService {
  private final PacketBundleRepository packetBundleRepository;

  public void enqueue(UUID sessionId, PacketBundle packetBundle) {
    packetBundleRepository.enqueue(sessionId, packetBundle);
  }

  public List<PacketBundle> dequeueAll(UUID sessionId) {
    return packetBundleRepository.dequeueAll(sessionId);
  }
}
