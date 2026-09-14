package africa.iag.erp.core

/** Tiny JSON codec for maps/lists used by the ERP store. */
object MiniJson {
    fun stringify(value: Any?): String = buildString { write(value) }

    fun parse(text: String): Any? = Parser(text).parseValue()

    @Suppress("UNCHECKED_CAST")
    fun parseObject(text: String): Map<String, Any?> =
        (parse(text) as? Map<String, Any?>) ?: emptyMap()

    private fun StringBuilder.write(value: Any?) {
        when (value) {
            null -> append("null")
            is Boolean -> append(value)
            is Number -> append(value)
            is String -> writeString(value)
            is Map<*, *> -> {
                append('{')
                var first = true
                for ((k, v) in value) {
                    if (!first) append(',')
                    first = false
                    writeString(k.toString())
                    append(':')
                    write(v)
                }
                append('}')
            }
            is Iterable<*> -> {
                append('[')
                var first = true
                for (item in value) {
                    if (!first) append(',')
                    first = false
                    write(item)
                }
                append(']')
            }
            else -> writeString(value.toString())
        }
    }

    private fun StringBuilder.writeString(value: String) {
        append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    private class Parser(private val text: String) {
        private var i = 0

        fun parseValue(): Any? {
            skip()
            if (i >= text.length) return null
            return when (text[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBool()
                'n' -> parseNull()
                else -> parseNumber()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val out = linkedMapOf<String, Any?>()
            skip()
            if (peek() == '}') {
                i++
                return out
            }
            while (true) {
                skip()
                val key = parseString()
                skip()
                expect(':')
                out[key] = parseValue()
                skip()
                when (peek()) {
                    ',' -> i++
                    '}' -> {
                        i++
                        return out
                    }
                    else -> error("Expected , or } at $i")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val out = mutableListOf<Any?>()
            skip()
            if (peek() == ']') {
                i++
                return out
            }
            while (true) {
                out.add(parseValue())
                skip()
                when (peek()) {
                    ',' -> i++
                    ']' -> {
                        i++
                        return out
                    }
                    else -> error("Expected , or ] at $i")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val buf = StringBuilder()
            while (i < text.length) {
                val ch = text[i++]
                when (ch) {
                    '"' -> return buf.toString()
                    '\\' -> {
                        val n = text[i++]
                        buf.append(
                            when (n) {
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                else -> n
                            },
                        )
                    }
                    else -> buf.append(ch)
                }
            }
            error("Unterminated string")
        }

        private fun parseBool(): Boolean {
            return if (text.startsWith("true", i)) {
                i += 4
                true
            } else if (text.startsWith("false", i)) {
                i += 5
                false
            } else error("Expected boolean at $i")
        }

        private fun parseNull(): Any? {
            if (!text.startsWith("null", i)) error("Expected null at $i")
            i += 4
            return null
        }

        private fun parseNumber(): Number {
            val start = i
            if (peek() == '-') i++
            while (i < text.length && (text[i].isDigit() || text[i] == '.')) i++
            val raw = text.substring(start, i)
            return if (raw.contains('.')) raw.toDouble() else raw.toLong()
        }

        private fun skip() {
            while (i < text.length && text[i].isWhitespace()) i++
        }

        private fun peek(): Char = if (i < text.length) text[i] else '\u0000'

        private fun expect(ch: Char) {
            skip()
            if (peek() != ch) error("Expected $ch at $i")
            i++
        }
    }
}
