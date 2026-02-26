package pe.nanamochi.banchus.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import pe.nanamochi.banchus.database.entity.Beatmap
import pe.nanamochi.banchus.domain.enums.BeatmapRankedStatus
import pe.nanamochi.banchus.domain.enums.Mode
import pe.nanamochi.banchus.dto.OsuApiBeatmap

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    imports = [BeatmapRankedStatus::class, Mode::class],
)
interface BeatmapMapper {
    @Mapping(target = "id", source = "beatmapId")
    @Mapping(target = "md5", source = "fileMd5")
    @Mapping(
        target = "status",
        expression = "java(BeatmapRankedStatus.fromValue(apiBeatmap.getApproved()))",
    )
    @Mapping(target = "mode", expression = "java(Mode.fromValue(apiBeatmap.getMode()))")
    @Mapping(target = "submissionDate", source = "submitDate")
    @Mapping(target = "lastUpdated", source = "lastUpdate")
    @Mapping(target = "totalLength", source = "totalLength")
    @Mapping(target = "drainLength", source = "hitLength")
    @Mapping(target = "cs", source = "diffSize")
    @Mapping(target = "ar", source = "diffApproach")
    @Mapping(target = "od", source = "diffOverall")
    @Mapping(target = "hp", source = "diffDrain")
    @Mapping(target = "starRating", source = "difficultyRating")
    @Mapping(target = "playcount", constant = "0L")
    @Mapping(target = "passcount", constant = "0L")
    @Mapping(target = "beatmapset", ignore = true)
    fun fromApi(apiBeatmap: OsuApiBeatmap): Beatmap
}
