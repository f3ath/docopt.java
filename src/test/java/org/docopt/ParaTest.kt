package org.docopt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class ParaTest {
    @TestFactory
    fun test(): List<DynamicTest> = Helper
        .getDefs(ParaTest::class.java.getResource("/testcases.docopt")!!)
        .toList()
        .map {
            val message = String.format(
                "%s\n$ %s", it.doc, quote(it.argv)
            )
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
}
