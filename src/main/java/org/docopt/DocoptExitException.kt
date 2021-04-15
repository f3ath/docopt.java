package org.docopt

/**
 * An exception thrown by [Docopt.parse] to indicate that the application
 * should exit. This could be normal (e.g. default `--help` behavior) or
 * abnormal (e.g. incorrect arguments).
 */
class DocoptExitException @JvmOverloads internal constructor(
    /**
     * Returns a numeric code indicating the cause of the exit. By convention, a
     * non-zero code indicates abnormal termination.
     *
     * @return the exit code
     */
    val exitCode: Int, message: String? = null,
    val printUsage: Boolean = false
) : RuntimeException(message) {

    companion object {
        private const val serialVersionUID = 1L
    }
}