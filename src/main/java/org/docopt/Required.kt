package org.docopt

import org.docopt.Py.list

internal class Required(children: List<Pattern?>?) : BranchPattern(children) {
    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>?
    ): MatchResult {
        val coll = collected ?: list()
        var l = left
        var c = coll
        for (pattern in children!!) {
            val m = pattern!!.match(l, c)
            l = m.left
            c = m.collected
            if (!m.matched()) {
                return MatchResult(false, left, coll)
            }
        }
        return MatchResult(true, l, c)
    }
}