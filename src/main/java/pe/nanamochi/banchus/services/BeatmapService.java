package pe.nanamochi.banchus.services;

import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.adapters.OsuApiV2Adapter;
import pe.nanamochi.banchus.entities.db.Beatmap;
import pe.nanamochi.banchus.repositories.BeatmapRepository;

/**
 * Service for managing beatmap operations.
 * Responsible for:
 * - Fetching beatmaps from local cache or osu! API v2
 * - Creating dummy beatmaps when API fails
 * - Validating beatmap status (ranked/approved/loved)
 * - Updating beatmap play/pass counts
 */
@Service
public class BeatmapService {

  private static final Logger logger = LoggerFactory.getLogger(BeatmapService.class);

  @Autowired private BeatmapRepository beatmapRepository;

  @Autowired private OsuApiV2Adapter osuApiAdapter;

  /**
   * Get beatmap from local DB or fetch from osu! API v2.
   * Attempts to fetch from local cache first, then from osu! API if not found.
   * Creates a dummy beatmap if API fails to ensure score can still be saved.
   *
   * @param mapMd5 The beatmap MD5 hash
   * @return Beatmap entity or null if unable to obtain beatmap
   */
  public Beatmap getOrFetchBeatmap(String mapMd5) {
    // Try to find locally first
    Optional<Beatmap> existingOpt = beatmapRepository.findByMd5(mapMd5);
    if (existingOpt.isPresent()) {
      logger.info("✓ Beatmap found locally: {} - {} [{}]",
          existingOpt.get().getArtist(),
          existingOpt.get().getTitle(),
          existingOpt.get().getVersion());
      return existingOpt.get();
    }

    // Fetch from osu! API v2
    logger.info("🌐 Fetching beatmap from osu! API v2: {}", mapMd5);
    Beatmap beatmapFromApi = osuApiAdapter.lookupBeatmapByMd5(mapMd5);

    if (beatmapFromApi != null) {
      beatmapFromApi.setServer("osuapi");
      Beatmap saved = beatmapRepository.save(beatmapFromApi);
      logger.info("✓ Beatmap saved from API: {} - {} [{}]",
          saved.getArtist(), saved.getTitle(), saved.getVersion());
      return saved;
    }

    // Create dummy beatmap if API fails
    logger.warn("⚠️ Could not fetch from API, creating dummy beatmap");
    return createDummyBeatmap(mapMd5);
  }

  /**
   * Create a dummy beatmap when the osu! API fails.
   * This allows scores to be saved even if beatmap metadata is unavailable.
   *
   * @param mapMd5 The beatmap MD5 hash
   * @return The saved dummy beatmap
   */
  private Beatmap createDummyBeatmap(String mapMd5) {
    Beatmap dummy = Beatmap.builder()
        .md5(mapMd5)
        .artist("Unknown")
        .title("Unknown")
        .creator("Unknown")
        .version("Unknown")
        .status(0) // Unranked
        .setId(0)
        .cs(0f)
        .ar(0f)
        .od(0f)
        .hp(0f)
        .bpm(0f)
        .maxCombo(0)
        .playcount(0)
        .passcount(0)
        .lastUpdate(LocalDateTime.now())
        .server("bancho")
        .build();

    Beatmap savedDummy = beatmapRepository.save(dummy);
    logger.info("✓ Dummy beatmap saved: {}", mapMd5);
    return savedDummy;
  }

  /**
   * Determine if a beatmap awards ranked score and PP.
   * In osu! convention:
   * - 0 = Unranked (no ranked score or PP)
   * - 1 = Ranked (awards both)
   * - 2 = Approved (awards both)
   * - 3 = Loved (awards ranked score only, no PP)
   *
   * @param beatmap The beatmap to check
   * @return true if beatmap is Ranked (1), Approved (2), or Loved (3)
   */
  public boolean isBeatmapRanked(Beatmap beatmap) {
    if (beatmap == null) {
      return false;
    }

    // Status 1 = Ranked, 2 = Approved, 3 = Loved
    boolean ranked = beatmap.getStatus() >= 1 && beatmap.getStatus() <= 3;
    logger.info("  Beatmap status: {} | Is ranked/approved/loved: {}", beatmap.getStatus(), ranked);
    return ranked;
  }

  /**
   * Update beatmap play/pass counts after a score submission.
   * Increments playcount by 1 and passcount by 1 if the score passed.
   *
   * @param beatmap The beatmap to update
   * @param passed Whether the score was passed
   */
  public void updatePlayPassCounts(Beatmap beatmap, boolean passed) {
    if (beatmap == null) {
      logger.warn("Cannot update play/pass counts: beatmap is null");
      return;
    }

    // Ensure playcount and passcount are never null
    if (beatmap.getPlaycount() == null) {
      beatmap.setPlaycount(0);
    }
    if (beatmap.getPasscount() == null) {
      beatmap.setPasscount(0);
    }

    // Increment playcount
    beatmap.setPlaycount(beatmap.getPlaycount() + 1);

    // Increment passcount if score was passed
    if (passed) {
      beatmap.setPasscount(beatmap.getPasscount() + 1);
    }

    beatmapRepository.save(beatmap);
    logger.info("✓ Beatmap stats updated: playcount={}, passcount={}", 
        beatmap.getPlaycount(), beatmap.getPasscount());
  }

  /**
   * Get beatmap by MD5 hash.
   * Returns the beatmap if found, null otherwise.
   *
   * @param mapMd5 The beatmap MD5 hash
   * @return The beatmap or null if not found
   */
  public Beatmap getBeatmapByMd5(String mapMd5) {
    Optional<Beatmap> beatmap = beatmapRepository.findByMd5(mapMd5);
    return beatmap.orElse(null);
  }

  /**
   * Check if a beatmap exists in the database.
   *
   * @param mapMd5 The beatmap MD5 hash
   * @return true if beatmap exists, false otherwise
   */
  public boolean beatmapExists(String mapMd5) {
    return beatmapRepository.findByMd5(mapMd5).isPresent();
  }
}
