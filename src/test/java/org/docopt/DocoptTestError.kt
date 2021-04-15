package org.docopt

internal class DocoptTestError(message: String?) : AssertionError(message) {
    @Synchronized
    override fun fillInStackTrace(): Throwable {
        return this
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}