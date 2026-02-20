package pe.nanamochi.banchus.domain.dto;

public record Geolocation(
    String status,
    String country,
    String countryCode,
    String region,
    String regionName,
    String city,
    String zip,
    float lat,
    float lon,
    String timezone,
    String isp,
    String org,
    String as,
    String query) {
  public static Geolocation local() {
    return new Geolocation(
        null, null, "kp", null, null, null, null, 0.0f, 0.0f, null, null, null, null, null);
  }
}
