package pe.nanamochi.banchus.domain.enums

enum class SubmissionStatus(val value: Int) {
    FAILED(0),
    SUBMITTED(1),
    BEST(2);

    companion object {
        fun fromValue(value: Int): SubmissionStatus =
            entries.find { it.value == value } ?: SUBMITTED
    }
}
