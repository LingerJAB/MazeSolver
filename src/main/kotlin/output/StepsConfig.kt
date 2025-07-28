package io.github.lingerjab.output

data class StepsConfig(
    val left: String = "L",
    val right: String = "R",
    val up: String = "U",
    val down: String = "D",
    val spliterator: String = ", ",
    val isReversed: Boolean = false,
)