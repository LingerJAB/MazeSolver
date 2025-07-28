package io.github.lingerjab.maze

data class Maze(
    val cellArray: Array<Array<CellType>>,
    val width: Int = cellArray.size,
    val height: Int = cellArray[0].size,
    var start: Pair<Int, Int>? = null,
    var end: Pair<Int, Int>? = null,
    var visited: List<Pair<Int, Int>> = listOf()
) {

    operator fun set(position: Pair<Int, Int>, type: CellType) {
        when (type) {
            CellType.START -> updateStart(position)
            CellType.END -> updateEnd(position)
            CellType.VISITED -> {
                if (get(position) == CellType.EMPTY) {
                    cellArray[position.first][position.second] = CellType.VISITED
                }
            }
            else -> {
                // 设置为空或其他非特殊值
                clearSpecialCell(position)
                cellArray[position.first][position.second] = type
            }
        }
    }

    private fun updateStart(position: Pair<Int, Int>) {
        if (get(position) == CellType.END) {
            clearEnd()
        } else {
            clearStart()
        }
        start = position
        cellArray[position.first][position.second] = CellType.START
    }

    private fun updateEnd(position: Pair<Int, Int>) {
        if (get(position) == CellType.START) {
            clearStart()
        } else {
            clearEnd()
        }
        end = position
        cellArray[position.first][position.second] = CellType.END
    }

    private fun clearSpecialCell(position: Pair<Int, Int>) {
        when (get(position)) {
            CellType.START -> clearStart()
            CellType.END -> clearEnd()
            else -> {}
        }
    }

    fun clearStart() {
        start?.let { (x, y) ->
            if (cellArray[x][y] == CellType.START) {
                cellArray[x][y] = CellType.EMPTY
            }
        }
        start = null
    }

    fun clearEnd() {
        end?.let { (x, y) ->
            if (cellArray[x][y] == CellType.END) {
                cellArray[x][y] = CellType.EMPTY
            }
        }
        end = null
    }

    fun clearVisited() {
        if (visited.isNotEmpty()) {
            visited.forEach{
                set(it, CellType.EMPTY)
            }
            visited = listOf()
        }
    }


    operator fun get(position: Pair<Int, Int>): CellType {
        return cellArray[position.first][position.second]
    }
}
