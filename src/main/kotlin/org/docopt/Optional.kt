package org.docopt

internal open class Optional(children: List<Pattern?>) : BranchPattern(children) {
    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>
    ): MatchResult {
        var l = left
        var c = collected

        for (pattern in children) {
            val u = pattern!!.match(l, c)
            l = u.left
            c = u.collected
        }
        return MatchResult(true, l, c)
    }
}