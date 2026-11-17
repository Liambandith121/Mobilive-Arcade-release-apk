package com.example

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.UserProfile
import com.example.data.formattedJoinDate
import com.example.data.unwrapFirestoreMessage
import com.example.ui.auth.GoogleSignInButton
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
}
