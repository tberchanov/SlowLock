package com.slowlock.feature.shortcut.data

import android.content.Context
import android.content.pm.ShortcutManager
import com.slowlock.core.domain.IoDispatcher
import com.slowlock.feature.shortcut.domain.PinSupport
import com.slowlock.feature.shortcut.domain.PinSupportRepository
import com.slowlock.feature.shortcut.domain.pinSupport
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The production support check, against the real launcher.
 *
 * **Re-evaluated on every ask and never cached** (FR-028). The user may have changed launcher
 * while the app was away, so a value read once at startup would be an assumption rather than a
 * check — and this is the gate the constitution names on *every* pin attempt.
 *
 * A missing `ShortcutManager` reads as [PinSupport.Unsupported]: with no way to ask, the honest
 * answer is that pinning cannot be relied on here. This never returns [PinSupport.Unknown] —
 * asking produces an answer, and `Unknown` means the question has not been asked yet.
 *
 * The decision is [pinSupport]'s, a pure function over one lambda, which is what lets `PinGateTest`
 * drive both answers with no launcher and no `Context`. This class holds only the wiring.
 */
@Singleton
class PinSupportSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : PinSupportRepository {

    override suspend fun current(): PinSupport = withContext(io) {
        pinSupport {
            context.getSystemService(ShortcutManager::class.java)
                ?.isRequestPinShortcutSupported == true
        }
    }
}
