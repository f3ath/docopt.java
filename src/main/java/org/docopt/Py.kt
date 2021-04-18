package org.docopt

import java.math.BigDecimal
import java.math.BigInteger
import java.util.Arrays
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object Py {
    fun bool(o: Any?): Boolean = when (o) {
        null -> false
        is Boolean -> o
        is String -> "" != o
        is Array<*> -> (o).size != 0
        is Collection<*> -> !o.isEmpty()
        is Map<*, *> -> o.isNotEmpty()
        is Number -> when (o) {
            is Int -> o != 0
            is Long -> o != 0L
            is Double -> o != 0.0
            is Float -> o != 0.0f
            is Byte -> o != 0.toByte()
            is Short -> o != 0.toShort()
            is AtomicInteger -> bool(o.get())
            is AtomicLong -> bool(o.get())
            is BigDecimal -> BigDecimal.ZERO != o
            is BigInteger -> BigInteger.ZERO != o
            else -> throw IllegalArgumentException("unknown numeric type:${o.javaClass}")
        }
        else -> true
    }

    fun <T> `in`(left: T?, vararg right: T): Boolean {
        for (o in right) {
            if (left != null) {
                if (left == o) {
                    return true
                }
            } else {
                if (o == null) {
                    return true
                }
            }
        }
        return false
    }

    fun <T> plus(a: List<T>, b: List<T>): List<T> = a + b

    fun repr(o: Any?): String = when (o) {
        null -> "null"
        is String -> "\"$o\""
        is Array<*> -> Arrays.toString(o)
        else -> o.toString()
    }

    fun <T> list(elements: Iterable<T>): List<T> = elements.toMutableList()

    fun <T> list(elements: Array<T>): List<T> = elements.toMutableList()

    fun <T> list(element: T): List<T> = mutableListOf(element)

    fun <T> list(): MutableList<T> = mutableListOf()

    fun <T> count(self: List<T>, obj: T): Int {
        var count = 0
        for (element in self) {
            if (element == obj) {
                count++
            }
        }
        return count
    }
}