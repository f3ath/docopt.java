package org.docopt

class Tokens(source: List<String>, private val error: Class<out Throwable>) : ArrayList<String>() {
    init {
        addAll(source)
    }

    fun move(): String? = if (isEmpty()) null else removeAt(0)

    fun current(): String? = if (isEmpty()) null else get(0)

    fun getError(): Class<out Throwable?> = this.error

    fun error(
        format: String?,
        vararg args: Any?
    ): IllegalStateException {
        val message = String.format(format!!, *args)
        if (error == DocoptLanguageError::class.java) {
            throw org.docopt.DocoptLanguageError(message)
        }
        if (error == DocoptExitException::class.java) {
            throw org.docopt.DocoptExitException(1, message, true)
        }
        return IllegalStateException(
            "Unexpected exception: "
                + error.name
        )
    }
}