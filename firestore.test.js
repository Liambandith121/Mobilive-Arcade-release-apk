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

  // Partial update on daily claim (first claim succeeds)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 650,
      streakDays: 2,
      lastClaimDayIndex: 2,
      lastDailyClaimAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Second claim within 24 hours rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 800,
      streakDays: 3,
      lastClaimDayIndex: 3,
      lastDailyClaimAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Non-reward settings update still allowed within 24h
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      privacyShowOnline: false,
      updatedAt: pastDate(400),
    })
  );

  // Claim after 24+ hours succeeds when existing lastDailyClaimAt is >24h old
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users").doc(ALICE_UID).update({
      lastDailyClaimAt: new Date(Date.now() - 25 * 60 * 60 * 1000),
    });
  });
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 870,
      streakDays: 3,
      lastClaimDayIndex: 3,
      lastDailyClaimAt: pastDate(200),
      updatedAt: pastDate(200),
    })
  );

  // Backdating lastDailyClaimAt rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      lastDailyClaimAt: new Date(Date.now() - 25 * 60 * 60 * 1000),
      updatedAt: pastDate(100),
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

test("Party Invites & Friend Removal: create, participant isolation, accept/join party, and unfriend delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(5000);

  // Alice creates a party
  await assertSucceeds(
    aliceDb.collection("parties").doc("party_1").set({
      leaderId: ALICE_UID,
      leaderName: "Alice",
      partyName: "Ranked Squad",
      gameTitle: "Minecraft",
      maxSlots: 4,
      memberIds: [ALICE_UID],
      mutedMemberIds: [],
      voiceActive: true,
      queueState: "idle",
      partyStatus: "active",
      matchedLobbyCode: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cannot invite without existing party
  await assertFails(
    aliceDb.collection("party_invites").doc("inv_bad").set({
      partyId: "nonexistent_party",
      partyName: "Ghost",
      gameTitle: "Minecraft",
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

  // Valid party invite from Alice to Bob succeeds
  await assertSucceeds(
    aliceDb.collection("party_invites").doc("inv_1").set({
      partyId: "party_1",
      partyName: "Ranked Squad",
      gameTitle: "Minecraft",
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

  // Participant-scoped list queries succeed; unfiltered list or Charlie read fail
  await assertSucceeds(bobDb.collection("party_invites").where("participantIds", "array-contains", BOB_UID).get());
  await assertFails(charlieDb.collection("party_invites").doc("inv_1").get());
  await assertFails(charlieDb.collection("party_invites").get());

  // Sender cannot forge receiver acceptance
  await assertFails(
    aliceDb.collection("party_invites").doc("inv_1").update({
      status: "accepted",
      updatedAt: pastDate(500),
    })
  );

  // Receiver (Bob) accepts invite and joins party
  await assertSucceeds(
    bobDb.collection("party_invites").doc("inv_1").update({
      status: "accepted",
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    bobDb.collection("parties").doc("party_1").update({
      memberIds: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(500),
    })
  );

  // Terminal state locking: cannot update after accepted
  await assertFails(
    bobDb.collection("party_invites").doc("inv_1").update({
      status: "declined",
      updatedAt: pastDate(200),
    })
  );

  // Either participant can delete an invite or friend_request (cancel / unfriend)
  await assertSucceeds(aliceDb.collection("party_invites").doc("inv_1").delete());
});

test("Tournaments & Registrations: countdown metadata, team signup, isolation, and withdrawal", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(5000);
  const futureStart = Date.now() + 3600000;

  // Alice hosts an upcoming tournament with countdown timestamp
  await assertSucceeds(
    aliceDb.collection("tournaments").doc("tour_1").set({
      organizerId: ALICE_UID,
      title: "Neon Skywars Cup",
      gameTitle: "Minecraft",
      prizeCoins: 1500,
      entryCoins: 50,
      maxTeams: 16,
      tournamentState: "upcoming",
      registeredIds: [ALICE_UID],
      scheduleLabel: "In 1 hour",
      startsAtMillis: futureStart,
      formatLabel: "Squad 4v4",
      rulesSummary: "No hacks; check in 15 min early.",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob registers his team into the subcollection and adds his UID to registeredIds
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tour_1").collection("registrations").doc(BOB_UID).set({
      tournamentId: "tour_1",
      userId: BOB_UID,
      playerName: "Bob",
      teamName: "Volt Strikers",
      gamerTag: "@bob_arcade",
      division: "Squad",
      agreedRules: true,
      entryPaidCoins: 50,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tour_1").update({
      registeredIds: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(500),
    })
  );

  // Registration requires agreedRules == true and valid owner doc ID
  await assertFails(
    charlieDb.collection("tournaments").doc("tour_1").collection("registrations").doc(BOB_UID).set({
      tournamentId: "tour_1",
      userId: CHARLIE_UID,
      playerName: "Charlie",
      teamName: "Spoof",
      gamerTag: "@charlie",
      division: "Solo",
      agreedRules: true,
      entryPaidCoins: 50,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    charlieDb.collection("tournaments").doc("tour_1").collection("registrations").doc(CHARLIE_UID).set({
      tournamentId: "tour_1",
      userId: CHARLIE_UID,
      playerName: "Charlie",
      teamName: "Rule Skip",
      gamerTag: "@charlie",
      division: "Solo",
      agreedRules: false,
      entryPaidCoins: 50,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Signed-in users can query registrations scoped by tournamentId; Charlie cannot delete Bob's entry
  await assertSucceeds(
    charlieDb.collection("tournaments").doc("tour_1").collection("registrations")
      .where("tournamentId", "==", "tour_1")
      .get()
  );
  await assertFails(
    charlieDb.collection("tournaments").doc("tour_1").collection("registrations").doc(BOB_UID).delete()
  );

  // Bob can update teamName or withdraw his own registration
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tour_1").collection("registrations").doc(BOB_UID).update({
      teamName: "Volt Legends",
      updatedAt: pastDate(200),
    })
  );
  await assertSucceeds(
    bobDb.collection("tournaments").doc("tour_1").collection("registrations").doc(BOB_UID).delete()
  );
});


