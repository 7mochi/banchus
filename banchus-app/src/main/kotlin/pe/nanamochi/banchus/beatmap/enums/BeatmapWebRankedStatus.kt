package pe.nanamochi.banchus.beatmap.enums

enum class BeatmapWebRankedStatus(val value: Int) {
    NOT_SUBMITTED(-1),
    PENDING(0),
    UPDATE_AVAILABLE(1),
    RANKED(2),
    APPROVED(3),
    QUALIFIED(4),
    LOVED(5);

    companion object {
        fun fromValue(value: Int): BeatmapWebRankedStatus {
            return entries.find { it.value == value } ?: NOT_SUBMITTED
        }

        fun convertToWebStatus(rankedStatus: BeatmapRankedStatus?): Int {
            if (rankedStatus == null) return NOT_SUBMITTED.value

            return when (rankedStatus) {
                BeatmapRankedStatus.GRAVEYARD,
                BeatmapRankedStatus.WIP,
                BeatmapRankedStatus.PENDING -> PENDING.value

                BeatmapRankedStatus.RANKED -> RANKED.value
                BeatmapRankedStatus.APPROVED -> APPROVED.value
                BeatmapRankedStatus.QUALIFIED -> QUALIFIED.value
                BeatmapRankedStatus.LOVED -> LOVED.value
            }
        }
    }
}
