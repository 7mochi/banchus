package pe.nanamochi.banchus.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy
import pe.nanamochi.banchus.database.entity.Beatmapset
import pe.nanamochi.banchus.domain.enums.BeatmapRankedStatus
import pe.nanamochi.banchus.dto.external.OsuApiBeatmap

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    imports = [BeatmapRankedStatus::class],
)
interface BeatmapsetMapper {
    @Mapping(target = "id", source = "beatmapsetId")
    @Mapping(
        target = "submissionStatus",
        expression = "java(BeatmapRankedStatus.fromValue(apiBeatmap.getApproved()))",
    )
    @Mapping(target = "hasVideo", source = "video")
    @Mapping(target = "hasStoryboard", source = "storyboard")
    @Mapping(target = "submissionDate", source = "submitDate")
    @Mapping(target = "approvedDate", source = "approvedDate")
    @Mapping(target = "lastUpdated", source = "lastUpdate")
    @Mapping(target = "totalPlaycount", constant = "0L")
    @Mapping(target = "beatmaps", ignore = true)
    @Mapping(target = "titleUnicode", ignore = true)
    @Mapping(target = "artistUnicode", ignore = true)
    @Mapping(target = "sourceUnicode", ignore = true)
    fun fromApi(apiBeatmap: OsuApiBeatmap): Beatmapset
}
