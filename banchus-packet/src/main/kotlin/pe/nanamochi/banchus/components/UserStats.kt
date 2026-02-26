package pe.nanamochi.banchus.components

data class UserStats(
    var globalRank: UInt = 0u,
    var rankedScore: ULong = 0UL,
    var totalScore: ULong = 0UL,
    var accuracy: Float = 0f,
    var playCount: UInt = 0u,
    var performancePoints: UShort = 0u,
)
