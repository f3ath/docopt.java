package org.docopt

import org.docopt.Helper.argv
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory

class ParaTest {
    @TestFactory
    fun test() = Helper.getDefs(ParaTest::class.java.getResource("/testcases.docopt")!!).map {
        val message = String.format(
            "\n\"\"\"%s\"\"\"\n$ %s\n\b", it.doc, argv(it.argv)
        )
        dynamicTest(message) {
            val actual: Any = try {
                Docopt(it.doc)
                    .withStdOut(null)
                    .withStdErr(null)
                    .withExit(false)
                    .parse(it.argv)
            } catch (e: DocoptExitException) {
                "\"user-error\""
            }
            assertEquals(it.expected, actual)
        }
    }
}