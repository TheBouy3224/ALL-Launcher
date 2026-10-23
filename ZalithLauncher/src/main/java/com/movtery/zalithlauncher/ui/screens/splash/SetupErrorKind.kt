package com.movtery.zalithlauncher.ui.screens.splash

/** Plain-language category of a first-run setup failure; the raw exception text is only shown under Details */
internal enum class SetupErrorKind { NO_SPACE, CANNOT_WRITE, OTHER }

internal fun classifySetupError(raw: String?): SetupErrorKind {
    val t = raw?.lowercase() ?: return SetupErrorKind.OTHER
    return when {
        "no space left" in t || "enospc" in t || "disk full" in t || "not enough space" in t -> SetupErrorKind.NO_SPACE
        "accessdenied" in t || "permission denied" in t || "eacces" in t || "erofs" in t ||
            "read-only" in t || "readonly" in t || "not permitted" in t || "unable to delete" in t -> SetupErrorKind.CANNOT_WRITE
        else -> SetupErrorKind.OTHER
    }
}
