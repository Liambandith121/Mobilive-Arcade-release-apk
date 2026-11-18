package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.MultiplayerWorld
import com.example.ui.screens.HostWorldDialog
import com.example.ui.screens.JoinWorldDialog
import com.example.ui.screens.WorldLobbyCard
import com.example.ui.theme.MobiliveArcadeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayScreenLanRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `worldLobbyCard hides lanAddress IP and port`() {
        val world = MultiplayerWorld(
            id = "w1",
            hostId = "h1",
            hostName = "Alex",
            worldName = "Sky Realm",
            gameName = "Minecraft",
            gameMode = "Survival",
            lanAddress = "192.168.1.45:19132",
            lanVisible = true,
            participantIds = listOf("h1")
        )
        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                WorldLobbyCard(
                    world = world,
                    isHost = false,
                    onJoinHere = {},
                    onCloseWorld = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Sky Realm").assertIsDisplayed()
        composeTestRule.onNodeWithText("LAN Visible").assertIsDisplayed()
        composeTestRule.onNodeWithText("192.168.1.45:19132", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("LAN:", substring = true).assertDoesNotExist()
    }

    @Test
    fun `joinWorldDialog has no address IP port or copy button`() {
        val world = MultiplayerWorld(
            id = "w2",
            hostId = "h1",
            hostName = "Steve",
            worldName = "Redstone Arena",
            gameName = "Minecraft",
            gameMode = "Creative",
            lanAddress = "192.168.1.77:19132",
            participantIds = listOf("h1", "p2"),
            maxPlayers = 8L
        )
        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                JoinWorldDialog(
                    world = world,
                    onDismiss = {},
                    onOpenMinecraft = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Host: Steve").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_minecraft_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("LAN Address", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("192.168.1.77", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("19132", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Copy LAN IP").assertDoesNotExist()
    }

    @Test
    fun `hostWorldDialog omits IP and port input and submits world`() {
        var hostedName = ""
        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                HostWorldDialog(
                    onDismiss = {},
                    onHost = { name, _, _, _ -> hostedName = name }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("host_world_name_input").assertIsDisplayed()
        composeTestRule.onNodeWithText("LAN IP : Port").assertDoesNotExist()
        composeTestRule.onNodeWithText("Detecting local LAN IP...", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("confirm_host_world_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Diamond Sky Realm", hostedName)
    }
}
