package org.docopt

import org.docopt.Py.`in`
import org.docopt.Py.join
import org.docopt.Py.list

/**
 * Branch/inner node of a pattern tree.
 */
internal abstract class BranchPattern(children: List<Pattern?>?) : Pattern() {
    override fun hashCode(): Int {
        val prime = 31
        var result = super.hashCode()
        result = (prime * result
            + (children?.hashCode() ?: 0))
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (javaClass != obj!!.javaClass) {
            return false
        }
        val other = obj as BranchPattern?
        return if (children == null) {
            other!!.children == null
        } else children == other!!.children
    }

     val children: MutableList<Pattern?>?
    override fun toString(): String {
        return String.format(
            "%s(%s)", javaClass.simpleName,
            if (children!!.isEmpty()) "" else join(", ", children)
        )
    }

     override fun flat(vararg types: Class<*>): List<Pattern> {
        if (`in`<Class<*>>(javaClass, *types)) {
            return list<Pattern>(this)
        }
        run {
            val result = list<Pattern>()
            for (child in children!!) {
                result.addAll(child!!.flat(*types))
            }
            return result
        }
    }

    init {
        this.children = list<Pattern?>(children!!)
    }
}