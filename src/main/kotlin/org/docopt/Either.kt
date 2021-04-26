package org.docopt

internal class Either(children: List<Pattern?>) : BranchPattern(children) {
    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>
    ): MatchResult = children
        .map { it!!.match(left, collected) }
        .filter { it.match }
        .minByOrNull { it.left.size }
        ?: MatchResult(false, left, collected)
}