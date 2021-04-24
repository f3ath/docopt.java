package org.docopt

internal class Command constructor(name: String?, value: Any? = false) :
    Argument(name, value) {
    override fun singleMatch(left: List<LeafPattern>): SingleMatchResult {
        for (n in left.indices) {
            val pattern = left[n]
            if (pattern is Argument) {
                if (name == pattern.value) {
                    return SingleMatchResult(n, Command(name, true))
                }
                break
            }
        }
        return SingleMatchResult()
    }
}