package com.example

import com.example.data.ArcadeCatalog
import com.example.data.PlayerStatusOption
import com.example.data.UserProfile
import com.example.data.formattedJoinDate
import com.example.data.formattedStatusLabel
import com.example.data.resolvedStatusKey
import com.example.data.unwrapFirestoreMessage
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArcadeLogicUnitTest {

    @Test
    fun `calculateLevel scales every 250 XP`() {
        assertEquals(1L, ArcadeCatalog.calculateLevel(0L))
        assertEquals(1L, ArcadeCatalog.calculateLevel(249L))
        assertEquals(2L, ArcadeCatalog.calculateLevel(250L))
        assertEquals(5L, ArcadeCatalog.calculateLevel(1000L))
    }

    @Test
    fun `xpForNextLevel calculates next threshold`() {
        assertEquals(250L, ArcadeCatalog.xpForNextLevel(1L))
        assertEquals(750L, ArcadeCatalog.xpForNextLevel(3L))
    }

    @Test
    fun `userProfile formattedJoinDate handles null and valid timestamps`() {
        val pendingProfile = UserProfile(displayName = "Nova", createdAt = null)
        assertEquals("Just joined", pendingProfile.formattedJoinDate())

        val epochDate = Date(1700000000000L)
        val expected = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(epochDate)
        val existingProfile = UserProfile(
            displayName = "Nova",
            createdAt = Timestamp(epochDate)
        )
        assertEquals(expected, existingProfile.formattedJoinDate())
    }

    @Test
    fun `dailyRewards spans 7 consecutive days`() {
        assertEquals(7, ArcadeCatalog.dailyRewards.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), ArcadeCatalog.dailyRewards.map { it.day })
        assertTrue(ArcadeCatalog.dailyRewards.all { it.coins > 0 && it.xp > 0 })
    }

    @Test
    fun `unwrapFirestoreMessage uses localized message or fallback`() {
        val withMessage = IllegalStateException("Network unavailable")
        assertEquals("Network unavailable", withMessage.unwrapFirestoreMessage("Fallback"))

        val withoutMessage = IllegalStateException()
        assertEquals("Fallback", withoutMessage.unwrapFirestoreMessage("Fallback"))
    }

    @Test
    fun `userProfile resolvedStatusKey and formattedStatusLabel prioritize live and normalize keys`() {
        val liveProfile = UserProfile(liveNow = true, playerStatus = "away")
        assertEquals("live", liveProfile.resolvedStatusKey())
        assertEquals("LIVE", liveProfile.formattedStatusLabel())

        val lfgProfile = UserProfile(liveNow = false, playerStatus = "lfg")
        assertEquals("lfg", lfgProfile.resolvedStatusKey())
        assertEquals("Squad Ready", lfgProfile.formattedStatusLabel())

        val unknownProfile = UserProfile(liveNow = false, playerStatus = "unknown")
        assertEquals(PlayerStatusOption.ONLINE.key, unknownProfile.resolvedStatusKey())
        assertEquals("Online", unknownProfile.formattedStatusLabel())
    }
}
