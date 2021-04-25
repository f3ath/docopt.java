package org.docopt

internal open class Argument(name: String? = null, value: Any? = null) : LeafPattern(name, value) {

    override fun singleMatch(left: List<LeafPattern>): SingleMatchResult = left
        .withIndex()
        .firstOrNull { (_, pattern) -> pattern is Argument }
        ?.let { (n, pattern) ->
            SingleMatchResult(
                position = n,
                match = Argument(name, pattern.value)
            )
        } ?: SingleMatchResult()
}