package org.docopt

import org.docopt.Py.count
import org.docopt.Py.list
import org.docopt.Py.set
import org.docopt.Py.split
import java.util.Arrays

internal abstract class Pattern {
    fun fix(): Pattern {
        fixIdentities(null)
        fixRepeatingArguments()
        return this
    }

    /**
     * Make pattern-tree tips point to same object if they are equal.
     */
    private fun fixIdentities(uniq: List<Pattern?>?) {
        // >>> if not hasattr(self, 'children')
        var uniq = uniq
        if (this !is BranchPattern) {
            return
        }
        if (uniq == null) {
            uniq = list<Pattern?>(set<Pattern>(flat()))
        }
        val children = this.children
        for (i in children!!.indices) {
            val child = children[i]
            if (child !is BranchPattern) {
                assert(uniq.contains(child))
                children[i] = uniq[uniq.indexOf(child)]
            } else {
                child.fixIdentities(uniq)
            }
        }
    }

    /**
     * Fix elements that should accumulate/increment values.
     */
    private fun fixRepeatingArguments() {
        var either: MutableList<List<Pattern?>>

        // >>> either = [list(child.children) for child in
        // transform(self).children]
        run {
            either = list()
            for (child in transform(this).children!!) {
                either.add(list<Pattern?>((child as Required).children!!))
            }
        }
        for (`$case` in either) {
            // >>> for e in [child for child in case if case.count(child) > 1]
            for (child in `$case`) { // ^^^
                if (count(`$case`, child) > 1) { // ^^^
                    val e = child as LeafPattern? // ^^^
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
        collected: List<LeafPattern>?
    ): MatchResult

     fun match(left: List<LeafPattern>): MatchResult {
        return match(left, null)
    }

    abstract override fun equals(obj: Any?): Boolean

    companion object {
        private val PARENTS = Arrays
            .asList(
                Required::class.java, Optional::class.java, OptionsShortcut::class.java,
                Either::class.java, OneOrMore::class.java
            )

        /**
         * Expand pattern into an (almost) equivalent one, but with single Either.
         *
         * Example: ((-a | -b) (-c | -d)) => (-a -c | -a -d | -b -c | -b -d) Quirks:
         * [-a] => (-a), (-a...) => (-a -a)
         */
        private fun transform(pattern: Pattern): Either {
            val result = list<List<Pattern?>>()
            var groups: MutableList<MutableList<Pattern?>>
            run {
                groups = list()
                groups.add(list(pattern))
            }
            while (!groups.isEmpty()) {
                val children = groups.removeAt(0)
                var child: BranchPattern? = null
                for (c in children) {
                    if (PARENTS.contains(c!!.javaClass)) {
                        child = c as BranchPattern?
                        break
                    }
                }

                // See above for changes from python implementation.
                if (child != null) {
                    children.remove(child)
                    if (child.javaClass == Either::class.java) {
                        for (c in child.children!!) {
                            // >>> groups.append([c] + children)
                            val group = list(c)
                            group.addAll(children)
                            groups.add(group)
                        }
                    } else if (child.javaClass == OneOrMore::class.java) {
                        // >>> groups.append(child.children * 2 + children)
                        val group = list<Pattern?>(child.children!!)
                        group.addAll(child.children!!)
                        group.addAll(children)
                        groups.add(group)
                    } else {
                        // >>> groups.append(child.children + children)
                        val group = list<Pattern?>(child.children!!)
                        group.addAll(children)
                        groups.add(group)
                    }
                } else {
                    result.add(children)
                }
            }

            // >>> return Either(*[Required(*e) for e in result])
            run {
                val required = list<Required?>()
                for (e in result) {
                    required.add(Required(e))
                }
                return Either(required)
            }
        }
    }
}