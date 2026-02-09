package pe.nanamochi.banchus.packets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.nanamochi.banchus.entities.commons.*;
import pe.nanamochi.banchus.entities.packets.*;
import pe.nanamochi.banchus.entities.packets.ReplayAction;
import pe.nanamochi.banchus.io.data.BanchoDataReader;
import pe.nanamochi.banchus.io.data.IDataReader;
import pe.nanamochi.banchus.packets.client.*;

@Component
public class PacketReader {

  private static final Logger logger = LoggerFactory.getLogger(PacketReader.class);

  private final IDataReader reader;

  public PacketReader() {
    this.reader = new BanchoDataReader();
  }

  public PacketReader(IDataReader reader) {
    this.reader = reader;
  }

  public Packet readPacket(InputStream stream) throws IOException {
    int packetId = reader.readUint16(stream);
    reader.readUint8(stream);
    int length = reader.readInt32(stream);
    byte[] data = stream.readNBytes(length);
    return readPacketType(packetId, new ByteArrayInputStream(data));
  }

  public Packet readPacketType(int packetId, InputStream stream) throws IOException {
    if (packetId == Packets.OSU_USER_STATUS.getId()) {
      return readUserStatus(stream);
    } else if (packetId == Packets.OSU_MESSAGE.getId()) {
      return readMessage(stream);
    } else if (packetId == Packets.OSU_EXIT.getId()) {
      return readUserExit(stream);
    } else if (packetId == Packets.OSU_STATUS_UPDATE_REQUEST.getId()) {
      return readStatusUpdateRequest(stream);
    } else if (packetId == Packets.OSU_PONG.getId()) {
      return readPong(stream);
    } else if (packetId == Packets.OSU_PRIVATE_MESSAGE.getId()) {
      return readPrivateMessage(stream);
    } else if (packetId == Packets.OSU_RECEIVE_UPDATES.getId()) {
      return readReceiveUpdates(stream);
    } else if (packetId == Packets.OSU_USER_STATS_REQUEST.getId()) {
      return readUserStatsRequest(stream);
    } else if (packetId == Packets.OSU_CHANNEL_JOIN.getId()) {
      return readChannelJoin(stream);
    } else if (packetId == Packets.OSU_CHANNEL_LEAVE.getId()) {
      return readChannelLeave(stream);
    } else if (packetId == Packets.OSU_START_SPECTATING.getId()) {
      return readStartSpectating(stream);
    } else if (packetId == Packets.OSU_STOP_SPECTATING.getId()) {
      return readStopSpectating(stream);
    } else if (packetId == Packets.OSU_SPECTATE_FRAMES.getId()) {
      return readSpectateFrames(stream);
    } else if (packetId == Packets.OSU_CANT_SPECTATE.getId()) {
      return readCantSpectate(stream);
    } else if (packetId == Packets.OSU_LOBBY_PART.getId()) {
      return readLobbyPart(stream);
    } else if (packetId == Packets.OSU_LOBBY_JOIN.getId()) {
      return readLobbyJoin(stream);
    } else if (packetId == Packets.OSU_MATCH_CREATE.getId()) {
      return readMatchCreate(stream);
    } else if (packetId == Packets.OSU_MATCH_JOIN.getId()) {
      return readMatchJoin(stream);
    } else if (packetId == Packets.OSU_MATCH_PART.getId()) {
      return readMatchPart(stream);
    } else if (packetId == Packets.OSU_MATCH_CHANGE_SETTINGS.getId()) {
      return readMatchChangeSettings(stream);
    } else if (packetId == Packets.OSU_MATCH_CHANGE_SLOT.getId()) {
      return readMatchChangeSlot(stream);
    } else if (packetId == Packets.OSU_MATCH_READY.getId()) {
      return readMatchReady(stream);
    } else if (packetId == Packets.OSU_MATCH_LOCK.getId()) {
      return readMatchLock(stream);
    } else if (packetId == Packets.OSU_MATCH_START.getId()) {
      return readMatchStart(stream);
    } else if (packetId == Packets.OSU_MATCH_SCORE_UPDATE.getId()) {
      return readMatchScoreUpdate(stream);
    } else if (packetId == Packets.OSU_MATCH_COMPLETE.getId()) {
      return readMatchComplete(stream);
    } else if (packetId == Packets.OSU_MATCH_CHANGE_MODS.getId()) {
      return readMatchChangeMods(stream);
    } else if (packetId == Packets.OSU_MATCH_LOAD_COMPLETE.getId()) {
      return readMatchLoadComplete(stream);
    } else if (packetId == Packets.OSU_MATCH_NO_BEATMAP.getId()) {
      return readMatchNoBeatmap(stream);
    } else if (packetId == Packets.OSU_MATCH_NOT_READY.getId()) {
      return readMatchNotReady(stream);
    } else if (packetId == Packets.OSU_MATCH_FAILED.getId()) {
      return readMatchFailed(stream);
    } else if (packetId == Packets.OSU_MATCH_HAS_BEATMAP.getId()) {
      return readMatchHasBeatmap(stream);
    } else if (packetId == Packets.OSU_MATCH_SKIP_REQUEST.getId()) {
      return readMatchSkipRequest(stream);
    } else if (packetId == Packets.OSU_MATCH_CHANGE_PASSWORD.getId()) {
      return readMatchChangePassword(stream);
    } else {
      logger.warn("Packet id {} not supported yet.", packetId);
      return null;
    }
  }

  public List<Packet> readPackets(byte[] data) throws IOException {
    return readPackets(new ByteArrayInputStream(data));
  }

  public List<Packet> readPackets(InputStream stream) throws IOException {
    List<Packet> packets = new ArrayList<>();
    while (stream.available() > 0) {
      packets.add(readPacket(stream));
    }
    return packets;
  }

  public ExitPacket readUserExit(InputStream stream) {
    return new ExitPacket();
  }

  public UserStatusPacket readUserStatus(InputStream stream) throws IOException {
    UserStatusPacket packet = new UserStatusPacket();
    Status action = Status.fromValue(reader.readUint8(stream));

    packet.setAction(action);
    packet.setText(reader.readString(stream));
    packet.setBeatmapChecksum(reader.readString(stream));
    packet.setMods(Mods.fromBitmask(reader.readUint32(stream)));
    packet.setMode(Mode.fromValue(reader.readUint8(stream)));
    packet.setBeatmapId(reader.readInt32(stream));

    return packet;
  }

  public MessagePacket readMessage(InputStream stream) throws IOException {
    MessagePacket packet = new MessagePacket();
    packet.setSender(reader.readString(stream));
    packet.setContent(reader.readString(stream));
    packet.setTarget(reader.readString(stream));
    packet.setSenderId(reader.readInt32(stream));
    return packet;
  }

  public StatusUpdateRequestPacket readStatusUpdateRequest(InputStream stream) {
    return new StatusUpdateRequestPacket();
  }

  public PongPacket readPong(InputStream stream) {
    return new PongPacket();
  }

  public PrivateMessagePacket readPrivateMessage(InputStream stream) throws IOException {
    PrivateMessagePacket packet = new PrivateMessagePacket();
    packet.setSender(reader.readString(stream));
    packet.setContent(reader.readString(stream));
    packet.setTarget(reader.readString(stream));
    packet.setSenderId(reader.readInt32(stream));
    return packet;
  }

  public ReceiveUpdatesPacket readReceiveUpdates(InputStream stream) {
    return new ReceiveUpdatesPacket();
  }

  public UserStatsRequestPacket readUserStatsRequest(InputStream stream) {
    return new UserStatsRequestPacket();
  }

  public ChannelJoinPacket readChannelJoin(InputStream stream) throws IOException {
    ChannelJoinPacket packet = new ChannelJoinPacket();
    packet.setName(reader.readString(stream));
    return packet;
  }

  public ChannelLeavePacket readChannelLeave(InputStream stream) throws IOException {
    ChannelLeavePacket packet = new ChannelLeavePacket();
    packet.setName(reader.readString(stream));
    return packet;
  }

  private StartSpectatingPacket readStartSpectating(InputStream stream) throws IOException {
    StartSpectatingPacket packet = new StartSpectatingPacket();
    packet.setUserId(reader.readInt32(stream));
    return packet;
  }

  private SpectateFramesPacket readSpectateFrames(InputStream stream) throws IOException {
    SpectateFramesPacket packet = new SpectateFramesPacket();
    ReplayFrameBundle replayFrameBundle = new ReplayFrameBundle();

    replayFrameBundle.setExtra(reader.readUint32(stream));
    int replayFrameCount = reader.readUint16(stream);

    List<ReplayFrame> replayFrames = new ArrayList<>();
    for (int i = 0; i < replayFrameCount; i++) {
      ReplayFrame frame = new ReplayFrame();
      frame.setButtonState(reader.readUint8(stream));
      frame.setTaikoByte(reader.readUint8(stream));
      frame.setX(reader.readFloat32(stream));
      frame.setY(reader.readFloat32(stream));
      frame.setTime(reader.readInt32(stream));
      replayFrames.add(frame);
    }
    replayFrameBundle.setFrames(replayFrames);
    replayFrameBundle.setAction(ReplayAction.fromValue(reader.readUint8(stream)));
    replayFrameBundle.setFrame(readScoreFrame(stream));
    replayFrameBundle.setSequence(reader.readUint16(stream));

    packet.setReplayFrameBundle(replayFrameBundle);
    return packet;
  }

  private StopSpectatingPacket readStopSpectating(InputStream stream) {
    return new StopSpectatingPacket();
  }

  private CantSpectatePacket readCantSpectate(InputStream stream) {
    return new CantSpectatePacket();
  }

  private LobbyPartPacket readLobbyPart(InputStream stream) {
    return new LobbyPartPacket();
  }

  private LobbyJoinPacket readLobbyJoin(InputStream stream) {
    return new LobbyJoinPacket();
  }

  private MatchCreatePacket readMatchCreate(InputStream stream) throws IOException {
    MatchCreatePacket packet = new MatchCreatePacket();
    packet.setMatch(readMatch(stream));
    return packet;
  }

  private MatchJoinPacket readMatchJoin(InputStream stream) throws IOException {
    MatchJoinPacket packet = new MatchJoinPacket();
    packet.setMatchId(reader.readInt32(stream));
    packet.setMatchPassword(reader.readString(stream));
    return packet;
  }

  private MatchPartPacket readMatchPart(InputStream stream) {
    return new MatchPartPacket();
  }

  private MatchChangeSettingsPacket readMatchChangeSettings(InputStream stream) throws IOException {
    MatchChangeSettingsPacket packet = new MatchChangeSettingsPacket();
    packet.setMatch(readMatch(stream));
    return packet;
  }

  private MatchChangeSlotPacket readMatchChangeSlot(InputStream stream) throws IOException {
    MatchChangeSlotPacket packet = new MatchChangeSlotPacket();
    packet.setSlotId(reader.readInt32(stream));
    return packet;
  }

  private MatchReadyPacket readMatchReady(InputStream stream) {
    return new MatchReadyPacket();
  }

  private MatchLockPacket readMatchLock(InputStream stream) throws IOException {
    MatchLockPacket packet = new MatchLockPacket();
    packet.setSlotId(reader.readInt32(stream));
    return packet;
  }

  private MatchStartPacket readMatchStart(InputStream stream) {
    return new MatchStartPacket();
  }

  private MatchScoreUpdatePacket readMatchScoreUpdate(InputStream stream) throws IOException {
    MatchScoreUpdatePacket packet = new MatchScoreUpdatePacket();
    packet.setFrame(readScoreFrame(stream));
    return packet;
  }

  private MatchCompletePacket readMatchComplete(InputStream stream) {
    return new MatchCompletePacket();
  }

  private MatchChangeModsPacket readMatchChangeMods(InputStream stream) throws IOException {
    MatchChangeModsPacket packet = new MatchChangeModsPacket();
    packet.setMods(reader.readUint32(stream));
    return packet;
  }

  private MatchLoadCompletePacket readMatchLoadComplete(InputStream stream) {
    return new MatchLoadCompletePacket();
  }

  private MatchNoBeatmapPacket readMatchNoBeatmap(InputStream stream) {
    return new MatchNoBeatmapPacket();
  }

  private MatchNotReadyPacket readMatchNotReady(InputStream stream) {
    return new MatchNotReadyPacket();
  }

  private MatchFailedPacket readMatchFailed(InputStream stream) {
    return new MatchFailedPacket();
  }

  private MatchHasBeatmapPacket readMatchHasBeatmap(InputStream stream) {
    return new MatchHasBeatmapPacket();
  }

  private MatchSkipRequestPacket readMatchSkipRequest(InputStream stream) {
    return new MatchSkipRequestPacket();
  }

  private MatchChangePasswordPacket readMatchChangePassword(InputStream stream) throws IOException {
    MatchChangePasswordPacket packet = new MatchChangePasswordPacket();
    packet.setMatch(readMatch(stream));
    return packet;
  }

  private ScoreFrame readScoreFrame(InputStream stream) throws IOException {
    ScoreFrame scoreFrame = new ScoreFrame();
    scoreFrame.setTime(reader.readInt32(stream));
    scoreFrame.setId(reader.readUint8(stream));
    scoreFrame.setTotal300(reader.readUint16(stream));
    scoreFrame.setTotal100(reader.readUint16(stream));
    scoreFrame.setTotal50(reader.readUint16(stream));
    scoreFrame.setTotalGeki(reader.readUint16(stream));
    scoreFrame.setTotalKatu(reader.readUint16(stream));
    scoreFrame.setTotalMiss(reader.readUint16(stream));
    scoreFrame.setTotalScore(reader.readUint32(stream));
    scoreFrame.setMaxCombo(reader.readUint16(stream));
    scoreFrame.setCurrentCombo(reader.readUint16(stream));
    scoreFrame.setPerfect(reader.readUint8(stream) == 1);
    scoreFrame.setHp(reader.readUint8(stream));
    scoreFrame.setTagByte(reader.readUint8(stream));
    scoreFrame.setUsingScoreV2(reader.readUint8(stream) == 1);
    if (scoreFrame.isUsingScoreV2()) {
      scoreFrame.setComboPortion((float) reader.readFloat64(stream));
      scoreFrame.setBonusPortion((float) reader.readFloat64(stream));
    }
    return scoreFrame;
  }

  private Match readMatch(InputStream stream) throws IOException {
    Match match = new Match();

    match.setId(reader.readUint16(stream));
    match.setInProgress(reader.readBoolean(stream));
    match.setType(MatchType.fromValue(reader.readUint8(stream)));
    match.setMods(reader.readUint32(stream));
    match.setName(reader.readString(stream));
    match.setPassword(reader.readString(stream));
    match.setBeatmapName(reader.readString(stream));
    match.setBeatmapId(reader.readInt32(stream));
    match.setBeatmapMd5(reader.readString(stream));

    List<MatchSlot> slots = new ArrayList<>();
    for (int i = 0; i < 16; i++) {
      MatchSlot slot = new MatchSlot();
      slot.setStatus(SlotStatus.toBitmask(SlotStatus.fromBitmask(reader.readUint8(stream))));
      slots.add(slot);
    }
    match.setSlots(slots);

    for (MatchSlot slot : match.getSlots()) {
      slot.setTeam(SlotTeam.fromValue(reader.readUint8(stream)));
    }

    for (MatchSlot slot : match.getSlots()) {
      if ((slot.getStatus() & SlotStatus.HAS_PLAYER.getValue()) != 0) {
        slot.setUserId(reader.readInt32(stream));
      }
    }

    match.setHostId(reader.readInt32(stream));
    match.setMode(Mode.fromValue(reader.readUint8(stream)));
    match.setScoringType(ScoringType.fromValue(reader.readUint8(stream)));
    match.setTeamType(MatchTeamType.fromValue(reader.readUint8(stream)));
    match.setFreemodsEnabled(reader.readBoolean(stream));

    if (match.isFreemodsEnabled()) {
      for (MatchSlot slot : match.getSlots()) {
        slot.setMods(Mods.toBitmask(Mods.fromBitmask(reader.readUint32(stream))));
      }
    }

    match.setRandomSeed(reader.readUint32(stream));

    return match;
  }
}
