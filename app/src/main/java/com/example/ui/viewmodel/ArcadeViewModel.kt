package com.example.ui.viewmodel

import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArcadeViewModel(
    private val repository: ArcadeRepository,
    val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _levelUpBanner = MutableStateFlow<Long?>(null)
    val levelUpBanner: StateFlow<Long?> = _levelUpBanner.asStateFlow()

    private val _isClaimingDailyReward = MutableStateFlow(false)
    val isClaimingDailyReward: StateFlow<Boolean> = _isClaimingDailyReward.asStateFlow()

    private val _isSubmittingTournamentRegistration = MutableStateFlow(false)
    val isSubmittingTournamentRegistration: StateFlow<Boolean> = _isSubmittingTournamentRegistration.asStateFlow()

    private val _selectedStreamId = MutableStateFlow<String?>(null)
    private val _selectedConversationId = MutableStateFlow<String?>(null)
    private val _selectedPostForComments = MutableStateFlow<SocialPost?>(null)
    private val _selectedTournamentId = MutableStateFlow<String?>(null)

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing $label", root)
                emit(UiState.Error(root.unwrapFirestoreMessage("Failed to load $label")))
            }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val userPrivate: StateFlow<UiState<UserPrivate?>> =
        repository.observeUserPrivate(currentUserId).toUiState("user settings")

    val myProfile: StateFlow<UiState<PublicProfile?>> =
        repository.observePublicProfile(currentUserId).toUiState("profile")

    val allProfiles: StateFlow<UiState<List<PublicProfile>>> =
        repository.observePublicProfiles().toUiState("players")

    val posts: StateFlow<UiState<List<SocialPost>>> =
        repository.observePublicPosts().toUiState("posts")

    val streams: StateFlow<UiState<List<LiveStream>>> =
        repository.observeStreams().toUiState("streams")

    val worlds: StateFlow<UiState<List<MultiplayerWorld>>> =
        repository.observeWorlds().toUiState("LAN worlds")

    val parties: StateFlow<UiState<List<PartyRoom>>> =
        repository.observeParties().toUiState("parties")

    val communities: StateFlow<UiState<List<GameCommunity>>> =
        repository.observeCommunities().toUiState("communities")

    val tournaments: StateFlow<UiState<List<Tournament>>> =
        repository.observeTournaments().toUiState("tournaments")

    val friendRequests: StateFlow<UiState<List<FriendRequest>>> =
        repository.observeFriendRequests(currentUserId).toUiState("friend requests")

    val partyInvites: StateFlow<UiState<List<PartyInvite>>> =
        repository.observePartyInvites(currentUserId).toUiState("party invites")

    val conversations: StateFlow<UiState<List<Conversation>>> =
        repository.observeConversations(currentUserId).toUiState("conversations")

    val gallery: StateFlow<UiState<List<GalleryMedia>>> =
        repository.observeGallery(currentUserId).toUiState("gallery")

    val notifications: StateFlow<UiState<List<UserNotification>>> =
        repository.observeNotifications(currentUserId).toUiState("notifications")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedStreamChat: StateFlow<UiState<List<StreamChatMessage>>> =
        _selectedStreamId
            .flatMapLatest { id ->
                if (id.isNullOrBlank()) flowOf(emptyList())
                else repository.observeStreamChat(id)
            }
            .toUiState("stream chat")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedMessages: StateFlow<UiState<List<ChatMessage>>> =
        _selectedConversationId
            .flatMapLatest { id ->
                if (id.isNullOrBlank()) flowOf(emptyList())
                else repository.observeMessages(id, currentUserId)
            }
            .toUiState("messages")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedPostComments: StateFlow<UiState<List<PostComment>>> =
        _selectedPostForComments
            .flatMapLatest { post ->
                if (post == null) flowOf(emptyList())
                else repository.observeComments(post.id)
            }
            .toUiState("comments")

    val postForComments: StateFlow<SocialPost?> = _selectedPostForComments.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedTournamentRegistrations: StateFlow<UiState<List<TournamentRegistration>>> =
        _selectedTournamentId
            .flatMapLatest { id ->
                if (id.isNullOrBlank()) flowOf(emptyList())
                else repository.observeTournamentRegistrations(id)
            }
            .toUiState("tournament registrations")

    init {
        viewModelScope.launch {
            val firebaseUser = Firebase.auth.currentUser
            if (firebaseUser != null) {
                val defaultHandle = (firebaseUser.displayName ?: "player")
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]"), "")
                    .ifBlank { "arcade_${firebaseUser.uid.take(5)}" }
                repository.ensureUserInitialized(
                    displayName = firebaseUser.displayName ?: "Arcade Player",
                    email = firebaseUser.email ?: "",
                    handle = defaultHandle,
                    avatarStyle = "cyber_visor",
                    avatarPhotoUri = firebaseUser.photoUrl?.toString().orEmpty()
                )
            }
        }
    }

    fun dismissLevelUpBanner() {
        _levelUpBanner.value = null
    }

    fun showBannerMessage(message: String) {
        _events.tryEmit(message)
    }

    fun selectStream(streamId: String?) {
        _selectedStreamId.value = streamId
    }

    fun selectConversation(conversationId: String?) {
        _selectedConversationId.value = conversationId
    }

    fun openPostComments(post: SocialPost?) {
        _selectedPostForComments.value = post
    }

    fun completeOnboarding(
        displayName: String,
        handle: String,
        bio: String,
        avatarStyle: String,
        avatarPhotoUri: String,
        currentGame: String
    ) {
        viewModelScope.launch {
            val res = repository.updatePublicProfile(displayName, handle, bio, avatarStyle, avatarPhotoUri, currentGame)
            emitResult(res, "Profile updated!")
        }
    }

    fun updatePlayerStatus(status: String) {
        val option = PlayerStatusOption.fromKey(status)
        viewModelScope.launch {
            emitResult(
                repository.updatePlayerStatus(option.key),
                "Profile status set to ${option.label}"
            )
        }
    }

    fun claimDailyReward() {
        val priv = (userPrivate.value as? UiState.Success)?.data ?: return
        if (_isClaimingDailyReward.value) return
        val status = priv.dailyRewardStatus()
        if (!status.canClaim) {
            _events.tryEmit("Daily bonus already claimed! Next bonus in ${status.formattedRemaining}.")
            return
        }
        viewModelScope.launch {
            _isClaimingDailyReward.value = true
            try {
                repository.claimDailyReward(priv)
                    .onSuccess { reward ->
                        val newLevel = ArcadeCatalog.calculateLevel(priv.xp + reward.xp)
                        if (newLevel > priv.level) _levelUpBanner.value = newLevel
                        _events.tryEmit("Day ${reward.day} claimed: +${reward.coins} Coins & +${reward.xp} XP! Next bonus in 24h.")
                    }
                    .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Could not claim daily reward")) }
            } finally {
                _isClaimingDailyReward.value = false
            }
        }
    }

    fun claimAchievement(ach: AchievementDef) {
        val priv = (userPrivate.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = repository.claimAchievement(priv, ach)
            res.onSuccess { gain ->
                if (gain?.leveledUp == true) {
                    _levelUpBanner.value = gain.newLevel
                }
                _events.tryEmit("Badge unlocked: ${ach.badgeTitle} (+${ach.rewardCoins} Coins)!")
            }.onFailure {
                _events.tryEmit(it.unwrapFirestoreMessage("Could not claim badge"))
            }
        }
    }

    fun createPost(postType: String, gameTag: String, caption: String, mediaUri: String) {
        val prof = (myProfile.value as? UiState.Success)?.data ?: return
        val priv = (userPrivate.value as? UiState.Success)?.data
        viewModelScope.launch {
            val res = repository.createPost(prof, postType, gameTag, caption, mediaUri)
            if (res.isSuccess && priv != null) {
                val gain = repository.grantActivityXp(priv, 35L, 15L).getOrNull()
                if (gain?.leveledUp == true) _levelUpBanner.value = gain.newLevel
            }
            emitResult(res, "$postType published (+35 XP)!")
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            emitResult(repository.deletePost(postId), "Deleted from feed")
        }
    }

    fun toggleLike(post: SocialPost) {
        viewModelScope.launch {
            repository.toggleLike(post)
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Like failed")) }
        }
    }

    fun sharePost(post: SocialPost) {
        viewModelScope.launch {
            repository.incrementShare(post)
        }
    }

    fun addComment(text: String) {
        val post = _selectedPostForComments.value ?: return
        val prof = (myProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            emitResult(repository.addComment(post, prof.displayName, text), "Comment posted")
        }
    }

    fun startLiveStream(title: String, gameTitle: String, recordReplay: Boolean, onStarted: (String) -> Unit) {
        val prof = (myProfile.value as? UiState.Success)?.data ?: return
        val priv = (userPrivate.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.startLiveStream(prof, title, gameTitle, recordReplay)
                .onSuccess { streamId ->
                    if (priv != null) {
                        val gain = repository.grantActivityXp(priv, 80L, 40L).getOrNull()
                        if (gain?.leveledUp == true) _levelUpBanner.value = gain.newLevel
                    }
                    _events.tryEmit("You are LIVE (+80 XP)!")
                    onStarted(streamId)
                }
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Failed to go live")) }
        }
    }

    fun updateStreamControls(stream: LiveStream, micEnabled: Boolean, slowModeSec: Long, recordReplay: Boolean, title: String) {
        viewModelScope.launch {
            emitResult(repository.updateStreamControls(stream, micEnabled, slowModeSec, recordReplay, title), "Stream controls updated")
        }
    }

    fun endLiveStream(stream: LiveStream, elapsedSec: Long) {
        viewModelScope.launch {
            emitResult(
                repository.endLiveStream(stream, elapsedSec),
                if (stream.recordReplay) "Stream ended & saved to Replay!" else "Stream ended"
            )
        }
    }

    fun sendStreamChat(streamId: String, text: String, messageType: String = "chat") {
        val prof = (myProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.sendStreamChat(streamId, prof.displayName, text, messageType)
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Failed to send chat")) }
        }
    }

    fun sendGiftCoins(stream: LiveStream, amount: Long) {
        val priv = (userPrivate.value as? UiState.Success)?.data ?: return
        val prof = (myProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            emitResult(repository.sendGiftCoinsToStream(priv, stream, prof.displayName, amount), "Sent $amount Gift Coins!")
        }
    }

    fun deleteStreamChat(streamId: String, msgId: String) {
        viewModelScope.launch {
            emitResult(repository.deleteStreamChat(streamId, msgId), "Chat message removed")
        }
    }

    fun hostWorld(worldName: String, gameName: String, gameMode: String, maxPlayers: Long) {
        val prof = (myProfile.value as? UiState.Success)?.data ?: return
        val priv = (userPrivate.value as? UiState.Success)?.data
        viewModelScope.launch {
            val res = repository.hostWorld(prof, worldName, gameName, gameMode, maxPlayers)
            if (res.isSuccess && priv != null) {
                val gain = repository.grantActivityXp(priv, 45L, 10L).getOrNull()
                if (gain?.leveledUp == true) _levelUpBanner.value = gain.newLevel
            }
            emitResult(res, "LAN World is now Open (+45 XP)!")
        }
    }

    fun joinWorld(world: MultiplayerWorld) {
        viewModelScope.launch {
            emitResult(repository.joinWorld(world), "Joined ${world.worldName}! Open Minecraft > Play > Worlds.")
        }
    }

    fun closeWorld(worldId: String) {
        viewModelScope.launch {
            emitResult(repository.closeWorld(worldId), "World closed")
        }
    }

    fun createParty(partyName: String, gameTitle: String, maxSlots: Long) {
        val prof = (myProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            emitResult(repository.createParty(prof, partyName, gameTitle, maxSlots), "Party Voice Room created!")
        }
    }

    fun joinOrLeaveParty(party: PartyRoom) {
        viewModelScope.launch {
            val leaving = party.memberIds.contains(currentUserId)
            emitResult(repository.joinOrLeaveParty(party), if (leaving) "Left party" else "Joined party voice!")
        }
    }

    fun togglePartyMicMute(party: PartyRoom) {
        viewModelScope.launch {
            repository.togglePartyMicMute(party)
                .onSuccess { muted -> _events.tryEmit(if (muted) "Mic muted" else "Mic live in party voice") }
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Mic update failed")) }
        }
    }

    fun toggleMatchmaking(party: PartyRoom) {
        viewModelScope.launch {
            val nextState = when (party.queueState) {
                "idle" -> "searching"
                "searching" -> "matched"
                else -> "idle"
            }
            val code = if (nextState == "matched") "MBL-${(1000..9999).random()}" else ""
            emitResult(
                repository.updateMatchmakingState(party, nextState, code),
                when (nextState) {
                    "searching" -> "Searching matchmaking queue..."
                    "matched" -> "Match found! Lobby $code"
                    else -> "Matchmaking cancelled"
                }
            )
        }
    }

    fun createCommunity(name: String, gameCategory: String, description: String) {
        viewModelScope.launch {
            emitResult(repository.createCommunity(name, gameCategory, description), "Community launched!")
        }
    }

    fun toggleJoinCommunity(community: GameCommunity) {
        viewModelScope.launch {
            repository.toggleJoinCommunity(community)
                .onSuccess { joined -> _events.tryEmit(if (joined) "Joined ${community.name}" else "Left ${community.name}") }
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Community update failed")) }
        }
    }

    fun selectTournament(tournamentId: String?) {
        _selectedTournamentId.value = tournamentId
    }

    fun createTournament(
        title: String,
        gameTitle: String,
        prizeCoins: Long,
        entryCoins: Long,
        maxTeams: Long,
        schedule: String,
        startsAtMillis: Long = 0L,
        formatLabel: String = "Squad 4v4",
        rulesSummary: String = ""
    ) {
        viewModelScope.launch {
            emitResult(
                repository.createTournament(
                    title = title,
                    gameTitle = gameTitle,
                    prizeCoins = prizeCoins,
                    entryCoins = entryCoins,
                    maxTeams = maxTeams,
                    scheduleLabel = schedule,
                    startsAtMillis = startsAtMillis,
                    formatLabel = formatLabel,
                    rulesSummary = rulesSummary
                ),
                "Tournament created!"
            )
        }
    }

    fun registerForTournament(
        tournament: Tournament,
        teamName: String,
        gamerTag: String,
        division: String,
        onComplete: () -> Unit = {}
    ) {
        if (_isSubmittingTournamentRegistration.value) return
        val priv = (userPrivate.value as? UiState.Success)?.data
        val prof = (myProfile.value as? UiState.Success)?.data
        viewModelScope.launch {
            _isSubmittingTournamentRegistration.value = true
            try {
                repository.registerForTournament(
                    tournament = tournament,
                    userPrivate = priv,
                    profile = prof,
                    teamName = teamName,
                    gamerTag = gamerTag,
                    division = division
                ).onSuccess {
                    if (priv != null) {
                        val updatedCoinsPriv = priv.copy(
                            coins = (priv.coins - tournament.entryCoins.coerceAtLeast(0L)).coerceAtLeast(0L)
                        )
                        repository.grantActivityXp(updatedCoinsPriv, 25L)
                            .onSuccess { gain ->
                                if (gain.leveledUp) _levelUpBanner.value = gain.newLevel
                            }
                    }
                    _events.tryEmit("Registered for ${tournament.title}! (+25 XP)")
                    onComplete()
                }.onFailure {
                    _events.tryEmit(it.unwrapFirestoreMessage("Tournament registration failed"))
                }
            } finally {
                _isSubmittingTournamentRegistration.value = false
            }
        }
    }

    fun withdrawTournamentRegistration(
        tournament: Tournament,
        entryPaidCoins: Long,
        onComplete: () -> Unit = {}
    ) {
        if (_isSubmittingTournamentRegistration.value) return
        val priv = (userPrivate.value as? UiState.Success)?.data
        viewModelScope.launch {
            _isSubmittingTournamentRegistration.value = true
            try {
                repository.withdrawTournamentRegistration(tournament, priv, entryPaidCoins)
                    .onSuccess {
                        _events.tryEmit("Withdrew registration from ${tournament.title}")
                        onComplete()
                    }
                    .onFailure {
                        _events.tryEmit(it.unwrapFirestoreMessage("Withdrawal failed"))
                    }
            } finally {
                _isSubmittingTournamentRegistration.value = false
            }
        }
    }

    fun updateTournamentState(tournamentId: String, nextState: String) {
        viewModelScope.launch {
            emitResult(
                repository.updateTournamentState(tournamentId, nextState),
                if (nextState == "active") "Tournament is now LIVE!" else "Tournament marked completed"
            )
        }
    }

    fun toggleRegisterTournament(tournament: Tournament) {
        viewModelScope.launch {
            repository.toggleRegisterTournament(tournament)
                .onSuccess { joined -> _events.tryEmit(if (joined) "Registered for ${tournament.title}!" else "Registration cancelled") }
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Registration failed")) }
        }
    }

    fun sendFriendRequest(receiver: PublicProfile) {
        val prof = (myProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            emitResult(repository.sendFriendRequest(prof, receiver), "Friend request sent to ${receiver.displayName}")
        }
    }

    fun respondFriendRequest(requestId: String, accept: Boolean) {
        viewModelScope.launch {
            emitResult(repository.respondFriendRequest(requestId, accept), if (accept) "Friend added!" else "Request declined")
        }
    }

    fun removeFriend(requestId: String, friendName: String) {
        viewModelScope.launch {
            emitResult(repository.removeFriendRequest(requestId), "Removed $friendName from friends")
        }
    }

    fun cancelFriendRequest(requestId: String, receiverName: String) {
        viewModelScope.launch {
            emitResult(repository.removeFriendRequest(requestId), "Cancelled request to $receiverName")
        }
    }

    fun inviteFriendsToParty(friends: List<PublicProfile>, party: PartyRoom) {
        val prof = (myProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.sendPartyInvites(prof, friends, party)
                .onSuccess { count ->
                    val targetMsg = if (count == 1) friends.firstOrNull()?.displayName ?: "1 friend" else "$count friends"
                    _events.tryEmit("Invited $targetMsg to ${party.partyName}!")
                }
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Could not send party invite")) }
        }
    }

    fun createPartyAndInviteFriends(
        partyName: String,
        gameTitle: String,
        maxSlots: Long,
        invitees: List<PublicProfile>,
        onCreated: () -> Unit = {}
    ) {
        val prof = (myProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.createPartyAndInviteFriends(prof, partyName, gameTitle, maxSlots, invitees)
                .onSuccess {
                    val count = invitees.size
                    val msg = if (count > 0) {
                        "Created $partyName and invited $count friend${if (count == 1) "" else "s"}!"
                    } else {
                        "Party Voice Room created!"
                    }
                    _events.tryEmit(msg)
                    onCreated()
                }
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Failed to create party")) }
        }
    }

    fun respondPartyInvite(invite: PartyInvite, accept: Boolean, onAccepted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.respondPartyInvite(invite, accept)
                .onSuccess {
                    if (accept) {
                        _events.tryEmit("Joined ${invite.partyName}!")
                        onAccepted()
                    } else {
                        _events.tryEmit("Declined invite to ${invite.partyName}")
                    }
                }
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Could not respond to party invite")) }
        }
    }

    fun cancelPartyInvite(inviteId: String) {
        viewModelScope.launch {
            emitResult(repository.cancelPartyInvite(inviteId), "Party invite cancelled")
        }
    }

    fun toggleFollow(target: PublicProfile) {
        val priv = (userPrivate.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.toggleFollow(priv, target)
                .onSuccess { nowFollowing -> _events.tryEmit(if (nowFollowing) "Following ${target.displayName}" else "Unfollowed ${target.displayName}") }
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Follow failed")) }
        }
    }

    fun createConversation(title: String, groupChat: Boolean, participants: List<PublicProfile>, onCreated: (String) -> Unit) {
        val prof = (myProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val ids = participants.map { it.userId }
            val labels = (participants.map { it.displayName } + prof.displayName).distinct()
            repository.createConversation(title, groupChat, ids, labels)
                .onSuccess { convId -> onCreated(convId) }
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Could not start chat")) }
        }
    }

    fun sendMessage(conversation: Conversation, text: String) {
        val prof = (myProfile.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.sendMessage(conversation, prof.displayName, text)
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Message failed")) }
        }
    }

    fun addGalleryMedia(title: String, mediaType: String, gameTitle: String, mediaUri: String, durationSec: Long) {
        viewModelScope.launch {
            emitResult(repository.addGalleryMedia(title, mediaType, gameTitle, mediaUri, durationSec), "Saved to Arcade Gallery!")
        }
    }

    fun deleteGalleryMedia(mediaId: String) {
        viewModelScope.launch {
            emitResult(repository.deleteGalleryMedia(mediaId), "Removed from Gallery")
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notifId)
        }
    }

    fun updatePrivacySettings(dmPolicy: String, showOnline: Boolean, lanVisible: Boolean, autoSaveReplays: Boolean) {
        viewModelScope.launch {
            emitResult(repository.updatePrivacySettings(dmPolicy, showOnline, lanVisible, autoSaveReplays), "Privacy & Settings saved")
        }
    }

    fun toggleBlockUser(targetUserId: String, handle: String) {
        val priv = (userPrivate.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.toggleBlockUser(priv, targetUserId)
                .onSuccess { blocked -> _events.tryEmit(if (blocked) "Blocked @$handle" else "Unblocked @$handle") }
                .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Block update failed")) }
        }
    }

    fun submitReport(targetUserId: String, targetHandle: String, reason: String, details: String) {
        viewModelScope.launch {
            emitResult(repository.submitReport(targetUserId, targetHandle, reason, details), "Report submitted to Moderation")
        }
    }

    fun signOut(credentialManager: CredentialManager) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                }
            }
        }
    }

    private fun emitResult(result: Result<Unit>, successMsg: String) {
        result
            .onSuccess { _events.tryEmit(successMsg) }
            .onFailure { _events.tryEmit(it.unwrapFirestoreMessage("Action failed")) }
    }

    companion object {
        private const val TAG = "ArcadeVM"
        private const val STOP_TIMEOUT_MS = 5000L

        fun factory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ArcadeViewModel(ArcadeRepository(db), currentUserId)
            }
        }
    }
}
