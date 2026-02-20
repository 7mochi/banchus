package pe.nanamochi.banchus.protocol.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.core.Packets;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.domain.enums.MatchTeamType;
import pe.nanamochi.banchus.domain.enums.Mods;
import pe.nanamochi.banchus.domain.enums.SlotTeam;
import pe.nanamochi.banchus.packets.client.MatchChangeSettingsPacket;
import pe.nanamochi.banchus.protocol.AbstractPacketHandler;
import pe.nanamochi.banchus.protocol.HandleClientPacket;
import pe.nanamochi.banchus.redis.model.MultiplayerSlot;
import pe.nanamochi.banchus.service.MatchBroadcastService;
import pe.nanamochi.banchus.service.MultiplayerService;

@Slf4j
@Component
@RequiredArgsConstructor
@HandleClientPacket(Packets.OSU_MATCH_CHANGE_SETTINGS)
public class MatchChangeSettingsHandler extends AbstractPacketHandler<MatchChangeSettingsPacket> {
  private final MultiplayerService multiplayerService;
  private final MatchBroadcastService matchBroadcastService;

  @Override
  public void handle(
      MatchChangeSettingsPacket packet, Session session, ByteArrayOutputStream responseStream)
      throws IOException {
    if (session.getMultiplayerMatchId() == null) {
      log.warn(
          "User {} tried to change match settings while not in a match.",
          session.getUser().getUsername());
      return;
    }

    multiplayerService
        .findById(session.getMultiplayerMatchId())
        .ifPresentOrElse(
            match -> {
              // Only the host can change match settings
              if (match.getHostUserId() != session.getUser().getId()) {
                log.warn(
                    "User {} tried to change match settings but is not the host.",
                    session.getUser().getUsername());
                return;
              }

              List<MultiplayerSlot> slots =
                  multiplayerService.getAllSlots(session.getMultiplayerMatchId());
              boolean needSlotUpdates = false;

              // If we switch to a versus mode, split all players into teams
              if (packet.getMatch().getTeamType() != match.getTeamType()
                  && (packet.getMatch().getTeamType() == MatchTeamType.TEAM_VS
                      || packet.getMatch().getTeamType() == MatchTeamType.TAG_TEAM_VS)) {
                needSlotUpdates = true;
                int teamIndex = 0;
                for (MultiplayerSlot slot : slots) {
                  if (slot.getUserId() == -1) continue;

                  if (teamIndex % 2 != 0) {
                    slot.setTeam(SlotTeam.BLUE);
                  } else {
                    slot.setTeam(SlotTeam.RED);
                  }
                  teamIndex++;
                }
              }

              // If freemod is activated, transfer match mods to slots
              // If freemod is disabled, clear slot mods
              if (packet.getMatch().isFreemodsEnabled() != match.isFreemodsEnabled()) {
                int mods = Mods.NO_MOD.getValue();
                // Copy bancho behavior
                if (packet.getMatch().isFreemodsEnabled()) {
                  mods = match.getMods() & (~Mods.SPEED_CHANGING);
                  packet.getMatch().setMods(match.getMods() & (Mods.SPEED_CHANGING));
                }

                needSlotUpdates = true;
                for (MultiplayerSlot slot : slots) {
                  if (slot.getUserId() != -1) {
                    slot.setMods(mods);
                  }
                }
              }

              // Update slots if needed
              if (needSlotUpdates) {
                for (MultiplayerSlot slot : slots) {
                  if (slot.getUserId() != -1) {
                    multiplayerService.updateSlot(match.getMatchId(), slot);
                  }
                }
              }

              // Update match settings
              match.setMatchName(packet.getMatch().getName());
              match.setMatchPassword(packet.getMatch().getPassword());
              match.setBeatmapName(packet.getMatch().getBeatmapName());
              match.setBeatmapId(packet.getMatch().getBeatmapId());
              match.setBeatmapMd5(packet.getMatch().getBeatmapMd5());
              match.setMode(
                  pe.nanamochi.banchus.domain.enums.Mode.fromValue(
                      packet.getMatch().getMode().getValue()));
              match.setMods(packet.getMatch().getMods());
              match.setScoringType(packet.getMatch().getScoringType());
              match.setTeamType(packet.getMatch().getTeamType());
              match.setFreemodsEnabled(packet.getMatch().isFreemodsEnabled());
              match.setRandomSeed(packet.getMatch().getRandomSeed());
              multiplayerService.update(match);

              // Inform relevant places of the new match state
              matchBroadcastService.broadcastMatchUpdates(match.getMatchId(), true, List.of());

              log.info(
                  "User {} changed settings for match {}.",
                  session.getUser().getUsername(),
                  match.getMatchId());
            },
            () ->
                log.warn(
                    "User {} tried to change match settings but their match doesn't exist.",
                    session.getUser().getUsername()));
  }
}
