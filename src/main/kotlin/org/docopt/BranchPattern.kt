package org.docopt

import org.docopt.Py.`in`
import org.docopt.Py.join
import org.docopt.Py.list

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
        if (children.isEmpty()) "" else join(", ", children)
    )

    override fun flat(vararg types: Class<*>): List<Pattern> {
        if (`in`(javaClass, *types)) return list<Pattern>(this)
        val result = list<Pattern>()
        for (child in children) result.addAll(child!!.flat(*types))
        return result
    }
}