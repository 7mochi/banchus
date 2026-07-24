package pe.nanamochi.banchus.core.enums

enum class Mods(val value: UInt, val displayName: String, val initial: String) {
    NO_MOD(0u, "No Mod", "NM"),
    NO_FAIL(1u shl 0, "No Fail", "NF"),
    EASY(1u shl 1, "Easy", "EZ"),
    NO_VIDEO(1u shl 2, "No Video", "NV"),
    HIDDEN(1u shl 3, "Hidden", "HD"),
    HARD_ROCK(1u shl 4, "Hard Rock", "HR"),
    SUDDEN_DEATH(1u shl 5, "Sudden Death", "SD"),
    DOUBLE_TIME(1u shl 6, "Double Time", "DT"),
    RELAX(1u shl 7, "Relax", "RX"),
    HALF_TIME(1u shl 8, "Half Time", "HT"),
    NIGHTCORE(1u shl 9, "Nightcore", "NC"),
    FLASHLIGHT(1u shl 10, "Flashlight", "FL"),
    AUTOPLAY(1u shl 11, "Autoplay", "AU"),
    SPUN_OUT(1u shl 12, "Spun Out", "SO"),
    AUTOPILOT(1u shl 13, "Autopilot", "AP"),
    PERFECT(1u shl 14, "Perfect", "PF"),
    KEY4(1u shl 15, "4K", "4K"),
    KEY5(1u shl 16, "5K", "5K"),
    KEY6(1u shl 17, "6K", "6K"),
    KEY7(1u shl 18, "7K", "7K"),
    KEY8(1u shl 19, "8K", "8K"),
    FADE_IN(1u shl 20, "Fade-In", "FI"),
    RANDOM(1u shl 21, "Random", "RD"),
    CINEMA(1u shl 22, "Cinema", "CN"),
    TARGET(1u shl 23, "Target Practice", "TC"),
    KEY9(1u shl 24, "9K", "9K"),
    KEY_COOP(1u shl 25, "Co-Op", "CO"),
    KEY1(1u shl 26, "1K", "1K"),
    KEY3(1u shl 27, "3K", "3K"),
    KEY2(1u shl 28, "2K", "2K"),
    SCORE_V2(1u shl 29, "Score V2", "V2"),
    MIRROR(1u shl 30, "Mirror", "MR");

    companion object {
        val SPEED_CHANGING: UInt = DOUBLE_TIME.value or HALF_TIME.value or NIGHTCORE.value

        val KEY_MODS_MASK: UInt =
            KEY1.value or
                KEY2.value or
                KEY3.value or
                KEY4.value or
                KEY5.value or
                KEY6.value or
                KEY7.value or
                KEY8.value or
                KEY9.value or
                KEY_COOP.value

        val OSU_SPECIFIC_MODS: UInt = SPUN_OUT.value or AUTOPILOT.value

        val MANIA_SPECIFIC_MODS: UInt =
            RANDOM.value or MIRROR.value or FADE_IN.value or KEY_MODS_MASK

        fun fromBitmask(bitmask: UInt): List<Mods> {
            if (bitmask == 0u) return listOf(NO_MOD)
            return entries.filter { it != NO_MOD && (bitmask and it.value) == it.value }
        }

        fun fromInitials(initials: Array<String>): List<Mods> {
            return entries.filter { mod ->
                initials.any { it.equals(mod.initial, ignoreCase = true) }
            }
        }

        fun toBitmask(mods: List<Mods>?): UInt =
            mods?.fold(0u) { acc, mod -> acc or mod.value } ?: 0u

        fun hasConflict(bitmask: UInt): Boolean {
            if ((bitmask and DOUBLE_TIME.value) != 0u && (bitmask and HALF_TIME.value) != 0u) {
                return true
            }

            if ((bitmask and NIGHTCORE.value) != 0u && (bitmask and DOUBLE_TIME.value) == 0u) {
                return true
            }

            if ((bitmask and EASY.value) != 0u && (bitmask and HARD_ROCK.value) != 0u) {
                return true
            }

            if ((bitmask and RELAX.value) != 0u && (bitmask and AUTOPILOT.value) != 0u) {
                return true
            }

            if ((bitmask and HIDDEN.value) != 0u && (bitmask and FADE_IN.value) != 0u) {
                return true
            }

            if ((bitmask and KEY_MODS_MASK).countOneBits() > 1) {
                return true
            }

            return false
        }

        fun filterInvalidModCombinations(bitmask: UInt, mode: Mode): UInt {
            var result = bitmask

            if (
                (result and (DOUBLE_TIME.value or NIGHTCORE.value)) ==
                    (DOUBLE_TIME.value or NIGHTCORE.value)
            ) {
                result = result and DOUBLE_TIME.value.inv()
            }

            if (
                ((result and (DOUBLE_TIME.value or NIGHTCORE.value)) != 0u) &&
                    (result and HALF_TIME.value) != 0u
            ) {
                result = result and HALF_TIME.value.inv()
            }

            if ((result and EASY.value) != 0u && (result and HARD_ROCK.value) != 0u) {
                result = result and HARD_ROCK.value.inv()
            }

            if ((result and (NO_FAIL.value or RELAX.value or AUTOPILOT.value)) != 0u) {
                result = result and SUDDEN_DEATH.value.inv()
                result = result and PERFECT.value.inv()
            }

            if ((result and (RELAX.value or AUTOPILOT.value)) != 0u) {
                result = result and NO_FAIL.value.inv()
            }

            if ((result and PERFECT.value) != 0u && (result and SUDDEN_DEATH.value) != 0u) {
                result = result and SUDDEN_DEATH.value.inv()
            }

            if (mode != Mode.OSU) {
                result = result and OSU_SPECIFIC_MODS.inv()
            }

            if (mode != Mode.MANIA) {
                result = result and MANIA_SPECIFIC_MODS.inv()
            } else {
                result = result and RELAX.value.inv()
                if ((result and HIDDEN.value) != 0u && (result and FADE_IN.value) != 0u) {
                    result = result and FADE_IN.value.inv()
                }
            }

            if (mode == Mode.OSU) {
                if (
                    (result and AUTOPILOT.value) != 0u &&
                        (result and (SPUN_OUT.value or RELAX.value)) != 0u
                ) {
                    result = result and AUTOPILOT.value.inv()
                }
            }

            val keyModsUsed = result and KEY_MODS_MASK
            if (keyModsUsed.countOneBits() > 1) {
                entries
                    .find { (it.value and KEY_MODS_MASK) != 0u && (keyModsUsed and it.value) != 0u }
                    ?.let { first -> result = result and (keyModsUsed and first.value.inv()).inv() }
            }

            return result
        }

        fun filterInvalidModCombinations(mods: List<Mods>, mode: Mode): List<Mods> {
            val bitmask = toBitmask(mods)
            val cleanBitmask = filterInvalidModCombinations(bitmask, mode)
            return fromBitmask(cleanBitmask)
        }

        fun getManiaKeyCount(mods: List<Mods>): Mods {
            val keyPriority = listOf(KEY9, KEY8, KEY7, KEY6, KEY5, KEY4, KEY3, KEY2)
            return keyPriority.find { it in mods } ?: KEY1
        }
    }
}
