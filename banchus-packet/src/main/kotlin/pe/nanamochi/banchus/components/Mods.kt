package pe.nanamochi.banchus.components

enum class Mods(val value: UInt) {
    NO_MOD(0u),
    NO_FAIL(1u),
    EASY(1u shl 1),
    TOUCHSCREEN(1u shl 2),
    HIDDEN(1u shl 3),
    HARD_ROCK(1u shl 4),
    SUDDEN_DEATH(1u shl 5),
    DOUBLE_TIME(1u shl 6),
    RELAX(1u shl 7),
    HALF_TIME(1u shl 8),
    NIGHTCORE(1u shl 9),
    FLASHLIGHT(1u shl 10),
    AUTOPLAY(1u shl 11),
    SPUN_OUT(1u shl 12),
    AUTOPILOT(1u shl 13),
    PERFECT(1u shl 14),
    KEY4(1u shl 15),
    KEY5(1u shl 16),
    KEY6(1u shl 17),
    KEY7(1u shl 18),
    KEY8(1u shl 19),
    FADE_IN(1u shl 20),
    RANDOM(1u shl 21),
    CINEMA(1u shl 22),
    TARGET(1u shl 23),
    KEY9(1u shl 24),
    KEY_COOP(1u shl 25),
    KEY1(1u shl 26),
    KEY3(1u shl 27),
    KEY2(1u shl 28),
    SCORE_V2(1u shl 29),
    MIRROR(1u shl 30);

    companion object {
        fun fromBitmask(bitmask: UInt): List<Mods> {
            if (bitmask == 0u) return listOf(NO_MOD)

            val selectedMods =
                entries.filter { mod -> mod != NO_MOD && (bitmask and mod.value) == mod.value }

            return selectedMods.ifEmpty { listOf(NO_MOD) }
        }

        fun toBitmask(mods: List<Mods>?): UInt {
            if (mods.isNullOrEmpty()) return NO_MOD.value
            return mods.fold(0u) { acc, mod -> acc or mod.value }
        }
    }
}

fun UInt.hasAny(mask: UInt): Boolean {
    return (this and mask) != 0u
}
