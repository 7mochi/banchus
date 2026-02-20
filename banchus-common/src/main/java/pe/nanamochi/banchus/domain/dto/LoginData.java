package pe.nanamochi.banchus.domain.dto;

public record LoginData(
    String username,
    String passwordMd5,
    String osuVersion,
    int utcOffset,
    boolean displayCity,
    boolean pmPrivate,
    String osuPathMd5,
    String adaptersStr,
    String adaptersMd5,
    String uninstallMd5,
    String diskSignatureMd5) {}
