package org.docopt

import org.docopt.Py.list
import java.io.PrintStream
import kotlin.system.exitProcess

class Docopt(
    private val doc: String,
    private val exit: Boolean = true,
    private val out: PrintStream? = System.out,
    private val err: PrintStream? = System.err,
) {
    private val usage: String
    private val options: List<Option>
    private val pattern: Required
    private val help = true
    private val version: String? = null
    private val optionsFirst = false

    private fun doParse(argv: List<String>): Map<String?, Any?> {
        val argv = Parser.parseArgv(
            Tokens(argv, DocoptExitException::class.java), list(options), optionsFirst
        )
        val patternOptions = pattern
            .flat(
                Option::class.java
            )
            .toMutableSet()

        for (optionsShortcut in pattern
            .flat(OptionsShortcut::class.java)) {
            run {
                val u = (optionsShortcut as BranchPattern).children
                u.clear()
                u.addAll(options)
                var o: Pattern?
                val i = u.iterator()
                while (i.hasNext()) {
                    o = i.next()
                    for (x in patternOptions) {
                        if (o == x) {
                            i.remove()
                            break
                        }
                    }
                }
            }
        }
        Parser.extras(help, version, argv, doc)
        val m = pattern.fix().match(argv)
        if (m.matched() && m.left.isEmpty()) {
            val u: MutableMap<String?, Any?> = HashMap()
            for (p in pattern.flat()) {
                check(p is LeafPattern)
                val lp = p
                u[lp.name] = lp.value
            }
            for (p in m.collected) {
                u[p.name] = p.value
            }
            return u
        }
        throw DocoptExitException(1, null, true)
    }

    @Throws(DocoptExitException::class)
    fun parse(argv: List<String>): Map<String?, Any?> {
        return try {
            doParse(argv)
        } catch (e: DocoptExitException) {
            if (!exit) {
                throw e
            }
            val ps = if (e.exitCode == 0) out else err
            if (ps != null) {
                val message = e.message
                if (message != null) {
                    ps.println(message)
                }
                if (e.printUsage) {
                    ps.println(usage)
                }
            }
            exitProcess(e.exitCode)
        }
    }

    fun parse(vararg argv: String): Map<String?, Any?> {
        return parse(listOf(*argv))
    }

    init {
        val usageSections = Parser.parseSection("usage:", doc)
        if (usageSections.size == 0) {
            throw DocoptLanguageError(
                "\"usage:\" (case-insensitive) not found."
            )
        }
        if (usageSections.size > 1) {
            throw DocoptLanguageError(
                "More than one \"usage:\" (case-insensitive)."
            )
        }
        usage = usageSections[0]
        options = Parser.parseDefaults(doc)
        pattern = Parser.parsePattern(Parser.formalUsage(usage), options)
    }
}