package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

class ArcadeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Flow<T>.logFirestoreErrors(op: OperationType, path: String, uid: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(exceptionToLog, op, path)
            }
            throw error
        }

    // --- User Initialization & Profile ---

    suspend fun ensureUserInitialized(
        displayName: String,
        email: String,
        handle: String,
        avatarStyle: String,
        avatarPhotoUri: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val userPath = "users/$uid"
        val profilePath = "public_profiles/$uid"
        return try {
            val userRef = db.collection("users").document(uid)
            val profileRef = db.collection("public_profiles").document(uid)
            val userSnap = userRef.get().await()
            val profileSnap = profileRef.get().await()
            val cleanHandle = handle.trim().ifBlank { "player_${uid.take(6)}" }.take(30)
            val cleanName = displayName.trim().ifBlank { "Arcade Player" }.take(50)

            val batch = db.batch()
            if (!userSnap.exists()) {
                val userPayload = mapOf(
                    "userId" to uid,
                    "email" to email.take(200),
                    "coins" to 500L,
                    "xp" to 50L,
                    "level" to 1L,
                    "streakDays" to 0L,
                    "lastClaimDayIndex" to 0L,
                    "activeBuffs" to listOf("STARTER_XP_10"),
                    "privacyDmPolicy" to "everyone",
                    "privacyShowOnline" to true,
                    "privacyLanVisible" to true,
                    "autoSaveReplays" to true,
                    "blockedUserIds" to emptyList<String>(),
                    "followingUserIds" to emptyList<String>(),
                    "joinedCommunityIds" to emptyList<String>(),
                    "claimedAchievementIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(userRef, userPayload)
            }
            if (!profileSnap.exists()) {
                val profilePayload = mapOf(
                    "userId" to uid,
                    "handle" to cleanHandle,
                    "displayName" to cleanName,
                    "bio" to "Live on Mobilive Arcade • Ready for co-op & LAN worlds!",
                    "avatarStyle" to avatarStyle.ifBlank { "cyber_visor" },
                    "avatarPhotoUri" to avatarPhotoUri.take(500),
                    "level" to 1L,
                    "xp" to 50L,
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "verifiedBadge" to false,
                    "liveNow" to false,
                    "searchable" to true,
                    "currentGame" to "Minecraft",
                    "playerStatus" to "online",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(profileRef, profilePayload)
            }
            if (!userSnap.exists()) {
                val welcomeNotif = userRef.collection("notifications").document()
                batch.set(
                    welcomeNotif,
                    mapOf(
                        "userId" to uid,
                        "title" to "Welcome to Mobilive Arcade!",
                        "body" to "You received 500 starter Gift Coins. Claim Day 1 in Rewards!",
                        "notifType" to "reward",
                        "readFlag" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$userPath / $profilePath")
            Result.failure(e)
        }
    }

    fun observeUserPrivate(uid: String): Flow<UserPrivate?> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap -> snap.toObject(UserPrivate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.GET, "users/$uid", uid)
        )
    }

    fun observePublicProfile(uid: String): Flow<PublicProfile?> = flow {
        val currentUid = requireUserId()
        emitAll(
            db.collection("public_profiles").document(uid)
                .snapshots()
                .map { snap -> snap.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.GET, "public_profiles/$uid", currentUid)
        )
    }

    fun observePublicProfiles(): Flow<List<PublicProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("public_profiles")
                .whereEqualTo("searchable", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.level }
                }
                .logFirestoreErrors(OperationType.LIST, "public_profiles", uid)
        )
    }

    suspend fun updatePublicProfile(
        displayName: String,
        handle: String,
        bio: String,
        avatarStyle: String,
        avatarPhotoUri: String,
        currentGame: String
    ): Result<Unit> = runMutation(OperationType.UPDATE, "public_profiles") { uid ->
        val cleanName = displayName.trim().take(50)
        val cleanHandle = handle.trim().take(30)
        require(cleanName.isNotEmpty()) { "Display name required" }
        require(cleanHandle.length >= 2) { "Handle must be at least 2 characters" }
        db.collection("public_profiles").document(uid).update(
            mapOf(
                "displayName" to cleanName,
                "handle" to cleanHandle,
                "bio" to bio.trim().take(200),
                "avatarStyle" to avatarStyle.ifBlank { "cyber_visor" },
                "avatarPhotoUri" to avatarPhotoUri.take(500),
                "currentGame" to currentGame.trim().take(60),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updatePlayerStatus(status: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val normalized = PlayerStatusOption.fromKey(status).key
        return try {
            val batch = db.batch()
            batch.update(
                db.collection("public_profiles").document(uid),
                mapOf(
                    "playerStatus" to normalized,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "privacyShowOnline" to (normalized != PlayerStatusOption.AWAY.key),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "public_profiles/$uid")
            Result.failure(e)
        }
    }

    // --- Level System, Buffs, Daily Gift Coins & Achievements ---

    suspend fun claimDailyReward(userPrivate: UserPrivate): Result<DailyRewardDay> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val nextIndex = ((userPrivate.lastClaimDayIndex % 7L) + 1L).toInt()
            val reward = ArcadeCatalog.dailyRewards.first { it.day == nextIndex }
            val hasCoinBuff = userPrivate.activeBuffs.contains("COIN_BUFF_15")
            val hasXpBuff = userPrivate.activeBuffs.contains("XP_BOOST_20")
            val coinEarned = if (hasCoinBuff) (reward.coins * 115L) / 100L else reward.coins
            val xpEarned = if (hasXpBuff) (reward.xp * 120L) / 100L else reward.xp
            val newXp = userPrivate.xp + xpEarned
            val newLevel = ArcadeCatalog.calculateLevel(newXp)
            val newCoins = userPrivate.coins + coinEarned
            val updatedBuffs = buildSet {
                addAll(userPrivate.activeBuffs)
                if (reward.buffLabel != null) add(reward.buffLabel)
                ArcadeCatalog.levelBuffs.filter { it.unlockLevel <= newLevel }.forEach { add(it.code) }
            }.toList().take(10)

            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "coins" to newCoins,
                    "xp" to newXp,
                    "level" to newLevel,
                    "streakDays" to userPrivate.streakDays + 1L,
                    "lastClaimDayIndex" to nextIndex.toLong(),
                    "activeBuffs" to updatedBuffs,
                    "lastDailyClaimAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("public_profiles").document(uid),
                mapOf(
                    "level" to newLevel,
                    "xp" to newXp,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val notifRef = db.collection("users").document(uid).collection("notifications").document()
            batch.set(
                notifRef,
                mapOf(
                    "userId" to uid,
                    "title" to "Day $nextIndex Gift Claimed!",
                    "body" to "+$coinEarned Coins & +$xpEarned XP added (Level $newLevel).",
                    "notifType" to "reward",
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(reward.copy(coins = coinEarned, xp = xpEarned))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun grantActivityXp(userPrivate: UserPrivate, baseXp: Long, bonusCoins: Long = 0L): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val multiplier = if (userPrivate.activeBuffs.contains("XP_BOOST_20")) 120L else 110L
            val earnedXp = (baseXp * multiplier) / 100L
            val newXp = userPrivate.xp + earnedXp
            val newLevel = ArcadeCatalog.calculateLevel(newXp)
            val leveledUp = newLevel > userPrivate.level
            val updatedBuffs = buildSet {
                addAll(userPrivate.activeBuffs)
                ArcadeCatalog.levelBuffs.filter { it.unlockLevel <= newLevel }.forEach { add(it.code) }
            }.toList().take(10)

            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "xp" to newXp,
                    "level" to newLevel,
                    "coins" to userPrivate.coins + bonusCoins + if (leveledUp) 200L else 0L,
                    "activeBuffs" to updatedBuffs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("public_profiles").document(uid),
                mapOf(
                    "xp" to newXp,
                    "level" to newLevel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(leveledUp)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun claimAchievement(userPrivate: UserPrivate, ach: AchievementDef): Result<Unit> =
        runMutation(OperationType.UPDATE, "users") { uid ->
            if (userPrivate.claimedAchievementIds.contains(ach.id)) return@runMutation
            val newXp = userPrivate.xp + ach.rewardXp
            val newLevel = ArcadeCatalog.calculateLevel(newXp)
            val updatedClaimed = (userPrivate.claimedAchievementIds + ach.id).distinct().take(50)
            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "coins" to userPrivate.coins + ach.rewardCoins,
                    "xp" to newXp,
                    "level" to newLevel,
                    "claimedAchievementIds" to updatedClaimed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("public_profiles").document(uid),
                mapOf(
                    "xp" to newXp,
                    "level" to newLevel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        }

    // --- Privacy, Follow, Block & Report ---

    suspend fun updatePrivacySettings(
        dmPolicy: String,
        showOnline: Boolean,
        lanVisible: Boolean,
        autoSaveReplays: Boolean
    ): Result<Unit> = runMutation(OperationType.UPDATE, "users") { uid ->
        val batch = db.batch()
        batch.update(
            db.collection("users").document(uid),
            mapOf(
                "privacyDmPolicy" to dmPolicy,
                "privacyShowOnline" to showOnline,
                "privacyLanVisible" to lanVisible,
                "autoSaveReplays" to autoSaveReplays,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("public_profiles").document(uid),
            mapOf(
                "playerStatus" to if (showOnline) PlayerStatusOption.ONLINE.key else PlayerStatusOption.AWAY.key,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
    }

    suspend fun toggleFollow(userPrivate: UserPrivate, targetProfile: PublicProfile): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (uid == targetProfile.userId) return Result.success(false)
        return try {
            val isFollowing = userPrivate.followingUserIds.contains(targetProfile.userId)
            val updatedFollowing = if (isFollowing) {
                userPrivate.followingUserIds - targetProfile.userId
            } else {
                (userPrivate.followingUserIds + targetProfile.userId).distinct().take(200)
            }
            val nextFollowerCount = if (isFollowing) {
                (targetProfile.followersCount - 1L).coerceAtLeast(0L)
            } else {
                targetProfile.followersCount + 1L
            }
            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "followingUserIds" to updatedFollowing,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("public_profiles").document(uid),
                mapOf(
                    "followingCount" to updatedFollowing.size.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("public_profiles").document(targetProfile.userId),
                mapOf(
                    "followersCount" to nextFollowerCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(!isFollowing)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "public_profiles/${targetProfile.userId}")
            Result.failure(e)
        }
    }

    suspend fun toggleBlockUser(userPrivate: UserPrivate, targetUserId: String): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val isBlocked = userPrivate.blockedUserIds.contains(targetUserId)
            val updatedBlocked = if (isBlocked) {
                userPrivate.blockedUserIds - targetUserId
            } else {
                (userPrivate.blockedUserIds + targetUserId).distinct().take(100)
            }
            db.collection("users").document(uid).update(
                mapOf(
                    "blockedUserIds" to updatedBlocked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(!isBlocked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetUserId: String,
        targetHandle: String,
        reason: String,
        details: String
    ): Result<Unit> = runMutation(OperationType.CREATE, "reports") { uid ->
        val docRef = db.collection("reports").document()
        docRef.set(
            mapOf(
                "reporterId" to uid,
                "targetUserId" to targetUserId.take(128),
                "targetHandle" to targetHandle.ifBlank { "player" }.take(40),
                "reason" to reason.ifBlank { "Inappropriate behavior" }.take(80),
                "details" to details.trim().take(300),
                "status" to "submitted",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // --- Posts, Stories, Shorts, Likes, Comments, Shares ---

    fun observePublicPosts(): Flow<List<SocialPost>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("posts")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SocialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "posts", uid)
        )
    }

    suspend fun createPost(
        profile: PublicProfile,
        postType: String,
        gameTag: String,
        caption: String,
        mediaUri: String
    ): Result<Unit> = runMutation(OperationType.CREATE, "posts") { uid ->
        val cleanCaption = caption.trim().take(500)
        require(cleanCaption.isNotEmpty()) { "Caption cannot be empty" }
        val docRef = db.collection("posts").document()
        docRef.set(
            mapOf(
                "authorId" to uid,
                "authorName" to profile.displayName.ifBlank { "Player" }.take(50),
                "authorHandle" to profile.handle.ifBlank { "player" }.take(32),
                "authorAvatar" to profile.avatarStyle.ifBlank { "cyber_visor" },
                "authorVerified" to profile.verifiedBadge,
                "postType" to postType,
                "visibility" to "public",
                "gameTag" to gameTag.ifBlank { "Minecraft" }.take(50),
                "caption" to cleanCaption,
                "mediaUri" to mediaUri.take(500),
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "sharesCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deletePost(postId: String): Result<Unit> =
        runMutation(OperationType.DELETE, "posts/$postId") {
            db.collection("posts").document(postId).delete().await()
        }

    suspend fun toggleLike(post: SocialPost): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val postRef = db.collection("posts").document(post.id)
            val likeRef = postRef.collection("likes").document(uid)
            val existingLike = likeRef.get().await()
            val batch = db.batch()
            if (existingLike.exists()) {
                batch.delete(likeRef)
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to (post.likesCount - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
                Result.success(false)
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "postId" to post.id,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to post.likesCount + 1L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.commit().await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/${post.id}")
            Result.failure(e)
        }
    }

    suspend fun incrementShare(post: SocialPost): Result<Unit> =
        runMutation(OperationType.UPDATE, "posts/${post.id}") {
            db.collection("posts").document(post.id).update(
                mapOf(
                    "sharesCount" to post.sharesCount + 1L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    fun observeComments(postId: String): Flow<List<PostComment>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "posts/$postId/comments", uid)
        )
    }

    suspend fun addComment(post: SocialPost, authorName: String, text: String): Result<Unit> =
        runMutation(OperationType.CREATE, "posts/${post.id}/comments") { uid ->
            val cleanText = text.trim().take(300)
            require(cleanText.isNotEmpty()) { "Comment cannot be empty" }
            val postRef = db.collection("posts").document(post.id)
            val commentRef = postRef.collection("comments").document()
            val batch = db.batch()
            batch.set(
                commentRef,
                mapOf(
                    "postId" to post.id,
                    "authorId" to uid,
                    "authorName" to authorName.ifBlank { "Player" }.take(50),
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to post.commentsCount + 1L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        }

    // --- Live Streaming, Stream Manager, Stream Chat & Replay ---

    fun observeStreams(): Flow<List<LiveStream>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("streams")
                .whereIn("streamStatus", listOf("live", "replay"))
                .snapshots()
                .map { snap ->
                    snap.toObjects(LiveStream::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "streams", uid)
        )
    }

    fun observeStream(streamId: String): Flow<LiveStream?> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("streams").document(streamId)
                .snapshots()
                .map { snap -> snap.toObject(LiveStream::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.GET, "streams/$streamId", uid)
        )
    }

    suspend fun startLiveStream(
        profile: PublicProfile,
        title: String,
        gameTitle: String,
        recordReplay: Boolean
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "Stream title is required" }
            val docRef = db.collection("streams").document()
            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "hostId" to uid,
                    "hostName" to profile.displayName.ifBlank { "Streamer" }.take(50),
                    "hostHandle" to profile.handle.ifBlank { "streamer" }.take(32),
                    "hostAvatar" to profile.avatarStyle.ifBlank { "cyber_visor" },
                    "title" to cleanTitle,
                    "gameTitle" to gameTitle.ifBlank { "Minecraft" }.take(60),
                    "streamStatus" to "live",
                    "viewerCount" to 1L,
                    "peakViewers" to 1L,
                    "totalGiftsCoins" to 0L,
                    "slowModeSec" to 0L,
                    "micEnabled" to true,
                    "recordReplay" to recordReplay,
                    "durationSec" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("public_profiles").document(uid),
                mapOf(
                    "liveNow" to true,
                    "currentGame" to gameTitle.ifBlank { "Minecraft" }.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "streams")
            Result.failure(e)
        }
    }

    suspend fun updateStreamControls(
        stream: LiveStream,
        micEnabled: Boolean,
        slowModeSec: Long,
        recordReplay: Boolean,
        title: String
    ): Result<Unit> = runMutation(OperationType.UPDATE, "streams/${stream.id}") {
        db.collection("streams").document(stream.id).update(
            mapOf(
                "micEnabled" to micEnabled,
                "slowModeSec" to slowModeSec.coerceIn(0L, 120L),
                "recordReplay" to recordReplay,
                "title" to title.trim().ifBlank { stream.title }.take(100),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun endLiveStream(stream: LiveStream, elapsedSec: Long): Result<Unit> =
        runMutation(OperationType.UPDATE, "streams/${stream.id}") { uid ->
            val nextStatus = if (stream.recordReplay) "replay" else "ended"
            val batch = db.batch()
            batch.update(
                db.collection("streams").document(stream.id),
                mapOf(
                    "streamStatus" to nextStatus,
                    "durationSec" to elapsedSec.coerceAtLeast(stream.durationSec),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("public_profiles").document(uid),
                mapOf(
                    "liveNow" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        }

    fun observeStreamChat(streamId: String): Flow<List<StreamChatMessage>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("streams").document(streamId).collection("chat")
                .whereEqualTo("streamId", streamId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StreamChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "streams/$streamId/chat", uid)
        )
    }

    suspend fun sendStreamChat(
        streamId: String,
        authorName: String,
        text: String,
        messageType: String = "chat"
    ): Result<Unit> = runMutation(OperationType.CREATE, "streams/$streamId/chat") { uid ->
        val cleanText = text.trim().take(250)
        require(cleanText.isNotEmpty()) { "Message cannot be empty" }
        db.collection("streams").document(streamId).collection("chat").document().set(
            mapOf(
                "streamId" to streamId,
                "authorId" to uid,
                "authorName" to authorName.ifBlank { "Viewer" }.take(50),
                "text" to cleanText,
                "messageType" to messageType,
                "giftCoins" to 0L,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun sendGiftCoinsToStream(
        userPrivate: UserPrivate,
        stream: LiveStream,
        senderName: String,
        giftAmount: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (userPrivate.coins < giftAmount) {
            return Result.failure(IllegalStateException("Not enough Gift Coins"))
        }
        return try {
            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "coins" to userPrivate.coins - giftAmount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("streams").document(stream.id),
                mapOf(
                    "totalGiftsCoins" to stream.totalGiftsCoins + giftAmount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val chatRef = db.collection("streams").document(stream.id).collection("chat").document()
            batch.set(
                chatRef,
                mapOf(
                    "streamId" to stream.id,
                    "authorId" to uid,
                    "authorName" to senderName.ifBlank { "Supporter" }.take(50),
                    "text" to "Sent $giftAmount Gift Coins!",
                    "messageType" to "gift",
                    "giftCoins" to giftAmount,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "streams/${stream.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteStreamChat(streamId: String, msgId: String): Result<Unit> =
        runMutation(OperationType.DELETE, "streams/$streamId/chat/$msgId") {
            db.collection("streams").document(streamId).collection("chat").document(msgId).delete().await()
        }

    // --- Multiplayer LAN Worlds ---

    fun detectLocalIpv4Address(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress ?: "192.168.1.100"
        } catch (_: Exception) {
            "192.168.1.100"
        }
    }

    fun observeWorlds(): Flow<List<MultiplayerWorld>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("worlds")
                .whereIn("worldStatus", listOf("open", "in_game"))
                .snapshots()
                .map { snap ->
                    snap.toObjects(MultiplayerWorld::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "worlds", uid)
        )
    }

    suspend fun hostWorld(
        profile: PublicProfile,
        worldName: String,
        gameName: String,
        gameMode: String,
        lanAddress: String,
        maxPlayers: Long
    ): Result<Unit> = runMutation(OperationType.CREATE, "worlds") { uid ->
        val cleanName = worldName.trim().take(80)
        require(cleanName.isNotEmpty()) { "World name is required" }
        val resolvedLanAddress = lanAddress.ifBlank {
            withContext(Dispatchers.IO) { "${detectLocalIpv4Address()}:19132" }
        }.take(80)
        db.collection("worlds").document().set(
            mapOf(
                "hostId" to uid,
                "hostName" to profile.displayName.ifBlank { "Host" }.take(50),
                "hostAvatar" to profile.avatarStyle.ifBlank { "pixel_knight" },
                "worldName" to cleanName,
                "gameName" to gameName.ifBlank { "Minecraft" }.take(50),
                "gameMode" to gameMode,
                "lanAddress" to resolvedLanAddress,
                "lanVisible" to true,
                "multiplayerOn" to true,
                "worldStatus" to "open",
                "maxPlayers" to maxPlayers.coerceIn(2L, 32L),
                "participantIds" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun joinWorld(world: MultiplayerWorld): Result<Unit> =
        runMutation(OperationType.UPDATE, "worlds/${world.id}") { uid ->
            val updated = (world.participantIds + uid).distinct().take(world.maxPlayers.toInt())
            db.collection("worlds").document(world.id).update(
                mapOf(
                    "participantIds" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun closeWorld(worldId: String): Result<Unit> =
        runMutation(OperationType.UPDATE, "worlds/$worldId") {
            db.collection("worlds").document(worldId).update(
                mapOf(
                    "worldStatus" to "closed",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    // --- Party System, Voice Chat & Matchmaking ---

    fun observeParties(): Flow<List<PartyRoom>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("parties")
                .whereEqualTo("partyStatus", "active")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "parties", uid)
        )
    }

    suspend fun createParty(
        profile: PublicProfile,
        partyName: String,
        gameTitle: String,
        maxSlots: Long
    ): Result<Unit> = runMutation(OperationType.CREATE, "parties") { uid ->
        val cleanName = partyName.trim().take(60)
        require(cleanName.isNotEmpty()) { "Party name required" }
        db.collection("parties").document().set(
            mapOf(
                "leaderId" to uid,
                "leaderName" to profile.displayName.ifBlank { "Leader" }.take(50),
                "partyName" to cleanName,
                "gameTitle" to gameTitle.ifBlank { "Minecraft" }.take(60),
                "maxSlots" to maxSlots.coerceIn(2L, 8L),
                "memberIds" to listOf(uid),
                "mutedMemberIds" to emptyList<String>(),
                "voiceActive" to true,
                "queueState" to "idle",
                "partyStatus" to "active",
                "matchedLobbyCode" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun joinOrLeaveParty(party: PartyRoom): Result<Unit> =
        runMutation(OperationType.UPDATE, "parties/${party.id}") { uid ->
            val inParty = party.memberIds.contains(uid)
            val updated = if (inParty) {
                party.memberIds - uid
            } else {
                (party.memberIds + uid).distinct().take(party.maxSlots.toInt())
            }
            db.collection("parties").document(party.id).update(
                mapOf(
                    "memberIds" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun togglePartyMicMute(party: PartyRoom): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val isMuted = party.mutedMemberIds.contains(uid)
            val updatedMuted = if (isMuted) party.mutedMemberIds - uid else (party.mutedMemberIds + uid).distinct()
            db.collection("parties").document(party.id).update(
                mapOf(
                    "mutedMemberIds" to updatedMuted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(!isMuted)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "parties/${party.id}")
            Result.failure(e)
        }
    }

    suspend fun updateMatchmakingState(party: PartyRoom, queueState: String, lobbyCode: String = ""): Result<Unit> =
        runMutation(OperationType.UPDATE, "parties/${party.id}") {
            db.collection("parties").document(party.id).update(
                mapOf(
                    "queueState" to queueState,
                    "matchedLobbyCode" to lobbyCode.take(24),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    // --- Game Communities & Tournaments ---

    fun observeCommunities(): Flow<List<GameCommunity>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("communities")
                .whereEqualTo("openCommunity", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GameCommunity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.memberCount }
                }
                .logFirestoreErrors(OperationType.LIST, "communities", uid)
        )
    }

    suspend fun createCommunity(name: String, gameCategory: String, description: String): Result<Unit> =
        runMutation(OperationType.CREATE, "communities") { uid ->
            val cleanName = name.trim().take(60)
            require(cleanName.length >= 2) { "Community name too short" }
            db.collection("communities").document().set(
                mapOf(
                    "creatorId" to uid,
                    "name" to cleanName,
                    "gameCategory" to gameCategory.ifBlank { "Minecraft" }.take(40),
                    "description" to description.trim().ifBlank { "Arcade squad & LAN events" }.take(250),
                    "badgeColor" to "cyan",
                    "openCommunity" to true,
                    "memberCount" to 1L,
                    "memberIds" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun toggleJoinCommunity(community: GameCommunity): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val isMember = community.memberIds.contains(uid)
            val updatedIds = if (isMember) community.memberIds - uid else (community.memberIds + uid).distinct().take(100)
            val nextCount = if (isMember) (community.memberCount - 1L).coerceAtLeast(0L) else community.memberCount + 1L
            db.collection("communities").document(community.id).update(
                mapOf(
                    "memberIds" to updatedIds,
                    "memberCount" to nextCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(!isMember)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "communities/${community.id}")
            Result.failure(e)
        }
    }

    fun observeTournaments(): Flow<List<Tournament>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("tournaments")
                .whereIn("tournamentState", listOf("upcoming", "active", "completed"))
                .snapshots()
                .map { snap ->
                    snap.toObjects(Tournament::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "tournaments", uid)
        )
    }

    suspend fun createTournament(
        title: String,
        gameTitle: String,
        prizeCoins: Long,
        entryCoins: Long,
        maxTeams: Long,
        scheduleLabel: String
    ): Result<Unit> = runMutation(OperationType.CREATE, "tournaments") { uid ->
        val cleanTitle = title.trim().take(80)
        require(cleanTitle.length >= 2) { "Tournament title required" }
        db.collection("tournaments").document().set(
            mapOf(
                "organizerId" to uid,
                "title" to cleanTitle,
                "gameTitle" to gameTitle.ifBlank { "Minecraft" }.take(50),
                "prizeCoins" to prizeCoins.coerceIn(0L, 1000000L),
                "entryCoins" to entryCoins.coerceIn(0L, 10000L),
                "maxTeams" to maxTeams.coerceIn(2L, 64L),
                "tournamentState" to "upcoming",
                "registeredIds" to listOf(uid),
                "scheduleLabel" to scheduleLabel.ifBlank { "Tonight 8:00 PM" }.take(40),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun toggleRegisterTournament(tournament: Tournament): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val isRegistered = tournament.registeredIds.contains(uid)
            val updated = if (isRegistered) {
                tournament.registeredIds - uid
            } else {
                (tournament.registeredIds + uid).distinct().take(tournament.maxTeams.toInt())
            }
            db.collection("tournaments").document(tournament.id).update(
                mapOf(
                    "registeredIds" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(!isRegistered)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tournaments/${tournament.id}")
            Result.failure(e)
        }
    }

    // --- Friends, Friend Requests, Messages & Group Chat ---

    fun observeFriendRequests(uid: String): Flow<List<FriendRequest>> = flow {
        requireUserId()
        emitAll(
            db.collection("friend_requests")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "friend_requests", uid)
        )
    }

    suspend fun sendFriendRequest(sender: PublicProfile, receiver: PublicProfile): Result<Unit> =
        runMutation(OperationType.CREATE, "friend_requests") { uid ->
            require(uid != receiver.userId) { "Cannot add yourself" }
            val docId = listOf(uid, receiver.userId).sorted().joinToString("_")
            db.collection("friend_requests").document(docId).set(
                mapOf(
                    "senderId" to uid,
                    "senderName" to sender.displayName.ifBlank { "Player" }.take(50),
                    "senderAvatar" to sender.avatarStyle.ifBlank { "cyber_visor" },
                    "receiverId" to receiver.userId,
                    "receiverName" to receiver.displayName.ifBlank { "Player" }.take(50),
                    "participantIds" to listOf(uid, receiver.userId),
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    suspend fun respondFriendRequest(requestId: String, accept: Boolean): Result<Unit> =
        runMutation(OperationType.UPDATE, "friend_requests/$requestId") {
            db.collection("friend_requests").document(requestId).update(
                mapOf(
                    "status" to if (accept) "accepted" else "declined",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    fun observeConversations(uid: String): Flow<List<Conversation>> = flow {
        requireUserId()
        emitAll(
            db.collection("conversations")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "conversations", uid)
        )
    }

    suspend fun createConversation(
        title: String,
        groupChat: Boolean,
        participantIds: List<String>,
        participantLabels: List<String>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val uniqueIds = (participantIds + uid).distinct().take(20)
            require(uniqueIds.size >= 2) { "Select at least one friend" }
            val docRef = db.collection("conversations").document()
            docRef.set(
                mapOf(
                    "title" to title.trim().ifBlank { "Arcade Chat" }.take(80),
                    "groupChat" to groupChat,
                    "participantIds" to uniqueIds,
                    "participantLabels" to participantLabels.take(20),
                    "lastMessage" to "Conversation started",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "conversations")
            Result.failure(e)
        }
    }

    fun observeMessages(conversationId: String, uid: String): Flow<List<ChatMessage>> = flow {
        requireUserId()
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "conversations/$conversationId/messages", uid)
        )
    }

    suspend fun sendMessage(
        conversation: Conversation,
        senderName: String,
        text: String
    ): Result<Unit> = runMutation(OperationType.CREATE, "conversations/${conversation.id}/messages") { uid ->
        val cleanText = text.trim().take(500)
        require(cleanText.isNotEmpty()) { "Message cannot be empty" }
        val convRef = db.collection("conversations").document(conversation.id)
        val msgRef = convRef.collection("messages").document()
        val batch = db.batch()
        batch.set(
            msgRef,
            mapOf(
                "conversationId" to conversation.id,
                "senderId" to uid,
                "senderName" to senderName.ifBlank { "Player" }.take(50),
                "text" to cleanText,
                "participantIds" to conversation.participantIds,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            convRef,
            mapOf(
                "lastMessage" to cleanText.take(250),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
    }

    // --- Gallery & Notifications ---

    fun observeGallery(uid: String): Flow<List<GalleryMedia>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("gallery")
                .snapshots()
                .map { snap ->
                    snap.toObjects(GalleryMedia::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "users/$uid/gallery", uid)
        )
    }

    suspend fun addGalleryMedia(
        title: String,
        mediaType: String,
        gameTitle: String,
        mediaUri: String,
        durationSec: Long
    ): Result<Unit> = runMutation(OperationType.CREATE, "users/gallery") { uid ->
        val cleanTitle = title.trim().ifBlank { "Arcade Capture" }.take(100)
        db.collection("users").document(uid).collection("gallery").document().set(
            mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "mediaType" to mediaType,
                "gameTitle" to gameTitle.ifBlank { "Minecraft" }.take(60),
                "mediaUri" to mediaUri.take(500),
                "durationSec" to durationSec.coerceIn(0L, 36000L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteGalleryMedia(mediaId: String): Result<Unit> =
        runMutation(OperationType.DELETE, "users/gallery/$mediaId") { uid ->
            db.collection("users").document(uid).collection("gallery").document(mediaId).delete().await()
        }

    fun observeNotifications(uid: String): Flow<List<UserNotification>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "users/$uid/notifications", uid)
        )
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> =
        runMutation(OperationType.UPDATE, "users/notifications/$notifId") { uid ->
            db.collection("users").document(uid).collection("notifications").document(notifId).update(
                mapOf(
                    "readFlag" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    private suspend inline fun runMutation(
        op: OperationType,
        path: String,
        crossinline block: suspend (uid: String) -> Unit
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            block(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, path)
            Result.failure(e)
        }
    }
}
