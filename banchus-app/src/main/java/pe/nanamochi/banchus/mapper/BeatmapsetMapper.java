package pe.nanamochi.banchus.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pe.nanamochi.banchus.database.entity.Beatmapset;
import pe.nanamochi.banchus.domain.dto.OsuApiBeatmap;
import pe.nanamochi.banchus.domain.enums.BeatmapRankedStatus;

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
      expression = "java(BeatmapRankedStatus.fromValue(apiBeatmap.getApproved()))")
  @Mapping(target = "hasVideo", source = "video")
  @Mapping(target = "hasStoryboard", source = "storyboard")
  @Mapping(target = "submissionDate", source = "submitDate")
  @Mapping(target = "approvedDate", source = "approvedDate")
  @Mapping(target = "lastUpdated", source = "lastUpdate")
  @Mapping(target = "totalPlaycount", constant = "0L")
  @Mapping(target = "languageId", source = "languageId")
  @Mapping(target = "genreId", source = "genreId")
  @Mapping(target = "beatmaps", ignore = true)
  @Mapping(target = "titleUnicode", ignore = true)
  @Mapping(target = "artistUnicode", ignore = true)
  @Mapping(target = "sourceUnicode", ignore = true)
  Beatmapset fromApi(OsuApiBeatmap apiBeatmap);
}
