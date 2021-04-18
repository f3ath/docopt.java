package org.docopt

import org.docopt.Py.`in`
import org.docopt.Py.bool
import org.docopt.Py.list
import org.docopt.Py.plus
import org.docopt.Py.repr

/**
 * Leaf/terminal node of a pattern tree.
 */
internal abstract class LeafPattern @JvmOverloads constructor(
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
            return if (!bool(types) || `in`<Class<*>>(javaClass, *types)) {
                list(this as Pattern)
            } else list()
        }
    }

    override fun match(
        left: List<LeafPattern>,
        collected: List<LeafPattern>
    ): MatchResult {
        var collected: List<LeafPattern>? = collected
        if (collected == null) {
            collected = list()
        }
        var pos: Int?
        var match: LeafPattern?
        run {
            val m = singleMatch(left)
            pos = m.position
            match = m.match
        }
        if (match == null) {
            return MatchResult(false, left, collected)
        }
        var left_: MutableList<LeafPattern>
        run {
            left_ = list()
            left_.addAll(left.subList(0, pos!!))
            if (pos!! + 1 < left.size) {
                left_.addAll(left.subList(pos!! + 1, left.size))
            }
        }
        var sameName: MutableList<LeafPattern>
        run {
            sameName = list()
            for (a in collected) {
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
                (if (v is String) list<Any>(v) else v)!!
            }
            if (sameName.isEmpty()) {
                match!!.value = increment
                return MatchResult(
                    true, left_,
                    plus(collected, list(match!!))
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
            return MatchResult(true, left_, collected)
        }
        return MatchResult(
            true, left_, plus(
                collected, list(
                    match!!
                )
            )
        )
    }

    protected abstract fun singleMatch(left: List<LeafPattern>): SingleMatchResult
    override fun hashCode(): Int {
        val prime = 31
        var result = super.hashCode()
        result = prime * result + (name?.hashCode() ?: 0)
        result = prime * result + if (value == null) 0 else value.hashCode()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (javaClass != obj!!.javaClass) {
            return false
        }
        val other = obj as LeafPattern?
        if (name == null) {
            if (other!!.name != null) {
                return false
            }
        } else if (name != other!!.name) {
            return false
        }
        return if (value == null) {
            other.value == null
        } else value == other.value
    }
}