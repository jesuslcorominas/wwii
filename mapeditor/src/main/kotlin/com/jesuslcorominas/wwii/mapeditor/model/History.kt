package com.jesuslcorominas.wwii.mapeditor.model

/** Undo/redo of immutable states. The editor records the state before each stroke. */
class History<T>(
    private val limit: Int = DEFAULT_LIMIT,
) {
    private val past = ArrayDeque<T>()
    private val future = ArrayDeque<T>()

    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    /** Records [before] as an undo step; a new change discards what could be redone. */
    fun record(before: T) {
        past.addLast(before)
        if (past.size > limit) past.removeFirst()
        future.clear()
    }

    /** The state to go back to from [current], or null if there is nothing to undo. */
    fun undo(current: T): T? {
        val previous = past.removeLastOrNull() ?: return null
        future.addLast(current)
        return previous
    }

    fun redo(current: T): T? {
        val next = future.removeLastOrNull() ?: return null
        past.addLast(current)
        return next
    }

    fun clear() {
        past.clear()
        future.clear()
    }

    private companion object {
        const val DEFAULT_LIMIT = 200
    }
}
