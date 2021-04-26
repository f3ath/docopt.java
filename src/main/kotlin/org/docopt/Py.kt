package org.docopt

import java.math.BigDecimal
import java.math.BigInteger
import java.util.Arrays
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern

internal object Py {
    object Re {
        const val IGNORECASE = Pattern.CASE_INSENSITIVE
        const val MULTILINE = (Pattern.MULTILINE or Pattern.UNIX_LINES)

        fun findAll(
            pattern: String,
            string: String, flags: Int
        ): MutableList<String> {
            return findAll(Pattern.compile(pattern, flags), string)
        }

        private fun findAll(
            pattern: Pattern,
            string: String
        ): MutableList<String> {
            val matcher = pattern.matcher(string)
            val result = mutableListOf<String>()
            while (matcher.find()) {
                if (matcher.groupCount() == 0) {
                    result.add(matcher.group())
                } else {
                    for (i in 0 until matcher.groupCount()) {
                        val match = matcher.group(i + 1)
                        if (match != null) {
                            result.add(match)
                        }
                    }
                }
            }
            return result
        }

    }

    fun bool(o: Any?): Boolean = when (o) {
        null -> false
        is Boolean -> o
        is String -> "" != o
        is Array<*> -> (o).size != 0
        is Collection<*> -> o.isNotEmpty()
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

    fun repr(o: Any?): String = when (o) {
        null -> "null"
        is String -> "\"$o\""
        is Array<*> -> Arrays.toString(o)
        else -> o.toString()
    }
}