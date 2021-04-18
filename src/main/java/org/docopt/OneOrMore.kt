package org.docopt

internal class OneOrMore(children: List<Pattern?>?) : BranchPattern(children) {
    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>
    ): MatchResult {
        var collected: List<LeafPattern> = collected ?: Py.list()
        assert(children.size == 1)
        var l = left
        var c = collected
        var l_: List<LeafPattern>? = null
        val matched = true
        var times = 0
        while (matched) {
            val m = children[0].match(l, c)
            l = m.left
            c = m.collected
            if (m.matched()) {
                times++
            }
            if (if (l == null) l_ == null else l == l_) {
                break
            }
            l_ = l
        }
        return if (times >= 1) {
            MatchResult(true, l, c)
        } else MatchResult(false, left, collected)
    }
}