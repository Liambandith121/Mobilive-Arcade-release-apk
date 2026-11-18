package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.PartyInvite
import com.example.data.PartyRoom
import com.example.data.PlayerStatusOption
import com.example.data.UserProfile
import com.example.ui.components.FriendManagementCard
import com.example.ui.components.InviteFriendsToPartySheet
import com.example.ui.components.OnlineFriendsSummaryCard
import com.example.ui.components.PartyInviteBannerCard
import com.example.ui.theme.MobiliveArcadeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FriendPartyInviteRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `onlineFriendsSummaryCard displays counts and opens invite sheet`() {
        var inviteClicks = 0
        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                OnlineFriendsSummaryCard(
                    totalFriends = 5,
                    onlineFriends = 3,
                    lfgFriends = 2,
                    pendingPartyInvites = 1,
                    onInviteSquad = { inviteClicks++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("online_friends_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("online_friends_count_label")
            .assertIsDisplayed()
            .assertTextContains("3 of 5 online • 2 squad ready", substring = true)
        composeTestRule.onNodeWithTag("open_party_invite_sheet_button")
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, inviteClicks)
    }

    @Test
    fun `partyInviteBannerCard displays details and dispatches accept and decline`() {
        var accepts = 0
        var declines = 0
        val invite = PartyInvite(
            id = "inv_1",
            partyId = "party_1",
            partyName = "Night Raid Squad",
            gameTitle = "Minecraft",
            senderId = "friend_1",
            senderName = "Nova",
            receiverId = "me",
            receiverName = "Host",
            participantIds = listOf("friend_1", "me"),
            status = "pending"
        )

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                PartyInviteBannerCard(
                    invite = invite,
                    onAccept = { accepts++ },
                    onDecline = { declines++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("party_invite_card_inv_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nova invited you to a Party").assertIsDisplayed()
        composeTestRule.onNodeWithText("Night Raid Squad • Minecraft").assertIsDisplayed()

        composeTestRule.onNodeWithTag("accept_party_invite_inv_1").performClick()
        composeTestRule.onNodeWithTag("decline_party_invite_inv_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, accepts)
        assertEquals(1, declines)
    }

    @Test
    fun `friendManagementCard shows online status and triggers party invite message and remove`() {
        val friend = UserProfile(
            id = "u2",
            userId = "u2",
            handle = "blaze_runner",
            displayName = "Blaze Runner",
            currentGame = "Minecraft",
            playerStatus = PlayerStatusOption.IN_GAME.key,
            level = 7L
        )
        var invited = 0
        var messaged = 0
        var removed = 0

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                FriendManagementCard(
                    profile = friend,
                    isFollowing = true,
                    hasPendingPartyInvite = false,
                    onInviteToParty = { invited++ },
                    onMessage = { messaged++ },
                    onToggleFollow = {},
                    onViewProfile = {},
                    onRemoveFriend = { removed++ },
                    onReport = {},
                    onBlock = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("friend_card_u2")
            .assertIsDisplayed()
            .assertTextContains("In-Game • Minecraft", substring = true)
        composeTestRule.onNodeWithTag("friend_status_badge_u2", useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("friend_invite_party_u2").performClick()
        composeTestRule.onNodeWithTag("friend_message_u2").performClick()
        composeTestRule.onNodeWithTag("friend_menu_u2").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Remove Friend").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, invited)
        assertEquals(1, messaged)
        assertEquals(1, removed)
    }

    @Test
    fun `inviteFriendsToPartySheet filters online friends selects targets and sends invites`() {
        val onlineFriend = UserProfile(
            userId = "f_online",
            handle = "aria_craft",
            displayName = "Aria Craft",
            currentGame = "Minecraft",
            playerStatus = PlayerStatusOption.LFG.key
        )
        val awayFriend = UserProfile(
            userId = "f_away",
            handle = "shadow_afk",
            displayName = "Shadow AFK",
            currentGame = "Roblox",
            playerStatus = PlayerStatusOption.AWAY.key
        )
        val party = PartyRoom(
            id = "p_1",
            leaderId = "me",
            leaderName = "Host",
            partyName = "Weekend LAN",
            gameTitle = "Minecraft",
            maxSlots = 4L,
            memberIds = listOf("me"),
            partyStatus = "active"
        )

        var invitedPartyId = ""
        var invitedUserIds = emptyList<String>()

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                InviteFriendsToPartySheet(
                    friends = listOf(onlineFriend, awayFriend),
                    activeParties = listOf(party),
                    pendingSentInvites = emptyList(),
                    currentUserId = "me",
                    initialParty = party,
                    onDismiss = {},
                    onInviteToExistingParty = { targetParty, invitees ->
                        invitedPartyId = targetParty.id
                        invitedUserIds = invitees.map { it.userId }
                    },
                    onCreatePartyAndInvite = { _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("invite_friends_party_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("invite_friend_row_f_online")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("invite_friend_row_f_away")
            .performScrollTo()
            .assertIsDisplayed()

        // Filter to Online Only: Away friend should disappear
        composeTestRule.onNodeWithTag("invite_filter_online_only")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("invite_friend_row_f_away").assertDoesNotExist()

        // Select the online friend and send invite
        composeTestRule.onNodeWithTag("invite_friend_row_f_online")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("send_party_invites_button")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("p_1", invitedPartyId)
        assertEquals(listOf("f_online"), invitedUserIds)
    }

    @Test
    fun `inviteFriendsToPartySheet creates new party when user has no active parties`() {
        val friend = UserProfile(
            userId = "f_1",
            handle = "nova",
            displayName = "Nova",
            playerStatus = PlayerStatusOption.ONLINE.key
        )
        var createdPartyName = ""
        var createdInvitees = emptyList<String>()

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                InviteFriendsToPartySheet(
                    friends = listOf(friend),
                    activeParties = emptyList(),
                    pendingSentInvites = emptyList(),
                    currentUserId = "me",
                    initialFriend = friend,
                    onDismiss = {},
                    onInviteToExistingParty = { _, _ -> },
                    onCreatePartyAndInvite = { partyName, _, _, invitees ->
                        createdPartyName = partyName
                        createdInvitees = invitees.map { it.userId }
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("invite_new_party_name_input")
            .performScrollTo()
            .performTextReplacement("Night Raid Express")
        composeTestRule.onNodeWithTag("send_party_invites_button")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("Night Raid Express", createdPartyName)
        assertEquals(listOf("f_1"), createdInvitees)
    }
}
