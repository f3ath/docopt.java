package org.docopt

import org.docopt.Option.Companion.parse
import org.docopt.Py.Re
import org.docopt.Py.Re.findAll
import org.docopt.Py.isUpper
import java.io.InputStream
import java.util.Scanner
import java.util.regex.Pattern.compile
import kotlin.text.RegexOption.MULTILINE

internal object Parser {
    fun parseOptions(doc: String) = parseSection("options:", doc)
        .map { dropHeader(it) }
        .flatMap { parseOptionsFromSection(it) }
        .toMutableList()

    private fun dropHeader(section: String) =
        section.split("options:", limit = 2, ignoreCase = true).last()

    private fun parseOptionsFromSection(section: String) =
        section
            .lines()
            .let { combineOptions(it) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { parse(it) }

    private fun combineOptions(lines: List<String>) = sequence {
        var option = lines.first()
        for (line in lines.drop(1)) {
            if (line.trim().startsWith("-")) {
                yield(option)
                option = line
            } else {
                option += line
            }
        }
        yield(option)
    }

    fun parsePattern(
        source: String,
        options: MutableList<Option>
    ): Required {

        val wrapped =
            Regex(
                listOf(
                    "\\.\\.\\.",
                    "\\[",
                    "\\]",
                    "\\(",
                    "\\)",
                    "\\|"
                ).joinToString("|"),
                MULTILINE
            ).replace(source) { " ${it.value} " }

        val tokens = Regex("\\S*<[\\w ]+>|\\S+")
            .findAll(wrapped)
            .map { it.value }
            .let { Tokens(it.toList(), DocoptLanguageError::class.java) }

        val result = parseExpr(tokens, options)
        if (tokens.isNotEmpty()) {
            tokens.throwError("unexpected ending: ${tokens.joinToString(" ")}")
        }
        return Required(result)
    }

    fun parseArgv(
        tokens: Tokens,
        options: MutableList<Option>,
        optionsFirst: Boolean
    ): List<LeafPattern> {
        val parsed = mutableListOf<LeafPattern>()

        while (tokens.isNotEmpty()) {
            val token = tokens.first()
            if ("--" == token) {
                tokens
                    .map { Argument(value = it) }
                    .forEach { parsed.add(it) }
                return parsed
            }

            if (token.startsWith("--")) {
                parsed.addAll(parseLong(tokens, options))
            } else if (token.startsWith("-") && "-" != token) {
                parsed.addAll(parseShorts(tokens, options))
            } else if (optionsFirst) {
                tokens
                    .map { Argument(value = it) }
                    .forEach { parsed.add(it) }
                return parsed
            } else {
                parsed.add(Argument(value = tokens.shift()))
            }
        }
        return parsed
    }

    fun read(stream: InputStream): String {
        Scanner(stream).use { scanner ->
            scanner.useDelimiter("\\A")
            return if (scanner.hasNext()) scanner.next() else ""
        }
    }

    fun parseSection(
        name: String,
        source: String
    ): List<String> {
        val u = findAll(
            "^([^\\n]*$name[^\\n]*\\n?(?:[ \\t].*?(?:\\n|$))*)",
            source,
            Re.IGNORECASE or Re.MULTILINE
        )
        return u.map { it.trim() }
    }

    fun formalUsage(section: String): String = section
        .split(":", limit = 2)
        .last()
        .lines()
        .map {
            it
                .trim()
                .split(Regex("\\s+"))
                .drop(1) // program name
                .joinToString(" ")
        }
        .joinToString(" | ") { "( $it )" }

    private fun parseExpr(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Pattern?> {
        var seq = parseSeq(tokens, options)
        if ("|" != tokens.firstOrNull()) {
            return seq
        }
        val result: MutableList<Pattern?> =
            if (seq.size > 1) mutableListOf(Required(seq))
            else seq
        while ("|" == tokens.firstOrNull()) {
            tokens.shift()
            seq = parseSeq(tokens, options)
            result.addAll(if (seq.size > 1) listOf(Required(seq)) else seq)
        }
        return if (result.size > 1) listOf(Either(result)) else result
    }

    private fun parseSeq(
        tokens: Tokens,
        options: MutableList<Option>
    ): MutableList<Pattern?> {
        val result = mutableListOf<Pattern?>()
        while (!arrayOf(null, "]", ")", "|").contains(tokens.firstOrNull())) {
            var atom = parseAtom(tokens, options)
            if ("..." == tokens.firstOrNull()) {
                atom = mutableListOf(OneOrMore(atom))
                tokens.shift()
            }
            result.addAll(atom)
        }
        return result
    }

    private fun parseAtom(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Pattern?> {
        val token = tokens.first()
        val result: List<Pattern?>
        if ("(" == token || "[" == token) {
            tokens.shift()
            val matching: String
            val u = parseExpr(tokens, options)
            if ("(" == token) {
                matching = ")"
                result = listOf(Required(u))
            } else {
                matching = "]"
                result = listOf(Optional(u))
            }
            if (matching != tokens.shift()) {
                tokens.throwError("unmatched '$token'")
            }
            return result
        }
        if ("options" == token) {
            tokens.shift()
            return listOf(OptionsShortcut())
        }
        if (token.startsWith("--") && "--" != token) {
            return parseLong(tokens, options)
        }
        if (token.startsWith("-") && "-" != token && "--" != token) {
            return parseShorts(tokens, options)
        }
        return if (token.startsWith("<") && token.endsWith(">") || isUpper(token))
            listOf(Argument(tokens.shift()))
        else
            listOf(Command(tokens.shift()))
    }

    private fun parseLong(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Option> {
        val parts = tokens.shift()!!.split("=", limit = 2)
        val long = parts.first()
        var value = if (parts.size > 1) parts.last() else null

        assert(long.startsWith("--"))
        val similar = options
            .map { it }
            .filter { it.long == long }.toMutableList()

        if (tokens.error == DocoptExitException::class.java && similar.isEmpty()) {
            options
                .filter { it.long?.startsWith(long) ?: false }
                .forEach { similar.add(it) }
        }
        if (similar.size > 1) {
            tokens.throwError(
                "$long is not a unique prefix: ${similar.map { it.long }.joinToString(", ")}?"
            )
        }
        var option: Option
        if (similar.size < 1) {
            val argCount = if (parts.size > 1) 1 else 0
            option = Option(null, long, argCount)
            options.add(option)
            if (tokens.error == DocoptExitException::class.java) {
                option = Option(null, long, argCount, if (argCount != 0) value else true)
            }
        } else {
            option = similar.first().clone()
            if (option.argCount == 0) {
                if (value != null) {
                    tokens.throwError("${option.long} must not have an argument")
                }
            } else {
                if (value == null) {
                    val u = tokens.firstOrNull()
                    if (u == null || "--" == u) {
                        tokens.throwError("${option.long} requires argument")
                    }
                    value = tokens.shift()
                }
            }
            if (tokens.error == DocoptExitException::class.java) {
                option.value = value ?: true
            }
        }
        return mutableListOf(option)
    }

    private fun parseShorts(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Option> {
        val token = tokens.shift()!!
        assert(token.startsWith("-") && !token.startsWith("--"))
        var left = token.replaceFirst(Regex("^-+"), "")
        val parsed = mutableListOf<Option>()
        while ("" != left) {
            val short = "-" + left[0]
            left = left.substring(1)
            val similar: MutableList<Option> = mutableListOf()
            options
                .filter { it.short == short }
                .forEach { similar.add(it) }
            if (similar.size > 1) tokens.throwError(
                "$short is specified ambiguously ${similar.size} times"
            )
            var o: Option
            if (similar.isEmpty()) {
                o = Option(short, null, 0)
                options.add(o)
                if (tokens.error == DocoptExitException::class.java) {
                    o = Option(short, null, 0, true)
                }
            } else {
                val u = similar.single()
                o = Option(short, u.long, u.argCount, u.value)
                var value: String? = null
                if (o.argCount != 0) if ("" == left) {
                    val tok = tokens.firstOrNull()
                    if (tok == null || "--" == tok) tokens.throwError("$short requires argument")
                    value = tokens.shift()
                } else {
                    value = left
                    left = ""
                }
                if (tokens.error == DocoptExitException::class.java) {
                    o.value = value ?: true
                }
            }
            parsed.add(o)
        }
        return parsed
    }
}