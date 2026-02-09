package pe.nanamochi.banchus.services.multiplayer;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.entities.commons.SlotStatus;
import pe.nanamochi.banchus.entities.commons.SlotTeam;
import pe.nanamochi.banchus.entities.redis.MultiplayerMatch;
import pe.nanamochi.banchus.entities.redis.MultiplayerSlot;
import pe.nanamochi.banchus.repositories.redis.MultiplayerMatchIdRepository;
import pe.nanamochi.banchus.repositories.redis.MultiplayerMatchRepository;
import pe.nanamochi.banchus.repositories.redis.MultiplayerSlotRepository;

@Service
@RequiredArgsConstructor
public class MultiplayerService {
  private final MultiplayerMatchIdRepository multiplayerMatchIdRepository;
  private final MultiplayerMatchRepository multiplayerMatchRepository;
  private final MultiplayerSlotRepository multiplayerSlotRepository;

  public MultiplayerMatch create(MultiplayerMatch match) {
    int multiplayerMatchId = claimId();
    match.setMatchId(multiplayerMatchId);
    match = multiplayerMatchRepository.create(match);

    for (int i = 0; i < 16; i++) {
      MultiplayerSlot multiplayerSlot = new MultiplayerSlot();
      multiplayerSlot.setSlotId(i);
      multiplayerSlot.setUserId(-1);
      multiplayerSlot.setSessionId(null);
      multiplayerSlot.setStatus(SlotStatus.OPEN.getValue());
      multiplayerSlot.setTeam(SlotTeam.NEUTRAL);
      multiplayerSlot.setMods(0);
      multiplayerSlot.setLoaded(false);
      multiplayerSlot.setSkipped(false);
      multiplayerSlotRepository.create(multiplayerMatchId, multiplayerSlot);
    }

    return match;
  }

  public List<MultiplayerMatch> getAllMatches() {
    return multiplayerMatchRepository.findAll();
  }

  public List<MultiplayerSlot> getAllSlots(int matchId) {
    return multiplayerSlotRepository.findAllByMatchId(matchId);
  }

  public MultiplayerMatch findById(int matchId) {
    return multiplayerMatchRepository.findById(matchId);
  }

  public MultiplayerSlot findSlotById(int matchId, int slotId) {
    return multiplayerSlotRepository.findById(matchId, slotId);
  }

  public MultiplayerSlot findSlotBySessionId(int matchId, UUID sessionId) {
    return multiplayerSlotRepository.findBySessionId(matchId, sessionId);
  }

  public MultiplayerMatch update(MultiplayerMatch match) {
    return multiplayerMatchRepository.update(match);
  }

  public MultiplayerSlot updateSlot(int matchId, MultiplayerSlot slot) {
    return multiplayerSlotRepository.update(matchId, slot);
  }

  public MultiplayerMatch delete(int matchId) {
    return multiplayerMatchRepository.delete(matchId);
  }

  public MultiplayerSlot deleteSlot(int matchId, int slotId) {
    return multiplayerSlotRepository.delete(matchId, slotId);
  }

  public boolean allCompleted(int matchId) {
    return multiplayerSlotRepository.allCompleted(matchId);
  }

  public boolean allLoaded(int matchId) {
    return multiplayerSlotRepository.allLoaded(matchId);
  }

  public boolean allSkipped(int matchId) {
    return multiplayerSlotRepository.allSkipped(matchId);
  }

  private int claimId() {
    Long newId = multiplayerMatchIdRepository.incrementId();

    if (newId != null && newId > 1) {
      return newId.intValue();
    }

    int lastMatchId =
        multiplayerMatchRepository.findAll().stream()
            .mapToInt(MultiplayerMatch::getMatchId)
            .max()
            .orElse(0);

    int nextId = lastMatchId + 1;
    multiplayerMatchIdRepository.setId(nextId);
    return nextId;
  }

  public Integer claimSlotId(int matchId) {
    List<MultiplayerSlot> slots = multiplayerSlotRepository.findAllByMatchId(matchId);
    for (MultiplayerSlot slot : slots) {
      if (slot.getUserId() != -1) continue;
      if (slot.getStatus() != SlotStatus.OPEN.getValue()) continue;
      return slot.getSlotId();
    }
    return null;
  }
}
