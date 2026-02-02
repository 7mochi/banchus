package pe.nanamochi.banchus.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pe.nanamochi.banchus.entities.BeatmapRankedStatus;
import pe.nanamochi.banchus.entities.Mode;
import pe.nanamochi.banchus.entities.db.Beatmap;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    imports = {BeatmapRankedStatus.class, Mode.class})
public interface BeatmapMapper {
  @Mapping(target = "id", source = "beatmapId")
  @Mapping(target = "md5", source = "fileMd5")
  @Mapping(target = "status", expression = "java(BeatmapRankedStatus.fromValue(api.getApproved()))")
  @Mapping(target = "mode", expression = "java(Mode.fromValue(api.getMode()))")
  @Mapping(target = "submissionDate", source = "submitDate")
  @Mapping(target = "lastUpdated", source = "lastUpdate")
  @Mapping(target = "totalLength", source = "totalLength")
  @Mapping(target = "drainLength", source = "hitLength")
  @Mapping(target = "cs", source = "diffSize")
  @Mapping(target = "ar", source = "diffApproach")
  @Mapping(target = "od", source = "diffOverall")
  @Mapping(target = "hp", source = "diffDrain")
  @Mapping(target = "starRating", source = "difficultyRating")
  @Mapping(target = "playcount", constant = "0")
  @Mapping(target = "passcount", constant = "0")
  @Mapping(target = "beatmapset", ignore = true)
  Beatmap fromApi(pe.nanamochi.banchus.entities.osuapi.Beatmap api);
}
