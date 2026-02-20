package pe.nanamochi.banchus.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.database.entity.Channel;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.database.entity.Stat;
import pe.nanamochi.banchus.database.entity.User;
import pe.nanamochi.banchus.domain.dto.Geolocation;
import pe.nanamochi.banchus.domain.dto.LoginData;
import pe.nanamochi.banchus.domain.dto.LoginResponse;
import pe.nanamochi.banchus.domain.enums.CountryCode;
import pe.nanamochi.banchus.domain.enums.Mode;
import pe.nanamochi.banchus.domain.enums.ServerPrivileges;
import pe.nanamochi.banchus.packets.server.*;
import pe.nanamochi.banchus.protocol.PacketWriter;
import pe.nanamochi.banchus.redis.model.PacketBundle;
import pe.nanamochi.banchus.service.infra.IPApiClient;
import pe.nanamochi.banchus.util.Privileges;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {
  private final UserService userService;
  private final SessionService sessionService;
  private final StatService statService;
  private final RankingService rankingService;
  private final ChannelService channelService;
  private final PacketBundleService packetBundleService;
  private final IPApiClient ipApiService;
  private final PacketWriter packetWriter;

  public LoginResponse handleLogin(String rawData, HttpHeaders headers) {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    try {
      LoginData loginData = parseLoginData(rawData);
      String ipRaw = headers.getFirst("X-Real-IP");

      if (ipRaw == null) {
        log.error("No IP address found in headers for login attempt.");
        return buildErrorResponse(stream, "Could not determine your IP address.");
      }

      InetAddress ipAddress = InetAddress.getByName(ipRaw);
      return userService
          .login(loginData.username(), loginData.passwordMd5())
          .map(user -> processSuccessfulLogin(user, loginData, ipAddress, stream))
          .orElseGet(() -> buildErrorResponse(stream, "Invalid username or password."));
    } catch (UnknownHostException e) {
      return buildErrorResponse(stream, "Invalid network configuration.");
    } catch (Exception e) {
      log.error("Critical error during login.", e);
      return buildErrorResponse(stream, "Internal server error.");
    }
  }

  private LoginResponse processSuccessfulLogin(
      User user, LoginData loginData, InetAddress ip, ByteArrayOutputStream stream) {
    try {
      Geolocation geolocation =
          (ip.isLoopbackAddress()) ? Geolocation.local() : ipApiService.fetchFromIP(ip);
      Session ownSession =
          sessionService.create(
              Session.builder()
                  .user(user)
                  .utcOffset(loginData.utcOffset())
                  .gamemode(Mode.OSU)
                  .country(CountryCode.fromCode(geolocation.countryCode().toLowerCase()))
                  .latitude(geolocation.lat())
                  .longitude(geolocation.lon())
                  .displayCityLocation(loginData.displayCity())
                  .pmPrivate(loginData.pmPrivate())
                  .primarySession(!loginData.osuVersion().contains("tourney"))
                  .osuVersion(loginData.osuVersion())
                  .osuPathMd5(loginData.osuPathMd5())
                  .adaptersStr(loginData.adaptersStr())
                  .adaptersMd5(loginData.adaptersMd5())
                  .uninstallMd5(loginData.uninstallMd5())
                  .diskSignatureMd5(loginData.diskSignatureMd5())
                  .lastCommunicatedAt(Instant.now())
                  .build());

      Stat ownStats =
          statService
              .findByUserAndGamemode(user, ownSession.getGamemode())
              .orElseThrow(() -> new RuntimeException("Stats not found"));
      int ownGlobalRank =
          Math.toIntExact(rankingService.getGlobalRank(ownSession.getGamemode(), user));

      writeLoginSuccessPackets(stream, user, ownSession, ownStats, ownGlobalRank);
      sendPresenceToOtherUsers(ownSession, ownStats, ownGlobalRank);
      sendOtherUsersPresenceToSelf(stream, ownSession);
      sendWelcomeAndStatusPackets(stream, user);

      return new LoginResponse(ownSession.getId().toString(), stream.toByteArray(), true);
    } catch (Exception e) {
      throw new RuntimeException("Error writing success packets", e);
    }
  }

  private void writeLoginSuccessPackets(
      ByteArrayOutputStream stream, User user, Session session, Stat stats, int globalRank)
      throws IOException {
    packetWriter.writePacket(stream, new ProtocolNegotiationPacket());
    packetWriter.writePacket(stream, new LoginReplyPacket(user.getId()));
    packetWriter.writePacket(
        stream,
        new LoginPermissionsPacket(
            Privileges.serverToClientPrivileges(
                user.getPrivileges() | ServerPrivileges.SUPPORTER.getValue())));

    List<Channel> autoJoinChannels = channelService.findByAutoJoin(true);
    for (Channel channel : autoJoinChannels) {
      if (!channelService.canRead(channel, user.getPrivileges())
          || channel.getName().equals("#lobby")) {
        continue;
      }

      int memberCount = channelService.getMemberIds(channel.getId()).size();
      packetWriter.writePacket(
          stream, new ChannelAvailablePacket(channel.getName(), channel.getTopic(), memberCount));
    }
    packetWriter.writePacket(stream, new ChannelInfoCompletePacket());
    packetWriter.writePacket(stream, new UserPresencePacket(user, session, globalRank));
    packetWriter.writePacket(stream, new UserStatsPacket(user, session, stats, globalRank));
  }

  private void sendPresenceToOtherUsers(Session ownSession, Stat ownStats, int ownGlobalRank)
      throws IOException {
    if (ownSession.getUser().isRestricted()) {
      return;
    }

    ByteArrayOutputStream otherStream = new ByteArrayOutputStream();
    packetWriter.writePacket(
        otherStream, new UserPresencePacket(ownSession.getUser(), ownSession, ownGlobalRank));
    packetWriter.writePacket(
        otherStream,
        new UserStatsPacket(ownSession.getUser(), ownSession, ownStats, ownGlobalRank));

    PacketBundle packetBundle = new PacketBundle(otherStream.toByteArray());

    sessionService.findAll().stream()
        .filter(otherSession -> !otherSession.getId().equals(ownSession.getId()))
        .forEach(otherSession -> packetBundleService.enqueue(otherSession.getId(), packetBundle));
  }

  private void sendOtherUsersPresenceToSelf(ByteArrayOutputStream stream, Session ownSession) {
    sessionService.findAll().stream()
        .filter(otherSession -> !otherSession.getId().equals(ownSession.getId()))
        .forEach(
            otherSession ->
                statService
                    .findByUserAndGamemode(otherSession.getUser(), otherSession.getGamemode())
                    .ifPresent(
                        otherStats -> {
                          try {
                            int otherRank =
                                Math.toIntExact(
                                    rankingService.getGlobalRank(
                                        otherSession.getGamemode(), otherSession.getUser()));

                            packetWriter.writePacket(
                                stream,
                                new UserPresencePacket(
                                    otherSession.getUser(), otherSession, otherRank));
                            packetWriter.writePacket(
                                stream,
                                new UserStatsPacket(
                                    otherSession.getUser(), otherSession, otherStats, otherRank));
                          } catch (IOException e) {
                            log.error("Error writing other user presence to login stream", e);
                          }
                        }));
  }

  private void sendWelcomeAndStatusPackets(ByteArrayOutputStream stream, User user)
      throws IOException {
    packetWriter.writePacket(stream, new AnnouncePacket("Welcome to Banchus!"));

    if (user.getSilenceEnd() != null) {
      long secondsRemaining = Duration.between(Instant.now(), user.getSilenceEnd()).toSeconds();
      if (secondsRemaining > 0) {
        packetWriter.writePacket(stream, new SilenceInfoPacket(Math.toIntExact(secondsRemaining)));
      } else {
        user.setSilenceEnd(null);
        userService.update(user);
      }
    }

    if (user.isRestricted()) {
      packetWriter.writePacket(stream, new AccountRestrictedPacket());
      packetWriter.writePacket(
          stream,
          new MessagePacket(
              "BanchoBot",
              "Your account is currently in restricted mode. Please visit the website for more"
                  + " information.",
              user.getUsername(),
              1));
    }
  }

  private LoginResponse buildErrorResponse(ByteArrayOutputStream stream, String message) {
    try {
      stream.reset();
      packetWriter.writePacket(stream, new LoginReplyPacket(-1));
      packetWriter.writePacket(stream, new AnnouncePacket(message));
    } catch (IOException e) {
      log.error("Could not write error packets", e);
    }
    return new LoginResponse("no", stream.toByteArray(), false);
  }

  private LoginData parseLoginData(String data) {
    try {
      String[] lines = data.split("\n", 3);
      String[] clientInfo = lines[2].split("\\|", 5);
      String[] hashes = clientInfo[3].split(":", 5);

      return new LoginData(
          lines[0],
          lines[1],
          clientInfo[0],
          Integer.parseInt(clientInfo[1]),
          clientInfo[2].equals("1"),
          clientInfo[4].equals("1"),
          hashes[0],
          hashes[1],
          hashes[2],
          hashes[3],
          hashes[4]);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid bancho login format.", e);
    }
  }
}
