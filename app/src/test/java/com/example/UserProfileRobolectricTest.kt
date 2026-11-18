package com.example

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.PlayerStatusOption
import com.example.data.UserPrivate
import com.example.data.UserProfile
import com.example.data.formattedJoinDate
import com.example.data.unwrapFirestoreMessage
import com.example.ui.auth.GoogleSignInButton
import com.example.ui.components.DailyRewardSectionCard
import com.example.ui.components.LevelProgressionCard
import com.example.ui.components.ProfileStatusButton
import com.example.ui.components.StatusPickerBottomSheet
import com.example.ui.components.UserProfileCard
import com.example.ui.theme.MobiliveArcadeTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserProfileRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `userProfileCard displays name join date and follow actions`() {
        val sampleProfile = UserProfile(
            id = "user_1",
            userId = "user_1",
            handle = "pixel_ace",
            displayName = "Pixel Ace",
            bio = "Minecraft LAN host & speedrunner",
            avatarStyle = "pixel_knight",
            level = 4L,
            xp = 820L,
            followersCount = 128L,
            followingCount = 42L,
            createdAt = Timestamp(Date(1700000000000L))
        )
        var followClicks = 0
        var actionClicks = 0

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                UserProfileCard(
                    profile = sampleProfile,
                    isOwnProfile = false,
                    isFollowing = false,
                    onToggleFollow = { followClicks++ },
                    onMessageOrAdd = { actionClicks++ },
                    actionLabel = "Add Friend",
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("user_profile_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_profile_display_name")
            .assertIsDisplayed()
            .assertTextContains("Pixel Ace")
        composeTestRule.onNodeWithTag("user_profile_join_date")
            .assertIsDisplayed()
            .assertTextContains("Joined ${sampleProfile.formattedJoinDate()}")

        composeTestRule.onNodeWithTag("user_profile_follow_button")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("user_profile_action_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, followClicks)
        assertEquals(1, actionClicks)
    }

    @Test
    fun `userProfileCard hides follow and add buttons for own profile`() {
        val ownProfile = UserProfile(
            userId = "me",
            handle = "arcade_host",
            displayName = "Arcade Host"
        )

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                UserProfileCard(
                    profile = ownProfile,
                    isOwnProfile = true,
                    isFollowing = false,
                    onToggleFollow = {},
                    onMessageOrAdd = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("user_profile_display_name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_profile_follow_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("user_profile_action_button").assertDoesNotExist()
    }

    @Test
    fun `googleSignInButton renders label and handles click`() {
        var clicked = false
        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                GoogleSignInButton(
                    label = "Sign in with Google",
                    isLoading = false,
                    onClick = { clicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("google_signin_button")
            .assertIsDisplayed()
            .assertTextContains("Sign in with Google")
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue(clicked)
    }

    @Test
    fun `unwrapFirestoreMessage extracts nested FirebaseFirestoreException under Robolectric`() {
        val fsEx = com.google.firebase.firestore.FirebaseFirestoreException(
            "PERMISSION_DENIED: Missing permissions",
            com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
        )
        val wrapped = RuntimeException("Outer wrapper", fsEx)
        val msg = wrapped.unwrapFirestoreMessage("Fallback")
        assertTrue(msg.contains("PERMISSION_DENIED"))
    }

    @Test
    fun `profileStatusButton renders status text and handles click`() {
        var clicked = 0
        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                ProfileStatusButton(
                    statusKey = PlayerStatusOption.IN_GAME.key,
                    currentGame = "Minecraft",
                    onClick = { clicked++ },
                    modifier = Modifier.testTag("standalone_status_button")
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("standalone_status_button")
            .assertIsDisplayed()
            .assertTextContains("In-Game • Minecraft", substring = true)
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, clicked)
    }

    @Test
    fun `userProfileCard displays status button and opens picker`() {
        var opened = 0
        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                UserProfileCard(
                    profile = UserProfile(
                        userId = "u1",
                        handle = "nova",
                        displayName = "Nova",
                        currentGame = "Minecraft",
                        playerStatus = PlayerStatusOption.LFG.key
                    ),
                    isOwnProfile = true,
                    isFollowing = false,
                    onToggleFollow = {},
                    onMessageOrAdd = {},
                    onOpenStatusPicker = { opened++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("user_profile_status_button")
            .assertIsDisplayed()
            .assertTextContains("Squad Ready", substring = true)
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, opened)
    }

    @Test
    fun `statusPickerBottomSheet lists statuses and selects option`() {
        var selected: PlayerStatusOption? = null
        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                StatusPickerBottomSheet(
                    currentStatusKey = PlayerStatusOption.ONLINE.key,
                    currentGame = "Valorant Mobile",
                    isLiveNow = false,
                    onDismiss = {},
                    onSelectStatus = { selected = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("status_picker_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status_option_in_game")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(PlayerStatusOption.IN_GAME, selected)
    }

    @Test
    fun `dailyRewardSectionCard enables claim when never claimed`() {
        var claims = 0
        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                DailyRewardSectionCard(
                    userPrivate = UserPrivate(
                        userId = "u1",
                        streakDays = 0L,
                        lastClaimDayIndex = 0L,
                        lastDailyClaimAt = null
                    ),
                    isClaiming = false,
                    onClaim = { claims++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("daily_reward_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("daily_reward_last_claimed")
            .assertIsDisplayed()
            .assertTextContains("Not claimed yet", substring = true)
        composeTestRule.onNodeWithTag("daily_reward_countdown")
            .assertIsDisplayed()
            .assertTextContains("Bonus ready", substring = true)
        composeTestRule.onNodeWithTag("claim_daily_coins_button")
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, claims)
    }

    @Test
    fun `dailyRewardSectionCard disables claim and shows countdown within 24 hours`() {
        val twoHoursAgo = Date(System.currentTimeMillis() - (2L * 60L * 60L * 1000L))
        var claims = 0
        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                DailyRewardSectionCard(
                    userPrivate = UserPrivate(
                        userId = "u1",
                        streakDays = 3L,
                        lastClaimDayIndex = 3L,
                        lastDailyClaimAt = Timestamp(twoHoursAgo)
                    ),
                    isClaiming = false,
                    onClaim = { claims++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("daily_reward_countdown")
            .assertIsDisplayed()
            .assertTextContains("Next bonus in", substring = true)
        composeTestRule.onNodeWithTag("claim_daily_coins_button")
            .assertIsNotEnabled()
            .assertTextContains("Come back in", substring = true)
        assertEquals(0, claims)
    }

    @Test
    fun `levelProgressionCard displays tier xp progress and boost`() {
        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                LevelProgressionCard(
                    userPrivate = UserPrivate(
                        userId = "u1",
                        xp = 600L,
                        level = 3L,
                        activeBuffs = listOf("STARTER_XP_10", "XP_BOOST_20")
                    )
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("level_progression_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("level_progression_title")
            .assertIsDisplayed()
            .assertTextContains("LV 3 • Coin Tactician", substring = true)
        composeTestRule.onNodeWithTag("level_progression_xp")
            .assertIsDisplayed()
            .assertTextContains("100 / 250 XP", substring = true)
    }
}
