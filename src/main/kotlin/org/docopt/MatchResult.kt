package org.docopt

internal data class MatchResult(
    val match: Boolean,
    val left: List<LeafPattern>,
    val collected: List<LeafPattern>
)