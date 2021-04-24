package org.docopt

internal open class Optional(children: List<Pattern?>) : BranchPattern(children) {
    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>?
    ): MatchResult {
        var ll: List<LeafPattern> = left
        var col: List<LeafPattern> = collected ?: mutableListOf()

        for (pattern in children) {
            val u = pattern!!.match(ll, col)
            ll = u.left
            col = u.collected
        }
        return MatchResult(true, ll, col)
    }
}