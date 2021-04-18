package org.docopt

import org.docopt.Helper.argv
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class ParaTest {
    @TestFactory
    fun testFact() = Helper.getDefs(ParaTest::class.java.getResource("/testcases.docopt")!!).map {
        val message = String.format(
            "\n\"\"\"%s\"\"\"\n$ %s\n\b", it.doc, argv(it.argv)
        )
        DynamicTest.dynamicTest(message) {
            val actual: Any = try {
                Docopt(it.doc)
                    .withStdOut(null)
                    .withStdErr(null)
                    .withExit(false)
                    .parse(it.argv)
            } catch (e: DocoptExitException) {
                "\"user-error\""
            }
            Assertions.assertEquals(it.expected, actual)
        }
    }
}