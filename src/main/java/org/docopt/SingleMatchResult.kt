package org.docopt

internal class SingleMatchResult(val position: Int?, val match: LeafPattern?) {
    override fun toString(): String = String.format(
        "%s(%d, %s)", javaClass.simpleName,
        position, match
    )
}