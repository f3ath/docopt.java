package org.docopt

internal class SingleMatchResult(
    val position: Int? = null,
    val match: LeafPattern? = null
) {
    override fun toString() = String.format(
        "%s(%d, %s)", javaClass.simpleName,
        position, match
    )
}