package app

/**
 * Мини-парсер JSON с позициями узлов и детектом дублей ключей + pretty-писатель.
 * Чистые функции без исключений наружу; ошибки возвращаются значением (ParseResult.errors).
 * Вызывается на каждое нажатие клавиши в редакторе — один проход O(n), как у JsonHighlight.
 */

sealed class JsonNode {
    abstract val offset: Int
    abstract val length: Int

    data class Obj(val entries: List<Pair<Str, JsonNode>>, override val offset: Int, override val length: Int) : JsonNode()
    data class Arr(val items: List<JsonNode>, override val offset: Int, override val length: Int) : JsonNode()
    data class Str(val value: String, override val offset: Int, override val length: Int) : JsonNode()
    data class Num(val raw: String, override val offset: Int, override val length: Int) : JsonNode()
    data class Bool(val value: Boolean, override val offset: Int, override val length: Int) : JsonNode()
    data class Null(override val offset: Int, override val length: Int) : JsonNode()
}

enum class Severity { ERROR, WARNING }

data class Diagnostic(val severity: Severity, val offset: Int, val length: Int, val message: String)

data class ParseResult(val root: JsonNode?, val errors: List<Diagnostic>, val warnings: List<Diagnostic> = emptyList()) {
    val ok: Boolean get() = errors.isEmpty() && root != null
}

private class ParseError(val at: Int, val len: Int, msg: String) : RuntimeException(msg)

private const val MAX_DEPTH = 100

private class Parser(private val text: String) {
    private var pos = 0
    private var depth = 0
    val warnings = mutableListOf<Diagnostic>()

    /** Толерантность к висячим запятым: не блокируем сохранение, но предупреждаем. */
    private fun warnTrailingComma() {
        warnings += Diagnostic(Severity.WARNING, pos - 1, 1, "Висячая запятая: Claude Code не примет файл; уберите её или сохраните через режим «Параметры»")
    }

    fun parse(): JsonNode {
        if (text.isNotEmpty() && text[0] == '﻿') pos = 1 // BOM пропускается, offsets остаются в координатах исходного текста
        skipWs()
        if (pos >= text.length) throw ParseError(pos, 1, "Пустой JSON")
        val v = value()
        skipWs()
        if (pos < text.length) throw ParseError(pos, 1, "Лишний символ после значения: '${text[pos]}'")
        return v
    }

    private fun skipWs() {
        while (pos < text.length && text[pos] in " \t\r\n") pos++
    }

    private fun peek(): Char {
        if (pos >= text.length) throw ParseError(pos, 1, "Неожиданный конец ввода")
        return text[pos]
    }

    private fun value(): JsonNode {
        if (++depth > MAX_DEPTH) throw ParseError(pos, 1, "Слишком глубокая вложенность (более $MAX_DEPTH уровней)")
        val v = when (val c = peek()) {
            '{' -> obj()
            '[' -> arr()
            '"' -> str()
            't' -> literal("true", JsonNode.Bool(true, pos, 4))
            'f' -> literal("false", JsonNode.Bool(false, pos, 5))
            'n' -> literal("null", JsonNode.Null(pos, 4))
            else -> if (c == '-' || c.isDigit()) number()
            else throw ParseError(pos, 1, "Неожиданный символ '$c'")
        }
        depth--
        return v
    }

    private fun literal(word: String, node: JsonNode): JsonNode {
        if (!text.startsWith(word, pos)) throw ParseError(pos, 1, "Ожидалось '$word'")
        pos += word.length
        return node
    }

    private fun obj(): JsonNode.Obj {
        val start = pos
        pos++
        val entries = mutableListOf<Pair<JsonNode.Str, JsonNode>>()
        skipWs()
        if (peek() == '}') { pos++; return JsonNode.Obj(entries, start, pos - start) }
        while (true) {
            skipWs()
            if (peek() == '}') { // сюда можно попасть только после висячей запятой
                warnTrailingComma()
                pos++
                return JsonNode.Obj(entries, start, pos - start)
            }
            if (peek() != '"') throw ParseError(pos, 1, "Ожидается ключ-строка в объекте")
            val key = str()
            if (entries.any { it.first.value == key.value }) {
                throw ParseError(key.offset, key.length, "Дубликат ключа \"${key.value}\"")
            }
            skipWs()
            if (peek() != ':') throw ParseError(pos, 1, "Ожидается ':' после ключа")
            pos++
            skipWs()
            entries.add(key to value())
            skipWs()
            when (peek()) {
                ',' -> pos++
                '}' -> { pos++; return JsonNode.Obj(entries, start, pos - start) }
                else -> throw ParseError(pos, 1, "Ожидается ',' или '}'")
            }
        }
    }

    private fun arr(): JsonNode.Arr {
        val start = pos
        pos++
        val items = mutableListOf<JsonNode>()
        skipWs()
        if (peek() == ']') { pos++; return JsonNode.Arr(items, start, pos - start) }
        while (true) {
            skipWs()
            if (peek() == ']') { // сюда можно попасть только после висячей запятой
                warnTrailingComma()
                pos++
                return JsonNode.Arr(items, start, pos - start)
            }
            items.add(value())
            skipWs()
            when (peek()) {
                ',' -> pos++
                ']' -> { pos++; return JsonNode.Arr(items, start, pos - start) }
                else -> throw ParseError(pos, 1, "Ожидается ',' или ']'")
            }
        }
    }

    private fun str(): JsonNode.Str {
        val start = pos
        pos++
        val sb = StringBuilder()
        while (true) {
            val c = peek()
            when {
                c == '"' -> { pos++; return JsonNode.Str(sb.toString(), start, pos - start) }
                c == '\\' -> {
                    pos++
                    when (val e = peek()) {
                        '"' -> { sb.append('"'); pos++ }
                        '\\' -> { sb.append('\\'); pos++ }
                        '/' -> { sb.append('/'); pos++ }
                        'b' -> { sb.append('\b'); pos++ }
                        'f' -> { sb.append(''); pos++ }
                        'n' -> { sb.append('\n'); pos++ }
                        'r' -> { sb.append('\r'); pos++ }
                        't' -> { sb.append('\t'); pos++ }
                        'u' -> {
                            pos++
                            if (pos + 4 > text.length || !text.substring(pos, pos + 4).all { it.isHexDigit() }) {
                                throw ParseError(pos, 1, "Некорректная \\u-последовательность")
                            }
                            sb.append(text.substring(pos, pos + 4).toInt(16).toChar())
                            pos += 4
                        }
                        else -> throw ParseError(pos, 1, "Недопустимый escape '\\$e'")
                    }
                }
                c < ' ' -> throw ParseError(pos, 1, "Управляющий символ в строке")
                else -> { sb.append(c); pos++ }
            }
        }
    }

    private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private fun number(): JsonNode.Num {
        val start = pos
        if (peek() == '-') pos++
        if (pos >= text.length || !text[pos].isDigit()) throw ParseError(start, 1, "Некорректное число")
        if (text[pos] == '0') pos++ else while (pos < text.length && text[pos].isDigit()) pos++
        if (pos < text.length && text[pos] == '.') {
            pos++
            if (pos >= text.length || !text[pos].isDigit()) throw ParseError(start, 1, "Некорректное число: ожидаются цифры после '.'")
            while (pos < text.length && text[pos].isDigit()) pos++
        }
        if (pos < text.length && (text[pos] == 'e' || text[pos] == 'E')) {
            pos++
            if (pos < text.length && (text[pos] == '+' || text[pos] == '-')) pos++
            if (pos >= text.length || !text[pos].isDigit()) throw ParseError(start, 1, "Некорректное число: ожидаются цифры экспоненты")
            while (pos < text.length && text[pos].isDigit()) pos++
        }
        return JsonNode.Num(text.substring(start, pos), start, pos - start)
    }
}

fun parseJson(text: String): ParseResult = try {
    val parser = Parser(text)
    ParseResult(parser.parse(), emptyList(), parser.warnings)
} catch (e: ParseError) {
    ParseResult(null, listOf(Diagnostic(Severity.ERROR, e.at, e.len, e.message ?: "Ошибка разбора")))
}

/** Канонический вывод: 2 пробела отступа, «ключ»: значение, порядок ключей как в дереве. */
fun writeJson(root: JsonNode): String = StringBuilder().apply { writeNode(root, 0) }.toString()

private fun StringBuilder.writeNode(n: JsonNode, ind: Int) {
    val pad = "  ".repeat(ind)
    val padInner = "  ".repeat(ind + 1)
    when (n) {
        is JsonNode.Obj ->
            if (n.entries.isEmpty()) append("{}")
            else {
                append("{\n")
                n.entries.forEachIndexed { i, (k, v) ->
                    append(padInner)
                    writeString(k.value)
                    append(": ")
                    writeNode(v, ind + 1)
                    if (i < n.entries.lastIndex) append(",")
                    append("\n")
                }
                append(pad).append("}")
            }
        is JsonNode.Arr ->
            if (n.items.isEmpty()) append("[]")
            else {
                append("[\n")
                n.items.forEachIndexed { i, v ->
                    append(padInner)
                    writeNode(v, ind + 1)
                    if (i < n.items.lastIndex) append(",")
                    append("\n")
                }
                append(pad).append("]")
            }
        is JsonNode.Str -> writeString(n.value)
        is JsonNode.Num -> append(n.raw)
        is JsonNode.Bool -> append(n.value)
        is JsonNode.Null -> append("null")
    }
}

private fun StringBuilder.writeString(s: String) {
    append('"')
    for (c in s) when (c) {
        '"' -> append("\\\"")
        '\\' -> append("\\\\")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        '\b' -> append("\\b")
        '' -> append("\\f")
        else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
    }
    append('"')
}
