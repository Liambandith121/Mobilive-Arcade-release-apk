package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.LiveStream
import com.example.ui.components.FloatingActionMenuPanel
import com.example.ui.components.FloatingLiveStreamMiniPlayer
import com.example.ui.theme.MobiliveArcadeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FloatingOverlayRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `floatingActionMenuPanel shows Message and Go Live Stream and triggers clicks`() {
        var messageClicked = false
        var goLiveClicked = false

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                FloatingActionMenuPanel(
                    isCurrentlyLive = false,
                    onMessageClick = { messageClicked = true },
                    onGoLiveClick = { goLiveClicked = true },
                    onCollapse = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("floating_action_menu").assertIsDisplayed()
        composeTestRule.onNodeWithText("Message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Go Live Stream").assertIsDisplayed()

        composeTestRule.onNodeWithTag("floating_message_button").performClick()
        composeTestRule.onNodeWithTag("floating_go_live_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(messageClicked)
        assertTrue(goLiveClicked)
    }

    @Test
    fun `floatingActionMenuPanel switches to Live Controls when already live`() {
        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                FloatingActionMenuPanel(
                    isCurrentlyLive = true,
                    onMessageClick = {},
                    onGoLiveClick = {},
                    onCollapse = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Live Controls").assertIsDisplayed()
        composeTestRule.onNodeWithText("Message").assertIsDisplayed()
    }

    @Test
    fun `floatingLiveStreamMiniPlayer renders host stream and dispatches manage and expand`() {
        var expanded = false
        var managed = false
        val stream = LiveStream(
            id = "stream_1",
            hostId = "user_1",
            hostName = "NovaGamer",
            title = "Ranked Bedwars Push",
            gameTitle = "Minecraft",
            streamStatus = "live",
            viewerCount = 24L,
            totalGiftsCoins = 350L,
            durationSec = 95L
        )

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                FloatingLiveStreamMiniPlayer(
                    stream = stream,
                    isHost = true,
                    onExpand = { expanded = true },
                    onManage = { managed = true },
                    onClose = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("floating_live_mini_player").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ranked Bedwars Push").assertIsDisplayed()
        composeTestRule.onNodeWithText("NovaGamer • 24 viewers • 350 Coins").assertIsDisplayed()

        composeTestRule.onNodeWithTag("floating_stream_manage_button").performClick()
        composeTestRule.onNodeWithTag("floating_stream_expand_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(managed)
        assertTrue(expanded)
    }
}
