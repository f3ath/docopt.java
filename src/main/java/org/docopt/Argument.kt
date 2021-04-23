package org.docopt

internal open class Argument : LeafPattern {
    constructor(name: String?, value: Any?) : super(name, value)
    constructor(name: String?) : super(name)

    override fun singleMatch(left: List<LeafPattern>): SingleMatchResult {

        return  left
            .withIndex()
            .firstOrNull { (_, pattern) -> pattern is Argument }
            ?.let { (n, pattern) ->
                SingleMatchResult(
                    position = n,
                    match = Argument(name, pattern.value)
                )
            } ?: SingleMatchResult()

    }
}