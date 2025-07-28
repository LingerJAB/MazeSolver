@file:Suppress("FunctionName")

package io.github.lingerjab

//import androidx.compose.material.Switch
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Grid4x4
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Output
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.github.lingerjab.maze.CellType
import io.github.lingerjab.maze.Maze
import io.github.lingerjab.maze.parseMaze
import io.github.lingerjab.maze.parseMazeWithBlank
import io.github.lingerjab.maze.solveMaze
import io.github.lingerjab.output.LanguageConfig
import io.github.lingerjab.output.OutputLang
import io.github.lingerjab.output.PathConfig
import io.github.lingerjab.output.StepsConfig
import io.github.lingerjab.output.TextConfig
import io.github.lingerjab.output.parseCode
import io.github.lingerjab.output.parsePath
import io.github.lingerjab.output.parseSteps
import io.github.lingerjab.output.parseText
import io.github.lingerjab.ui.theme.AppTheme
import java.awt.Dimension

val jetBrainsMono = FontFamily(
    Font(resource = "fonts/JetBrainsMonoNL-Regular.ttf", weight = FontWeight.Normal),
    Font(resource = "fonts/JetBrainsMonoNL-Bold.ttf", weight = FontWeight.Bold)
)

fun main() = application {
    val windowState = remember {
        WindowState(
            width = 800.dp,
            height = 600.dp
        )
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Maze Solver",
        icon = painterResource("app_icon.png"),
        state = windowState,
        resizable = true,
    ) {
        LaunchedEffect(Unit) {
            window.minimumSize = Dimension(600, 500)
        }

        AppTheme {
            App()
        }
    }
}

@Composable
fun App() {
    val defaultMaze = "011010\n111111\n110101\n110101\n110101\nS0000E"
    var maze by remember { mutableStateOf<Maze>(parseMaze(defaultMaze)) }
    var mazePath by remember { mutableStateOf<List<Pair<Int, Int>>?>(null) }
    var isEditLocked by remember { mutableStateOf(true) }
    var hasStart by remember { mutableStateOf(maze.start != null) }
    var hasEnd by remember { mutableStateOf(maze.end != null) }
    var trigger by remember { mutableStateOf(0) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    // 保留空格和换行
    var useSpace: Boolean by remember { mutableStateOf(false) }

    // 来自 CharMappingTextField 的映射状态
    var startChar: Char? by remember { mutableStateOf(null) }
    var endChar: Char? by remember { mutableStateOf(null) }
    var wallChar: Char? by remember { mutableStateOf(null) }
    var emptyChar: Char? by remember { mutableStateOf(null) }
    var ignoredText by remember { mutableStateOf("") }


    var mazeInput by remember { mutableStateOf("") }
    fun checkRemainingChars(
        text: String,
        start: Char?,
        end: Char?,
        wall: Char?,
        empty: Char?,
        ignored: String
    ): List<Char> {
        val toIgnore = buildSet {
            start?.let { add(it) }
            end?.let { add(it) }
            wall?.let { add(it) }
            empty?.let { add(it) }
            addAll(ignored.toSet())
        }

        return text.asSequence()
            .filter { it !in toIgnore && !it.isWhitespace() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    val doAnalyze = {
        maze = if (useSpace) {
            // parseMazeWithBlank(str, startChar, endChar, wallChar)
            parseMazeWithBlank(
                str = mazeInput,
                startChar = startChar ?: 'S',
                endChar = endChar ?: 'E',
                wallChar = wallChar ?: '1',
                ignoreChars = ignoredText
            )
        } else {
            // parseMaze(str, startChar, endChar, wallChar, emptyChar)
            parseMaze(
                str = mazeInput,
                startChar = startChar ?: 'S',
                endChar = endChar ?: 'E',
                wallChar = wallChar ?: '1',
                emptyChar = emptyChar ?: '0',
                ignoreChars = ignoredText
            )
        }
        hasStart = maze.start != null
        hasEnd = maze.end != null
        println("解析出的迷宫: $maze")
    }
    val clipboardManager = LocalClipboardManager.current
    var remainChars by remember {
        mutableStateOf(
            checkRemainingChars(
                mazeInput,
                startChar,
                endChar,
                wallChar,
                emptyChar,
                ignoredText
            )
        )
    }
    val isAnalyzeEnabled by remember(mazeInput, wallChar, emptyChar, useSpace) {
        derivedStateOf {
            mazeInput.isNotBlank() && wallChar != null && (useSpace || emptyChar != null) && remainChars.isEmpty()
        }
    }

    fun handleCellClick(row: Int, col: Int, isRightClick: Boolean) {
        // 重新计算 hasStart 和 hasEnd 状态
        val maze = maze
        maze.clearVisited()
        val cell = maze[row to col]

        if (isRightClick) {
            if (cell == CellType.WALL || cell == CellType.START || cell == CellType.END) {
                return // 只能放在 EMPTY 上
            }

            hasStart = maze.start != null
            hasEnd = maze.end != null

            when {
                !hasStart -> {
                    maze.clearEnd()
                    maze[row to col] = CellType.START
                    hasStart = true
                    hasEnd = false
                }

                !hasEnd -> {
                    maze[row to col] = CellType.END
                    hasEnd = true
                }

                else -> {
                    maze.clearStart()
                    maze.clearEnd()
                    // 已有 Start 和 End，重新开始设置 Start
                    maze[row to col] = CellType.START
                    hasStart = true
                    hasEnd = false
                }
            }
        } else {
            val newType = when (cell) {
                CellType.EMPTY -> CellType.WALL
                CellType.WALL -> CellType.EMPTY
                else -> return
            }
            maze[row to col] = newType
        }

        // 触发重组（强制更新状态）
        trigger++
    }


    Surface {
        HelpDialog(showDialog = showHelpDialog, onDismiss = { showHelpDialog = false })
        AboutDialog(showDialog = showAboutDialog, onDismiss = { showAboutDialog = false })

        val verticalScrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.padding(10.dp).verticalScroll(verticalScrollState)
            ) {
                FunctionCard(icon = Icons.Outlined.Info, title = "开始") {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Maze Solver", fontSize = 30.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("一个简单的字符迷宫求解器\n点击右侧帮助按钮查看使用说明 😋")
                        }
                        Column(verticalArrangement = Arrangement.SpaceEvenly) {
                            Button(
                                onClick = { showHelpDialog = true },
                                content = { Text("帮助") }
                            )
                            Button(
                                onClick = { showAboutDialog = true },
                                content = { Text("关于") }
                            )
                        }
                    }
                }

                FunctionCard(icon = Icons.Outlined.Edit, title = "输入") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        MazeTextField(
                            text = mazeInput,
                            onTextChange = {
                                mazeInput = it
                                remainChars =
                                    checkRemainingChars(it, startChar, endChar, wallChar, emptyChar, ignoredText)
                            },
                            useSpace = useSpace,
                            isAnalyzeButtonEnable = isAnalyzeEnabled,
                            onPaste = { mazeInput = clipboardManager.getText()?.text ?: "" },
                            onAnalyze = doAnalyze
                        )
                        Spacer(modifier = Modifier.height(16.dp))


                        CharMappingTextFields(
                            initialStart = startChar,
                            initialEnd = endChar,
                            initialWall = wallChar,
                            initialEmpty = emptyChar,
                            onMappingChange = { s, e, w, emp, i, useSp ->
                                startChar = s
                                endChar = e
                                wallChar = w
                                emptyChar = emp
                                ignoredText = i
                                useSpace = useSp
                                remainChars =
                                    checkRemainingChars(mazeInput, startChar, endChar, wallChar, emptyChar, ignoredText)
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = buildAnnotatedString {
                                if (remainChars.isEmpty()) {
                                    withStyle(style = SpanStyle(color = Color.Green)) {
                                        if (mazeInput.isNotEmpty()) append("无多余的字符")
                                        else append("输入迷宫后开始检查")
                                    }
                                } else {
                                    append("请检查以下字符: ")
                                    for (char in remainChars) {
                                        withStyle(style = SpanStyle(color = Color.Red)) {
                                            append("$char ")
                                        }
                                    }
                                }
                            }
                        )
                    }
                }


                var stepsConfig by remember { mutableStateOf(StepsConfig()) }
                var pathConfig by remember { mutableStateOf(PathConfig()) }
                var textConfig by remember { mutableStateOf(TextConfig()) }
                var codeConfig by remember { mutableStateOf(LanguageConfig()) }

                var stepsConfigText by remember { mutableStateOf("L;R;U;D;, ;") }
                var stepsConfigIsReversed by remember { mutableStateOf(false) }

                var pathConfigIsXY by remember { mutableStateOf(false) }
                var pathConfigIsReversed by remember { mutableStateOf(false) }
                var pathConfigIsStartWithZero by remember { mutableStateOf(true) }

                var textConfigText by remember { mutableStateOf("S;E;1;0;0; ;") }

                var codeConfigSelection by remember { mutableStateOf(OutputLang.C) }
                var codeConfigText by remember { mutableStateOf("S;E;1;0;0;") }

                val doOutputSteps: () -> String = {
                    val params = stepsConfigText.split(";")
                    stepsConfig = stepsConfig.copy(
                        left = params.getOrElse(0) { "" },
                        right = params.getOrElse(1) { "" },
                        up = params.getOrElse(2) { "" },
                        down = params.getOrElse(3) { "" },
                        spliterator = params.getOrElse(4) { "" },
                        isReversed = stepsConfigIsReversed
                    )
                    mazePath?.run { parseSteps(this, stepsConfig) } ?: "该迷宫无可解步骤"
                }
                val doOutputPath: () -> String = {
                    pathConfig = pathConfig.copy(
                        isXY = pathConfigIsXY,
                        startWithZero = pathConfigIsStartWithZero,
                        isReversed = pathConfigIsReversed
                    )

                    mazePath?.run { parsePath(this, pathConfig) } ?: "该迷宫无可解路径"
                }
                val doOutputCode: () -> String = {
                    val params = codeConfigText.split(";")
                    codeConfig = codeConfig.copy(
                        language = codeConfigSelection,
                        start = params.getOrElse(0) { "" },
                        end = params.getOrElse(1) { "" },
                        wall = params.getOrElse(2) { "" },
                        empty = params.getOrElse(3) { "" },
                        visited = params.getOrElse(4) { "" }
                    )
                    parseCode(maze, codeConfig)
                }
                val doOutputText: () -> String = {
                    val params = textConfigText.split(";")
                    textConfig = textConfig.copy(
                        start = params.getOrElse(0) { "" },
                        end = params.getOrElse(1) { "" },
                        wall = params.getOrElse(2) { "" },
                        empty = params.getOrElse(3) { "" },
                        visited = params.getOrElse(4) { "" },
                        spliterator = params.getOrElse(5) { "" }
                    )
                    parseText(maze, textConfig)
                }

                var stepsResult by remember { mutableStateOf("") }
                var pathResult by remember { mutableStateOf("") }
                var textResult by remember { mutableStateOf("") }
                var codeResult by remember { mutableStateOf("") }

                FunctionCard(icon = Icons.Rounded.Grid4x4, title = "可视化") {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        val verticalScrollMazeState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .horizontalScroll(verticalScrollMazeState)
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            MazeGrid(
                                maze = maze,
                                isEditLocked = isEditLocked,
                                trigger = trigger,
                                onCellClick = ::handleCellClick
                            )
                        }
                        HorizontalScrollbar(
                            adapter = rememberScrollbarAdapter(verticalScrollMazeState),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(horizontalArrangement = Arrangement.Start) {
                                Text(
                                    "编辑锁定",
                                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp)
                                )
                                Switch(
                                    checked = isEditLocked,
                                    onCheckedChange = { isEditLocked = it },
                                    thumbContent = {
                                        if (isEditLocked) Icon(
                                            Icons.Rounded.Lock,
                                            "Lock",
                                            Modifier.size(16.dp)
                                        ) else Icon(Icons.Rounded.LockOpen, "Unlock", Modifier.size(16.dp))
                                    }
                                )
                            }
                            Button(
                                onClick = {
                                    mazePath = solveMaze(maze)

                                    stepsResult = doOutputSteps()
                                    pathResult = doOutputPath()
                                    textResult = doOutputText()
                                    codeResult = doOutputCode()
                                    trigger++
                                },
                                enabled = hasStart && hasEnd
                            ) { Text("求解") }
                        }
                    }
                }

                FunctionCard(icon = Icons.Rounded.Output, title = "输出") {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        OutputRow(
                            icon = Icons.Default.Gamepad,
                            text = stepsResult,
                            title = "步骤",
                            onTextChange = { stepsResult = it }
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                val suggestions = arrayOf("L;R;U;D;, ;", "a;d;w;s;;", "左;右;上;下;;", "A;D;W;S;;")
                                var expanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("参数") },
                                        value = stepsConfigText,
                                        onValueChange = { stepsConfigText = it },
                                        placeholder = { Text("使用分号分隔,可留空,空格敏感") },
                                        supportingText = { Text("格式: <Left>;<Right>;<Up>;<Down>;<Spliterator>") },
                                        trailingIcon = {
                                            IconButton(onClick = { expanded = !expanded }) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "展开建议"
                                                )
                                            }
                                        },
                                    )

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                    ) {
                                        suggestions.forEach { suggestion ->
                                            DropdownMenuItem(
                                                onClick = {
                                                    stepsConfigText = suggestion
                                                    expanded = false
                                                }
                                            ) { Text(suggestion) }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("反向")
                                    Switch(checked = stepsConfigIsReversed,
                                        onCheckedChange = {
                                            stepsConfigIsReversed = it
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(15.dp))
                                Button(onClick = {
                                    stepsResult = doOutputSteps()
                                    it()
                                }) { Text("确定") }
                            }
                        }

                        Divider(modifier = Modifier.fillMaxWidth())
                        OutputRow(
                            icon = Icons.Default.Polyline,
                            text = pathResult,
                            title = "路径",
                            onTextChange = { pathResult = it }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text("XY坐标")
                                        Switch(checked = pathConfigIsXY,
                                            onCheckedChange = {
                                                pathConfigIsXY = it
                                                pathConfig = pathConfig.copy(isXY = pathConfigIsXY)
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(15.dp))
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text("原点为0")
                                        Switch(checked = pathConfigIsStartWithZero,
                                            onCheckedChange = {
                                                pathConfigIsStartWithZero = it
                                                pathConfig = pathConfig.copy(startWithZero = pathConfigIsStartWithZero)
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(15.dp))
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text("反向")
                                        Switch(checked = pathConfigIsReversed,
                                            onCheckedChange = {
                                                pathConfigIsReversed = it
                                                pathConfig = pathConfig.copy(isReversed = pathConfigIsReversed)
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(15.dp))
                                Button(onClick = {
                                    pathResult = doOutputPath()
                                    it()
                                }) { Text("确定") }
                            }

                        }

                        Divider(modifier = Modifier.fillMaxWidth())
                        OutputRow(
                            icon = Icons.Default.Article,
                            text = textResult,
                            title = "文本",
                            onTextChange = { textResult = it }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val suggestions = arrayOf("S;E;1;0;0; ;", "S;E;1;0;V; ;", "S;E;#;*;*;;", "S;E;#; ; ;;")
                                var expanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("参数") },
                                        value = textConfigText,
                                        onValueChange = { textConfigText = it },
                                        placeholder = { Text("使用分号分隔,可留空,空格敏感") },
                                        supportingText = { Text("格式: <Start>;<End>;<Wall>;<Empty>;<Visited>;<Spliterator>") },
                                        trailingIcon = {
                                            IconButton(onClick = { expanded = !expanded }) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "展开建议"
                                                )
                                            }
                                        },
                                    )

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                    ) {
                                        suggestions.forEach { suggestion ->
                                            DropdownMenuItem(
                                                onClick = {
                                                    textConfigText = suggestion
                                                    expanded = false
                                                }
                                            ) { Text(suggestion) }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(15.dp))
                                Button(onClick = {
                                    textResult = doOutputText()
                                    it()
                                }) { Text("确定") }
                            }
                        }

                        Divider(modifier = Modifier.fillMaxWidth())
                        OutputRow(
                            icon = Icons.Default.Code,
                            text = codeResult,
                            title = "代码",
                            onTextChange = { codeResult = it }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val suggestions = arrayOf("S;E;1;0;0;", "S;E;1;0;V;", "S;E;#;*;*;", "S;E;#; ; ;")
                                var expanded by remember { mutableStateOf(false) }
                                // 输入框
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("参数") },
                                        value = codeConfigText,
                                        onValueChange = { codeConfigText = it },
                                        placeholder = { Text("使用分号分隔,可留空,空格敏感") },
                                        supportingText = { Text("格式: <Start>;<End>;<Wall>;<Empty>;<Visited>") },
                                        trailingIcon = {
                                            IconButton(onClick = { expanded = !expanded }) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = "展开建议"
                                                )
                                            }
                                        },
                                    )

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                    ) {
                                        suggestions.forEach { suggestion ->
                                            DropdownMenuItem(
                                                onClick = {
                                                    codeConfigText = suggestion
                                                    expanded = false
                                                }
                                            ) { Text(suggestion) }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                // 语言选择按钮
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text("语言:")
                                    LanguageSelectMenu(
                                        options = OutputLang.entries,
                                        onOptionSelected = { codeConfigSelection = it },
                                        selectedOption = codeConfigSelection
                                    )
                                }
                                Spacer(modifier = Modifier.width(15.dp))
                                Button(onClick = {
                                    codeResult = doOutputCode()
                                    it()
                                }) { Text("确定") }
                            }
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(verticalScrollState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(10.dp)
            )
        }
    }
}

@Composable
fun HelpDialog(showDialog: Boolean, onDismiss: () -> Unit) {
    if (showDialog) {
        DialogWindow(
            onCloseRequest = onDismiss,
            title = "使用说明"
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .sizeIn(minWidth = 300.dp, minHeight = 150.dp)
            ) {
                Text("这是一个迷宫求解器。\n你可以输入迷宫图后点击求解按钮。")
            }
        }
    }
}

//todo
@Composable
fun AboutDialog(showDialog: Boolean, onDismiss: () -> Unit) {
    if (showDialog) {
        DialogWindow(
            onCloseRequest = onDismiss,
            title = "关于"
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .sizeIn(minWidth = 300.dp, minHeight = 150.dp)
            ) {
                Text("关于软件")
            }
        }
    }
}

/**
 * @param initialStart   初始起点字符（可 null）
 * @param initialEnd     初始终点字符（可 null）
 * @param initialWall    初始墙体字符（不可 null）
 * @param initialEmpty   初始空地字符（不可 null，只有当 useSpace=false 时生效）
 * @param onMappingChange 每次任意值变化时回调：start, end, wall, empty（空格模式下 empty 固定为 ' ' ）
 */
@Composable
fun CharMappingTextFields(
    modifier: Modifier = Modifier,
    initialStart: Char? = null,
    initialEnd: Char? = null,
    initialWall: Char? = null,
    initialEmpty: Char? = null,
    onMappingChange: (start: Char?, end: Char?, wall: Char?, empty: Char?, ignoredText: String, useSpace: Boolean) -> Unit
) {
    // —— 内部可编辑状态 ——
    var startText by remember { mutableStateOf(initialStart?.toString() ?: "") }
    var endText by remember { mutableStateOf(initialEnd?.toString() ?: "") }
    var wallText by remember { mutableStateOf(initialWall?.toString() ?: "") }
    var emptyText by remember { mutableStateOf(initialEmpty?.toString() ?: "") }
    var ignoredText by remember { mutableStateOf("") }
    var useSpace by remember { mutableStateOf(false) }

    // —— 提取单字符或 null ——
    val sChar = startText.singleOrNull()
    val eChar = endText.singleOrNull()
    val wChar = wallText.singleOrNull()
    val empChar = if (useSpace) ' ' else emptyText.singleOrNull()

    // —— 校验空与长度 ——
    val startErr = startText.length > 1 || (startText.isNotEmpty() && startText[0].isWhitespace())
    val endErr = endText.length > 1 || (endText.isNotEmpty() && endText[0].isWhitespace())
    val wallErr = wallText.length != 1 || wallText[0].isWhitespace()
    val empErr = !useSpace && (emptyText.length != 1 || emptyText[0].isWhitespace())

    // —— 查找重复的字符集合 ——
    val allChars = listOfNotNull(sChar, eChar, wChar, empChar)
    val duplicates = allChars
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys

    // —— 每个框是否重复 ——
    val startDup = sChar != null && sChar in duplicates
    val endDup = eChar != null && eChar in duplicates
    val wallDup = wChar != null && wChar in duplicates
    val empDup = empChar != null && empChar in duplicates

    // —— 状态提升回调 ——
    LaunchedEffect(sChar, eChar, wChar, empChar, ignoredText, useSpace) {
        onMappingChange(sChar, eChar, wChar, empChar, ignoredText, useSpace)
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        @Composable
        fun commonTextField(
            label: String,
            text: String,
            onlyOneChar: Boolean = true,
            onTextChange: (String) -> Unit,
            isError: Boolean,
            errorMsg: String?,
            enabled: Boolean = true
        ) {
            Column {
                Text(label)
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (!onlyOneChar || it.length <= 1) {
                            onTextChange(it)
                        }
                    },
                    singleLine = true,
                    isError = isError,
                    enabled = enabled,
                    modifier = Modifier.width(if (onlyOneChar) 60.dp else 100.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii
                    )
                )
                if (errorMsg != null) {
                    Text(errorMsg, color = Color.Red, style = MaterialTheme.typography.caption)
                }
            }
        }

        commonTextField(
            label = "Start",
            text = startText,
            onTextChange = { startText = it },
            isError = startErr || startDup,
            errorMsg = when {
                startErr -> "1 个可见字符"
                startDup -> "字符重复"
                else -> null
            }
        )

        commonTextField(
            label = "End",
            text = endText,
            onTextChange = { endText = it },
            isError = endErr || endDup,
            errorMsg = when {
                endErr -> "1 个可见字符"
                endDup -> "字符重复"
                else -> null
            }
        )

        commonTextField(
            label = "Wall *",
            text = wallText,
            onTextChange = { wallText = it },
            isError = wallErr || wallDup,
            errorMsg = when {
                wallErr -> "必填 1 个\n可见字符"
                wallDup -> "字符重复"
                else -> null
            }
        )

        Column {
            Text("Empty *")
            OutlinedTextField(
                value = if (useSpace) "空格" else emptyText,
                onValueChange = { if (!useSpace && it.length <= 1) emptyText = it },
                singleLine = true,
                isError = (!useSpace && empErr) || empDup,
                enabled = !useSpace,
                modifier = Modifier.width(60.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii
                )
            )
            Spacer(Modifier.width(4.dp))
            val empMsg = when {
                useSpace -> null
                empErr -> "必填 1 个\n可见字符"
                empDup -> "字符重复"
                else -> null
            }
            if (empMsg != null) {
                Text(empMsg, color = Color.Red, style = MaterialTheme.typography.caption)
            }
        }

        commonTextField(
            label = "Ignore",
            text = ignoredText,
            onTextChange = { ignoredText = it },
            onlyOneChar = false,
            isError = false,
            errorMsg = null,
            enabled = true
        )

        Spacer(Modifier.width(10.dp))
        Column {
            Text("路线用空格")
            Switch(checked = useSpace, onCheckedChange = { useSpace = it })
        }
    }
}


@Composable
fun OutputRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    text: String = "",
    onTextChange: (String) -> Unit,
    configContent: @Composable (onConfirm: () -> Unit) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showConfig by remember { mutableStateOf(false) }
    val textFieldFocusRequester = remember { FocusRequester() }
    var selection by remember { mutableStateOf(TextRange.Zero) }

    Row(modifier = modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(5.dp))
                Text(title)
            }
            Button(onClick = { showConfig = !showConfig }) { Text("设置") }
        }
        Spacer(modifier.width(10.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 输入框/配置
            if (!showConfig) {
                OutlinedTextField(
                    value = TextFieldValue(text = text, selection = selection),
                    onValueChange = {
                        onTextChange(it.text)
                        selection = it.selection
                    },
                    label = { Text(title) },
                    modifier = Modifier.heightIn(max = 110.dp)//.verticalScroll(scrollState)
                        .focusRequester(textFieldFocusRequester).weight(1f)
                )
                Spacer(Modifier.width(15.dp))
                Button(onClick = {
                    clipboardManager.setText(buildAnnotatedString { append(text) })
                    textFieldFocusRequester.requestFocus()
                    selection = TextRange(0, text.length)
                }) {
                    Text("复制")
                }
            } else {
                configContent {
                    showConfig = false
                }
            }
        }
    }
}

@Composable
fun IconLabel(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
    ) {
        Icon(icon, contentDescription = null)
        Text(
            modifier = modifier.padding(start = 5.dp),
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun FunctionCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.padding(16.dp)
    ) {
        IconLabel(icon = icon, text = title)
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier.padding(vertical = 5.dp).fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp),
        ) {
            content()
        }
    }
}

@Composable
fun LanguageSelectMenu(
    options: List<OutputLang>,
    selectedOption: OutputLang,
    onOptionSelected: (OutputLang) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedOption.name)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                ) {
                    Text(option.name)
                }

            }
        }
    }
}


@Composable
fun MazeGrid(
    maze: Maze,
    cellSize: Int = 15,
    modifier: Modifier = Modifier,
    isEditLocked: Boolean = true,
    trigger: Int,
    onCellClick: ((row: Int, col: Int, isRightClick: Boolean) -> Unit)
) {
    // Trigger parameter to force recomposition
    trigger

    val cellArray = maze.cellArray
    Column(modifier = modifier.fillMaxWidth()) {
        for (row in cellArray.indices) {
            Row {
                for (col in cellArray[row].indices) {
                    val cellType = cellArray[row][col]
                    val color = when (cellType) {
                        CellType.EMPTY -> Color.White
                        CellType.WALL -> Color.Black
                        CellType.START -> Color.Green
                        CellType.END -> Color.Red
                        CellType.VISITED -> Color.Yellow
                        else -> Color.LightGray
                    }

                    Box(
                        modifier = Modifier
                            .size(cellSize.dp)
                            .background(color)
                            .border(1.dp, Color.Gray)
                            .pointerInput(isEditLocked) {
                                awaitPointerEventScope {
                                    if (!isEditLocked) {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val input = event.changes.firstOrNull() ?: continue

                                            if (input.changedToDown()) {
                                                val isRightClick = event.buttons.isSecondaryPressed
                                                onCellClick(row, col, isRightClick)
                                            }
                                        }
                                    }
                                }
                            }

                    )
                }
            }
        }
    }
}

@Composable
fun MazeTextField(
    text: String,
    onTextChange: (String) -> Unit,
    useSpace: Boolean,
    isAnalyzeButtonEnable: Boolean,
    onPaste: () -> Unit,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            label = { Text("Maze") },
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState)
                .heightIn(min = 104.dp, max = 200.dp),
            supportingText = {
                if (useSpace) Text("文本会保留空行和空格为有效路线")
                else Text("文本会自动忽略空行空格")
            },
            placeholder = { Text("输入或粘贴迷宫") },
            textStyle = TextStyle(fontFamily = jetBrainsMono)
        )
        Column(modifier = Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onPaste) {
                Text("粘贴")
            }
            Button(
                onClick = onAnalyze,
                enabled = isAnalyzeButtonEnable
            ) {
                Text("分析")
            }
        }
    }
}

