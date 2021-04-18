package org.docopt

import java.net.URL

object Helper {
    fun pureBaseName(url: URL): String {
        val name = url.path
        return if (name.isEmpty()) {
            name
        } else name
            .replaceFirst("^.+/".toRegex(), "")
            .replaceFirst("\\.[^.]+$".toRegex(), "")
    }

    fun argv(argv: String): List<String>? {
        val u = Python.list(argv.trim { it <= ' ' }
            .split("\\s+".toRegex()).toTypedArray())
        u.removeAt(0)
        return u
    }

    fun argv(argv: List<String>): String {
        val sb = StringBuilder()
        for (arg in argv) {
            sb.append("\"")
            sb.append(arg.replace("\"".toRegex(), "\\\""))
            sb.append("\" ")
        }
        if (argv.isNotEmpty()) {
            sb.setLength(sb.length - 1)
        }
        return sb.toString()
    }
}