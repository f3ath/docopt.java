package org.docopt

internal open class Optional(children: List<Pattern?>?) : BranchPattern(children) {
    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>
    ): MatchResult {
        var left: List<LeafPattern>? = left
        var collected: List<LeafPattern>? = collected
        if (collected == null) {
            collected = Py.list()
        }
        for (pattern in children) {
            val u = pattern.match(left, collected)
            left = u.left
            collected = u.collected
        }
        return MatchResult(true, left, collected)
    }
}