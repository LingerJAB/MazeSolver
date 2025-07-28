package io.github.lingerjab.maze

enum class Step(val position: Pair<Int, Int>) {
    UP(0 to 1),
    DOWN(0 to -1),
    LEFT(-1 to 0),
    RIGHT(1 to 0),
    NONE(0 to 0);

    fun reverse(): Step {
        return when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
            else -> NONE
        }
    }

    override fun toString(): String {
        return this.name
    }
}