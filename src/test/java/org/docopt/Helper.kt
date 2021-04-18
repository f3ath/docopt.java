package org.docopt

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.docopt.Py.partition
import java.io.IOException
import java.net.URL
import java.util.regex.Pattern

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

    fun getDefs(url: URL): List<TestDefinition> {
        println("Generating test cases from $url")
        var raw = DocoptStatic.read(url.openStream())
        raw = Pattern.compile("#.*$", Pattern.MULTILINE).matcher(raw)
            .replaceAll("")
        if (raw.startsWith("\"\"\"")) {
            raw = raw.substring(3)
        }
        val defs: MutableList<TestDefinition> = ArrayList()
        for (fixture in raw.split("r\"\"\"".toRegex()).toTypedArray()) {
            if (fixture.isEmpty()) {
                continue
            }
            val doc1: String
            val body: String
            run {
                val u = partition(fixture, "\"\"\"")
                doc1 = u[0]
                body = u[2]
            }
            var first = true
            for (_case in body.split("\\$".toRegex()).toTypedArray()) {
                if (first) {
                    first = false
                    continue
                }
                val argv1: String
                val expect: String
                run {
                    val u = partition(_case.trim { it <= ' ' }, "\n")
                    argv1 = u[0]
                    expect = u[2]
                }
                val argv = argv(argv1)
                val expected = this.expect(expect)
                val def = TestDefinition(doc1, argv, expected)
                defs.add(def)
            }
        }
        return defs
    }
}