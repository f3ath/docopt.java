package org.docopt

internal open class Argument : LeafPattern {
    constructor(name: String?, value: Any?) : super(name, value)
    constructor(name: String?) : super(name)

    override fun singleMatch(left: List<LeafPattern>): SingleMatchResult {
        // >>> for n, pattern in enumerate(left)
        for (n in left.indices) {
            val pattern = left[n]
            if (pattern.javaClass == Argument::class.java) {
                return SingleMatchResult(
                    n, Argument(
                        name,
                        pattern.value
                    )
                )
            }
        }
        return SingleMatchResult(null, null)
    }
}