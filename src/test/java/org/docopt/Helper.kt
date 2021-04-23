package org.docopt

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.docopt.Py.partition
import java.io.IOException
import java.net.URL
import java.util.regex.Pattern

object Helper {

    fun getDefs(url: URL) = sequence {
        println("Generating test cases from $url")
        var raw = Parser.read(url.openStream())

        raw = Pattern
            .compile("#.*$", Pattern.MULTILINE)
            .matcher(raw)
            .replaceAll("")

        for (fixture in raw.split("r\"\"\"")) {
            if (fixture.isEmpty()) continue

            val (doc1, _, body) = partition(fixture, "\"\"\"")
            var first = true
            for (_case in body.split("$")) {
                if (first) {
                    first = false
                    continue
                }
                val (argv1, _, expect) = partition(_case.trim(), "\n")
                val argv = argv1
                    .trim()
                    .split(Regex("\\s+"))
                    .drop(1)
                val expected = expect(expect)
                yield(TestDefinition(doc1, argv, expected))
            }
        }
    }

    private val types: TypeReference<Map<String, Any>> =
        object : TypeReference<Map<String, Any>>() {}

    private fun expect(expect: String): Any? =
        if ("\"user-error\"" == expect) {
            expect
        } else try {
            ObjectMapper().readValue(expect, types)
        } catch (e: IOException) {
            throw IllegalStateException(
                "could not parse JSON object from:\n$expect", e
            )
        }
}