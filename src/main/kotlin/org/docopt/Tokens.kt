package org.docopt

class Tokens(
    source: List<String>,
    val error: Class<out Throwable>
) : ArrayList<String>(source) {

    fun throwError(
        message: String
    ): Nothing {
        if (error == DocoptLanguageError::class.java) {
            throw DocoptLanguageError(message)
        }
        if (error == DocoptExitException::class.java) {
            throw DocoptExitException(1, message, true)
        }
        throw IllegalStateException("Unexpected exception: ${error.name}")
    }
}