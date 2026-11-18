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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

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
            val claimedReward = db.runTransaction { transaction ->
                val userRef = db.collection("users").document(uid)
                val profileRef = db.collection("public_profiles").document(uid)
                val userSnap = transaction.get(userRef)
                val latestPrivate = userSnap.toObject(
                    UserPrivate::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: userPrivate

                val lastClaimMs = latestPrivate.lastDailyClaimAt?.toDate()?.time
                val rewardStatus = ArcadeCatalog.dailyRewardStatus(lastClaimMs, System.currentTimeMillis())
                if (!rewardStatus.canClaim) {
                    throw IllegalStateException(
                        "Daily bonus already claimed! Next bonus in ${rewardStatus.formattedRemaining}."
                    )
                }

                val nextIndex = ((latestPrivate.lastClaimDayIndex % 7L) + 1L).toInt()
                val reward = ArcadeCatalog.dailyRewards.first { it.day == nextIndex }
                val hasCoinBuff = latestPrivate.activeBuffs.contains("COIN_BUFF_15")
                val hasXpBuff = latestPrivate.activeBuffs.contains("XP_BOOST_20")
                val coinEarned = if (hasCoinBuff) (reward.coins * 115L) / 100L else reward.coins
                val xpGain = LevelProgressionTracker.applyXpGain(
                    currentXp = latestPrivate.xp,
                    baseXp = reward.xp,
                    multiplierPercent = if (hasXpBuff) 120L else 100L
                )
                val newCoins = latestPrivate.coins + coinEarned + xpGain.levelUpBonusCoins
                val updatedBuffs = buildSet {
                    addAll(latestPrivate.activeBuffs)
                    if (reward.buffLabel != null) add(reward.buffLabel)
                    ArcadeCatalog.levelBuffs.filter { it.unlockLevel <= xpGain.newLevel }.forEach { add(it.code) }
                }.toList().take(10)

                transaction.update(
                    userRef,
                    mapOf(
                        "coins" to newCoins,
                        "xp" to xpGain.newXp,
                        "level" to xpGain.newLevel,
                        "streakDays" to latestPrivate.streakDays + 1L,
                        "lastClaimDayIndex" to nextIndex.toLong(),
                        "activeBuffs" to updatedBuffs,
                        "lastDailyClaimAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    profileRef,
                    mapOf(
                        "level" to xpGain.newLevel,
                        "xp" to xpGain.newXp,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val notifRef = userRef.collection("notifications").document()
                transaction.set(
                    notifRef,
                    mapOf(
                        "userId" to uid,
                        "title" to "Day $nextIndex Daily Bonus Claimed!",
                        "body" to "+$coinEarned Coins & +${xpGain.xpEarned} XP added (Level ${xpGain.newLevel}). Come back in 24h!",
                        "notifType" to "reward",
                        "readFlag" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                reward.copy(coins = coinEarned, xp = xpGain.xpEarned)
            }.await()
            Result.success(claimedReward)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun grantActivityXp(userPrivate: UserPrivate, baseXp: Long, bonusCoins: Long = 0L): Result<XpGainResult> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val result = db.runTransaction { transaction ->
                val userRef = db.collection("users").document(uid)
                val profileRef = db.collection("public_profiles").document(uid)
                val latestPrivate = transaction.get(userRef).toObject(
                    UserPrivate::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: userPrivate

                val multiplier = if (latestPrivate.activeBuffs.contains("XP_BOOST_20")) 120L else 110L
                val xpGain = LevelProgressionTracker.applyXpGain(
                    currentXp = latestPrivate.xp,
                    baseXp = baseXp,
                    multiplierPercent = multiplier
                )
                val updatedBuffs = buildSet {
                    addAll(latestPrivate.activeBuffs)
                    ArcadeCatalog.levelBuffs.filter { it.unlockLevel <= xpGain.newLevel }.forEach { add(it.code) }
                }.toList().take(10)

                transaction.update(
                    userRef,
                    mapOf(
                        "xp" to xpGain.newXp,
                        "level" to xpGain.newLevel,
                        "coins" to latestPrivate.coins + bonusCoins.coerceAtLeast(0L) + xpGain.levelUpBonusCoins,
                        "activeBuffs" to updatedBuffs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    profileRef,
                    mapOf(
                        "xp" to xpGain.newXp,
                        "level" to xpGain.newLevel,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (xpGain.leveledUp) {
                    val tier = LevelTier.forLevel(xpGain.newLevel)
                    val notifRef = userRef.collection("notifications").document()
                    transaction.set(
                        notifRef,
                        mapOf(
                            "userId" to uid,
                            "title" to "Level ${xpGain.newLevel} Reached!",
                            "body" to "Rank unlocked: ${tier.title} (+${xpGain.levelUpBonusCoins} bonus Coins).",
                            "notifType" to "reward",
                            "readFlag" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                xpGain
            }.await()
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun claimAchievement(userPrivate: UserPrivate, ach: AchievementDef): Result<XpGainResult?> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val gain = db.runTransaction { transaction ->
                val userRef = db.collection("users").document(uid)
                val profileRef = db.collection("public_profiles").document(uid)
                val latestPrivate = transaction.get(userRef).toObject(
                    UserPrivate::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: userPrivate

                if (latestPrivate.claimedAchievementIds.contains(ach.id)) {
                    return@runTransaction null
                }

                val xpGain = LevelProgressionTracker.applyXpGain(
                    currentXp = latestPrivate.xp,
                    baseXp = ach.rewardXp,
                    multiplierPercent = 100L
                )
                val updatedClaimed = (latestPrivate.claimedAchievementIds + ach.id).distinct().take(50)
                val updatedBuffs = buildSet {
                    addAll(latestPrivate.activeBuffs)
                    ArcadeCatalog.levelBuffs.filter { it.unlockLevel <= xpGain.newLevel }.forEach { add(it.code) }
                }.toList().take(10)

                transaction.update(
                    userRef,
                    mapOf(
                        "coins" to latestPrivate.coins + ach.rewardCoins + xpGain.levelUpBonusCoins,
                        "xp" to xpGain.newXp,
                        "level" to xpGain.newLevel,
                        "activeBuffs" to updatedBuffs,
                        "claimedAchievementIds" to updatedClaimed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    profileRef,
                    mapOf(
                        "xp" to xpGain.newXp,
                        "level" to xpGain.newLevel,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                xpGain
            }.await()
            Result.success(gain)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
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
        maxPlayers: Long
    ): Result<Unit> = runMutation(OperationType.CREATE, "worlds") { uid ->
        val cleanName = worldName.trim().take(80)
        require(cleanName.isNotEmpty()) { "World name is required" }
        db.collection("worlds").document().set(
            mapOf(
                "hostId" to uid,
                "hostName" to profile.displayName.ifBlank { "Host" }.take(50),
                "hostAvatar" to profile.avatarStyle.ifBlank { "pixel_knight" },
                "worldName" to cleanName,
                "gameName" to gameName.ifBlank { "Minecraft" }.take(50),
                "gameMode" to gameMode,
                "lanAddress" to "local-lan",
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
                        .sortedWith(
                            compareBy<Tournament> {
                                when (it.tournamentState) {
                                    "upcoming" -> 0
                                    "active" -> 1
                                    else -> 2
                                }
                            }.thenBy {
                                if (it.startsAtMillis > 0L) it.startsAtMillis else Long.MAX_VALUE
                            }.thenByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                        )
                }
                .logFirestoreErrors(OperationType.LIST, "tournaments", uid)
        )
    }

    fun observeTournamentRegistrations(tournamentId: String): Flow<List<TournamentRegistration>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("tournaments").document(tournamentId).collection("registrations")
                .whereEqualTo("tournamentId", tournamentId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TournamentRegistration::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "tournaments/$tournamentId/registrations", uid)
        )
    }

    suspend fun createTournament(
        title: String,
        gameTitle: String,
        prizeCoins: Long,
        entryCoins: Long,
        maxTeams: Long,
        scheduleLabel: String,
        startsAtMillis: Long = 0L,
        formatLabel: String = "Squad 4v4",
        rulesSummary: String = ""
    ): Result<Unit> = runMutation(OperationType.CREATE, "tournaments") { uid ->
        val cleanTitle = title.trim().take(80)
        require(cleanTitle.length >= 2) { "Tournament title required" }
        val resolvedStart = if (startsAtMillis > 0L) startsAtMillis else System.currentTimeMillis() + 3600_000L
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
                "scheduleLabel" to scheduleLabel.ifBlank { "In 1 hour" }.take(40),
                "startsAtMillis" to resolvedStart.coerceAtLeast(0L),
                "formatLabel" to formatLabel.ifBlank { "Squad 4v4" }.take(32),
                "rulesSummary" to rulesSummary.trim().ifBlank { "Fair play required • Check in 15 min before kickoff." }.take(240),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun registerForTournament(
        tournament: Tournament,
        userPrivate: UserPrivate?,
        profile: PublicProfile?,
        teamName: String,
        gamerTag: String,
        division: String
    ): Result<Unit> = runMutation(OperationType.WRITE, "tournaments/${tournament.id}/registrations") { uid ->
        require(tournament.tournamentState == "upcoming") { "Registration is only open for upcoming tournaments." }
        require(!tournament.registeredIds.contains(uid)) { "You are already registered for this tournament." }
        require(tournament.registeredIds.size < tournament.maxTeams.toInt()) { "Tournament bracket is full." }
        val cleanTeam = teamName.trim().take(50)
        val cleanTag = gamerTag.trim().take(40)
        require(cleanTeam.isNotEmpty()) { "Team or squad name is required." }
        require(cleanTag.isNotEmpty()) { "In-game tag or ID is required." }
        val normalizedDivision = when (division) {
            "Solo", "Duo", "Squad" -> division
            else -> "Squad"
        }
        val fee = tournament.entryCoins.coerceAtLeast(0L)
        if (fee > 0L) {
            val balance = userPrivate?.coins ?: 0L
            require(balance >= fee) { "Need $fee Gift Coins to cover the tournament entry fee." }
        }

        val tourRef = db.collection("tournaments").document(tournament.id)
        val regRef = tourRef.collection("registrations").document(uid)
        val updatedIds = (tournament.registeredIds + uid).distinct().take(tournament.maxTeams.toInt())
        val playerName = profile?.displayName?.ifBlank { "Contender" }?.take(50) ?: "Contender"

        val batch = db.batch()
        batch.set(
            regRef,
            mapOf(
                "tournamentId" to tournament.id,
                "userId" to uid,
                "playerName" to playerName,
                "teamName" to cleanTeam,
                "gamerTag" to cleanTag,
                "division" to normalizedDivision,
                "agreedRules" to true,
                "entryPaidCoins" to fee,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            tourRef,
            mapOf(
                "registeredIds" to updatedIds,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (fee > 0L && userPrivate != null) {
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "coins" to (userPrivate.coins - fee).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
    }

    suspend fun withdrawTournamentRegistration(
        tournament: Tournament,
        userPrivate: UserPrivate?,
        refundCoins: Long
    ): Result<Unit> = runMutation(OperationType.WRITE, "tournaments/${tournament.id}/registrations") { uid ->
        require(tournament.tournamentState != "completed") { "Completed tournaments cannot be modified." }
        val tourRef = db.collection("tournaments").document(tournament.id)
        val regRef = tourRef.collection("registrations").document(uid)
        val updatedIds = tournament.registeredIds - uid
        val safeRefund = refundCoins.coerceIn(0L, 10000L)

        val batch = db.batch()
        batch.update(
            tourRef,
            mapOf(
                "registeredIds" to updatedIds,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.delete(regRef)
        if (safeRefund > 0L && userPrivate != null) {
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "coins" to userPrivate.coins + safeRefund,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
    }

    suspend fun updateTournamentState(tournamentId: String, nextState: String): Result<Unit> =
        runMutation(OperationType.UPDATE, "tournaments/$tournamentId") {
            require(nextState in listOf("upcoming", "active", "completed")) { "Invalid tournament state" }
            db.collection("tournaments").document(tournamentId).update(
                mapOf(
                    "tournamentState" to nextState,
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
            val docRef = db.collection("friend_requests").document(docId)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val existingStatus = existingSnap.getString("status")
                if (existingStatus == "accepted") {
                    throw IllegalStateException("Already friends with ${receiver.displayName}")
                }
                if (existingStatus == "declined") {
                    docRef.delete().await()
                }
            }
            docRef.set(
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

    suspend fun removeFriendRequest(requestId: String): Result<Unit> =
        runMutation(OperationType.DELETE, "friend_requests/$requestId") {
            require(requestId.isNotBlank()) { "Friend request ID required" }
            db.collection("friend_requests").document(requestId).delete().await()
        }

    // --- Multiplayer Party Invites ---

    fun observePartyInvites(uid: String): Flow<List<PartyInvite>> = flow {
        requireUserId()
        emitAll(
            db.collection("party_invites")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PartyInvite::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .logFirestoreErrors(OperationType.LIST, "party_invites", uid)
        )
    }

    suspend fun sendPartyInvites(
        sender: PublicProfile,
        receivers: List<PublicProfile>,
        party: PartyRoom
    ): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val validReceivers = receivers
            .filter { it.userId != uid && !party.memberIds.contains(it.userId) }
            .distinctBy { it.userId }
        if (validReceivers.isEmpty()) {
            return Result.failure(IllegalStateException("Select at least one friend not already in the party"))
        }
        return try {
            val batch = db.batch()
            validReceivers.forEach { receiver ->
                val docRef = db.collection("party_invites").document()
                batch.set(
                    docRef,
                    mapOf(
                        "partyId" to party.id,
                        "partyName" to party.partyName.ifBlank { "Arcade Party" }.take(60),
                        "gameTitle" to party.gameTitle.ifBlank { "Minecraft" }.take(60),
                        "senderId" to uid,
                        "senderName" to sender.displayName.ifBlank { "Player" }.take(50),
                        "senderAvatar" to sender.avatarStyle.ifBlank { "cyber_visor" },
                        "receiverId" to receiver.userId,
                        "receiverName" to receiver.displayName.ifBlank { "Friend" }.take(50),
                        "participantIds" to listOf(uid, receiver.userId),
                        "status" to "pending",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(validReceivers.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "party_invites")
            Result.failure(e)
        }
    }

    suspend fun createPartyAndInviteFriends(
        sender: PublicProfile,
        partyName: String,
        gameTitle: String,
        maxSlots: Long,
        invitees: List<PublicProfile>
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val cleanName = partyName.trim().take(60)
            require(cleanName.isNotEmpty()) { "Party name required" }
            val cleanGame = gameTitle.ifBlank { "Minecraft" }.take(60)
            val partyRef = db.collection("parties").document()
            val batch = db.batch()
            batch.set(
                partyRef,
                mapOf(
                    "leaderId" to uid,
                    "leaderName" to sender.displayName.ifBlank { "Leader" }.take(50),
                    "partyName" to cleanName,
                    "gameTitle" to cleanGame,
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
            )
            invitees
                .filter { it.userId != uid }
                .distinctBy { it.userId }
                .forEach { receiver ->
                    val inviteRef = db.collection("party_invites").document()
                    batch.set(
                        inviteRef,
                        mapOf(
                            "partyId" to partyRef.id,
                            "partyName" to cleanName,
                            "gameTitle" to cleanGame,
                            "senderId" to uid,
                            "senderName" to sender.displayName.ifBlank { "Player" }.take(50),
                            "senderAvatar" to sender.avatarStyle.ifBlank { "cyber_visor" },
                            "receiverId" to receiver.userId,
                            "receiverName" to receiver.displayName.ifBlank { "Friend" }.take(50),
                            "participantIds" to listOf(uid, receiver.userId),
                            "status" to "pending",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            batch.commit().await()
            Result.success(partyRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "parties")
            Result.failure(e)
        }
    }

    suspend fun respondPartyInvite(invite: PartyInvite, accept: Boolean): Result<Unit> =
        runMutation(OperationType.UPDATE, "party_invites/${invite.id}") { uid ->
            val inviteRef = db.collection("party_invites").document(invite.id)
            if (!accept) {
                inviteRef.update(
                    mapOf(
                        "status" to "declined",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                return@runMutation
            }

            val partyRef = db.collection("parties").document(invite.partyId)
            val partySnap = partyRef.get().await()
            val party = partySnap.toObject(PartyRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("This party room is no longer available.")
            require(party.partyStatus == "active") { "This party room has closed." }
            val updatedMembers = (party.memberIds + uid).distinct()
            require(updatedMembers.size <= party.maxSlots.toInt()) { "Party is already full." }

            val batch = db.batch()
            batch.update(
                inviteRef,
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                partyRef,
                mapOf(
                    "memberIds" to updatedMembers,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        }

    suspend fun cancelPartyInvite(inviteId: String): Result<Unit> =
        runMutation(OperationType.DELETE, "party_invites/$inviteId") {
            db.collection("party_invites").document(inviteId).delete().await()
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
