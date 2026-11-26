package com.example.system

import android.content.Context
import android.content.Intent
import com.example.container.BaseCloneContainerActivity
import com.example.container.CloneSlot1Activity
import com.example.container.CloneSlot2Activity
import com.example.container.CloneSlot3Activity
import com.example.container.CloneSlot4Activity
import com.example.container.CloneSlot5Activity
import com.example.container.CloneSlot6Activity
import com.example.container.CloneSlot7Activity
import com.example.container.CloneSlot8Activity
import com.example.container.CloneUnlimitedSlotActivity
import com.example.data.ClonedSpace

object CloneSlotLauncher {

    fun activityClassForSlot(slotIndex: Int): Class<out BaseCloneContainerActivity> = when (slotIndex) {
        1 -> CloneSlot1Activity::class.java
        2 -> CloneSlot2Activity::class.java
        3 -> CloneSlot3Activity::class.java
        4 -> CloneSlot4Activity::class.java
        5 -> CloneSlot5Activity::class.java
        6 -> CloneSlot6Activity::class.java
        7 -> CloneSlot7Activity::class.java
        8 -> CloneSlot8Activity::class.java
        else -> CloneUnlimitedSlotActivity::class.java
    }

    fun launchSpace(
        context: Context,
        space: ClonedSpace,
        workProfileManager: WorkProfileManager,
        clearSessionOnOpen: Boolean = false
    ): Boolean {
        return if (space.isWorkProfile) {
            workProfileManager.launchWorkApp(space.packageName)
        } else {
            val targetClass = activityClassForSlot(space.slotIndex)
            val intent = Intent(context, targetClass).apply {
                putExtra(BaseCloneContainerActivity.EXTRA_CLONE_ID, space.id)
                putExtra(BaseCloneContainerActivity.EXTRA_DISPLAY_NAME, space.displayName)
                putExtra(BaseCloneContainerActivity.EXTRA_TARGET_URL, space.targetUrl)
                putExtra(BaseCloneContainerActivity.EXTRA_SLOT_INDEX, space.slotIndex)
                putExtra(BaseCloneContainerActivity.EXTRA_BADGE_TEXT, space.badgeText)
                putExtra(BaseCloneContainerActivity.EXTRA_BADGE_COLOR, space.badgeColorHex)
                putExtra(BaseCloneContainerActivity.EXTRA_DESKTOP_UA, space.useDesktopUserAgent)
                putExtra(BaseCloneContainerActivity.EXTRA_CLEAR_SESSION, clearSessionOnOpen)
                putExtra(BaseCloneContainerActivity.EXTRA_USE_SESSION_VAULT, space.isUnlimitedPool)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            }
            context.startActivity(intent)
            true
        }
    }
}
