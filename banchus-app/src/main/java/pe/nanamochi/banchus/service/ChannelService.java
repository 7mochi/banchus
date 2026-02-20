package pe.nanamochi.banchus.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.database.entity.Channel;
import pe.nanamochi.banchus.database.entity.Session;
import pe.nanamochi.banchus.database.repository.ChannelRepository;
import pe.nanamochi.banchus.redis.repository.ChannelMembersRepository;

@Service
@RequiredArgsConstructor
public class ChannelService {
  private final ChannelRepository channelRepository;
  private final ChannelMembersRepository membersRepository;

  public List<Channel> findByAutoJoin(boolean autoJoin) {
    return channelRepository.findByAutoJoin(autoJoin);
  }

  public Optional<Channel> findByName(String name) {
    return channelRepository.findByName(name);
  }

  public Channel create(Channel channel) {
    return channelRepository.save(channel);
  }

  public void delete(Channel channel) {
    if (!channelRepository.existsById(channel.getId())) {
      throw new IllegalArgumentException("Channel not found: " + channel.getId());
    }
    channelRepository.delete(channel);
  }

  public void joinChannel(Channel channel, Session session) {
    membersRepository.add(channel.getId(), session.getId());
  }

  public void leaveChannel(Channel channel, Session session) {
    membersRepository.remove(channel.getId(), session.getId());
  }

  public Set<UUID> getMemberIds(UUID channelId) {
    return membersRepository.getMembers(channelId);
  }

  public boolean canRead(Channel channel, int userPrivileges) {
    if (channel.getReadPrivileges() == 0) return true;
    return (userPrivileges & channel.getReadPrivileges()) != 0;
  }

  public boolean canWrite(Channel channel, int userPrivileges) {
    if (channel.getWritePrivileges() == 0) return true;
    return (userPrivileges & channel.getWritePrivileges()) != 0;
  }
}
