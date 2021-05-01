package org.docopt

internal class Command constructor(name: String?, value: Any? = false) :
    Argument(name, value) {
    override fun singleMatch(left: List<LeafPattern>): SingleMatchResult = left
        .indexOfFirst { it is Argument && it.value == name }
        .let {
            if (it > -1)
                SingleMatchResult(it, Command(name, true))
            else
                SingleMatchResult()
        }
}