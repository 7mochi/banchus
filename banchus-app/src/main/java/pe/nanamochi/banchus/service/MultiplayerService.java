package pe.nanamochi.banchus.service;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.domain.enums.SlotStatus;
import pe.nanamochi.banchus.domain.enums.SlotTeam;
import pe.nanamochi.banchus.redis.model.MultiplayerMatch;
import pe.nanamochi.banchus.redis.model.MultiplayerSlot;
import pe.nanamochi.banchus.redis.repository.MultiplayerRepository;

@Service
@RequiredArgsConstructor
public class MultiplayerService {
  private final MultiplayerRepository multiplayerRepository;

  public MultiplayerMatch create(MultiplayerMatch match) {
    int matchId = multiplayerRepository.nextMatchId().intValue();
    match.setMatchId(matchId);

    List<MultiplayerSlot> slots = new ArrayList<>(16);
    for (int i = 0; i < 16; i++) {
      slots.add(
          MultiplayerSlot.builder()
              .slotId(i)
              .userId(-1)
              .status(SlotStatus.OPEN.getValue())
              .team(SlotTeam.NEUTRAL)
              .mods(0)
              .build());
    }
    match.setSlots(slots);

    return multiplayerRepository.create(match);
  }

  public List<MultiplayerMatch> findAll() {
    return multiplayerRepository.findAll();
  }

  public Optional<MultiplayerMatch> findById(int matchId) {
    return Optional.ofNullable(multiplayerRepository.findById(matchId));
  }

  public MultiplayerMatch update(MultiplayerMatch match) {
    return multiplayerRepository.update(match);
  }

  public void deleteMatch(int matchId) {
    multiplayerRepository.delete(matchId);
  }

  public Optional<MultiplayerSlot> findSlotBySessionId(int matchId, UUID sessionId) {
    return findById(matchId)
        .flatMap(
            match ->
                match.getSlots().stream()
                    .filter(s -> sessionId.equals(s.getSessionId()))
                    .findFirst());
  }

  public List<MultiplayerSlot> getAllSlots(int matchId) {
    return findById(matchId)
        .map(
            match ->
                match.getSlots().stream()
                    .sorted(Comparator.comparingInt(MultiplayerSlot::getSlotId))
                    .toList())
        .orElse(List.of());
  }

  public MultiplayerMatch updateSlot(int matchId, MultiplayerSlot updatedSlot) {
    return findById(matchId)
        .map(
            match -> {
              List<MultiplayerSlot> slots = match.getSlots();
              boolean found = false;

              for (int i = 0; i < slots.size(); i++) {
                if (slots.get(i).getSlotId() == updatedSlot.getSlotId()) {
                  slots.set(i, updatedSlot);
                  found = true;
                  break;
                }
              }

              if (found) {
                return multiplayerRepository.update(match);
              }
              return match;
            })
        .orElse(null);
  }

  public boolean allPlayersLoaded(int matchId) {
    return findById(matchId).map(MultiplayerMatch::allLoaded).orElse(false);
  }

  public boolean allPlayersSkipped(int matchId) {
    return findById(matchId).map(MultiplayerMatch::allSkipped).orElse(false);
  }

  public boolean allPlayersCompleted(int matchId) {
    return findById(matchId).map(MultiplayerMatch::allCompleted).orElse(false);
  }

  public Optional<Integer> claimFirstAvailableSlotId(int matchId) {
    return findById(matchId)
        .flatMap(
            match ->
                match.getSlots().stream()
                    .filter(s -> s.getUserId() == -1 && s.getStatus() == SlotStatus.OPEN.getValue())
                    .map(MultiplayerSlot::getSlotId)
                    .findFirst());
  }

  public Optional<MultiplayerSlot> findSlotById(int matchId, int slotId) {
    return findById(matchId)
        .flatMap(
            match -> match.getSlots().stream().filter(s -> s.getSlotId() == slotId).findFirst());
  }

  public MultiplayerMatch resetSlot(int matchId, int slotId) {
    return findById(matchId)
        .map(
            match -> {
              match.getSlots().stream()
                  .filter(s -> s.getSlotId() == slotId)
                  .findFirst()
                  .ifPresent(
                      slot -> {
                        slot.setUserId(-1);
                        slot.setSessionId(null);
                        slot.setStatus(SlotStatus.OPEN.getValue());
                        slot.setTeam(SlotTeam.NEUTRAL);
                        slot.setMods(0);
                        slot.setLoaded(false);
                        slot.setSkipped(false);
                      });
              return multiplayerRepository.update(match);
            })
        .orElse(null);
  }

  private boolean isSlotPlaying(MultiplayerSlot slot) {
    return (slot.getStatus() & SlotStatus.PLAYING.getValue()) != 0;
  }
}
