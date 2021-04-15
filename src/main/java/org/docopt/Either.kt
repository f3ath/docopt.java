package org.docopt

import java.util.Collections

internal class Either(children: List<Pattern?>?) : BranchPattern(children) {
    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>
    ): MatchResult {
        var collected: List<LeafPattern>? = collected
        if (collected == null) {
            collected = Python.list()
        }
        val outcomes = Python.list<MatchResult>()
        for (pattern in children) {
            val m = pattern.match(left, collected)
            if (m.matched()) {
                outcomes.add(m)
            }
        }
        if (!outcomes.isEmpty()) {
            // >>> return min(outcomes, key=lambda outcome: len(outcome[1]))
            run {
                return Collections.min(outcomes, java.util.Comparator { o1, o2 ->
                    val s1 = o1.left.size
                    val s2 = o2.left.size
                    s1.compareTo(s2)
                })
            }
        }
        return MatchResult(false, left, collected)
    }
}