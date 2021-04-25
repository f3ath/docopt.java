package org.docopt

import kotlin.reflect.KClass

/**
 * Branch/inner node of a pattern tree.
 */
internal abstract class BranchPattern(children: Collection<Pattern?>) : Pattern() {
    override fun hashCode(): Int = (31 * super.hashCode() + children.hashCode())

    override fun equals(other: Any?): Boolean =
        other is BranchPattern && children == other.children

    val children: MutableList<Pattern?> = children.toMutableList()

    override fun toString(): String = String.format(
        "%s(%s)", javaClass.simpleName,
        if (children.isEmpty()) "" else children.joinToString<Any?>(", ")
    )

    override fun flat(vararg types: KClass<*>): List<Pattern> =
        if (types.contains(this::class)) listOf(this)
        else children.flatMap { it!!.flat(*types) }
}