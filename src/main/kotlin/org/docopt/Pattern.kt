package org.docopt

import org.docopt.Py.count
import org.docopt.Py.list
import org.docopt.Py.set
import org.docopt.Py.split

internal abstract class Pattern {
    fun fix(): Pattern {
        fixIdentities()
        fixRepeatingArguments()
        return this
    }

    /**
     * Make pattern-tree tips point to same object if they are equal.
     */
    private fun fixIdentities(uniq: List<Pattern?>? = null) {
        var u = uniq
        if (this !is BranchPattern) {
            return
        }
        if (u == null) {
            u = list<Pattern?>(set(flat()))
        }
        for (i in children!!.indices) {
            val child = children[i]
            if (child !is BranchPattern) {
                assert(u.contains(child))
                children[i] = u[u.indexOf(child)]
            } else {
                child.fixIdentities(u)
            }
        }
    }

    /**
     * Fix elements that should accumulate/increment values.
     */
    private fun fixRepeatingArguments() {

        val either: MutableList<List<Pattern?>> = list()
        for (child in transform(this).children!!) {
            either.add(list((child as Required).children!!))
        }
        for (case in either) {
            for (child in case) {
                if (count(case, child) > 1) {
                    val e = child as LeafPattern?
                    if (e!!.javaClass == Argument::class.java
                        || e.javaClass == Option::class.java && (e as Option?)!!
                            .argCount != 0
                    ) {
                        if (e.value == null) {
                            e.value = list<Any>()
                        } else if (e.value !is List<*>) {
                            e.value = split(e.value.toString())
                        }
                    }
                    if (e.javaClass == Command::class.java
                        || e.javaClass == Option::class.java && (e as Option?)!!
                            .argCount == 0
                    ) {
                        e.value = 0
                    }
                }
            }
        }
    }

    abstract fun flat(vararg types: Class<*>): List<Pattern>

    abstract fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>? = null
    ): MatchResult

    abstract override fun equals(other: Any?): Boolean
    override fun hashCode(): Int = javaClass.hashCode()

    companion object {
        private val parents = listOf(
            Required::class.java,
            Optional::class.java,
            OptionsShortcut::class.java,
            Either::class.java,
            OneOrMore::class.java
        )

        /**
         * Expand pattern into an (almost) equivalent one, but with single Either.
         *
         * Example: ((-a | -b) (-c | -d)) => (-a -c | -a -d | -b -c | -b -d) Quirks:
         * [-a] => (-a), (-a...) => (-a -a)
         */
        private fun transform(pattern: Pattern): Either {
            val result = list<List<Pattern?>>()
            val groups: MutableList<MutableList<Pattern?>> = list()
            groups.add(list(pattern))
            while (groups.isNotEmpty()) {
                val children = groups.removeAt(0)
                var child: BranchPattern? = null
                for (c in children) {
                    if (parents.contains(c!!.javaClass)) {
                        child = c as BranchPattern?
                        break
                    }
                }

                if (child != null) {
                    children.remove(child)
                    if (child.javaClass == Either::class.java) {
                        for (c in child.children!!) {
                            val group = list(c)
                            group.addAll(children)
                            groups.add(group)
                        }
                    } else if (child.javaClass == OneOrMore::class.java) {
                        val group = list(child.children!!)
                        group.addAll(child.children!!)
                        group.addAll(children)
                        groups.add(group)
                    } else {
                        val group = list(child.children!!)
                        group.addAll(children)
                        groups.add(group)
                    }
                } else {
                    result.add(children)
                }
            }
            return Either(result.map { Required(it) })
        }
    }
}