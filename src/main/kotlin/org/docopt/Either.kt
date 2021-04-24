package org.docopt

import java.util.Collections

internal class Either(children: List<Pattern?>?) : BranchPattern(children ?: listOf()) {
    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>?
    ): MatchResult {
        val col: List<LeafPattern> = collected ?: Py.list()
        val outcomes = Py.list<MatchResult>()
        for (pattern in children!!) {
            val m = pattern!!.match(left, col)
            if (m.matched()) {
                outcomes.add(m)
            }
        }
        if (outcomes.isNotEmpty()) {
            return Collections.min(outcomes) { o1, o2 ->
                val s1 = o1.left.size
                val s2 = o2.left.size
                s1.compareTo(s2)
            }
        }
        return MatchResult(false, left, col)
    }
}