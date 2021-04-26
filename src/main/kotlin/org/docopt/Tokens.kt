package org.docopt

sealed class Tokens(
    tokens: List<String>,
) : ArrayList<String>(tokens) {
    abstract fun throwError(message: String): Nothing
}

class ArgTokens(tokens: List<String>) : Tokens(tokens) {
    override fun throwError(message: String) =
        throw DocoptExitException(exitCode = 1, message = message, printUsage = true)
}

class UsageTokens(tokens: List<String>) : Tokens(tokens) {
    override fun throwError(message: String) = throw DocoptLanguageError(message)
}