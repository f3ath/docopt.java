package org.docopt

/**
 * Error in construction of usage-message by developer.
 */
internal class DocoptLanguageError(message: String) : Error(message)