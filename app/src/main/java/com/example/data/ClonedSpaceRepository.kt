package com.example.data

import kotlinx.coroutines.flow.Flow

class ClonedSpaceRepository(
    private val dao: ClonedSpaceDao
) {
    companion object {
        const val PINNED_PROCESS_SLOTS = 8
    }

    val allSpaces: Flow<List<ClonedSpace>> = dao.observeAll()

    suspend fun getById(id: Long): ClonedSpace? = dao.getById(id)

    /**
     * Allocates a dedicated pinned parallel OS slot (1..8) if one is free,
     * otherwise returns 0 to route into the Unlimited Dynamic Session Vault Pool.
     * There is never a hard cap on how many clones a user can create.
     */
    suspend fun allocateSlotOrUnlimited(): Int {
        val used = dao.getUsedContainerSlots().toSet()
        return (1..PINNED_PROCESS_SLOTS).firstOrNull { it !in used } ?: 0
    }

    suspend fun insert(space: ClonedSpace): Long = dao.insert(space)

    suspend fun update(space: ClonedSpace) = dao.update(space)

    suspend fun recordLaunch(id: Long) {
        dao.updateLastLaunched(id, System.currentTimeMillis())
    }

    suspend fun setLocked(id: Long, locked: Boolean) {
        dao.updateLocked(id, locked)
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}
