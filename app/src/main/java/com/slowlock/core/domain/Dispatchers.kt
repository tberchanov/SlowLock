package com.slowlock.core.domain

import javax.inject.Qualifier

/**
 * The dispatcher a repository does disk or binder work on.
 *
 * This qualifier is the seam (D4). There is deliberately no `DispatcherProvider` interface: a
 * qualifier already lets a test substitute through the same constructor parameter, and an interface
 * with one production implementation on the same side of that seam is what FR-044 forbids.
 *
 * Obligation D1: `Dispatchers.IO` is named at one place in this project, `CoreDataModule`. Anywhere
 * else is a call site deciding for its callers, which Constitution IV prohibits.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * The dispatcher for CPU-bound work — sorting, collating, rasterising.
 *
 * Same rules as [IoDispatcher]: provided once, injected everywhere, never named at a call site.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
