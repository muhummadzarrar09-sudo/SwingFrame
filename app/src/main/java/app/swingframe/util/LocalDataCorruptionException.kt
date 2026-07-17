package app.swingframe.util

/** Indicates app-private JSON that was quarantined instead of being treated as valid empty data. */
class LocalDataCorruptionException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
