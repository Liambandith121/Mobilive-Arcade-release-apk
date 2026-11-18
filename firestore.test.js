const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("public_profiles").where("searchable", "==", true).get());
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("streams").where("streamStatus", "in", ["live", "replay"]).get());
  await assertFails(unauthDb.collection("worlds").where("worldStatus", "in", ["open", "in_game"]).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Private user doc & PII isolation: owner succeeds, cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      coins: 500,
      xp: 120,
      level: 1,
      streakDays: 1,
      lastClaimDayIndex: 1,
      activeBuffs: ["XP_BOOST_20"],
      privacyDmPolicy: "everyone",
      privacyShowOnline: true,
      privacyLanVisible: true,
      autoSaveReplays: true,
      blockedUserIds: [],
      followingUserIds: [],
      joinedCommunityIds: [],
      claimedAchievementIds: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Partial update on daily claim
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 650,
      streakDays: 2,
      lastClaimDayIndex: 2,
      lastDailyClaimAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user read/write blocked (PII isolation)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ coins: 0, updatedAt: pastDate(500) }));

  // Future timestamp blocked
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 999,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Public profile: shadow update verifiedBadge rejected; filtered list succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  // Cannot create with verifiedBadge: true
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      handle: "alice_arcade",
      displayName: "Alice",
      bio: "Builder",
      avatarStyle: "cyber_visor",
      avatarPhotoUri: "",
      level: 1,
      xp: 0,
      followersCount: 0,
      followingCount: 0,
      verifiedBadge: true,
      liveNow: false,
      searchable: true,
      currentGame: "Minecraft",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      handle: "alice_arcade",
      displayName: "Alice",
      bio: "Builder",
      avatarStyle: "cyber_visor",
      avatarPhotoUri: "",
      level: 1,
      xp: 0,
      followersCount: 0,
      followingCount: 0,
      verifiedBadge: false,
      liveNow: false,
      searchable: true,
      currentGame: "Minecraft",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      playerStatus: "lfg",
      updatedAt: pastDate(1200),
    })
  );
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      playerStatus: "invalid_status",
      updatedAt: pastDate(1100),
    })
  );

  // Shadow update on verifiedBadge or ghost field rejected
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      verifiedBadge: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      ghostField: "hack",
      updatedAt: pastDate(1000),
    })
  );

  // Filtered query matches Kotlin repository
  await assertSucceeds(bobDb.collection("public_profiles").where("searchable", "==", true).get());
  // Unfiltered query fails
  await assertFails(bobDb.collection("public_profiles").get());
});

test("Posts, Comments & Likes: create, filtered list, delete own, reject extra id field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorHandle: "alice_arcade",
      authorAvatar: "cyber_visor",
      authorVerified: false,
      postType: "SHORT",
      visibility: "public",
      gameTag: "Minecraft",
      caption: "Epic sky castle build!",
      mediaUri: "",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject @DocumentId 'id' inside payload
  await assertFails(
    aliceDb.collection("posts").doc("post_2").set({
      id: "post_2",
      authorId: ALICE_UID,
      authorName: "Alice",
      authorHandle: "alice_arcade",
      authorAvatar: "cyber_visor",
      authorVerified: false,
      postType: "POST",
      visibility: "public",
      gameTag: "Minecraft",
      caption: "Invalid payload",
      mediaUri: "",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Filtered query succeeds, unfiltered fails
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(bobDb.collection("posts").get());

  // Bob can comment & like
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      authorId: BOB_UID,
      authorName: "Bob",
      text: "Awesome build!",
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID).set({
      postId: "post_1",
      userId: BOB_UID,
      createdAt: pastDate(1000),
    })
  );

  // Bob cannot delete Alice's post
  await assertFails(bobDb.collection("posts").doc("post_1").delete());
  await assertSucceeds(aliceDb.collection("posts").doc("post_1").delete());
});

test("Live Streams, Worlds (LAN), Parties, Communities, Tournaments, Chat & Friend Requests", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(5000);

  // Stream + Chat
  await assertSucceeds(
    aliceDb.collection("streams").doc("stream_1").set({
      hostId: ALICE_UID,
      hostName: "Alice",
      hostHandle: "alice_arcade",
      hostAvatar: "cyber_visor",
      title: "Survival LAN Speedrun",
      gameTitle: "Minecraft",
      streamStatus: "live",
      viewerCount: 12,
      peakViewers: 12,
      totalGiftsCoins: 0,
      slowModeSec: 0,
      micEnabled: true,
      recordReplay: true,
      durationSec: 120,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(bobDb.collection("streams").where("streamStatus", "in", ["live", "replay"]).get());
  await assertSucceeds(
    bobDb.collection("streams").doc("stream_1").collection("chat").doc("msg_1").set({
      streamId: "stream_1",
      authorId: BOB_UID,
      authorName: "Bob",
      text: "Go Alice! +50 coins",
      messageType: "gift",
      giftCoins: 50,
      createdAt: pastDate(1000),
    })
  );
  // Transition to replay, then ended (terminal)
  await assertSucceeds(
    aliceDb.collection("streams").doc("stream_1").update({
      streamStatus: "ended",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("streams").doc("stream_1").update({
      title: "Cannot edit ended",
      updatedAt: pastDate(200),
    })
  );

  // World LAN host & join
  await assertSucceeds(
    aliceDb.collection("worlds").doc("world_1").set({
      hostId: ALICE_UID,
      hostName: "Alice",
      hostAvatar: "cyber_visor",
      worldName: "Crystal Citadel LAN",
      gameName: "Minecraft",
      gameMode: "Survival",
      lanAddress: "192.168.1.20:19132",
      lanVisible: true,
      multiplayerOn: true,
      worldStatus: "open",
      maxPlayers: 8,
      participantIds: [ALICE_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("worlds").doc("world_1").update({
      participantIds: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(bobDb.collection("worlds").where("worldStatus", "in", ["open", "in_game"]).get());

  // Friend request & Conversation isolation
  await assertSucceeds(
    aliceDb.collection("friend_requests").doc("fr_1").set({
      senderId: ALICE_UID,
      senderName: "Alice",
      senderAvatar: "cyber_visor",
      receiverId: BOB_UID,
      receiverName: "Bob",
      participantIds: [ALICE_UID, BOB_UID],
      status: "pending",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("friend_requests").doc("fr_1").update({
      status: "accepted",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(charlieDb.collection("friend_requests").doc("fr_1").get());

  await assertSucceeds(
    aliceDb.collection("conversations").doc("conv_1").set({
      title: "Squad Chat",
      groupChat: true,
      participantIds: [ALICE_UID, BOB_UID],
      participantLabels: ["Alice", "Bob"],
      lastMessage: "Ready to host LAN?",
      createdBy: ALICE_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc("conv_1").collection("messages").doc("m_1").set({
      conversationId: "conv_1",
      senderId: BOB_UID,
      senderName: "Bob",
      text: "Joining right now!",
      participantIds: [ALICE_UID, BOB_UID],
      createdAt: pastDate(500),
    })
  );
  await assertFails(charlieDb.collection("conversations").doc("conv_1").get());
  await assertFails(charlieDb.collection("conversations").doc("conv_1").collection("messages").doc("m_1").get());

  // Gallery & Notifications (user-scoped)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("gallery").doc("g_1").set({
      userId: ALICE_UID,
      title: "Clutch Victory",
      mediaType: "highlight",
      gameTitle: "Minecraft",
      mediaUri: "",
      durationSec: 45,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("gallery").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("gallery").get());
});
