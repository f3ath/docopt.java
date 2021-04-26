package org.docopt

internal sealed class Tokens(
    tokens: List<String>,
) : ArrayList<String>(tokens) {
    abstract fun error(message: String): Throwable
}

internal class ArgTokens(tokens: List<String>) : Tokens(tokens) {
    override fun error(message: String) =
        DocoptExitException(exitCode = 1, message = message, printUsage = true)
}

internal class UsageTokens(tokens: List<String>) : Tokens(tokens) {
    override fun error(message: String) = DocoptLanguageError(message)
}