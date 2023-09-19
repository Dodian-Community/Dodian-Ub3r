package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.comm.LoginManager;
import net.dodian.uber.comm.PacketData;
import net.dodian.uber.comm.SocketHandler;
import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.ShopHandler;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.combat.impl.CombatStyleHandler;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.npc.NpcDrop;
import net.dodian.uber.game.model.entity.npc.NpcUpdating;
import net.dodian.uber.game.model.item.*;
import net.dodian.uber.game.model.object.DoorHandler;
import net.dodian.uber.game.model.object.RS2Object;
import net.dodian.uber.game.model.player.content.Skillcape;
import net.dodian.uber.game.model.player.packets.OutgoingPacket;
import net.dodian.uber.game.model.player.packets.PacketHandler;
import net.dodian.uber.game.model.player.packets.outgoing.*;
import net.dodian.uber.game.model.player.quests.QuestSend;
import net.dodian.uber.game.model.player.skills.Agility;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.fletching.Fletching;
import net.dodian.uber.game.model.player.skills.prayer.Prayer;
import net.dodian.uber.game.model.player.skills.prayer.Prayers;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask;
import net.dodian.uber.game.party.RewardItem;
import net.dodian.uber.game.security.*;
import net.dodian.utilities.*;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.dodian.uber.game.combat.ClientExtensionsKt.getRangedStr;
import static net.dodian.uber.game.combat.PlayerAttackCombatKt.*;
import static net.dodian.utilities.DatabaseKt.*;
import static net.dodian.utilities.DotEnvKt.*;

public class Client extends Player implements Runnable {

	public Fletching fletching = new Fletching();
	public boolean immune = false, loggingOut = false, loadingDone = false, reloadHp = false;
	public boolean canPreformAction = true;
	long lastBar = 0;
	public long lastSave, lastProgressSave, snaredUntil = 0;
	public boolean checkTime = false;

	public Entity target = null;
	int otherdbId = -1;
	public int convoId = -1, nextDiag = -1, npcFace = 591;
	public boolean pLoaded = false;
	public int maxQuests = QuestSend.values().length;
	public int[] quests = new int[maxQuests];
	public String failer = "";
	Date now = new Date();
	public long mutedHours;
	public long mutedTill;
	public long rightNow = now.getTime();
	public boolean mining = false;
	public boolean stringing = false;
	public boolean filling = false;
	public int boneItem = -1;
	public int mineIndex = 0, minePick = 0;
	public long lastDoor = 0;
	public int clientPid = -1;
	public long session_start = 0;
	public boolean pickupWanted = false, duelWin = false;
	public int pickX, pickY, pickId, pickTries;
	public CopyOnWriteArrayList<Friend> friends = new CopyOnWriteArrayList<>();
	public CopyOnWriteArrayList<Friend> ignores = new CopyOnWriteArrayList<>();
	public boolean tradeLocked = false;
	public boolean officialClient = true;
	/*
	 * Danno: Last row all disabled. As none have effect.
	 */
	public int[] duelButtons = {26069, 26070, 26071, 30136, 2158, 26065 /* 26072, 26073, 26074, 26066, 26076 */};
	public String[] duelNames = {"No Ranged", "No Melee", "No Magic", "No Gear Change", "Fun Weapons", "No Deflect Damage",
			"No Drinks", "No Food", "No prayer", "No Movement", "Obstacles"};
	/*
	 * Danno: Last row all disabled. As none have effect.
	 */
	public boolean[] duelRule = {false, false, false, false, false, true, true, true, true, true, true};

	/*
	 * Danno: Testing for armor restriction rules.
	 */
	private final boolean[] duelBodyRules = new boolean[11];

	private final int[] trueSlots = {0, 1, 2, 13, 3, 4, 5, 7, 12, 10, 9};
	private final int[] falseSlots = {0, 1, 2, 4, 5, 6, -1, 7, -1, 10, 9, -1, 9, 3};
	private final int[] stakeConfigId = new int[23];
	public int[] duelLine = {6698, 6699, 6697, 7817, 669, 6696, 6701, 6702, 6703, 6704, 6731};
	public boolean duelRequested = false, inDuel = false, duelConfirmed = false, duelConfirmed2 = false,
			duelFight = false;
	public int duel_with = 0;
	public boolean tradeRequested = false, inTrade = false, canOffer = true, tradeConfirmed = false,
			tradeConfirmed2 = false, tradeResetNeeded = false;
	public int trade_reqId = 0;
	public CopyOnWriteArrayList<GameItem> offeredItems = new CopyOnWriteArrayList<>();
	public CopyOnWriteArrayList<GameItem> otherOfferedItems = new CopyOnWriteArrayList<>();
	public boolean adding = false;
	public ArrayList<RS2Object> objects = new ArrayList<>();
	public long animationReset = 0, lastButton = 0;
	public int cookAmount = 0, cookIndex = 0, enterAmountId = 0;
	public boolean cooking = false;
	// Dodian: fishing
	int fishIndex;
	boolean fishing = false;
	// Dodian: teleports
	int tX = 0, tY = 0, tStage = 0, tH = 0, tEmote = 0;
	// Dodian: crafting
	boolean crafting = false;
	int cItem = -1;
	int cAmount = 0;
	int cLevel = 1;
	int cExp = 0;
	public int cSelected = -1, cIndex = -1;
	public String dMsg = "";

	public boolean spinning = false;
	public int dialogInterface = 2459;
	public boolean fletchings = false, fletchingOther = false;
	public int fletchId = -1, fletchAmount = -1, fletchLog = -1, originalS = -1, fletchExp = 0;
	public int fletchOtherId1 = -1, fletchOtherId2 = -1, fletchOtherId3 = -1,
			fletchOtherAmount = -1, fletchOtherAmt = -1, fletchOtherXp = -1;
	public long fletchOtherTime = 0;
	public boolean smelting = false;
	public int smelt_id, smeltCount, smeltExperience;

	public boolean shafting = false;
	public int random_skill = -1;
	public String[] otherGroups = new String[10];
	public int autocast_spellIndex = -1;
	public int loginDelay = 0;
	public boolean validClient = true, muted = false;
	public int newPms = 0;

	public int[] requiredLevel = {1, 10, 20, 30, 40, 50, 60, 70, 74, 76, 80, 82, 86, 88, 92,
			94, 96};

	public int[] baseDamage = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32};
	public String[] spellName = {"Smoke Rush", "Shadow Rush", "Blood Rush", "Ice Rush",
			"Smoke Burst", "Shadow Burst", "Blood Burst", "Ice Burst",
			"Smoke Blitz", "Shadow Blitz", "Blood Blitz", "Ice Blitz",
			"Smoke Barrage", "Shadow Barrage", "Blood Barrage", "Ice Barrage"};
	public int[] ancientId = {12939, 12987, 12901, 12861, 12963, 13011, 12919, 12881, 12951, 12999, 12911, 12871, 12975, 13023, 12929, 12891};
	public long[] coolDown = {2400, 2400, 3000, 3000};
	public int[] ancientButton = {51133, 51185, 51091, 24018, 51159, 51211, 51111, 51069, 51146, 51198, 51102, 51058, 51172, 51224, 51122, 51080};
	public String properName = "";
	public int actionButtonId = 0;
	public long lastAttack = 0;
	public long[] globalCooldown = new long[10];
	public boolean validLogin = false;

	public void ReplaceObject2(Position pos, int NewObjectID, int Face, int ObjectType) {
		if (!withinDistance(new int[]{pos.getX(), pos.getY(), 60}) || getPosition().getZ() != pos.getZ())
			return;
		send(new SetMap(pos));
		getOutputStream().createFrame(101);
		getOutputStream().writeByteC((ObjectType << 2) + (Face & 3));
		getOutputStream().writeByte(0);
		/* CREATE OBJECT */
		if (NewObjectID != -1) {
			getOutputStream().createFrame(151);
			getOutputStream().writeByteS(0);
			getOutputStream().writeWordBigEndian(NewObjectID);
			getOutputStream().writeByteS((ObjectType << 2) + (Face & 3));
		}
	}

	/**
	 * @param o 0 = X | 1 = Y | = Distance allowed.
	 */
	private boolean withinDistance(int[] o) {
		int dist = o[2];
		int deltaX = o[0] - getPosition().getX(), deltaY = o[1] - getPosition().getY();
		return (deltaX <= (dist - 1) && deltaX >= -dist && deltaY <= (dist - 1) && deltaY >= -dist);
	}

	public boolean wearing = false;

	public int resetanim = 8;

	public void refreshSkill(Skill skill) {
		try {
			int out = Skills.getLevelForExperience(getExperience(skill));
			if (skill == Skill.HITPOINTS) out = getCurrentHealth();
			else if (skill == Skill.PRAYER) out = getCurrentPrayer();
			else if(boostedLevel[skill.getId()] != 0) out += boostedLevel[skill.getId()];
			setSkillLevel(skill.getId(), out, getExperience(skill));
			setLevel(out, skill);
			getOutputStream().createFrame(134);
			getOutputStream().writeByte(skill.getId());
			getOutputStream().writeDWord_v1(getExperience(skill));
			getOutputStream().writeByte(out);
		} catch (Exception e) {
			//TODO: Fix this shiet up?!
		}
	}

	public int getbattleTimer(int weapon) {
		String wep = GetItemName(weapon).toLowerCase();
		int speed = 4;
		if (wep.contains("dart") || wep.contains("knife"))
			speed = 2;
		else if (wep.contains("wolfbane"))
			speed = 3;
		else if (wep.contains("longsword") || wep.contains("mace") || wep.contains("axe") && !wep.contains("dharok")
				|| wep.contains("spear") || wep.contains("tzhaar-ket-em") || wep.contains("torag") || wep.contains("guthan")
				|| wep.contains("verac") || wep.contains("staff") && !wep.contains("ahrim") || wep.contains("composite")
				|| wep.contains("crystal") || wep.contains("thrownaxe") || wep.contains("longbow"))
			speed = 5;
		else if (wep.contains("battleaxe") || wep.contains("warhammer") || wep.contains("godsword")
				|| wep.contains("barrelchest") || wep.contains("ahrim") || wep.contains("toktz-mej-tal")
				|| wep.contains("dorgeshuun") || wep.contains("crossbow") || wep.contains("javelin"))
			speed = 6;
		else if (wep.contains("2h sword") || wep.contains("halberd") || wep.contains("maul") || wep.contains("balmung")
				|| wep.contains("tzhaar-ket-om") || wep.contains("dharok"))
			speed = 7;
		return (int) ((speed * 1000) * 0.6);
	}

	public void CheckGear() {
		boolean foundStaff = false;
		for (int a = 0; a < staffs.length && !foundStaff; a++) {
			if (getEquipment()[Equipment.Slot.WEAPON.getId()] == staffs[a])
				foundStaff = true;
		}
		if (!foundStaff)
			autocast_spellIndex = -1;
		checkBow();
	}

	public int distanceToPoint(int pointX, int pointY) {
		return (int) Math.sqrt(Math.pow(getPosition().getX() - pointX, 2) + Math.pow(getPosition().getY() - pointY, 2));
	}

	public void animation(int id, Position pos) {
		for (int i = 0; i < PlayerHandler.players.length; i++) {
			Player p = PlayerHandler.players[i];
			if (p != null) {
				Client person = (Client) p;
				if (person.distanceToPoint(pos.getX(), pos.getY()) <= 60 && pos.getZ() == getPosition().getZ())
					person.animation2(id, pos);
			}
		}
	}

	public void animation2(int id, Position pos) {
		send(new SetMap(pos));
		getOutputStream().createFrame(4);
		getOutputStream().writeByte(0);
		getOutputStream().writeWord(id);
		getOutputStream().writeByte(0);
		getOutputStream().writeWord(0);
	}

	public void stillgfx(int id, Position pos, int time) {
		// for (Player p : server.playerHandler.players) {
		for (int i = 0; i < PlayerHandler.players.length; i++) {
			Player p = PlayerHandler.players[i];
			if (p != null) {
				Client person = (Client) p;
				if (person.distanceToPoint(pos.getX(), pos.getY()) <= 60 && getPosition().getZ() == pos.getZ())
					person.stillgfx2(id, pos, 0, time);
			}
		}
	}

	public void stillgfx(int id, int y, int x) {
		stillgfx(id, new Position(x, y, getPosition().getZ()), 0);
	}

	public void stillgfx2(int id, Position pos, int height, int time) {
		send(new SetMap(pos));
		getOutputStream().createFrame(4);
		getOutputStream().writeByte(0); // Tiles away (X >> 4 + Y & 7)
		getOutputStream().writeWord(id); // Graphic id
		getOutputStream().writeByte(height); // height of the spell above it's basic
		// place, i think it's written in pixels
		// 100 pixels higher
		getOutputStream().writeWord(time); // Time before casting the graphic
	}

	public boolean AnimationReset; // Resets Animations With The Use Of The
	// ActionTimer

	public void createProjectile(int casterY, int casterX, int offsetY,
								 int offsetX, int angle, int speed, int gfxMoving, int startHeight,
								 int endHeight, int MageAttackIndex, int begin, int slope, int initDistance) {
		try {
			send(new SetMap(new Position(casterY, casterX)));
			getOutputStream().createFrame(117);
			getOutputStream().writeByte(angle); // Starting place of the projectile
			getOutputStream().writeByte(offsetY); // Distance between caster and enemy
			// Y
			getOutputStream().writeByte(offsetX); // Distance between caster and enemy
			// X
			getOutputStream().writeWord(MageAttackIndex); // The NPC the missle is
			// locked on to
			getOutputStream().writeWord(gfxMoving); // The moving graphic ID
			getOutputStream().writeByte(startHeight); // The starting height
			getOutputStream().writeByte(endHeight); // Destination height
			getOutputStream().writeWord(begin); // Time the missle is created
			getOutputStream().writeWord(speed); // Speed minus the distance making it
			// set
			getOutputStream().writeByte(slope); // Initial slope
			getOutputStream().writeByte(initDistance); // Initial distance from source (in the
			// direction of the missile) //64
		} catch (Exception e) {
			Server.logError(e.getMessage());
		}
	}

	public void arrowGfx(int offsetY, int offsetX, int angle, int speed,
						 int gfxMoving, int startHeight, int endHeight, int index, int begin, int slope) {
		for (int a = 0; a < Constants.maxPlayers; a++) {
			Client projCheck = (Client) PlayerHandler.players[a];
			if (projCheck != null && projCheck.dbId > 0 && projCheck.getPosition().getX() > 0 && !projCheck.disconnected
					&& Math.abs(getPosition().getX() - projCheck.getPosition().getX()) <= 60
					&& Math.abs(getPosition().getY() - projCheck.getPosition().getY()) <= 60) {
				projCheck.createProjectile(getPosition().getY(), getPosition().getX(), offsetY, offsetX, angle, speed, gfxMoving,
						startHeight, endHeight, index, begin, slope, 64);
			}
		}
	}

	public void println_debug(String str) {
		String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
		System.out.println("[" + timestamp + "] [client-" + getSlot() + "-" + getPlayerName() + "]: " + str);
	}

	public void println(String str) {
		String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
		System.out.println("[" + timestamp + "] [client-" + getSlot() + "-" + getPlayerName() + "]: " + str);
	}

	public void rerequestAnim() {
		requestAnim(-1, 0);
		getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
	}

	public void sendFrame200(int MainFrame, int SubFrame) {
		getOutputStream().createFrame(200);
		getOutputStream().writeWord(MainFrame);
		getOutputStream().writeWord(SubFrame);
		//flushOutStream();
	}

	public void sendFrame164(int Frame) {
		getOutputStream().createFrame(164);
		getOutputStream().writeWordBigEndian_dup(Frame);
		//flushOutStream();
	}

	public void sendFrame246(int MainFrame, int SubFrame, int SubFrame2) {
		getOutputStream().createFrame(246);
		getOutputStream().writeWordBigEndian(MainFrame);
		getOutputStream().writeWord(SubFrame);
		getOutputStream().writeWord(SubFrame2);
		//flushOutStream();
	}

	public void sendFrame185(int Frame) {
		getOutputStream().createFrame(185);
		getOutputStream().writeWordBigEndianA(Frame);
		//flushOutStream();
	}

	public void sendQuestSomething(int id) {
		getOutputStream().createFrame(79);
		getOutputStream().writeWordBigEndian(id);
		getOutputStream().writeWordA(0);
		//flushOutStream();
	}

	public void clearQuestInterface() {
		for (int j : QuestInterface)
			send(new SendString("", j));
	}

	public void showInterface(int interfaceid) {
		resetAction();
		getOutputStream().createFrame(97);
		getOutputStream().writeWord(interfaceid);
		//flushOutStream();
	}

	public int ancients = 1;
	public int newheightLevel = 0;
	public int[] QuestInterface = {8145, 8147, 8148, 8149, 8150, 8151, 8152, 8153, 8154, 8155, 8156, 8157, 8158, 8159,
			8160, 8161, 8162, 8163, 8164, 8165, 8166, 8167, 8168, 8169, 8170, 8171, 8172, 8173, 8174, 8175, 8176, 8177, 8178,
			8179, 8180, 8181, 8182, 8183, 8184, 8185, 8186, 8187, 8188, 8189, 8190, 8191, 8192, 8193, 8194, 8195, 12174,
			12175, 12176, 12177, 12178, 12179, 12180, 12181, 12182, 12183, 12184, 12185, 12186, 12187, 12188, 12189, 12190,
			12191, 12192, 12193, 12194, 12195, 12196, 12197, 12198, 12199, 12200, 12201, 12202, 12203, 12204, 12205, 12206,
			12207, 12208, 12209, 12210, 12211, 12212, 12213, 12214, 12215, 12216, 12217, 12218, 12219, 12220, 12221, 12222,
			12223};

	public int bonusSpec = 0, animationSpec = 0, emoteSpec = 0;
	public boolean specsOn = true;

	public int[] statId = {10252, 11000, 10253, 11001, 10254, 11002, 10255, 11011, 11013, 11014, 11010, 11012, 11006,
			11009, 11008, 11004, 11003, 11005, 47002, 54090, 11007};
	public String[] BonusName = {"Stab", "Slash", "Crush", "Magic", "Range", "Stab", "Slash", "Crush", "Prayer", "Range",
			"Str", "Spell Dmg"};

	// public int pGender;
	public int i;
	// public int gender;

	public int XremoveSlot = 0;
	public int XinterfaceID = 0;
	public int XremoveID = 0;

	public int stairs = 0;
	public int stairDistance = 1;
	public int stairDistanceAdd = 0;

	public int[] woodcutting = {0, 0, 0, 1, -1, 3};
	public int[] smithing = {0, 0, 0, -1, -1, 0};

	public int skillX = -1;
	public int skillY = -1;
	public int CombatExpRate = 1;

	public int WanneBank = 0;
	public int WanneShop = 0;
	public int WanneThieve = 0;

	public static final int bufferSize = 1000000;
	public java.net.Socket mySock;
	public Stream inputStream, outputStream;
	public byte[] buffer;
	public int readPtr, writePtr;
	public Cryption inStreamDecryption = null, outStreamDecryption = null;

	public int timeOutCounter = 0; // to detect timeouts on the connection to
	// the client

	public int returnCode = 2; // Tells the client if the login was successfull

	private SocketHandler mySocketHandler;

	public Client(java.net.Socket s, int _playerId) {
		super(_playerId);
		mySock = s;
		mySocketHandler = new SocketHandler(this, s);
		outputStream = new Stream(new byte[bufferSize]);
		outputStream.currentOffset = 0;
		inputStream = new Stream(new byte[bufferSize]);
		inputStream.currentOffset = 0;
		readPtr = writePtr = 0;
	}

	public void shutdownError(String errorMessage) {
		Utils.println(": " + errorMessage);
		destruct();
	}

	public void destruct() {
		if (mySock == null) {
			return;
		} // already shutdown
		try {
			if (saveNeeded && !tradeSuccessful) { //Attempt to fix a potential dupe?
				saveStats(true, true);
			}
			//ConnectionList.getInstance().remove(mySock.getInetAddress()); //Do we need?!
			disconnected = true;
			mySock.close();
			mySock = null;
			mySocketHandler = null;
			inputStream = null;
			outputStream = null;
			isActive = false;
			buffer = null;
		} catch (java.io.IOException ioe) {
			System.out.println("something wrong with the destruct!");
		}
		super.destruct();
	}

	public Stream getInputStream() {
		return this.inputStream;
	}

	public Stream getOutputStream() {
		return this.outputStream;
	}

	public void send(OutgoingPacket packet) {
		packet.send(this);
	}

	// writes any data in outStream to the relaying buffer
	public void flushOutStream() {
		if (disconnected || getOutputStream().currentOffset == 0) {
			return;
		}
			Integer length = getOutputStream().currentOffset;
			byte[] copy = new byte[length];
			System.arraycopy(getOutputStream().buffer, 0, copy, 0, copy.length);
			mySocketHandler.queueOutput(copy);
			getOutputStream().currentOffset = 0;
	}

	// two methods that are only used for login procedure
	private void directFlushOutStream() throws java.io.IOException {
		mySocketHandler.getOutput().write(getOutputStream().buffer, 0, getOutputStream().currentOffset);
		getOutputStream().currentOffset = 0; // reset
	}

	private void fillInStream(int forceRead) throws java.io.IOException {
		getInputStream().currentOffset = 0;
		mySocketHandler.getInput().read(getInputStream().buffer, 0, forceRead);
	}

	private void fillInStream(PacketData pData) throws java.io.IOException {
		getInputStream().currentOffset = 0;
		getInputStream().buffer = pData.getData();
		currentPacket = pData;
	}

	public void run() {
		// we just accepted a new connection - handle the login stuff
		isActive = false;
		long serverSessionKey, clientSessionKey;

//	if (!KeyServer.verifiedKeys()){
//		System.out.println("User rejected due to unverified client.");
//		disconnected = true;
//		returnCode = 4;
//	}

		// randomize server part of the session key
		serverSessionKey = ((long) (java.lang.Math.random() * 99999999D) << 32)
				+ (long) (java.lang.Math.random() * 99999999D);

		try {
			returnCode = 2;
			fillInStream(2);
			if (getInputStream().readUnsignedByte() != 14) {
				shutdownError("Expected login Id 14 from client.");
				disconnected = true;
				return;
			}
			getInputStream().readUnsignedByte();
			for (int i = 0; i < 8; i++) {
				mySocketHandler.getOutput().write(10);
			}
			mySocketHandler.getOutput().write(0);
			//out.write(0);
			getOutputStream().writeQWord(serverSessionKey);
			directFlushOutStream();
			fillInStream(2);
			int loginType = getInputStream().readUnsignedByte(); // this is either 16
			if (loginType != 16 && loginType != 18) {
				shutdownError("Unexpected login type " + loginType);
				return;
			}
			int loginPacketSize = getInputStream().readUnsignedByte();
			int loginEncryptPacketSize = loginPacketSize - (36 + 1 + 1 + 2); // the
			if (loginEncryptPacketSize <= 0) {
				shutdownError("Zero RSA packet size!");
				return;
			}
			fillInStream(loginPacketSize);
			if (getInputStream().readUnsignedByte() != 255 || getInputStream().readUnsignedWord() != 317) {
				returnCode = 6;
			}
			getInputStream().readUnsignedByte();
			for (int i = 0; i < 9; i++) { //Client shiet?!
				getInputStream().readDWord();
			}

			loginEncryptPacketSize--; // don't count length byte
			int tmp = getInputStream().readUnsignedByte();
			if (loginEncryptPacketSize != tmp) {
				shutdownError("Encrypted packet data length (" + loginEncryptPacketSize
						+ ") different from length byte thereof (" + tmp + ")");
				return;
			}
			tmp = getInputStream().readUnsignedByte();
			if (tmp != 10) {
				shutdownError("Encrypted packet Id was " + tmp + " but expected 10");
				return;
			}
			clientSessionKey = getInputStream().readQWord();
			serverSessionKey = getInputStream().readQWord();

			String customClientVersion = getInputStream().readString();
			officialClient = customClientVersion.equals(getGameClientCustomVersion());
			setPlayerName(getInputStream().readString());
			if (getPlayerName() == null || getPlayerName().isEmpty()) {
				setPlayerName("player" + getSlot());
			}
			playerPass = getInputStream().readString();
			String playerServer;
			try {
				playerServer = getInputStream().readString();
			} catch (Exception e) {
				playerServer = "play.dodian.com";
			}
			setPlayerName(getPlayerName().toLowerCase());
			// playerPass = playerPass.toLowerCase();
			// System.out.println("valid chars");
			char[] validChars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
					's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
					'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
					'_', ' '};
			setPlayerName(getPlayerName().trim());

			int[] sessionKey = new int[4];
			sessionKey[0] = (int) (clientSessionKey >> 32);
			sessionKey[1] = (int) clientSessionKey;
			sessionKey[2] = (int) (serverSessionKey >> 32);
			sessionKey[3] = (int) serverSessionKey;
			inStreamDecryption = new Cryption(sessionKey);
			for (int i = 0; i < 4; i++) {
				sessionKey[i] += 50;
			}
			outStreamDecryption = new Cryption(sessionKey);
			getOutputStream().packetEncryption = outStreamDecryption;

			int letters = 0;
			for (int i = 0; i < getPlayerName().length(); i++) {
				boolean valid = false;
				for (char validChar : validChars) {
					if (getPlayerName().charAt(i) == validChar) {
						valid = true;
						// break;
					}
					if (valid && getPlayerName().charAt(i) != '_' && getPlayerName().charAt(i) != ' ') {
						letters++;
					}
				}
				if (!valid) {
					returnCode = 4;
					disconnected = true;
				}
			}
			if (letters < 1) {
				returnCode = 3;
				disconnected = true;
			}
			char first = getPlayerName().charAt(0);
			properName = Character.toUpperCase(first) + getPlayerName().substring(1).toLowerCase();
			setPlayerName(properName.replace("_", " "));
			longName = Utils.playerNameToInt64(getPlayerName());
			if (Server.updateRunning && (Server.updateStartTime + (Server.updateSeconds * 1000L)) - System.currentTimeMillis() < 60000) { //Checks if update!
				returnCode = 14;
				disconnected = true;
			}
			int loadgame = Server.loginManager.loadgame(this, getPlayerName(), playerPass);
			switch (playerGroup) {
				case 6: // root admin
				case 18: // root admin
				case 10: // content dev
					playerRights = 2;
					premium = true;
					break;
				case 9: // player moderator
				case 5: // global mod
					playerRights = 1;
					premium = true;
					break;
				default:
					if(playerGroup == 2)
						Server.loginManager.updatePlayerForumRegistration(this);
					premium = true;
					playerRights = 0;
			}
			for (String otherGroup : otherGroups) {
				if (otherGroup == null) {
					continue;
				}
				String temp = otherGroup.trim();
				if (temp.length() > 0) {
					int group = Integer.parseInt(temp);
					switch (group) {
						case 14:
							premium = true;
							break;
						case 3:
						case 19:
							playerRights = 1;
							break;
					}
				}
			}
			for (int i = 0; i < getEquipment().length; i++) {
				if (getEquipment()[i] == 0) {
					getEquipment()[i] = -1;
					getEquipmentN()[i] = 0;
				}
			}
			if (loadgame == 0 && returnCode != 6) {
				validLogin = true;
				if (getPosition().getX() > 0 && getPosition().getY() > 0) {
					teleportToX = getPosition().getX();
					teleportToY = getPosition().getY();
				}
			} else {
				if (returnCode != 6 && returnCode != 5)
					returnCode = loadgame;
				disconnected = true;
				teleportToX = 0;
				teleportToY = 0;
			}
			if (getSlot() == -1) {
				mySocketHandler.getOutput().write(7);
			} else if (playerServer.equals("INVALID")) {
				mySocketHandler.getOutput().write(10);
			} else {
				if (mySocketHandler.getOutput() != null)
					mySocketHandler.getOutput().write(returnCode); // login response (1: wait 2seconds,
					// 2=login successfull, 4=ban :-)
				else
					returnCode = 21;
				if (returnCode == 21)
					mySocketHandler.getOutput().write(loginDelay);
			}
			mySocketHandler.getOutput().write(getGameWorldId() > 1 && playerRights < 2 ? 2 : playerRights); // mod level
			mySocketHandler.getOutput().write(0);
			getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
		} catch (java.lang.Exception __ex) {
			destruct();
			return;
		}
		if (getSlot() == -1 || returnCode != 2) {
			return;
		}
		isActive = true;
		Thread mySocketThread = Server.createNewConnection(mySocketHandler);
		mySocketThread.start();
		packetSize = 0;
		packetType = -1;
		readPtr = 0;
		writePtr = 0;
	}

	public void setSidebarInterface(int menuId, int form) {
		getOutputStream().createFrame(71);
		getOutputStream().writeWord(form);
		getOutputStream().writeByteA(menuId);
	}

	public void setSkillLevel(int skillNum, int currentLevel, int XP) {
		if (skillNum == 0) {
			send(new SendString(String.valueOf(currentLevel), 4004));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4005));
		}
		if (skillNum == 2) {
			send(new SendString(String.valueOf(currentLevel), 4006));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4007));
		}
		if (skillNum == 1) {
			send(new SendString(String.valueOf(currentLevel), 4008));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4009));
		}
		if (skillNum == 4) {
			send(new SendString(String.valueOf(currentLevel), 4010));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4011));
		}
		if (skillNum == 5) {
			send(new SendString(String.valueOf(currentLevel), 4012));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4013));
		}
		if (skillNum == 6) {
			send(new SendString(String.valueOf(currentLevel), 4014));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4015));
		}
		if (skillNum == 3) {
			send(new SendString(String.valueOf(currentLevel), 4016));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4017));
		}
		if (skillNum == 16) {
			send(new SendString(String.valueOf(currentLevel), 4018));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4019));
		}
		if (skillNum == 15) {
			send(new SendString(String.valueOf(currentLevel), 4020));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4021));
		}
		if (skillNum == 17) {
			send(new SendString(String.valueOf(currentLevel), 4022));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4023));
		}
		if (skillNum == 12) {
			send(new SendString(String.valueOf(currentLevel), 4024));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4025));
		}
		if (skillNum == 9) {
			send(new SendString(String.valueOf(currentLevel), 4026));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4027));
		}
		if (skillNum == 14) {
			send(new SendString(String.valueOf(currentLevel), 4028));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4029));
		}
		if (skillNum == 13) {
			send(new SendString(String.valueOf(currentLevel), 4030));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4031));
		}
		if (skillNum == 10) {
			send(new SendString(String.valueOf(currentLevel), 4032));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4033));
		}
		if (skillNum == 7) {
			send(new SendString(String.valueOf(currentLevel), 4034));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4035));
		}
		if (skillNum == 11) {
			send(new SendString(String.valueOf(currentLevel), 4036));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4037));
		}
		if (skillNum == 8) {
			send(new SendString(String.valueOf(currentLevel), 4038));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4039));
		}
		if (skillNum == 20) {
			send(new SendString(String.valueOf(currentLevel), 4152));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 4153));
		}
		if (skillNum == 18) {
			send(new SendString(String.valueOf(currentLevel), 12166));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 12167));
		}
		if (skillNum == 19) {
			send(new SendString(String.valueOf(currentLevel), 13926));
			send(new SendString(String.valueOf(Skills.getLevelForExperience(XP)), 13927));
		}
	}

	public void logout() {
		// declineDuel();
		if (!saveNeeded) {
			return;
		}
		if (UsingAgility) {
			xLog = true;
			return;
		}
		if (!validClient) {
			return;
		}
		saveStats(true, true);
		saveNeeded = false;
		//ConnectionList.getInstance().remove(mySock.getInetAddress());
		send(new SendMessage("Please wait... logging out may take time"));
		send(new SendString("     Please wait...", 2458));
		send(new SendString("Click here to logout", 2458));
		getOutputStream().createFrame(109);
		loggingOut = true;
	}

	/*
	 * public void saveStats(boolean logout){ server.login.saveStats(this,
	 * logout); if(logout){ long elapsed = System.currentTimeMillis() -
	 * session_start; server.login.sendSession(dbId, clientPid, elapsed,
	 * connectedFrom); } }
	 */

	public void saveStats(boolean logout, boolean updateProgress) {
		if (loginDelay > 0) {
			println("Incomplete login, aborting save");
			return;
		}
		if (!loadingDone || !validLogin) {
			return;
		}
		if (getPlayerName() == null || getPlayerName().equals("null") || dbId < 1) {
			saveNeeded = false;
			return;
		}
		if (getPlayerName().indexOf("'") > 0 || playerPass.indexOf("`") > 0) {
			println_debug("Invalid player name");
			return;
		}
		if (logout) {
			saving = true;
      /*for (Player p : PlayerHandler.players) {
        if (p != null && !p.disconnected && p.dbId > 0) {
          if (p.getDamage().containsKey(getSlot())) {
            p.getDamage().put(getSlot(), 0);
          }
        }
      }*/ //TODO: Fix this pvp shiet
			if (getGameWorldId() < 2) {
				long elapsed = System.currentTimeMillis() - start;
				int minutes = (int) (elapsed / 60000);
				Server.login.sendSession(dbId, officialClient ? 1 : 1337, minutes, connectedFrom, start, System.currentTimeMillis());
			}
			//System.out.println("exorth save!");
			PlayerHandler.playersOnline.remove(longName);
			PlayerHandler.allOnline.remove(longName);
			for (Client c : PlayerHandler.playersOnline.values()) {
				if (c.hasFriend(longName)) {
					c.refreshFriends();
				}
			}
			if(inTrade) declineTrade();
			else if(inDuel && !duelFight) declineDuel();
			else if (duel_with > 0 && validClient(duel_with) && inDuel && duelFight) {
				Client p = getClient(duel_with);
				p.duelWin = true;
				p.DuelVictory();
			}
		}
		// TODO: Look into improving this, and potentially a system to configure player saving per world id...
		if (getGameWorldId() < 2 || getPlayerName().toLowerCase().startsWith("pro noob"))
			try {
				Statement statement = getDbConnection().createStatement();
				long allxp = 0;
				for (int i = 0; i < 21; i++) {
					if (i != 18) {
						allxp += getExperience(Skill.getSkill(i));
					}
				}
				int totallvl = totalLevel();
				String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
				StringBuilder query = new StringBuilder("UPDATE " + DbTables.GAME_CHARACTERS_STATS + " SET total=" + totallvl + ", combat=" + determineCombatLevel() + ", ");
				StringBuilder query2 = new StringBuilder("INSERT INTO " + DbTables.GAME_CHARACTERS_STATS_PROGRESS + " SET updated='" + timeStamp + "', total=" + totallvl + ", combat=" + determineCombatLevel() + ", uid=" + dbId + ", ");
				for (int i = 0; i < 21; i++) {
					query.append(Objects.requireNonNull(Skill.getSkill(i)).getName()).append("=").append(getExperience(Skill.getSkill(i))).append(", ");
					query2.append(Skill.getSkill(i).getName()).append("=").append(getExperience(Skill.getSkill(i))).append(", ");
				}
				query.append("totalxp=").append(allxp).append(" WHERE uid=").append(dbId);
				query2.append("totalxp=").append(allxp);

				statement.executeUpdate(query.toString());
				if (updateProgress) statement.executeUpdate(query2.toString());
				System.currentTimeMillis();
				StringBuilder inventory = new StringBuilder();
				StringBuilder equipment = new StringBuilder();
				StringBuilder bank = new StringBuilder();
				StringBuilder list = new StringBuilder();
				StringBuilder boss_log = new StringBuilder();
				StringBuilder prayer = new StringBuilder();
				StringBuilder boosted = new StringBuilder();
				for (int i = 0; i < playerItems.length; i++) {
					if (playerItems[i] > 0) {
						inventory.append(i).append("-").append(playerItems[i] - 1).append("-").append(playerItemsN[i]).append(" ");
					}
				}
				for (int i = 0; i < bankItems.length; i++) {
					if (bankItems[i] > 0) {
						bank.append(i).append("-").append(bankItems[i] - 1).append("-").append(bankItemsN[i]).append(" ");
					}
				}
				for (int i = 0; i < getEquipment().length; i++) {
					if (getEquipment()[i] > 0) {
						equipment.append(i).append("-").append(getEquipment()[i]).append("-").append(getEquipmentN()[i]).append(" ");
					}
				}
				for (int i = 0; i < boss_name.length; i++) {
					if (boss_amount[i] >= 0) {
						boss_log.append(boss_name[i]).append(":").append(boss_amount[i]).append(" ");
					}
				}
				prayer.append(getCurrentPrayer());
				for(Prayers.Prayer pray : Prayers.Prayer.values()) {
					if(getPrayerManager().isPrayerOn(pray))
						prayer.append(":").append(pray.getButtonId());
				}
				boosted.append(lastRecover);
				for(int boost : boostedLevel.clone()) {
					boosted.append(":").append(boost);
				}
				int num = 0;
				for (Friend f : friends) {
					if (f.name > 0 && num < 200) {
						list.append(f.name).append(" ");
						num++;
					}
				}
				if (logout)
					saveNeeded = false;

				String last = "";
				long elapsed = System.currentTimeMillis() - session_start;
				if (elapsed > 10000) {
					last = ", lastlogin = '" + System.currentTimeMillis() + "'";
				}
				statement.executeUpdate("UPDATE " + DbTables.GAME_CHARACTERS + " SET uuid= '" + LoginManager.UUID + "', lastvote=" + lastVoted + ", pkrating=" + 1500 + ", health="
						+ getCurrentHealth() + ", equipment='" + equipment + "', inventory='" + inventory + "', bank='" + bank
						+ "', friends='" + list + "', fightStyle = " + FightType + ", slayerData='" + saveTaskAsString() + "', essence_pouch='" + getPouches() + "'"
						+ ", autocast=" + autocast_spellIndex + ", news=" + latestNews + ", agility = '" + agilityCourseStage + "', height = " + getPosition().getZ() + ", x = " + getPosition().getX()
						+ ", y = " + getPosition().getY() + ", lastlogin = '" + System.currentTimeMillis() + "', Boss_Log='"
						+ boss_log + "', songUnlocked='" + getSongUnlockedSaveText() + "', travel='" + saveTravelAsString() + "', look='" + getLook() + "', unlocks='" + saveUnlocksAsString() + "'" +
						", prayer='"+prayer+"', boosted='"+boosted+"'" + last
						+ " WHERE id = " + dbId);
				statement.close();
				//println_debug("Save:  " + getPlayerName() + " (" + (System.currentTimeMillis() - start) + "ms)");
			} catch (Exception e) {
				e.printStackTrace();
				println_debug("Save Exception: " + getSlot() + ", " + getPlayerName());
			}
	}

	public void saveStats(boolean logout) {
		saveStats(logout, false);
	}

	public void fromBank(int itemID, int fromSlot, int amount) {
		if (!IsBanking) {
			send(new RemoveInterfaces());
			return;
		}
		if (bankItems[fromSlot] - 1 != itemID || (bankItems[fromSlot] - 1 != itemID && bankItemsN[fromSlot] != amount)) {
			return;
		}
		int id = GetNotedItem(itemID);
		if (amount > 0) {
			if (bankItems[fromSlot] > 0) {
				if (!takeAsNote) {
					// if (Item.itemStackable[bankItems[fromSlot] - 1]) {
					if (Server.itemManager.isStackable(itemID)) {
						if (bankItemsN[fromSlot] > amount) {
							if (addItem((bankItems[fromSlot] - 1), amount)) {
								bankItemsN[fromSlot] -= amount;
								resetBank();
								resetItems(5064);
							}
						} else {
							if (addItem(itemID, bankItemsN[fromSlot])) {
								bankItems[fromSlot] = 0;
								bankItemsN[fromSlot] = 0;
								resetBank();
								resetItems(5064);
							}
						}
					} else {
						while (amount > 0) {
							if (bankItemsN[fromSlot] > 0) {
								if (addItem(itemID, 1)) {
									bankItemsN[fromSlot] -= 1;
									amount--;
								} else {
									amount = 0;
								}
							} else {
								amount = 0;
							}
						}
						resetBank();
						resetItems(5064);
					}
				} else if (id > 0) {
					if (bankItemsN[fromSlot] > amount) {
						if (addItem(id, amount)) {
							bankItemsN[fromSlot] -= amount;
							resetBank();
							resetItems(5064);
						}
					} else {
						if (addItem(id, bankItemsN[fromSlot])) {
							bankItems[fromSlot] = 0;
							bankItemsN[fromSlot] = 0;
							resetBank();
							resetItems(5064);
						}
					}
				} else {
					send(new SendMessage(Server.itemManager.getName(itemID) + " can't be drawn as note."));
					if (Server.itemManager.isStackable(itemID)) {
						if (bankItemsN[fromSlot] > amount) {
							if (addItem(itemID, amount)) {
								bankItemsN[fromSlot] -= amount;
								resetBank();
								resetItems(5064);
							}
						} else {
							if (addItem(itemID, bankItemsN[fromSlot])) {
								bankItems[fromSlot] = 0;
								bankItemsN[fromSlot] = 0;
								resetBank();
								resetItems(5064);
							}
						}
					} else {
						while (amount > 0) {
							if (bankItemsN[fromSlot] > 0) {
								if (addItem(itemID, 1)) {
									bankItemsN[fromSlot] -= 1;
									amount--;
								} else {
									amount = 0;
								}
							} else {
								amount = 0;
							}
						}
						resetBank();
						resetItems(5064);
					}
				}
			}
		}
	}

	public int getInvAmt(int itemID) {
		int amt = 0;
		for (int slot = 0; slot < playerItems.length; slot++) {
			if (playerItems[slot] == (itemID + 1)) {
				amt += playerItemsN[slot];
			}
		}
		return amt;
	}

	public int getBankAmt(int itemID) {
		int slot = -1;
		for (int i = 0; i < playerBankSize && slot == -1; i++)
			if (bankItems[i] == itemID + 1)
				slot = i;
		return slot == -1 ? 0 : bankItemsN[slot];
	}

	public boolean giveExperience(int amount, Skill skill) {
		if (amount < 1)
			return false;
		if (randomed) {
			send(new SendMessage("You must answer the genie before you can gain experience!"));
			return false;
		}
		amount = amount * getGameMultiplierGlobalXp();
		int oldXP = getExperience(skill),
				newXP = Math.min(getExperience(skill) + amount, 200000000);
		int oldLevel = Skills.getLevelForExperience(oldXP), newLevel = Skills.getLevelForExperience(newXP);
		amount = oldXP < 200000000 && newXP == 200000000 ? 200000000 - oldXP : newXP == 200000000 ? 0 : amount;
		addExperience(amount, skill);
		int animation = -1;
		if(newLevel - oldLevel > 0) {
			animation = 199;
			if (newLevel == 99) {
				animation = 623;
				publicyell(getPlayerName() + " has just reached the max level for " + skill.getName() + "!");
			} else if (newLevel > 90)
				publicyell(getPlayerName() + "'s " + skill.getName() + " level is now " + newLevel + "!");
			send(new SendMessage("Congratulations, you just advanced " + (skill == Skill.ATTACK || skill == Skill.AGILITY ? "an" : "a") + " " + skill.getName() + " level."));
		}
		if (oldXP < 50000000 && newXP >= 50000000) { // 50 million announcement!
			animation = 623;
			publicyell(getPlayerName() + "'s " + skill.getName() + " has just reached 50 million experience!");
		}
		if (oldXP < 75000000 && newXP >= 75000000) { // 75 million announcement!
			animation = 623;
			publicyell(getPlayerName() + "'s " + skill.getName() + " has just reached 75 million experience!");
		}
		if (oldXP < 100000000 && newXP >= 100000000) { // 100 million announcement!
			animation = 623;
			publicyell(getPlayerName() + "'s " + skill.getName() + " has just reached 100 million experience!");
		}
		if (oldXP < 125000000 && newXP >= 125000000) { // 100 million announcement!
			animation = 623;
			publicyell(getPlayerName() + "'s " + skill.getName() + " has just reached 125 million experience!");
		}
		if (oldXP < 150000000 && newXP >= 150000000) { // 150 million announcement!
			animation = 623;
			publicyell(getPlayerName() + "'s " + skill.getName() + " has just reached 150 million experience!");
		}
		if (oldXP < 175000000 && newXP >= 175000000) { // 150 million announcement!
			animation = 623;
			publicyell(getPlayerName() + "'s " + skill.getName() + " has just reached 175 million experience!");
		}
		if (oldXP < 200000000 && newXP >= 200000000) { // 200 million announcement!
			animation = 623;
			publicyell(getPlayerName() + "'s " + skill.getName() + " has just reached the maximum experience!");
		}
    /*double chance = (double)newXp / 1000000; //1 xp = 0.000001, 0.0001 %
    double roll = Math.random() * 1;
    if(getGameWorldId() > 1)
    	send(new SendMessage("XP gained: "+ newXp +", your chance is " + chance * 100 + "% and you rolled a " + roll * 100));
    if(roll <= chance && getGameWorldId() > 1) //Test world 2 or higher for this to trigger!
    	Balloons.triggerBalloonEvent(this);*/
		setLevel(Skills.getLevelForExperience(getExperience(skill)), skill);
		refreshSkill(skill);
		if(skill == Skill.FIREMAKING)
			updateBonus(11);
		if(skill == Skill.HITPOINTS && newLevel > maxHealth)
			maxHealth = newLevel;
		else if(skill == Skill.PRAYER && newLevel > maxPrayer)
			maxPrayer = newLevel;
		if(animation != -1)
			animation(animation, getPosition());
		getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
		return true;
	}

	public boolean bankItem(int itemID, int fromSlot, int amount) {
		if (playerItemsN[fromSlot] <= 0 || playerItems[fromSlot] <= 0 || playerItems[fromSlot] - 1 != itemID) {
			return false;
		}
		int id = GetUnnotedItem(itemID);
		if (id == 0) {
			if (playerItems[fromSlot] <= 0) {
				return false;
			}
			if (Server.itemManager.isStackable(itemID) || playerItemsN[fromSlot] > 1) {
				int toBankSlot = 0;
				boolean alreadyInBank = false;

				for (int i = 0; i < playerBankSize; i++) {
					if (bankItems[i] - 1 == itemID) { //Bank starts at value 0 while items should start at -1!
						if (playerItemsN[fromSlot] < amount) {
							amount = playerItemsN[fromSlot];
						}
						alreadyInBank = true;
						toBankSlot = i;
						i = playerBankSize + 1;
					}
				}

				if (!alreadyInBank && freeBankSlots() > 0) {
					for (int i = 0; i < playerBankSize; i++) {
						if (bankItems[i] <= 0) {
							toBankSlot = i;
							i = playerBankSize + 1;
						}
					}
					bankItems[toBankSlot] = itemID + 1; //To continue on comment above..Dodian thing :D
					if (playerItemsN[fromSlot] < amount) {
						amount = playerItemsN[fromSlot];
					}
					if ((bankItemsN[toBankSlot] + amount) <= maxItemAmount && (bankItemsN[toBankSlot] + amount) > -1) {
						bankItemsN[toBankSlot] += amount;
					} else {
						send(new SendMessage("Bank full!"));
						return false;
					}
					deleteItem(itemID, fromSlot, amount);
					resetItems(5064);
					resetBank();
					return true;
				} else if (alreadyInBank) {
					if ((bankItemsN[toBankSlot] + amount) <= maxItemAmount && (bankItemsN[toBankSlot] + amount) > -1) {
						bankItemsN[toBankSlot] += amount;
					} else {
						send(new SendMessage("Bank full!"));
						return false;
					}
					deleteItem(itemID, fromSlot, amount);
					resetItems(5064);
					resetBank();
					return true;
				} else {
					send(new SendMessage("Bank full!"));
					return false;
				}
			} else {
				itemID = playerItems[fromSlot];
				int toBankSlot = 0;
				boolean alreadyInBank = false;

				for (int i = 0; i < playerBankSize; i++) {
					if (bankItems[i] == playerItems[fromSlot]) {
						alreadyInBank = true;
						toBankSlot = i;
						i = playerBankSize + 1;
					}
				}
				if (!alreadyInBank && freeBankSlots() > 0) {
					for (int i = 0; i < playerBankSize; i++) {
						if (bankItems[i] <= 0) {
							toBankSlot = i;
							i = playerBankSize + 1;
						}
					}
					int firstPossibleSlot = 0;
					boolean itemExists = false;

					while (amount > 0) {
						itemExists = false;
						for (int i = firstPossibleSlot; i < playerItems.length; i++) {
							if ((playerItems[i]) == itemID) {
								firstPossibleSlot = i;
								itemExists = true;
								i = 30;
							}
						}
						if (itemExists) {
							bankItems[toBankSlot] = playerItems[firstPossibleSlot];
							bankItemsN[toBankSlot] += 1;
							deleteItem((playerItems[firstPossibleSlot] - 1), firstPossibleSlot, 1);
							amount--;
						} else {
							amount = 0;
						}
					}
					resetItems(5064);
					resetBank();
					return true;
				} else if (alreadyInBank) {
					int firstPossibleSlot = 0;
					boolean itemExists = false;

					while (amount > 0) {
						itemExists = false;
						for (int i = firstPossibleSlot; i < playerItems.length; i++) {
							if ((playerItems[i]) == itemID) {
								firstPossibleSlot = i;
								itemExists = true;
								i = 30;
							}
						}
						if (itemExists) {
							bankItemsN[toBankSlot] += 1;
							deleteItem((playerItems[firstPossibleSlot] - 1), firstPossibleSlot, 1);
							amount--;
						} else {
							amount = 0;
						}
					}
					resetItems(5064);
					resetBank();
					return true;
				} else {
					send(new SendMessage("Bank full!"));
					return false;
				}
			}
		} else if (id > 0) {
			if (playerItems[fromSlot] <= 0) {
				return false;
			}
			if (Server.itemManager.isStackable(playerItems[fromSlot] - 1) || playerItemsN[fromSlot] > 1) {
				int toBankSlot = 0;
				boolean alreadyInBank = false;
				for (int i = 0; i < playerBankSize; i++) {
					if (bankItems[i] == GetUnnotedItem(playerItems[fromSlot] - 1) + 1) {
						if (playerItemsN[fromSlot] < amount) {
							amount = playerItemsN[fromSlot];
						}
						alreadyInBank = true;
						toBankSlot = i;
						i = playerBankSize + 1;
					}
				}
				if (!alreadyInBank && freeBankSlots() > 0) {
					for (int i = 0; i < playerBankSize; i++) {
						if (bankItems[i] <= 0) {
							toBankSlot = i;
							i = playerBankSize + 1;
						}
					}
					bankItems[toBankSlot] = id + 1;
					if (playerItemsN[fromSlot] < amount) {
						amount = playerItemsN[fromSlot];
					}
					if ((bankItemsN[toBankSlot] + amount) <= maxItemAmount && (bankItemsN[toBankSlot] + amount) > -1) {
						bankItemsN[toBankSlot] += amount;
					} else {
						return false;
					}
					deleteItem((playerItems[fromSlot] - 1), fromSlot, amount);
					resetItems(5064);
					resetBank();
					return true;
				} else if (alreadyInBank) {
					if ((bankItemsN[toBankSlot] + amount) <= maxItemAmount && (bankItemsN[toBankSlot] + amount) > -1) {
						bankItemsN[toBankSlot] += amount;
					} else {
						return false;
					}
					deleteItem((playerItems[fromSlot] - 1), fromSlot, amount);
					resetItems(5064);
					resetBank();
					return true;
				} else {
					send(new SendMessage("Bank full!"));
					return false;
				}
			} else {
				itemID = playerItems[fromSlot];
				int toBankSlot = 0;
				boolean alreadyInBank = false;

				for (int i = 0; i < playerBankSize; i++) {
					if (bankItems[i] == (playerItems[fromSlot] - 1)) {
						alreadyInBank = true;
						toBankSlot = i;
						i = playerBankSize + 1;
					}
				}
				if (!alreadyInBank && freeBankSlots() > 0) {
					for (int i = 0; i < playerBankSize; i++) {
						if (bankItems[i] <= 0) {
							toBankSlot = i;
							i = playerBankSize + 1;
						}
					}
					int firstPossibleSlot = 0;
					boolean itemExists = false;

					while (amount > 0) {
						itemExists = false;
						for (int i = firstPossibleSlot; i < playerItems.length; i++) {
							if ((playerItems[i]) == itemID) {
								firstPossibleSlot = i;
								itemExists = true;
								i = 30;
							}
						}
						if (itemExists) {
							bankItems[toBankSlot] = (playerItems[firstPossibleSlot] - 1);
							bankItemsN[toBankSlot] += 1;
							deleteItem((playerItems[firstPossibleSlot] - 1), firstPossibleSlot, 1);
							amount--;
						} else {
							amount = 0;
						}
					}
					resetItems(5064);
					resetBank();
					return true;
				} else if (alreadyInBank) {
					int firstPossibleSlot = 0;
					boolean itemExists = false;

					while (amount > 0) {
						itemExists = false;
						for (int i = firstPossibleSlot; i < playerItems.length; i++) {
							if ((playerItems[i]) == itemID) {
								firstPossibleSlot = i;
								itemExists = true;
								i = 30;
							}
						}
						if (itemExists) {
							bankItemsN[toBankSlot] += 1;
							deleteItem((playerItems[firstPossibleSlot] - 1), firstPossibleSlot, 1);
							amount--;
						} else {
							amount = 0;
						}
					}
					resetItems(5064);
					resetBank();
					return true;
				} else {
					send(new SendMessage("Bank full!"));
					return false;
				}
			}
		} else {
			send(new SendMessage("Item not supported " + itemID));
			return false;
		}
	}

	public void resetItems(int WriteFrame) {
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(WriteFrame);
		getOutputStream().writeWord(playerItems.length);
		for (int i = 0; i < playerItems.length; i++) {
			if (playerItemsN[i] > 254) {
				getOutputStream().writeByte(255); // item's stack count. if over 254,
				// write byte 255
				getOutputStream().writeDWord_v2(playerItemsN[i]); // and then the real
				// value with
				// writeDWord_v2
			} else {
				getOutputStream().writeByte(playerItemsN[i]);
			}
			if (playerItems[i] < 0) {
				playerItems[i] = -1;
			}
			getOutputStream().writeWordBigEndianA(playerItems[i]); // item id
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public void sendInventory(int interfaceId, ArrayList<GameItem> inv) {
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(interfaceId); // bank
		getOutputStream().writeWord(inv.size()); // number of items
		for (GameItem item : inv) {
			int amt = item.getAmount();
			int id = item.getId();
			if (amt > 254) {
				getOutputStream().writeByte(255);
				getOutputStream().writeDWord_v2(amt);
			} else {
				getOutputStream().writeByte(amt); // amount
			}
			getOutputStream().writeWordBigEndianA(id + 1); // itemID
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public void SetSmithing(int WriteFrame) {
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(WriteFrame);
		getOutputStream().writeWord(Constants.SmithingItems.length);
		for (int i = 0; i < Constants.SmithingItems.length; i++) {
			Constants.SmithingItems[i][0] += 1;
			if (Constants.SmithingItems[i][1] > 254) {
				getOutputStream().writeByte(255); // item's stack count. if over 254,
				// write byte 255
				getOutputStream().writeDWord_v2(Constants.SmithingItems[i][1]); // and
				// then
				// the real
				// value
				// with
				// writeDWord_v2
			} else {
				getOutputStream().writeByte(Constants.SmithingItems[i][1]);
			}
			if (Constants.SmithingItems[i][0] < 0) {
				playerItems[i] = 7500;
			}
			getOutputStream().writeWordBigEndianA(Constants.SmithingItems[i][0]); // item
			// id
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public void resetOTItems(int WriteFrame) {
		Client other = getClient(trade_reqId);
		if (!validClient(trade_reqId)) {
			return;
		}
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(WriteFrame);
		int len = other.offeredItems.toArray().length;
		int current = 0;
		getOutputStream().writeWord(len);
		for (GameItem item : other.offeredItems) {
			if (item.getAmount() > 254) {
				getOutputStream().writeByte(255); // item's stack count. if over 254,
				// write byte 255
				getOutputStream().writeDWord_v2(item.getAmount()); // and then the real
				// value with
				// writeDWord_v2
			} else {
				getOutputStream().writeByte(item.getAmount());
			}
			getOutputStream().writeWordBigEndianA(item.getId() + 1); // item id
			current++;
		}
		if (current < 27) {
			for (int i = current; i < 28; i++) {
				getOutputStream().writeByte(1);
				getOutputStream().writeWordBigEndianA(-1);
			}
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public void resetTItems(int WriteFrame) {
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(WriteFrame);
		int len = offeredItems.toArray().length;
		int current = 0;
		getOutputStream().writeWord(len);
		for (GameItem item : offeredItems) {
			if (item.getAmount() > 254) {
				getOutputStream().writeByte(255); // item's stack count. if over 254,
				// write byte 255
				getOutputStream().writeDWord_v2(item.getAmount()); // and then the real
				// value with
				// writeDWord_v2
			} else {
				getOutputStream().writeByte(item.getAmount());
			}
			getOutputStream().writeWordBigEndianA(item.getId() + 1); // item id
			current++;
		}
		if (current < 27) {
			for (int i = current; i < 28; i++) {
				getOutputStream().writeByte(1);
				getOutputStream().writeWordBigEndianA(-1);
			}
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public void resetShop(int ShopID) {
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(3900);
		getOutputStream().writeWord(ShopHandler.MaxShopItems);

		for (int i = 0; i < ShopHandler.MaxShopItems; i++) {
			if (ShopHandler.ShopItems[ShopID][i] > 0) {
				if (ShopHandler.ShopItemsN[ShopID][i] > 254) {
					getOutputStream().writeByte(255); // item's stack count. if over
					getOutputStream().writeDWord_v2(ShopHandler.ShopItemsN[ShopID][i]); // and
				} else {
					getOutputStream().writeByte(ShopHandler.ShopItemsN[ShopID][i]);
				}
				getOutputStream().writeWordBigEndianA(ShopHandler.ShopItems[ShopID][i]); // item
			} else { //If nothing to buy!
				getOutputStream().writeByte(0);
				getOutputStream().writeWordBigEndianA(0);
			}
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public void resetBank() {
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(5382); // bank
		getOutputStream().writeWord(playerBankSize); // number of items
		for (int i = 0; i < playerBankSize; i++) {
			if (bankItemsN[i] > 254) {
				getOutputStream().writeByte(255);
				getOutputStream().writeDWord_v2(bankItemsN[i]);
			} else {
				getOutputStream().writeByte(bankItemsN[i]); // amount
			}
			if (bankItemsN[i] < 1) {
				bankItems[i] = 0;
			}
			if (bankItems[i] < 0) {
				bankItems[i] = 7500;
			}
			getOutputStream().writeWordBigEndianA(bankItems[i]); // itemID
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public void sendBank(ArrayList<Integer> id, ArrayList<Integer> amt) {
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(5382); // bank
		getOutputStream().writeWord(id.size()); // number of items
		for (int i = 0; i < id.size(); i++) {
			if (amt.get(i) > 254) {
				getOutputStream().writeByte(255);
				getOutputStream().writeDWord_v2(amt.get(i));
			} else {
				getOutputStream().writeByte(amt.get(i)); // amount
			}
			getOutputStream().writeWordBigEndianA(id.get(i) + 1); // itemID
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public void sendBank(int interfaceId, ArrayList<GameItem> bank) {
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(interfaceId); // bank
		getOutputStream().writeWord(bank.size()); // number of items
		for (GameItem item : bank) {
			int amt = item.getAmount();
			int id = item.getId();
			if (amt > 254) {
				getOutputStream().writeByte(255);
				getOutputStream().writeDWord_v2(amt);
			} else {
				getOutputStream().writeByte(amt); // amount
			}
			getOutputStream().writeWordBigEndianA(id + 1); // itemID
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public void moveItems(int from, int to, int moveWindow) {
		if (moveWindow == 3214 || moveWindow == 5064) {
			int tempI = playerItems[to];
			int tempN = playerItemsN[to];
			playerItems[to] = playerItems[from];
			playerItemsN[to] = playerItemsN[from];
			playerItems[from] = tempI;
			playerItemsN[from] = tempN;
			resetItems(moveWindow);
		}
		if (moveWindow == 5382 && from >= 0 && to >= 0 && from < playerBankSize && to < playerBankSize) {
			int tempI = bankItems[from];
			int tempN = bankItemsN[from];
			bankItems[from] = bankItems[to];
			bankItemsN[from] = bankItemsN[to];
			bankItems[to] = tempI;
			bankItemsN[to] = tempN;
			resetBank();
		}
	}

	public int freeBankSlots() {
		int freeS = 0;

		for (int i = 0; i < playerBankSize; i++) {
			if (bankItems[i] <= 0) {
				freeS++;
			}
		}
		return freeS;
	}

	public int freeSlots() {
		int freeSlot = 0;

		for (int playerItem : playerItems) {
			if (playerItem <= 0) {
				freeSlot++;
			}
		}
		return freeSlot;
	}

	public void pickUpItem(int id, int x, int y) {
		boolean specialItems = (x == 2611 && y == 3096) || (x == 2612 && y == 3096) || (x == 2563 && y == 9511) || (x == 2564 && y == 9511);
		if (specialItems && playerRights < 2) {
			dropAllItems();
			return;
		}
		for (GroundItem item : Ground.items) {
			if (item.id == id && getPosition().getX() == x && getPosition().getY() == y && (item.visible || dbId == item.playerId)) {
				if (getPosition().getX() == item.x && getPosition().getY() == item.y) {
					if (premiumItem(id) && !premium) {
						send(new SendMessage("You must be a premium member to use this item"));
						return;
					}
					if (item.isTaken())
						continue;
					if (addItem(item.id, item.amount)) { //Fixed so stackable items can be added with full inv as long as you got it.
						item.setTaken(true);
						Ground.deleteItem(item);
						//send(new Sound(356)); //TODO: fix sounds!
						PickupLog.recordPickup(this, item.id, item.amount, item.npc ? Server.npcManager.getName(item.npcId) : item.canDespawn ? "" + item.playerId : "Global Item Pickup", getPosition().copy());
					}
					break;
				}
			}
		}
	}

	public void openUpBank() {
		if (!Server.banking) {
			send(new SendMessage("Banking have been disabled!"));
			return;
		}
		resetAction(true);
		resetBank();
		resetItems(5064);
		send(new SendString(takeAsNote ? "No Note" : "Note", 5389));
		send(new SendString("Bank All", 5391));
		send(new SendString("Bank of " + getPlayerName(), 5383));
		IsBanking = true;
		checkBankInterface = false;
		send(new InventoryInterface(5292, 5063));
	}

	public void openUpShop(int ShopID) {
		if (!Server.shopping) {
			send(new SendMessage("Shopping have been disabled!"));
			return;
		}
		if (ShopID == 20 || ShopID == 34) {
			if (!premium) {
				send(new SendMessage("You need to be a premium member to access this shop."));
				return;
			}
		}
		send(new SendString(ShopHandler.ShopName[ShopID], 3901));
		send(new InventoryInterface(3824, 3822));
		resetItems(3823);
		resetShop(ShopID);
		IsShopping = true;
		MyShopID = ShopID;
	}

	private void checkItemUpdate() {
		if (IsShopping) {
			resetItems(3823);
		} else if (IsBanking || checkBankInterface) {
			resetItems(5064);
			send(new InventoryInterface(5292, 5063));
		} else if (isPartyInterface) {
			resetItems(5064);
			send(new InventoryInterface(2156, 5063));
		}
	}

	public boolean addItem(int item, int amount) {
		if (item < 0 || amount < 1) {
			return false;
		}
		amount = !Server.itemManager.isStackable(item) ? 1 : amount;
		if ((freeSlots() >= amount && !Server.itemManager.isStackable(item)) || freeSlots() > 0) {
			for (int i = 0; i < playerItems.length; i++) {
				if (playerItems[i] == (item + 1) && Server.itemManager.isStackable(item) && playerItems[i] > 0) {
					playerItems[i] = (item + 1);
					if ((playerItemsN[i] + amount) < maxItemAmount && (playerItemsN[i] + amount) > -1) {
						playerItemsN[i] += amount;
					} else {
						playerItemsN[i] = maxItemAmount;
					}
					getOutputStream().createFrameVarSizeWord(34);
					getOutputStream().writeWord(3214);
					getOutputStream().writeByte(i);
					getOutputStream().writeWord(playerItems[i]);
					if (playerItemsN[i] > 254) {
						getOutputStream().writeByte(255);
						getOutputStream().writeDWord(playerItemsN[i]);
					} else {
						getOutputStream().writeByte(playerItemsN[i]); // amount
					}
					getOutputStream().endFrameVarSizeWord();
					checkItemUpdate();
					return true;
				}
			}
			for (int i = 0; i < playerItems.length; i++) {
				if (playerItems[i] <= 0) {
					playerItems[i] = item + 1;
					playerItemsN[i] = Math.min(amount, maxItemAmount);
					getOutputStream().createFrameVarSizeWord(34);
					getOutputStream().writeWord(3214);
					getOutputStream().writeByte(i);
					getOutputStream().writeWord(playerItems[i]);
					if (playerItemsN[i] > 254) {
						getOutputStream().writeByte(255);
						getOutputStream().writeDWord(playerItemsN[i]);
					} else {
						getOutputStream().writeByte(playerItemsN[i]); // amount
					}
					getOutputStream().endFrameVarSizeWord();
					checkItemUpdate();
					return true;
				}
			}
			return false;
		} else if (contains(item) && Server.itemManager.isStackable(item)) {
			int slot = -1;
			for (int i = 0; i < playerItems.length; i++) {
				if (playerItems[i] == item + 1) {
					slot = i;
					break;
				}
			}
			if ((long) playerItemsN[slot] + (long) amount > (long) Integer.MAX_VALUE) {
				send(new SendMessage("Failed! Reached max item amount!"));
				return false;
			}
			playerItemsN[slot] = playerItemsN[slot] + amount;
			getOutputStream().createFrameVarSizeWord(34);
			getOutputStream().writeWord(3214);
			getOutputStream().writeByte(slot);
			getOutputStream().writeWord(playerItems[slot]);
			if (playerItemsN[slot] > 254) {
				getOutputStream().writeByte(255);
				getOutputStream().writeDWord(playerItemsN[slot]);
			} else {
				getOutputStream().writeByte(playerItemsN[slot]); // amount
			}
			getOutputStream().endFrameVarSizeWord();
			checkItemUpdate();
			return true;
		} else {
			send(new SendMessage("Not enough space in your inventory."));
			return false;
		}
	}

	public void addItemSlot(int item, int amount, int slot) {
		item++;
		playerItems[slot] = item;
		playerItemsN[slot] = amount;
		getOutputStream().createFrameVarSizeWord(34);
		getOutputStream().writeWord(3214);
		getOutputStream().writeByte(slot);
		getOutputStream().writeWord(item);
		if (amount > 254) {
			getOutputStream().writeByte(255);
			getOutputStream().writeDWord(amount);
		} else {
			getOutputStream().writeByte(amount); // amount
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public void dropItem(int id, int slot) {
		if (inTrade || inDuel || IsBanking) {
			return;
		}
		if (!Server.dropping) {
			send(new SendMessage("Dropping has been disabled.  Please try again later"));
			return;
		}
		send(new RemoveInterfaces()); //Need this to stop interface abuse
		int amount = 0;
		if (playerItems[slot] == (id + 1) && playerItemsN[slot] > 0) {
			amount = playerItemsN[slot];
		}
		if (amount < 1) {
			return;
		}
		/*
		 * if(dropTries < 1){ dropTries++; for(int i = 0; i < 2; i++){ sendMessage (
		 * "WARNING: dropping this item will DELETE it, not drop it"); send(new
		 * SendMessage( "To confirm, drop again"); return; } }
		 */
		//send(new Sound(376));
		deleteItem(id, slot, amount);
		GroundItem drop = new GroundItem(getPosition().copy(), id, amount, getSlot(), -1);
		Ground.items.add(drop);
		DropLog.recordDrop(this, drop.id, drop.amount, "Player", getPosition().copy(), "Inventory Drop");
	}

	public void deleteItem(int id, int amount) {
		deleteItem(id, GetItemSlot(id), amount);
	}

	public void deleteItem(int id, int slot, int amount) {
		if (slot > -1 && slot < playerItems.length) {
			if ((playerItems[slot] - 1) == id) {
				if (playerItemsN[slot] > amount) {
					playerItemsN[slot] -= amount;
				} else {
					playerItemsN[slot] = 0;
					playerItems[slot] = 0;
				}
				resetItems(3214);
				if (IsBanking) {
					send(new InventoryInterface(5292, 5063)); // 5292
					resetItems(5064);
				}
			}
		}
	}

	public void deleteItemBank(int id, int slot, int amount) {
		if (slot > -1 && slot < bankItems.length) {
			if ((bankItems[slot] - 1) == id) {
				if (bankItemsN[slot] > amount) {
					bankItemsN[slot] -= amount;
				} else {
					bankItemsN[slot] = 0;
					bankItems[slot] = 0;
				}
				resetItems(3214);
				if (IsBanking) {
					send(new InventoryInterface(5292, 5063)); // 5292
					resetItems(5064);
				}
			}
		}
	}

	public void deleteItemBank(int id, int amount) {
		deleteItemBank(id, GetBankItemSlot(id), amount);
	}

	public void setEquipment(int wearID, int amount, int targetSlot) {
		getOutputStream().createFrameVarSizeWord(34);
		getOutputStream().writeWord(1688);
		getOutputStream().writeByte(targetSlot);
		getOutputStream().writeWord(wearID == -1 ? 0 : wearID + 1);
		if (amount > 254) {
			getOutputStream().writeByte(255);
			getOutputStream().writeDWord(amount);
		} else
			getOutputStream().writeByte(amount); // amount
		getOutputStream().endFrameVarSizeWord();
		if (targetSlot == Equipment.Slot.WEAPON.getId()) {
			CheckGear();
			CombatStyleHandler.setWeaponHandler(this, -1);
			requestAnims(wearID);
		}
		GetBonus(true);
		getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
	}

	public boolean wear(int wearID, int slot, int Interface) {
		if (inTrade) {
			return false;
		}
		if (emptyEssencePouch(wearID)) { //Runecrafting Pouches
			return false;
		}
		if (wearID == 5733) { //Potato
			wipeInv();
			return false;
		}
		if (wearID == 4155) { //Enchanted gem
			SlayerTask.slayerTasks checkTask = SlayerTask.slayerTasks.getTask(getSlayerData().get(1));
			if (checkTask != null && getSlayerData().get(3) > 0)
				send(new SendMessage("You need to kill " + getSlayerData().get(3) + " more " + checkTask.getTextRepresentation()));
			else
				send(new SendMessage("You need to be assigned a task!"));
			return false;
		}
		if (duelFight && duelRule[3]) {
			send(new SendMessage("Equipment changing has been disabled in this duel"));
			return false;
		}
		if (duelConfirmed && !duelFight)
			return false;
		if (!playerHasItem(wearID)) {
			return false;
		}
		int targetSlot = Server.itemManager.getSlot(wearID);
		if (!canUse(wearID)) {
			send(new SendMessage("You must be a premium member to use this item"));
			return false;
		}
		if (targetSlot != 8 && duelBodyRules[falseSlots[targetSlot]]) {
			send(new SendMessage("Current duel rules restrict this from being worn!"));
			return false;
		}
		if ((playerItems[slot] - 1) == wearID) {
			if(!checkEquip(wearID, targetSlot, slot))
				return false;
			int wearAmount = playerItemsN[slot];
			if (wearAmount < 1) {
				return false;
			}
			if (wearID >= 0) {
				deleteItem(wearID, slot, wearAmount);
				if (getEquipment()[targetSlot] != wearID && getEquipment()[targetSlot] >= 0) {
					addItem(getEquipment()[targetSlot], getEquipmentN()[targetSlot]);
				} else if (Server.itemManager.isStackable(wearID) && getEquipment()[targetSlot] == wearID) {
					wearAmount = getEquipmentN()[targetSlot] + wearAmount;
				} else if (getEquipment()[targetSlot] >= 0) {
					addItem(getEquipment()[targetSlot], getEquipmentN()[targetSlot]);
				}
			}
			getEquipment()[targetSlot] = wearID;
			getEquipmentN()[targetSlot] = wearAmount;
			setEquipment(getEquipment()[targetSlot], getEquipmentN()[targetSlot], targetSlot);
			wearing = false;
			getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
			return true;
		}
		return false;
	}

	public boolean checkEquip(int id, int slot, int invSlot) {
		int CLAttack = GetCLAttack(id);
		int CLDefence = GetCLDefence(id);
		int CLStrength = GetCLStrength(id);
		int CLMagic = GetCLMagic(id);
		int CLRanged = GetCLRanged(id);
		boolean failCheck = false;
		String itemName = Server.itemManager.getName(id);
		if (CLAttack > Skills.getLevelForExperience(getExperience(Skill.ATTACK))) {
			send(new SendMessage("You need " + CLAttack + " Attack to equip " + itemName.toLowerCase() + "."));
			failCheck = true;
		}
		if (CLDefence > Skills.getLevelForExperience(getExperience(Skill.DEFENCE))) {
			send(new SendMessage("You need " + CLDefence + " Defence to equip " + itemName.toLowerCase() + "."));
			failCheck = true;
		}
		if (CLStrength > Skills.getLevelForExperience(getExperience(Skill.STRENGTH))) {
			send(new SendMessage("You need " + CLStrength + " Strength to equip " + itemName.toLowerCase() + "."));
			failCheck = true;
		}
		if (CLMagic > Skills.getLevelForExperience(getExperience(Skill.MAGIC))) {
			send(new SendMessage("You need " + CLMagic + " Magic to equip " + itemName.toLowerCase() + "."));
			failCheck = true;
		}
		if (CLRanged > Skills.getLevelForExperience(getExperience(Skill.RANGED))) {
			send(new SendMessage("You need " + CLRanged + " Ranged to equip " + itemName.toLowerCase() + "."));
			failCheck = true;
		}
		if (Skills.getLevelForExperience(getExperience(Skill.AGILITY)) < 60 && id == 4224) {
			send(new SendMessage("You need 60 Agility to equip " + itemName.toLowerCase() + "."));
			failCheck = true;
		}
		Skillcape skillcape = Skillcape.getSkillCape(id);
		if (skillcape != null) {
			if (Skillcape.isTrimmed(id) && getExperience(skillcape.getSkill()) < 50000000) {
				send(new SendMessage("This cape requires 50 million " + skillcape.getSkill().getName() + " experience to wear."));
				failCheck = true;
			} else if (Skills.getLevelForExperience(getExperience(skillcape.getSkill())) < 99) {
				send(new SendMessage("This cape requires level 99 " + skillcape.getSkill().getName() + " to wear."));
				failCheck = true;
			}
		}
		/* 2handed check! */
		int shield = getEquipment()[Equipment.Slot.SHIELD.getId()];
		int weapon = getEquipment()[Equipment.Slot.WEAPON.getId()];
		boolean twoHanded = Server.itemManager.isTwoHanded(id) || Server.itemManager.isTwoHanded(weapon);
		if(twoHanded && !failCheck) {
			if (slot == Equipment.Slot.WEAPON.getId()) {
				if (shield > 0 && hasSpace()) {
					remove(slot, true);
					remove(Equipment.Slot.SHIELD.getId(), true);
					if(invSlot == -1)
						addItem(weapon, 1);
					else
						addItemSlot(weapon, 1, invSlot);
					addItem(shield, 1);
				} else if (shield > 0) {
					send(new SendMessage("Not enough space to equip this item!"));
					failCheck = true;
				}
			} else if (slot == Equipment.Slot.SHIELD.getId()) {
				remove(Equipment.Slot.WEAPON.getId(), true);
				if(invSlot == -1)
					addItem(weapon, 1);
				else
					addItemSlot(weapon, 1, invSlot);
			}
		}
		/* Bow check! */
		boolean check = (id == 4212 || id == 6724 || id == 20997) || (weapon == 4212 || weapon == 6724 || weapon == 20997);
		if(check && !failCheck) {
			if(slot == 5 && id != 3844 && id != 4224 && id != 1540) {
				if (shield > 0 && hasSpace()) {
					remove(slot, true);
					remove(Equipment.Slot.WEAPON.getId(), true);
					if(invSlot == -1)
						addItem(weapon, 1);
					else
						addItemSlot(weapon, 1, invSlot);
					addItem(shield, 1);
				} else if(shield > 0) {
					send(new SendMessage("Not enough space to equip this item!"));
					failCheck = true;
				} else if (shield < 1 && invSlot != -1) {
					remove(Equipment.Slot.WEAPON.getId(), true);
					addItemSlot(weapon, 1, invSlot);
				} else if (invSlot == -1) failCheck = true;
			}
			if(slot == 3 && invSlot != -1 && (id == 4212 || id == 6724 || id == 20997)) {
				boolean shieldCheck = shield == 3844 || shield == 4224 || shield == 1540;
				if(!shieldCheck && shield > 0 && weapon > 0 && hasSpace()) {
					remove(slot, true);
					remove(Equipment.Slot.SHIELD.getId(), true);
					addItemSlot(weapon, 1, invSlot);
					addItem(shield, 1);
				} else if(shield > 0 && weapon > 0 && !hasSpace()) {
					send(new SendMessage("Not enough space to equip this item!"));
					failCheck = true;
				} else if(shield > 0 && !shieldCheck) {
					remove(Equipment.Slot.SHIELD.getId(), true);
					addItemSlot(shield, 1, invSlot);
				}
			}
		}
		return !failCheck;
	}

	public boolean remove(int slot, boolean force) {
		if (duelFight && duelRule[3] && !force) {
			send(new SendMessage("Equipment changing has been disabled in this duel!"));
			return false;
		}
		if (duelConfirmed && !force) {
			return false;
		}
		getEquipment()[slot] = -1;
		getEquipmentN()[slot] = 0;
		setEquipment(getEquipment()[slot], getEquipmentN()[slot], slot);
		getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
		return true;
	}

	public void deleteequiment(int wearID, int slot) {
		if (getEquipment()[slot] == wearID) {
			getEquipment()[slot] = -1;
			getEquipmentN()[slot] = 0;
			setEquipment(getEquipment()[slot], getEquipmentN()[slot], slot);
		}
	}

	public void setChatOptions(int publicChat, int privateChat, int tradeBlock) {
		getOutputStream().createFrame(206);
		getOutputStream().writeByte(publicChat); // On = 0, Friends = 1, Off = 2,
		// Hide = 3
		getOutputStream().writeByte(privateChat); // On = 0, Friends = 1, Off = 2
		getOutputStream().writeByte(tradeBlock); // On = 0, Friends = 1, Off = 2
	}

	// upon connection of a new client all the info has to be sent to client
	// prior to starting the regular communication
	public void initialize() {
		getOutputStream().createFrame(249);
		getOutputStream().writeByteA(playerIsMember); // 1 = member, 0 = f2p
		getOutputStream().writeWordBigEndianA(getSlot());
		getOutputStream().createFrame(107); // resets something in the client
		// here is the place for seting up the UI, stats, etc...
		setChatOptions(0, 0, 0);
		frame36(287, 1); //SPLIT PRIVATE CHAT ON/OFF
		/*
		 * for (int i = 0; i < 25; i++) { if(i != 3) setSkillLevel(i,
		 * playerLevel[i], playerXP[i]); }
		 */
		setSidebarInterface(0, 2423); // attack tab
		setSidebarInterface(1, 3917); // skills tab
		setSidebarInterface(2, 638); // quest tab
		setSidebarInterface(3, 3213); // backpack tab
		setSidebarInterface(4, 1644); // items wearing tab
		setSidebarInterface(5, 5608); // pray tab
		setSidebarInterface(6, 12855); // magic tab (ancient = 12855)
		setSidebarInterface(7, -1); // ancient magicks
		setSidebarInterface(8, 5065); // friend
		setSidebarInterface(9, 5715); // ignore
		setSidebarInterface(10, 2449); // logout tab
		setSidebarInterface(11, 904); // wrench tab
		setSidebarInterface(12, 147); // run tab
		setSidebarInterface(13, 962); // harp tab
      /*if (getEquipment()[Equipment.Slot.WEAPON.getId()] == 2518) {
        getOutputStream().createFrameVarSize(104);
        getOutputStream().writeByteC(1);
        getOutputStream().writeByteA(1);
        getOutputStream().writeString("Throw At");
        getOutputStream().endFrameVarSize();
      } else {
        getOutputStream().createFrameVarSize(104);
        getOutputStream().writeByteC(1);
        getOutputStream().writeByteA(0);
        getOutputStream().writeString("null");
        getOutputStream().endFrameVarSize();
      }*/ //We want this for snowballs????
		//send(new SendMessage("Please vote! You can earn your reward by doing ::redeem "+getPlayerName()+" every 6hrs."));
//    send(new SendMessage("<col=CB1D1D>Santa has come! A bell MUST be rung to celebrate!!!"));
//    send(new SendMessage("<col=CB1D1D>Click it for a present!! =)"));
//    send(new SendMessage("@redPlease have one inventory space open! If you don't PM Logan.."));
		/* Set a player active to a world! */
		PlayerHandler.playersOnline.put(longName, this);
		PlayerHandler.allOnline.put(longName, getGameWorldId());
		/* Sets look! */
		if (lookNeeded) {
			defaultCharacterLook(this);
			showInterface(3559);
		} else
			setLook(playerLooks);
		//Login.banUid(); //Not sure what this do!
		//Arrays.fill(lastMessage, ""); //We need this?!
		/* Friend configs! */
		for (Client c : PlayerHandler.playersOnline.values()) {
			if (c.hasFriend(longName)) {
				c.refreshFriends();
			}
		}
		/* Login write settings! */
		frame36(287, 1);
		WriteEnergy();
		pmstatus(2);

		setEquipment(getEquipment()[Equipment.Slot.HEAD.getId()], getEquipmentN()[Equipment.Slot.HEAD.getId()],
				Equipment.Slot.HEAD.getId());
		setEquipment(getEquipment()[Equipment.Slot.CAPE.getId()], getEquipmentN()[Equipment.Slot.CAPE.getId()],
				Equipment.Slot.CAPE.getId());
		setEquipment(getEquipment()[Equipment.Slot.NECK.getId()], getEquipmentN()[Equipment.Slot.NECK.getId()],
				Equipment.Slot.NECK.getId());
		setEquipment(getEquipment()[Equipment.Slot.ARROWS.getId()], getEquipmentN()[Equipment.Slot.ARROWS.getId()],
				Equipment.Slot.ARROWS.getId());
		setEquipment(getEquipment()[Equipment.Slot.CHEST.getId()], getEquipmentN()[Equipment.Slot.CHEST.getId()],
				Equipment.Slot.CHEST.getId());
		setEquipment(getEquipment()[Equipment.Slot.SHIELD.getId()], getEquipmentN()[Equipment.Slot.SHIELD.getId()],
				Equipment.Slot.SHIELD.getId());
		setEquipment(getEquipment()[Equipment.Slot.LEGS.getId()], getEquipmentN()[Equipment.Slot.LEGS.getId()],
				Equipment.Slot.LEGS.getId());
		setEquipment(getEquipment()[Equipment.Slot.HANDS.getId()], getEquipmentN()[Equipment.Slot.HANDS.getId()],
				Equipment.Slot.HANDS.getId());
		setEquipment(getEquipment()[Equipment.Slot.FEET.getId()], getEquipmentN()[Equipment.Slot.FEET.getId()],
				Equipment.Slot.FEET.getId());
		setEquipment(getEquipment()[Equipment.Slot.RING.getId()], getEquipmentN()[Equipment.Slot.RING.getId()],
				Equipment.Slot.RING.getId());
		setEquipment(getEquipment()[Equipment.Slot.WEAPON.getId()], getEquipmentN()[Equipment.Slot.WEAPON.getId()],
				Equipment.Slot.WEAPON.getId());
		/* Extra slot that I forgot to add...Obs! */
		setEquipment(getEquipment()[Equipment.Slot.BLESSING.getId()], getEquipmentN()[Equipment.Slot.BLESSING.getId()],
				Equipment.Slot.BLESSING.getId());
		/* Reset values for items*/
		resetItems(3214);
		resetBank();
		//replaceDoors(); //Not sure we need this at this point!
		send(new SendString("Using this will send a notification to all online mods", 5967));
		send(new SendString("@yel@Then click below to indicate which of our rules is being broken.", 5969));
		send(new SendString("4: Bug abuse (includes noclip)", 5974));
		send(new SendString("5: Dodian staff impersonation", 5975));
		send(new SendString("6: Monster luring or abuse", 5976));
		send(new SendString("8: Item Duplication", 5978));
		send(new SendString("10: Misuse of yell channel", 5980));
		send(new SendString("12: Possible duped items", 5982));
		send(new SendString("Old magic", 12585));
		send(new SendString("", 6067));
		send(new SendString("", 6071));
		//RegionMusic.sendSongSettings(this); //Music from client 2.95
		/* Set configurations! */
		setConfigIds();
		/* Done loading in a character! */
		send(new SendMessage("Welcome to Uber Server"));
		if (newPms > 0) {
			send(new SendMessage("You have " + newPms + " new messages.  Check your inbox at Dodian.net to view them."));
		}
		/* Check for refunded items! */
		try {
			String query = "SELECT * FROM uber3_refunds WHERE receiver='"+dbId+"' AND message='0' AND claimed IS NULL ORDER BY date ASC";
			Statement stm = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE);
			boolean gotResult = stm.executeQuery(query).next();
			if(gotResult) {
				send(new SendMessage("<col=4C4B73>You have some unclaimed items to claim!"));
				stm.executeUpdate("UPDATE uber3_refunds SET message='1' where message='0'");
			}
			stm.close();
		} catch (Exception e) {
			System.out.println("Error in checking sql!!" + e.getMessage() + ", " + e);
			e.printStackTrace();
		}
		loaded = true;
		/* Update of both player and npc! */
		PlayerUpdating.getInstance().update(this, outputStream);
		NpcUpdating.getInstance().update(this, outputStream);
	}

	public void update() { //Update npc before player!
		PlayerUpdating.getInstance().update(this, outputStream);
		flushOutStream();
		NpcUpdating.getInstance().update(this, outputStream);
		flushOutStream();
	}

	public int packetSize = 0, packetType = -1;
	public boolean canAttack = true;

	public void process() {// is being called regularily every 600 ms
		//RegionMusic.handleRegionMusic(this);
		if (mutedTill * 1000 <= rightNow) {
			send(new SendString(invis ? "You are invisible!" : "", 6572));
		} else {
			mutedHours = ((mutedTill * 1000) - rightNow) / (60 * 60 * 1000);
			send(new SendString("Muted: " + mutedHours + " hours", 6572));
		}
		if(getPositionName(getPosition()) == positions.BRIMHAVEN_DUNGEON) {
			boolean gotIcon = getEquipment()[Equipment.Slot.NECK.getId()] == 8923;
			if(iconTimer > 0 && !gotIcon) iconTimer--;
			else if(iconTimer == 0 && !gotIcon) { //Hit with dmg!
				send(new SendMessage("The strange aura from the dungeon makes you vulnerable!"));
				int dmg = 5 + Misc.random(15);
				this.dealDamage(dmg, dmg >= 15);
				iconTimer = 6;
			} else iconTimer = 6;
		} else
			iconTimer = 6;
		/* Prayer drain*/
		if(prayers.getDrainRate() > 0.0) {
			prayers.drainRate += 0.6;
			if(prayers.drainRate >= prayers.getDrainRate()) {
				drainPrayer(1);
				prayers.drainRate = 0.0;
			}
		} else prayers.drainRate = 0.0;
		/* Stat restore + drain */
		lastRecoverEffect++;
		if (lastRecoverEffect % 25 == 0) {
			lastRecover--;
			if (lastRecover == 0) {
				lastRecoverEffect = 0;
				lastRecover = 4;
				for (int i = 0; i < 21; i++) {
					Skill skill = Skill.getSkill(i);
					if (skill == Skill.HITPOINTS)
						heal(1);
					else if (skill != Skill.PRAYER) {
						if(boostedLevel[i] > 0)
							boostedLevel[i]--;
						else if(boostedLevel[i] < 0)
							boostedLevel[i]++;
					}
					refreshSkill(skill);
				}
			}
		}
		if (reloadHp) {
			heal(getMaxHealth());
			setCurrentPrayer(getMaxPrayer());
		}
		// RubberCheck();
		QuestSend.questInterface(this);
		long now = System.currentTimeMillis();
		if (now >= walkBlock && UsingAgility) {
			UsingAgility = false;
			disconnected = xLog;
			if (!disconnected)
				getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
		}
		if (getWildLevel() < 1) {
			if (wildyLevel > 0)
				setWildLevel(0);
			updatePlayerDisplay();
		} else
			setWildLevel(getWildLevel());
		if (duelFight || inWildy()) {
			getOutputStream().createFrameVarSize(104);
			getOutputStream().writeByteC(3);
			getOutputStream().writeByteA(1);
			getOutputStream().writeString("Attack");
			getOutputStream().endFrameVarSize();
			getOutputStream().createFrameVarSize(104);
			getOutputStream().writeByteC(2);
			getOutputStream().writeByteA(0);
			getOutputStream().writeString("null");
			getOutputStream().endFrameVarSize();
			if (!inWildy()) {
				getOutputStream().createFrameVarSize(104);
				getOutputStream().writeByteC(4); // command slot
				getOutputStream().writeByteA(0); // 0 or 1; 1 if command should be placed
				getOutputStream().writeString("null");
				getOutputStream().endFrameVarSize();
			} else {
				getOutputStream().createFrameVarSize(104);
				getOutputStream().writeByteC(4);
				getOutputStream().writeByteA(0);
				getOutputStream().writeString("Trade with");
				getOutputStream().endFrameVarSize();
			}
		} else {
			getOutputStream().createFrameVarSize(104);
			getOutputStream().writeByteC(3);
			getOutputStream().writeByteA(1);
			getOutputStream().writeString("null");
			getOutputStream().endFrameVarSize();
			getOutputStream().createFrameVarSize(104);
			getOutputStream().writeByteC(4);
			getOutputStream().writeByteA(0);
			getOutputStream().writeString("Trade with");
			getOutputStream().endFrameVarSize();
			getOutputStream().createFrameVarSize(104);
			getOutputStream().writeByteC(2);
			getOutputStream().writeByteA(0);
			getOutputStream().writeString("Duel");
			getOutputStream().endFrameVarSize();
		}
		if (getEquipment()[Equipment.Slot.WEAPON.getId()] == 4566) {
			getOutputStream().createFrameVarSize(104);
			getOutputStream().writeByteC(1);
			getOutputStream().writeByteA(1);
			getOutputStream().writeString("Whack");
			getOutputStream().endFrameVarSize();
		} else {
			getOutputStream().createFrameVarSize(104);
			getOutputStream().writeByteC(1);
			getOutputStream().writeByteA(0);
			getOutputStream().writeString("null");
			getOutputStream().endFrameVarSize();
		}
		if (disconnectAt > 0 && now >= disconnectAt) {
			disconnected = true;
		}
		if (checkTime && now >= lastAction) {
			send(new SendMessage("Time's up (went " + (now - lastAction) + " over)"));
			checkTime = false;
		}
		if (pickupWanted) {
			if (pickTries < 1) {
				pickupWanted = false;
			}
			pickTries--;
			if (getPosition().getX() == pickX && getPosition().getY() == pickY) {
				pickUpItem(pickId, pickX, pickY);
				pickupWanted = false;
			}
		}
		if (animationReset > 0 && System.currentTimeMillis() >= animationReset) {
			animationReset = 0;
			rerequestAnim();
			if (originalS > 0) {
				wear(originalS, Equipment.Slot.SHIELD.getId(), 0);
			}
		}
		if (inTrade && tradeResetNeeded) {
			Client o = getClient(trade_reqId);
			if (o.tradeResetNeeded) {
				resetTrade();
				o.resetTrade();
			}
		}
		if (tStage == 1) { //Set emote for teleport!
			requestAnim(tEmote, 0);
			animation(308, getPosition());
			tStage = 2;
		} else if (tStage == 2 && System.currentTimeMillis() - lastTeleport >= 1200) {
			getPosition().moveTo(tX, tY, tH);
			teleportToX = getPosition().getX();
			teleportToY = getPosition().getY();
			tH = 0;
			getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
			tStage = 0;
			GetBonus(true);
			rerequestAnim();
			UsingAgility = false;
		}
		long current = System.currentTimeMillis();
		if (isInCombat() && current - getLastCombat() >= 7500) {
			setInCombat(false);
		}
		if (wildyLevel < 1 && current - lastBar >= 30000) {
			lastBar = current;
			updatePlayerDisplay();

			// barTimer = 0;
		}
		// Save every minute
		if (current - lastSave >= 60000) {
			saveStats(false);
			lastSave = now;
		}
		// Update progress every hour
		if (current - lastProgressSave >= (60000) * 60) {
			saveStats(false, true);
			lastProgressSave = now;
		}
		if (startDuel && duelChatTimer <= 0) {
			startDuel = false;
		}
		if (resetanim <= 0) {
			rerequestAnim();
			resetanim = 8;
		}

		if (AnimationReset && actionTimer <= 0) {
			rerequestAnim();
			AnimationReset = false;
			getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
		}
		if (actionTimer > 0) {
			actionTimer -= 1;
		}
		// Shop
		if (UpdateShop) {
			resetItems(3823);
			resetShop(MyShopID);
		}

		// check stairs
		if (stairs > 0) {
			if (GoodDistance(skillX, skillY, getPosition().getX(), getPosition().getY(), stairDistance)) {
				stairs(stairs, getPosition().getX(), getPosition().getY());
			}
		}
		// check banking
		if (WanneBank > 0) {
			if (GoodDistance(skillX, skillY, getPosition().getX(), getPosition().getY(), WanneBank)) {
				openUpBank();
				WanneBank = 0;
			}
		}
		// check shopping
		if (WanneShop > 0) {
			if (GoodDistance(skillX, skillY, getPosition().getX(), getPosition().getY(), 1)) {
				openUpShop(WanneShop);
				WanneShop = 0;
			}
		}
		// woodcutting check
		if (woodcuttingIndex >= 0) {
			if (GoodDistance(skillX, skillY, getPosition().getX(), getPosition().getY(), 3)) {
				send(new RemoveInterfaces());
				woodcutting();
			}
		}

		// Attacking in wilderness
		if (attackingPlayer && deathStage == 0) {
			attackTarget(this);
		}
		// Attacking an NPC
		else if (attackingNpc && deathStage == 0) {
			attackTarget(this);
		}
		// If killed apply dead
		if (deathStage == 0 && getCurrentHealth() < 1) {
			if(target instanceof Npc) {
				Npc npc = Server.npcManager.getNpc(target.getSlot());
				npc.removeEnemy(this);
			} else {
				Client p = getClient(duel_with);
				if (duel_with > 0 && validClient(duel_with) && inDuel && duelFight) {
					p.duelWin = true;
					p.DuelVictory();
				}
			}
			resetAttack(); //Should reset values correctly ?!
			requestAnim(836, 5);
			setCurrentHealth(0);
			deathStage++;
			deathTimer = System.currentTimeMillis();
			prayers.reset();
			send(new SendMessage("Oh dear you have died!"));
		} else if (deathStage == 1 && now - deathTimer >= 1800) {
			getPosition().moveTo(2604 + Misc.random(6), 3101 + Misc.random(3), 0); //Update position!
			teleportToX = getPosition().getX();
			teleportToY = getPosition().getY();
			deathStage = 0;
			deathTimer = 0;
			/* Stats check! */
			for(int i = 0; i < boostedLevel.length; i++) {
				if(i == 3)
					heal(Skills.getLevelForExperience(getExperience(Skill.HITPOINTS)));
				else if (i == 5)
					setCurrentPrayer(Skills.getLevelForExperience(getExperience(Skill.PRAYER)));
				else
					boostedLevel[i] = 0;
				refreshSkill(Skill.getSkill(i));
			}
			/* Death in other content! */
			if (inWildy())
				died();
			if (getSkullIcon() >= 0)
				setSkullIcon(-1);
			/* Item check !*/
			GetBonus(true);
			requestAnims(getEquipment()[3]);
			getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
		}
		if (smithing[4] > 0) {
			if (GoodDistance(skillX, skillY, getPosition().getX(), getPosition().getY(), 1)) {
				smithing();
			}
		}
		if (smelting && now - lastAction >= 1800) {
			lastAction = now;
			smelt(smelt_id);
		} else if (goldCrafting && now - lastAction >= 1800) {
			lastAction = now;
			goldCraft();
		} else if (shafting && now - lastAction >= 1800) {
			lastAction = now;
			shaft();
		} else if (fletchings && now - lastAction >= 1800) {
			lastAction = now;
			fletching.fletchBow(this);
		} else if (fletchingOther && now - lastAction >= fletchOtherTime) {
			lastAction = now;
			fletching.fletchOther(this);
		} else if (filling) {
			lastAction = now;
			fill();
		} else if (spinning && now - lastAction >= getSpinSpeed()) {
			lastAction = now;
			spin();
		} else if (boneItem > 0 && now - lastAction >= 1800) {
			lastAction = now;
			stillgfx(624, new Position(skillY, skillX, getPosition().getZ()), 0);
			Prayer.altarBones(this, boneItem);
		} else if (mixPots && now - lastAction >= potTime) {
			lastAction = now;
			mixPots();
		} else if (crafting && now - lastAction >= 1800) {
			lastAction = now;
			craft();
		} else if (fishing && now - lastAction >= Utils.fishTime[fishIndex]) {
			lastAction = now;
			fish();
		} else if (mining && now - lastAction >= getMiningSpeed()) {
			lastAction = now;
			mining(mineIndex);
		} else if (mining && now - lastPickAction >= 600) {
			lastPickAction = now;
			requestAnim(getMiningEmote(Utils.picks[minePick]), 0);
		} else if (cooking && now - lastAction >= 1800) {
			lastAction = now;
			cook();
		}
		// Snowing
		// Npc Talking
		if (NpcWanneTalk == 2) { // Bank Booth
			if (GoodDistance2(getPosition().getX(), getPosition().getY(), skillX, skillY, 1)) {
				NpcDialogue = 1;
				NpcTalkTo = 494;
				NpcWanneTalk = 0;
			}
		} else if (NpcWanneTalk > 0) {
			if (GoodDistance2(getPosition().getX(), getPosition().getY(), skillX, skillY, 2))
				if (NpcWanneTalk == 804) {
					openTan();
					NpcWanneTalk = 0;
				} else {
					NpcDialogue = NpcWanneTalk;
					NpcTalkTo = GetNPCID(skillX, skillY);
					NpcWanneTalk = 0;
				}
		}
		if (NpcDialogue > 0 && !NpcDialogueSend) {
			UpdateNPCChat();
		}

		if (isKicked) {
			disconnected = true;
			if (saveNeeded) {
				saveStats(true);
			}
			getOutputStream().createFrame(109);
		}

		/* Items update! Might cause some lag if more than 500 items?! */
		/* TODO: Add better way of handling ground items! */
		if (!Ground.items.isEmpty() || !(Ground.items.size() < 0)) {
			for (GroundItem item : Ground.items) {
				if (!item.canDespawn && item.taken && now - item.dropped >= item.timeDisplay) {
					item.taken = false;
					item.visible = false;
				}
				if (!item.visible && (now - item.dropped >= item.timeDisplay || !item.canDespawn) || (!item.visible && !item.canDespawn && !item.taken) && (now - item.dropped >= item.timeDisplay || !item.canDespawn)) {
						if (Server.itemManager.isTradable(item.id) && dbId != item.playerId
								&& Math.abs(getPosition().getX() - item.x) <= 114 && Math.abs(getPosition().getY() - item.y) <= 114 && getPosition().getZ() == item.z) {
							send(new CreateGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
						}
					}
					item.visible = true;
					if (item.canDespawn && item.visible && now - item.dropped >= (item.timeDisplay + item.timeDespawn)) {
						Ground.deleteItem(item);
					}
				}
			}

		if (Server.updateRunning && now - Server.updateStartTime > (Server.updateSeconds * 1000L)) {
			logout();
		}
	}

	public boolean packetProcess() { //Packet fixed?!
		if (disconnected) {
			return false;
		}
		Queue<PacketData> data = mySocketHandler.getPackets();
		if (data == null || data.isEmpty() || data.stream() == null)
			return false;
		try {
			fillInStream(data.poll());
		} catch (IOException e) {
			e.printStackTrace();
			//System.out.println("Player" + getPlayerName() + " disconnected.");
			System.out.println(e);
			//saveStats(true); //Not sure if needed yet!
			disconnected = true;
			return false;
		}
		parseIncomingPackets();
		return true;
	}

	public PacketData currentPacket;

	public void parseIncomingPackets() {
		lastPacket = System.currentTimeMillis();
		PacketHandler.process(this, currentPacket.getId(), currentPacket.getLength());
	}

	public void changeInterfaceStatus(int inter, boolean show) {
		getOutputStream().createFrame(171);
		getOutputStream().writeByte((byte) (!show ? 1 : 0));
		getOutputStream().writeWord(inter);
	}

	public void setMenuItems(int[] items) {
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(8847);
		getOutputStream().writeWord(items.length);

		for (int i = 0; i < items.length; i++) {
			getOutputStream().writeByte((byte) 1);
			getOutputStream().writeWordBigEndianA(items[i] + 1);
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public void setMenuItems(int[] items, int[] amount) {
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(8847);
		getOutputStream().writeWord(items.length);

		for (int i = 0; i < items.length; i++) {
			getOutputStream().writeByte((byte) amount[i]);
			getOutputStream().writeWordBigEndianA(items[i] + 1);
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public int currentSkill = -1;

	public void showSkillMenu(int skillID, int child) throws IOException {
		if (currentSkill != skillID)
			send(new RemoveInterfaces());
		int slot = 8720;
		for (int i = 0; i < 80; i++) {
			send(new SendString("", slot));
			slot++;
		}
		if (skillID < 23) {
			changeInterfaceStatus(15307, false);
			changeInterfaceStatus(15304, false);
			changeInterfaceStatus(15294, false);
			changeInterfaceStatus(8863, false);
			changeInterfaceStatus(8860, false);
			changeInterfaceStatus(8850, false);
			changeInterfaceStatus(8841, false);
			changeInterfaceStatus(8838, false);
			changeInterfaceStatus(8828, false);
			changeInterfaceStatus(8825, true);
			changeInterfaceStatus(8813, true);
			send(new SendString("", 8849));
		}
		if (skillID == 0) {
			send(new SendString("Attack", 8846));
			send(new SendString("Defence", 8823));
			send(new SendString("Range", 8824));
			send(new SendString("Magic", 8827));
			String prem = " @red@(Premium only)";
			slot = 8760;
			String[] s = {"Abyssal Whip", "Bronze", "Iron", "Steel", "Mithril", "Adamant", "Rune", "Unholy book", "Unholy blessing", "Granite longsword", "Obsidian weapon", "Dragon",
					"Skillcape" + prem};
			String[] s1 = {"1", "1", "1", "10", "20", "30", "40", "45", "45", "50", "55", "60", "99"};
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			int[] items = {4151, 1291, 1293, 1295, 1299, 1301, 1303, 3842, 20223, 21646, 6523, 1305, 9747};
			setMenuItems(items);
		} else if (skillID == 1) {
			send(new SendString("Attack", 8846));
			send(new SendString("Defence", 8823));
			send(new SendString("Range", 8824));
			send(new SendString("Magic", 8827));
			String prem = " @red@(Premium only)";
			slot = 8760;
			String[] s = {"Skeletal", "Bronze", "Iron", "Steel", "Mithril", "Splitbark", "Adamant", "Rune", "Ancient blessing", "Granite", "Obsidian", "Dragon", "Crystal shield (with 60 agility)", "Dragonfire shield",
					"Skillcape" + prem};
			String[] s1 = {"1", "1", "1", "10", "20", "20", "30", "40", "45", "50", "55", "60", "70", "75", "99"};
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			int[] items = {6139, 1117, 1115, 1119, 1121, 3387, 1123, 1127, 20235, 10564, 21301, 3140, 4224, 11284, 9753};
			setMenuItems(items);
		} else if (skillID == 2) {
			send(new SendString("Strength", 8846));
			changeInterfaceStatus(8825, false);
			changeInterfaceStatus(8813, false);
			slot = 8760;
			String prem = " @red@(Premium only)";
			String[] s = {"Unholy book", "Unholy blessing", "War blessing", "Granite maul", "Obsidian maul", "Skillcape" + prem};
			String[] s1 = {"45", "45", "45", "50", "55", "99"};
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			int[] items = {3842, 20223, 20232, 4153, 6528, 9750};
			setMenuItems(items);
		} else if (skillID == 3) {
			send(new SendString("Hitpoints", 8846));
			changeInterfaceStatus(8825, false);
			changeInterfaceStatus(8813, false);
			send(new SendString("Health restore", 8716));
			slot = 8760;
			String prem = " @red@(Premium only)";
			String[] s = {"Shrimps (3 health)", "Rat meat (3 health)", "Bread (5 health)", "Thin snail (7 health)", "Trout (8 health)", "Salmon (10 health)", "Lobster (12 health)", "Swordfish (14 health)", "Monkfish (16 health)" + prem, "Shark (20 health)",
					"Sea Turtle (22 health)" + prem, "Manta Ray (24 health)" + prem};
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			int[] items = {315, 2142, 2309, 3369, 333, 329, 379, 373, 7946, 385, 397, 391};
			setMenuItems(items);
		} else if (skillID == 4) {
			changeInterfaceStatus(8825, false);
			send(new SendString("Bows", 8846));
			send(new SendString("Armour", 8823));
			send(new SendString("Misc", 8824));
			String prem = " @red@(Premium only)";
			slot = 8760;
			String[] s = new String[0];
			String[] s1 = new String[0];
			if (child == 0) {
				s = new String[]{"Oak bow", "Willow bow", "Maple bow", "Yew bow", "Magic bow", "Crystal bow", "Seercull"/*, "Twisted bow"*/};
				s1 = new String[]{"1", "20", "30", "40", "50", "65", "75"/*, "85"*/};
			} else if (child == 1) {
				s = new String[]{"Leather", "Green dragonhide body (with 40 defence)", "Green dragonhide chaps",
						"Green dragonhide vambraces", "Book of balance", "Peaceful blessing", "Honourable blessing", "Blue dragonhide body (with 40 defence)", "Blue dragonhide chaps",
						"Blue dragonhide vambraces", "Red dragonhide body (with 40 defence)", "Red dragonhide chaps",
						"Red dragonhide vambraces", "Black dragonhide body (with 40 defence)", "Black dragonhide chaps",
						"Black dragonhide vambraces", "Spined"};
				s1 = new String[]{"1", "40", "40", "40", "45", "45", "45", "50", "50", "50", "60", "60", "60", "70", "70", "70", "75"};
			} else if (child == 2) {
				s = new String[]{"Bronze arrow", "Iron arrow", "Steel arrow", "Mithril arrow", "Adamant arrow", "Rune arrow", "Dragon arrow", "Skillcape" + prem};
				s1 = new String[]{"1", "1", "10", "20", "30", "40", "60", "99"};
			}
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			if (child == 0)
				setMenuItems(new int[]{843, 849, 853, 857, 861, 4212, 6724/*, 20997*/});
			else if (child == 1)
				setMenuItems(new int[]{1129, 1135, 1099, 1065, 3844, 20226, 20229, 2499, 2493, 2487, 2501, 2495, 2489, 2503, 2497, 2491, 6133});
			else if (child == 2)
				setMenuItems(new int[]{882, 884, 886, 888, 890, 892, 11212, 9756});
		} else if (skillID == 6) { // Magic need to be done?
			changeInterfaceStatus(8825, false);
			send(new SendString("Spells", 8846));
			send(new SendString("Armor", 8823));
			send(new SendString("Misc", 8824));
			String prem = " @red@(Premium only)";
			slot = 8760;
			String[] s = new String[0];
			String[] s1 = new String[0];
			if (child == 0) {
				s = new String[]{"High Alch", "Smoke Rush", "Enchant Sapphire", "Shadow Rush", "Blood Rush", "Enchant Emerald", "Ice Rush", "Smoke Burst", "Superheat", "Enchant Ruby", "Shadow Burst", "Enchant Diamond", "Blood Burst", "Enchant Dragonstone", "Ice Burst", "Smoke Blitz", "Shadow Blitz", "Blood Blitz", "Ice Blitz", "Smoke Barrage", "Enchant Onyx", "Shadow Barrage", "Blood Barrage", "Ice Barrage"};
				s1 = new String[]{"1", "1", "7", "10", "20", "27", "30", "40", "43", "49", "50", "57", "60", "68", "70", "74", "76", "80", "82", "86", "87", "88", "92", "94"};
			} else if (child == 1) {
				s = new String[]{"Blue Mystic", "White Mystic", "Splitbark (with 20 defence)", "Black Mystic", "Holy book", "Holy blessing", "Infinity"};
				s1 = new String[]{"1", "20", "20", "35", "45", "45", "50"};
			} else if (child == 2) {
				s = new String[]{"Zamorak staff", "Saradomin staff", "Guthix staff", "Ancient staff", "Obsidian staff", "Master wand", "Skillcape" + prem};
				s1 = new String[]{"1", "1", "1", "25", "40", "50", "99"};
			}
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			if (child == 0)
				setMenuItems(new int[]{561, 565, 564, 565, 565, 564, 565, 565, 561, 564, 565, 564, 565, 564, 565, 565, 565, 565, 565, 565, 564, 565, 565, 565},
						new int[]{1, 1, 10, 1, 1, 10, 1, 1, 1, 10, 1, 10, 1, 10, 1, 1, 1, 1, 1, 1, 10, 1, 1, 1});
			else if (child == 1)
				setMenuItems(new int[]{4089, 4109, 3385, 4099, 3840, 20220, 6918});
			else if (child == 2)
				setMenuItems(new int[]{2417, 2415, 2416, 4675, 6526, 6914, 9762});
		} else if (skillID == 17) {
			send(new SendString("Thieving", 8846));
			changeInterfaceStatus(8825, false);
			changeInterfaceStatus(8813, false);
			slot = 8760;
			String prem = " @red@(Premium only)";
			String[] s = {"Cage", "Farmer", "Baker stall", "fur stall", "silver stall", "Master Farmer", "Yanille chest", "Spice Stall", "Legends chest" + prem, "Gem Stall" + prem};
			String[] s1 = {"1", "10", "10", "40", "65", "70", "70", "80", "85", "90"};
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			int[] items = {4443, 3243, 2309, 1739, 2349, 5068, 6759, 199, 6759, 1623};
			setMenuItems(items);
		} else if (skillID == 22) {
			send(new SendString("Runecrafting", 8846));
			changeInterfaceStatus(8825, false);
			changeInterfaceStatus(8813, false);
			slot = 8760;
			String prem = " @red@(Premium only)";
			String[] s = {"Small pouch", "Nature rune", "Medium pouch", "Large pouch", "Blood rune", "Giant pouch", "Cosmic rune", "Skillcape" + prem};
			String[] s1 = {"1", "1", "20", "40", "50", "60", "75", "99"};
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			int[] items = {5509, 561, 5510, 5512, 565, 5514, 564, 9765};
			setMenuItems(items);
			//crafting == 12
//    } else if (skillID == 12) {
//        send(new SendString("Fishing", 8846));
//        changeInterfaceStatus(8825, false);
//        changeInterfaceStatus(8813, false);
//        slot = 8760;
//        String prem = " @red@(Premium only)";
//        String[] s = { "Flax", "Trout", "Salmon", "Lobster", "Swordfish", "Monkfish" + prem, "Shark",
//            "Sea Turtle" + prem, "Manta Ray" + prem, "" };
//        String[] s1 = { "1", "20", "30", "40", "50", "60", "70", "85", "95" };
//        for (int i = 0; i < s.length; i++) {
//          send(new SendString(s[i], slot++));
//        }
//        slot = 8720;
//        for (int i = 0; i < s1.length; i++) {
//          send(new SendString(s1[i], slot++));
//        }
//        int items[] = { 317, 335, 331, 377, 371, 7944, 383, 395, 389 };
//        setMenuItems(items);
		} else if (skillID == 9) {
			send(new SendString("Fishing", 8846));
			changeInterfaceStatus(8825, false);
			changeInterfaceStatus(8813, false);
			slot = 8760;
			String prem = " @red@(Premium only)";
			String[] s = {"Shrimps", "Trout", "Salmon", "Lobster", "Swordfish", "Monkfish" + prem, "Shark",
					"Sea Turtle" + prem, "Manta Ray" + prem, ""};
			String[] s1 = {"1", "20", "30", "40", "50", "60", "70", "85", "95"};
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			int[] items = {317, 335, 331, 377, 371, 7944, 383, 395, 389};
			setMenuItems(items);
		} else if (skillID == Skill.CRAFTING.getId()) {
			changeInterfaceStatus(8827, true);
			send(new SendString("Spinning", 8846));
			send(new SendString("Armor", 8823));
			send(new SendString("Jewelry", 8824));
			send(new SendString("Other", 8827));

			String prem = " @red@(Premium only)";
			slot = 8760;
			String[] s = new String[0];
			String[] s1 = new String[0];
			if (child == 0) {
				s = new String[]{"Ball of wool", "Bow string", "2 tick stringing", "1 tick stringing"};
				s1 = new String[]{"1", "10", "40", "70"};
			} else if (child == 1) {
				s = new String[]{"Leather gloves", "Leather boots", "Leather cowl", "Leather vambraces",
						"Leather body", "Leather chaps", "Coif", "Green d'hide vamb", "Green d'hide chaps",
						"Green d'hide body", "Blue d'hide vamb", "Blue d'hide chaps", "Blue d'hide body",
						"Red d'hide vamb", "Red d'hide chaps", "Red d'hide body", "Black d'hide vamb",
						"Black d'hide chaps", "Black d'hide body"};
				s1 = new String[]{"1", "7", "9", "11", "14", "18", "39", "50", "54", "58", "62", "66", "70", "73",
						"76", "79", "82", "85", "88"};
			} else if (child == 2) {
				s = new String[]{"Gold ring", "Gold necklace", "Gold bracelet", "Gold amulet", "Cut sapphire",
						"Sapphire ring", "Sapphire necklace", "Sapphire bracelet", "Sapphire amulet", "Cut emerald",
						"Emerald ring", "Emerald necklace", "Emerald bracelet", "Emerald amulet", "Cut ruby",
						"Ruby ring", "Ruby necklace", "Ruby bracelet", "Cut diamond", "Diamond ring", "Ruby amulet",
						"Cut dragonstone", "Dragonstone ring", "Diamond necklace", "Diamond bracelet", "Cut onyx",
						"Onyx ring", "Diamond amulet", "Dragonstone necklace", "Dragonstone bracelet",
						"Dragonstone amulet", "Onyx necklace", "Onyx bracelet", "Onyx amulet"};
				s1 = new String[]{"5", "6", "7", "8", "20", "20", "22", "23", "24", "27", "27", "29", "30", "31",
						"34", "34", "40", "42", "43", "43", "50", "55", "55", "56", "58", "67", "67", "70", "72", "74",
						"80", "82", "84", "90"};
			} else if (child == 3) {
				s = new String[]{"Skillcape" + prem};
				s1 = new String[]{"99"};
			}
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			if (child == 0)
				setMenuItems(new int[]{1759, 1777});
			else if (child == 1)
				setMenuItems(new int[]{1059, 1061, 1167, 1063, 1129, 1095, 1169, 1065, 1099, 1135, 2487, 2493, 2499,
						2489, 2495, 2501, 2491, 2497, 2503});
			else if (child == 2)
				setMenuItems(new int[]{1635, 1654, 11069, 1692, 1607, 1637, 1656, 11072, 1694, 1605, 1639, 1658,
						11076, 1696, 1603, 1641, 1660, 11085, 1601, 1643, 1698, 1615, 1645, 1662, 11092, 6573, 6575,
						1700, 1664, 11115, 1702, 6577, 11130, 6581});
			else if (child == 3)
				setMenuItems(new int[]{9780});
		} else if (skillID == Skill.SMITHING.getId()) {
			changeInterfaceStatus(8827, true);
			changeInterfaceStatus(8828, true);
			changeInterfaceStatus(8838, true);
			changeInterfaceStatus(8841, true);
			changeInterfaceStatus(8850, true);
			send(new SendString("Smelting", 8846));
			send(new SendString("Bronze", 8823));
			send(new SendString("Iron", 8824));
			send(new SendString("Steel", 8827));
			send(new SendString("Mithril", 8837));
			send(new SendString("Adamant", 8840));
			send(new SendString("Runite", 8843));
			send(new SendString("Special", 8859));

			String prem = " @red@(Premium only)";
			slot = 8760;
			String[] s = new String[0];
			String[] s1 = new String[0];
			int[] item = new int[0];
			int[] amt = new int[0];
			if (child == 0) {
				s = new String[]{"Bronze bar", "Iron bar (" + (50 + ((getLevel(Skill.SMITHING) + 1) / 4)) + "% success)", "Steel bar (2 coal & 1 iron ore)",
						"Gold bar", "Mithril bar (3 coal & 1 mithril ore)", "Adamantite bar (4 coal & 1 adamantite ore)", "Runite bar (6 coal & 1 runite ore)"};
				s1 = new String[]{"1", "15", "30", "40", "55", "70", "85"};
			} else if (child > 0 && child <= Constants.smithing_frame.length) {
				s = new String[Constants.smithing_frame[child - 1].length];
				s1 = new String[Constants.smithing_frame[child - 1].length];
				item = new int[Constants.smithing_frame[child - 1].length];
				amt = new int[Constants.smithing_frame[child - 1].length];
				for (int i = 0; i < s.length; i++) {
					item[i] = Constants.smithing_frame[child - 1][i][0];
					amt[i] = Constants.smithing_frame[child - 1][i][1];
					s[i] = this.GetItemName(item[i]);
					s1[i] = String.valueOf(Constants.smithing_frame[child - 1][i][2]);
				}
			} else if (child == Constants.smithing_frame.length + 1) {
				s = new String[]{"Skillcape" + prem};
				s1 = new String[]{"99"};
			}
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}

			if (child == 0)
				setMenuItems(new int[]{2349, 2351, 2353, 2357, 2359, 2361, 2363});
			else if (child > 0 && child <= Constants.smithing_frame.length)
				setMenuItems(item, amt);
			else if (child == Constants.smithing_frame.length + 1)
				setMenuItems(new int[]{9795});
		} else if (skillID == 10) {
			send(new SendString("Cooking", 8846));
			changeInterfaceStatus(8825, false);
			changeInterfaceStatus(8813, false);
			slot = 8760;
			String prem = " @red@(Premium only)";
			String[] s = {"Shrimps", "Meat", "Bread", "Thin snail", "Trout", "Salmon", "Lobster", "Swordfish", "Monkfish" + prem, "Shark",
					"Sea Turtle" + prem, "Manta Ray" + prem};
			String[] s1 = {"1", "1", "10", "15", "20", "30", "40", "50", "60", "70", "85", "95"};
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			int[] items = {315, 2142, 2309, 3369, 333, 329, 379, 373, 7946, 385, 397, 391};
			setMenuItems(items);
		} else if (skillID == 16) {
			send(new SendString("Agility", 8846));
			changeInterfaceStatus(8825, false);
			changeInterfaceStatus(8813, false);
			slot = 8760;
			String prem = " @red@(Premium only)";
			String[] s = {"Gnome Course", "Barbarian Course", "Yanille castle wall", "Crystal Shield", "Taverly dungeon shortcut", "Wilderness course", "Prime boss shortcut", "Skillcape" + prem};
			String[] s1 = {"1", "40", "50", "60", "70", "70", "85", "99"};
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			int[] items = {751, 1365, -1, 4224, 4155, 964, 4155, 9771};
			setMenuItems(items);
		} else if (skillID == 7) {
			send(new SendString("Axes", 8846));
			send(new SendString("Logs", 8823));
			send(new SendString("Misc", 8824));
			changeInterfaceStatus(8825, false);
			String prem = " @red@(Premium only)";
			slot = 8760;
			String[] s = new String[0];
			String[] s1 = new String[0];
			if (child == 0) {
				s = new String[]{"Bronze Axe", "Iron Axe", "Steel Axe", "Mithril Axe", "Adamant Axe", "Rune Axe",
						"Dragon Axe"};
				s1 = new String[]{"1", "1", "6", "21", "31", "41", "61"};
			} else if (child == 1) {
				s = new String[]{"Logs", "Oak logs", "Willow logs", "Maple logs", "Yew logs", "Magic logs"};
				s1 = new String[]{"1", "15", "30", "45", "60", "75"};
			} else if (child == 2) {
				s = new String[]{"Skillcape" + prem};
				s1 = new String[]{"99"};
			}
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			if (child == 0)
				setMenuItems(new int[]{1351, 1349, 1353, 1355, 1357, 1359, 6739});
			else if (child == 1)
				setMenuItems(new int[]{1511, 1521, 1519, 1517, 1515, 1513});
			else if (child == 2)
				setMenuItems(new int[]{9807});
		} else if (skillID == Skill.MINING.getId()) {
			send(new SendString("Pickaxes", 8846));
			send(new SendString("Ores", 8823));
			send(new SendString("Misc", 8824));
			changeInterfaceStatus(8825, false);
			String prem = " @red@(Premium only)";
			slot = 8760;
			String[] s = new String[0];
			String[] s1 = new String[0];
			if (child == 0) {
				s = new String[]{"Bronze Pickaxe", "Iron Pickaxe", "Steel Pickaxe", "Mithril Pickaxe", "Adamant Pickaxe",
						"Rune Pickaxe", "Dragon Pickaxe"};
				s1 = new String[]{"1", "1", "6", "21", "31", "41", "61"};
			} else if (child == 1) {
				s = new String[]{"Rune essence", "Copper ore", "Tin ore", "Iron ore", "Coal", "Gold ore", "Mithril ore",
						"Adamant ore", "Runite ore"};
				s1 = new String[]{"1", "1", "1", "15", "30", "40", "55", "70", "85"};
			} else if (child == 2) {
				s = new String[]{"Skillcape" + prem};
				s1 = new String[]{"99"};
			}
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			if (child == 0)
				setMenuItems(new int[]{1265, 1267, 1269, 1273, 1271, 1275, 11920});
			else if (child == 1)
				setMenuItems(new int[]{1436, 436, 438, 440, 453, 444, 447, 449, 451});
			else if (child == 2)
				setMenuItems(new int[]{9792});
		} else if (skillID == 18) {
			send(new SendString("Master", 8846));
			send(new SendString("Monsters", 8823));
			send(new SendString("Misc", 8824));
			changeInterfaceStatus(8825, false);
			String prem = " @red@(Premium only)";
			slot = 8760;
			String[] s = new String[0];
			String[] s1 = new String[0];
			if (child == 0) {
				s = new String[]{"Mazchna (level 3 combat)", "Vannaka (level 3 combat)", "Duradel (level 50 combat)"};
				s1 = new String[]{"1", "50", "50"};
			} else if (child == 1) {
				s = new String[]{"Crawling hands", "Pyrefiend", "Albino bat", "Death spawn", "Jelly", "Head mourner", "Jungle horrors", "Skeletal hellhound", "Lesser demon", "Bloodvelds", "Greater demon", "Black demon", "Gargoyles", "Cave horrors", "Berserker Spirit", "Aberrant Spectres", "Tzhaar", "Mithril Dragon", "Abyssal demon", "Dagannoth Prime"};
				s1 = new String[]{"1", "20", "25", "30", "30", "45", "45", "50", "50", "53", "55", "60", "63", "65", "70", "73", "80", "83", "85", "90"};
			} else if (child == 2) {
				s = new String[]{"Skillcape" + prem};
				s1 = new String[]{"99"};
			}
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			if (child == 0)
				setMenuItems(new int[]{4155});
			else if (child == 1)
				setMenuItems(new int[]{ 4133, 4138, -1, -1, 4142, -1, -1, -1, -1, 4141, -1, -1, 4147, 8900, -1, 4144, -1, -1, 4149, -1 });
			else if (child == 2)
				setMenuItems(new int[]{9786});
		} else if (skillID == 8) {
			send(new SendString("Firemaking", 8846));
			changeInterfaceStatus(8825, false);
			changeInterfaceStatus(8813, false);
			changeInterfaceStatus(8825, false);
			String prem = " @red@(Premium only)";
			slot = 8760;
			String[] s;
			String[] s1;
			s = new String[]{"Logs", "Oak logs", "Willow logs", "Maple logs", "Yew logs", "Magic logs",
					"Skillcape" + prem};
			s1 = new String[]{"1", "15", "30", "45", "60", "75", "99"};
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			setMenuItems(new int[]{1511, 1521, 1519, 1517, 1515, 1513, 9804});
		} else if (skillID == Skill.HERBLORE.getId()) {
			send(new SendString("Potions", 8846));
			send(new SendString("Herbs", 8823));
			send(new SendString("Misc", 8824));
			changeInterfaceStatus(8825, false);
			String prem = " @red@(Premium only)";
			slot = 8760;
			String[] s = new String[0];
			String[] s1 = new String[0];
			if (child == 0) {
				s = new String[]{"Attack Potion \\nGuam leaf & eye of newt", "Strength Potion\\nTarromin & limpwurt root", "Defence Potion\\nRanarr weed & white berries", "Prayer potion\\nRanarr weed & snape grass",
						"Super Attack Potion\\nIrit leaf & eye of newt", "Super Strength Potion\\nKwuarm & limpwurt root", "Super Restore Potion\\nSnapdragon & red spiders' eggs", "Super Defence Potion\\nCadantine & white berries",
						"Ranging Potion\\nDwarf weed & wine of Zamorak"};
				s1 = new String[]{"3", "14", "25", "38", "46", "55", "60", "65", "75"};
			} else if (child == 1) {
				String[][] array = new String[2][Utils.grimy_herbs.length];
				for(int i = 0; i < array[1].length; i++) {
					array[0][i] = GetItemName(Utils.grimy_herbs[i] != 3051 && Utils.grimy_herbs[i] != 3049 ? Utils.grimy_herbs[i] + 50 : Utils.grimy_herbs[i] - 51);
					array[1][i] = Integer.toString(Utils.grimy_herbs_lvl[i]);
				}
				s = array[0];
				s1 = array[1];
			} else if (child == 2) {
				s = new String[]{"Skillcape" + prem};
				s1 = new String[]{"99"};
			}
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			if (child == 0)
				setMenuItems(new int[]{121, 115, 133, 139, 145, 157, 3026, 163, 169});
			else if (child == 1)
				setMenuItems(Utils.grimy_herbs);
			else if (child == 2)
				setMenuItems(new int[]{9774});
		} else if (skillID == 11) {
			send(new SendString("Fletching", 8846));
			changeInterfaceStatus(8825, false);
			changeInterfaceStatus(8813, false);
			slot = 8760;
			String[] s = {"Arrow Shafts", "Oak Shortbow", "Oak Longbow", "Willow Shortbow", "Willow Longbow",
					"Maple Shortbow", "Maple Longbow", "Yew Shortbow", "Yew Longbow", "Magic Shortbow", "Magic Longbow"};
			String[] s1 = {"1", "20", "25", "35", "40", "50", "55", "65", "70", "80", "85"};
			for (int i = 0; i < s.length; i++) {
				send(new SendString(s[i], slot++));
			}
			slot = 8720;
			for (int i = 0; i < s1.length; i++) {
				send(new SendString(s1[i], slot++));
			}
			int[] items = {52, 54, 56, 60, 58, 64, 62, 68, 66, 72, 70};
			setMenuItems(items);
		}
		/*
		 * if (skillID == 0) { send(new SendString("Attack", 8846)); send(new
		 * SendString("Defence", 8823)); send(new SendString("Range", 8824));
		 * send(new SendString("Magic", 8827)); slot = 8760; String[] s = {
		 * "Abyssal Whip", "Bronze", "Iron", "Steel", "Mithril", "Adamant", "Rune",
		 * "Dragon", "Decorative Sword" }; String[] s1 = { "1", "1", "1", "10",
		 * "20", "30", "40", "60", "80" }; for (int i = 0; i < s.length; i++) {
		 * send(new SendString(s[i], slot++)); } slot = 8720; for (int i = 0; i <
		 * s1.length; i++) { send(new SendString(s1[i], slot++)); } int items[] = {
		 * 4151, 1291, 1293, 1295, 1299, 1301, 1303, 1305, 4068 };
		 * setMenuItems(items); } else if (skillID == 1) { send(new
		 * SendString("Attack", 8846)); send(new SendString("Defence", 8823));
		 * send(new SendString("Range", 8824)); send(new SendString("Magic", 8827));
		 * slot = 8760; String[] s = { "Skeletal", "Bronze", "Iron", "Steel",
		 * "Mithril", "Adamant", "Rune", "Dragon", "Barrows" }; String[] s1 = { "1",
		 * "1", "1", "10", "20", "30", "40", "60", "90" }; for (int i = 0; i <
		 * s.length; i++) { send(new SendString(s[i], slot++)); } slot = 8720; for
		 * (int i = 0; i < s1.length; i++) { send(new SendString(s1[i], slot++)); }
		 * int items[] = { 6139, 1117, 1115, 1119, 1121, 1123, 1127, 3140, 4964 };
		 * setMenuItems(items); } else if (skillID == 4) {
		 * changeInterfaceStatus(8825, false); send(new SendString("Bows", 8846));
		 * send(new SendString("Armour", 8823)); send(new SendString("Misc", 8824));
		 * slot = 8760; String[] s = new String[0]; String[] s1 = new String[0]; if
		 * (child == 0) { s = new String[] { "Oak bow", "Willow bow", "Maple bow",
		 * "Yew bow", "Magic bow", "Crystal bow" }; s1 = new String[] { "1", "20",
		 * "30", "40", "50", "70" }; } else if (child == 1) { s = new String[] {
		 * "Leather", "Green dragonhide body (with 40 defence)",
		 * "Green dragonhide chaps", "Green dragonhide vambraces",
		 * "Blue dragonhide body (with 40 defence)", "Blue dragonhide chaps",
		 * "Blue dragonhide vambraces", "Red dragonhide body (with 40 defence)",
		 * "Red dragonhide chaps", "Red dragonhide vambraces",
		 * "Black dragonhide body (with 40 defence)", "Black dragonhide chaps",
		 * "Black dragonhide vambraces", "Snakeskin body (with 60 defence)",
		 * "Snakeskin chaps (with 60 defence)", "Snakeskin boots",
		 * "Snakeskin vambraces" }; s1 = new String[] { "1", "40", "40", "40", "50",
		 * "50", "50", "60", "60", "60", "70", "70", "70", "80", "80", "80", "80" };
		 * } for (int i = 0; i < s.length; i++) { send(new SendString(s[i],
		 * slot++)); } slot = 8720; for (int i = 0; i < s1.length; i++) { send(new
		 * SendString(s1[i], slot++)); } if (child == 0) setMenuItems(new int[] {
		 * 843, 849, 853, 857, 861, 4212 }); else if (child == 1) setMenuItems(new
		 * int[] { 1129, 1135, 1099, 1065, 2499, 2493, 2487, 2501, 2495, 2489, 2503,
		 * 2497, 2491, 6322, 6324, 6328, 6330 }); } else if (skillID == 17) {
		 * send(new SendString("Thieving", 8846)); changeInterfaceStatus(8825,
		 * false); changeInterfaceStatus(8813, false); slot = 8760; String[] s = {
		 * "Cages", "Bakers stall", "Fur Stall", "Silk Stall", "Spice Stall",
		 * "Gem Stall" }; String[] s1 = { "1", "10", "10", "40", "80", "92" }; for
		 * (int i = 0; i < s.length; i++) { send(new SendString(s[i], slot++)); }
		 * slot = 8720; for (int i = 0; i < s1.length; i++) { send(new
		 * SendString(s1[i], slot++)); } int items[] = { 4443, 2309, 314, 950, 253,
		 * 1631 }; setMenuItems(items); }
		 */
		sendQuestSomething(8717);
		if (currentSkill != skillID)
			showInterface(8714);
		currentSkill = skillID;
	}

	public static void publicyell(String message) {
		for (Player p : PlayerHandler.players) {
			if (p == null || !p.isActive) {
				continue;
			}
			Client temp = (Client) p;
			if (temp.getPosition().getX() > 0 && temp.getPosition().getY() > 0) {
				if (temp != null && !temp.disconnected && p.isActive) {
					temp.send(new SendMessage(message));
				}
			}
		}
	}

	public void yell(String message) {
		for (Player p : PlayerHandler.players) {
			if (p == null || !p.isActive)
				continue;
			Client temp = (Client) p;
			temp.send(new SendMessage(message + ":yell:"));
		}
	}

	public void yellKilled(String message) {
		for (Player p : PlayerHandler.players) {
			if (p == null || !p.isActive || !(p.inWildy() || p.inEdgeville()))
				continue;
			Client temp = (Client) p;
			temp.send(new SendMessage(message + ":yell:"));
		}
	}

	public int[] EssenceMineX = {2893, 2921, 2911, 2926, 2899};
	public int[] EssenceMineY = {4846, 4846, 4832, 4817, 4817};

	/*
	 * [0] North West [1] North East [2] Center [3] South East [4] South West
	 */
	public int[] EssenceMineRX = {3253, 3105, 2681, 2591};
	public int[] EssenceMineRY = {3401, 9571, 3325, 3086};

	/*
	 * [0] Varrock [1] Wizard Tower [2] Ardougne [3] Magic Guild
	 */
	private long stairBlock = 0;

	public boolean stairs(int stairs, int teleX, int teleY) {
		if (stairBlock > System.currentTimeMillis()) {
			resetStairs();
			System.out.println(getPlayerName() + " stair blocked!");
			return false;
		}
		stairBlock = System.currentTimeMillis() + 1000;
		if (!IsStair) {
			IsStair = true;
			if (stairs == 1) {
				if (skillX == 2715 && skillY == 3470) {
					if (getPosition().getY() < 3470 || getPosition().getX() < 2715) {
						// resetStairs();
						return false;
					} else {
						getPosition().setZ(1);
						teleportToX = teleX;
						teleportToY = teleY;
						resetStairs();
						return true;
					}
				}
			}
			if (stairs == "legendsUp".hashCode()) {
				if (skillX == 2732 && skillY == 3377) {
					getPosition().setZ(1);
					teleportToX = 2732;
					teleportToY = 3380;
					resetStairs();
					return true;
				}
			}
			if (stairs == "legendsDown".hashCode()) {
				if (skillX == 2732 && skillY == 3378) {
					getPosition().setZ(0);
					teleportToX = 2732;
					teleportToY = 3376;
					resetStairs();
					return true;
				}
			}
			if (stairs == 1) {
				getPosition().setZ(getPosition().getZ() + 1);
			} else if (stairs == 2) {
				getPosition().setZ(getPosition().getZ() - 1);
			} else if (stairs == 21) {
				getPosition().setZ(getPosition().getZ() + 1);
			} else if (stairs == 22) {
				getPosition().setZ(getPosition().getZ() - 1);
			} else if (stairs == 69)
				getPosition().setZ(getPosition().getZ() + 1);
			teleportToX = teleX;
			teleportToY = teleY;
			if (stairs == 3 || stairs == 5 || stairs == 9) {
				teleportToY += 6400;
			} else if (stairs == 4 || stairs == 6 || stairs == 10) {
				teleportToY -= 6400;
			} else if (stairs == 7) {
				teleportToX = 3104;
				teleportToY = 9576;
			} else if (stairs == 8) {
				teleportToX = 3105;
				teleportToY = 3162;
			} else if (stairs == 11) {
				teleportToX = 2856;
				teleportToY = 9570;
			} else if (stairs == 12) {
				teleportToX = 2857;
				teleportToY = 3167;
			} else if (stairs == 13) {
				getPosition().setZ(getPosition().getZ() + 3);
				teleportToX = skillX;
				teleportToY = skillY;
			} else if (stairs == 15) {
				teleportToY += (6400 - (stairDistance + stairDistanceAdd));
			} else if (stairs == 14) {
				teleportToY -= (6400 - (stairDistance + stairDistanceAdd));
			} else if (stairs == 16) {
				teleportToX = 2828;
				teleportToY = 9772;
			} else if (stairs == 17) {
				teleportToX = 3494;
				teleportToY = 3465;
			} else if (stairs == 18) {
				teleportToX = 3477;
				teleportToY = 9845;
			} else if (stairs == 19) {
				teleportToX = 3543;
				teleportToY = 3463;
			} else if (stairs == 20) {
				teleportToX = 3549;
				teleportToY = 9865;
			} else if (stairs == 21) {
				teleportToY += (stairDistance + stairDistanceAdd);
			} else if (stairs == 69) {
				teleportToY = stairDistanceAdd;
				teleportToX = stairDistance;
			} else if (stairs == 22) {
				teleportToY -= (stairDistance + stairDistanceAdd);
			} else if (stairs == 23) {
				teleportToX = 2480;
				teleportToY = 5175;
			} else if (stairs == 24) {
				teleportToX = 2862;
				teleportToY = 9572;
			} else if (stairs == 25) {
				Essence = (getPosition().getZ() / 4);
				getPosition().setZ(0);
				teleportToX = EssenceMineRX[Essence];
				teleportToY = EssenceMineRY[Essence];
			} else if (stairs == 26) {
				int EssenceRnd = Utils.random3(EssenceMineX.length);

				teleportToX = EssenceMineX[EssenceRnd];
				teleportToY = EssenceMineY[EssenceRnd];
				getPosition().setZ((Essence * 4));
			} else if (stairs == 27) {
				teleportToX = 2453;
				teleportToY = 4468;
			} else if (stairs == 28) {
				teleportToX = 3201;
				teleportToY = 3169;
			}
			if (stairs == 5 || stairs == 10) {
				teleportToX += (stairDistance + stairDistanceAdd);
				teleportToY = getPosition().getY();
				getPosition().setZ(0);
			}
			if (stairs == 6 || stairs == 9) {
				teleportToX -= (stairDistance - stairDistanceAdd);
			}
		}
		resetStairs();
		return true;
	}

	public boolean resetStairs() {
		stairs = 0;
		skillX = -1;
		setSkillY(-1);
		stairDistance = 1;
		stairDistanceAdd = 0;
		resetWalkingQueue();
		final Client p = this;
		EventManager.getInstance().registerEvent(new Event(500) {

			@Override
			public void execute() {
				p.resetWalkingQueue();
				stop();
			}

		});
		return true;
	}

	public boolean usingBow = false;

	public boolean IsItemInBag(int ItemID) {
		for (int playerItem : playerItems) {
			if ((playerItem - 1) == ItemID) {
				return true;
			}
		}
		return false;
	}

	public boolean AreXItemsInBag(int ItemID, int Amount) {
		int ItemCount = 0;

		for (int playerItem : playerItems) {
			if ((playerItem - 1) == ItemID) {
				ItemCount++;
			}
			if (ItemCount == Amount) {
				return true;
			}
		}
		return false;
	}

	public int GetItemSlot(int ItemID) {
		for (int i = 0; i < playerItems.length; i++) {
			if ((playerItems[i] - 1) == ItemID) {
				return i;
			}
		}
		return -1;
	}

	public int GetBankItemSlot(int ItemID) {
		for (int i = 0; i < bankItems.length; i++) {
			if ((bankItems[i] - 1) == ItemID) {
				return i;
			}
		}
		return -1;
	}

	public boolean randomed2;
	// private int setLastVote = 0;

	public void pmstatus(int status) { // status: loading = 0 connecting = 1
		// fine = 2
		getOutputStream().createFrame(221);
		getOutputStream().writeByte(status);
	}

	public boolean playerHasItem(int itemID) {
		itemID++;
        for (int playerItem : playerItems) {
            if (playerItem == itemID) {
                return true;
            }
        }
		return false;
	}

	public void wipeInv() {
		for (int i = 0; i < playerItems.length; i++) {
			if (playerItems[i] - 1 != 5733)
				deleteItem(playerItems[i] - 1, i, playerItemsN[i]);
		}
		send(new SendMessage("Your inventory has been wiped!"));
	}

	public boolean checkItem(int itemID) {
		itemID++;
        for (int playerItem : playerItems) {
            if (playerItem == itemID) {
                return true;
            }
        }
		for (int i = 0; i < getEquipment().length; i++) {
			if (getEquipment()[i] == itemID) {
				return true;
			}
		}
        for (int bankItem : bankItems) {
            if (bankItem == itemID) {
                return true;
            }
        }
		return false;
	}

	public boolean playerHasItem(int itemID, int amt) {
		itemID++;
		int found = 0;
		for (int i = 0; i < playerItems.length; i++) {
			if (playerItems[i] == itemID) {
				if (playerItemsN[i] >= amt) {
					return true;
				} else {
					found++;
				}
			}
		}
		return found >= amt;
	}

	public void sendpm(long name, int rights, byte[] chatmessage, int messagesize) {
		getOutputStream().createFrameVarSize(196);
		getOutputStream().writeQWord(name);
		getOutputStream().writeDWord(handler.lastchatid++); // must be different for
		// each message
		getOutputStream().writeByte(rights);
		getOutputStream().writeBytes(chatmessage, messagesize, 0);
		getOutputStream().endFrameVarSize();
	}

	public void loadpm(long name, int world) {
		/*
		 * if(world != 0) { world += 9; } else if(world == 0){ world += 1; }
		 */
		if (world != 0) {
			world += 9;
		}
		getOutputStream().createFrame(50);
		getOutputStream().writeQWord(name);
		getOutputStream().writeByte(world);
	}

	public int[] staffs = {2415, 2416, 2417, 4675, 4710, 6526, 6914};

	public boolean DeleteArrow() {
		if (getEquipmentN()[Equipment.Slot.ARROWS.getId()] == 0) {
			deleteequiment(getEquipment()[Equipment.Slot.ARROWS.getId()], Equipment.Slot.ARROWS.getId());
			return false;
		}
		if (getEquipmentN()[Equipment.Slot.ARROWS.getId()] > 0) {
			getOutputStream().createFrameVarSizeWord(34);
			getOutputStream().writeWord(1688);
			getOutputStream().writeByte(Equipment.Slot.ARROWS.getId());
			getOutputStream().writeWord(getEquipment()[Equipment.Slot.ARROWS.getId()] + 1);
			if (getEquipmentN()[Equipment.Slot.ARROWS.getId()] - 1 > 254) {
				getOutputStream().writeByte(255);
				getOutputStream().writeDWord(getEquipmentN()[Equipment.Slot.ARROWS.getId()] - 1);
			} else {
				getOutputStream().writeByte(getEquipmentN()[Equipment.Slot.ARROWS.getId()] - 1); // amount
			}
			getOutputStream().endFrameVarSizeWord();
			getEquipmentN()[Equipment.Slot.ARROWS.getId()] -= 1;
		}
		getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
		return true;
	}

	public void ReplaceObject(int objectX, int objectY, int NewObjectID, int Face, int ObjectType) {
		send(new SetMap(new Position(objectX, objectY)));
		getOutputStream().createFrame(101);
		getOutputStream().writeByteC((ObjectType << 2) + (Face & 3));
		getOutputStream().writeByte(0);

		if (NewObjectID != -1) {
			getOutputStream().createFrame(151);
			getOutputStream().writeByteS(0);
			getOutputStream().writeWordBigEndian(NewObjectID);
			getOutputStream().writeByteS((ObjectType << 2) + (Face & 3));
		}
	}

	public int GetNPCID(int coordX, int coordY) {
		for (Npc n : Server.npcManager.getNpcs()) {
			if (n.getPosition().getX() == coordX && n.getPosition().getY() == coordY) {
				return n.getId();
			}
		}
		return 1;
	}

	public String GetNpcName(int NpcID) {
		return Server.npcManager.getName(NpcID).replaceAll("_", " ");
	}

	public String GetItemName(int ItemID) {
		return Server.itemManager.getName(ItemID);
	}

	public double GetShopSellValue(int ItemID, int Type, int fromSlot) {
		return Server.itemManager.getShopSellValue(ItemID);
	}

	public double GetShopBuyValue(int ItemID, int Type, int fromSlot) {
		return Server.itemManager.getShopBuyValue(ItemID);
	}

	public int GetUnnotedItem(int ItemID) {
		String NotedName = Server.itemManager.getName(ItemID);
		for (Item item : Server.itemManager.items.values()) {
			if (item.getNoteable() && item.getId() != ItemID && item.getName().equals(NotedName) && !item.getDescription().startsWith("Swap this note at any bank for a")) {
				return item.getId();
			}
		}
		return 0;
	}

	public int GetNotedItem(int ItemID) {
		String NotedName = Server.itemManager.getName(ItemID);
		for (Item item : Server.itemManager.items.values()) {
			if (!item.getNoteable() && item.getId() != ItemID && item.getName().equals(NotedName) && item.getDescription().startsWith("Swap_this_note_at_any_bank")) {
				return item.getId();
			}
		}
		return 0;
	}

	public void WriteEnergy() {
		send(new SendString("100%", 149));
	}

	public void ResetBonus() {
		for (int i = 0; i < playerBonus.length; i++) {
			playerBonus[i] = 0;
		}
	}

	public void GetBonus(boolean update) {
		ResetBonus();
		for (int i = 0; i < 14; i++) {
			if (getEquipment()[i] > -1) {
				int timed = checkObsidianBonus(getEquipment()[i]) ? 2 : 1;
				if(!(duelFight && i == 8))
				for (int k = 0; k < playerBonus.length; k++) {
					int bonus = Server.itemManager.getBonus(getEquipment()[i], k);
					playerBonus[k] += bonus * timed;
				}
			}
		}
		if(update) WriteBonus();
	}

	public void WriteBonus() {
		for (int i = 0; i < playerBonus.length; i++)
			updateBonus(i);
	}

	public int neglectDmg() {
		int bonus = 0;
		if(getEquipment()[Equipment.Slot.SHIELD.getId()] == 11284)
			bonus += ((getLevel(Skill.FIREMAKING) + 1) / 5) * 10;
		return Math.min(1000, playerBonus[11] + bonus);
	}

	public double magicDmg() {
		double bonus = playerBonus[3] / 10D;
		return bonus <= 0.0 ? 1.0 : (1.0 + (bonus / 100D));
	}

	public void updateBonus(int id) {
		String send;
		if(id == 3) {
			double dmg = (magicDmg() - 1.0) * 100D;
			send = "Spell Dmg: " + String.format("%3.1f", dmg) + "%";
		} else if(id == 11)
			send = "Neglect Dmg: " + String.format("%3.1f", neglectDmg() / 10D) + "%";
		else if(id == 10)
			send = (usingBow ? "Ranged str: " : "Melee str: ") + "" + (usingBow && getRangedStr(this) >= 0 ? "+" : playerBonus[id] >= 0 ? "+" : "-") + "" + (usingBow ? getRangedStr(this) : playerBonus[id]);
		else
			send = BonusName[id] + ": " + (playerBonus[id] >= 0 ? "+" + playerBonus[id] : playerBonus[id]);
		send(new SendString(send, 1675 + (id >= 10 ? id + 1 : id)));
	}

	public boolean GoodDistance2(int objectX, int objectY, int playerX, int playerY, int distance) {
		for (int i = 0; i <= distance; i++) {
			for (int j = 0; j <= distance; j++) {
				if (objectX == playerX
						&& ((objectY + j) == playerY || (objectY - j) == playerY)) {
					return true;
				} else if (objectY == playerY
						&& ((objectX + j) == playerX || (objectX - j) == playerX)) {
					return true;
				}
			}
		}
		return false;
	}

	private int[] woodcuttingDelays = {1200, 1800, 3000, 4200, 5400, 7200};
	private int[] woodcuttingLevels = {1, 15, 30, 45, 60, 75};
	private int[] woodcuttingLogs = {1511, 1521, 1519, 1517, 1515, 1513};
	private int[] woodcuttingExp = {80, 152, 272, 400, 700, 1000};
	public int woodcuttingIndex = -1;

	public boolean CheckObjectSkill(int objectID, String name) {
		boolean GoFalse = false;
		/* Do we wish to keep? */
		if (name.contains("oak"))
			objectID = 1281;
		else if (name.contains("willow"))
			objectID = 1308;
		else if (name.contains("maple tree"))
			objectID = 1307;
		else if (name.contains("yew"))
			objectID = 1309;
		else if (name.contains("magic tree"))
			objectID = 1306;
		else if (name.equalsIgnoreCase("tree"))
			objectID = 1276;

		switch (objectID) {

			/*
			 *
			 * WOODCUTTING
			 *
			 */
			case 1276:
			case 1277:
			case 1278:
			case 1279:
			case 1280:
			case 1330:
			case 1332:
			case 2409:
			case 3033:
			case 3034:
			case 3035:
			case 3036:
			case 3879:
			case 3881:
			case 3882:
			case 3883: // Normal Tree
			case 1315:
			case 1316:
			case 1318:
			case 1319: // Evergreen
			case 1282:
			case 1283:
			case 1284:
			case 1285:
			case 1286:
			case 1287:
			case 1289:
			case 1290:
			case 1291:
			case 1365:
			case 1383:
			case 1384:
			case 5902:
			case 5903:
			case 5904: // Dead Tree
				woodcuttingIndex = 0;
				break;

			case 1281:
			case 3037: // Oak Tree
				woodcuttingIndex = 1;
				break;

			case 1308:
			case 5551:
			case 5552: // Willow Tree
				woodcuttingIndex = 2;
				break;

			case 1307:
			case 4674: // Maple Tree
				woodcuttingIndex = 3;
				break;

			case 1309: // Yew Tree
			case 1754:
				woodcuttingIndex = 4;
				break;

			case 1306: // Magic Tree
			case 1762:
				woodcuttingIndex = 5;
				break;

			default:
				GoFalse = true;
				break;
		}
		return !GoFalse;
	}

	public int CheckSmithing(int ItemID) {
		int Type = -1;
		if (!IsItemInBag(2347)) {
			send(new SendMessage("You need a " + GetItemName(2347) + " to hammer bars."));
			return -1;
		}
		switch (ItemID) {
			case 2349: // Bronze Bar
				Type = 1;
				break;

			case 2351: // Iron Bar
				Type = 2;
				break;

			case 2353: // Steel Bar
				Type = 3;
				break;

			case 2359: // Mithril Bar
				Type = 4;
				break;

			case 2361: // Adamantite Bar
				Type = 5;
				break;

			case 2363: // Runite Bar
				Type = 6;
				break;
		}
		if (Type == -1)
			send(new SendMessage("You cannot smith this item."));
		else
			smithing[3] = ItemID;
		return Type;
	}

	public void OpenSmithingFrame(int Type) {
		int Type2 = Type - 1;
		int Length = 22;

		if (Type == 1 || Type == 2) {
			Length += 1;
		} else if (Type == 3) {
			Length += 2;
		}
		// Sending amount of bars + make text green if lvl is highenough
		send(new SendString("", 1132)); // Wire
		send(new SendString("", 1096));
		send(new SendString("", 11459)); // Lantern
		send(new SendString("", 11461));
		send(new SendString("", 1135)); // Studs
		send(new SendString("", 1134));
		String bar, color, color2, name = "";

		if (Type == 1) {
			name = "Bronze ";
		} else if (Type == 2) {
			name = "Iron ";
		} else if (Type == 3) {
			name = "Steel ";
		} else if (Type == 4) {
			name = "Mithril ";
		} else if (Type == 5) {
			name = "Adamant ";
		} else if (Type == 6) {
			name = "Rune ";
		}
		for (int i = 0; i < Length; i++) {
			bar = "bar";
			color = "@red@";
			color2 = "@bla@";
			if (Constants.smithing_frame[Type2][i][3] > 1) {
				bar = bar + "s";
			}
			if (getLevel(Skill.SMITHING) >= Constants.smithing_frame[Type2][i][2]) {
				color2 = "@whi@";
			}
			int Type3 = Type2;

			if (Type2 >= 3) {
				Type3 = (Type2 + 2);
			}
			if (AreXItemsInBag((2349 + (Type3 * 2)), Constants.smithing_frame[Type2][i][3])) {
				color = "@gre@";
			}
			send(new SendString(color + "" + Constants.smithing_frame[Type2][i][3] + "" + bar,
					Constants.smithing_frame[Type2][i][4]));
			String linux_hack = GetItemName(Constants.smithing_frame[Type2][i][0]);
			int index = GetItemName(Constants.smithing_frame[Type2][i][0]).indexOf(name);
			if (index > 0) {
				linux_hack = linux_hack.substring(index + 1);
				send(new SendString(linux_hack, Constants.smithing_frame[Type2][i][5]));
			}
			// send(new SendString(
			// color2 + ""
			// + GetItemName(Constants.smithing_frame[Type2][i][0]).replace(name,
			// ""),
			// Constants.smithing_frame[Type2][i][5]);
		}
		Constants.SmithingItems[0][0] = Constants.smithing_frame[Type2][0][0]; // Dagger
		Constants.SmithingItems[0][1] = Constants.smithing_frame[Type2][0][1];
		Constants.SmithingItems[1][0] = Constants.smithing_frame[Type2][4][0]; // Sword
		Constants.SmithingItems[1][1] = Constants.smithing_frame[Type2][4][1];
		Constants.SmithingItems[2][0] = Constants.smithing_frame[Type2][8][0]; // Scimitar
		Constants.SmithingItems[2][1] = Constants.smithing_frame[Type2][8][1];
		Constants.SmithingItems[3][0] = Constants.smithing_frame[Type2][9][0]; // Long
		// Sword
		Constants.SmithingItems[3][1] = Constants.smithing_frame[Type2][9][1];
		Constants.SmithingItems[4][0] = Constants.smithing_frame[Type2][18][0]; // 2
		// hand
		// sword
		Constants.SmithingItems[4][1] = Constants.smithing_frame[Type2][18][1];
		SetSmithing(1119);
		Constants.SmithingItems[0][0] = Constants.smithing_frame[Type2][1][0]; // Axe
		Constants.SmithingItems[0][1] = Constants.smithing_frame[Type2][1][1];
		Constants.SmithingItems[1][0] = Constants.smithing_frame[Type2][2][0]; // Mace
		Constants.SmithingItems[1][1] = Constants.smithing_frame[Type2][2][1];
		Constants.SmithingItems[2][0] = Constants.smithing_frame[Type2][13][0]; // Warhammer
		Constants.SmithingItems[2][1] = Constants.smithing_frame[Type2][13][1];
		Constants.SmithingItems[3][0] = Constants.smithing_frame[Type2][14][0]; // Battle
		// axe
		Constants.SmithingItems[3][1] = Constants.smithing_frame[Type2][14][1];
		Constants.SmithingItems[4][0] = Constants.smithing_frame[Type2][17][0]; // Claws
		Constants.SmithingItems[4][1] = Constants.smithing_frame[Type2][17][1];
		SetSmithing(1120);
		Constants.SmithingItems[0][0] = Constants.smithing_frame[Type2][15][0]; // Chain
		// body
		Constants.SmithingItems[0][1] = Constants.smithing_frame[Type2][15][1];
		Constants.SmithingItems[1][0] = Constants.smithing_frame[Type2][20][0]; // Plate
		// legs
		Constants.SmithingItems[1][1] = Constants.smithing_frame[Type2][20][1];
		Constants.SmithingItems[2][0] = Constants.smithing_frame[Type2][19][0]; // Plate
		// skirt
		Constants.SmithingItems[2][1] = Constants.smithing_frame[Type2][19][1];
		Constants.SmithingItems[3][0] = Constants.smithing_frame[Type2][21][0]; // Plate
		// body
		Constants.SmithingItems[3][1] = Constants.smithing_frame[Type2][21][1];
		Constants.SmithingItems[4][0] = -1; // Lantern
		Constants.SmithingItems[4][1] = 0;
		if (Type == 2 || Type == 3) {
			color2 = "@bla@";
			if (getLevel(Skill.SMITHING) >= Constants.smithing_frame[Type2][22][2]) {
				color2 = "@whi@";
			}
			Constants.SmithingItems[4][0] = Constants.smithing_frame[Type2][22][0]; // Lantern
			Constants.SmithingItems[4][1] = Constants.smithing_frame[Type2][22][1];
			send(new SendString(color2 + "" + GetItemName(Constants.smithing_frame[Type2][22][0]).replace(name, ""), 11461));
		}
		SetSmithing(1121);
		Constants.SmithingItems[0][0] = Constants.smithing_frame[Type2][3][0]; // Medium
		Constants.SmithingItems[0][1] = Constants.smithing_frame[Type2][3][1];
		Constants.SmithingItems[1][0] = Constants.smithing_frame[Type2][10][0]; // Full
		// Helm
		Constants.SmithingItems[1][1] = Constants.smithing_frame[Type2][10][1];
		Constants.SmithingItems[2][0] = Constants.smithing_frame[Type2][12][0]; // Square
		Constants.SmithingItems[2][1] = Constants.smithing_frame[Type2][12][1];
		Constants.SmithingItems[3][0] = Constants.smithing_frame[Type2][16][0]; // Kite
		Constants.SmithingItems[3][1] = Constants.smithing_frame[Type2][16][1];
		Constants.SmithingItems[4][0] = Constants.smithing_frame[Type2][6][0]; // Nails
		Constants.SmithingItems[4][1] = Constants.smithing_frame[Type2][6][1];
		SetSmithing(1122);
		Constants.SmithingItems[0][0] = Constants.smithing_frame[Type2][5][0]; // Dart
		// Tips
		Constants.SmithingItems[0][1] = Constants.smithing_frame[Type2][5][1];
		Constants.SmithingItems[1][0] = Constants.smithing_frame[Type2][7][0]; // Arrow
		// Heads
		Constants.SmithingItems[1][1] = Constants.smithing_frame[Type2][7][1];
		Constants.SmithingItems[2][0] = Constants.smithing_frame[Type2][11][0]; // Knives
		Constants.SmithingItems[2][1] = Constants.smithing_frame[Type2][11][1];
		Constants.SmithingItems[3][0] = -1; // Wire
		Constants.SmithingItems[3][1] = 0;
		if (Type == 1) {
			color2 = "@bla@";
			if (getLevel(Skill.SMITHING) >= Constants.smithing_frame[Type2][22][2]) {
				color2 = "@whi@";
			}
			Constants.SmithingItems[3][0] = Constants.smithing_frame[Type2][22][0]; // Wire
			Constants.SmithingItems[3][1] = Constants.smithing_frame[Type2][22][1];
			send(new SendString(color2 + "" + GetItemName(Constants.smithing_frame[Type2][22][0]).replace(name, ""), 1096));
		}
		for (int i = 0; i < 22; i++) {
			if (getLevel(Skill.SMITHING) >= Constants.smithing_frame[Type2][i][2]) {
				color2 = "@whi@";
			} else
				color2 = "@bla@";
			send(new SendString(color2 + "" + GetItemName(Constants.smithing_frame[Type2][i][0]).replace(name, ""), Constants.smithing_frame[Type2][i][5]));
		}
		Constants.SmithingItems[4][0] = -1; // Studs
		Constants.SmithingItems[4][1] = 0;
		if (Type == 3) {
			color2 = "@bla@";
			if (getLevel(Skill.SMITHING) >= Constants.smithing_frame[Type2][23][2]) {
				color2 = "@whi@";
			}
			Constants.SmithingItems[4][0] = Constants.smithing_frame[Type2][23][0]; // Studs
			Constants.SmithingItems[4][1] = Constants.smithing_frame[Type2][23][1];
			send(new SendString(color2 + "" + GetItemName(Constants.smithing_frame[Type2][23][0]).replace(name, ""), 1134));
		}
		SetSmithing(1123);
		showInterface(994);
		smithing[2] = Type;
	}

	public boolean smithing() {
		if (IsItemInBag(2347)) {
			if (!smithCheck(smithing[4])) {
				IsAnvil = true;
				resetAction();
				return false;
			}
			int bars = 0;
			int Length = 22;
			int barid;
			int xp = 0;
			int ItemN = 1;

			if (smithing[2] >= 4) {
				barid = (2349 + ((smithing[2] + 1) * 2));
			} else {
				barid = (2349 + ((smithing[2] - 1) * 2));
			}
			if (smithing[2] == 1 || smithing[2] == 2) {
				Length += 1;
			} else if (smithing[2] == 3) {
				Length += 2;
			}
			int[] possibleBars = {2349, 2351, 2353, 2359, 2361, 2363};
			int[] bar_xp = {13, 25, 38, 50, 63, 75};
			for (int i = 0; i < Constants.smithing_frame.length; i++) {
				for (int i1 = 0; i1 < Constants.smithing_frame[i].length; i1++) {
					for (int i2 = 0; i2 < Constants.smithing_frame[i][i1].length; i2++) {
						if (Constants.smithing_frame[i][i1][0] == smithing[4]) {
							if (!AreXItemsInBag(possibleBars[i], Constants.smithing_frame[i][i1][3])) {
								send(new SendMessage("You are missing bars needed to smith this!"));
								IsAnvil = true;
								resetAction();
								return false;
							}
							xp = bar_xp[i];
						}
					}
				}
			}
			for (int i = 0; i < Length; i++) {
				if (Constants.smithing_frame[(smithing[2] - 1)][i][0] == smithing[4]) {
					bars = Constants.smithing_frame[(smithing[2] - 1)][i][3];
					if (smithing[1] == 0) {
						smithing[1] = Constants.smithing_frame[(smithing[2] - 1)][i][2];
					}
					ItemN = Constants.smithing_frame[(smithing[2] - 1)][i][1];
				}
			}
			if (getLevel(Skill.SMITHING) >= smithing[1]) {
				if (AreXItemsInBag(barid, bars)) {
					int[] barLevelRequired = {1, 15, 30, 55, 70, 85};
					if (System.currentTimeMillis() - lastAction >= 600 && !IsAnvil) {
						lastAction = System.currentTimeMillis();
						setFocus(skillX, skillY);
						send(new SendMessage("You start hammering the bar..."));
						requestAnim(0x382, 0);
						IsAnvil = true;
						int diff = getLevel(Skill.SMITHING) - barLevelRequired[CheckSmithing(smithing[3]) - 1];
						smithing[0] = 5 - (diff >= 14 ? 2 : diff >= 7 ? 1 : 0);
					}
					if (System.currentTimeMillis() - lastAction >= (smithing[0] * 600) && IsAnvil) {
						lastAction = System.currentTimeMillis();
						for (int i = 0; i < bars; i++) {
							deleteItem(barid, GetItemSlot(barid), playerItemsN[GetItemSlot(barid)]);
						}
						int experience = xp * bars * 30;
						giveExperience(experience, Skill.SMITHING);
						addItem(smithing[4], ItemN);
						send(new SendMessage("You smith a " + GetItemName(smithing[4]) + ""));
						requestAnim(0x382, 0);
						triggerRandom(experience);
					}
				} else {
					send(new SendMessage(
							"You need " + bars + " " + GetItemName(barid) + " to smith a " + GetItemName(smithing[4])));
					rerequestAnim();
					resetAction();
				}
			} else {
				send(new SendMessage("You need " + smithing[1] + " Smithing to smith a " + GetItemName(smithing[4])));
				IsAnvil = true;
				resetAction();
				return false;
			}
		} else {
			send(new SendMessage("You need a " + GetItemName(2347) + " to hammer bars."));
			IsAnvil = true;
			resetAction();
			return false;
		}
		return true;
	}

	public void resetSM() {
		if(IsAnvil) {
			smithing[0] = 0;
			smithing[1] = 0;
			smithing[2] = 0;
			smithing[3] = -1;
			smithing[4] = -1;
			smithing[5] = 0;
			IsAnvil = false;
			skillX = -1;
			setSkillY(-1);
			IsAnvil = false;
			rerequestAnim();
		}
	}

	/* WOODCUTTING */

	public boolean woodcutting() {
		if (randomed || fletchings || isFiremaking || shafting) {
			return false;
		}
		if (woodcuttingIndex < 0) {
			resetAction();
			return false;
		}

		int WCAxe = findAxe();
		if (WCAxe < 0) {
			send(new SendMessage("You need a axe in which you got the required woodcutting level for."));
			resetAction();
			return false;
		}
		if (woodcuttingLevels[woodcuttingIndex] > getLevel(Skill.WOODCUTTING)) {
			send(new SendMessage(
					"You need a woodcutting level of " + woodcuttingLevels[woodcuttingIndex] + " to cut this tree."));
			resetAction();
			return false;
		}
		if (freeSlots() < 1) {
			send(new SendMessage("You got full inventory!"));
			resetAction();
			return false;
		}
		if (System.currentTimeMillis() - lastAction >= 600 && !IsCutting) {
			lastAction = System.currentTimeMillis();
			send(new SendMessage("You swing your axe at the tree..."));
			requestAnim(getWoodcuttingEmote(Utils.axes[WCAxe]), 0);
			IsCutting = true;
		}
		if (IsCutting)
			requestAnim(getWoodcuttingEmote(Utils.axes[WCAxe]), 0);
		if (System.currentTimeMillis() - lastAction >= getWoodcuttingSpeed() && IsCutting) {
			lastAction = System.currentTimeMillis();
			giveExperience(woodcuttingExp[woodcuttingIndex], Skill.WOODCUTTING);
			send(new SendMessage("You cut some " + GetItemName(woodcuttingLogs[woodcuttingIndex]).toLowerCase() + ""));
			addItem(woodcuttingLogs[woodcuttingIndex], 1);
			triggerRandom(woodcuttingExp[woodcuttingIndex]);
			if (Misc.chance(30) == 1) {
				send(new SendMessage("You take a rest"));
				resetAction(true);
				return false;
			}
		}
		return true;
	}

	public int getWoodcuttingEmote(int item) {
		switch (item) {
			case 1351: //bronze
				return 879;
			case 1349: //iron
				return 877;
			case 1353: //steel
				return 875;
			case 1355: // mithril
				return 871;
			case 1357: // addy
				return 869;
			case 1359: // rune
				return 867;
			case 6739: // dragon
			case 20011: //3rd age
				return 2846;
		}
		return -1;
	}

	public int getMiningEmote(int item) {
		switch (item) {
			case 1275: //bronze
				return 624;
			case 1271: //iron
				return 628;
			case 1273: //steel
				return 629;
			case 1269: // mithril
				return 627;
			case 1267: // addy
				return 626;
			case 1265: // rune
				return 625;
			case 11920: //dragon
			case 20014: //3rd age
				return 7139;
		}
		return -1;
	}

	public long getMiningSpeed() {
		double pickBonus = Utils.pickBonus[minePick];
		double level = (double) getLevel(Skill.MINING) / 600;
		double random = (50 + Misc.random(100)) / 100D;
		double bonus = 1 + pickBonus * random + level;
		double time = Utils.mineTimes[mineIndex] / bonus;
		return (long) time;
	}

	public long getWoodcuttingSpeed() {
		double axeBonus = Utils.axeBonus[findAxe()];
		double level = (double) getLevel(Skill.WOODCUTTING) / 600;
		double random = (75 + Misc.random(50)) / 100D;
		double bonus = 1 + axeBonus * random + level;
		double time = woodcuttingDelays[woodcuttingIndex] / bonus;
		return (long) time;
	}

	public void resetWC() {
			woodcutting[0] = 0;
			woodcutting[1] = 0;
			woodcutting[2] = 0;
			woodcutting[4] = 0;
			skillX = -1;
			setSkillY(-1);
			woodcuttingIndex = -1;
			IsCutting = false;
			rerequestAnim();
	}

	public boolean fromTrade(int itemID, int fromSlot, int amount) {
		if (System.currentTimeMillis() - lastButton >= 200) {
			lastButton = System.currentTimeMillis();
		} else {
			return false;
		}
		try {
			Client other = getClient(trade_reqId);
			if (!inTrade || !validClient(trade_reqId) || !canOffer) {
				declineTrade();
				return false;
			}
			if (!checkGameitemAmount(fromSlot, amount, offeredItems) || offeredItems.get(fromSlot).getId() != itemID) {
				return false;
			}
			int count = 0;
			if (!Server.itemManager.isStackable(itemID)) {
				for (GameItem item : offeredItems) {
					if (item.getId() == itemID) {
						count++;
					}
				}
			} else
				count = offeredItems.get(fromSlot).getAmount();
			amount = amount > count ? count : amount;
			boolean found = false;
			for (GameItem item : offeredItems) {
				if (item.getId() == itemID) {
					if (item.isStackable()) {
						if (amount < item.getAmount())
							offeredItems.set(fromSlot, new GameItem(item.getId(), item.getAmount() - amount));
						else
							offeredItems.remove(item);
						found = true;
					} else {
            /*if (item.getAmount() > amount) {
              item.removeAmount(amount);
              found = true;
            } else {
              amount = item.getAmount();
              found = true;
              offeredItems.remove(item);
            }*/
						if (amount == 1) {
							offeredItems.remove(item);
							found = true;
						} else {
							offeredItems.remove(item);
							addItem(itemID, 1);
							amount--;
						}
					}
					if (found) { //If found add item to inventory!
						addItem(itemID, amount);
						break;
					}
				}
			}
			tradeConfirmed = false;
			other.tradeConfirmed = false;
			resetItems(3322);
			resetTItems(3415);
			other.resetOTItems(3416);
			send(new SendString("", 3431));
			other.send(new SendString("", 3431));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean tradeItem(int itemID, int fromSlot, int amount) {
		if (System.currentTimeMillis() - lastButton >= 200) {
			lastButton = System.currentTimeMillis();
		} else {
			return false;
		}
		if (!Server.itemManager.isStackable(itemID))
			amount = amount > getInvAmt(itemID) ? getInvAmt(itemID) : amount;
		else
			amount = amount > playerItemsN[fromSlot] ? playerItemsN[fromSlot] : amount;
		Client other = getClient(trade_reqId);
		if (!inTrade || !validClient(trade_reqId) || !canOffer) {
			System.out.println("declining in tradeItem()");
			declineTrade();
			return false;
		}
		if (!playerHasItem(itemID, amount) || playerItems[fromSlot] != (itemID + 1)) {
			return false;
		}
		if (!Server.itemManager.isTradable(itemID) && playerRights < 2 && other.playerRights < 2) {
			send(new SendMessage("You can't trade this item"));
			return false;
		}
		if (Server.itemManager.isStackable(itemID)) {
			boolean inTrade = false;
			for (GameItem item : offeredItems) {
				if (item.getId() == itemID) {
					inTrade = true;
					item.addAmount(amount);
					deleteItem(itemID, fromSlot, amount);
					break;
				}
			}
			if (!inTrade) {
				offeredItems.add(new GameItem(itemID, amount));
				deleteItem(itemID, fromSlot, amount);
			}
		} else {
			for (int a = 1; a <= amount; a++) {
				if (a == 1) {
					offeredItems.add(new GameItem(itemID, 1));
					deleteItem(itemID, fromSlot, amount);
				} else {
					int slot = findItem(itemID, playerItems, playerItemsN);
					if (slot >= 0 && slot < 28)
						//tradeItem(itemID, slot, 1);
						offeredItems.add(new GameItem(itemID, 1));
					deleteItem(itemID, slot, amount);
				}
			}
		}
		resetItems(3322);
		resetTItems(3415);
		other.resetOTItems(3416);
		send(new SendString("", 3431));
		other.send(new SendString("", 3431));
		return true;
	}

	/* Shops */
	public boolean sellItem(int itemID, int fromSlot, int amount) {
		/* Item Values */
		int original = itemID;
		itemID = GetUnnotedItem(original) > 0 ? GetUnnotedItem(original) : itemID;
		int price = (int) Math.floor(GetShopBuyValue(itemID, 0, fromSlot));
		/* Functions */
		if (!Server.shopping || tradeLocked) {
			send(new SendMessage(tradeLocked ? "You are trade locked!" : "Currently selling stuff to the store has been disabled!"));
			return false;
		}
		if (price <= 0 || !Server.itemManager.isTradable(itemID) || ShopHandler.ShopBModifier[MyShopID] > 2) {
			send(new SendMessage("You cannot sell " + GetItemName(itemID).toLowerCase() + " in this store."));
			return false;
		}
		if (ShopHandler.ShopBModifier[MyShopID] == 2 && !ShopHandler.findDefaultItem(MyShopID, itemID)) {
			send(new SendMessage("Can't sell that item to the store!"));
			return false;
		}
		int slot = -1;
		for (int i = 0; i < ShopHandler.MaxShopItems; i++) {
			if (ShopHandler.ShopItems[MyShopID][i] <= 0 && slot == -1)
				slot = i;
			else if (itemID == ShopHandler.ShopItems[MyShopID][i] - 1) {
				slot = i;
				i = ShopHandler.MaxShopItems; //Just to stop the loop!
			}
		}
		if (slot == -1) { //If we do not have a slot means the store is full!
			send(new SendMessage("Can't sell more items to the store!"));
			return false;
		}
		/* Amount checks */
		boolean stack = Server.itemManager.isStackable(original);
		amount = amount > getInvAmt(original) ? getInvAmt(original) : amount;
		amount = Integer.MAX_VALUE - ShopHandler.ShopItemsN[MyShopID][slot] < amount ? Integer.MAX_VALUE - ShopHandler.ShopItemsN[MyShopID][slot] : amount;
		amount = Integer.MAX_VALUE - getInvAmt(995) < amount * price ? (Integer.MAX_VALUE - getInvAmt(995)) / price : amount;

		if (amount > 0) { // Code to check if there is any amount to sell!
			if (!stack) {
				for (int i = 0; i < amount; i++) {
					deleteItem(original, 1);
				}
			} else {
				deleteItem(original, amount);
			}
			ShopHandler.ShopItems[MyShopID][slot] = itemID + 1;
			ShopHandler.ShopItemsN[MyShopID][slot] += amount;
			addItem(995, amount * price);
		} else
			send(new SendMessage("Could not sell anything!"));
		/* Store update! */
		resetItems(3823);
		resetShop(MyShopID);
		UpdatePlayerShop();
		return true;
	}

	public int eventShopValues(int slot) {
		switch (slot) {
			case 0:
				return 8000;
			case 1:
			case 2:
				return 12000;
			case 3:
			case 4:
				return 4000;
			case 5:
				return 25000;
			case 6:
				return 15000;
		}
		return 0;
	}

	public boolean buyItem(int itemID, int fromSlot, int amount) {
		if (amount > 0 && itemID == (ShopHandler.ShopItems[MyShopID][fromSlot] - 1)) {
			boolean stack = Server.itemManager.isStackable(itemID);
			amount = ShopHandler.ShopItemsN[MyShopID][fromSlot] < amount ? ShopHandler.ShopItemsN[MyShopID][fromSlot] : amount;
			if (!canUse(itemID)) {
				send(new SendMessage("You must be a premium member to buy this item"));
				send(new SendMessage("Visit Dodian.net to subscribe"));
				return false;
			}
			if (!stack && freeSlots() < 1) {
				send(new SendMessage("Not enough space in your inventory."));
				return false;
			}
			int currency = MyShopID == 55 ? 11997 : 995;
			int TotPrice2 = MyShopID == 55 ? eventShopValues(fromSlot) : (int) Math.floor(GetShopSellValue(itemID, 0, fromSlot));
			TotPrice2 = MyShopID >= 9 && MyShopID <= 11 ? (int) (TotPrice2 * 1.5) : TotPrice2;
			int coins = getInvAmt(currency);
			amount = amount * TotPrice2 > coins ? coins / TotPrice2 : amount;
			if (amount == 0) {
				send(new SendMessage("You don't have enough " + GetItemName(currency).toLowerCase() + ""));
				return false;
			}
			if (!stack) {
				for (int i = amount; i > 0; i--) {
					if (freeSlots() == 0) {
						send(new SendMessage("Not enough space in your inventory."));
						return false;
					}
					if (addItem(itemID, 1)) {
						deleteItem(currency, TotPrice2);
						ShopHandler.ShopItemsN[MyShopID][fromSlot] -= 1;
						if ((fromSlot + 1) > ShopHandler.ShopItemsStandard[MyShopID] && ShopHandler.ShopItemsN[MyShopID][fromSlot] <= 0) {
							ShopHandler.resetAnItem(MyShopID, fromSlot);
							break;
						}
					} else {
						send(new SendMessage("Not enough space in your inventory."));
						return false;
					}
				}
			} else {
				if (addItem(itemID, amount)) {
					deleteItem(currency, TotPrice2 * amount);
					ShopHandler.ShopItemsN[MyShopID][fromSlot] -= amount;
					if ((fromSlot + 1) > ShopHandler.ShopItemsStandard[MyShopID] && ShopHandler.ShopItemsN[MyShopID][fromSlot] <= 0) {
						ShopHandler.resetAnItem(MyShopID, fromSlot);
					}
				} else
					return false;
			}
			resetItems(3823);
			resetShop(MyShopID);
			UpdatePlayerShop();
			return true;
		}
		return false;
	}

	public void UpdatePlayerShop() {
		for (int i = 1; i < Constants.maxPlayers; i++) {
			if (PlayerHandler.players[i] != null) {
				if (PlayerHandler.players[i].IsShopping && PlayerHandler.players[i].MyShopID == MyShopID
						&& i != getSlot()) {
					PlayerHandler.players[i].UpdateShop = true;
				}
			}
		}
	}

	/* NPC Talking */
	public void UpdateNPCChat() {
		switch (NpcDialogue) {
			case 1:

				/*
				 * sendFrame200(4901, 554); send(new SendString(GetNpcName(NpcTalkTo),
				 * 4902); send(new SendString("Good day, how can I help you?", 4904);
				 * send(new SendNpcDialogueHead(NpcTalkTo, 4901); sendFrame164(4900);
				 */
				sendFrame200(4883, 591);
				send(new SendString(GetNpcName(NpcTalkTo), 4884));
				send(new SendString("Good day, how can I help you?", 4885));
				send(new SendString("Click here to continue", 4886));
				send(new NpcDialogueHead(NpcTalkTo, 4883));
				sendFrame164(4882);
				NpcDialogueSend = true;
				break;

			case 2:
				send(new Frame171(1, 2465));
				send(new Frame171(0, 2468));
				send(new SendString("What would you like to say?", 2460));
				send(new SendString("I'd like to access my bank account, please.", 2461));
				send(new SendString("I'd like to check my PIN settings.", 2462));
				sendFrame164(2459);
				NpcDialogueSend = true;
				break;

			case 3:
				sendFrame200(4883, 591);
				send(new SendString(GetNpcName(NpcTalkTo), 4884));
				send(new SendString("Do you want to buy some runes?", 4885));
				send(new SendString("Click here to continue", 4886));
				send(new NpcDialogueHead(NpcTalkTo, 4883));
				sendFrame164(4882);
				NpcDialogueSend = true;
				nextDiag = 4;
				break;

			case 4:
				send(new Frame171(1, 2465));
				send(new Frame171(0, 2468));
				send(new SendString("Select an Option", 2460));
				send(new SendString("Yes please!", 2461));
				send(new SendString("Oh it's a rune shop. No thank you, then.", 2462));
				sendFrame164(2459);
				NpcDialogueSend = true;
				break;

			case 5:
				sendFrame200(969, 974);
				send(new SendString(getPlayerName(), 970));
				send(new SendString("Oh it's a rune shop. No thank you, then.", 971));
				send(new SendString("Click here to continue", 972));
				sendFrame185(969);
				sendFrame164(968);
				NpcDialogueSend = true;
				break;

			case 6:
				sendFrame200(4888, 592);
				send(new SendString(GetNpcName(NpcTalkTo), 4889));
				send(new SendString("Well, if you find somone who does want runes, please", 4890));
				send(new SendString("send them my way.", 4891));
				send(new SendString("Click here to continue", 4892));
				send(new NpcDialogueHead(NpcTalkTo, 4888));
				sendFrame164(4887);
				NpcDialogueSend = true;
				break;

			case 7: /* NEED TO CHANGE FOR GUARD */
				sendFrame200(4883, 591);
				send(new SendString(GetNpcName(NpcTalkTo), 4884));
				send(new SendString("Well, if you find somone who does want runes, please send them my way.", 4885));
				send(new SendString("Click here to continue", 4886));
				send(new NpcDialogueHead(NpcTalkTo, 4883));
				sendFrame164(4882);
				NpcDialogueSend = true;
				break;
			case 8:
				sendFrame200(4883, 591);
				send(new SendString(GetNpcName(NpcTalkTo), 4884));
				send(new SendString("Pins have not been implemented yet", 4885));
				send(new SendString("Click here to continue", 4886));
				send(new NpcDialogueHead(NpcTalkTo, 4883));
				sendFrame164(4882);
				NpcDialogueSend = true;
				break;
			case 9:
				sendFrame200(4883, 1597);
				send(new SendString(GetNpcName(NpcTalkTo), 4884));
				send(new SendString("Select an Option", 2460));
				send(new SendString("Can you teleport me to the mage arena?", 2461));
				send(new SendString("Whats at the mage arena?", 2462));
				sendFrame164(2459);
				NpcDialogueSend = true;
				break;
			case 10:
				sendFrame200(4883, 804);
				send(new SendString(GetNpcName(804), 4884));
				send(new SendString(dMsg, 4885));
				send(new NpcDialogueHead(804, 4883));
				sendFrame164(4882);
				NpcDialogueSend = true;
				break;
			case 1000:
				sendFrame200(4883, npcFace);
				send(new SendString(GetNpcName(NpcTalkTo).replace("_", " "), 4884));
				send(new SendString("Hi there, what would you like to do?", 4885));
				send(new SendString("Click here to continue", 4886));
				send(new NpcDialogueHead(NpcTalkTo, 4883));
				sendFrame164(4882);
				NpcDialogueSend = true;
				nextDiag = 1001;
				break;
			case 1001:
				send(new Frame171(1, 2465));
				send(new Frame171(0, 2468));
				send(new SendString("What would you like to do?", 2460));
				send(new SendString("Gamble", 2461));
				send(new SendString("Nothing", 2462));
				sendFrame164(2459);
				NpcDialogueSend = true;
				break;
			case 1002:
				sendFrame200(4883, npcFace);
				send(new SendString(GetNpcName(NpcTalkTo).replace("_", " "), 4884));
				send(new SendString("Cant talk right now!", 4885));
				send(new SendString("Click here to continue", 4886));
				send(new NpcDialogueHead(NpcTalkTo, 4883));
				sendFrame164(4882);
				NpcDialogueSend = true;
				break;
			case 11:
				sendFrame200(4883, npcFace);
				send(new SendString(GetNpcName(NpcTalkTo), 4884));
				send(new SendString("Hi there noob, what do you want?", 4885));
				send(new SendString("Click here to continue", 4886));
				send(new NpcDialogueHead(NpcTalkTo, 4883));
				sendFrame164(4882);
				NpcDialogueSend = true;
				nextDiag = 12;
				break;
			case 12:
      /*send(new Frame171(1, 2465));
      send(new Frame171(0, 2468));
      send(new SendString("What would you like to say?", 2460));
      send(new SendString("I'd like a task please", 2461));
      send(new SendString("Can you teleport me to west ardougne?", 2462));
      sendFrame164(2459);*/
				if (NpcTalkTo == 405 && (determineCombatLevel() < 50 || getLevel(Skill.SLAYER) < 50)) {
					sendFrame200(4888, npcFace);
					send(new SendString(GetNpcName(NpcTalkTo), 4889));
					send(new SendString("You need 50 combat and slayer", 4890));
					send(new SendString("to be assign tasks from me!", 4891));
					send(new SendString("Click here to continue", 4892));
					send(new NpcDialogueHead(NpcTalkTo, 4888));
					sendFrame164(4887);
				} else if (NpcTalkTo == 403 && (!checkItem(989) || getLevel(Skill.SLAYER) < 50)) {
					sendFrame200(4888, npcFace);
					send(new SendString(GetNpcName(NpcTalkTo), 4889));
					send(new SendString("You need a crystal key and 50 slayer", 4890));
					send(new SendString("to be assign tasks from me!", 4891));
					send(new SendString("Click here to continue", 4892));
					send(new NpcDialogueHead(NpcTalkTo, 4888));
					sendFrame164(4887);
				} else {
					String taskName = getSlayerData().get(0) == -1 || getSlayerData().get(3) <= 0 ? "" : "" + SlayerTask.slayerTasks.getTask(getSlayerData().get(1)).getTextRepresentation();
					String[] slayerMaster = new String[]{
							"What would you like to say?", "I'd like a task please",
							!taskName.equals("") ? "Cancel " + taskName.toLowerCase() + " task" : "No task to skip", "I'd like to upgrade my slayer mask", "Can you teleport me to west ardougne?"};
					showPlayerOption(slayerMaster);
				}
				NpcDialogueSend = true;
				break;
			case 13:
				if (NpcTalkTo == 405 && (determineCombatLevel() < 50 || getLevel(Skill.SLAYER) < 50)) {
					sendFrame200(4888, npcFace);
					send(new SendString(GetNpcName(NpcTalkTo), 4889));
					send(new SendString("You need 50 combat and slayer", 4890));
					send(new SendString("to be assign tasks from me!", 4891));
					send(new SendString("Click here to continue", 4892));
					send(new NpcDialogueHead(NpcTalkTo, 4888));
					sendFrame164(4887);
				} else if (NpcTalkTo == 403 && (!checkItem(989) || getLevel(Skill.SLAYER) < 50)) {
					sendFrame200(4888, npcFace);
					send(new SendString(GetNpcName(NpcTalkTo), 4889));
					send(new SendString("You need a crystal key and 50 slayer", 4890));
					send(new SendString("to be assign tasks from me!", 4891));
					send(new SendString("Click here to continue", 4892));
					send(new NpcDialogueHead(NpcTalkTo, 4888));
					sendFrame164(4887);
				} else {
					if (NpcTalkTo == 402) {
						ArrayList<SlayerTask.slayerTasks> tasks = SlayerTask.mazchnaTasks(this);
						if (tasks.isEmpty()) {
							send(new SendMessage("You cant get any task!"));
							break;
						}
						if (getSlayerData().get(3) > 0) {
							sendFrame200(4883, npcFace);
							send(new SendString(GetNpcName(NpcTalkTo), 4884));
							send(new SendString("You already have a task!", 4885));
							send(new SendString("Click here to continue", 4886));
							send(new NpcDialogueHead(NpcTalkTo, 4883));
							sendFrame164(4882);
							NpcDialogueSend = true;
							break;
						}
						SlayerTask.slayerTasks task = tasks.get(Misc.random(tasks.size() - 1));
						if (task != null) {
							int amt = task.getAssignedAmountRange().getValue();
							sendFrame200(4901, npcFace);
							send(new SendString(GetNpcName(NpcTalkTo), 4902));
							send(new SendString("You must go out and kill " + amt + " " + task.getTextRepresentation() + "", 4903));
							send(new SendString("If you want a new task that's too bad", 4904));
							send(new SendString("Visit Dodian.net for a slayer guide", 4905));
							send(new SendString("", 4906));
							send(new SendString("Click here to continue", 4907));
							send(new NpcDialogueHead(NpcTalkTo, 4901));
							sendFrame164(4900);
							getSlayerData().set(0, NpcTalkTo);
							getSlayerData().set(1, task.ordinal());
							getSlayerData().set(2, amt);
							getSlayerData().set(3, amt);
						}
					} else if (NpcTalkTo == 403) {
						ArrayList<SlayerTask.slayerTasks> tasks = SlayerTask.vannakaTasks(this);
						if (tasks.isEmpty()) {
							send(new SendMessage("You cant get any task!"));
							break;
						}
						if (getSlayerData().get(3) > 0) {
							sendFrame200(4883, npcFace);
							send(new SendString(GetNpcName(NpcTalkTo), 4884));
							send(new SendString("You already have a task!", 4885));
							send(new SendString("Click here to continue", 4886));
							send(new NpcDialogueHead(NpcTalkTo, 4883));
							sendFrame164(4882);
							NpcDialogueSend = true;
							break;
						}
						SlayerTask.slayerTasks task = tasks.get(Misc.random(tasks.size() - 1));
						if (task != null) {
							int amt = task.getAssignedAmountRange().getValue();
							sendFrame200(4901, npcFace);
							send(new SendString(GetNpcName(NpcTalkTo), 4902));
							send(new SendString("You must go out and kill " + amt + " " + task.getTextRepresentation() + "", 4903));
							send(new SendString("If you want a new task that's too bad", 4904));
							send(new SendString("Visit Dodian.net for a slayer guide", 4905));
							send(new SendString("", 4906));
							send(new SendString("Click here to continue", 4907));
							send(new NpcDialogueHead(NpcTalkTo, 4901));
							sendFrame164(4900);
							getSlayerData().set(0, NpcTalkTo);
							getSlayerData().set(1, task.ordinal());
							getSlayerData().set(2, amt);
							getSlayerData().set(3, amt);
						}
					} else if (NpcTalkTo == 405) {
						ArrayList<SlayerTask.slayerTasks> tasks = SlayerTask.duradelTasks(this);
						if (tasks.isEmpty()) {
							send(new SendMessage("You cant get any task!"));
							break;
						}
						if (getSlayerData().get(3) > 0) {
							sendFrame200(4883, npcFace);
							send(new SendString(GetNpcName(NpcTalkTo), 4884));
							send(new SendString("You already have a task!", 4885));
							send(new SendString("Click here to continue", 4886));
							send(new NpcDialogueHead(NpcTalkTo, 4883));
							sendFrame164(4882);
							NpcDialogueSend = true;
							break;
						}
						SlayerTask.slayerTasks task = tasks.get(Misc.random(tasks.size() - 1));
						if (task != null) {
							int amt = task.getAssignedAmountRange().getValue();
							sendFrame200(4901, npcFace);
							send(new SendString(GetNpcName(NpcTalkTo), 4902));
							send(new SendString("You must go out and kill " + amt + " " + task.getTextRepresentation() + "", 4903));
							send(new SendString("If you want a new task that's too bad", 4904));
							send(new SendString("Visit Dodian.net for a slayer guide", 4905));
							send(new SendString("", 4906));
							send(new SendString("Click here to continue", 4907));
							send(new NpcDialogueHead(NpcTalkTo, 4901));
							sendFrame164(4900);
							getSlayerData().set(0, NpcTalkTo);
							getSlayerData().set(1, task.ordinal());
							getSlayerData().set(2, amt);
							getSlayerData().set(3, amt);
						}
					} else
						send(new SendMessage("This npc do not wish to talk to you!"));
				}
				NpcDialogueSend = true;
				break;
			case 31: //Skip task
				String taskName = getSlayerData().get(0) == -1 || getSlayerData().get(3) <= 0 ? "" : "" + SlayerTask.slayerTasks.getTask(getSlayerData().get(1)).getTextRepresentation();
				int cost = getSlayerData().get(0) == 403 ? 250000 :
						getSlayerData().get(0) == 405 ? 500000 :
								100000; //Default 100k!

				sendFrame200(4888, npcFace);
				send(new SendString(GetNpcName(NpcTalkTo), 4889));
				send(new SendString(taskName.equals("") ? "You do not have a task currently." : "I can cancel your " + taskName.toLowerCase() + " task", 4890));
				send(new SendString(taskName.equals("") ? "Talk to me to get one." : "task for " + cost + " coins.", 4891));
				send(new SendString("Click here to continue", 4892));
				send(new NpcDialogueHead(NpcTalkTo, 4888));
				sendFrame164(4887);
				if (!taskName.equals(""))
					nextDiag = 32;
				NpcDialogueSend = true;
				break;
			case 32:
				taskName = getSlayerData().get(0) == -1 || getSlayerData().get(3) <= 0 ? "" : "" + SlayerTask.slayerTasks.getTask(getSlayerData().get(1)).getTextRepresentation();
				if (!taskName.equals(""))
					showPlayerOption(new String[]{"Do you wish to cancel your " + taskName.toLowerCase() + " task?", "Yes", "No"});
				NpcDialogueSend = true;
				break;
			case 33:
				taskName = getSlayerData().get(0) == -1 || getSlayerData().get(3) <= 0 ? "" : "" + SlayerTask.slayerTasks.getTask(getSlayerData().get(1)).getTextRepresentation();
				cost = getSlayerData().get(0) == 403 ? 250000 :
						getSlayerData().get(0) == 405 ? 500000 :
								100000; //Default 100k!
				int coinAmount = this.getInvAmt(995);
				if (coinAmount >= cost) {
					deleteItem(995, cost);
					getSlayerData().set(3, 0);
					sendFrame200(4883, npcFace);
					send(new SendString(GetNpcName(NpcTalkTo), 4884));
					send(new SendString("I have now canceled your " + taskName.toLowerCase() + " task!", 4885));
					send(new NpcDialogueHead(NpcTalkTo, 4883));
					sendFrame164(4882);
					nextDiag = 12;
				} else {
					sendFrame200(4883, npcFace);
					send(new SendString(GetNpcName(NpcTalkTo), 4884));
					send(new SendString("You do not have enough coins to cancel your task!", 4885));
					send(new NpcDialogueHead(NpcTalkTo, 4883));
					sendFrame164(4882);
				}
				send(new SendString("Click here to continue", 4886));
				NpcDialogueSend = true;
				break;
			case 34: //Upgrade black mask!
				if(!playerHasItem(8921))
					showNPCChat(NpcTalkTo, 596, new String[]{"You do not have a black mask!"});
				else {
					showNPCChat(NpcTalkTo, 591, new String[]{"Would you like to upgrade your black mask?", "It will cost you 2 million gold pieces."});
					this.nextDiag = 35;
				}
				NpcDialogueSend = true;
				break;
			case 35:
				showPlayerOption(new String[]{"Do you wish to upgrade?", "Yes, please", "No, please"});
				break;
			case 36:
				showPlayerChat(new String[]{"Yes, thank you."}, 614);
				NpcDialogueSend = true;
				nextDiag = 37;
				break;
			case 37:
				if(!playerHasItem(995, 2000000))
					showNPCChat(NpcTalkTo, 596, new String[]{"You do not have enough money!"});
				else {
					deleteItem(995, 2000000);
					deleteItem(8921, 1);
					addItem(11784, 1);
					showNPCChat(NpcTalkTo, 592, new String[]{"Here is your imbued black mask."});
				}
				NpcDialogueSend = true;
				break;
			case 14:
				sendFrame200(4883, npcFace);
				send(new SendString(GetNpcName(NpcTalkTo), 4884));
				send(new SendString("Be careful out there!", 4885));
				send(new SendString("Click here to continue", 4886));
				send(new NpcDialogueHead(NpcTalkTo, 4883));
				sendFrame164(4882);
				NpcDialogueSend = true;
				triggerTele(2542, 3306, 0, false);
				break;
			case 15:
				if (getSlayerData().get(0) != -1) {
					int slayerMaster = getSlayerData().get(0);
					String out = "Talk to me to get a new task!";
					SlayerTask.slayerTasks checkTask = SlayerTask.slayerTasks.getTask(getSlayerData().get(1));
					if (checkTask != null && getSlayerData().get(3) > 0)
						out = "You need to kill " + getSlayerData().get(3) + " more " + checkTask.getTextRepresentation();
					sendFrame200(4883, npcFace);
					send(new SendString(GetNpcName(slayerMaster), 4884));
					send(new SendString(out, 4885));
					send(new SendString("Click here to continue", 4886));
					send(new NpcDialogueHead(slayerMaster, 4883));
					sendFrame164(4882);
				} else
					send(new SendMessage("You have yet to get a task. Talk to a slayer master!"));
				NpcDialogueSend = true;
				break;
			case 16:
				sendFrame200(4883, npcFace);
				send(new SendString(GetNpcName(NpcTalkTo), 4884));
				send(new SendString("Would you like to buy some herblore supplies?", 4885));
				send(new SendString("Click here to continue", 4886));
				send(new NpcDialogueHead(NpcTalkTo, 4883));
				sendFrame164(4882);
				NpcDialogueSend = true;
				nextDiag = 17;
				break;

			case 17:
				send(new Frame171(1, 2465));
				send(new Frame171(0, 2468));
				send(new SendString("Would you like to buy herblore supplies?", 2460));
				send(new SendString("Sure, what do you have?", 2461));
				send(new SendString("No thanks", 2462));
				sendFrame164(2459);
				NpcDialogueSend = true;
				break;
			case 19:
				sendFrame200(4883, 591);
				send(new SendString(GetNpcName(NpcTalkTo), 4884));
				send(new SendString("Would you like to buy some supplies?", 4885));
				send(new SendString("Click here to continue", 4886));
				send(new NpcDialogueHead(NpcTalkTo, 4883));
				sendFrame164(4882);
				NpcDialogueSend = true;
				nextDiag = 20;
				break;
			case 20:
				send(new Frame171(1, 2465));
				send(new Frame171(0, 2468));
				send(new SendString("Would you like to buy some supplies?", 2460));
				send(new SendString("Sure, what do you have?", 2461));
				send(new SendString("No thanks", 2462));
				sendFrame164(2459);
				NpcDialogueSend = true;
				break;
			case 21:
				showNPCChat(getGender() == 0 ? 1306 : 1307, 588, new String[]{
						"Hello there, would you like to change your looks?",
						"If so, it will be free of charge"
				});
				NpcDialogueSend = true;
				break;
			case 22:
				showPlayerOption(new String[]{"Would you like to change your looks?", "Sure", "No thanks" });
				NpcDialogueSend = true;
				break;
			case 23:
				showPlayerChat(new String[]{ "I would love that." }, 614);
				NpcDialogueSend = true;
				break;
			case 24:
				showPlayerChat(new String[]{ "Not at the moment." }, 614);
				NpcDialogueSend = true;
				break;
			case 25:
				//showInterface(3559);
				sendFrame248(3559, 3213);
				NpcDialogue = 0;
				NpcDialogueSend = false;
				break;
			case 26:
				showPlayerOption(new String[]{ "What would you like to do?", "Enable specials", "Disable specials" });
				NpcDialogueSend = true;
				break;
			case 27:
				showPlayerOption(new String[]{ "What would you like to do?", "Enable boss yell messages", "Disable boss yell messages" });
				NpcDialogueSend = true;
				break;
			case 162:
				showNPCChat(NpcTalkTo, 591, new String[]{"Fancy meeting you here maggot.", "If you have any agility ticket,", "I would gladly take them from you."});
				nextDiag = 163;
				NpcDialogueSend = true;
				break;
			case 163:
				send(new Frame171(1, 2465));
				send(new Frame171(0, 2468));
				send(new SendString("Trade in tickets or teleport to agility course?", 2460));
				send(new SendString("Trade in tickets.", 2461));
				send(new SendString("Another course, please.", 2462));
				sendFrame164(2459);
				NpcDialogueSend = true;
				break;
			case 164: //Multiple!
				int type = skillX == 3002 && skillY == 3931 ? 3 : skillX == 2547 && skillY == 3554 ? 2 : 1;
				String[] type_gnome = new String[]{
						"Which course do you wish to be taken to?", "Barbarian", "Wilderness", "Stay here"};
				String[] type_barbarian = new String[]{
						"Which course do you wish to be taken to?", "Gnome", "Wilderness", "Stay here"};
				String[] type_wilderness = new String[]{
						"Which course do you wish to be taken to?", "Gnome", "Barbarian", "Stay here"};
				showPlayerOption(type == 3 ? type_wilderness : type == 2 ? type_barbarian : type_gnome);
				NpcDialogueSend = true;
				break;
			case 536:
				showPlayerOption(new String[]{"Do you wish to enter?", "Sacrifice 5 dragon bones", "Stay here"});
				NpcDialogueSend = true;
				break;
			case 2345:
				if(!checkUnlock(0)) {
					showNPCChat(NpcTalkTo, 591, new String[]{"Hello!", "Are you looking to enter my dungeon?", "You have to pay a to enter.", "You can also pay a one time fee."});
					nextDiag = ++NpcDialogue;
				} else
					showNPCChat(NpcTalkTo, 591, new String[]{"You can enter freely, no need to pay me anything."});
				NpcDialogueSend = true;
				break;
			case 2346:
				if(!checkUnlock(0) && checkUnlockPaid(0) != 1)
					showPlayerOption(new String[]{
							"Select a option", "Enter fee", "Permanent unlock", "Nevermind"});
				else if(checkUnlock(0))
					showNPCChat(NpcTalkTo, 591, new String[]{"You can enter freely, no need to pay me anything."});
				else
					showNPCChat(NpcTalkTo, 591, new String[]{"You have already paid.", "Just enter the dungeon now."});
				NpcDialogueSend = true;
				break;
			case 2182: //Jad cave
			case 2347: //Cave horror dungeon
				showPlayerOption(new String[]{"Select a option", "Ship ticket", "Coins"});
				NpcDialogueSend = true;
				break;
			case 2180:
				if(!checkUnlock(1)) {
					showNPCChat(NpcTalkTo, 591, new String[]{"Hello!", "Are you looking to enter my cave?", "You have to pay a to enter.", "You can also pay a one time fee."});
					nextDiag = ++NpcDialogue;
				} else
					showNPCChat(NpcTalkTo, 591, new String[]{"You can enter freely, no need to pay me anything."});
				NpcDialogueSend = true;
				break;
			case 2181:
				if(!checkUnlock(1) && checkUnlockPaid(1) != 1)
					showPlayerOption(new String[]{
							"Select a option", "Enter fee", "Permanent unlock", "Nevermind"});
				else if(checkUnlock(1))
					showNPCChat(NpcTalkTo, 591, new String[]{"You can enter freely, no need to pay me anything."});
				else
					showNPCChat(NpcTalkTo, 591, new String[]{"You have already paid.", "Just enter the cave now."});
				NpcDialogueSend = true;
				break;
			case 3648:
				showNPCChat(NpcTalkTo, 591, new String[]{"Hello dear.", "Would you like to travel?"});
				nextDiag = 3649;
				break;
			case 3649:
				showPlayerOption(new String[]{ "Do you wish to travel?", "Yes", "No" });
				break;
			case 6481:
				if(totalLevel() >= Skills.maxTotalLevel)
					showNPCChat(NpcTalkTo, 591, new String[]{"I see that you have trained up all your skills.", "I am utmost impressed!"});
				else
					showNPCChat(NpcTalkTo, 591, new String[]{"You are quite weak!"});
				nextDiag = NpcDialogue + 1;
				NpcDialogueSend = true;
				break;
			case 6482:
				if(totalLevel() >= Skills.maxTotalLevel) {
					showNPCChat(NpcTalkTo, 591, new String[]{"Would you like to purchase this cape on my back?", "It will cost you 13.37 million coins."});
					nextDiag = NpcDialogue + 1;
				} else
					showNPCChat(NpcTalkTo, 591, new String[]{"Come back when you have trained up your skills!"});
				NpcDialogueSend = true;
				break;
			case 6483:
				showPlayerOption(new String[]{ "Purchase the max cape?", "Yes", "No" });
				NpcDialogueSend = true;
				break;
			case 6484:
				int coins = 13370000;
				int freeSlot = getInvAmt(995) == coins ? 1 : 2;
				if(freeSlots() < freeSlot) {
					showNPCChat(NpcTalkTo, 591, new String[]{"You need atleast " + (freeSlot == 1 ? "one" : "two") + " free inventory slot" + (freeSlot != 1 ? "s" : "") + "."});
					nextDiag = NpcTalkTo;
				} else if(!playerHasItem(995, coins))
					showNPCChat(NpcTalkTo, 591, new String[]{"You are missing " + (coins - getInvAmt(995)) + " amount of coins!"});
				else {
					showNPCChat(NpcTalkTo, 591, new String[]{"Here you go.", "Max cape just for you."});
					deleteItem(995, coins);
					addItem(13281, 1);
					addItem(13280, 1);
				}
				NpcDialogueSend = true;
				break;
			case 8051:
				showNPCChat(NpcTalkTo, 591, new String[]{"Happy Holidays adventurer!"});
				nextDiag = 8052;
				NpcDialogueSend = true;
				break;
			case 8052:
				showNPCChat(NpcTalkTo, 591, new String[]{"The monsters are trying to ruin the new year!", "You must slay them to take back your gifts and", "save the spirit of 2021!"});
				nextDiag = 8053;
				NpcDialogueSend = true;
				break;
			case 8053:
				showPlayerOption(new String[]{
						"Select a option", "I'd like to see your shop.", "I'll just be on my way."});
				NpcDialogueSend = true;
				break;
			case 48054: //Travel shiet unlock!
				showPlayerOption(new String[]{ "Unlock the travel?", "Yes", "No" });
				NpcDialogueSend = true;
				break;
			case 10000:
				if(getLevel(Skill.SMITHING) >= 60 && playerHasItem(2347))
					showPlayerOption(new String[]{ "What would you like to make?", "Head", "Body", "Legs", "Boots", "Gloves" });
				else {
					send(new SendMessage(getLevel(Skill.SMITHING) < 60 ? "You need level 60 smithing to do this." : "You need a hammer to handle this material."));
					NpcDialogueSend = true;
				}
				break;
		}
	}

	public void showPlayerOption(String[] text) {
		int base = 2459;
		if (text.length == 4)
			base = 2469;
		if (text.length == 5)
			base = 2480;
		if (text.length == 6)
			base = 2492;
		send(new Frame171(1, base + 4 + text.length - 1));
		send(new Frame171(0, base + 7 + text.length - 1));
		for (int i = 0; i < text.length; i++)
			send(new SendString(text[i], base + 1 + i));
		sendFrame164(base);
	}

	public void showNPCChat(int npcId, int emote, String[] text) {
		int base = 4882;
		if (text.length == 2)
			base = 4887;
		if (text.length == 3)
			base = 4893;
		if (text.length == 4)
			base = 4900;
		send(new NpcDialogueHead(npcId, base + 1));
		sendFrame200(base + 1, emote);
		send(new SendString(GetNpcName(npcId), base + 2));
		for (int i = 0; i < text.length; i++)
			send(new SendString(text[i], base + 3 + i));
		send(new SendString("Click here to continue", base + 3 + text.length));
		sendFrame164(base);
	}

	public void showPlayerChat(String[] text, int emote) {
		int base = 968;
		if(text.length == 2)
			base = 973;
		if(text.length == 3)
			base = 979;
		if(text.length == 4)
			base = 986;
		send(new PlayerDialogueHead(base + 1));
		sendFrame200(base + 1, emote); //614 seems standard!
		send(new SendString(getPlayerName(), base + 2));
		for (int i = 0; i < text.length; i++)
			send(new SendString(text[i], base + 3 + i));
		sendFrame164(base);
	}

	/* Equipment level checking */
	public int GetCLAttack(int ItemID) {
		if (ItemID == -1) {
			return 1;
		}
		String ItemName = GetItemName(ItemID);
		String ItemName2 = ItemName.replaceAll("Bronze", "");

		ItemName2 = ItemName2.replaceAll("Iron", "");
		ItemName2 = ItemName2.replaceAll("Steel", "");
		ItemName2 = ItemName2.replaceAll("Black", "");
		ItemName2 = ItemName2.replaceAll("Mithril", "");
		ItemName2 = ItemName2.replaceAll("Adamant", "");
		ItemName2 = ItemName2.replaceAll("Rune", "");
		ItemName2 = ItemName2.replaceAll("Granite", "");
		ItemName2 = ItemName2.replaceAll("Dragon", "");
		ItemName2 = ItemName2.replaceAll("Crystal", "");
		ItemName2 = ItemName2.trim();
		if (ItemName2.startsWith("claws") || ItemName2.startsWith("dagger") || ItemName2.startsWith("sword")
				|| ItemName2.startsWith("scimitar") || ItemName2.startsWith("mace") || ItemName2.startsWith("longsword")
				|| ItemName2.startsWith("battleaxe") || ItemName2.startsWith("warhammer") || ItemName2.startsWith("2h sword")
				|| ItemName2.startsWith("halberd") || ItemName2.endsWith("axe") || ItemName2.endsWith("pickaxe")) {
			if (ItemName.startsWith("Bronze")) {
				return 1;
			} else if (ItemName.startsWith("Iron")) {
				return 1;
			} else if (ItemName.startsWith("Steel")) {
				return 10;
			} else if (ItemName.startsWith("Black")) {
				return 10;
			} else if (ItemName.startsWith("Mithril")) {
				return 20;
			} else if (ItemName.startsWith("Adamant")) {
				return 30;
			} else if (ItemName.startsWith("Rune")) {
				return 40;
			} else if (ItemName.startsWith("Dragon")) {
				return 60;
			}
		}
		if (ItemID == 21646)
			return 50;
		if (ItemID == 6523 || ItemID == 6525 || ItemID == 6527)
			return 55;
		if (ItemID == 3842 ||ItemID == 20223)
			return 45;
		return 1;
	}

	public int GetCLDefence(int ItemID) {
		if (ItemID == -1) return 1;
		String checkName = GetItemName(ItemID).toLowerCase();
		if (checkName.endsWith("arrow") || checkName.endsWith("hat") || (checkName.endsWith("axe") && !checkName.startsWith("battle")))
			return 1;
		String ItemName = GetItemName(ItemID);
		if (ItemName.toLowerCase().contains("beret") || ItemName.toLowerCase().contains("cavalier") || ItemName.toLowerCase().contains("mystic") || checkName.contains("mask") || checkName.contains("partyhat"))
			return 1;
		String ItemName2 = ItemName.replaceAll("Bronze", "");
		ItemName2 = ItemName2.replaceAll("Iron", "");
		ItemName2 = ItemName2.replaceAll("Steel", "");
		ItemName2 = ItemName2.replaceAll("Black", "");
		ItemName2 = ItemName2.replaceAll("Mithril", "");
		ItemName2 = ItemName2.replaceAll("Adamant", "");
		ItemName2 = ItemName2.replaceAll("Rune", "");
		ItemName2 = ItemName2.replaceAll("Granite", "");
		ItemName2 = ItemName2.replaceAll("Dragon", "");
		ItemName2 = ItemName2.replaceAll("Crystal", "");
		ItemName2 = ItemName2.trim();
		if (ItemName2.startsWith("claws") || ItemName2.startsWith("dagger") || ItemName2.startsWith("sword")
				|| ItemName2.startsWith("scimitar") || ItemName2.startsWith("mace") || ItemName2.startsWith("longsword")
				|| ItemName2.startsWith("battleaxe") || ItemName2.startsWith("warhammer") || ItemName2.startsWith("2h sword")
				|| ItemName2.startsWith("harlberd") || ItemName2.startsWith("pickaxe")) {// It's a weapon,
			return 1;
		} else if (ItemName.startsWith("Ahrims") || ItemName.startsWith("Karil") || ItemName.startsWith("Torag")
				|| ItemName.startsWith("Verac") || ItemName.endsWith("Guthan") || ItemName.endsWith("Dharok") ||
				ItemName.endsWith("staff") || ItemName.endsWith("crossbow") || ItemName.endsWith("hammers")
				|| ItemName.endsWith("flail") || ItemName.endsWith("warspear") || ItemName.endsWith("greataxe")) {
			return 1;
		} else {
			if (ItemName.startsWith("Saradomin") || ItemName.startsWith("Zamorak") || ItemName.startsWith("Guthix")
					&& ItemName.toLowerCase().contains("staff") && ItemName.toLowerCase().contains("cape"))
				return 1;
			if (ItemID == 11284)
				return 75;
			if (ItemID == 4224)
				return 70;
			if (ItemName.startsWith("Bronze")) {
				return 1;
			} else if (ItemName.startsWith("Iron")) {
				return 1;
			} else if (ItemName.startsWith("Steel") && !ItemName.contains("arrow")) {
				return 5;
			} else if (ItemName.startsWith("Black") && !ItemName.contains("hide")) {
				return 10;
			} else if (ItemName.startsWith("Slayer helmet")) {
				return 10;
			} else if (ItemName.startsWith("Mithril") && !ItemName.contains("arrow")) {
				return 20;
			} else if (ItemName.startsWith("Splitbark")) {
				return 20;
			} else if (ItemName.startsWith("Adamant") && !ItemName.contains("arrow")) {
				return 30;
			} else if (ItemName.startsWith("Rune") && !ItemName.endsWith("cape") && !ItemName.contains("arrow")) {
				return 40;
			} else if (ItemName.startsWith("Granite") && ItemID != 4153 && ItemID != 21646) {
				return 50;
			} else if (ItemName.startsWith("Dragon") && !ItemName.contains("hide") && !ItemName.toLowerCase().contains("ring") && !ItemName.toLowerCase().contains("necklace") && !ItemName.toLowerCase().contains("amulet")) {
				return 60;
			} else if (ItemName.startsWith("Rock-shell")) {
				return 60;
			}
		}
		if (ItemName.toLowerCase().contains("ghostly"))
			return 70;
		if (ItemName.startsWith("Skeletal"))
			return 1;
		if (ItemName.startsWith("Snakeskin body") || ItemName.startsWith("Snakeskin chaps"))
			return 60;
		if (ItemID == 1135 || ItemID == 2499 || ItemID == 2501 || ItemID == 2503)
			return 40;
		if(ItemID == 20235)
			return 45;
		if (ItemID == 6524 || ItemID == 21298 || ItemID == 21301 || ItemID == 21304) //Obsidian
			return 55;
		return 1;
	}

	public int GetCLStrength(int ItemID) {
		if (ItemID == -1) return 1;
		if (ItemID == 3842 || ItemID == 20223 || ItemID == 20232)
			return 45;
		if (ItemID == 4153)
			return 50;
		if (ItemID == 6528)
			return 55;
		return 1;
	}

	public int GetCLMagic(int ItemID) {
		if (ItemID == -1) return 1;
		String ItemName = GetItemName(ItemID);
		if (ItemName.startsWith("White Mystic") || ItemName.startsWith("Splitbark"))
			return 20;
		if(ItemID == 4675)
			return 25;
		if (ItemName.startsWith("Black Mystic"))
			return 35;
		if(ItemID == 6526)
			return 40;
		if(ItemID == 3840 || ItemID == 20220)
			return 45;
		if (ItemName.startsWith("Infinity") || ItemID == 6914)
			return 50;
		if (ItemID == 13235)
			return 60;
		if (ItemName.toLowerCase().contains("ghostly"))
			return 70;
		if (ItemName.startsWith("Ahrim"))
			return 80;
		return 1;
	}

	public int GetCLRanged(int ItemID) {
		if (ItemID == -1) return 1;
		String ItemName = GetItemName(ItemID);
		if (ItemName.startsWith("Karil")) {
			return 90;
		}
		if (ItemName.startsWith("Snakeskin")) {
			return 80;
		}
		if (ItemName.startsWith("New crystal bow")) {
			return 65;
		}
		if (ItemName.startsWith("Oak")) {
			return 1;
		}
		if (ItemName.startsWith("Willow")) {
			return 20;
		}
		if (ItemName.startsWith("Maple")) {
			return 30;
		}
		if (ItemName.startsWith("Yew")) {
			return 40;
		}
		if (ItemName.startsWith("Magic") && !ItemName.toLowerCase().contains("cape")) {
			return 50;
		}
		if (ItemName.startsWith("Green d")) {
			return 40;
		}
		if (ItemName.startsWith("Blue d")) {
			return 50;
		}
		if (ItemName.startsWith("Red d")) {
			return 60;
		}
		if (ItemName.startsWith("Black d")) {
			return 70;
		}
		if (ItemName.startsWith("Spined")) {
			return 75;
		}
		if (ItemName.startsWith("Dragon arr")) {
			return 60;
		}
		if (ItemName.startsWith("Rune arr")) {
			return 40;
		}
		if (ItemName.startsWith("Adamant arr")) {
			return 30;
		}
		if (ItemName.startsWith("Mithril arr")) {
			return 20;
		}
		if (ItemName.startsWith("Steel arr")) {
			return 10;
		}
		if (ItemID == 3844 || ItemID == 20226 || ItemID == 20229)
			return 45;
		if (ItemID == 6724)
			return 75;
		if (ItemID == 20997)
			return 85;
		return 1;
	}

	public void setInterfaceWalkable(int ID) {
		getOutputStream().createFrame(208);
		getOutputStream().writeWordBigEndian_dup(ID);
		//flushOutStream();
	}

	public void RefreshDuelRules() {
		/*
		 * Danno: Testing ticks/armour blocks.
		 */

		// "No Ranged", "No Melee", "No Magic",
		// "No Gear Change", "Fun Weapons", "No Retreat", "No Drinks",
		// "No Food", "No prayer", "No Movement", "Obstacles" };
		int configValue = 0;
		for (int i = 0; i < duelLine.length; i++) {
			if (duelRule[i]) {
				send(new SendString(/* "@red@" + */duelNames[i], duelLine[i]));
				configValue += stakeConfigId[i + 11];
			} else {
				send(new SendString(/* "@gre@" + */duelNames[i], duelLine[i]));
			}
		}
		for (int i = 0; i < duelBodyRules.length; i++) {
			if (duelBodyRules[i])
				configValue += stakeConfigId[i];
		}
		frame87(286, configValue);
	}

	public void DuelVictory() {
		Client other = getClient(duel_with);
		if (validClient(duel_with)) {
			send(new SendMessage("You have defeated " + other.getPlayerName() + "!"));
			send(new SendString("" + other.determineCombatLevel(), 6839));
			send(new SendString(other.getPlayerName(), 6840));
		}
		boolean stake = false;
		String playerStake = "";
		for (GameItem item : offeredItems) {
			if (item.getId() > 0 && item.getAmount() > 0) {
				playerStake += "(" + item.getId() + ", " + item.getAmount() + ")";
				stake = true;
			}
		}
		String opponentStake = "";
		for (GameItem item : otherOfferedItems) {
			if (item.getId() > 0 && item.getAmount() > 0) {
				opponentStake += "(" + item.getId() + ", " + item.getAmount() + ")";
				stake = true;
			}
		}
		resetAttack();

		if (stake) {
			DuelLog.recordDuel(this.getPlayerName(), other.getPlayerName(), playerStake, opponentStake, this.getPlayerName());
			itemsToVScreen_old();
			acceptDuelWon();
			other.resetDuel();
		} else {
			if (validClient(duel_with))
				other.resetDuel();
			resetDuel();
			// duelStatus = -1;
		}
		if (stake) {
			showInterface(6733);
		}

		/*
		 * Danno: Reset health.
		 */
		heal(getMaxHealth());
		getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);

	}

	public void itemsToVScreen_old() {
		if (disconnectAt > 0) {
			acceptDuelWon();
			return;
		}
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(6822);
		getOutputStream().writeWord(otherOfferedItems.toArray().length);
		for (GameItem item : otherOfferedItems) {
			if (item.getAmount() > 254) {
				getOutputStream().writeByte(255); // item's stack count. if over 254,
				// write byte 255
				getOutputStream().writeDWord_v2(item.getAmount()); // and then the real
				// value with
				// writeDWord_v2
			} else {
				getOutputStream().writeByte(item.getAmount());
			}
			getOutputStream().writeWordBigEndianA(item.getId() + 1); // item id
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public void refreshDuelScreen() {
		Client other = getClient(duel_with);
		if (!validClient(duel_with)) {
			return;
		}
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(6669);
		getOutputStream().writeWord(offeredItems.toArray().length);
		int current = 0;
		for (GameItem item : offeredItems) {
			if (item.getAmount() > 254) {
				getOutputStream().writeByte(255); // item's stack count. if over 254,
				// write byte 255
				getOutputStream().writeDWord_v2(item.getAmount()); // and then the real
				// value with
				// writeDWord_v2
			} else {
				getOutputStream().writeByte(item.getAmount());
			}
			getOutputStream().writeWordBigEndianA(item.getId() + 1); // item id
			current++;
		}
		if (current < 27) {
			for (int i = current; i < 28; i++) {
				getOutputStream().writeByte(1);
				getOutputStream().writeWordBigEndianA(-1);
			}
		}
		getOutputStream().endFrameVarSizeWord();
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(6670);
		getOutputStream().writeWord(other.offeredItems.toArray().length);
		current = 0;
		for (GameItem item : other.offeredItems) {
			if (item.getAmount() > 254) {
				getOutputStream().writeByte(255); // item's stack count. if over 254,
				// write byte 255
				getOutputStream().writeDWord_v2(item.getAmount()); // and then the real
				// value with
				// writeDWord_v2
			} else {
				getOutputStream().writeByte(item.getAmount());
			}
			getOutputStream().writeWordBigEndianA(item.getId() + 1); // item id
			current++;
		}
		if (current < 27) {
			for (int i = current; i < 28; i++) {
				getOutputStream().writeByte(1);
				getOutputStream().writeWordBigEndianA(-1);
			}
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public boolean stakeItem(int itemID, int fromSlot, int amount) {
		if (System.currentTimeMillis() - lastButton < 200)
			return false;
		lastButton = System.currentTimeMillis();
		if (!Server.itemManager.isStackable(itemID))
			amount = Math.min(amount, getInvAmt(itemID));
		else
			amount = Math.min(amount, playerItemsN[fromSlot]);
		if (!Server.itemManager.isTradable(itemID)) {
			send(new SendMessage("You can't trade that item"));
			return false;
		}
		Client other = getClient(duel_with);
		if (!inDuel || !validClient(duel_with)) {
			declineDuel();
			return false;
		}
		if (!canOffer) {
			declineDuel();
			return false;
		}
		if (!playerHasItem(itemID, amount) || playerItems[fromSlot] != (itemID + 1)) {
			return false;
		}
		if (!Server.itemManager.isTradable(itemID)) {
			send(new SendMessage("You can't trade this item"));
			return false;
		}
		if (Server.itemManager.isStackable(itemID)) {
			boolean inTrade = false;
			for (GameItem item : offeredItems) {
				if (item.getId() == itemID) {
					inTrade = true;
					item.addAmount(amount);
					deleteItem(itemID, fromSlot, amount);
					break;
				}
			}
			if (!inTrade) {
				offeredItems.add(new GameItem(itemID, amount));
				deleteItem(itemID, fromSlot, amount);
			}
		} else {
			for (int a = 1; a <= amount; a++) {
				if (a == 1) {
					offeredItems.add(new GameItem(itemID, 1));
					deleteItem(itemID, fromSlot, amount);
				} else {
					int slot = findItem(itemID, playerItems, playerItemsN);
					if (slot >= 0 && slot < 28)
						//tradeItem(itemID, slot, 1);
						offeredItems.add(new GameItem(itemID, 1));
					deleteItem(itemID, slot, amount);
				}
			}
		}
		resetItems(3214);
		resetItems(3322);
		other.resetItems(3214);
		other.resetItems(3322);
		refreshDuelScreen();
		other.refreshDuelScreen();
		send(new SendString("", 6684));
		other.send(new SendString("", 6684));
		return true;
	}

	public boolean checkGameitemAmount(int slot, int amount, CopyOnWriteArrayList<GameItem> item) {
		int count = 0;
		if (item.isEmpty()) return false;
		if (!item.get(slot).isStackable()) {
			for (GameItem checkItem : item) {
				if (checkItem.getId() == id)
					count++;
			}
		} else
			count = item.get(slot).getAmount();
		return amount >= count;
	}

	public boolean fromDuel(int itemID, int fromSlot, int amount) {
		if (System.currentTimeMillis() - lastButton >= 200) {
			lastButton = System.currentTimeMillis();
		} else {
			return false;
		}
		Client other = getClient(duel_with);
		if (!inDuel || !validClient(duel_with)) {
			declineDuel();
			return false;
		}
		if (!checkGameitemAmount(fromSlot, amount, offeredItems) || offeredItems.get(fromSlot).getId() != itemID) {
			return false;
		}
		int count = 0;
		if (!Server.itemManager.isStackable(itemID)) {
			for (GameItem item : offeredItems) {
				if (item.getId() == itemID) {
					count++;
				}
			}
		} else
			count = offeredItems.get(fromSlot).getAmount();
		amount = amount > count ? count : amount;
		boolean found = false;
		for (GameItem item : offeredItems) {
			if (item.getId() == itemID) {
				if (item.isStackable()) {
					if (amount < item.getAmount())
						offeredItems.set(fromSlot, new GameItem(item.getId(), item.getAmount() - amount));
					else
						offeredItems.remove(item);
					found = true;
				} else {
					if (amount == 1) {
						offeredItems.remove(item);
						found = true;
					} else {
						offeredItems.remove(item);
						addItem(itemID, 1);
						amount--;
					}
				}
				if (found) { //If found add item to inventory!
					addItem(itemID, amount);
					break;
				}
			}
		}
		duelConfirmed = false;
		other.duelConfirmed = false;
		resetItems(3214);
		other.resetItems(3214);
		resetItems(3322);
		other.resetItems(3322);
		refreshDuelScreen();
		other.refreshDuelScreen();
		other.send(new SendString("", 6684));

		return true;
	}

	public static String passHash(String in, String salt) {
		String passM = new MD5(in).compute();
		return new MD5(passM + salt).compute();
	}

	public String getLook() {
		String out = "";
		for (int playerLook : playerLooks) {
			out += playerLook + " ";
		}
		return out;
	}

	public String getPouches() {
		String out = "";
		for (int i = 0; i < runePouchesAmount.length; i++) {
			out += runePouchesAmount[i] + (i == runePouchesAmount.length - 1 ? "" : ":");
		}
		return out;
	}

	public void setLook(int[] parts) {
		/*
		 * if (parts.length != 13) { println("setLook:  Invalid array length!"); return;
		 * }
		 */
		setGender(parts[0]);
		setHead(parts[1]);
		setBeard(parts[2]);
		setTorso(parts[3]);
		setArms(parts[4]);
		setHands(parts[5]);
		setLegs(parts[6]);
		setFeet(parts[7]);
		pHairC = parts[8];
		pTorsoC = parts[9];
		pLegsC = parts[10];
		pFeetC = parts[11];
		pSkinC = parts[12];
		playerLooks = parts;
		getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
	}

	public boolean runeCheck() {
		if (playerHasItem(565))
			return true;
		send(new SendMessage("This spell requires 1 blood rune"));
		return false;
	}

	public void resetPos() {
		getPosition().moveTo(2604 + Misc.random(6), 3101 + Misc.random(3), 0); //Update position!
		teleportToX = getPosition().getX();
		teleportToY = getPosition().getY();
	}

	public boolean canUse(int id) {
		return !(!premium && premiumItem(id));
	}

	public boolean premiumItem(int id) {
		return Server.itemManager.isPremium(id);
	}

	public void debug(String text) {
		if (debug) {
			send(new SendMessage(text));
		}
	}

	public void showRandomEvent() {
		resetAction(true);
		if (!randomed || !randomed2) {
			random_skill = Utils.random(20);
			send(new SendString("Click the @or1@" + Skill.getSkill(random_skill).getName() + " @yel@button", 2810));
			send(new SendString("", 2811));
			send(new SendString("", 2831));
			randomed = true;
			showInterface(2808);
		}
	}

	public void triggerRandom(int xp) {
		xp /= 5;
		int reduceChance = Math.min(xp < 50 ? xp : xp < 100 ? (xp * 3) / 2 : xp < 200 ? xp * 2 : xp < 400 ? xp * 3 : xp * 4, 3000);
		reduceChance *= 2 + Misc.random(8);
		//System.out.println("testing..." + reduceChance + ", " + Math.min(reduceChance, 7000));
		if (Misc.chance(6500 - Math.min(reduceChance, 6000)) == 1) {
			showRandomEvent();
			chestEvent = 0;
		}
		if(chestEvent >= 50) { //Prevent auto clicking I believe!
			int chance = Misc.chance(100);
			int trigger = (chestEvent - 50) * 2;
			if(trigger >= chance) {
				chestEventOccur = true;
				chestEvent = 0;
				send(new SendMessage("The server randomly detect you standing still for to long! Please move!"));
			}
		}
	}

	public void openGenie() {
		if (inDuel || duelFight || IsBanking) {
			send(new SendMessage("Finish what you are doing first!"));
			return;
		}
		send(new SendString("Select a skill in which you wish to gain experience!", 2810));
		send(new SendString("", 2811));
		send(new SendString("", 2831));
		genie = true;
		showInterface(2808);
	}

	public int findItem(int id, int[] items, int[] amounts) {
		for (int i = 0; i < playerItems.length; i++) {
			if ((items[i] - 1) == id && amounts[i] > 0) {
				return i;
			}
		}
		return -1;
	}

	public boolean hasSpace() {
		for (int i = 0; i < playerItems.length; i++) {
			if (playerItems[i] == -1 || playerItems[i] == 0) {
				return true;
			}
		}
		return false;
	}

	public int getFreeSpace() {
		int spaces = 0;
		for (int i = 0; i < playerItems.length; i++) {
			if (playerItems[i] == -1 || playerItems[i] == 0) {
				spaces += 1;
			}
		}
		return spaces;
	}

	public void smelt(int id) {
		requestAnim(0x383, 0);
		smelt_id = id;
		smelting = true;
		int smelt_barId = -1;
		ArrayList<Integer> removed = new ArrayList<>();
		if (smeltCount < 1) {
			resetAction(true);
			return;
		}
		smeltCount--;
		switch (id) {
			case 2349: // bronze
				if (playerHasItem(436) && playerHasItem(438)) {
					smelt_barId = 2349;
					removed.add(436);
					removed.add(438);
				} else
					send(new SendMessage("You need a tin and copper to do this!"));
				break;
			case 2351: // iron ore
				if (getLevel(Skill.SMITHING) < 15) {
					send(new SendMessage("You need level 15 smithing to do this!"));
					break;
				}
				if (playerHasItem(440)) {
					int ran = new Range(1, 100).getValue();
					int diff = (getLevel(Skill.SMITHING) + 1) / 4;
					if (ran <= 50 + diff) {
						smelt_barId = 2351;
						removed.add(440);
					} else {
						smelt_barId = 0;
						removed.add(440);
						send(new SendMessage("You fail to refine the iron"));
					}
				} else
					send(new SendMessage("You need a iron ore to do this!"));
				break;
			case 2353:
				if (getLevel(Skill.SMITHING) < 30) {
					send(new SendMessage("You need level 30 smithing to do this!"));
					break;
				}
				if (playerHasItem(440) && playerHasItem(453, 2)) {
					smelt_barId = 2353;
					removed.add(440);
					removed.add(453);
					removed.add(453);
				} else
					send(new SendMessage("You need a iron ore and 2 coal to do this!"));
				break;
			case 2357:
				if (getLevel(Skill.SMITHING) < 40) {
					send(new SendMessage("You need level 40 smithing to do this!"));
					break;
				}
				if (playerHasItem(444, 1)) {
					smelt_barId = 2357;
					removed.add(444);
				} else
					send(new SendMessage("You need a gold ore to do this!"));
				break;
			case 2359:
				if (getLevel(Skill.SMITHING) < 55) {
					send(new SendMessage("You need level 55 smithing to do this!"));
					break;
				}
				if (playerHasItem(447) && playerHasItem(453, 3)) {
					smelt_barId = 2359;
					removed.add(447);
					removed.add(453);
					removed.add(453);
					removed.add(453);
				} else
					send(new SendMessage("You need a mithril ore and 3 coal to do this!"));
				break;
			case 2361:
				if (getLevel(Skill.SMITHING) < 70) {
					send(new SendMessage("You need level 70 smithing to do this!"));
					break;
				}
				if (playerHasItem(449) && playerHasItem(453, 4)) {
					smelt_barId = 2361;
					removed.add(449);
					removed.add(453);
					removed.add(453);
					removed.add(453);
					removed.add(453);
				} else
					send(new SendMessage("You need a adamantite ore and 4 coal to do this!"));
				break;
			case 2363:
				if (getLevel(Skill.SMITHING) < 85) {
					send(new SendMessage("You need level 85 smithing to do this!"));
					break;
				}
				if (playerHasItem(451) && playerHasItem(453, 6)) {
					smelt_barId = 2363;
					removed.add(451);
					for (int i = 0; i < 6; i++)
						removed.add(453);
				} else
					send(new SendMessage("You need a runite ore and 6 coal to do this!"));
				break;
			default:
				println("Unknown smelt: " + id);
				break;
		}
		if (smelt_barId == -1) {
			resetAction();
			return;
		}
		for (Integer removeId : removed) {
			deleteItem(removeId, 1);
		}
		if (smelt_barId > 0) {
			addItem(smelt_barId, 1);
			giveExperience(smeltExperience, Skill.SMITHING);
			triggerRandom(smeltExperience);
		}
	}

	public void superHeat(int id) {
		resetAction(false);
		ArrayList<Integer> removed = new ArrayList<>();
		int smelt_barId = 0;
		boolean fail = false;
		switch (id) {

			case 436: // Tin
			case 438: // Copper
				if (playerHasItem(436) && playerHasItem(438)) {
					smelt_barId = 2349;
					removed.add(436);
					removed.add(438);
				} else
					send(new SendMessage("You need a tin and copper to do this!"));
				break;
			case 440: // iron ore
				if (playerHasItem(440) && !playerHasItem(453, 2)) {
					if (getLevel(Skill.SMITHING) < 15) {
						send(new SendMessage("You need level 15 smithing to do this!"));
						break;
					}
					smelt_barId = 2351;
					removed.add(440);
				} else if (playerHasItem(440) && playerHasItem(453, 2)) {
					if (getLevel(Skill.SMITHING) < 30) {
						send(new SendMessage("You need level 30 smithing to do this!"));
						break;
					}
					smelt_barId = 2353;
					removed.add(440);
					removed.add(453);
					removed.add(453);
				}
				break;
			case 444:
				if (getLevel(Skill.SMITHING) < 40) {
					send(new SendMessage("You need level 40 smithing to do this!"));
					break;
				}
				smelt_barId = 2357;
				removed.add(444);
				break;
			case 447:
				if (getLevel(Skill.SMITHING) < 55) {
					send(new SendMessage("You need level 55 smithing to do this!"));
					break;
				}
				if (playerHasItem(447) && playerHasItem(453, 3)) {
					smelt_barId = 2359;
					removed.add(447);
					removed.add(453);
					removed.add(453);
					removed.add(453);
				} else
					send(new SendMessage("You need a mithril ore and 3 coal to do this!"));
				break;
			case 449:
				if (getLevel(Skill.SMITHING) < 70) {
					send(new SendMessage("You need level 70 smithing to do this!"));
					break;
				}
				if (playerHasItem(449) && playerHasItem(453, 4)) {
					smelt_barId = 2361;
					removed.add(449);
					removed.add(453);
					removed.add(453);
					removed.add(453);
					removed.add(453);
				} else
					send(new SendMessage("You need a adamantite ore and 4 coal to do this!"));
				break;
			case 451:
				if (getLevel(Skill.SMITHING) < 85) {
					send(new SendMessage("You need level 85 smithing to do this!"));
					break;
				}
				if (playerHasItem(451) && playerHasItem(453, 6)) {
					smelt_barId = 2363;
					removed.add(451);
					for (int i = 0; i < 6; i++)
						removed.add(453);
				} else
					send(new SendMessage("You need a runite ore and 6 coal to do this!"));
				break;
			default:
				fail = true;
				break;
		}
		int xp = 0;
		for (int i = 0; i < Utils.smelt_bars.length && xp == 0; i++)
			if (Utils.smelt_bars[i][0] == smelt_barId)
				xp = Utils.smelt_bars[i][1] * 4;
		if (fail) {
			send(new SendMessage("You can only use this spell on ores."));
			callGfxMask(85, 100);
		} else if (smelt_barId > 0 && xp > 0) {
			lastMagic = System.currentTimeMillis();
			requestAnim(725, 0);
			callGfxMask(148, 100);
			deleteRunes(new int[]{561}, new int[]{1});
			for (Integer removeId : removed)
				deleteItem(removeId, 1);
			addItem(smelt_barId, 1);
			giveExperience(xp, Skill.SMITHING);
			giveExperience(500, Skill.MAGIC);
		} else
			send(new SendMessage("This give no xp?!"));
		send(new SendSideTab(6));
	}

	public void runecraft(int rune, int level, int xp) {
		if (!contains(1436)) {
			send(new SendMessage("You do not have any rune essence!"));
			return;
		}
		if (getLevel(Skill.RUNECRAFTING) < level) {
			send(new SendMessage("You must have " + level + " runecrafting to craft " + GetItemName(rune).toLowerCase()));
			return;
		}
		int count = 0;
		int extra = 0;
		for (int c = 0; c < playerItems.length; c++) {
			if (playerItems[c] == 1437 && playerItemsN[c] > 0) {
				count++;
				deleteItem(1436, 1);
				int chance = (getLevel(Skill.RUNECRAFTING) + 1) / 2;
				int roll = 1 + Misc.random(99);
				if (roll <= chance)
					extra++;
			}
		}
		send(new SendMessage("You craft " + (count + extra) + " " + GetItemName(rune).toLowerCase() + "s"));
		addItem(rune, count + extra);
		giveExperience((xp * count), Skill.RUNECRAFTING);
		triggerRandom(xp * count);
	}

	public boolean fillEssencePouch(int pouch) {
		int slot = pouch == 5509 ? 0 : ((pouch - 5508) / 2);
		if (slot >= 0 && slot <= 3) {
			if (getLevel(Skill.RUNECRAFTING) < runePouchesLevel[slot]) {
				send(new SendMessage("You need level " + runePouchesLevel[slot] + " runecrafting to do this!"));
				return true;
			}
			if (runePouchesAmount[slot] >= runePouchesMaxAmount[slot]) {
				send(new SendMessage("This pouch is currently full of essence!"));
				return true;
			}
			int max = runePouchesMaxAmount[slot] - runePouchesAmount[slot];
			int amount = Math.min(getInvAmt(1436), max);
			if (amount > 0) {
				for (int i = 0; i < amount; i++)
					deleteItem(1436, 1);
				runePouchesAmount[slot] += amount;
			} else
				send(new SendMessage("No essence in your inventory!"));
			return true;
		}
		return false;
	}

	public boolean emptyEssencePouch(int pouch) {
		int slot = pouch == 5509 ? 0 : ((pouch - 5508) / 2);
		if (slot >= 0 && slot <= 3) {
			if (getLevel(Skill.RUNECRAFTING) < runePouchesLevel[slot]) {
				send(new SendMessage("You need level " + runePouchesLevel[slot] + " runecrafting to do this!"));
				return true;
			}
			int amount = freeSlots();
			if (amount <= 0) {
				send(new SendMessage("Not enough inventory slot to empty the pouch!"));
				return true;
			}
			amount = Math.min(amount, runePouchesAmount[slot]);
			if (amount > 0) {
				for (int i = 0; i < amount; i++)
					addItem(1436, 1);
				runePouchesAmount[slot] -= amount;
			} else
				send(new SendMessage("No essence in your pouch!"));
			return true;
		}
		return false;
	}

	public void resetAction(boolean full) {
		smelting = false;
		smelt_id = -1;
		goldCrafting = false;
		goldIndex = -1;
		goldSlot = -1;
		boneItem = -1;
		shafting = false;
		spinning = false;
		crafting = false;
		fishing = false;
		stringing = false;
		mining = false;
		minePick = -1;
		cooking = false;
		filling = false;
		mixPots = false;
		// woodcutting check
		if (woodcuttingIndex >= 0) {
			resetWC();
		}
		// smithing check
		if (smithing[0] > 0) {
			resetSM();
			send(new RemoveInterfaces());
		}
		if (fletchings || fletchingOther) {
			getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
		}
		fletchings = false;
		fletchingOther = false;
		if (full) {
			rerequestAnim();
		}
	}

	public void resetAction() {
		resetAction(true);
	}

	public void shaft() {
		if (IsCutting || isFiremaking)
			resetAction();
		send(new RemoveInterfaces());
		if (playerHasItem(1511)) {
			deleteItem(1511, 1);
			addItem(52, 15);
			requestAnim(1248, 0);
			giveExperience(50, Skill.FLETCHING);
			triggerRandom(50);
		} else {
			resetAction();
		}
	}

	public void fill() {
		if (playerHasItem(229)) {
			deleteItem(229, 1);
			addItem(227, 1);
			requestAnim(832, 0);
			animationReset = System.currentTimeMillis() + 600;
		} else {
			resetAction(true);
			resetAction();
		}
	}

	public long getSpinSpeed() {
		int craftingLevel = getLevel(Skill.CRAFTING);
		return craftingLevel >= 40 && craftingLevel < 70 ? 1200 : craftingLevel >= 70 ? 600 : 1800;
	}

	public void spin() {
		if (playerHasItem(1779)) {
			deleteItem(1779, 1);
			addItem(1777, 1);
			giveExperience(50, Skill.CRAFTING);
			triggerRandom(50);
		} else if (playerHasItem(1737)) {
			deleteItem(1737, 1);
			addItem(1759, 1);
			giveExperience(100, Skill.CRAFTING);
			triggerRandom(100);
		} else {
			send(new SendMessage("You do not have anything to spin!"));
			resetAction(true);
		}
		lastAction = System.currentTimeMillis();
	}

	public void replaceDoors() {
		for (int d = 0; d < DoorHandler.doorX.length; d++) {
			if (DoorHandler.doorX[d] > 0 && DoorHandler.doorHeight[d] == getPosition().getZ()
					&& Math.abs(DoorHandler.doorX[d] - getPosition().getX()) <= 120
					&& Math.abs(DoorHandler.doorY[d] - getPosition().getY()) <= 120) {
				if (distanceToPoint(DoorHandler.doorX[d], DoorHandler.doorY[d]) < 50) {
					ReplaceObject(DoorHandler.doorX[d], DoorHandler.doorY[d], DoorHandler.doorId[d], DoorHandler.doorFace[d], 0);
				}
			}
		}
	}

	public void openTan() {
		send(new SendString("Regular Leather", 14777));
		send(new SendString("50gp", 14785));
		send(new SendString("", 14781));
		send(new SendString("", 14789));
		send(new SendString("", 14778));
		send(new SendString("", 14786));
		send(new SendString("", 14782));
		send(new SendString("", 14790));
		int[] soon = {14779, 14787, 14783, 14791, 14780, 14788, 14784, 14792};
		String[] dhide = {"Green", "Red", "Blue", "Black"};
		String[] cost = {"1,000gp", "5,000gp", "2,000gp", "10,000gp"};
		int type = 0;
		for (int i = 0; i < soon.length; i++) {
			if (type == 0) {
				send(new SendString(dhide[i / 2], soon[i]));
				type = 1;
			} else {
				send(new SendString(cost[i / 2], soon[i]));
				type = 0;
			}
		}
		sendFrame246(14769, 250, 1741);
		sendFrame246(14773, 250, -1);
		sendFrame246(14771, 250, 1753);
		sendFrame246(14772, 250, 1751);
		sendFrame246(14775, 250, 1749);
		sendFrame246(14776, 250, 1747);
		showInterface(14670);
	}

	public void startTan(int amount, int type) {
		int[] hide = {1739, -1, 1753, 1751, 1749, 1747};
		int[] leather = {1741, -1, 1745, 2505, 2507, 2509};
		int[] charge = {50, 0, 1000, 2000, 5000, 10000};
		if (!playerHasItem(995, charge[type])) {
			send(new SendMessage("You need atleast " + charge[type] + " coins to do this!"));
			return;
		}
		amount = getInvAmt(995) > amount * charge[type] ? getInvAmt(995) / charge[type] : amount;
		amount = Math.min(amount, getInvAmt(hide[type]));
		for (int i = 0; i < amount; i++) {
			deleteItem(hide[type], 1);
			deleteItem(995, charge[type]);
			addItem(leather[type], 1);
		}
	}

	public void startCraft(int actionbutton) {
		send(new RemoveInterfaces());
		int[] buttons = {33187, 33186, 33185, 33190, 33189, 33188, 33193, 33192, 33191, 33196, 33195, 33194, 33199, 33198,
				33197, 33202, 33201, 33200, 33205, 33204, 33203};
		int[] amounts = {1, 5, 10, 1, 5, 10, 1, 5, 10, 1, 5, 10, 1, 5, 10, 1, 5, 10, 1, 5, 10};
		int[] ids = {1129, 1129, 1129, 1059, 1059, 1059, 1061, 1061, 1061, 1063, 1063, 1063, 1095, 1095, 1095, 1169, 1169,
				1169, 1167, 1167, 1167};
		int[] levels = {14, 1, 7, 11, 18, 38, 9};
		int[] exp = {33, 18, 21, 29, 38, 52, 20};
		int amount = 0, id = -1;
		int index = 0;
		for (int i = 0; i < buttons.length; i++) {
			if (actionbutton == buttons[i]) {
				amount = amounts[i];
				id = ids[i];
				index = i / 3;
				break;
			}
		}
		if (getLevel(Skill.CRAFTING) >= levels[index]) {
			cSelected = 1741;
			crafting = true;
			cItem = id;
			cAmount = amount == 10 ? getInvAmt(cSelected) : amount;
			cLevel = levels[index];
			cExp = Math.round(exp[index] * 8);
		} else if (id != -1) {
			send(new SendMessage("You need level " + levels[index] + " crafting to craft a " + GetItemName(id).toLowerCase()));
			send(new RemoveInterfaces());
		}
	}

	public void craft() {
		if (getLevel(Skill.CRAFTING) < cLevel) {
			send(new SendMessage("You need " + cLevel + " crafting to make a " + GetItemName(cItem).toLowerCase()));
			resetAction(true);
			return;
		}
		if (!playerHasItem(1733) || !playerHasItem(1734) || !playerHasItem(cSelected, 1)) {
			send(new SendMessage(!playerHasItem(1733) ? "You need a needle to craft!" : !playerHasItem(1734) ? "You have run out of thread!" : "You have run out of " + GetItemName(cSelected).toLowerCase() + "!"));
			resetAction(true);
			return;
		}
		if (cAmount > 0) {
			requestAnim(1249, 0);
			deleteItem(cSelected, 1);
			deleteItem(1734, 1);
			send(new SendMessage("You crafted a " + GetItemName(cItem).toLowerCase()));
			addItem(cItem, 1);
			giveExperience(cExp, Skill.CRAFTING);
			cAmount--;
			if (cAmount < 1)
				resetAction(true);
			triggerRandom(cExp);
		} else
			resetAction(true);
	}

	public void craftMenu(int i) {
		send(new SendString("What would you like to make?", 8898));
		send(new SendString("Vambraces", 8889));
		send(new SendString("Chaps", 8893));
		send(new SendString("Body", 8897));
		sendFrame246(8883, 250, Constants.gloves[i]);
		sendFrame246(8884, 250, Constants.legs[i]);
		sendFrame246(8885, 250, Constants.chests[i]);
		sendFrame164(8880);
	}

	public void startHideCraft(int b) {
		int[] buttons = {34185, 34184, 34183, 34182, 34189, 34188, 34187, 34186, 34193, 34192, 34191, 34190};
		int[] amounts = {1, 5, 10, 27};
		int index = 0;
		int index2 = 0;
		for (int i = 0; i < buttons.length; i++) {
			if (buttons[i] == b) {
				index = i % 4;
				index2 = i / 4;
				break;
			}
		}
		cSelected = Constants.leathers[cIndex];
		cAmount = amounts[index] == 27 ? getInvAmt(cSelected) : amounts[index];
		cExp = Constants.leatherExp[cIndex];
		int required;
		if (index2 == 0) {
			required = Constants.gloveLevels[cIndex];
			cItem = Constants.gloves[cIndex];
		} else if (index2 == 1) {
			required = Constants.legLevels[cIndex];
			cItem = Constants.legs[cIndex];
		} else {
			required = Constants.chestLevels[cIndex];
			cItem = Constants.chests[cIndex];
		}
		if (required != -1 && getLevel(Skill.CRAFTING) >= required) {
			cExp = cExp * 8;
			crafting = true;
			send(new RemoveInterfaces());
		} else if (required >= 0 && cItem != -1) {
			send(new SendMessage("You need level " + required + " crafting to craft a " + GetItemName(cItem).toLowerCase()));
			send(new RemoveInterfaces());
		} else
			send(new SendMessage("Can't make this??"));
	}

	public void modYell(String msg) {
		for (int i = 0; i < PlayerHandler.players.length; i++) {
			Client p = (Client) PlayerHandler.players[i];
			if (p != null && !p.disconnected && p.getPosition().getX() > 0 && p.dbId > 0 && p.playerRights > 0) {
				p.send(new SendMessage(msg));
			}
		}
	}

	public void triggerTele(int x, int y, int height, boolean prem) {
		triggerTele(x, y, height, prem, 1816);
	}

	public void triggerTele(int x, int y, int height, boolean prem, int emote) {
		if (inDuel || duelStatus == 3 || UsingAgility || System.currentTimeMillis() - lastTeleport < 3000) {
			return;
		}
		if (randomed2) {
			send(new SendMessage("You can't teleport out of here!"));
			return;
		}
		if (inWildy()) {
			send(new SendMessage("You can't teleport out of the wilderness!"));
			return;
		}
		if (prem && !premium) {
			send(new SendMessage("This spell is only available to premium members, visit Dodian.net for info"));
			return;
		}
		lastTeleport = System.currentTimeMillis();
		resetAction();
		resetWalkingQueue();
		tX = x;
		tY = y;
		tH = height;
		tStage = 1;
		tEmote = emote;
		UsingAgility = true;
	}

	public void startSmelt(int id) {
		int[] amounts = {1, 5, 10, 28};
		int index = 0, index2 = 0;
		for (int i = 0; i < Utils.buttons_smelting.length; i++) {
			if (id == Utils.buttons_smelting[i]) {
				index = i % 4;
				index2 = i / 4;
			}
		}
		smelt_id = Utils.smelt_bars[index2][0];
		smeltCount = amounts[index];
		smeltExperience = Utils.smelt_bars[index2][1] * 4;
		smelting = true;
		send(new RemoveInterfaces());
	}

	public void startFishing(int object, int click) {
		boolean valid = false;
		for (int i = 0; i < Utils.fishSpots.length; i++) {
			if (Utils.fishSpots[i] == object) {
				if (click == 1 && (i == 0 || i == 2 || i == 4 || i == 6)) {
					valid = true;
					fishIndex = i;
					break;
				} else if (click == 2 && (i == 1 || i == 3 || i == 5 || i == 7)) {
					valid = true;
					fishIndex = i;
					break;
				}
			}
		}
		if (!valid) {
			resetAction(true);
			return;
		}
		if (!playerHasItem(-1)) {
			send(new SendMessage("Not enough inventory space!"));
			resetAction(true);
			return;
		}
		if ((fishIndex == 4 || fishIndex >= 6) && !premium) {
			send(new SendMessage("You need to be premium to fish from this spot!"));
			resetAction(true);
			return;
		}
		if (!playerHasItem(314) && fishIndex == 1) {
			send(new SendMessage("You do not have any feathers!"));
			resetAction(true);
			return;
		}
		if (getLevel(Skill.FISHING) < Utils.fishReq[fishIndex]) {
			send(new SendMessage("You need " + Utils.fishReq[fishIndex] + " fishing to fish here"));
			resetAction(true);
			return;
		}
		if (!playerHasItem(Utils.fishTool[fishIndex])) {
			send(new SendMessage("You need a " + GetItemName(Utils.fishTool[fishIndex]) + " to fish here"));
			resetAction(true);
			return;
		}
		lastAction = System.currentTimeMillis() + Utils.fishTime[fishIndex];
		requestAnim(Utils.fishAnim[fishIndex], 0);
		fishing = true;
	}

	public void fish() {
		lastAction = System.currentTimeMillis();
		if (!playerHasItem(-1)) {
			send(new SendMessage("Not enough inventory space!"));
			resetAction(true);
			return;
		}
		if (!playerHasItem(314) && fishIndex == 1) {
			send(new SendMessage("You do not have any feathers!"));
			resetAction(true);
			return;
		}
		if (fishIndex == 1) {
			deleteItem(314, 1);
			int random = Misc.random(6);
			if (getLevel(Skill.FISHING) >= 30 && random < 3) {
				addItem(331, 1);
				giveExperience(Utils.fishExp[fishIndex] + 100, Skill.FISHING);
				send(new SendMessage("You fish some salmon."));
			} else {
				giveExperience(Utils.fishExp[fishIndex], Skill.FISHING);
				addItem(Utils.fishId[fishIndex], 1);
				send(new SendMessage("You fish some trout."));
			}
		} else {
			giveExperience(Utils.fishExp[fishIndex], Skill.FISHING);
			addItem(Utils.fishId[fishIndex], 1);
			send(new SendMessage("You fish some " + GetItemName(Utils.fishId[fishIndex]).toLowerCase() + ""));
		}
		requestAnim(Utils.fishAnim[fishIndex], 0);
		triggerRandom(Utils.fishExp[fishIndex]);
		if (Misc.chance(30) == 1) {
			send(new SendMessage("You take a rest"));
			resetAction(true);
		}
	}

	public void startCooking(int id) {
		if (inTrade || inDuel) {
			send(new SendMessage("Cannot cook in duel or trade"));
			return;
		}
		boolean valid = false;
		for (int i = 0; i < Utils.cookIds.length; i++) {
			if (id == Utils.cookIds[i]) {
				cookIndex = i;
				valid = true;
			}
		}
		if (valid) {
			cookAmount = getInvAmt(id);
			cooking = true;
		}

	}

	public void cook() {
		if (inTrade || inDuel || cookAmount < 1) {
			resetAction(true);
			return;
		}
		if (!playerHasItem(Utils.cookIds[cookIndex])) {
			send(new SendMessage("You are out of fish"));
			resetAction(true);
			return;
		}
		int id = Utils.cookIds[cookIndex];
		int ran = 0, index = 0;
		for (int i = 0; i < Utils.cookIds.length; i++) {
			if (id == Utils.cookIds[i]) {
				index = i;
			}
		}
		if (getLevel(Skill.COOKING) < Utils.cookLevel[index]) {
			send(new SendMessage("You need " + Utils.cookLevel[index] + " cooking to cook the " + Server.itemManager.getName(id).toLowerCase() + "."));
			resetAction(true);
			return;
		}
		switch (id) {
			case 2134:
			case 317:
				ran = 30 - getLevel(Skill.COOKING);
				break;
			case 2307:
				ran = 36 - getLevel(Skill.COOKING);
				break;
			case 3363:
				ran = 42 - getLevel(Skill.COOKING);
				break;
			case 335:
				ran = 50 - getLevel(Skill.COOKING);
				break;
			case 331:
				ran = 60 - getLevel(Skill.COOKING);
				break;
			case 377:
				ran = 70 - getLevel(Skill.COOKING);
				break;
			case 371:
				ran = 80 - getLevel(Skill.COOKING);
				break;
			case 7944:
				ran = 90 - getLevel(Skill.COOKING);
				break;
			case 383:
				ran = 100 - getLevel(Skill.COOKING);
				break;
			case 395:
				ran = 110 - getLevel(Skill.COOKING);
				break;
			case 389:
				ran = 120 - getLevel(Skill.COOKING);
				break;
		}
		if (getEquipment()[Equipment.Slot.HANDS.getId()] == 775)
			ran -= 4;
		if (getEquipment()[Equipment.Slot.HEAD.getId()] == 1949)
			ran -= 4;
		if (getEquipment()[Equipment.Slot.HEAD.getId()] == 1949 && getEquipment()[Equipment.Slot.HANDS.getId()] == 775)
			ran -= 2;
		ran = ran < 0 ? 0 : ran > 100 ? 100 : ran;
		boolean burn = 1 + Utils.random(99) <= ran;
		if (Utils.cookExp[index] > 0) {
			cookAmount--;
			deleteItem(id, 1);
			setFocus(skillX, skillY);
			requestAnim(883, 0);
			if (!burn) {
				addItem(Utils.cookedIds[index], 1);
				send(new SendMessage("You cook the " + GetItemName(id)));
				giveExperience(Utils.cookExp[index], Skill.COOKING);
			} else {
				addItem(Utils.burnId[index], 1);
				send(new SendMessage("You burn the " + GetItemName(id)));
			}
			triggerRandom(Utils.cookExp[index]);
		}
	}

	public boolean inHeat() { //King black dragon's domain!
		return getPosition().getX() >= 3264 && getPosition().getX() <= 3327 && getPosition().getY() >= 9344 && getPosition().getY() <= 9407;
	}

	public void openTrade() {
		if (inHeat()) {
			send(new SendMessage("It would not be a wise idea to trade with the heat in the background!"));
			return;
		}
		send(new InventoryInterface(3323, 3321)); // trading window + bag
		inTrade = true;
		tradeRequested = false;
		resetItems(3322);
		resetTItems(3415);
		resetOTItems(3416);
		String out = PlayerHandler.players[trade_reqId].getPlayerName();
		if (PlayerHandler.players[trade_reqId].playerRights == 1) {
			out = "@cr1@" + out;
		} else if (PlayerHandler.players[trade_reqId].playerRights == 2) {
			out = "@cr2@" + out;
		}
		send(new SendString("Trading With: " + PlayerHandler.players[trade_reqId].getPlayerName(), 3417));
		send(new SendString("", 3431));
		send(new SendString("Are you sure you want to make this trade?", 3535));
	}

	public void declineTrade() {
		declineTrade(true);
	}

	public void declineTrade(boolean tellOther) {
		Client other = getClient(trade_reqId);
		/* Prevent a dupe? */
		inTrade = false;
		if (tellOther && validClient(trade_reqId)) {
			other.declineTrade(false);
			other.inTrade = false;
		}
		/* Clear the trade! */
		for (GameItem item : offeredItems) {
			if (item.getAmount() > 0) {
				if (Server.itemManager.isStackable(item.getId())) {
					addItem(item.getId(), item.getAmount());
				} else {
					for (int i = 0; i < item.getAmount(); i++) {
						addItem(item.getId(), 1);
					}
				}
			}
		}
		send(new RemoveInterfaces());
		canOffer = true;
		tradeConfirmed = false;
		tradeConfirmed2 = false;
		offeredItems.clear();
		trade_reqId = -1;
	}

	public boolean validClient(int index) {
		Client p = (Client) PlayerHandler.players[index];
		return p != null && !p.disconnected && p.dbId > 0;
	}

	public Client getClient(int index) {
		return index < 0 ? null : ((Client) PlayerHandler.players[index]);
	}

	public void tradeReq(int id) {
		// followPlayer(id);
		facePlayer(id);
		if (!Server.trading) {
			send(new SendMessage("Trading has been temporarily disabled"));
			return;
		}
		for (int a = 0; a < PlayerHandler.players.length; a++) {
			Client o = getClient(a);
			if (a != getSlot() && validClient(a) && o.dbId > 0 && o.dbId == dbId) {
				logout();
			}
		}
		Client other = (Client) PlayerHandler.players[id];
		if (validClient(trade_reqId)) {
			setFocus(other.getPosition().getX(), other.getPosition().getY());
			if (inTrade || inDuel || other.inTrade || other.inDuel) {
				send(new SendMessage("That player is busy at the moment"));
				trade_reqId = 0;
				return;
			}
			if (tradeLocked && other.playerRights < 1) {
				return;
			}
		}
		if (dbId == other.dbId) {
			return;
		}
		/*
		 * if(other.connectedFrom.equals(connectedFrom) &&
		 * !connectedFrom.equals("127.0.0.1")){ tradeRequested = false; return; }
		 */
		if (validClient(trade_reqId) && !inTrade && other.tradeRequested && other.trade_reqId == getSlot()) {
			openTrade();
			other.openTrade();
		} else if (validClient(trade_reqId) && !inTrade && System.currentTimeMillis() - lastButton > 1000) {
			lastButton = System.currentTimeMillis();
			tradeRequested = true;
			trade_reqId = id;
			send(new SendMessage("Sending trade request..."));
			other.send(new SendMessage(getPlayerName() + ":tradereq:"));
		}
	}

	public void confirmScreen() {
		canOffer = false;
		send(new InventoryInterface(3443, 3213)); // trade confirm + normal bag
		inTrade = true;
		resetItems(3214);
		StringBuilder SendTrade = new StringBuilder("Absolutely nothing!");
		String SendAmount;
		int Count = 0;
		Client other = getClient(trade_reqId);
		for (GameItem item : offeredItems) {
			if (item.getId() > 0) {
				if (item.getAmount() >= 1000 && item.getAmount() < 1000000) {
					SendAmount = "@cya@" + (item.getAmount() / 1000) + "K @whi@(" + Utils.format(item.getAmount()) + ")";
				} else if (item.getAmount() >= 1000000) {
					SendAmount = "@gre@" + (item.getAmount() / 1000000) + " million @whi@(" + Utils.format(item.getAmount())
							+ ")";
				} else {
					SendAmount = "" + Utils.format(item.getAmount());
				}
				if (Count == 0) {
					SendTrade = new StringBuilder(GetItemName(item.getId()));
				} else {
					SendTrade.append("\\n").append(GetItemName(item.getId()));
				}
				if (item.isStackable()) {
					SendTrade.append(" x ").append(SendAmount);
				}
				Count++;
			}
		}
		send(new SendString(SendTrade.toString(), 3557));
		SendTrade = new StringBuilder("Absolutely nothing!");
		Count = 0;
		for (GameItem item : other.offeredItems) {
			if (item.getId() > 0) {
				if (item.getAmount() >= 1000 && item.getAmount() < 1000000) {
					SendAmount = "@cya@" + (item.getAmount() / 1000) + "K @whi@(" + Utils.format(item.getAmount()) + ")";
				} else if (item.getAmount() >= 1000000) {
					SendAmount = "@gre@" + (item.getAmount() / 1000000) + " million @whi@(" + Utils.format(item.getAmount())
							+ ")";
				} else {
					SendAmount = "" + Utils.format(item.getAmount());
				}
				// SendAmount = SendAmount;
				if (Count == 0) {
					SendTrade = new StringBuilder(GetItemName(item.getId()));
				} else {
					SendTrade.append("\\n").append(GetItemName(item.getId()));
				}
				if (Server.itemManager.isStackable(item.getId())) {
					SendTrade.append(" x ").append(SendAmount);
				}
				Count++;
			}
		}
		send(new SendString(SendTrade.toString(), 3558));
	}

	private boolean tradeSuccessful = false;

	public void giveItems() {
		Client other = getClient(trade_reqId);
		if (validClient(trade_reqId)) {
			try {
				CopyOnWriteArrayList<GameItem> offerCopy = new CopyOnWriteArrayList<>();
				CopyOnWriteArrayList<GameItem> otherOfferCopy = new CopyOnWriteArrayList<>();
				for (GameItem item : other.offeredItems) {
					otherOfferCopy.add(new GameItem(item.getId(), item.getAmount()));
				}
				for (GameItem item : offeredItems) {
					offerCopy.add(new GameItem(item.getId(), item.getAmount()));
				}
				for (GameItem item : other.offeredItems) {
					if (item.getId() > 0) {
						addItem(item.getId(), item.getAmount());
						println("TradeConfirmed, item=" + item.getId());
					}
				}
				if (this.dbId > other.dbId)
					TradeLog.recordTrade(dbId, other.dbId, offerCopy, otherOfferCopy, true);
				send(new RemoveInterfaces());
				tradeResetNeeded = true;
				saveStats(false);
				tradeSuccessful = true;
				//System.out.println("trade succesful");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void resetTrade() {
		offeredItems.clear();
		inTrade = false;
		trade_reqId = 0;
		canOffer = true;
		tradeConfirmed = false;
		tradeConfirmed2 = false;
		send(new RemoveInterfaces());
		tradeResetNeeded = false;
		send(new SendString("Are you sure you want to make this trade?", 3535));
	}

	public void duelReq(int pid) {
		facePlayer(pid);
		Client other = getClient(pid);
		if (inWildy() || other.inWildy()) {
			send(new SendMessage("You cant duel in the wilderness!"));
			return;
		}
		if (!Server.dueling) {
			send(new SendMessage("Dueling has been temporarily disabled"));
			return;
		}
		for (int a = 0; a < PlayerHandler.players.length; a++) {
			Client o = getClient(a);
			if (a != getSlot() && validClient(a) && o.dbId > 0 && o.dbId == dbId) {
				logout();
			}
		}
		duel_with = pid;
		duelRequested = true;
		if (!validClient(duel_with)) {
			return;
		}
		setFocus(other.getPosition().getX(), other.getPosition().getY());
		if (inTrade || inDuel || other.inDuel || other.inTrade || other.duelFight || other.duelConfirmed
				|| other.duelConfirmed2) {
			send(new SendMessage("Other player is busy at the moment"));
			duelRequested = false;
			return;
		}
		if (tradeLocked && other.playerRights < 1) {
			return;
		}
		if (other.connectedFrom.equals(connectedFrom) && getGameWorldId() == 1) { //We do not wish for others to duel?!
			duelRequested = false;
			return;
		}
		if (duelRequested && other.duelRequested && duel_with == other.getSlot() && other.duel_with == getSlot()) {
			openDuel();
			other.openDuel();
		} else {
			send(new SendMessage("Sending duel request..."));
			other.send(new SendMessage(getPlayerName() + ":duelreq:"));
		}
	}

	public void openDuel() {
		RefreshDuelRules();
		refreshDuelScreen();
		inDuel = true;
		Client other = getClient(duel_with);
		send(new SendString("Dueling with: " + other.getPlayerName() + " (level-" + other.determineCombatLevel() + ")", 6671));
		send(new SendString("", 6684));
		send(new InventoryInterface(6575, 3321));
		resetItems(3322);
		sendArmour();
	}

	public void declineDuel() {
		Client other = getClient(duel_with);
		inDuel = false;
		if (validClient(duel_with) && other.inDuel) {
			other.declineDuel();
		}
		send(new RemoveInterfaces());
		canOffer = true;
		duel_with = 0;
		duelRequested = false;
		duelConfirmed = false;
		duelConfirmed2 = false;
		duelFight = false;
		for (GameItem item : offeredItems) {
			if (item.getAmount() < 1) {
				continue;
			}
			println("adding " + item.getId() + ", " + item.getAmount());
			if (Server.itemManager.isStackable(item.getId()) || Server.itemManager.isNote(item.getId())) {
				addItem(item.getId(), item.getAmount());
			} else {
				addItem(item.getId(), 1);
			}
		}
		offeredItems.clear();
		/*
		 * Danno: Reset's duel options when duel declined to stop scammers.
		 */
		resetDuel();
		RefreshDuelRules();
		failer = "";
	}

	public void confirmDuel() {
		Client other = getClient(duel_with);
		if (!validClient(duel_with)) {
			declineDuel();
		}
		StringBuilder out = new StringBuilder();
		for (GameItem item : offeredItems) {
			if (Server.itemManager.isStackable(item.getId()) || Server.itemManager.isNote(item.getId())) {
				out.append(GetItemName(item.getId())).append(" x ").append(Utils.format(item.getAmount())).append(", ");
			} else {
				out.append(GetItemName(item.getId())).append(", ");
			}
		}
		send(new SendString(out.toString(), 6516));
		out = new StringBuilder();
		for (GameItem item : other.offeredItems) {
			if (Server.itemManager.isStackable(item.getId()) || Server.itemManager.isNote(item.getId())) {
				out.append(GetItemName(item.getId())).append(" x ").append(Utils.format(item.getAmount())).append(", ");
			} else {
				out.append(GetItemName(item.getId())).append(", ");
			}
		}
		send(new SendString(out.toString(), 6517));
		send(new SendString("Movement will be disabled", 8242));
		for (int i = 8243; i <= 8253; i++) {
			send(new SendString("", i));
		}
		send(new SendString("Hitpoints will be restored", 8250));
		send(new SendString("", 6571));
		showInterface(6412);
	}

	public void startDuel() {
		canAttack = false;
		// canAttack = true;
		canOffer = false;
		send(new RemoveInterfaces());
		duelFight = true;
		prayers.reset();
		GetBonus(true); //Set bonus due to blessing!
		for(int i = 0; i < boostedLevel.length; i++) {
			boostedLevel[i] = 0;
			refreshSkill(Skill.getSkill(i));
		}
		Client other = getClient(duel_with);
		for (GameItem item : other.offeredItems) {
			otherOfferedItems.add(new GameItem(item.getId(), item.getAmount()));
		}
		otherdbId = other.dbId;

		final Client player = this;
		EventManager.getInstance().registerEvent(new Event(1000) {
			long start = System.currentTimeMillis();

			public void execute() {
				long now = System.currentTimeMillis();
				if (now - start >= 4000) {
					player.requestForceChat("Fight!");
					player.canAttack = true;
					stop();
				} else if (now - start >= 3000) {
					player.requestForceChat("1");
				} else if (now - start >= 2000) {
					player.requestForceChat("2");
				} else if (now - start >= 1000) {
					player.requestForceChat("3");

				}
			}
		});
	}

	/*
	 * Danno: Edited for new duel rules, for future use.
	 */
	public void resetDuel() {
		send(new RemoveInterfaces());
		duelWin = false;
		canOffer = true;
		duel_with = 0;
		duelRequested = false;
		duelConfirmed = false;
		duelConfirmed2 = false;
		offeredItems.clear();
		otherOfferedItems.clear();
		duelFight = false;
		canAttack = true;
		inDuel = false;
		duelRule = new boolean[]{false, false, false, false, false, true, true, true, true, true, true};
		Arrays.fill(duelBodyRules, false);
		otherdbId = -1;
	}

	public void frame36(int Interface, int Status) {
		getOutputStream().createFrame(36);
		getOutputStream().writeWordBigEndian(Interface); // The button
		getOutputStream().writeByte(Status); // The Status of the button
	}

	public void frame87(int Interface, int Status) {
		getOutputStream().createFrame(87);
		getOutputStream().writeWordBigEndian(Interface); // The button
		getOutputStream().writeDWord_v1(Status); // The Status of the button
	}

	public boolean duelButton(int button) {
		Client other = getClient(duel_with);
		boolean found = false;
		if (System.currentTimeMillis() - lastButton < 800) {
			return false;
		}
		if (inDuel && !duelFight && !duelConfirmed2 && !other.duelConfirmed2 && !(duelConfirmed && other.duelConfirmed)) {
			for (int i = 0; i < duelButtons.length; i++) {
				if (button == duelButtons[i]) {
					found = true;
					if (duelRule[i]) {
						duelRule[i] = false;
						other.duelRule[i] = false;
					} else {
						duelRule[i] = true;
						other.duelRule[i] = true;
					}
				}
			}
			if (found) {
				lastButton = System.currentTimeMillis();
				duelConfirmed = false;
				other.duelConfirmed = false;
				send(new SendString("", 6684));
				other.send(new SendString("", 6684));

				other.duelRule[i] = duelRule[i];
				RefreshDuelRules();
				other.RefreshDuelRules();
			}
		}
		return found;
	}

	public boolean duelButton2(int button) {
		Client other = getClient(duel_with);
		/*
		 * Danno: Null check :p
		 */
		if (other == null)
			return false;
		boolean found = false;
		if (System.currentTimeMillis() - lastButton < 400) {
			return false;
		}
		if (inDuel && !duelFight && !duelConfirmed2 && !other.duelConfirmed2 && !(duelConfirmed && other.duelConfirmed)) {
			if (duelBodyRules[button]) {
				duelBodyRules[button] = false;
				other.duelBodyRules[button] = false;
			} else {
				duelBodyRules[button] = true;
				other.duelBodyRules[button] = true;
			}
			lastButton = System.currentTimeMillis();
			duelConfirmed = false;
			other.duelConfirmed = false;
			send(new SendString("", 6684));
			other.send(new SendString("", 6684));
			other.duelBodyRules[i] = duelBodyRules[i];
			RefreshDuelRules();
			other.RefreshDuelRules();
		}
		return found;
	}

	public void addFriend(long name) {
		// On = 0, Friends = 1, Off = 2
		for (Friend f : friends) {
			if (f.name == name) {
				send(new SendMessage(Utils.longToPlayerName(name) + " is already on your friends list"));
				return;
			}
		}
		friends.add(new Friend(name, true));
		for (Client c : PlayerHandler.playersOnline.values()) {
			if (c.hasFriend(longName)) {
				c.refreshFriends();
			}
		}
		refreshFriends();
	}

	public void sendPmMessage(long friend, byte[] pmchatText, int pmchatTextSize) {
		if (muted) {
			return;
		}
		boolean found = false;
		for (Friend f : friends) {
			if (f.name == friend) {
				found = true;
				break;
			}
		}
		if (!found) {
			send(new SendMessage("That player is not on your friends list"));
			return;
		}
		if (PlayerHandler.playersOnline.containsKey(friend)) {
			Client to = PlayerHandler.playersOnline.get(friend);
			boolean specialRights = to.playerGroup == 6 || to.playerGroup == 10 || to.playerGroup == 35;
			if (specialRights && to.busy && playerRights < 1) {
				send(new SendMessage("<col=FF0000>This player is busy and did not receive your message."));
				//send(new SendMessage("Please only report glitch/bugs to him/her on the forums"));
				return;
			}
			if (to.Privatechat == 0 || (to.Privatechat == 1 && to.hasFriend(longName))) {
				to.sendpm(longName, playerRights, pmchatText, pmchatTextSize);
				PmLog.recordPm(this.getPlayerName(), to.getPlayerName(), Utils.textUnpack(pmchatText, pmchatTextSize));
			} else {
				send(new SendMessage("That player is not available"));
			}
		} else if (PlayerHandler.allOnline.containsKey(friend)) { //Not sure why we need this code!
		} else {
			send(new SendMessage("That player is not online"));
		}
		/*
		 * for (int i1 = 0; i1 < handler.players.length; i1++) { client to =
		 * getClient(i1); if (validClient(i1)) { if (validClient(i1) && to.dbId > 0
		 * && misc.playerNameToInt64(to.playerName) == friend) { if (to.Privatechat
		 * == 0 || (to.Privatechat == 1 && to.hasFriend(misc
		 * .playerNameToInt64(playerName)))) { // server.login.sendChat(dbId,
		 * to.dbId, 3, absX, absY, // ); if (to.officialClient || to.okClient) {
		 * to.sendpm(misc.playerNameToInt64(playerName), playerRights, pmchatText,
		 * pmchatTextSize); } else { to .send(new SendMessage(
		 * "A player has tried to send you a private message"); to .send(new
		 * SendMessage(
		 * "For technical reasons, you must use the dodian client for friends list features"
		 * ); } sent = true; break; } } } } if (!sent) { send(new SendMessage(
		 * "Could not find player"); }
		 */
	}

	public boolean hasFriend(long name) {
		for (Friend f : friends) {
			if (f.name == name) {
				return true;
			}
		}
		return false;
	}

	public void refreshFriends() {
		/*for (Friend f : friends) {
			if (PlayerHandler.playersOnline.containsKey(f.name)) {
				loadpm(f.name, getGameWorldId());
			} else {
				loadpm(f.name, 0);
			}
		}*/
		for (Friend f : friends) {
			if (PlayerHandler.allOnline.containsKey(f.name)) {
				boolean ignored = false;
				for (Player p : PlayerHandler.players) {
					if (p == null)
						continue;
					if (p.longName == f.name) {
						Client player = (Client) p;
						for (Friend ignore : player.ignores) {
							ignored = ignore.name == this.longName;
						}
					}
				}
				if (!ignored)
					loadpm(f.name, 1);
				else
					loadpm(f.name, 0);
			} else {
				loadpm(f.name, 0);
			}
		}
	}

	public void removeFriend(long name) {
		for (Friend f : friends) {
			if (f.name == name) {
				friends.remove(f);
				refreshFriends();
				return;
			}
		}
	}

	public void removeIgnore(long name) {
		/*for (Friend f : ignores) {
			if (f.name == name) {
				ignores.remove(f);
				refreshFriends();
				return;
			}
		}*/
		for (Friend f : ignores) {
			if (f.name == name) {
				ignores.remove(f);
				refreshFriends();
				if (PlayerHandler.allOnline.containsKey(f.name)) {
					for (Player p : PlayerHandler.players) {
						if (p == null)
							continue;
						if (p.longName == f.name) {
							Client player = (Client) p;
							player.refreshFriends();
						}
					}
				}
				break;
			}
		}
	}

	public void addIgnore(long name) {
		boolean canAdd = true;
		for (Friend f : ignores) {
			if (f.name == name) {
				send(new SendMessage("You already got this guy on your ignoreList!"));
				canAdd = false;
				break;
			}
		}
		if (canAdd) {
			if (ignores.size() < 100) {
				ignores.add(new Friend(name, true));
				if (PlayerHandler.allOnline.containsKey(name)) {
					for (Player p : PlayerHandler.players) {
						if (p == null)
							continue;
						if (p.longName == name) {
							Client player = (Client) p;
							player.refreshFriends();
						}
					}
				}
			} else
			send(new SendMessage("Maximum ignores reached!"));
		}
	}

	public void triggerChat(int button) {
		if (!playerPotato.isEmpty())
			if (playerPotato.get(0) == 2 && playerPotato.get(3) == 1) {
				send(new RemoveInterfaces());
				Npc tempNpc = Server.npcManager.getNpc(playerPotato.get(1));
				int npcId = playerPotato.get(2);

				if (button == 1) {
					try {
						java.sql.Connection conn = getDbConnection();
						Statement statement = conn.createStatement();
						String sql = "delete from " + DbTables.GAME_NPC_SPAWNS + " where id='" + npcId + "' && x='" + tempNpc.getPosition().getX() + "' && y='" + tempNpc.getPosition().getY() + "' && height='" + tempNpc.getPosition().getZ() + "'";
						if (statement.executeUpdate(sql) < 1)
							send(new SendMessage("This npc has already been removed!"));
						else { //Functions to remove npc!
							tempNpc.die();
							EventManager.getInstance().registerEvent(new Event(tempNpc.getTimeOnFloor() + 600) {
								@Override
								public void execute() {
									Server.npcManager.getNpcs().remove(tempNpc);
									this.stop();
								}
							});
							send(new SendMessage("You removed this npc spawn!"));
						}
						statement.executeUpdate(sql);
						statement.close();
					} catch (Exception e) {
						send(new SendMessage("Something went wrong in removing this npc!"));
					}
				} else if (button == 2) {
					if (!tempNpc.getData().getDrops().isEmpty()) {
						int line = 8147;
						double totalChance = 0.0;
						clearQuestInterface();
						send(new SendString("@dre@Drops for @blu@" + tempNpc.npcName() + "@bla@(@gre@" + npcId + "@bla@)", 8144));
						ArrayList<String> text = new ArrayList<>();
						/* 100% drops! */
						for (NpcDrop drop : tempNpc.getData().getDrops()) {
							if(drop.getChance() >= 100.0)
								text.add((drop.getMinAmount() == drop.getMaxAmount() ? drop.getMinAmount() + "" : drop.getMinAmount() + " - " + drop.getMaxAmount()) + " " + GetItemName(drop.getId()) + "(" + drop.getId() + ")");
						}
						/* All other drops! */
						for (NpcDrop drop : tempNpc.getData().getDrops()) {
							if(drop.getChance() < 100.0) {
								int min = drop.getMinAmount();
								int max = drop.getMaxAmount();
								int itemId = drop.getId();
								double chance = drop.getChance();
								text.add((min == max ? min + "" : min + " - " + max) + " " + GetItemName(itemId) + "(" + itemId + ") " + chance + "% (1:"+ Math.round(100.0/chance) +")" + (drop.rareShout() ? ", YELL" : ""));
								totalChance += chance;
							}
						}
						for(String txt : text) {
							send(new SendString(txt, line));
							line++;
							if (line == 8196) line = 12174;
						}
						send(new SendString(totalChance > 100.0 ? "You are currently " + (totalChance - 100.0) + " % over!" : "Total drops %: " + totalChance + "%", 8145));
						send(new SendString(totalChance < 0.0 || totalChance >= 100.0 ? "" : "Nothing " + (100.0 - totalChance) + "%", line));
						sendQuestSomething(8143);
						showInterface(8134);
						//flushOutStream();
					} else
						send(new SendMessage("Npc " + tempNpc.npcName() + " (" + npcId + ") has no assigned drops!"));
				} else if (button == 3) {
					Server.npcManager.reloadDrops(this, npcId);
				} else if (button == 4) {
					tempNpc.showConfig(this);
				} else if (button == 5) {
					Server.npcManager.reloadAllData(this, npcId);
				}
				playerPotato.clear();
			}

		if (convoId == 0) {
			if (button == 1) {
				openUpBank();
			} else {
				nextDiag = 8;
			}
		}
		if (NpcDialogue == 12) { //Slayer dialogue
			nextDiag = button == 1 ? 13 : button == 2 ? 31 : button == 4 ? 14 : 34;
		}
		if (NpcDialogue == 32) { //Slayer dialogue
			if (button == 1)
				nextDiag = 33;
			else
				send(new RemoveInterfaces());
		}
		if (NpcDialogue == 35) { //Slayer dialogue
			if(button == 1) {
				nextDiag = 36;
			} else
				showPlayerChat(new String[]{ "No, thank you." }, 614);
		}
		if (convoId == 2) {
			if (button == 1) {
				WanneShop = 39;
			} else {
				send(new RemoveInterfaces());
			}
		}
		if (convoId == 3) {
			if (button == 1) {
				WanneShop = 9;
			} else {
				send(new RemoveInterfaces());
			}
		}
		if (convoId == 4) {
			if (button == 1) {
				WanneShop = 22;
			} else {
				send(new RemoveInterfaces());
			}
		}
		/*
		 * if (convoId == 1001) { if (button == 1) { //send(new RemoveInterfaces());
		 * getOutputStream().createFrame(27); } else { send(new RemoveInterfaces());
		 * } }
		 */
		if (NpcDialogue == 163) {
			if (button == 1)
				spendTickets();
			else
				nextDiag = 164;
		} else if (NpcDialogue == 164) {
			int type = skillX == 3002 && skillY == 3931 ? 3 : skillX == 2547 && skillY == 3554 ? 2 : 1;
			if (button == 1)
				teleportTo(type == 1 ? 2547 : 2474, type == 1 ? 3553 : 3438, 0);
			else if (button == 2)
				teleportTo(type == 3 ? 2547 : 3002, type == 3 ? 3553 : 3932, 0);
			send(new RemoveInterfaces());
		} else if (NpcDialogue == 3649) {
			if (button == 1)
				setTravelMenu();
			else if(button == 2)
				showPlayerChat(new String[]{ "No thank you." }, 614);
			NpcDialogueSend = false;
			NpcDialogue = -1;
		} else if (NpcDialogue == 2346) {
			if (button == 1) { //One time pay!
				nextDiag = 2347;
			} else if (button == 2) { //One time fee
				if(!checkUnlock(0)) {
					int maximumTickets = 10, minimumTicket = 1, ticketValue = 300_000;
					int missing = (maximumTickets - minimumTicket) * ticketValue;
					if(!playerHasItem(621, minimumTicket)) {
						showNPCChat(NpcTalkTo, 591, new String[]{"You need a minimum of " + minimumTicket + " ship ticket", "to unlock permanent!"});
						return;
					}
					missing -= (getInvAmt(621) - minimumTicket) * ticketValue;
					if(missing > 0) {
						if(getInvAmt(995) >= missing) {
							deleteItem(621, getInvAmt(621) < maximumTickets ? getInvAmt(621) : maximumTickets);
							deleteItem(995, missing);
							addUnlocks(0, checkUnlockPaid(0) + "", "1");
							showNPCChat(NpcTalkTo, 591, new String[]{"Thank you for the payment.", "You may enter freely into my dungeon."});
						} else showNPCChat(NpcTalkTo, 591, new String[]{"You do not have enough coins to do this!", "You are currently missing "+(missing - getInvAmt(995))+" coins or", (int)Math.ceil((missing - getInvAmt(995))/300_000D) + " ship tickets."});
					} else { //Using ship tickets as payment!
						deleteItem(621, maximumTickets);
						addUnlocks(0, checkUnlockPaid(0) + "", "1");
						showNPCChat(NpcTalkTo, 591, new String[]{"Thank you for the ship tickets.", "You may enter freely into my dungeon."});
					}
				}
			} else if (button == 3) { //Nevermind!
				showPlayerChat(new String[]{"I do not want anything."}, 614);
			} else
				send(new RemoveInterfaces());
		} else if (NpcDialogue == 2347) {
			if(checkUnlockPaid(0) > 0 || checkUnlock(0)) {
				showNPCChat(NpcTalkTo, 591, new String[]{"You have already paid me.", "Please step into my dungeon."});
			} else if(button == 1) {
				if(getInvAmt(621) > 0 || getBankAmt(621) > 0) {
					addUnlocks(0, "1", checkUnlock(0) ? "1" : "0");
					if(getInvAmt(621) > 0)
						deleteItem(621, 1);
					else deleteItemBank(621, 1);
					showNPCChat(NpcTalkTo, 591, new String[]{"You can now step into the dungeon."});
				} else showNPCChat(NpcTalkTo, 596, new String[]{"You need a ship ticket to enter my dungeon!"});
			} else if (button == 2) {
				long amount = getInvAmt(995) + getBankAmt(995);
				int total = 300_000;
				if(amount >= total) {
					addUnlocks(0, "1", checkUnlock(0) ? "1" : "0");
					int remain = total - getInvAmt(995);
					deleteItem(995, total);
					if(remain > 0)
						deleteItemBank(995, remain);
					showNPCChat(NpcTalkTo, 591, new String[]{"You can now step into the dungeon."});
				} else showNPCChat(NpcTalkTo, 596, new String[]{"You need atleast "+(300_000 - amount)+" more coins to enter my dungeon!"});
			}
		} else if (NpcDialogue == 2181) {
			if (button == 1) { //One time pay!
				nextDiag = 2182;
			} else if (button == 2) { //One time fee
				if(!checkUnlock(1)) {
					int maximumTickets = 20, minimumTicket = 3, ticketValue = 300_000;
					int missing = (maximumTickets - minimumTicket) * ticketValue;
					if(!playerHasItem(621, minimumTicket)) {
						showNPCChat(NpcTalkTo, 591, new String[]{"You need a minimum of " + minimumTicket + " ship ticket", "to unlock permanent!"});
						return;
					}
					missing -= (getInvAmt(621) - minimumTicket) * ticketValue;
					if(missing > 0) {
						if(getInvAmt(995) >= missing) {
							deleteItem(621, getInvAmt(621) < maximumTickets ? getInvAmt(621) : maximumTickets);
							deleteItem(995, missing);
							addUnlocks(1, checkUnlockPaid(1) + "", "1");
							showNPCChat(NpcTalkTo, 591, new String[]{"Thank you for the payment.", "You may enter freely into my cave."});
						} else showNPCChat(NpcTalkTo, 591, new String[]{"You do not have enough coins to do this!", "You are currently missing "+(missing - getInvAmt(995))+" coins or", (int)Math.ceil((missing - getInvAmt(995))/300_000D) + " ship tickets."});
					} else { //Using ship tickets as payment!
						deleteItem(621, maximumTickets);
						addUnlocks(1, checkUnlockPaid(1) + "", "1");
						showNPCChat(NpcTalkTo, 591, new String[]{"Thank you for the ship tickets.", "You may enter freely into my cave."});
					}
				}
			} else if (button == 3) { //Nevermind!
				showPlayerChat(new String[]{"I do not want anything."}, 614);
			} else
				send(new RemoveInterfaces());
		} else if (NpcDialogue == 2182) {
			if(checkUnlockPaid(1) > 0 || checkUnlock(1)) {
				showNPCChat(NpcTalkTo, 591, new String[]{"You have already paid me.", "Please step into my cave."});
			} else if(button == 1) {
				if(getInvAmt(621) > 0 || getBankAmt(621) > 0) {
					addUnlocks(1, "1", checkUnlock(1) ? "1" : "0");
					if(getInvAmt(621) > 0)
						deleteItem(621, 1);
					else deleteItemBank(621, 1);
					showNPCChat(NpcTalkTo, 591, new String[]{"You can now step into the cave."});
				} else showNPCChat(NpcTalkTo, 596, new String[]{"You need a ship ticket to enter my cave!"});
			} else if (button == 2) {
				long amount = getInvAmt(995) + getBankAmt(995);
				int total = 300_000;
				if(amount >= total) {
					addUnlocks(1, "1", checkUnlock(1) ? "1" : "0");
					int remain = total - getInvAmt(995);
					deleteItem(995, total);
					if(remain > 0)
						deleteItemBank(995, remain);
					showNPCChat(NpcTalkTo, 591, new String[]{"You can now step into the cave."});
				} else showNPCChat(NpcTalkTo, 596, new String[]{"You need atleast "+(300_000 - amount)+" more coins to enter my cave!"});
			}
		} else if (NpcDialogue == 6483) {
			if(button == 1)
				nextDiag = NpcDialogue + 1;
			else if(button == 2)
				showPlayerChat(new String[]{ "No thank you." }, 614);
		} else if (NpcDialogue == 8053) {
			if (button == 1) {
				send(new RemoveInterfaces());
				openUpShop(55);
				//TODO: Add reward shop
			} else
				send(new RemoveInterfaces());
		} else if (NpcDialogue == 10000) {
			if (button == 1) {
				if(playerHasItem(6161) && playerHasItem(6159)) {
					deleteItem(6159, 1);
					deleteItem(6161, 1);
					addItem(6128, 1);
					showPlayerChat(new String[]{ "I just made Rock-shell head." }, 614);
				} else
					showPlayerChat(new String[]{"I need the following items:", GetItemName(6161) + " and " + GetItemName(6159)}, 614);
			} else if (button == 2) {
				if(playerHasItem(6157) && playerHasItem(6159) && playerHasItem(6161)) {
					deleteItem(6157, 1);
					deleteItem(6159, 1);
					deleteItem(6161, 1);
					addItem(6129, 1);
					showPlayerChat(new String[]{ "I just made Rock-shell body." }, 614);
				} else
					showPlayerChat(new String[]{"I need the following items:", GetItemName(6161) + ", " + GetItemName(6159) + " and " + GetItemName(6157)}, 614);
			} else if (button == 3) {
				if(playerHasItem(6159) && playerHasItem(6157)) {
					deleteItem(6157, 1);
					deleteItem(6159, 1);
					addItem(6130, 1);
					showPlayerChat(new String[]{ "I just made Rock-shell legs." }, 614);
				} else
					showPlayerChat(new String[]{"I need the following items:", GetItemName(6159) + " and " + GetItemName(6157)}, 614);
			} else if (button == 4) {
				if (playerHasItem(6161) && playerHasItem(6159)) {
					deleteItem(6159, 1);
					deleteItem(6161, 1);
					addItem(6145, 1);
					showPlayerChat(new String[]{"I just made Rock-shell boots."}, 614);
				} else
					showPlayerChat(new String[]{"I need the following items:", GetItemName(6161) + " and " + GetItemName(6159)}, 614);
			} else if (button == 5) {
				if (playerHasItem(6161, 2)) {
					deleteItem(6161, 1);
					deleteItem(6161, 1);
					addItem(6151, 1);
					showPlayerChat(new String[]{"I just made Rock-shell gloves."}, 614);
				} else
					showPlayerChat(new String[]{"I need two of " + GetItemName(6161)}, 614);
			}
			NpcDialogueSend = true;
		} else if (NpcDialogue == 536) {
			if (button == 1) {
				long amount = getInvAmt(536) + getInvAmt(537) + getBankAmt(536);
				int amt = 5;
				if (amount >= 5) {
					while (amt > 0) {
						for (int slot = 0; slot < 28 && amt > 0; slot++) {
							if (playerItems[slot] - 1 == 536) {
								deleteItem(536, slot, 1);
								amt--;
							}
						}
						for (int slot = 0; slot < 28; slot++) {
							if (playerItems[slot] - 1 == 537) {
								int toDelete = playerItemsN[slot] >= amt ? amt : playerItemsN[slot];
								deleteItem(537, slot, toDelete);
								amt -= toDelete;
								break;
							}
						}
						for (int slot = 0; slot < bankItems.length; slot++) {
							if (bankItems[slot] - 1 == 536) {
								bankItemsN[slot] -= amt;
								break;
							}
						}
						amt = 0;
					}
					Agility agi = new Agility(this);
					agi.kbdEntrance();
					send(new SendMessage("You sacrifice 5 dragon bones!"));
				} else
					send(new SendMessage("You need to have 5 dragon bones to sacrifice!"));
				send(new RemoveInterfaces());
			} else
				send(new RemoveInterfaces());
		} else if (NpcDialogue == 48054) {
			if(getInvAmt(621) < 1) {
				send(new SendMessage("You need a ship ticket to unlock this travel!"));
			} else if (button == 1) {
				int id = actionButtonId == 48054 ? 4 : actionButtonId == 3056 ? 3 : actionButtonId - 3058;
				if(!getTravel(id)) {
					deleteItem(621, 1);
					saveTravel(id);
					send(new SendMessage("You have now unlocked the travel!"));
				} else
					send(new SendMessage("You have already unlocked this travel!"));
			}
			NpcDialogueSend = false;
			NpcDialogue = -1;
			setTravelMenu();
		}
		if (nextDiag > 0) {
			NpcDialogue = nextDiag;
			NpcDialogueSend = false;
			nextDiag = -1;
		}
	}

	public boolean smithCheck(int id) {
		for (int i = 0; i < Constants.smithing_frame.length; i++) {
			for (int i1 = 0; i1 < Constants.smithing_frame[i].length; i1++) {
				if (id == Constants.smithing_frame[i][i1][0]) {
					return true;
				}
			}
		}
		send(new SendMessage("Client hack detected!"));
		return false;
	}

	public int findPick() {
		int Eaxe = -1, Iaxe = -1;
		int weapon = getEquipment()[Equipment.Slot.WEAPON.getId()];
		for (int i = 0; i < Utils.picks.length; i++) {
			if (Utils.picks[i] == weapon) {
				if (getLevel(Skill.MINING) >= Utils.pickReq[i])
					Eaxe = i;
			}
			for (int ii = 0; ii < playerItems.length; ii++) {
				if (Utils.picks[i] == playerItems[ii] - 1) {
					if (getLevel(Skill.MINING) >= Utils.pickReq[i]) {
						Iaxe = i;
					}
				}
			}
		}
		return Eaxe > Iaxe ? Eaxe : Iaxe > Eaxe ? Iaxe : -1;
	}

	public int findAxe() {
		int Eaxe = -1;
		int Iaxe = -1;
		int weapon = getEquipment()[Equipment.Slot.WEAPON.getId()];
		for (int i = 0; i < Utils.axes.length; i++) {
			if (Utils.axes[i] == weapon) {
				if (getLevel(Skill.WOODCUTTING) >= Utils.axeReq[i])
					Eaxe = i;
			}
			for (int ii = 0; ii < playerItems.length; ii++) {
				if (Utils.axes[i] == playerItems[ii] - 1) {
					if (getLevel(Skill.WOODCUTTING) >= Utils.axeReq[i]) {
						Iaxe = i;
					}
				}
			}
		}
		if (Eaxe >= Iaxe)
			return Eaxe;
		if (Iaxe >= Eaxe)
			return Iaxe;
		return -1;
	}

	public void mining(int index) {
		int pickaxe = -1;
		pickaxe = findPick();
		if (pickaxe < 0) {
			minePick = -1;
			resetAction();
			send(new SendMessage("You do not have an pickaxe that you can use."));
			return;
		} else
			minePick = pickaxe;
		if (minePick >= 0) {
			requestAnim(getMiningEmote(Utils.picks[pickaxe]), 0);
		} else {
			resetAction();
			send(new SendMessage("You need a pickaxe to mine this rock"));
		}
		if (!playerHasItem(-1)) {
			send(new SendMessage("Your inventory is full!"));
			resetAction(true);
			return;
		}
		if (index != 6) {
			send(new SendMessage("You mine some " + GetItemName(Utils.ore[index]).toLowerCase() + ""));
		}
		addItem(Utils.ore[index], 1);
		giveExperience(Utils.oreExp[index], Skill.MINING);
		requestAnim(getMiningEmote(Utils.picks[pickaxe]), 0);
		triggerRandom(Utils.oreExp[index]);
		if (Misc.chance(30) == 1) {
			send(new SendMessage("You take a rest"));
			resetAction(true);
		}
	}

	public void callGfxMask(int id, int height) {
		setGraphic(id, height == 0 ? 65536 : 65536 * height);
		getUpdateFlags().setRequired(UpdateFlag.GRAPHICS, true);
	}

	public void AddToCords(int X, int Y) {
		newWalkCmdSteps = Math.abs(X + Y);
		if (newWalkCmdSteps % 2 != 0 && newWalkCmdSteps > 1) {
			newWalkCmdSteps /= 2;
		}
		if (++newWalkCmdSteps > 50) {
			newWalkCmdSteps = 0;
		}
		int l = getPosition().getX();
		l -= mapRegionX * 8;
		for (i = 1; i < newWalkCmdSteps; i++) {
			newWalkCmdX[i] = X;
			newWalkCmdY[i] = Y;
			tmpNWCX[i] = newWalkCmdX[i];
			tmpNWCY[i] = newWalkCmdY[i];
		}
		newWalkCmdX[0] = newWalkCmdY[0] = tmpNWCX[0] = tmpNWCY[0] = 0;
		int j1 = getPosition().getY();
		j1 -= mapRegionY * 8;
		newWalkCmdIsRunning = false;
		for (i = 0; i < newWalkCmdSteps; i++) {
			newWalkCmdX[i] += l;
			newWalkCmdY[i] += j1;
		}
	}

	public void AddToCords(int X, int Y, long time) {
		walkBlock = System.currentTimeMillis() + time;
		AddToCords(X, Y);
	}

	public void startAttack(Entity enemy) {
		target = enemy;
		if(target instanceof Npc) {
			attackingNpc = true;
			faceNpc(target.getSlot());
			//System.out.println("test for npc slot: " + target.getSlot());
		} else {
			Server.playerHandler.getClient(target.getSlot()).target = this;
			attackingPlayer = true;
			facePlayer(target.getSlot());
			//System.out.println("test for player slot: " + target.getSlot());
		}
	}

	public void resetAttack() {
		rerequestAnim();
		if (target instanceof Npc)
			attackingNpc = false;
		else
			attackingPlayer = false;
		target = null;
		faceTarget(65535);
	}

	private void requestAnims(int wearID) {

		setStandAnim(Server.itemManager.getStandAnim(getEquipment()[Equipment.Slot.WEAPON.getId()]));
		setWalkAnim(Server.itemManager.getWalkAnim(getEquipment()[Equipment.Slot.WEAPON.getId()]));
		setRunAnim(Server.itemManager.getRunAnim(getEquipment()[Equipment.Slot.WEAPON.getId()]));
	}

	public int getWildLevel() {
		int lvl = 0;
		if (getPosition().getY() >= 3524 && getPosition().getY() < 3904 && getPosition().getX() >= 2954
				&& getPosition().getX() <= 3327)
			lvl = (((getPosition().getY() - 3520) / 8)) + 1;
		return lvl;
	}

	public void setWildLevel(int level) {
		wildyLevel = level;
		getOutputStream().createFrame(208);
		getOutputStream().writeWordBigEndian_dup(197);
		send(new SendString("Level: " + wildyLevel, 199));
	}

	public void updatePlayerDisplay() {
		String serverName = getGameWorldId() == 1 ? "Uber Server 3.0" : "Beta World";
		send(new SendString(serverName + " (" + PlayerHandler.getPlayerCount() + " online)", 6570));
		send(new SendString("", 6664));
		setInterfaceWalkable(6673);
	}

	public void playerKilled(Client other) {
		other.setSkullIcon(1);
		other.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
	}

	public void setSnared(int time) {
		snaredUntil = System.currentTimeMillis() + time;
		stillgfx(617, getPosition().getX(), getPosition().getY());
		send(new SendMessage("You have been snared!"));
		resetWalkingQueue();
	}

	public void died() {
		int highestDmg = 0, slot = -1;
		for (Entity e : getDamage().keySet()) {
			if (getDamage().get(e) > highestDmg) {
				highestDmg = getDamage().get(e);
				slot = e.getSlot();
			}
		}
		getDamage().clear();
		if(slot >= 0) {
			Ground.items.add(new GroundItem(getPosition().copy(), 526, 1, slot, -1));
			if (validClient(slot)) {
				getClient(slot).send(new SendMessage("You have defeated " + getPlayerName() + "!"));
				yellKilled(getClient(slot).getPlayerName() + " has just slain " + getPlayerName() + " in the wild!");
				Client other = getClient(slot);
				playerKilled(other);
			}
		}
		/* Stuff dropped to the floor! */
      /*for (int i = 0; i < getEquipment().length; i++) {
        if (getEquipment()[i] > 0) {
          if (Server.itemManager.isTradable(getEquipment()[i]))
            Ground.items.add(new GroundItem(getPosition().getX(), getPosition().getY(), getEquipment()[i],
                    getEquipmentN()[i], slot, -1));
          else
            Ground.items.add(new GroundItem(getPosition().getX(), getPosition().getY(), getEquipment()[i],
                    getEquipmentN()[i], getSlot(), -1));
        }
        getEquipment()[i] = -1;
        getEquipmentN()[i] = 0;
        deleteequiment(0, i);
      }
      for (int i = 0; i < playerItems.length; i++) {
        if (playerItems[i] > 0) {
          if (Server.itemManager.isTradable((playerItems[i] - 1)))
            Ground.items.add(new GroundItem(getPosition().getX(), getPosition().getY(), (playerItems[i] - 1),
                    playerItemsN[i], slot, -1));
          else
            Ground.items.add(new GroundItem(getPosition().getX(), getPosition().getY(), (playerItems[i] - 1),
                    playerItemsN[i], getSlot(), -1));
        }
        deleteItem((playerItems[i] - 1), i, playerItemsN[i]);
      }*/
	}

	public void acceptDuelWon() {
		if (duelFight && duelWin) {
			duelWin = false;
			if (System.currentTimeMillis() - lastButton < 1000) {
				lastButton = System.currentTimeMillis();
				return;
			} else {
				lastButton = System.currentTimeMillis();
			}
			Client other = getClient(duel_with);
			CopyOnWriteArrayList<GameItem> offerCopy = new CopyOnWriteArrayList<GameItem>();
			CopyOnWriteArrayList<GameItem> otherOfferCopy = new CopyOnWriteArrayList<GameItem>();
			for (GameItem item : otherOfferedItems) {
				otherOfferCopy.add(new GameItem(item.getId(), item.getAmount()));
			}
			for (GameItem item : offeredItems) {
				offerCopy.add(new GameItem(item.getId(), item.getAmount()));
			}
			for (GameItem item : otherOfferedItems) {
				if (item.getId() > 0 && item.getAmount() > 0) {
					if (Server.itemManager.isStackable(item.getId())) {
						addItem(item.getId(), item.getAmount());
					} else {
						addItem(item.getId(), 1);
					}
				}
			}
			for (GameItem item : offeredItems) {
				if (item.getId() > 0 && item.getAmount() > 0) {
					addItem(item.getId(), item.getAmount());
				}
			}
			if (this.dbId > other.dbId)
				TradeLog.recordTrade(dbId, otherdbId, offerCopy, otherOfferCopy, false);
			resetDuel();
			GetBonus(true);
			saveStats(false);
			if (validClient(duel_with)) {
				other.resetDuel();
				GetBonus(true);
				other.saveStats(false);
			}
		}
	}

	public boolean contains(int item) {
		for (int i = 0; i < playerItems.length; i++) {
			if (playerItems[i] == item + 1)
				return true;
		}
		return false;
	}

	public void setConfigIds() {
		stakeConfigId[0] = 16384; // No head armour
		stakeConfigId[1] = 32768; // No capes
		stakeConfigId[2] = 65536; // No amulets
		stakeConfigId[3] = 134217728; // No arrows
		stakeConfigId[4] = 131072; // No weapon
		stakeConfigId[5] = 262144; // No body armour
		stakeConfigId[6] = 524288; // No shield
		stakeConfigId[7] = 2097152; // No leg armour
		stakeConfigId[8] = 67108864; // No hand armour
		stakeConfigId[9] = 16777216; // No feet armour
		stakeConfigId[10] = 8388608; // No rings
		stakeConfigId[11] = 16; // No ranging
		stakeConfigId[12] = 32; // No melee
		stakeConfigId[13] = 64; // No magic
		stakeConfigId[14] = 8192; // no gear change
		stakeConfigId[15] = 4096; // fun weapons
		stakeConfigId[16] = 1; // no retreat
		stakeConfigId[17] = 128; // No drinks
		stakeConfigId[18] = 256; // No food
		stakeConfigId[19] = 512; // No prayer
		stakeConfigId[20] = 2; // movement
		stakeConfigId[21] = 1024; // obstacles
		stakeConfigId[22] = -1; // No specials
	}

	/**
	 * Shows armour in the duel screen slots! (hopefully lol)
	 */
	public void sendArmour() {
		for (int e = 0; e < getEquipment().length; e++) {
			getOutputStream().createFrameVarSizeWord(34);
			getOutputStream().writeWord(13824);
			getOutputStream().writeByte(e);
			getOutputStream().writeWord(getEquipment()[e] + 1);
			if (getEquipmentN()[e] > 254) {
				getOutputStream().writeByte(255);
				getOutputStream().writeDWord(getEquipmentN()[e]);
			} else {
				getOutputStream().writeByte(getEquipmentN()[e]); // amount
			}
			getOutputStream().endFrameVarSizeWord();
		}
	}

	public boolean hasTradeSpace() {
		if (!validClient(trade_reqId)) {
			return false;
		}
		Client o = getClient(trade_reqId);
		int spaces = 0;
		ArrayList<GameItem> items = new ArrayList<GameItem>();
		for (GameItem item : o.offeredItems) {
			if (item == null)
				continue;
			if (item.getAmount() > 0) {
				if (!items.contains(item)) {
					items.add(item);
					spaces += 1;
				} else {
					if (!item.isStackable()) {
						spaces += 1;
					}
				}
			}
		}
		if (spaces > getFreeSpace()) {
			failer = getPlayerName() + " does not have enough space to hold items being traded.";
			o.failer = getPlayerName() + " does not have enough space to hold items being traded.";
			return false;
		}
		return true;
	}

	/**
	 * @return if player has enough space to remove items.
	 */
	public boolean hasEnoughSpace() {
		if (!inDuel || !validClient(duel_with)) {
			return false;
		}
		Client o = getClient(duel_with);
		int spaces = 0;
		for (int i = 0; i < duelBodyRules.length; i++) {
			if (!duelBodyRules[i])
				continue;
			if (getEquipmentN()[trueSlots[i]] > 0) {
				spaces += 1;
			}
		}
		ArrayList<GameItem> items = new ArrayList<GameItem>();
		for (GameItem item : offeredItems) {
			if (item == null)
				continue;
			if (item.getAmount() > 0) {
				if (!items.contains(item)) {
					items.add(item);
					spaces += 1;
				} else {
					if (!item.isStackable()) {
						spaces += 1;
					}
				}
			}
		}
		for (GameItem item : o.offeredItems) {
			if (item == null)
				continue;
			if (item.getAmount() > 0) {
				if (!items.contains(item)) {
					items.add(item);
					spaces += 1;
				} else {
					if (!Server.itemManager.isStackable(item.getId())) {
						spaces += 1;
					}
				}
			}
		}
		if (spaces > getFreeSpace()) {
			failer = getPlayerName() + " does not have enough space to hold items being removed and/or staked.";
			o.failer = getPlayerName() + " does not have enough space to hold items being removed and/or staked.";
			return false;
		}
		return true;

	}

	public void removeEquipment() {
		for (int i = 0; i < duelBodyRules.length; i++) {
			if (!duelBodyRules[i])
				continue;
			if (getEquipmentN()[trueSlots[i]] > 0) {
				int id = getEquipment()[trueSlots[i]];
				int amount = getEquipmentN()[trueSlots[i]];
				if(remove(trueSlots[i], true))
					addItem(id, amount);
			}
		}
	}

	public void requestForceChat(String s) {
		forcedChat = s;
		getUpdateFlags().setRequired(UpdateFlag.FORCED_CHAT, true);
	}

	/**
	 * @param skillX the skillX to set
	 */
	public void setSkillX(int skillX) {
		this.skillX = skillX;
	}

	/**
	 * @param skillY the skillY to set
	 */
	public void setSkillY(int skillY) {
		this.skillY = skillY;
		if (WanneBank > 0)
			WanneBank = 0;
		if (NpcWanneTalk > 0)
			NpcWanneTalk = 0;
	}

	public void spendTickets() {
		send(new RemoveInterfaces());
		int slot = -1;
		for (int s = 0; s < playerItems.length; s++) {
			if ((playerItems[s] - 1) == 2996) {
				slot = s;
				break;
			}
		}
		if (slot == -1) {
			send(new SendMessage("You have no agility tickets!"));
		} else if (playerItemsN[slot] < 10) {
			send(new SendMessage("You must hand in at least 10 tickets at once"));
		} else {
			int amount = playerItemsN[slot];
			giveExperience(amount * 700, Skill.AGILITY);
			send(new SendMessage("You exchange your " + amount + " agility tickets"));
			deleteItem(2996, playerItemsN[slot]);
		}
	}

	public long potTime = 0;
	public boolean mixPots = false;
	private int mixPotAmt = 0, mixPotId1 = 0, mixPotId2 = 0, mixPotId3 = 0, mixPotXp = 0;

	public void setPots(long time, int id1, int id2, int id3, int xp) {
		mixPotAmt = 14;
		potTime = time;
		mixPotId1 = id1;
		mixPotId2 = id2;
		mixPotId3 = id3;
		mixPotXp = xp;
		mixPots = true;
	}

	public void mixPots() {
		if (mixPotAmt < 1) {
			resetAction(true);
			return;
		}
		if (!playerHasItem(mixPotId1, 1) || !playerHasItem(mixPotId2, 1)) {
			resetAction(true);
			return;
		}
		mixPotAmt--;
		IsBanking = false;
		this.requestAnim(363, 0);
		getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
		deleteItem(mixPotId1, 1);
		deleteItem(mixPotId2, 1);
		addItem(mixPotId3, 1);
		giveExperience(mixPotXp, Skill.HERBLORE);
		triggerRandom(mixPotXp);
	}

	public void guideBook() {
		send(new SendMessage("this is me a guide book!"));
		clearQuestInterface();
		showInterface(8134);
		send(new SendString("Newcomer's Guide", 8144));
		send(new SendString("---------------------------", 8145));
		send(new SendString("Welcome to Dodian.net!", 8147));
		send(new SendString("This guide is to help new players to get a general", 8148));
		send(new SendString("understanding of how Dodian works!", 8149));
		send(new SendString("", 8150));
		send(new SendString("For specific boss or skill locations", 8151));
		send(new SendString("navigate to the 'Guides' section of the forums.", 8152));
		send(new SendString("", 8153));
		send(new SendString("Here in Yanille, there are various enemies to kill,", 8154));
		send(new SendString("with armor rewards that get better the higher their level.", 8155));
		send(new SendString("", 8156));
		send(new SendString("From Yanille, you can also head North-East to access", 8157));
		send(new SendString("the mining area or South-West", 8158));
		send(new SendString("up the stairs in the magic guild to access the essence mine.", 8159));
		send(new SendString("", 8160));
		send(new SendString("If you navigate over to your spellbook, you will see", 8161));
		send(new SendString("some teleports, these all lead to key points on the server", 8162));
		send(new SendString("", 8163));
		send(new SendString("Seers, Catherby, Fishing Guild, and Gnome Stronghold", 8164));
		send(new SendString("teleports will all bring you to skilling locations.", 8165));
		send(new SendString("", 8166));
		send(new SendString("Legends Guild, and Taverly teleports", 8167));
		send(new SendString("will all bring you to locations with more monsters to train on.", 8168));
		send(new SendString("", 8169));
		send(new SendString("Teleporting to Taverly and heading up the path", 8170));
		send(new SendString("is how you access the Slayer Master!", 8171));
		send(new SendString("", 8172));
		send(new SendString("If you have more questions please visit the 'Guides'", 8173));
		send(new SendString("section of the forums, and if you still can't find the answer.", 8174));
		send(new SendString("Feel free to just ask a moderator!", 8175));
		send(new SendString("---------------------------", 8176));
	}

	private long lastVoted;

	public Prayers getPrayerManager() {
		return prayers;
	}

	private int getItem(Player p, int i, int i2) {
		if (!playerHasItem(items[i2]) && items[i2] != -1) {
			return blanks[i][i2];
		}
		return jewelry[i][i2];
	}

	public int[][] blanks = {{-1, 1649, 1650, 1651, 1652, 1653, 6564}, {-1, 1668, 1669, 1670, 1671, 1672, 6565},
			{-1, 1687, 1688, 1689, 1690, 1691, 6566},};

	public int[] startSlots = {4233, 4239, 4245, 79};
	public int[] items = {-1, 1607, 1605, 1603, 1601, 1615, 6573};
	public int[] black = {1647, 1666, 1685, 11067};
	public int[] sizes = {120, 100, 75, 11067};

	public int[] moulds = {1592, 1597, 1595, 11065};

	public int findStrungAmulet(int amulet) {
		for (int i = 0; i < strungAmulets.length; i++) {
			if (jewelry[2][i] == amulet) {
				return strungAmulets[i];
			}
		}
		return -1;
	}

	public int[] strungAmulets = {1692, 1694, 1696, 1698, 1700, 1702, 6581};

	private int[][] jewelry = {{1635, 1637, 1639, 1641, 1643, 1645, 6575}, {1654, 1656, 1658, 1660, 1662, 1664, 6577},
			{1673, 1675, 1677, 1679, 1681, 1683, 6579}, {11069, 11072, 11076, 11085, 11092, 11115, 11130}};

	private int[][] jewelry_levels = {{5, 20, 27, 34, 43, 55, 67}, {6, 22, 29, 40, 56, 72, 82},
			{8, 23, 31, 50, 70, 80, 90}, {7, 24, 30, 42, 58, 74, 84}};

	private int[][] jewelry_xp = {{15, 40, 55, 70, 85, 100, 115}, {20, 55, 60, 75, 90, 105, 120},
			{30, 65, 70, 85, 100, 150, 165}, {25, 60, 65, 80, 95, 110, 125}};

	public void showItemsGold() {
		int slot = 0;
		for (int i = 0; i < 3; i++) {
			slot = startSlots[i];
			if (!playerHasItem(moulds[i])) {
				changeInterfaceStatus(startSlots[i] - 5, true);
				changeInterfaceStatus(startSlots[i] - 1, false);
				continue;
			} else {
				changeInterfaceStatus(startSlots[i] - 5, false);
				changeInterfaceStatus(startSlots[i] - 1, true);
			}
			int[] itemsToShow = new int[7];
			for (int i2 = 0; i2 < 7; i2++) {
				itemsToShow[i2] = getItem(this, i, i2);
				if (i2 != 0 && itemsToShow[i2] != jewelry[i][i2])
					if (i2 < 7)
						sendFrame246(slot + 13 + i2 - 1 - i, sizes[i], black[i]);
					else
						sendFrame246(slot + 1788 - (i * 5), sizes[i], black[i]);
				else if (i2 != 0) {
					if (i2 < 7)
						sendFrame246(slot + 13 + i2 - 1 - i, sizes[i], -1);
					else
						sendFrame246(slot + 1788 - (i * 5), sizes[i], -1);
				}
			}
			setGoldItems(slot, itemsToShow);
		}
	}

	public void setGoldItems(int slot, int[] items) {
		getOutputStream().createFrameVarSizeWord(53);
		getOutputStream().writeWord(slot);
		getOutputStream().writeWord(items.length);

		for (int i = 0; i < items.length; i++) {
			getOutputStream().writeByte((byte) 1);
			getOutputStream().writeWordBigEndianA(items[i] + 1);
		}
		getOutputStream().endFrameVarSizeWord();
	}

	public int goldIndex = -1, goldSlot = -1;
	public int goldCraftingCount = 0;
	public boolean goldCrafting = false;

	public void goldCraft() {
		// int gem = gemReq[goldSlot];
		int level = jewelry_levels[goldIndex][goldSlot];
		int amount = goldCraftingCount;
		int item = jewelry[goldIndex][goldSlot];
		int xp = jewelry_xp[goldIndex][goldSlot];
		if (goldIndex == -1 || goldSlot == -1) {
			goldCrafting = false;
			resetAction();
			return;
		}
		if (amount <= 0) {
			goldCrafting = false;
			resetAction();
			return;
		}
		if (level > getLevel(Skill.CRAFTING)) {
			send(new SendMessage("You need a crafting level of " + level + " to make this."));
			goldCrafting = false;
			return;
		}
		if (!playerHasItem(2357)) {
			goldCrafting = false;
			send(new SendMessage("You need at least one gold bar."));
			return;
		}
		if (goldSlot != 0 && !playerHasItem(items[goldSlot])) {
			goldCrafting = false;
			send(new SendMessage("You need a " + GetItemName(items[goldSlot]).toLowerCase() + " to make this."));
			return;
		}
		goldCraftingCount--;
		if (goldCraftingCount <= 0) {
			goldCrafting = false;
		}
		requestAnim(0x383, 0);
		deleteItem(2357, 1);
		if (goldSlot != 0)
			deleteItem(items[goldSlot], 1);
		send(new SendMessage("You craft a " + GetItemName(item).toLowerCase() + ""));
		addItem(item, 1);
		giveExperience(xp * 10, Skill.CRAFTING);
		triggerRandom(xp * 10);
	}

	public void startGoldCrafting(int interfaceID, int slot, int amount) {
		int index = 0;
		int[] inters = {4233, 4239, 4245};
		for (int i = 0; i < 3; i++)
			if (inters[i] == interfaceID)
				index = i;
		int level = jewelry_levels[index][slot];
		if (level > getLevel(Skill.CRAFTING)) {
			send(new SendMessage("You need a crafting level of " + level + " to make this."));
			return;
		}
		if (!playerHasItem(2357)) {
			send(new SendMessage("You need at least one gold bar."));
			return;
		}
		if (slot != 0 && !playerHasItem(items[slot])) {
			send(new SendMessage("You need a " + GetItemName(items[slot]).toLowerCase() + " to make this."));
			return;
		}
		goldCraftingCount = amount;
		goldIndex = index;
		goldSlot = slot;
		goldCrafting = true;
		send(new RemoveInterfaces());
	}

	public void deleteRunes(int[] runes, int[] qty) {
		for (int i = 0; i < runes.length; i++) {
			deleteItem(runes[i], qty[i]);
		}
	}

	public boolean hasRunes(int[] runes, int[] qty) {
		for (int i = 0; i < runes.length; i++) {
			if (!playerHasItem(runes[i], qty[i])) {
				return false;
			}
		}
		return true;
	}

	public void checkBow() {
		int weaponId = getEquipment()[Equipment.Slot.WEAPON.getId()];
		usingBow = bowWeapon(weaponId);
	}

	public boolean bowWeapon(int weaponId) {
		boolean bow = false;
		for (int i = 0; i < Constants.shortbow.length && !bow; i++)
			if (weaponId == Constants.shortbow[i] || weaponId == Constants.longbow[i])
				bow = true;
		if (weaponId == 4212 || weaponId == 6724 || weaponId == 20997 ||
				weaponId == 11235 || (weaponId >= 12765 && weaponId <= 12768))
			bow = true;
		return bow;
	}

	public void RottenTomato(final Client c) {
		for (int i = 0; i < PlayerHandler.players.length; i++) {
			Client o = (Client) PlayerHandler.players[i];
			final int oX = c.getPosition().getX();
			final int oY = c.getPosition().getY();
			final int pX = o.getPosition().getX();
			final int pY = o.getPosition().getY();
			final int offX = (oY - pY) * -1;
			final int offY = (oX - pX) * -1;
			//createProjectile(oX, oY, offX, offY, 50, 90, 1281, 21, 21, 2518 - 1);
			sendAnimation(2968);
			//c.turnPlayerTo(pX, pY);
			EventManager.getInstance().registerEvent(new Event(600) {

				@Override
				public void execute() {
					//if (c == null || c.disconnected) {
					o.gfx0(1282);
					this.stop();
					// }
				}
			});
			if (playerHasItem(2518, 1)) {
				deleteItem(2518, 1);
			} else {
				deleteItem(2518, Equipment.Slot.WEAPON.getId());
				deleteItem(2518, 1);
			}
		}
	}

	public boolean checkInv = false;

	public void openUpOtherInventory(String player) {
		if (IsBanking || IsShopping || duelFight) {
			send(new SendMessage("Please finish with what you are doing!"));
			return;
		}
		ArrayList<GameItem> otherInv = new ArrayList<GameItem>();
		if (PlayerHandler.getPlayer(player) != null) { //Online check
			Client other = (Client) PlayerHandler.getPlayer(player);
			for (int i = 0; i < other.playerItems.length; i++) {
				otherInv.add(i, new GameItem(other.playerItems[i] - 1, other.playerItemsN[i]));
			}
			sendInventory(3214, otherInv);
			send(new SendMessage("User " + player + "'s inventory is now being shown."));
			checkInv = true;
		} else {
			try {
				java.sql.Connection conn = getDbConnection();
				Statement statement = conn.createStatement();
				String query = "SELECT * FROM " + DbTables.WEB_USERS_TABLE + " WHERE username = '" + player + "'";
				ResultSet results = statement.executeQuery(query);
				int id = -1;
				if (results.next())
					id = results.getInt("userid");
				if (id >= 0) {
					query = "SELECT * FROM " + DbTables.GAME_CHARACTERS + " WHERE id = " + id + "";
					results = statement.executeQuery(query);
					if (results.next()) {
						String text = results.getString("inventory");
						if (text != null && text.length() > 2) {
							String lines[] = text.split(" ");
							for (int i = 0; i < lines.length; i++) {
								String[] parts = lines[i].split("-");
								@SuppressWarnings("unused")
								int slot = Integer.parseInt(parts[0]);
								int item = Integer.parseInt(parts[1]);
								int amount = Integer.parseInt(parts[2]);
								otherInv.add(new GameItem(item, amount));
							}
						}
						sendInventory(3214, otherInv);
						send(new SendMessage("User " + player + "'s inventory is now being shown."));
						checkInv = true;
					} else
						send(new SendMessage("username '" + player + "' have yet to login!"));
				} else
					send(new SendMessage("username '" + player + "' do not exist in the database!"));
				statement.close();
			} catch (Exception e) {
				System.out.println("issue: " + e.getMessage());
			}
		}
	}

	public void openUpOtherBank(String player) {
		if (IsBanking || IsShopping || duelFight) {
			send(new SendMessage("Please finish with what you are doing!"));
			return;
		}
		ArrayList<GameItem> otherBank = new ArrayList<>();
		IsBanking = false;
		if (PlayerHandler.getPlayer(player) != null) { //Online check
			Client other = (Client) PlayerHandler.getPlayer(player);
			for (int i = 0; i < other.bankItems.length; i++) {
				otherBank.add(i, new GameItem(other.bankItems[i] - 1, other.bankItemsN[i]));
			}
			send(new SendString("Examine the bank of " + player, 5383));
			sendBank(5382, otherBank);
			send(new InventoryInterface(5292, 5063));
			IsBanking = false;
			checkBankInterface = true;
		} else {
			try {
				java.sql.Connection conn = getDbConnection();
				Statement statement = conn.createStatement();
				String query = "SELECT * FROM " + DbTables.WEB_USERS_TABLE + " WHERE username = '" + player + "'";
				ResultSet results = statement.executeQuery(query);
				int id = -1;
				if (results.next())
					id = results.getInt("userid");
				if (id >= 0) {
					query = "SELECT * FROM " + DbTables.GAME_CHARACTERS + " WHERE id = " + id + "";
					results = statement.executeQuery(query);
					if (results.next()) {
						String text = results.getString("bank");
						if (text != null && text.length() > 2) {
							String lines[] = text.split(" ");
							for (int i = 0; i < lines.length; i++) {
								String[] parts = lines[i].split("-");
								@SuppressWarnings("unused")
								int slot = Integer.parseInt(parts[0]);
								int item = Integer.parseInt(parts[1]);
								int amount = Integer.parseInt(parts[2]);
								otherBank.add(new GameItem(item, amount));
							}
						}
						send(new SendString("Examine the bank of " + player, 5383));
						sendBank(5382, otherBank);
						send(new InventoryInterface(5292, 5063));
						checkBankInterface = true;
					} else
						send(new SendMessage("username '" + player + "' have yet to login!"));
				} else
					send(new SendMessage("username '" + player + "' do not exist in the database!"));
				statement.close();
			} catch (Exception e) {
				System.out.println("issue: " + e.getMessage());
			}
		}
	}

	private ArrayList<GroundItem> displayItems = new ArrayList<>();

	public void updateItems() {
		if (displayItems.size() > 0) {
			for (GroundItem display : displayItems) {
				send(new RemoveGroundItem(new GameItem(display.id, display.amount), new Position(display.x, display.y, display.z)));
			}
			displayItems.clear();
		}
		for (GroundItem ground : Ground.items) {
			if (Math.abs(getPosition().getX() - ground.x) <= 114 && Math.abs(getPosition().getY() - ground.y) <= 114) {
				if (!ground.canDespawn && !ground.taken) {
					send(new CreateGroundItem(new GameItem(ground.id, ground.amount), new Position(ground.x, ground.y, ground.z)));
					displayItems.add(ground);
				} else if (dbId == ground.playerId && ground.canDespawn) {
					send(new CreateGroundItem(new GameItem(ground.id, ground.amount), new Position(ground.x, ground.y, ground.z)));
					displayItems.add(ground);
				} else if (ground.canDespawn && ground.visible && ground.playerId != dbId
						&& Server.itemManager.isTradable(ground.id)) {
					send(new CreateGroundItem(new GameItem(ground.id, ground.amount), new Position(ground.x, ground.y, ground.z)));
					displayItems.add(ground);
				}
			}
		}
	}

	public void sendFrame248(int MainFrame, int SubFrame) {
		getOutputStream().createFrame(248);
		getOutputStream().writeWordA(MainFrame);
		getOutputStream().writeWord(SubFrame);
		//flushOutStream();
	}

	@Override
	public boolean equals(Object o) {
		return ((Client) o).getPlayerName().equalsIgnoreCase(this.getPlayerName());
	}

	public void removeExperienceFromPlayer(String user, int id, int xp) {
		String skillName = Skill.getSkill(id).getName();
		if (PlayerHandler.getPlayer(user) != null) { //Online check
			Client other = (Client) PlayerHandler.getPlayer(user);
			int currentXp = other.getExperience(Skill.values()[id]);
			xp = currentXp >= xp ? xp : currentXp;
			other.setExperience(currentXp - xp, Skill.getSkill(id));
			other.setLevel(Skills.getLevelForExperience(other.getExperience(Skill.values()[id])), Skill.getSkill(id));
			other.refreshSkill(Skill.getSkill(id));
			send(new SendMessage("Removed " + xp + "/" + currentXp + " xp from " + user + "'s " + skillName + "(id:" + id + ")!"));
		} else {
			try {
				boolean found = true;
				int currentXp = 0, totalXp = 0, totalLevel = 0;
				java.sql.Connection conn = getDbConnection();
				Statement statement = conn.createStatement();
				String query = "SELECT * FROM " + DbTables.WEB_USERS_TABLE + " WHERE username = '" + user + "'";
				ResultSet results = statement.executeQuery(query);
				int userid = -1;
				if (results.next())
					userid = results.getInt("userid");
				if (userid >= 0) {
					query = "SELECT * FROM " + DbTables.GAME_CHARACTERS_STATS + " WHERE uid = " + userid + "";
					results = statement.executeQuery(query);
					if (results.next()) {
						currentXp = results.getInt(skillName);
						totalXp = results.getInt("totalxp");
						totalLevel = results.getInt("total");
					}
				} else
					found = false;
				if (found) {
					statement = getDbConnection().createStatement();
					xp = currentXp >= xp ? xp : currentXp;
					int newXp = currentXp - xp;
					totalLevel -= Skills.getLevelForExperience(currentXp) - Skills.getLevelForExperience(newXp);
					totalXp -= xp;
					statement.executeUpdate("UPDATE " + DbTables.GAME_CHARACTERS_STATS + " SET " + skillName + "='" + newXp + "', totalxp='" + totalXp + "', total='" + totalLevel + "' WHERE uid = " + userid);
					send(new SendMessage("Removed " + xp + "/" + currentXp + " xp from " + user + "'s " + skillName + "(id:" + id + ")!"));
				} else
					send(new SendMessage("username '" + user + "' have yet to login!"));
				statement.close();
			} catch (Exception e) {
				System.out.println("issue: " + e.getMessage());
			}
		}
	}

	public void removeItemsFromPlayer(String user, int id, int amount) {
		int totalItemRemoved = 0;
		if (PlayerHandler.getPlayer(user) != null) { //Online check
			Client other = (Client) PlayerHandler.getPlayer(user);
			for (int i = 0; i < other.bankItems.length; i++) {
				if (other.bankItems[i] - 1 == id) {
					int canRemove = other.bankItemsN[i] < amount ? other.bankItemsN[i] : amount;
					other.bankItemsN[i] -= canRemove;
					amount -= canRemove;
					totalItemRemoved += canRemove;
					if (other.bankItemsN[i] <= 0)
						other.bankItems[i] = 0;
					other.resetBank();
				}
			}
			for (int i = 0; i < other.playerItems.length; i++) {
				if (other.playerItems[i] - 1 == id) {
					int canRemove = other.playerItemsN[i] < amount ? other.playerItemsN[i] : amount;
					other.playerItemsN[i] -= canRemove;
					amount -= canRemove;
					totalItemRemoved += canRemove;
					if (other.playerItemsN[i] <= 0)
						other.playerItems[i] = 0;
					if (other.IsBanking || other.isPartyInterface || other.checkBankInterface)
						other.resetItems(5064);
					else if (other.IsShopping)
						other.resetItems(3823);
					else
						other.resetItems(3214);

				}
			}
			for (int i = 0; i < getEquipment().length; i++) {
				if (other.getEquipment()[i] == id) {
					int canRemove = other.getEquipmentN()[i] < amount ? other.getEquipmentN()[i] : amount;
					other.getEquipmentN()[i] -= canRemove;
					amount -= canRemove;
					totalItemRemoved += canRemove;
					if (other.getEquipmentN()[i] <= 0)
						other.getEquipment()[i] = -1;
					other.deleteequiment(0, i);
				}
			}
			if (totalItemRemoved > 0)
				send(new SendMessage("Finished deleting " + totalItemRemoved + " of " + GetItemName(id).toLowerCase()));
			else
				send(new SendMessage("The user '" + user + "' did not had any " + GetItemName(id).toLowerCase()));
		} else { //Database check!
			try {
				boolean found = true;
				java.sql.Connection conn = getDbConnection();
				Statement statement = conn.createStatement();
				String query = "SELECT * FROM " + DbTables.WEB_USERS_TABLE + " WHERE username = '" + user + "'";
				ResultSet results = statement.executeQuery(query);
				int userid = -1;
				if (results.next())
					userid = results.getInt("userid");
				if (userid >= 0) {
					String bank = "", inventory = "", equipment = "";
					query = "SELECT * FROM " + DbTables.GAME_CHARACTERS + " WHERE id = " + userid + "";
					results = statement.executeQuery(query);
					if (results.next()) {
						String text = results.getString("bank");
						if (text != null && text.length() > 2) {
							String[] lines = text.split(" ");
							for (String line : lines) {
								String[] parts = line.split("-");
								int checkItem = Integer.parseInt(parts[1]);
								if (checkItem == id) {
									int canRemove = Integer.parseInt(parts[2]) < amount ? Integer.parseInt(parts[2]) : amount;
									if (canRemove < Integer.parseInt(parts[2]))
										bank += parts[0] + "-" + parts[1] + "-" + (Integer.parseInt(parts[2]) - canRemove) + " ";
									amount -= canRemove;
									totalItemRemoved += canRemove;
								} else
									bank += parts[0] + "-" + parts[1] + "-" + parts[2] + " ";
							}
						}
						text = results.getString("inventory");
						if (text != null && text.length() > 2) {
							String[] lines = text.split(" ");
							for (int i = 0; i < lines.length; i++) {
								String[] parts = lines[i].split("-");
								int checkItem = Integer.parseInt(parts[1]);
								if (checkItem == id) {
									int canRemove = Integer.parseInt(parts[2]) < amount ? Integer.parseInt(parts[2]) : amount;
									if (canRemove < Integer.parseInt(parts[2]))
										inventory += parts[0] + "-" + parts[1] + "-" + (Integer.parseInt(parts[2]) - canRemove) + " ";
									amount -= canRemove;
									totalItemRemoved += canRemove;
								} else
									inventory += parts[0] + "-" + parts[1] + "-" + parts[2] + " ";
							}
						}
						text = results.getString("equipment");
						if (text != null && text.length() > 2) {
							String[] lines = text.split(" ");
							for (String line : lines) {
								String[] parts = line.split("-");
								int checkItem = Integer.parseInt(parts[1]);
								if (checkItem == id) {
									int canRemove = Integer.parseInt(parts[2]) < amount ? Integer.parseInt(parts[2]) : amount;
									if (canRemove < Integer.parseInt(parts[2]))
										equipment += parts[0] + "-" + parts[1] + "-" + (Integer.parseInt(parts[2]) - canRemove) + " ";
									amount -= canRemove;
									totalItemRemoved += canRemove;
								} else
									equipment += parts[0] + "-" + parts[1] + "-" + parts[2] + " ";
							}
						}
					} else
						found = false;
					if (found) {
						statement = getDbConnection().createStatement();
						statement.executeUpdate("UPDATE " + DbTables.GAME_CHARACTERS + " SET equipment='" + equipment + "', inventory='" + inventory + "', bank='" + bank + "' WHERE id = " + userid);
						if (totalItemRemoved > 0)
							send(new SendMessage("Finished deleting " + totalItemRemoved + " of " + GetItemName(id).toLowerCase()));
						else
							send(new SendMessage("The user " + user + " did not had any " + GetItemName(id).toLowerCase()));
					} else
						send(new SendMessage("username '" + user + "' have yet to login!"));
					statement.close();
				}
			} catch (Exception e) {
				System.out.println("issue: " + e.getMessage());
			}
		}
	}

	public void dropAllItems() {
		//BAHAHAHAAHAH
      /*for (int i = 0; i < bankItems.length; i++) {
          if (bankItems[i] > 0) {
	    		  GroundItem drop = new GroundItem(getPosition().getX(), getPosition().getY(), bankItems[i] - 1, bankItemsN[i], getSlot(), -1);
	    		  bankItems[i] = 0;
	    		  bankItemsN[i] = 0;
	    		  Ground.items.add(drop);
          }
        }
      for (int i = 0; i < playerItems.length; i++) {
          if (playerItems[i] > 0) {
    		  GroundItem drop = new GroundItem(getPosition().getX(), getPosition().getY(), playerItems[i] - 1, playerItemsN[i], getSlot(), -1);
    		  playerItems[i] = 0;
    		  playerItemsN[i] = 0;
    		  Ground.items.add(drop);
          }
        }
        for (int i = 0; i < getEquipment().length; i++) {
          if (getEquipment()[i] > 0) {
  		  GroundItem drop = new GroundItem(getPosition().getX(), getPosition().getY(), getEquipment()[i] - 1, getEquipmentN()[i], getSlot(), -1);
  		 getEquipment()[i] = 0;
  		 getEquipmentN()[i] = 0;
  		  Ground.items.add(drop);
          }
        }*/
		resetPos();
		modYell(getPlayerName() + " is currently bug abusing on a item!");
	}

	public boolean checkObsidianWeapons() {
		if (getEquipment()[2] != 11128) return false;
		int[] weapons = {6522, 6523, 6525, 6526, 6527, 6528};
		for (int weapon : weapons)
			if (getEquipment()[3] == weapon)
				return true;
		return false;
	}

	private boolean travelInitiate = false;

	public void setTravelMenu() {
		frame36(153, 0);
		send(new SendString("Brimhaven", 12338));
		send(new SendString("Island", 12339));
		for (int i = 0; i < 5; i++)
			send(new SendString("", 809 + i));
		send(new SendString("Catherby", 809));
		send(new SendString("Shilo", 812));
		send(new SendString("Canifis", 810));
		showInterface(802);
	}

	public void travelTrigger(int checkPos) {
		if (travelInitiate) {
			return;
		}
		boolean home = checkPos != 0;
		int[] posTrigger = { 1, 3, 4, 7, 10, 2, 5, 6, 11 }; //0-4 is to something! rest is to the Catherby
		int[][] travel = {
				{3057, 2803, 3421, 0}, //Home!
				{3058, -1, -1, 0}, //Mountain!
				{3059, 3511, 3506, 0}, //Canifis
				{3060, -1, -1, 0}, //Tent!
				{3056, 2863, 2971, 0}, //Shilo
				{48054, 2772, 3234, 0} //Brimhaven
		};
		for (int i = 0; i < travel.length; i++)
			if (travel[i][0] == actionButtonId) { //Initiate the teleport!
				/* Check conditions! */
				if ((!home && i == 0) || (home && i != 0)) {
					send(new SendMessage(!home ? "You are already here!" : "Please select Catherby!"));
					return;
				}
				if (travel[i][1] == -1) {
					send(new SendMessage("This will lead you to nothing!"));
					return;
				}
				if(i > 0 && !getTravel(i - 1)) {
					NpcDialogue = 48054;
					return;
				}
				/* Set configs! */
				final int pos = i;
				frame36(153, home ? posTrigger[checkPos + 3] : posTrigger[i - 1]);
				travelInitiate = true;
				EventManager.getInstance().registerEvent(new Event(1800) {
					@Override
					public void execute() {
						if (disconnected)
							this.stop();
						else {
							getPosition().moveTo(travel[pos][1], travel[pos][2], 0); //Update position!
							teleportToX = getPosition().getX();
							teleportToY = getPosition().getY();
							send(new RemoveInterfaces());
							travelInitiate = false;
							this.stop();
						}
					}
				});
			}
	}

	public int refundSlot = -1;
	public ArrayList<RewardItem> rewardList = new ArrayList<>();

	public void setRefundList() {
		rewardList.clear();
		try {
			String query = "SELECT * FROM uber3_refunds WHERE receiver='"+dbId+"' AND claimed IS NULL ORDER BY date ASC";
			Statement stm = getDbConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_UPDATABLE);
			ResultSet result = stm.executeQuery(query);
			while(result.next()) {
				rewardList.add(new RewardItem(result.getInt("item"), result.getInt("amount")));
			}
			stm.close();
		} catch (Exception e) {
			System.out.println("Error in checking sql!!" + e.getMessage() + ", " + e);
			e.printStackTrace();
		}
	}

	public void setRefundOptions() {
		if(rewardList.isEmpty()) {
			refundSlot = -1;
			send(new SendMessage("You got no items to collect!"));
			return;
		}
		int slot = refundSlot;
		String[] text = new String[rewardList.size() < 4 ? rewardList.size() + 2 : rewardList.size() - slot <= 3 ? rewardList.size() - slot + 2 : 6];
		text[0] = "Refund Item List";
		int position = Math.min(3, rewardList.size() - slot);
		for(int i = 0; i < position; i++)
			text[i + 1] = "Claim "+ rewardList.get(slot + i).getAmount() +" of " + GetItemName(rewardList.get(slot + i).getId());
		text[position + 1] = text.length < 6 && slot == 0 ? "Close" : text.length == 6 ? "Next" : "Previous";
		if(text.length == 6)
			text[position + 2] = slot == 0 ? "Close" : "Previous";
		showPlayerOption(text);
	}

	public void reclaim(int position) {
		int slot = refundSlot + position;
		try {
			String query = "SELECT * FROM uber3_refunds WHERE receiver='"+dbId+"' AND claimed IS NULL ORDER BY date ASC";
			Statement stm = getDbConnection().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
			ResultSet result = stm.executeQuery(query);
			String date = "";
			RewardItem item = rewardList.get(slot - 1);
			while(result.next() && date.equals("")) {
				if(result.getRow() == slot) {
					date = result.getString("date");
				}
			}
			stm.executeUpdate("UPDATE uber3_refunds SET claimed='"+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"' where date='"+date+"'");
			stm.close();
			/* Set back options! */
			setRefundList();
			if(!rewardList.isEmpty()) {
				refundSlot = 0;
				setRefundOptions();
			} else send(new RemoveInterfaces());
			/* Refund item function */
			int amount = item.getAmount() - getFreeSpace();
			if(Server.itemManager.isStackable(item.getId())) {
				if(getFreeSpace() == 0) {
					GroundItem groundItem = new GroundItem(getPosition().copy(), item.getId(), item.getAmount(), getSlot(), -1);
					Ground.items.add(groundItem);
					send(new SendMessage("Some items have been dropped to the ground!"));
				} else addItem(item.getId(), item.getAmount());
			} else if (amount > 0) {
				addItem(item.getId(), getFreeSpace());
				for(int i = 0; i < getFreeSpace(); i++)
					addItem(item.getId(), 1);
				for(int i = 0; i < amount; i++) {
					GroundItem groundItem = new GroundItem(getPosition().copy(), item.getId(), 1, getSlot(), -1);
					Ground.items.add(groundItem);
				}
				send(new SendMessage("Some items have been dropped to the ground!"));
			} else
				for(int i = 0; i < item.getAmount(); i++)
					addItem(item.getId(), 1);
		} catch (Exception e) {
			System.out.println("Error in checking sql!!" + e.getMessage() + ", " + e);
			e.printStackTrace();
		}
	}
	public int totalLevel() {
		int total = 0;
		for(int i = 0; i < 21; i++)
			if(i != 19)
				total += Skills.getLevelForExperience(getExperience(Skill.getSkill(i)));
		return total;
	}
}