package org.docopt

import org.docopt.Py.repr
import kotlin.reflect.KClass

/**
 * Leaf/terminal node of a pattern tree.
 */
internal abstract class LeafPattern constructor(
    val name: String?,
    var value: Any? = null
) : Pattern() {
    override fun toString(): String = String.format(
        "%s(%s, %s)", javaClass.simpleName,
        repr(name), repr(value)
    )

    override fun flat(vararg types: KClass<*>): List<Pattern> =
        if (types.isEmpty() || types.contains(this::class)) {
            listOf(this)
        } else listOf()

    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>
    ): MatchResult {
        val m = singleMatch(left)
        val pos = m.position
        val match = m.match ?: return MatchResult(false, left, collected)
        val myLeft: MutableList<LeafPattern> = mutableListOf()

        myLeft.addAll(left.subList(0, pos!!))
        if (pos + 1 < left.size) myLeft.addAll(left.subList(pos + 1, left.size))
        val sameName: MutableList<LeafPattern> = mutableListOf()
        sameName.addAll(collected.filter { it.name == name })

        if (value !is Int && value !is List<*>) {
            return MatchResult(
                true,
                myLeft,
                collected + mutableListOf(match)
            )
        }

        val increment: Any = if (value is Int) {
            1
        } else {
            val v = match.value!!
            if (v is String) mutableListOf(v) else v
        }
        if (sameName.isEmpty()) {
            match.value = increment
            return MatchResult(
                true, myLeft,
                collected + mutableListOf(match)
            )
        }
        val first = sameName.first()
        val v = first.value
        if (v is Int) {
            first.value = v + increment as Int
        } else if (v is List<*>) {
            (v as MutableList<LeafPattern>).addAll(increment as List<LeafPattern>)
        }

        // TODO: Should collected be copied to a new list?
        return MatchResult(true, myLeft, collected)
    }

    protected abstract fun singleMatch(left: List<LeafPattern>): SingleMatchResult

    override fun hashCode(): Int {
        return 31 * (31 * super.hashCode() + (name?.hashCode()
            ?: 0)) + if (value == null) 0 else value.hashCode()
    }

    override fun equals(other: Any?): Boolean =
        other is LeafPattern && other.name == name && other.value == value
}