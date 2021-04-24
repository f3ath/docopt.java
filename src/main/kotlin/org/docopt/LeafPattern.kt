package org.docopt

import org.docopt.Py.bool
import org.docopt.Py.repr

/**
 * Leaf/terminal node of a pattern tree.
 */
internal abstract class LeafPattern constructor(
    val name: String?,
    var value: Any? = null
) : Pattern() {
    override fun toString(): String {
        return String.format(
            "%s(%s, %s)", javaClass.simpleName,
            repr(name), repr(value)
        )
    }

    override fun flat(vararg types: Class<*>): List<Pattern> {
        run {
            return if (!bool(types) || types.contains(javaClass)) {
                mutableListOf(this as Pattern)
            } else mutableListOf()
        }
    }

    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>?
    ): MatchResult {
        val col: List<LeafPattern> = collected ?: mutableListOf()
        var pos: Int?
        var match: LeafPattern?
        run {
            val m = singleMatch(left)
            pos = m.position
            match = m.match
        }
        if (match == null) {
            return MatchResult(false, left, col)
        }
        var left_: MutableList<LeafPattern>
        run {
            left_ = mutableListOf()
            left_.addAll(left.subList(0, pos!!))
            if (pos!! + 1 < left.size) {
                left_.addAll(left.subList(pos!! + 1, left.size))
            }
        }
        var sameName: MutableList<LeafPattern>
        run {
            sameName = mutableListOf()
            for (a in col) {
                if (name == a.name) {
                    sameName.add(a)
                }
            }
        }
        val increment: Any
        if (value is Int || value is List<*>) {
            increment = if (value is Int) {
                1
            } else {
                val v = match!!.value
                (if (v is String) mutableListOf(v) else v)!!
            }
            if (sameName.isEmpty()) {
                match!!.value = increment
                return MatchResult(
                    true, left_,
                    col + mutableListOf(match!!)
                )
            }
            run {
                val p = sameName[0]
                val v = p.value
                if (v is Int) {
                    val b = increment as Int
                    p.value = v + b
                } else if (v is List<*>) {
                    val a = v as MutableList<LeafPattern>
                    val b = increment as List<LeafPattern>
                    a.addAll(b)
                }
            }

            // TODO: Should collected be copied to a new list?
            return MatchResult(true, left_, col)
        }
        return MatchResult(
            true, left_, col + mutableListOf(match!!)
        )
    }

    protected abstract fun singleMatch(left: List<LeafPattern>): SingleMatchResult

    override fun hashCode(): Int {
        var result = 31 * super.hashCode() + (name?.hashCode() ?: 0)
        result = 31 * result + if (value == null) 0 else value.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean =
        other is LeafPattern && other.name == name && other.value == value
}