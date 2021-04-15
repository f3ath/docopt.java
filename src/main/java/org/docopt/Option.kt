package org.docopt

import org.docopt.Python.Re

internal class Option @JvmOverloads constructor(
    `$short`: String?, `$long`: String?, argCount: Int = 0,
    value: Any? = false
) : LeafPattern( // >>> @property
    // >>> def name(self):
    // >>> return self.long or self.short
    `$long` ?: `$short`,  // >>> self.value = None if value is False and argcount else
    // value
    if (java.lang.Boolean.FALSE == value && argCount != 0) null else value
) {
    val short: String?
    val long: String?
    val argCount: Int
    override fun singleMatch(left: List<LeafPattern>): SingleMatchResult {
        for (n in left.indices) {
            val pattern = left[n]
            if (name == pattern.name) {
                return SingleMatchResult(n, pattern)
            }
        }
        return SingleMatchResult(null, null)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = super.hashCode()
        result = prime * result + (long?.hashCode() ?: 0)
        result = prime * result + (short?.hashCode() ?: 0)
        result = prime * result + argCount
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (!super.equals(other)) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val other = other as Option
        if (long == null) {
            if (other.long != null) {
                return false
            }
        } else if (long != other.long) {
            return false
        }
        if (short == null) {
            if (other.short != null) {
                return false
            }
        } else if (short != other.short) {
            return false
        }
        return argCount == other.argCount
    }

    override fun toString(): String {
        return String.format(
            "%s(%s, %s, %s, %s)", javaClass.simpleName,
            Python.repr(short), Python.repr(long), Python.repr(argCount), Python.repr(value)
        )
    }

    companion object {
        @JvmStatic
		fun parse(optionDescription: String): Option {
            var short: String? = null
            var long: String? = null
            var argCount = 0
            var value: Any? = false
            var options: String
            var description: String
            run {
                val a = Python.partition(optionDescription.trim { it <= ' ' }, "  ")
                options = a[0]
                description = a[2]
            }
            options = options.replace(",".toRegex(), " ").replace("=".toRegex(), " ")
            for (s in Python.split(options)) {
                if (s.startsWith("--")) {
                    long = s
                } else if (s.startsWith("-")) {
                    short = s
                } else {
                    argCount = 1
                }
            }
            if (argCount != 0) {
                val matched = Re.findAll(
                    "\\[default: (.*)\\]",
                    description, Re.IGNORECASE
                )
                value = if (Python.bool(matched)) matched[0] else null
            }
            return Option(short, long, argCount, value)
        }
    }

    // >>> def __init__(self, short=None, long=None, argcount=0, value=False)
    init {
        assert(argCount == 0 || argCount == 1)
        short = `$short`
        long = `$long`
        this.argCount = argCount
    }
}