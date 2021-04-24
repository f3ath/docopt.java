package org.docopt

import org.docopt.Py.`in`
import org.docopt.Py.isUpper
import org.docopt.Py.join
import org.docopt.Py.list
import org.docopt.Py.partition

internal object Helper {

    fun parseExpr(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Pattern?> {
        var seq = parseSeq(tokens, options)
        if ("|" != tokens.current()) {
            return seq
        }
        val result: MutableList<Pattern?> =
            if (seq.size > 1) list(Required(seq))
            else seq
        while ("|" == tokens.current()) {
            tokens.move()
            seq = parseSeq(tokens, options)
            result.addAll(if (seq.size > 1) listOf(Required(seq)) else seq)
        }
        return if (result.size > 1) listOf(Either(result)) else result
    }

    private fun parseSeq(
        tokens: Tokens,
        options: MutableList<Option>
    ): MutableList<Pattern?> {
        val result = list<Pattern?>()
        while (!`in`(tokens.current(), null, "]", ")", "|")) {
            var atom = parseAtom(tokens, options)
            if ("..." == tokens.current()) {
                atom = list<OneOrMore?>(OneOrMore(atom))
                tokens.move()
            }
            result.addAll(atom)
        }
        return result
    }

    private fun parseAtom(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Pattern?> {
        val token = tokens.current()
        val result: List<Pattern?>
        if ("(" == token || "[" == token) {
            tokens.move()
            val matching: String
            val u = parseExpr(tokens, options)
            if ("(" == token) {
                matching = ")"
                result = listOf(Required(u))
            } else {
                matching = "]"
                result = listOf(Optional(u))
            }
            if (matching != tokens.move()) {
                throw tokens.error("unmatched '%s'", token)
            }
            return result
        }
        if ("options" == token) {
            tokens.move()
            return listOf(OptionsShortcut())
        }
        if (token!!.startsWith("--") && "--" != token) {
            return parseLong(tokens, options)
        }
        if (token.startsWith("-") && !("-" == token || "--" == token)) {
            return parseShorts(tokens, options)
        }
        return if (token.startsWith("<") && token.endsWith(">") || isUpper(token)) {
            listOf(Argument(tokens.move()))
        } else listOf(Command(tokens.move()))
    }

    fun parseLong(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Option> {
        val long: String
        val eq: String
        var value: String?
        val a = partition(tokens.move()!!, "=")
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

        if (tokens.getError() == DocoptExitException::class.java && similar.isEmpty()) {
            options
                .filter { it.long?.startsWith(long) ?: false }
                .forEach { similar.add(it) }
        }
        if (similar.size > 1) {
            throw tokens.error(
                "%s is not a unique prefix: %s?", long,
                join(", ", similar.map { it.long })
            )
        }
        var o: Option
        if (similar.size < 1) {
            val argCount = if ("=" == eq) 1 else 0
            o = Option(null, long, argCount)
            options.add(o)
            if (tokens.getError() == DocoptExitException::class.java) {
                o = Option(null, long, argCount, if (argCount != 0) value else true)
            }
        } else {
            run {
                val u = similar[0]
                o = Option(
                    u.short, u.long, u.argCount,
                    u.value
                )
            }
            if (o.argCount == 0) {
                if (value != null) {
                    throw tokens.error("%s must not have an argument", o.long)
                }
            } else {
                if (value == null) {
                    val u = tokens.current()
                    if (u == null || "--" == u) {
                        throw tokens.error(
                            "%s requires argument",
                            o.long
                        )
                    }
                    value = tokens.move()
                }
            }
            if (tokens.getError() == DocoptExitException::class.java) {
                o.value = if (value != null) value else true
            }
        }
        return list(o)
    }

    fun parseShorts(
        tokens: Tokens,
        options: MutableList<Option>
    ): List<Option> {
        val token = tokens.move()
        assert(token!!.startsWith("-") && !token.startsWith("--"))
        var left = token.replaceFirst("^-+".toRegex(), "")
        val parsed = list<Option>()
        while ("" != left) {
            val short = "-" + left[0]
            left = left.substring(1)
            val similar: MutableList<Option> = list()
            options
                .filter { it.short == short }
                .forEach { similar.add(it) }
            if (similar.size > 1) throw tokens.error(
                "%s is specified ambiguously %d times",
                short, similar.size
            )
            var o: Option
            if (similar.size < 1) {
                o = Option(short, null, 0)
                options.add(o)
                if (tokens.getError() == DocoptExitException::class.java) {
                    o = Option(short, null, 0, true)
                }
            } else {
                val u = similar[0]
                o = Option(short, u.long, u.argCount, u.value)
                var value: String? = null
                if (o.argCount != 0) if ("" == left) {
                    val u = tokens.current()
                    if (u == null || "--" == u) throw tokens.error(
                        "%s requires argument",
                        short
                    )
                    value = tokens.move()
                } else {
                    value = left
                    left = ""
                }
                if (tokens.getError() == DocoptExitException::class.java) {
                    o.value = value ?: true
                }
            }
            parsed.add(o)
        }
        return parsed
    }
}