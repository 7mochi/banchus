package pe.nanamochi.banchus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.database.entity.Stat;
import pe.nanamochi.banchus.database.entity.User;
import pe.nanamochi.banchus.domain.enums.CountryCode;
import pe.nanamochi.banchus.domain.enums.Mode;
import pe.nanamochi.banchus.redis.repository.RankingRepository;

@Service
@RequiredArgsConstructor
public class RankingService {
  private final RankingRepository rankingRepository;

  public long getGlobalRank(Mode mode, User user) {
    return rankingRepository.getGlobalRank(mode, user);
  }

  public long getCountryRank(Mode mode, User user, CountryCode countryCode) {
    return rankingRepository.getCountryRank(mode, user, countryCode);
  }

  public void update(Mode mode, User user, Stat stat) {
    rankingRepository.updateRanking(mode, user, stat);
  }
}
