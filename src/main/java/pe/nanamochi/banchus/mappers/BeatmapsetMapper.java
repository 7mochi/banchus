package pe.nanamochi.banchus.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pe.nanamochi.banchus.entities.BeatmapRankedStatus;
import pe.nanamochi.banchus.entities.db.Beatmapset;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    imports = BeatmapRankedStatus.class)
public interface BeatmapsetMapper {
  @Mapping(target = "id", source = "beatmapsetId")
  @Mapping(target = "title", source = "title")
  @Mapping(target = "artist", source = "artist")
  @Mapping(target = "source", source = "source")
  @Mapping(target = "creator", source = "creator")
  @Mapping(target = "tags", source = "tags")
  @Mapping(
      target = "submissionStatus",
      expression = "java(BeatmapRankedStatus.fromValue(api.getApproved()))")
  @Mapping(target = "hasVideo", source = "video")
  @Mapping(target = "hasStoryboard", source = "storyboard")
  @Mapping(target = "submissionDate", source = "submitDate")
  @Mapping(target = "approvedDate", source = "approvedDate")
  @Mapping(target = "lastUpdated", source = "lastUpdate")
  @Mapping(target = "totalPlaycount", constant = "0L")
  @Mapping(target = "languageId", source = "languageId")
  @Mapping(target = "genreId", source = "genreId")
  @Mapping(target = "beatmaps", ignore = true)

  // osu!api v1 doesnt provide these fields, so we ignore them
  // when we move to osu!api v2 we can remove these ignores
  @Mapping(target = "titleUnicode", ignore = true)
  @Mapping(target = "artistUnicode", ignore = true)
  @Mapping(target = "sourceUnicode", ignore = true)
  Beatmapset fromApi(pe.nanamochi.banchus.entities.osuapi.Beatmap api);
}
