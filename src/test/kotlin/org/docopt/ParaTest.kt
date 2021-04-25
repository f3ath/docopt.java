package org.docopt

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.net.URL
import java.util.regex.Pattern

class ParaTest {
    @TestFactory
    fun test(): List<DynamicTest> =
        getDefs(ParaTest::class.java.getResource("/testcases.docopt")!!)
            .toList()
            .map {
                val message = String.format("%s\n$ %s", it.doc, quote(it.argv))
                dynamicTest(message) {
                    println(message)
                    val actual: Any = try {
                        Docopt(
                            doc = it.doc,
                            stdout = null,
                            stderr = null,
                            exitOnException = false
                        ).parse(it.argv)
                    } catch (e: DocoptExitException) {
                        "user-error"
                    }
                    assertEquals(it.expected, actual)
                }
            }

    private fun quote(argv: List<String>) = argv
        .map { it.replace("\"", "\\\"") }
        .joinToString(separator = " ") { "\"$it\"" }

    private fun getDefs(url: URL) = sequence {
        println("Generating test cases from $url")

        var raw = Parser.read(url.openStream())

        raw = Pattern
            .compile("#.*$", Pattern.MULTILINE)
            .matcher(raw)
            .replaceAll("")

        for (fixture in raw.split("r\"\"\"")) {
            if (fixture.isEmpty()) continue

            val (doc1, body) = fixture.split("\"\"\"", limit = 2)
            var first = true
            for (_case in body.split("$")) {
                if (first) {
                    first = false
                    continue
                }
                val (argv1, expect) = _case.trim().split("\n", limit = 2)
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
        if ("\"user-error\"" == expect) "user-error"
        else ObjectMapper().readValue(expect, types)
}
