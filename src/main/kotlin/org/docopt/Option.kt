package org.docopt

internal class Option constructor(
    val short: String?,
    val long: String?,
    val argCount: Int = 0,
    value: Any? = false
) : LeafPattern(
    long ?: short,
    if (false == value && argCount != 0) null else value
) {
    init {
        assert(argCount == 0 || argCount == 1)
    }

    fun clone() = Option(short, long, argCount, value)

    override fun singleMatch(left: List<LeafPattern>): SingleMatchResult = left
        .withIndex()
        .firstOrNull { (_, pattern) -> pattern.name == name }
        ?.let { (n, pattern) ->
            SingleMatchResult(
                position = n,
                match = pattern
            )
        } ?: SingleMatchResult()

    override fun hashCode(): Int {
        val prime = 31
        var result = super.hashCode()
        result = prime * result + (long?.hashCode() ?: 0)
        result = prime * result + (short?.hashCode() ?: 0)
        result = prime * result + argCount
        return result
    }

    override fun equals(other: Any?): Boolean = other is Option
        && super.equals(other)
        && other.long == long
        && other.short == short
        && other.argCount == argCount

    override fun toString(): String = String.format(
        "%s(%s, %s, %s, %s)", javaClass.simpleName,
        Py.repr(short)
    )

    companion object {
        fun parse(optionDescription: String): Option {
            var short: String? = null
            var long: String? = null
            var argCount = 0
            var value: Any? = false
            var options: String
            val description: String
            val a = Py.partition(optionDescription.trim(), "  ")
            options = a[0]
            description = a[2]
            options = options.replace(",", " ").replace("=", " ")
            for (s in Py.split(options)) {
                when {
                    s.startsWith("--") -> long = s
                    s.startsWith("-") -> short = s
                    else -> argCount = 1
                }
            }
            if (argCount != 0) {
                val matched = Py.Re.findAll(
                    "\\[default: (.*)\\]", description, Py.Re.IGNORECASE
                )
                value = if (Py.bool(matched)) matched[0] else null
            }
            return Option(short, long, argCount, value)
        }
    }
}