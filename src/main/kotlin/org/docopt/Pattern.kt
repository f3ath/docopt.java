package org.docopt

import kotlin.reflect.KClass

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
        if (this !is BranchPattern) return
        val u = uniq ?: flat().toMutableList()
        for (i in children.indices) {
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
        val either: MutableList<List<Pattern?>> = mutableListOf()
        for (child in transform(this).children) {
            either.add((child as Required).children.toMutableList())
        }
        for (case in either) {
            for (child in case) {
                if (case.filter { it == child }.size > 1) {
                    val e = child as LeafPattern?
                    if (e is Argument || e is Option && e.argCount != 0) {
                        if (e.value == null) {
                            e.value = mutableListOf<Any>()
                        } else if (e.value !is List<*>) {
                            e.value = e.value.toString().trim().split(Regex("\\s+")).toMutableList()
                        }
                    }
                    if (e is Command || e is Option && e.argCount == 0) {
                        e.value = 0
                    }
                }
            }
        }
    }

    abstract fun flat(vararg types: KClass<*>): List<Pattern>

    abstract fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern> = listOf()
    ): MatchResult

    abstract override fun equals(other: Any?): Boolean

    override fun hashCode(): Int = javaClass.hashCode()

    companion object {

        /**
         * Expand pattern into an (almost) equivalent one, but with single Either.
         *
         * Example: ((-a | -b) (-c | -d)) => (-a -c | -a -d | -b -c | -b -d) Quirks:
         * [-a] => (-a), (-a...) => (-a -a)
         */
        private fun transform(pattern: Pattern): Either {
            val result = mutableListOf<List<Pattern?>>()
            val groups: MutableList<MutableList<Pattern?>> =
                mutableListOf(mutableListOf(pattern))

            while (groups.isNotEmpty()) {
                val children = groups.removeAt(0)
                val child: BranchPattern? = children
                    .filterIsInstance<BranchPattern>()
                    .firstOrNull()

                if (child != null) {
                    children.remove(child)
                    when (child) {
                        is Either -> child.children.forEach {
                            groups.add((children + listOf(it)).toMutableList())
                        }
                        is OneOrMore -> groups.add(
                            (child.children + child.children + children).toMutableList()
                        )
                        else -> groups.add((child.children + children).toMutableList())
                    }
                } else {
                    result.add(children)
                }
            }
            return Either(result.map { Required(it) })
        }
    }
}