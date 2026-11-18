package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.Tournament
import com.example.data.TournamentRegistration
import com.example.data.UserPrivate
import com.example.data.UserProfile
import com.example.data.countdownStatus
import com.example.ui.components.TournamentCard
import com.example.ui.components.TournamentCountdownStrip
import com.example.ui.components.TournamentRegistrationSheet
import com.example.ui.components.TournamentSpotlightCard
import com.example.ui.theme.MobiliveArcadeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TournamentRegistrationRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tournamentCountdownStrip renders digital tiles and starting soon banner`() {
        val now = 1_700_000_000_000L
        val upcomingSoon = Tournament(
            id = "t_soon",
            tournamentState = "upcoming",
            startsAtMillis = now + ((25L * 60L) + 14L) * 1000L
        ).countdownStatus(now)

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                TournamentCountdownStrip(
                    countdown = upcomingSoon,
                    compact = false,
                    tagPrefix = "test_soon"
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("test_soon_countdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("STARTING SOON").assertIsDisplayed()
        composeTestRule.onNodeWithText("25m 14s").assertIsDisplayed()
        composeTestRule.onNodeWithText("HRS").assertIsDisplayed()
        composeTestRule.onNodeWithText("MIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("SEC").assertIsDisplayed()
    }

    @Test
    fun `tournamentSpotlightCard renders countdown and opens registration sheet`() {
        val now = 1_700_000_000_000L
        val tournament = Tournament(
            id = "t_spot",
            organizerId = "org_1",
            title = "Cyber Arena Grand Prix",
            gameTitle = "Valorant Mobile",
            prizeCoins = 2500L,
            entryCoins = 0L,
            maxTeams = 16L,
            tournamentState = "upcoming",
            registeredIds = listOf("org_1", "u_2"),
            scheduleLabel = "In 1 day",
            startsAtMillis = now + ((1L * 86400L) + (4L * 3600L)) * 1000L,
            formatLabel = "Squad 4v4"
        )
        var clicks = 0

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                TournamentSpotlightCard(
                    tournament = tournament,
                    nowMillis = now,
                    isRegistered = false,
                    onOpenRegistration = { clicks++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tournament_spotlight_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cyber Arena Grand Prix").assertIsDisplayed()
        composeTestRule.onNodeWithTag("spotlight_tournament_countdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("DAYS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("spotlight_register_button")
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, clicks)
    }

    @Test
    fun `tournamentCard displays countdown team fill and triggers registration callback`() {
        val now = 1_700_000_000_000L
        val tournament = Tournament(
            id = "t_card",
            organizerId = "org_1",
            title = "Weekend Bedwars Qualifier",
            gameTitle = "Minecraft",
            prizeCoins = 1000L,
            entryCoins = 50L,
            maxTeams = 16L,
            tournamentState = "upcoming",
            registeredIds = listOf("org_1"),
            scheduleLabel = "In 2 hours",
            startsAtMillis = now + (2L * 3600_000L),
            formatLabel = "Duo 2v2"
        )
        var opened = 0

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                TournamentCard(
                    tournament = tournament,
                    nowMillis = now,
                    isRegistered = false,
                    onOpenRegistration = { opened++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tournament_card_t_card")
            .assertIsDisplayed()
            .assertTextContains("Weekend Bedwars Qualifier", substring = true)
            .assertTextContains("1 / 16 Teams Registered", substring = true)
            .assertTextContains("Entry: 50 Coins", substring = true)
        composeTestRule.onNodeWithTag("tournament_t_card_countdown", useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("tournament_register_button_t_card", useUnmergedTree = true)
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, opened)
    }

    @Test
    fun `tournamentRegistrationSheet validates balance and submits team signup`() {
        val now = 1_700_000_000_000L
        val tournament = Tournament(
            id = "t_reg",
            organizerId = "org_1",
            title = "Solaris Cup",
            gameTitle = "Rocket League Sideswipe",
            prizeCoins = 1500L,
            entryCoins = 100L,
            maxTeams = 16L,
            tournamentState = "upcoming",
            registeredIds = listOf("org_1"),
            scheduleLabel = "In 3 hours",
            startsAtMillis = now + (3L * 3600_000L),
            formatLabel = "Duo 2v2",
            rulesSummary = "Best of 3 elimination"
        )
        var submittedTeam = ""
        var submittedTag = ""
        var submittedDiv = ""

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                TournamentRegistrationSheet(
                    tournament = tournament,
                    nowMillis = now,
                    currentUserId = "me_1",
                    userPrivate = UserPrivate(userId = "me_1", coins = 500L),
                    myProfile = UserProfile(userId = "me_1", displayName = "Nova", handle = "nova_99"),
                    registrations = emptyList(),
                    isSubmitting = false,
                    onDismiss = {},
                    onConfirmRegister = { team, tag, div ->
                        submittedTeam = team
                        submittedTag = tag
                        submittedDiv = div
                    },
                    onWithdrawRegistration = {},
                    onUpdateTournamentState = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tournament_registration_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sheet_tournament_countdown").assertIsDisplayed()

        composeTestRule.onNodeWithTag("registration_team_name_input")
            .performScrollTo()
            .performTextReplacement("Crimson Boosters")
        composeTestRule.onNodeWithTag("registration_gamer_tag_input")
            .performScrollTo()
            .performTextReplacement("Nova#777")
        composeTestRule.onNodeWithTag("registration_division_solo")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithTag("confirm_tournament_registration_button")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("Crimson Boosters", submittedTeam)
        assertEquals("Nova#777", submittedTag)
        assertEquals("Solo", submittedDiv)
    }

    @Test
    fun `tournamentRegistrationSheet blocks signup when entry coins exceed user coins`() {
        val now = 1_700_000_000_000L
        val tournament = Tournament(
            id = "t_paid",
            organizerId = "org_1",
            title = "High Roller Open",
            gameTitle = "PUBG Mobile",
            prizeCoins = 5000L,
            entryCoins = 250L,
            maxTeams = 16L,
            tournamentState = "upcoming",
            registeredIds = listOf("org_1"),
            startsAtMillis = now + 7200_000L
        )

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                TournamentRegistrationSheet(
                    tournament = tournament,
                    nowMillis = now,
                    currentUserId = "me_1",
                    userPrivate = UserPrivate(userId = "me_1", coins = 50L),
                    myProfile = UserProfile(userId = "me_1", displayName = "Nova", handle = "nova_99"),
                    registrations = emptyList(),
                    isSubmitting = false,
                    onDismiss = {},
                    onConfirmRegister = { _, _, _ -> },
                    onWithdrawRegistration = {},
                    onUpdateTournamentState = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Insufficient Gift Coins: need 250 Coins (you have 50).")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_tournament_registration_button")
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun `tournamentRegistrationSheet shows contender pass roster and supports withdrawal`() {
        val now = 1_700_000_000_000L
        val tournament = Tournament(
            id = "t_pass",
            organizerId = "org_1",
            title = "Midnight Legends",
            gameTitle = "Minecraft",
            prizeCoins = 1200L,
            entryCoins = 100L,
            maxTeams = 16L,
            tournamentState = "upcoming",
            registeredIds = listOf("org_1", "me_1"),
            startsAtMillis = now + 5400_000L
        )
        val reg = TournamentRegistration(
            id = "me_1",
            tournamentId = "t_pass",
            userId = "me_1",
            playerName = "Nova",
            teamName = "Nova Knights",
            gamerTag = "@nova_99",
            division = "Duo",
            agreedRules = true,
            entryPaidCoins = 100L
        )
        var refundedCoins = -1L

        composeTestRule.setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                TournamentRegistrationSheet(
                    tournament = tournament,
                    nowMillis = now,
                    currentUserId = "me_1",
                    userPrivate = UserPrivate(userId = "me_1", coins = 400L),
                    myProfile = UserProfile(userId = "me_1", displayName = "Nova", handle = "nova_99"),
                    registrations = listOf(reg),
                    isSubmitting = false,
                    onDismiss = {},
                    onConfirmRegister = { _, _, _ -> },
                    onWithdrawRegistration = { paid -> refundedCoins = paid },
                    onUpdateTournamentState = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tournament_registered_pass_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmed Contender Pass").assertIsDisplayed()
        composeTestRule.onNodeWithText("Team: Nova Knights (Duo)").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tournament_contenders_section")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("withdraw_tournament_registration_button")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(100L, refundedCoins)
    }
}
