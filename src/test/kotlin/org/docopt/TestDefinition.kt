package org.docopt

data class TestDefinition(
    val doc: String,
    val argv: List<String>,
    val expected: Any?,
)
