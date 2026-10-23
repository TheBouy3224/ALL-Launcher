package com.movtery.zalithlauncher.ui.screens.splash

import org.junit.Assert.assertEquals
import org.junit.Test

class SetupErrorKindTest {
    @Test fun noSpace() {
        assertEquals(SetupErrorKind.NO_SPACE, classifySetupError("java.io.IOException: write failed: ENOSPC (No space left on device)"))
    }

    @Test fun cannotWrite() {
        assertEquals(SetupErrorKind.CANNOT_WRITE, classifySetupError("java.nio.file.AccessDeniedException: /data/user/0/x/files/components/a"))
        assertEquals(SetupErrorKind.CANNOT_WRITE, classifySetupError("java.io.FileNotFoundException: /x (Permission denied)"))
        assertEquals(SetupErrorKind.CANNOT_WRITE, classifySetupError("open failed: EROFS (Read-only file system)"))
    }

    @Test fun otherAndMissing() {
        assertEquals(SetupErrorKind.OTHER, classifySetupError("java.lang.IllegalStateException: boom"))
        assertEquals(SetupErrorKind.OTHER, classifySetupError(null))
    }
}
