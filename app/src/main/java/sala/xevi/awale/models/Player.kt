package sala.xevi.awale.models

import sala.xevi.awale.R

class Player (var name: String, var score: Int, var level: Levels = Levels.HUMAN){
    var previousScore = score

    /**
     * Enum class. The strings will be array-levels
     */
    enum class Levels {
        HUMAN,
        SILLY,
        NOT_SO_SILLY,
        INTERMEDIATE,
        SMART,
        VERY_SMART;

    } //resources.getStringArray(R.array.array_levels)[Player.Levels.VERY_SMART.ordinal] <-- will get the number, and we have an array in R.strings

}