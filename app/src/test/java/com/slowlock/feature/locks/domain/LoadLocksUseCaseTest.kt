package com.slowlock.feature.locks.domain

import com.slowlock.core.domain.AppTarget
import com.slowlock.core.domain.AppTargetRepository
import com.slowlock.core.domain.DelayConfig
import com.slowlock.core.domain.DelayConfigRepository
import com.slowlock.core.domain.IconTreatment
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the Locks screen shows, decided without a device.
 *
 * Two rules meet here that used to sit in two places a JVM test could not reach together: the
 * reconciliation against the launcher, which lived inside `LockOrderStore` behind a
 * `SharedPreferences`, and the choice about what a silent launcher means, which lived in the
 * holder behind `Dispatchers.Main` (contract U7-U11).
 */
class LoadLocksUseCaseTest {

    /**
     * U7: `null` is "could not ask", and the only safe reading is the last list that was known
     * good. A use case that treated it as an empty set would empty the user's screen — and would
     * also write that emptiness back.
     */
    @Test
    fun `a launcher that cannot be asked leaves the list and the record untouched`() = runTest {
        val order = FakeOrder(stored = listOf(NOTES, MAIL))

        val locks = useCase(order = order, pinned = null)()

        assertEquals(listOf(NOTES, MAIL), locks.map { it.packageName })
        assertNull("a read that failed must never be written back", order.saved)
    }

    /**
     * U11: an empty set is the opposite claim — the launcher holds none of them — and it does empty
     * the list. Paired with the case above so neither passes by treating both the same way.
     */
    @Test
    fun `a launcher holding no shortcuts empties the list`() = runTest {
        val order = FakeOrder(stored = listOf(NOTES, MAIL))

        val locks = useCase(order = order, pinned = emptySet())()

        assertEquals(emptyList<String>(), locks.map { it.packageName })
        assertEquals(emptyList<String>(), order.saved)
    }

    /** U8: known packages keep their position, and a newly pinned one is appended. */
    @Test
    fun `a newly pinned package is appended after the packages already known`() = runTest {
        val order = FakeOrder(stored = listOf(NOTES, MAIL))

        val locks = useCase(order = order, pinned = setOf(MAIL, NOTES, PHOTOS))()

        assertEquals(listOf(NOTES, MAIL, PHOTOS), locks.map { it.packageName })
    }

    /**
     * U9: the ordinary visit costs a read and no write. Without this, the screen would rewrite the
     * same value to disk on every `ON_RESUME`.
     */
    @Test
    fun `an unchanged order is not written back`() = runTest {
        val order = FakeOrder(stored = listOf(NOTES, MAIL))

        useCase(order = order, pinned = setOf(NOTES, MAIL))()

        assertNull("nothing changed, so nothing may be written", order.saved)
    }

    /** U9's other half: an order that did change is written, or the next visit reshuffles. */
    @Test
    fun `a changed order is written back`() = runTest {
        val order = FakeOrder(stored = listOf(NOTES))

        useCase(order = order, pinned = setOf(NOTES, PHOTOS))()

        assertEquals(listOf(NOTES, PHOTOS), order.saved)
    }

    /** FR-005: the row carries the delay and treatment the configuration store holds. */
    @Test
    fun `a resolvable package becomes an available row carrying its stored values`() = runTest {
        val locks = useCase(
            order = FakeOrder(stored = listOf(NOTES)),
            pinned = null,
            config = { DelayConfig(30, IconTreatment.Gray) },
        )()

        assertEquals(listOf(Lock(NOTES, LABEL, VERSION, 30, IconTreatment.Gray)), locks)
        assertTrue(locks.single().isAvailable)
    }

    /**
     * U10, and the constitution's null-`getLaunchIntentForPackage()` obligation: the row is
     * produced, not dropped and not thrown over — the home screen may still carry its icon.
     */
    @Test
    fun `an unresolvable package becomes an unavailable row and does not throw`() = runTest {
        val locks = useCase(
            order = FakeOrder(stored = listOf(NOTES)),
            pinned = null,
            config = { DelayConfig(15, IconTreatment.Invert) },
            resolve = { null },
        )()

        val row = locks.single()
        assertNull(row.label)
        assertFalse(row.isAvailable)
        // Still the user's values: a reinstall must not silently lose what they chose.
        assertEquals(15, row.delaySeconds)
        assertEquals(IconTreatment.Invert, row.treatment)
        assertEquals(NOTES, row.packageName)
    }

    /** FR-032: a recorded package with nothing configured reads as [DelayConfig.DEFAULT]. */
    @Test
    fun `a package with no stored configuration reads as the default`() = runTest {
        val locks = useCase(order = FakeOrder(stored = listOf(NOTES)), pinned = null)()

        assertEquals(DelayConfig.DEFAULT_SECONDS, locks.single().delaySeconds)
        assertEquals(IconTreatment.entries.first(), locks.single().treatment)
    }

    /**
     * U10 and FR-006: row order is the lock list's and nothing re-sorts it. A row that moved would
     * look to the user like the lock had been re-made, and the gap left by a gone app must not
     * close.
     */
    @Test
    fun `row order matches the lock list order, gaps included`() = runTest {
        val packages = listOf(NOTES, MAIL, PHOTOS)

        val locks = useCase(
            order = FakeOrder(stored = packages),
            pinned = null,
            resolve = { if (it == MAIL) null else AppTarget(it, LABEL, VERSION) },
        )()

        assertEquals(packages, locks.map { it.packageName })
    }

    private fun useCase(
        order: FakeOrder,
        pinned: Set<String>?,
        config: (String) -> DelayConfig = { DelayConfig.DEFAULT },
        resolve: (String) -> AppTarget? = { AppTarget(it, LABEL, VERSION) },
    ) = LoadLocksUseCase(
        lockOrder = order,
        pinnedShortcuts = FakePinned(pinned),
        config = FakeConfig(config),
        targets = FakeTargets(resolve),
    )

    /** Records the write so a test can assert that one did *not* happen (U7, U9). */
    private class FakeOrder(private val stored: List<String>) : LockOrderRepository {
        var saved: List<String>? = null
        override suspend fun loadOrder(): List<String> = stored
        override suspend fun saveOrder(order: List<String>) {
            saved = order
        }
    }

    private class FakePinned(private val ids: Set<String>?) : PinnedShortcutsRepository {
        override suspend fun pinnedIds(): Set<String>? = ids
    }

    private class FakeConfig(private val answer: (String) -> DelayConfig) : DelayConfigRepository {
        override suspend fun load(packageName: String): DelayConfig = answer(packageName)
        override suspend fun save(packageName: String, config: DelayConfig) =
            error("loading the lock list must never write a configuration")
    }

    private class FakeTargets(private val answer: (String) -> AppTarget?) : AppTargetRepository {
        override suspend fun resolve(packageName: String): AppTarget? = answer(packageName)
    }

    private companion object {
        const val NOTES = "com.example.notes"
        const val MAIL = "com.example.mail"
        const val PHOTOS = "com.example.photos"
        const val LABEL = "Notes"
        const val VERSION = 42L
    }
}
