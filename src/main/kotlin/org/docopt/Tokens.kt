package org.docopt

class Tokens(
    source: List<String>,
    val error: Class<out Throwable>
) : ArrayList<String>(source) {

    fun move(): String? = if (isEmpty()) null else removeAt(0)

    fun current(): String? = if (isEmpty()) null else get(0)

    fun throwError(
        format: String?,
        vararg args: Any?
    ): IllegalStateException {
        val message = String.format(format!!, *args)
        if (error == DocoptLanguageError::class.java) {
            throw DocoptLanguageError(message)
        }
        if (error == DocoptExitException::class.java) {
            throw DocoptExitException(1, message, true)
        }
        return IllegalStateException("Unexpected exception: ${error.name}")
    }
}