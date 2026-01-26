package pe.nanamochi.banchus.controllers;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.nanamochi.banchus.adapters.OsuApiV2Adapter;
import pe.nanamochi.banchus.entities.db.Beatmap;
import pe.nanamochi.banchus.repositories.BeatmapRepository;
import pe.nanamochi.banchus.services.LeaderboardService;
import pe.nanamochi.banchus.services.ReplayService;

/**
 * Controlador para endpoints /web/* que son usados por el cliente osu!
 * para descargar leaderboards, replays, etc.
 * 
 * Responsabilidades:
 * - Validar y parsear parámetros HTTP
 * - Delegar lógica de negocio a servicios
 * - Formatear respuestas HTTP
 */
@RestController
@RequestMapping("/bancho/web")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class WebController {

  private static final Logger logger = LoggerFactory.getLogger(WebController.class);

  @Autowired private BeatmapRepository beatmapRepository;

  @Autowired private OsuApiV2Adapter osuApiAdapter;

  @Autowired private LeaderboardService leaderboardService;

  @Autowired private ReplayService replayService;

  /**
   * GET /web/osu-osz2-getscores.php Retorna el leaderboard de scores para un beatmap Solo
   * scores pasados (status=1) en orden de PP descendente
   */
  @GetMapping("/osu-osz2-getscores.php")
  public ResponseEntity<String> getScores(
      @RequestParam(value = "k", required = false) String apiKey,
      @RequestParam(value = "vv", required = false) String version,
      @RequestParam(value = "v", required = false) String beatmapMd5,
      @RequestParam(value = "c", required = false) String beatmapMd5Alt,
      @RequestParam(value = "m", required = false, defaultValue = "0") Integer gameMode,
      @RequestParam(value = "mods", required = false, defaultValue = "0") Integer mods,
      @RequestParam(value = "country", required = false) String country,
      @RequestParam(value = "f", required = false) String filename,
      @RequestParam(value = "t", required = false, defaultValue = "0") String type) {

    // The 'c' parameter is the actual beatmap MD5 hash
    String actualMapMd5 = beatmapMd5Alt;
    if (actualMapMd5 == null || actualMapMd5.isEmpty()) {
      actualMapMd5 = beatmapMd5;
    }

    if (actualMapMd5 == null || actualMapMd5.isEmpty()) {
      logger.error("═══════════════════════════════════════════════════════════\n" +
          "❌ No beatmap MD5 provided\n" +
          "═══════════════════════════════════════════════════════════");
      return ResponseEntity.ok("");
    }

    logger.info("═══════════════════════════════════════════════════════════\n" +
        "📥 [GET] /web/osu-osz2-getscores.php\n" +
        "  RAW PARAMS:\n" +
        "    v (expected v) = {}\n" +
        "    c (beatmap hash) = {}\n" +
        "    vv (version) = {}\n" +
        "    f (filename) = {}\n" +
        "  Final beatmap_md5 = {}\n" +
        "  type = {}\n" +
        "  country = {}", 
        beatmapMd5, beatmapMd5Alt, version, mods, actualMapMd5, type, country);

    try {
      // Step 1: Check if beatmap exists locally
      var beatmapOpt = beatmapRepository.findByMd5(actualMapMd5);
      Beatmap beatmap = null;
      
      if (beatmapOpt.isPresent()) {
        beatmap = beatmapOpt.get();
      } else {
        // Step 2: If not found locally, try to get from osu! API
        beatmap = osuApiAdapter.lookupBeatmapByMd5(actualMapMd5);
        
        if (beatmap != null) {
          // Save to local database for future requests
          beatmap = beatmapRepository.save(beatmap);
        } else {
          // Create a dummy beatmap to avoid nulls in database
          Beatmap dummy = Beatmap.builder()
              .md5(actualMapMd5)
              .artist("Unknown Artist")
              .title("Unknown Title")
              .creator("Unknown Creator")
              .version("Unknown")
              .status(0)
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
          beatmap = beatmapRepository.save(dummy);
        }
      }
      
      // Step 3: Search for scores using LeaderboardService (filtered by game mode)
      var scores = leaderboardService.getScoresFiltered(actualMapMd5, gameMode);

      // Formatear respuesta usando LeaderboardService con formato correcto para osu!
      String formattedResponse = leaderboardService.formatLeaderboardResponse(beatmap, scores);
      
      logger.info("🔍 Searching for beatmap with MD5: {}\n" +
          "✓ Beatmap found: id={}, title={}\n" +
          "🔍 Searching for scores with beatmap MD5: {}\n" +
          "✓ Query executed, found {} scores\n" +
          "✓ Retornando {} scores para {}\n" +
          "═══════════════════════════════════════════════════════════",
          actualMapMd5, beatmap != null ? beatmap.getId() : "null", 
          beatmap != null ? beatmap.getTitle() : "null",
          actualMapMd5, scores != null ? scores.size() : 0, 
          scores != null ? scores.size() : 0, actualMapMd5);

      return ResponseEntity.ok(formattedResponse);

    } catch (Exception e) {
      logger.error("═══════════════════════════════════════════════════════════\n" +
          "❌ Error al obtener scores: {}\n" +
          "═══════════════════════════════════════════════════════════", 
          e.getMessage(), e);
      return ResponseEntity.status(500).body("error");
    }
  }

  /**
   * GET /web/osu-getreplay.php Descarga un replay para un score específico
   */
  @GetMapping("/osu-getreplay.php")
  public ResponseEntity<Resource> getReplay(
      @RequestParam(value = "c", required = false) String scoreId,
      @RequestParam(value = "u", required = false) String username,
      @RequestParam(value = "m", required = false) String beatmapId,
      @RequestParam(value = "h", required = false) String fileHash) {

    logger.info("═══════════════════════════════════════════════════════════");
    logger.info("📥 [GET] /web/osu-getreplay.php");
    logger.info("  scoreId = {}", scoreId);
    logger.info("  username = {}", username);
    logger.info("  beatmapId = {}", beatmapId);
    logger.info("═══════════════════════════════════════════════════════════");

    if (scoreId == null || scoreId.isEmpty()) {
      logger.error("❌ No scoreId provided");
      return ResponseEntity.badRequest().build();
    }

    try {
      byte[] replayData = replayService.getReplay(Long.parseLong(scoreId));

      if (replayData == null || replayData.length == 0) {
        logger.info("⚠️  No replay found for score {}", scoreId);
        return ResponseEntity.notFound().build();
      }

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.setContentLength(replayData.length);
      // Use setContentDisposition with Attachment disposition instead of form-data
      headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
          .filename("replay.osr")
          .build());

      logger.info("✓ Returning replay for score {}: {} bytes", scoreId, replayData.length);

      return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(replayData));

    } catch (Exception e) {
      logger.error("❌ Error downloading replay: {}", e.getMessage(), e);
      return ResponseEntity.status(500).build();
    }
  }

  /**
   * GET /web/osu-rate.php Simple confirmation endpoint for rating/submission
   * The osu! client calls this to confirm beatmap submission before posting the score
   */
  @GetMapping("/osu-rate.php")
  public ResponseEntity<String> rateGetHandler(
      @RequestParam(value = "u", required = false) String username,
      @RequestParam(value = "p", required = false) String pass,
      @RequestParam(value = "c", required = false) String mapMd5) {

    logger.info("═══════════════════════════════════════════════════════════");
    logger.info("📥 [GET] /web/osu-rate.php");
    logger.info("  username = {}", username);
    logger.info("  mapMd5 = {}", mapMd5);
    logger.info("═══════════════════════════════════════════════════════════");

    // Just return OK - client uses this as a ping to confirm server is ready for score submission
    return ResponseEntity.ok("ok");
  }

  /**
   * Catch-all para loguear peticiones desconocidas a /web/*
   */
  @GetMapping("/**")
  public ResponseEntity<String> logGetRequest(HttpServletRequest request) {
    String path = request.getRequestURI();
    String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";

    logger.info("═══════════════════════════════════════════════════════════");
    logger.info("📨 [GET] {} {}", path, queryString);
    logger.info("User-Agent: {}", request.getHeader("User-Agent"));
    logger.info("═══════════════════════════════════════════════════════════");

    return ResponseEntity.status(404).body("Not Found");
  }

  /**
   * Catch-all para loguear peticiones POST desconocidas a /web/*
   */
  @PostMapping("/**")
  public ResponseEntity<String> logPostRequest(
      HttpServletRequest request, @RequestBody(required = false) byte[] body) {
    String path = request.getRequestURI();
    String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";

    logger.info("═══════════════════════════════════════════════════════════");
    logger.info("📨 [POST] {} {}", path, queryString);
    logger.info("User-Agent: {}", request.getHeader("User-Agent"));
    if (body != null) {
      logger.info("Body size: {} bytes", body.length);
    }
    logger.info("═══════════════════════════════════════════════════════════");

    return ResponseEntity.status(404).body("Not Found");
  }
}
