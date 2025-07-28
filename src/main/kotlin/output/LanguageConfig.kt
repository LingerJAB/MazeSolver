package io.github.lingerjab.output

data class LanguageConfig(
    val language: OutputLang = OutputLang.C,
    val start: String = "S",
    val end: String = "E",
    val wall: String = "1",
    val empty: String = "0",
    val visited: String = empty,
)
