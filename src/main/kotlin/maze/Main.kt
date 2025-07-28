package io.github.lingerjab.maze

import io.github.lingerjab.output.LanguageConfig
import io.github.lingerjab.output.OutputLang
import io.github.lingerjab.output.PathConfig
import io.github.lingerjab.output.StepsConfig
import io.github.lingerjab.output.TextConfig
import io.github.lingerjab.output.parseCode
import io.github.lingerjab.output.parsePath
import io.github.lingerjab.output.parseSteps
import io.github.lingerjab.output.parseText

fun main() {
    var str = """
        1 S 1 1 1 1 1 1 1
        1 0 0 1 0 0 0 0 1
        1 1 0 1 0 1 1 1 1
        1 0 0 0 0 1 0 0 E
        1 1 1 1 0 0 0 0 1
        1 1 0 1 0 1 1 0 1
        1 0 0 0 0 0 0 0 1
    """.trimIndent()
    println(str)
    println()

    val maze = parseMaze(str)
    val path = solveMaze(maze)
    if (path == null) error("No path found")

    println(parseSteps(path, StepsConfig()))
    println(parsePath(path, PathConfig(true,true,false)))
    println(parseText(maze, TextConfig()))
    println(parseCode(maze, LanguageConfig(language = OutputLang.Json)))
}

