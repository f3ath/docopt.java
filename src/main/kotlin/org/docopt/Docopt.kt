package org.docopt

import java.io.PrintStream
import kotlin.system.exitProcess

class Docopt(
    private val doc: String,
    private val exit: Boolean = true,
    private val out: PrintStream? = System.out,
    private val err: PrintStream? = System.err,
    private val addHelpCommand: Boolean = true,
    private val applicationVersion: String? = null,
    private val optionsFirst: Boolean = false
) {
    private val usage: String = Parser.parseSection("usage:", doc).single()
    private val options = Parser.parseDefaults(doc)
    private val pattern: Required = Parser.parsePattern(Parser.formalUsage(usage), options)

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
        val options = Parser.parseArgv(
            Tokens(argv, DocoptExitException::class.java), options.toMutableList(), optionsFirst
        )
        val patternOptions = pattern.flat(Option::class).toSet()

        for (optionsShortcut in pattern.flat(OptionsShortcut::class)) {
            val u = (optionsShortcut as BranchPattern).children
            u.clear()
            u.addAll(this.options)
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
        throwIfHelpRequested(options)
        throwIfVersionRequested(options)
        val m = pattern.fix().match(options)
        if (m.match && m.left.isEmpty()) {
            val u = mutableMapOf<String, Any?>()
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

    private fun throwIfVersionRequested(options: List<LeafPattern>) {
        val requested = options.any { "--version" == it.name }
        if (applicationVersion != null && requested)
            throw DocoptExitException(0, applicationVersion, false)
    }

    private fun throwIfHelpRequested(options: List<LeafPattern>) {
        val requested = options.any {
            ("-h" == it.name || "--help" == it.name) && Py.bool(it.value)
        }
        if (addHelpCommand && requested) throw DocoptExitException(0, doc.trim(), false)
    }
}