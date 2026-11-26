package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object CloneEngineType {
    const val WEB_CONTAINER = "WEB_CONTAINER"
    const val WORK_PROFILE = "WORK_PROFILE"
}

@Entity(tableName = "cloned_spaces")
data class ClonedSpace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val displayName: String,
    val engineType: String,
    val slotIndex: Int = 0, // 1..8 = pinned parallel process slot; 0 = unlimited dynamic vault pool (for WEB_CONTAINER) or native Work OS
    val targetUrl: String = "",
    val packageName: String = "",
    val presetKey: String? = null,
    val badgeText: String = "2",
    val badgeColorHex: Long = 0xFF8B5CF6,
    val useDesktopUserAgent: Boolean = false,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLaunchedAt: Long = 0L
) {
    val isContainer: Boolean get() = engineType == CloneEngineType.WEB_CONTAINER
    val isWorkProfile: Boolean get() = engineType == CloneEngineType.WORK_PROFILE
    val isUnlimitedPool: Boolean get() = isContainer && slotIndex == 0

    val slotLabel: String
        get() = when {
            isWorkProfile -> "WORK OS"
            slotIndex in 1..8 -> "SLOT ${slotIndex.toString().padStart(2, '0')}"
            else -> "VAULT ∞"
        }
}
