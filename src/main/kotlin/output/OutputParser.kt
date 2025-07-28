package io.github.lingerjab.output

import io.github.lingerjab.maze.CellType
import io.github.lingerjab.maze.CellType.*
import io.github.lingerjab.maze.Maze
import io.github.lingerjab.maze.Step

fun parseSteps(mazePath: List<Pair<Int, Int>>, stepsConfig: StepsConfig): String {
    val (left, right, up, down,spliterator,isReversed) = stepsConfig
    var steps = mutableListOf<Step>()
    for (i in 1 until mazePath.size) {
        val cur = mazePath[i]
        val prev = mazePath[i - 1]
        when {
            cur.first > prev.first -> steps.add(Step.DOWN)
            cur.first < prev.first -> steps.add(Step.UP)
            cur.second > prev.second -> steps.add(Step.RIGHT)
            cur.second < prev.second -> steps.add(Step.LEFT)
        }
    }
    if (isReversed) steps = steps.map { it.reverse() }.reversed().toMutableList()

    return steps.joinToString(spliterator) {
        when (it) {
            Step.UP -> up
            Step.DOWN -> down
            Step.LEFT -> left
            Step.RIGHT -> right
            Step.NONE -> ""
        }
    }
}

fun parsePath(mazePath: List<Pair<Int, Int>>, pathConfig: PathConfig): String {
    val (isXY, isReversed, startWithZero) = pathConfig
    var mazePath = mazePath.map { it.first to it.second }
    if (isXY) mazePath = mazePath.map { it.second to it.first }
    if (!startWithZero) mazePath= mazePath.map { it.first + 1 to it.second + 1 }
    if (isReversed) mazePath = mazePath.reversed()

    return mazePath.joinToString(" ")
}

fun parseText(maze: Maze, textConfig: TextConfig): String {
    val (start, end, wall, empty, visited, spliterator) = textConfig
    return maze.cellArray.joinToString("\n") { row ->
        row.joinToString(spliterator) { cell ->
            when (cell) {
                START -> start
                END -> end
                WALL -> wall
                EMPTY -> empty
                VISITED -> visited
            }
        }
    }
}


fun parseCode(maze: Maze, languageConfig: LanguageConfig): String {
    val lang = languageConfig.language
    fun cellToChar(cell: CellType): String = when (cell) {
        START -> languageConfig.start
        END -> languageConfig.end
        WALL -> languageConfig.wall
        EMPTY -> languageConfig.empty
        VISITED -> languageConfig.visited
    }

    val grid = maze.cellArray
    val rows = grid.map { row -> row.map { cellToChar(it) } }

    return when (lang) {
        OutputLang.C -> {
            val rowStrs = rows.joinToString(",\n    ") { row ->
                "\"${row.joinToString("")}\""
            }
            "char grid[${rows.size}][${rows[0].size + 1}] = {\n    $rowStrs\n};"
        }

        OutputLang.Python -> {
            val rowStrs = rows.joinToString(",\n  ") { row ->
                "[${row.joinToString(", ") { "\"$it\"" }}]"
            }
            "grid = [\n  $rowStrs\n]"
        }

        OutputLang.Kotlin -> {
            val rowStrs = rows.joinToString(",\n    ") { row ->
                "listOf(${row.joinToString(", ") { "'$it'" }})"
            }
            "val grid = listOf(\n    $rowStrs\n)"
        }

        OutputLang.Java -> {
            val rowStrs = rows.joinToString(",\n    ") { row ->
                "{${row.joinToString(", ") { "'$it'" }}}"
            }
            "char[][] grid = {\n    $rowStrs\n};"
        }

        OutputLang.Json -> {
            val rowStrs = rows.joinToString(",\n  ") { row ->
                "[${row.joinToString(", ") { "\"$it\"" }}]"
            }
            "[\n  $rowStrs\n]"
        }
    }
}