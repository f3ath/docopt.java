package org.docopt

import java.util.Collections

internal class Either(children: List<Pattern?>) : BranchPattern(children) {
    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>?
    ): MatchResult {
        val col: List<LeafPattern> = collected ?: listOf()
        val outcomes = Py.list<MatchResult>()
        for (pattern in children) {
            val m = pattern!!.match(left, col)
            if (m.match) {
                outcomes.add(m)
            }
        }
        if (outcomes.isNotEmpty()) {
            return Collections.min(outcomes) { o1, o2 ->
                o1.left.size.compareTo(o2.left.size)
            }
        }
        return MatchResult(false, left, col)
    }
}