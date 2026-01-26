package pe.nanamochi.banchus.utils;

/**
 * Converts between different beatmap ranked status formats.
 * 
 * osu! API v2 format (stored in database):
 * - GRAVEYARD = -2
 * - WIP = -1
 * - PENDING = 0
 * - RANKED = 1
 * - APPROVED = 2
 * - QUALIFIED = 3
 * - LOVED = 4
 * 
 * Web/Leaderboard format (sent to client):
 * - NOT_SUBMITTED = -1
 * - PENDING = 0
 * - UPDATE_AVAILABLE = 1
 * - RANKED = 2
 * - APPROVED = 3
 * - QUALIFIED = 4
 * - LOVED = 5
 */
public class RankedStatusConverter {

  // API v2 format
  public static final int API_GRAVEYARD = -2;
  public static final int API_WIP = -1;
  public static final int API_PENDING = 0;
  public static final int API_RANKED = 1;
  public static final int API_APPROVED = 2;
  public static final int API_QUALIFIED = 3;
  public static final int API_LOVED = 4;

  // Web/Leaderboard format
  public static final int WEB_NOT_SUBMITTED = -1;
  public static final int WEB_PENDING = 0;
  public static final int WEB_UPDATE_AVAILABLE = 1;
  public static final int WEB_RANKED = 2;
  public static final int WEB_APPROVED = 3;
  public static final int WEB_QUALIFIED = 4;
  public static final int WEB_LOVED = 5;

  /**
   * Convert API v2 ranked status to web/leaderboard format.
   * This is used when sending beatmap status to the osu! client.
   *
   * @param apiStatus The status from osu! API v2 (stored in database)
   * @return The status in web format
   */
  public static int apiToWebStatus(int apiStatus) {
    switch (apiStatus) {
      case API_GRAVEYARD:
      case API_WIP:
      case API_PENDING:
        return WEB_PENDING;
      case API_RANKED:
        return WEB_RANKED;
      case API_APPROVED:
        return WEB_APPROVED;
      case API_QUALIFIED:
        return WEB_QUALIFIED;
      case API_LOVED:
        return WEB_LOVED;
      default:
        return WEB_PENDING; // Default to pending for unknown statuses
    }
  }

  /**
   * Get a human-readable name for an API v2 status.
   *
   * @param apiStatus The status value
   * @return Human-readable status name
   */
  public static String getApiStatusName(int apiStatus) {
    switch (apiStatus) {
      case API_GRAVEYARD:
        return "Graveyard";
      case API_WIP:
        return "WIP";
      case API_PENDING:
        return "Pending";
      case API_RANKED:
        return "Ranked";
      case API_APPROVED:
        return "Approved";
      case API_QUALIFIED:
        return "Qualified";
      case API_LOVED:
        return "Loved";
      default:
        return "Unknown";
    }
  }

  /**
   * Get a human-readable name for a web status.
   *
   * @param webStatus The status value
   * @return Human-readable status name
   */
  public static String getWebStatusName(int webStatus) {
    switch (webStatus) {
      case WEB_NOT_SUBMITTED:
        return "Not Submitted";
      case WEB_PENDING:
        return "Pending";
      case WEB_UPDATE_AVAILABLE:
        return "Update Available";
      case WEB_RANKED:
        return "Ranked";
      case WEB_APPROVED:
        return "Approved";
      case WEB_QUALIFIED:
        return "Qualified";
      case WEB_LOVED:
        return "Loved";
      default:
        return "Unknown";
    }
  }
}
