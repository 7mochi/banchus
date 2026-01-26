package pe.nanamochi.banchus.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pe.nanamochi.banchus.entities.db.Beatmap;
import pe.nanamochi.banchus.repositories.BeatmapRepository;

/**
 * Service for calculating Performance Points (PP) from score data.
 * Uses rosu-pp-jar (Rust binding via JNA) for accurate PP calculations.
 */
@Service
public class PerformancePointsService {

  private static final Logger logger = LoggerFactory.getLogger(PerformancePointsService.class);

  @Autowired
  private BeatmapRepository beatmapRepository;

  @Value("${beatmap.cache.directory:/tmp/banchus-beatmaps}")
  private String beatmapCacheDir;

  private static final String OSU_BEATMAP_URL = "https://osu.ppy.sh/osu/";
  private static File nativeLibrary;

  static {
    try {
      // Cargar la librería nativa .so desde el JAR de rosu-pp-jar
      nativeLibrary = loadNativeLibrary();
      if (nativeLibrary != null) {
        System.load(nativeLibrary.getAbsolutePath());
        logger.info("✓ Native library loaded from: {}", nativeLibrary.getAbsolutePath());
      }
    } catch (Exception e) {
      logger.warn("⚠️ Could not load native library: {}", e.getMessage());
    }
  }

  /**
   * Cargar la librería nativa .so desde el JAR de rosu-pp-jar
   */
  private static File loadNativeLibrary() {
    try {
      String libName = "librosu_pp_java.so";
      String osName = System.getProperty("os.name").toLowerCase();
      
      // Determinar el nombre del archivo según el SO
      if (osName.contains("win")) {
        libName = "rosu_pp_java.dll";
      } else if (osName.contains("mac")) {
        libName = "librosu_pp_java.dylib";
      }
      
      // Obtener la librería desde el ClassLoader
      InputStream libStream = PerformancePointsService.class.getClassLoader()
          .getResourceAsStream(libName);
      
      if (libStream == null) {
        System.err.println("⚠️ Native library not found in JAR: " + libName);
        return null;
      }
      
      // Crear archivo temporal
      File tempDir = new File(System.getProperty("java.io.tmpdir"));
      File tempLib = new File(tempDir, libName);
      
      // Copiar archivo
      Files.copy(libStream, tempLib.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      tempLib.deleteOnExit();
      
      System.out.println("✓ Native library extracted to: " + tempLib.getAbsolutePath());
      return tempLib;
      
    } catch (Exception e) {
      System.err.println("❌ Error loading native library: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  public double calculatePP(
      String mapMd5,
      int n300,
      int n100,
      int n50,
      int nmiss,
      int maxCombo,
      int mods,
      double accuracy,
      int gameMode) {

    try {
      logger.info("📊 Calculating PP using rosu-pp-jar:");
      logger.info("  - Beatmap MD5: {}", mapMd5);
      logger.info("  - Hits: 300={}, 100={}, 50={}, Misses={}", n300, n100, n50, nmiss);
      logger.info("  - Max Combo: {} | Accuracy: {}%", maxCombo, String.format("%.2f", accuracy));

      Optional<Beatmap> beatmapOpt = beatmapRepository.findByMd5(mapMd5);
      if (beatmapOpt.isEmpty()) {
        logger.warn("⚠️ Beatmap not found in database for MD5: {}", mapMd5);
        return 0.0;
      }

      Beatmap beatmap = beatmapOpt.get();
      logger.info("✓ Beatmap found: {} - {} [{}]", 
          beatmap.getArtist(), beatmap.getTitle(), beatmap.getVersion());

      File osuFile = getOrDownloadBeatmapFile(beatmap);
      if (osuFile == null || !osuFile.exists()) {
        logger.warn("⚠️ Could not get beatmap file");
        return 0.0;
      }

      logger.info("✓ Beatmap file ready: {}", osuFile.getAbsolutePath());

      double pp = calculatePPWithRosuPp(
          osuFile,
          n300, n100, n50, nmiss,
          maxCombo, mods, accuracy, gameMode
      );

      logger.info("✓ PP calculated: {}", String.format("%.2f", pp));
      return pp;

    } catch (Exception e) {
      logger.error("❌ Error calculating PP: {}", e.getMessage());
      e.printStackTrace();
      return 0.0;
    }
  }

  private double calculatePPWithRosuPp(
      File osuFile,
      int n300, int n100, int n50, int nmiss,
      int maxCombo, int mods,
      double accuracy, int gameMode) {

    try {
      logger.info("🔧 Loading rosu-pp-jar native library...");

      // Use the generated classes from rosu-pp-jar
      Class<?> beatmapClass = Class.forName("pe.nanamochi.rosu_pp_jar.Beatmap");
      Class<?> performanceClass = Class.forName("pe.nanamochi.rosu_pp_jar.Performance");
      Class<?> modsClass = Class.forName("pe.nanamochi.rosu_pp_jar.Mods");

      logger.info("✓ rosu-pp-jar classes loaded successfully");

      // Parse beatmap using Beatmap.fromPath(String path)
      Object beatmap = beatmapClass
          .getMethod("fromPath", String.class)
          .invoke(null, osuFile.getAbsolutePath());

      logger.info("✓ Beatmap parsed by rosu-pp");

      // Create Mods object from bits
      Object modsObj = null;
      if (mods > 0) {
        modsObj = modsClass
            .getMethod("fromBits", Integer.class)
            .invoke(null, mods);
        logger.info("✓ Mods object created from bits: {}", mods);
      }

      // Create Performance object using Performance.create(Beatmap)
      Object performance = performanceClass
          .getMethod("create", beatmapClass)
          .invoke(null, beatmap);

      // Set score parameters using setter methods
      performanceClass.getMethod("setAccuracy", Double.class)
          .invoke(performance, accuracy);

      performanceClass.getMethod("setCombo", Integer.class)
          .invoke(performance, maxCombo);

      performanceClass.getMethod("setMisses", Integer.class)
          .invoke(performance, nmiss);

      if (modsObj != null) {
        performanceClass.getMethod("setMods", modsClass)
            .invoke(performance, modsObj);
      }

      logger.info("✓ Score parameters set: accuracy={}, combo={}, misses={}, mods={}", 
          accuracy, maxCombo, nmiss, mods);

      // Calculate PP using calculate() method
      Double pp = (Double) performanceClass
          .getMethod("calculate")
          .invoke(performance);

      logger.info("✓ PP from rosu-pp: {}", String.format("%.2f", pp));
      
      // Clean up resources
      if (performance instanceof AutoCloseable) {
        ((AutoCloseable) performance).close();
      }
      if (beatmap instanceof AutoCloseable) {
        ((AutoCloseable) beatmap).close();
      }
      if (modsObj != null && modsObj instanceof AutoCloseable) {
        ((AutoCloseable) modsObj).close();
      }
      
      return pp != null ? pp : 0.0;

    } catch (ClassNotFoundException e) {
      logger.error("❌ rosu-pp-jar classes not found: {}", e.getMessage());
      logger.error("   Make sure rosu-pp-jar is in the classpath");
      return 0.0;
    } catch (Exception e) {
      logger.error("❌ Error using rosu-pp-jar: {}", e.getMessage());
      e.printStackTrace();
      return 0.0;
    }
  }

  private File getOrDownloadBeatmapFile(Beatmap beatmap) {
    try {
      Path cachePath = Paths.get(beatmapCacheDir);
      Files.createDirectories(cachePath);

      File cachedFile = cachePath.resolve(beatmap.getMd5() + ".osu").toFile();

      if (cachedFile.exists()) {
        logger.info("✓ Using cached beatmap file");
        return cachedFile;
      }

      Integer beatmapId = beatmap.getId();
      if (beatmapId == null || beatmapId == 0) {
        logger.warn("⚠️ Beatmap ID not available");
        return null;
      }

      String downloadUrl = OSU_BEATMAP_URL + beatmapId;
      logger.info("📥 Downloading from: {}", downloadUrl);

      downloadFile(downloadUrl, cachedFile);

      if (cachedFile.exists()) {
        logger.info("✓ Beatmap cached");
        return cachedFile;
      }

      return null;

    } catch (IOException e) {
      logger.error("❌ Error getting beatmap file: {}", e.getMessage());
      return null;
    }
  }

  private void downloadFile(String urlString, File destination) throws IOException {
    try (var in = new URL(urlString).openStream()) {
      Files.copy(in, destination.toPath());
    }
  }

  @Cacheable(value = "beatmapStars", key = "#mapMd5")
  public double getCachedStarRating(String mapMd5) {
    return 0.0;
  }

  public double calculatePPFromStars(
      double starRating, int n300, int n100, int n50, int nmiss,
      int maxCombo, int mods, double accuracy, int maxComboOnBeatmap) {
    return 0.0;
  }
}
