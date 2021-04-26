package org.docopt

sealed class Tokens(
    tokens: List<String>,
) : ArrayList<String>(tokens) {
    abstract fun throwError(message: String): Nothing
}

class ArgTokens(tokens: List<String>) : Tokens(tokens) {
    override fun throwError(message: String): Nothing {
        throw DocoptExitException(1, message, true)
    }
}

class UsageTokens(tokens: List<String>) : Tokens(tokens) {
    override fun throwError(message: String): Nothing {
        throw DocoptLanguageError(message)
    }
}