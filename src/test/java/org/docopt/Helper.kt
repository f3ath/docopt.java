package org.docopt

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
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

    fun argv(argv: String): List<String> {
        val u = argv.trim { it <= ' ' }
            .split("\\s+".toRegex()).toTypedArray().toMutableList()
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

    val types: TypeReference<Map<String, Any>> =
        object : TypeReference<Map<String, Any>>() {}

    fun expect(expect: String): Any? = if ("\"user-error\"" == expect) {
        "\"user-error\""
    } else try {
        ObjectMapper().readValue(expect, types)
    } catch (e: IOException) {
        throw IllegalStateException(
            "could not parse JSON object from:\n$expect", e
        )
    }


}