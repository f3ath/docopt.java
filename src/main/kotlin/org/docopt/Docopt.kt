package org.docopt

import java.io.PrintStream
import kotlin.system.exitProcess

class Docopt(
    private val doc: String,
    private val exitOnException: Boolean = true,
    private val stdout: PrintStream? = System.out,
    private val stderr: PrintStream? = System.err,
    private val addHelpCommand: Boolean = true,
    private val applicationVersion: String? = null,
    private val optionsFirst: Boolean = false
) {
    private val usage = Parser.findSections("usage:", doc).single()
    private val options = Parser.parseOptions(doc)
    private val pattern = Parser.parsePattern(usage, options)

    fun parse(argv: List<String>) = try {
        doParse(argv)
    } catch (e: DocoptExitException) {
        if (!exitOnException) throw e
        val stream = if (e.isError()) stderr else stdout
        stream?.let {
            if (e.message != null) it.println(e.message)
            if (e.printUsage) it.println(usage)
        }
        exitProcess(e.exitCode)
    }

    private fun doParse(argv: List<String>): Map<String, Any?> {
        val options = Parser.parseArgv(
            Tokens(argv, DocoptExitException::class.java),
            options.toMutableList(),
            optionsFirst
        )
        val patternOptions = pattern.flat(Option::class)

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