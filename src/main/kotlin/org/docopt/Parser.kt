package org.docopt

import org.docopt.Option.Companion.parse
import org.docopt.Py.Re
import org.docopt.Py.Re.findAll
import java.io.InputStream
import java.util.Scanner
import kotlin.text.RegexOption.MULTILINE

internal object Parser {
    /**
     * Parses the "*options:" sections
     */
    fun parseOptions(doc: String) = findSections("options:", doc)
        .map { dropHeader(it) }
        .flatMap { parseOptionsFromSection(it) }
        .toMutableList()

    private fun dropHeader(section: String) =
        section.split(":", limit = 2, ignoreCase = true).last()

    private fun parseOptionsFromSection(section: String) =
        section
            .lines()
            .let { mergeMultilineOptions(it) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { parse(it) }

    private fun mergeMultilineOptions(lines: List<String>) = sequence {
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

    fun parseUsageSection(
        usage: String,
        options: MutableList<Option>
    ): Required {
        val formal = formalUsage(usage)
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
            ).replace(formal) { " ${it.value} " }

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

    private fun parseExpr(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Pattern> {
        var seq = parseSeq(tokens, options).toMutableList()
        if ("|" != tokens.firstOrNull()) {
            return seq
        }
        val result: MutableList<Pattern> =
            if (seq.size > 1) mutableListOf(Required(seq))
            else seq
        while ("|" == tokens.firstOrNull()) {
            tokens.removeFirstOrNull()
            seq = parseSeq(tokens, options).toMutableList()
            result.addAll(if (seq.size > 1) listOf(Required(seq)) else seq)
        }
        return if (result.size > 1) listOf(Either(result)) else result
    }

    private fun parseSeq(
        tokens: Tokens,
        options: MutableList<Option>
    ) = sequence {
        while (tokens.firstOrNull() !in arrayOf(null, "]", ")", "|")) {
            val atom = parseAtom(tokens, options)
            if ("..." == tokens.firstOrNull()) {
                yield(OneOrMore(atom))
                tokens.removeFirstOrNull()
            } else {
                yieldAll(atom)
            }
        }
    }

    private fun parseAtom(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Pattern> {
        val token = tokens.first()
        val matchingBrace = mapOf("(" to ")", "[" to "]")
        if ("(" == token || "[" == token) {
            tokens.removeFirstOrNull()
            val patterns = parseExpr(tokens, options)
            val result = if ("(" == token) {
                listOf(Required(patterns))
            } else {
                listOf(Optional(patterns))
            }
            return result.also {
                if (tokens.removeFirstOrNull() != matchingBrace[token]) {
                    tokens.throwError("unmatched '$token'")
                }
            }
        }
        if ("options" == token) {
            tokens.removeFirstOrNull()
            return listOf(OptionsShortcut())
        }
        if (token.startsWith("--") && "--" != token) {
            return parseLong(tokens, options)
        }
        if (token.startsWith("-") && "-" != token && "--" != token) {
            return parseShorts(tokens, options)
        }
        return if (token.startsWith("<") && token.endsWith(">") || isUpper(token))
            listOf(Argument(tokens.removeFirstOrNull()))
        else
            listOf(Command(tokens.removeFirstOrNull()))
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
                parsed.add(Argument(value = tokens.removeFirstOrNull()))
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

    fun findSections(
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

    private fun formalUsage(section: String): String = section
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

    private fun parseLong(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Option> {
        val parts = tokens.removeFirst().split("=", limit = 2)
        val long = parts.first()
        var value = if (parts.size > 1) parts.last() else null

        assert(long.startsWith("--"))
        val similar = options
            .map { it }
            .filter { it.long == long }
            .toMutableList()

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
                    value = tokens.removeFirstOrNull()
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
        val token = tokens.removeFirstOrNull()!!
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
                    value = tokens.removeFirstOrNull()
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

    private fun isUpper(self: String): Boolean {
        val letters = self.toCharArray().filter { it.isLetter() }
        return letters.isNotEmpty() && letters.all { it.isUpperCase() }
    }
}