package org.docopt

internal class MatchResult(
    private val match: Boolean, val left: List<LeafPattern>,
    val collected: List<LeafPattern>
) {

    fun matched(): Boolean {
        return match
    }
}