package org.docopt

internal class OneOrMore(children: List<Pattern?>) : BranchPattern(children) {
    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>?
    ): MatchResult {
        val col = collected ?: listOf()
        var l = left
        var c = col
        var ll: List<LeafPattern>? = null
        var times = 0
        while (true) {
            val m = children.single()!!.match(l, c)
            l = m.left
            c = m.collected
            if (m.match) times++
            if (l == ll) break
            ll = l
        }
        return if (times >= 1)
            MatchResult(true, l, c)
        else
            MatchResult(false, left, col)
    }
}