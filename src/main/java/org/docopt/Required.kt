package org.docopt

import org.docopt.Py.list

internal class Required(children: List<Pattern?>?) : BranchPattern(children) {
    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>?
    ): MatchResult {
        var collected: List<LeafPattern>? = collected
        if (collected == null) {
            collected = list()
        }
        var l = left
        var c = collected
        for (pattern in children) {
            val m = pattern.match(l, c)
            l = m.left
            c = m.collected
            if (!m.matched()) {
                return MatchResult(false, left, collected)
            }
        }
        return MatchResult(true, l, c)
    }
}