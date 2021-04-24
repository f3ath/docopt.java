package org.docopt

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.IOException
import java.net.URL
import java.util.regex.Pattern

class ParaTest {
    @TestFactory
    fun test(): List<DynamicTest> =
        getDefs(ParaTest::class.java.getResource("/testcases.docopt")!!)
            .toList()
            .map {
                val message = String.format("%s\n$ %s", it.doc, quote(it.argv))
                println(message)
                dynamicTest(message) {
                    val actual: Any = try {
                        Docopt(it.doc, out = null, err = null, exit = false)
                            .parse(it.argv)
                    } catch (e: DocoptExitException) {
                        "\"user-error\""
                    }
                    assertEquals(it.expected, actual)
                }
            }

    private fun quote(argv: List<String>) = argv
        .map { it.replace("\"", "\\\"") }
        .joinToString(separator = " ") { "\"$it\"" }

    private fun getDefs(url: URL) = sequence {
        println("Generating test cases from $url")

        var raw = Helper.read(url.openStream())

        raw = Pattern
            .compile("#.*$", Pattern.MULTILINE)
            .matcher(raw)
            .replaceAll("")

        for (fixture in raw.split("r\"\"\"")) {
            if (fixture.isEmpty()) continue

            val (doc1, _, body) = Py.partition(fixture, "\"\"\"")
            var first = true
            for (_case in body.split("$")) {
                if (first) {
                    first = false
                    continue
                }
                val (argv1, _, expect) = Py.partition(_case.trim(), "\n")
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
