package pe.nanamochi.banchus.utils;

import org.springframework.http.MediaType;

public class Media {
  public static MediaType getImageMediaType(byte[] data) {
    if (data == null || data.length < 4) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }

    if (data[0] == (byte) 0x89 && data[1] == (byte) 0x50) {
      return MediaType.IMAGE_PNG;
    }

    if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) {
      return MediaType.IMAGE_JPEG;
    }

    return MediaType.APPLICATION_OCTET_STREAM;
  }
}
