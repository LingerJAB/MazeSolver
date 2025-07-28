package io.github.lingerjab.maze

import java.util.LinkedList
import java.util.Queue

fun parseMaze(
    str: String,
    startChar: Char = 'S',
    endChar: Char = 'E',
    wallChar: Char = '1',
    emptyChar: Char = '0',
    ignoreChars: String = ""
): Maze {
    var start: Pair<Int, Int>? = null
    var end: Pair<Int, Int>? = null

    val lines = str.lines()
        .filter { it.isNotBlank() } // 忽略空行
        .map { it.trim().toCharArray().filter { it != ' ' && it !in ignoreChars } }

    val maxCols = lines.maxOfOrNull { it.size } ?: 0
    val rowCount = lines.size

    val mazeArray = Array(rowCount) { row ->
        Array(maxCols) { col ->
            val token = lines.getOrNull(row)?.getOrNull(col)
            when (token) {
                startChar -> {
                    start = row to col
                    CellType.START
                }

                endChar -> {
                    end = row to col
                    CellType.END
                }

                emptyChar -> CellType.EMPTY
                wallChar -> CellType.WALL
                null -> CellType.WALL // 缺口补 WALL
                else -> throw IllegalArgumentException("Invalid keyword: '$token' at row=$row, col=$col")
            }
        }
    }

    return Maze(
        cellArray = mazeArray,
        start = start,
        end = end
    )
}

fun parseMazeWithBlank(
    str: String,
    startChar: Char = 'S',
    endChar: Char = 'E',
    wallChar: Char = '1',
    ignoreChars: String = ""
): Maze {
    var start: Pair<Int, Int>? = null
    var end: Pair<Int, Int>? = null

    val lines = str.filter { it !in ignoreChars }.lines() // 不过滤空行
    val rowCount = lines.size
    val maxCols = lines.maxOfOrNull { it.length } ?: 0

    val mazeArray = Array(rowCount) { row ->
        Array(maxCols) { col ->
            val ch = lines[row].getOrNull(col)
            when (ch) {
                startChar -> {
                    start = row to col
                    CellType.START
                }

                endChar -> {
                    end = row to col
                    CellType.END
                }

                wallChar -> CellType.WALL
                ' ' -> CellType.EMPTY
                null -> CellType.EMPTY // 缺口补空格
                else -> throw IllegalArgumentException("Invalid character: '$ch' at row=$row, col=$col")
            }
        }
    }

    return Maze(
        cellArray = mazeArray,
        start = start,
        end = end
    )
}


/**
 * @return 始末路径 the path from start to end
 */
fun solveMaze(maze: Maze): List<Pair<Int, Int>>? {
    val mazeArr = maze.cellArray
    val rows = maze.width
    val cols = maze.height

    var start: Pair<Int, Int>? = null
    var end: Pair<Int, Int>? = null

    // 找起点和终点
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            when (mazeArr[r][c]) {
                CellType.START -> start = r to c
                CellType.END -> end = r to c
                else -> {}
            }
        }
    }

    if (start == null || end == null) {
        println("未找到起点或终点")
        return null
    }

    // BFS：队列 + 前驱记录
    val queue: Queue<Pair<Int, Int>> = LinkedList()
    val visited = Array(rows) { BooleanArray(cols) }
    val prev = Array(rows) { Array<Pair<Int, Int>?>(cols) { null } }

    queue.add(start)
    visited[start.first][start.second] = true

    val directions = listOf(
        -1 to 0, 1 to 0, 0 to -1, 0 to 1
    )

    while (queue.isNotEmpty()) {
        val (r, c) = queue.poll()

        if ((r to c) == end) break

        for ((dr, dc) in directions) {
            val nr = r + dr
            val nc = c + dc

            if (nr in 0 until rows && nc in 0 until cols &&
                !visited[nr][nc] && mazeArr[nr][nc] != CellType.WALL
            ) {
                visited[nr][nc] = true
                prev[nr][nc] = r to c
                queue.add(nr to nc)
            }
        }
    }

// 判断是否有解
    if (prev[end.first][end.second] == null) {
        println("无法到达终点")
        return null
    }

// 回溯路径并记录 path
    val path = mutableListOf<Pair<Int, Int>>()
    var cur = end

    while (cur != null && cur != start) {
        path.add(cur)
        val (r, c) = cur
        if (mazeArr[r][c] == CellType.EMPTY) {
            mazeArr[r][c] = CellType.VISITED
        }
        cur = prev[r][c]
    }

    path.add(start)
    path.reverse() // 从起点到终点
    maze.visited = path
    return path
}