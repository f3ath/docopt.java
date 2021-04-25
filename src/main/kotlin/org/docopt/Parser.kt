package org.docopt

import org.docopt.Option.Companion.parse
import org.docopt.Py.Re
import org.docopt.Py.Re.findAll
import org.docopt.Py.Re.split
import org.docopt.Py.Re.sub
import org.docopt.Py.isUpper
import org.docopt.Py.partition
import org.docopt.Py.split
import java.io.InputStream
import java.util.Scanner

internal object Parser {

    fun parseDefaults(doc: String?): MutableList<Option> {
        val defaults = mutableListOf<Option>()
        for (s in parseSection("options:", doc)) {
            val ss = partition(s, ":")[2]
            var split: List<String?>
            val pattern = "\\n *(-\\S+?)"
            val s1 = "\n" + ss
            split = split(pattern, s1).toMutableList()
            split.removeAt(0)
            run {
                val u = mutableListOf<String?>()
                var i = 1
                while (i < split.size) {
                    u.add(split[i - 1] + split[i])
                    i += 2
                }
                split = u
            }
            for (sss in split) {
                if (sss!!.startsWith("-")) {
                    defaults.add(parse(sss))
                }
            }
        }
        return defaults
    }

    fun parsePattern(
        source: String?,
        options: MutableList<Option>
    ): Required {
        var source1 = source
        source1 = sub("([\\[\\]\\(\\)\\|]|\\.\\.\\.)", " $1 ", source1!!)
        var sour2: MutableList<String>
        run {
            sour2 = mutableListOf()
            for (s in split("\\s+|(\\S*<.*?>)", source1)) {
                if (s != null && s != "") {
                    sour2.add(s)
                }
            }
        }
        val tokens = Tokens(sour2, DocoptLanguageError::class.java)
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
                parsed.add(Argument(value = tokens.pop()))
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
        source: String?
    ): List<String> {
        val u = findAll(
            "^([^\\n]*$name[^\\n]*\\n?(?:[ \\t].*?(?:\\n|$))*)",
            source!!,
            Re.IGNORECASE or Re.MULTILINE
        )
        for (i in u.indices) {
            u[i] = u[i].trim()
        }
        return u
    }

    fun formalUsage(section: String?): String {
        var sec = section
        run {
            val u = partition(sec!!, ":")
            sec = u[2]
        }
        val pu = split(sec!!)
        run {
            val sb = StringBuilder()
            sb.append("( ")
            val u: String = pu.removeAt(0)
            if (pu.isNotEmpty()) {
                for (s in pu) {
                    if (s == u) {
                        sb.append(") | (")
                    } else {
                        sb.append(s)
                    }
                    sb.append(" ")
                }
                sb.setLength(sb.length - 1)
            }
            sb.append(" )")
            return sb.toString()
        }
    }

    private fun parseExpr(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Pattern?> {
        var seq = parseSeq(tokens, options)
        if ("|" != tokens.peek()) {
            return seq
        }
        val result: MutableList<Pattern?> =
            if (seq.size > 1) mutableListOf(Required(seq))
            else seq
        while ("|" == tokens.peek()) {
            tokens.pop()
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
        while (!arrayOf(null, "]", ")", "|").contains(tokens.peek())) {
            var atom = parseAtom(tokens, options)
            if ("..." == tokens.peek()) {
                atom = mutableListOf(OneOrMore(atom))
                tokens.pop()
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
            tokens.pop()
            val matching: String
            val u = parseExpr(tokens, options)
            if ("(" == token) {
                matching = ")"
                result = listOf(Required(u))
            } else {
                matching = "]"
                result = listOf(Optional(u))
            }
            if (matching != tokens.pop()) {
                tokens.throwError("unmatched '$token'")
            }
            return result
        }
        if ("options" == token) {
            tokens.pop()
            return listOf(OptionsShortcut())
        }
        if (token.startsWith("--") && "--" != token) {
            return parseLong(tokens, options)
        }
        if (token.startsWith("-") && "-" != token && "--" != token) {
            return parseShorts(tokens, options)
        }
        return if (token.startsWith("<") && token.endsWith(">") || isUpper(token))
            listOf(Argument(tokens.pop()))
        else
            listOf(Command(tokens.pop()))
    }

    private fun parseLong(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Option> {
        val long: String
        val eq: String
        var value: String?
        val a = partition(tokens.pop()!!, "=")
        long = a[0]
        eq = a[1]
        value = a[2]

        assert(long.startsWith("--"))
        if ("" == eq && "" == value) {
            value = null
        }
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
            val argCount = if ("=" == eq) 1 else 0
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
                    val u = tokens.peek()
                    if (u == null || "--" == u) {
                        tokens.throwError("${option.long} requires argument")
                    }
                    value = tokens.pop()
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
        val token = tokens.pop()
        assert(token!!.startsWith("-") && !token.startsWith("--"))
        var left = token.replaceFirst("^-+".toRegex(), "")
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
            if (similar.size < 1) {
                o = Option(short, null, 0)
                options.add(o)
                if (tokens.error == DocoptExitException::class.java) {
                    o = Option(short, null, 0, true)
                }
            } else {
                val u = similar[0]
                o = Option(short, u.long, u.argCount, u.value)
                var value: String? = null
                if (o.argCount != 0) if ("" == left) {
                    val u = tokens.peek()
                    if (u == null || "--" == u) tokens.throwError(
                        "$short requires argument"
                    )
                    value = tokens.pop()
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