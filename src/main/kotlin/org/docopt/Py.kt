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

        /**
         * Determines if [pattern] contains at least one capturing group.
         */
        private fun hasGrouping(pattern: String): Boolean {

            var i = -1

            // Find the potential beginning of a group by looking for a left
            // parenthesis character.
            while (pattern.indexOf('(', i + 1).also { i = it } != -1) {
                var c = 0

                // Count the number of escape characters immediately preceding
                // the
                // left parenthesis character.
                for (j in i - 1 downTo -1 + 1) {
                    if (pattern[j] != '\\') {
                        break
                    }
                    c++
                }

                // If there is an even number of consecutive escape characters,
                // the character is not escaped and begins a group.
                if (c % 2 == 0) {
                    return true
                }
            }
            return false
        }

        fun split(
            pattern: String,
            string: String
        ): List<String?> {
            if (!hasGrouping(pattern)) {
                return string.split(pattern.toRegex()).toTypedArray().toMutableList()
            }
            val matcher = Pattern.compile(pattern, 0).matcher(string)
            val matches = mutableListOf<String?>()
            var start = 0
            while (matcher.find()) {
                matches.add(string.substring(start, matcher.start()))
                for (i in 0 until matcher.groupCount()) {
                    val element = matcher.group(i + 1)
                    matches.add(element)
                }
                start = matcher.end()
            }
            matches.add(string.substring(start))
            return matches
        }

        fun sub(pattern: String, repl: String, string: String): String =
            Pattern.compile(pattern, 0).matcher(string).replaceAll(repl)
    }

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

    fun <T> count(self: List<T>, obj: T): Int {
        var count = 0
        for (element in self) {
            if (element == obj) {
                count++
            }
        }
        return count
    }

    fun <T> set(elements: Iterable<T>): Set<T> = elements.toMutableSet()

    fun join(self: String, iterable: Iterable<*>): String = iterable.joinToString(self)

    fun partition(self: String, sep: String): Array<String> {
        val i = self.indexOf(sep)
        if (i == -1) {
            return arrayOf(self, "", "")
        }

        val j = i + sep.length
        return arrayOf(
            self.substring(0, i), sep,
            if (j < self.length) self.substring(j) else ""
        )
    }

    fun isUpper(self: String): Boolean {
        var result = false
        for (c in self.toCharArray()) {
            if (Character.isLetter(c)) {
                result = if (Character.isUpperCase(c)) {
                    true
                } else {
                    return false
                }
            }
        }
        return result
    }

    fun split(self: String): MutableList<String> =
        self.trim().split("\\s+".toRegex()).toMutableList()
}