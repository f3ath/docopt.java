package org.docopt

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

    init {
        val usageSections = Parser.parseSection("usage:", doc)
        if (usageSections.isEmpty()) {
            throw DocoptLanguageError(
                "\"usage:\" (case-insensitive) not found."
            )
        }
        if (usageSections.size > 1) {
            throw DocoptLanguageError(
                "More than one \"usage:\" (case-insensitive)."
            )
        }
        usage = usageSections.first()
        options = Parser.parseDefaults(doc)
        pattern = Parser.parsePattern(Parser.formalUsage(usage), options)
    }

    fun parse(argv: List<String>): Map<String, Any?> = try {
        doParse(argv)
    } catch (e: DocoptExitException) {
        if (!exit) throw e
        val ps = if (e.exitCode == 0) out else err
        ps?.let {
            if (e.message != null) {
                it.println(e.message)
            }
            if (e.printUsage) {
                it.println(usage)
            }

        }
        exitProcess(e.exitCode)
    }

    fun parse(vararg argv: String) = parse(listOf(*argv))

    private fun doParse(argv: List<String>): Map<String, Any?> {
        val aaa = Parser.parseArgv(
            Tokens(argv, DocoptExitException::class.java), options.toMutableList(), optionsFirst
        )
        val patternOptions = pattern.flat(Option::class).toSet()

        for (optionsShortcut in pattern.flat(OptionsShortcut::class)) {
            val u = (optionsShortcut as BranchPattern).children
            u.clear()
            u.addAll(options)
            val i = u.iterator()
            while (i.hasNext()) {
                val o = i.next()
                for (x in patternOptions) {
                    if (o == x) {
                        i.remove()
                        break
                    }
                }
            }
        }
        Parser.extras(help, version, aaa, doc)
        val m = pattern.fix().match(aaa)
        if (m.match && m.left.isEmpty()) {
            val u =  mutableMapOf<String, Any?>()
            for (p in pattern.flat()) {
                check(p is LeafPattern)
                u[p.name!!] = p.value
            }
            for (p in m.collected) {
                u[p.name!!] = p.value
            }
            return u
        }
        throw DocoptExitException(exitCode = 1, printUsage = true)
    }
}