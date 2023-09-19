package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.music.RegionSong;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.prayer.Prayers;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask;
import net.dodian.uber.game.party.Balloons;
import net.dodian.uber.game.party.RewardItem;
import net.dodian.utilities.Stream;
import net.dodian.utilities.Utils;

import java.util.*;

public abstract class Player extends Entity {
    public boolean yellOn = true, genie = false;
    public boolean saving = false;
    public long disconnectAt = 0, longName = 0;
    public int wildyLevel = 0, violations = 0;
    public long lastAction = 0, lastMagic = 0;
    public long lastPickAction = 0;
    public long lastTeleport = 0;
    private int playerNpc = -1;
    public int dbId = -1;
    public boolean premium = false, randomed = false;
    public int latestNews = 0;
    public int playerGroup = 3;
    public long lastPacket;
    public int[] playerLooks = new int[13];
    public boolean saveNeeded = true, lookNeeded = false;
    private boolean inCombat = false;
    private long lastCombat = 0;
    public long start = 0, lastPlayerCombat = 0;
    public static int id = -1, localId = -1;// dbId = -1; //mysql userid
    public boolean busy = false, invis = false;
    public String[] boss_name = {"Dad", "Black_Knight_Titan", "San_Tojalon", "Nechryael", "Ice_Queen", "Ungadulu",
            "Abyssal_Guardian", "Head_Mourners", "King_Black_Dragon", "Jungle_Demon", "Black_Demon", "Dwayne", "Dagannoth_Prime",
    "TzTok-Jad"};
    public int[] boss_amount = new int[boss_name.length];
    // dueling
    public int duelStatus = -1; // 0 = Requesting duel, 1 = in duel screen, 2 =
    // waiting for other player to accept, 3 = in
    // duel, 4 = won
    public int duelChatTimer = -1, iconTimer = 6;
    public boolean startDuel = false;
    public String forcedChat = "";
    private int headIcon = -1, skullIcon = -1;
    public int customCombat = -1;
    private WalkToTask walkToTask;
    public boolean IsPMLoaded = false;
    public int playerIsMember;
    public int[] playerBonus = new int[12];
    public int FightType = 1;
    private int playerSE = 0x328; // SE = Standard Emotion
    private int playerSEW = 0x333; // SEW = Standard Emotion Walking
    private int playerSER = 0x338; // SER = Standard Emotion Run
    public boolean IsCutting = false, IsAnvil = false;
    public boolean isFiremaking = false;
    public boolean attackingPlayer = false, attackingNpc = false;
    public int Essence;
    public boolean IsShopping = false;
    public int MyShopID = 0;
    public boolean UpdateShop = false;
    public int NpcDialogue = 0;
    public int NpcTalkTo = 0;
    public boolean NpcDialogueSend = false;
    public int NpcWanneTalk = 0;
    public boolean IsBanking = false, isPartyInterface = false, checkBankInterface;
    public boolean debug = false;
    private boolean crit;
    private boolean isNpc;
    public boolean initialized = false, disconnected = false;
    public boolean isActive = false;
    public boolean isKicked = false;
    public int actionTimer = 0;
    public String connectedFrom = "";
    public int ip = 0;
    public String UUID = "";
    public boolean takeAsNote = false;
    private String playerName = null; // name of the connecting client
    public String playerPass = null; // name of the connecting client
    public int playerRights; // 0=normal player, 1=player mod, 2=real mod,
    public PlayerHandler handler = null;
    public int maxItemAmount = Integer.MAX_VALUE;
    public int[] playerItems = new int[28];
    public int[] playerItemsN = new int[28];
    public int playerBankSize = 350;
    public int[] bankItems = new int[800];
    public int[] bankItemsN = new int[800];
    private int pGender;
    public int pHairC;
    public int pTorsoC;
    public int pLegsC;
    public int pFeetC;
    public int pSkinC;
    private int pHead;
    private int pTorso;
    private int pArms;
    private int pHands;
    private int pLegs;
    private int pFeet;
    private int pBeard;
    private final int[] playerEquipment = new int[14];
    private final int[] playerEquipmentN = new int[14];
    private final int[] playerLevel = new int[21];
    private final int[] playerXP = new int[21];
    public int maxHealth = 10, currentHealth = getLevel(Skill.HITPOINTS);
    public int maxPrayer = 1, currentPrayer = getLevel(Skill.PRAYER);
    public final static int maxPlayerListSize = Constants.maxPlayers;
    public Player[] playerList = new Player[maxPlayerListSize]; // To remove -Dashboard
    public int playerListSize = 0;
    public ArrayList<Player> playersUpdating = new ArrayList<>();
    private final Set<Npc> localNpcs = new LinkedHashSet<>(254);
    public boolean loaded = false;
    private final boolean[] songUnlocked = new boolean[RegionSong.values().length];
    private int faceTarget = -1;
    public int[] newWalkCmdX = new int[WALKING_QUEUE_SIZE];
    public int[] newWalkCmdY = new int[WALKING_QUEUE_SIZE];
    public int[] tmpNWCX = new int[WALKING_QUEUE_SIZE];
    public int[] tmpNWCY = new int[WALKING_QUEUE_SIZE];
    public int newWalkCmdSteps = 0;
    public boolean newWalkCmdIsRunning = false;
    public int[] travelBackX = new int[WALKING_QUEUE_SIZE];
    public int[] travelBackY = new int[WALKING_QUEUE_SIZE];
    public int numTravelBackSteps = 0;
    private int graphicId = 0;
    private int graphicHeight = 0;
    public int m4001 = 0;
    public int m4002 = 0;
    public int m4003 = 0;
    public int m4004 = 0;
    public int m4005 = 0;
    public int m4006 = 0;
    public int m4007 = 0;
    //Runecrafting
    public int[] runePouchesAmount = {0, 0, 0, 0};
    public int[] runePouchesLevel = {1, 20, 40, 60};
    public int[] runePouchesMaxAmount = {4, 7, 10, 13};
    //Agility
    public boolean UsingAgility = false;
    public int agilityCourseStage = 0;
    public long walkBlock = 0;
    public boolean xLog = false;
    public ArrayList<RewardItem> offeredPartyItems = new ArrayList<>();
    /*
     Entity(1 = player, 2 = npc, 3 = object, 4 = itemInv)
     slot
     id
     functionId (1 = use item on entity, 2 = click entity)
     */
    public ArrayList<Integer> playerPotato = new ArrayList<>();
    //Slayer
    private final ArrayList<Integer> slayerData = new ArrayList<>();
    private final ArrayList<Boolean> travelData = new ArrayList<>();
    private final ArrayList<Integer> paid = new ArrayList<>();
    private final ArrayList<Boolean> unlocked = new ArrayList<>();
    public int unlockLength = 2;
    public int lastRecoverEffect = 0, lastRecover = 4;
    public int boostedLevel[] = new int[21];
    public int chestEvent = 0;
    public boolean chestEventOccur = false, updateAnnounced = false;


    public Player(int slot) {
        super(new Position(-1, -1, 0), slot, Entity.Type.PLAYER);
        lastPacket = System.currentTimeMillis();
        /*
        // Setting player items
        Arrays.fill(playerItems, 0);
        // Setting Item amounts
        Arrays.fill(playerItemsN, 0);

        for (int i = 0; i < playerLevel.length; i++) { // Setting Levels
            if (i == 3) {
                playerLevel[i] = 10;
                playerXP[i] = 1155;
            } else {
                playerLevel[i] = 1;
                playerXP[i] = 0;
            }
        }

        for (int i = 0; i < playerBankSize; i++) { // Setting bank items
            bankItems[i] = 0;
        }

        for (int i = 0; i < playerBankSize; i++) { // Setting bank item amounts
            bankItemsN[i] = 0;
        }*/
        teleportToX = teleportToY = -1;
        mapRegionX = mapRegionY = -1;
        currentX = currentY = 0;
        resetWalkingQueue();
    }

    public void defaultCharacterLook(Client temp) {
        int[] testLook = {0, 3, 14, 18, 26, 34, 38, 42, 2, 14, 5, 4, 0}; // DEfault look!
        System.arraycopy(testLook, 0, playerLooks, 0, 13);
        temp.setLook(playerLooks);
    }

    void destruct() {
        getPosition().moveTo(-1, -1);
        mapRegionX = mapRegionY = -1;
        currentX = currentY = 0;
        resetWalkingQueue();
    }

    public void setTask(String input) {
        if (input.equals(""))
            input = "-1,-1,0,0,0,0,-1";
        String[] tasks = input.split(",");
        for (String task : tasks) slayerData.add(Integer.parseInt(task));
    }
    public String saveTaskAsString() {
        StringBuilder tasks = new StringBuilder();
        for (int i = 0; i < slayerData.size(); i++) {
            tasks.append(slayerData.get(i).toString()).append(i == slayerData.size() - 1 ? "" : ",");
        }
        return tasks.toString();
    }
    public ArrayList<Integer> getSlayerData() {
        //"-1,-1,0,0,0,0,-1" //master, taskid, total, currentAmount, streak, points, partner
        return slayerData;
    }
    public void setTravel(String input) {
        if (input.equals("")) input = "0:0:0:0:0";
        String[] travel = input.split(":");
        for (String s : travel) travelData.add(s.equals("1"));
    }
    public String saveTravelAsString() {
        StringBuilder travel = new StringBuilder();
        for (int i = 0; i < travelData.size(); i++) {
            int id = travelData.get(i) ? 1 : 0;
            travel.append(id).append(i == travelData.size() - 1 ? "" : ":");
        }
        return travel.toString();
    }
    public boolean getTravel(int i) {
        return travelData.get(i);
    }
    public void saveTravel(int i) {
        travelData.set(i, true);
    }
    public void addUnlocks(int i, String... check) {
            if(check.length == 1) {
                if(unlocked.isEmpty() || i == unlocked.size()) {
                    paid.add(i, -1);
                    unlocked.add(i, check[0].equals("1"));
                } else {
                    paid.set(i, -1);
                    unlocked.set(i, check[0].equals("1"));
                }
            } else if (check.length == 2) {
                if(unlocked.isEmpty() || i == unlocked.size()) {
                    unlocked.add(i, check[1].equals("1"));
                    paid.add(i, check[0].equals("1") ? 1 : 0);
                } else {
                    unlocked.set(i, check[1].equals("1"));
                    paid.set(i, check[0].equals("1") ? 1 : 0);
                }
            }
    }
    public String saveUnlocksAsString() {
        StringBuilder unlocks = new StringBuilder();
        for (int i = 0; i < unlocked.size(); i++) {
            int unlock = unlocked.get(i) ? 1 : 0;
            unlocks.append(paid.get(i) == -1 ? unlock + "" : paid.get(i) + "," + unlock).append(i == unlocked.size() - 1 ? "" : ":");
        }
        return unlocks.toString();
    }
    public boolean checkUnlock(int i) {
        return !unlocked.isEmpty() && unlocked.size() >= i && unlocked.get(i);
    }
    public int checkUnlockPaid(int i) {
        return paid.isEmpty() || paid.size() < i  ? -1 : paid.get(i);
    }

    public abstract void initialize();

    public abstract void update();

    public void receieveDamage(Entity e, int amount, boolean crit) {
        amount = Math.min(amount, currentHealth);
        if (getDamage().containsKey(e)) {
            getDamage().put(e, getDamage().get(e) + amount);
        } else {
            getDamage().put(e, amount);
        }
        dealDamage(amount, crit);
    }

    public void println_debug(String str) {
        System.out.println("[player-" + getSlot() + "]: " + str);
    }

    public void println(String str) {
        System.out.println("[player-" + getSlot() + "]: " + str);
    }

    public String getSongUnlockedSaveText() {
        StringBuilder out = new StringBuilder();
        for (boolean b : songUnlocked) {
            out.append(b ? 1 : 0).append(" ");
        }
        return out.toString();
    }

    public boolean withinDistance(Object o) {
        if (getPosition().getZ() != o.z)
            return false;
        int deltaX = o.x - getPosition().getX(),
                deltaY = o.y - getPosition().getY();
        return deltaX <= 64 && deltaX >= -64 && deltaY <= 64 && deltaY >= -64;
    }

    public boolean withinDistance(Player otherPlr) {
        if (!otherPlr.isActive)
            return false;
        if (getPosition().getZ() != otherPlr.getPosition().getZ())
            return false;
        int deltaX = otherPlr.getPosition().getX() - getPosition().getX(),
                deltaY = otherPlr.getPosition().getY() - getPosition().getY();
        return deltaX <= 15 && deltaX >= -16 && deltaY <= 15 && deltaY >= -16;
    }

    public boolean withinDistance(Npc npc) {
        if (getPosition().getZ() != npc.getPosition().getZ())
            return false;
        if (!npc.visible)
            return false;
        int deltaX = npc.getPosition().getX() - getPosition().getX(),
                deltaY = npc.getPosition().getY() - getPosition().getY();
        return deltaX <= 15 && deltaX >= -16 && deltaY <= 15 && deltaY >= -16;
    }

    /**
     * @return The username of the player.
     */
    public String getPlayerName() {
        return this.playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public WalkToTask getWalkToTask() {
        return this.walkToTask;
    }

    public void setWalkToTask(WalkToTask walkToTask) {
        this.walkToTask = walkToTask;
    }

    public int mapRegionX, mapRegionY; // the map region the player is
    // currently in
    private int currentX, currentY; // relative x/y coordinates (to map region)
    // Note that mapRegionX*8+currentX yields absX

    public static final int WALKING_QUEUE_SIZE = 50;
    public int[] walkingQueueX = new int[WALKING_QUEUE_SIZE], walkingQueueY = new int[WALKING_QUEUE_SIZE];
    public int wQueueReadPtr = 0; // points to slot for reading from queue
    public int wQueueWritePtr = 0; // points to (first free) slot for writing
    public boolean isRunning = false;
    public int teleportToX, teleportToY; // contain absolute x/y

    public void resetWalkingQueue() {
        wQueueReadPtr = wQueueWritePtr = 0;
        // properly initialize this to make the "travel back" algorithm work
        for (int i = 0; i < WALKING_QUEUE_SIZE; i++) {
            walkingQueueX[i] = currentX;
            walkingQueueY[i] = currentY;
        }
    }

    public void addToWalkingQueue(int x, int y) {
        int next = (wQueueWritePtr + 1) % WALKING_QUEUE_SIZE;
        if (next == wQueueWritePtr)
            return; // walking queue full, silently discard the data
        walkingQueueX[wQueueWritePtr] = x;
        walkingQueueY[wQueueWritePtr] = y;
        wQueueWritePtr = next;
    }

    // returns 0-7 for next walking direction or -1, if we're not moving
    public int getNextWalkingDirection() {
        if (wQueueReadPtr == wQueueWritePtr)
            return -1; // walking queue empty
        int dir;
        do {
            dir = Utils.direction(currentX, currentY, walkingQueueX[wQueueReadPtr], walkingQueueY[wQueueReadPtr]);
            if (dir == -1)
                wQueueReadPtr = (wQueueReadPtr + 1) % WALKING_QUEUE_SIZE;
            else if ((dir & 1) != 0) {
                println_debug("Invalid waypoint in walking queue!");
                resetWalkingQueue();
                return -1;
            }
        } while (dir == -1 && wQueueReadPtr != wQueueWritePtr);
        if (dir == -1)
            return -1;
        dir >>= 1;
        currentX += Utils.directionDeltaX[dir];
        currentY += Utils.directionDeltaY[dir];
        getPosition().moveTo(getPosition().getX() + Utils.directionDeltaX[dir],
                getPosition().getY() + Utils.directionDeltaY[dir]);
        return dir;
    }

    // calculates directions of player movement, or the new coordinates when
    // teleporting
    private boolean didTeleport = false; // set to true if char did teleport in
    // this cycle
    private boolean mapRegionDidChange = false;
    public boolean firstSend = false;
    private int primaryDirection = -1, secondaryDirection = -1; // direction char is going in this cycle

    public void getNextPlayerMovement() {
        Client temp = (Client) this;
        mapRegionDidChange = false;
        didTeleport = false;
        primaryDirection = secondaryDirection = -1;
        if (teleportToX != -1 && teleportToY != -1) {
            mapRegionDidChange = true;
            if (mapRegionX != -1 && mapRegionY != -1) {
                // check, whether destination is within current map region
                int relX = teleportToX - mapRegionX * 8, relY = teleportToY - mapRegionY * 8;
                if (relX >= 2 * 8 && relX < 11 * 8 && relY >= 2 * 8 && relY < 11 * 8)
                    mapRegionDidChange = false;
            }
            if (mapRegionDidChange) {
                // after map region change the relative coordinates range
                // between 48 - 55+
                if (firstSend) {
                    temp.pLoaded = false;
                } else {
                    firstSend = true;
                }
                mapRegionX = (teleportToX >> 3) - 6;
                mapRegionY = (teleportToY >> 3) - 6;
                // playerListSize = 0; // completely rebuild playerList after
                // teleport AND map region change
            }
            currentX = teleportToX - 8 * mapRegionX;
            currentY = teleportToY - 8 * mapRegionY;
            resetWalkingQueue();
            getPosition().moveTo(teleportToX, teleportToY);
            Balloons.updateBalloons(temp);
            GlobalObject.updateObject(temp);
            teleportToX = teleportToY = -1;
            didTeleport = true;
        } else {
            primaryDirection = getNextWalkingDirection();
            if (primaryDirection == -1)
                return; // standing
            if (isRunning && !temp.UsingAgility) {
                secondaryDirection = getNextWalkingDirection();
            }

            // check, if we're required to change the map region
            int deltaX = 0, deltaY = 0;
            if (currentX < 2 * 8) {
                deltaX = 4 * 8;
                mapRegionX -= 4;
                mapRegionDidChange = true;
            } else if (currentX >= 11 * 8) {
                deltaX = -4 * 8;
                mapRegionX += 4;
                mapRegionDidChange = true;
            }
            if (currentY < 2 * 8) {
                deltaY = 4 * 8;
                mapRegionY -= 4;
                mapRegionDidChange = true;
            } else if (currentY >= 11 * 8) {
                deltaY = -4 * 8;
                mapRegionY += 4;
                mapRegionDidChange = true;
            }

            if (mapRegionDidChange) {
                // have to adjust all relative coordinates
                if (firstSend) {
                    temp.pLoaded = false;
                } else {
                    firstSend = true;
                }
                currentX += deltaX;
                currentY += deltaY;
                for (int i = 0; i < WALKING_QUEUE_SIZE; i++) {
                    walkingQueueX[i] += deltaX;
                    walkingQueueY[i] += deltaY;
                }
            }
        }
    }

    public int getLevel(Skill skill) {
        return playerLevel[skill.getId()];
    }

    public int getExperience(Skill skill) {
        return playerXP[skill.getId()];
    }

    public void addExperience(int experience, Skill skill) {
        playerXP[skill.getId()] += experience;
    }

    public void setLevel(int level, Skill skill) {
        playerLevel[skill.getId()] = level;
    }

    public void setExperience(int experience, Skill skill) {
        playerXP[skill.getId()] = experience;
    }

    // handles anything related to character position basically walking, running
    // and standing
    // applies to only to "non-thisPlayer" charracters
    public void updatePlayerMovement(Stream str) {
        if (primaryDirection == -1) {
            // don't have to update the character position, because the char is
            // just standing
            if (getUpdateFlags().isUpdateRequired() || getUpdateFlags().get(UpdateFlag.CHAT)) {
                // tell client there's an update block appended at the end
                str.writeBits(1, 1);
                str.writeBits(2, 0);
            } else
                str.writeBits(1, 0);
        } else if (secondaryDirection == -1) {
            // send "walking packet"
            str.writeBits(1, 1);
            str.writeBits(2, 1);
            str.writeBits(3, Utils.xlateDirectionToClient[primaryDirection]);
            str.writeBits(1, getUpdateFlags().isUpdateRequired() ? 1 : 0);
        } else {
            // send "running packet"
            str.writeBits(1, 1);
            str.writeBits(2, 2);
            str.writeBits(3, Utils.xlateDirectionToClient[primaryDirection]);
            str.writeBits(3, Utils.xlateDirectionToClient[secondaryDirection]);
            str.writeBits(1, getUpdateFlags().isUpdateRequired() ? 1 : 0);
        }

    }

    public void addNewPlayer(Player plr, Stream str, Stream updateBlock) {
        int id = plr.getSlot();
        playerList[playerListSize++] = plr;
        playersUpdating.add(plr);
        str.writeBits(11, id);
        str.writeBits(1, 1);// Requires update?
        boolean savedFlag = plr.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE);
        plr.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
        PlayerUpdating.getInstance().appendBlockUpdate(plr, updateBlock);
        plr.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, savedFlag);
        str.writeBits(1, 1); // set to true, if we want to discard the
        // (clientside) walking queue
        // no idea what this might be useful for yet
        int z = plr.getPosition().getY() - getPosition().getY();
        if (z < 0)
            z += 32;
        str.writeBits(5, z); // y coordinate relative to thisPlayer
        z = plr.getPosition().getX() - getPosition().getX();
        if (z < 0)
            z += 32;
        str.writeBits(5, z); // x coordinate relative to thisPlayer
    }

    private final byte[] chatText = new byte[4096];
    private byte chatTextSize = 0;
    private int chatTextEffects = 0, chatTextColor = 0;

    public byte[] getChatText() {
        return this.chatText;
    }

    public byte getChatTextSize() {
        return this.chatTextSize;
    }

    public void setChatTextSize(byte chatTextSize) {
        this.chatTextSize = chatTextSize;
    }

    public int getChatTextEffects() {
        return this.chatTextEffects;
    }

    public void setChatTextEffects(int chatTextEffects) {
        this.chatTextEffects = chatTextEffects;
    }

    public int getChatTextColor() {
        return this.chatTextColor;
    }

    public void setChatTextColor(int chatTextColor) {
        this.chatTextColor = chatTextColor;
    }

    public void clearUpdateFlags() {
        IsStair = false; //What is this?!
        faceTarget(65535);
        getUpdateFlags().clear();
    }

    public void faceTarget(int index) {
        this.faceTarget = index;
        getUpdateFlags().setRequired(UpdateFlag.FACE_CHARACTER, true);
    }
    public void faceNpc(int index) {
        faceTarget(index);
    }
    public void facePlayer(int index) {
        faceTarget(32768 + index);
    }

    public void gfx0(int gfx) {
        graphicId = gfx;
        graphicHeight = 65536;
        getUpdateFlags().setRequired(UpdateFlag.GRAPHICS, true);
    }

    public int getFaceTarget() {
        return this.faceTarget;
    }
    public abstract void process(); //Send every 600 ms

    public abstract boolean packetProcess(); //Send every 600 ms

    public void postProcessing() {
        if (newWalkCmdSteps > 0) {
            int firstX = newWalkCmdX[0], firstY = newWalkCmdY[0]; // the point

            // travel backwards to find a proper connection vertex
            int lastDir;
            boolean found = false;
            numTravelBackSteps = 0;
            int ptr = wQueueReadPtr;
            int dir = Utils.direction(currentX, currentY, firstX, firstY);
            if (dir != -1 && (dir & 1) != 0) {
                // we can't connect first and current directly
                do {
                    lastDir = dir;
                    if (--ptr < 0)
                        ptr = WALKING_QUEUE_SIZE - 1;

                    travelBackX[numTravelBackSteps] = walkingQueueX[ptr];
                    travelBackY[numTravelBackSteps++] = walkingQueueY[ptr];
                    dir = Utils.direction(walkingQueueX[ptr], walkingQueueY[ptr], firstX, firstY);
                    if (lastDir != dir) {
                        found = true;
                        break; // either of those two, or a vertex between
                        // those is a candidate
                    }

                } while (ptr != wQueueWritePtr);
            } else
                found = true; // we didn't need to go back in time because the current position already can be connected to first

            if (!found) {
                println_debug("Fatal: couldn't find connection vertex! Dropping packet.");
                Client temp = (Client) this;
                temp.saveStats(true);
                disconnected = true;
            } else {
                wQueueWritePtr = wQueueReadPtr; // discard any yet unprocessed
                // waypoints from queue

                addToWalkingQueue(currentX, currentY); // have to add this in
                // order to keep
                // consistency in the
                // queue

                if (dir != -1 && (dir & 1) != 0) {

                    for (int i = 0; i < numTravelBackSteps - 1; i++) {
                        addToWalkingQueue(travelBackX[i], travelBackY[i]);
                    }
                    int wayPointX2 = travelBackX[numTravelBackSteps - 1], wayPointY2 = travelBackY[numTravelBackSteps - 1];
                    int wayPointX1, wayPointY1;
                    if (numTravelBackSteps == 1) {
                        wayPointX1 = currentX;
                        wayPointY1 = currentY;
                    } else {
                        wayPointX1 = travelBackX[numTravelBackSteps - 2];
                        wayPointY1 = travelBackY[numTravelBackSteps - 2];
                    }

                    dir = Utils.direction(wayPointX1, wayPointY1, wayPointX2, wayPointY2);
                    if (dir == -1 || (dir & 1) != 0) {
                        println_debug("Fatal: The walking queue is corrupt! wp1=(" + wayPointX1 + ", " + wayPointY1 + "), "
                                + "wp2=(" + wayPointX2 + ", " + wayPointY2 + ")");
                    } else {
                        dir >>= 1;
                        found = false;
                        int x = wayPointX1, y = wayPointY1;
                        while (x != wayPointX2 || y != wayPointY2) {
                            x += Utils.directionDeltaX[dir];
                            y += Utils.directionDeltaY[dir];
                            if ((Utils.direction(x, y, firstX, firstY) & 1) == 0) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            println_debug("Fatal: Internal error: unable to determine connection vertex!" + "  wp1=(" + wayPointX1
                                    + ", " + wayPointY1 + "), wp2=(" + wayPointX2 + ", " + wayPointY2 + "), " + "first=(" + firstX + ", "
                                    + firstY + ")");
                        } else
                            addToWalkingQueue(wayPointX1, wayPointY1);
                    }
                } else {
                    for (int i = 0; i < numTravelBackSteps; i++) {
                        addToWalkingQueue(travelBackX[i], travelBackY[i]);
                    }
                }

                for (int i = 0; i < newWalkCmdSteps; i++) {
                    addToWalkingQueue(newWalkCmdX[i], newWalkCmdY[i]);
                }

            }
            isRunning = (newWalkCmdIsRunning || buttonOnRun);
        }
        newWalkCmdSteps = 0;
    }

    public boolean buttonOnRun = true;

    public void kick() {
        isKicked = true;
    }

    private int hitDiff = 0;
    protected boolean IsStair = false;
    public int deathStage = 0;
    public long deathTimer = 0;

    public void appendMask400Update(Stream str) { // Xerozcheez: Something to
        str.writeByteA(m4001);
        str.writeByteA(m4002);
        str.writeByteA(m4003);
        str.writeByteA(m4004);
        str.writeWordA(m4005);
        str.writeWordBigEndianA(m4006);
        str.writeByteA(m4007); // direction
    }

    // PM Stuff
    public abstract void loadpm(long l, int world);

    public int Privatechat = 0;

    public abstract void sendpm(long name, int rights, byte[] chatmessage, int messagesize);

    public void dealDamage(int amt, boolean crit) {
        Client plr = ((Client) this);
        plr.debug("Dealing " + amt + " damage to you (hp=" + currentHealth + ")");
        double rolledChance = Math.random();
        double level = ((getLevel(Skill.PRAYER) + 1) / 8D) / 100D;
        double chance = level + 0.025; //(((Client) this).getEquipment()[3] == 11284 ? 0.1 : 0.0), maybe?!
        double dmg = ((Client) this).neglectDmg() / 10D;
        double reduceDamage = 1.0 - (dmg / 100);
        int oldDmg = amt;
        if (!(plr.inDuel && plr.duelRule[5]) && rolledChance <= chance && playerBonus[11] > 0 && oldDmg > 0) {
            amt = reduceDamage <= 0 ? 0 : (int)(amt * reduceDamage);
            if(amt != oldDmg)
                ((Client) this).send(new SendMessage("<col=FFD700>You neglected "+(amt == 0 ? "all" : "some")+" of the damage!"));
        }
        currentHealth -= amt;
        hitDiff = amt;
        if (plr.target != null && plr.target instanceof Player) { //Pvp damage profile!
            int totalDmg = 0;
            if (getDamage().containsKey(plr.target)) {
                totalDmg = getDamage().get(plr.target) + hitDiff;
                getDamage().remove(plr.target);
            } else
                totalDmg = hitDiff;
            getDamage().put(plr.target, totalDmg);
        }
        this.crit = crit;
        ((Client) this).refreshSkill(Skill.HITPOINTS);
        getUpdateFlags().setRequired(UpdateFlag.HIT, true);
    }

    public void dealDamage(int amt, boolean crit, Entity attacker, Entity.damageType dmg) {
        Client plr = ((Client) this);
        Npc npc = ((Npc) attacker);
        if(dmg.equals(damageType.FIRE_BREATH)) { //Dragons new effect!
            boolean gotAntiEffect = plr.getEquipment()[Equipment.Slot.SHIELD.getId()] == 1540
                    || plr.getEquipment()[Equipment.Slot.SHIELD.getId()] == 11284
                    || prayers.isPrayerOn(Prayers.Prayer.PROTECT_MAGIC);
            if(npc != null && npc.getId() == 239 && gotAntiEffect) amt /= 2;
            else if (npc != null && npc.getId() != 239 && gotAntiEffect) {
                amt *= 3; amt /= 10; //Ugly way to write reduce dmg by 70%
            } else plr.send(new SendMessage("You are badly burnt by the dragon fire!"));
        } else if(dmg.equals(damageType.MELEE) && prayers.isPrayerOn(Prayers.Prayer.PROTECT_MELEE)) amt /= 2;
        else if(dmg.equals(damageType.RANGED) && prayers.isPrayerOn(Prayers.Prayer.PROTECT_RANGE)) amt /= 2;
        else if(dmg.equals(damageType.MAGIC) && prayers.isPrayerOn(Prayers.Prayer.PROTECT_MAGIC)) amt /= 2;
        else if(dmg.equals(damageType.JAD_RANGED) && prayers.isPrayerOn(Prayers.Prayer.PROTECT_RANGE)) amt = 0;
        else if(dmg.equals(damageType.JAD_MAGIC) && prayers.isPrayerOn(Prayers.Prayer.PROTECT_MAGIC)) amt = 0;
        dealDamage(amt, crit);
    }

    private void delayedHit(Entity source, Entity target, final int damage, final boolean b, int delay) {
        if(source instanceof Client && target instanceof Npc) {
            final Client p = (Client) source;
            final Npc n = (Npc) target;
            EventManager.getInstance().registerEvent(new Event(delay) {

                public void execute() {
                    if(p.disconnected) {
                        stop();
                        return;
                    }
                    if(!n.alive) {
                        stop();
                        return;
                    }
                    n.dealDamage(p, damage, b);
                    stop();
                }

            });
        }
        if(source instanceof Client && target instanceof Client) {
            final Client p = (Client) source;
            final Client other = (Client) target;
            EventManager.getInstance().registerEvent(new Event(delay) {

                public void execute() {
                    if(p.disconnected) {
                        stop();
                        return;
                    }
                    if(other.disconnected || other.deathStage > 0) {
                        stop();
                        return;
                    }
                    other.receieveDamage(p, damage, b);
                    stop();
                }

            });
        }
    }

    public void sendAnimation(int id) {
        this.setAnimationId(id);
        getUpdateFlags().setRequired(UpdateFlag.ANIM, true);
    }

    public void teleportTo(int x, int y, int z) {
        getPosition().moveTo(x, y, z); //Update position!
        teleportToX = getPosition().getX();
        teleportToY = getPosition().getY();
    }

    Prayers prayers = new Prayers(this);

    public void setSkullIcon(int id) {
        skullIcon = id;
    }

    public boolean isSongUnlocked(int songId) {
        return this.songUnlocked[songId];
    }
    public boolean blackMaskEffect(int npcId) {
        String taskName = getSlayerData().get(0) == -1 || getSlayerData().get(3) <= 0 ? "" : "" + Objects.requireNonNull(SlayerTask.slayerTasks.getTask(getSlayerData().get(1))).getTextRepresentation();
        SlayerTask.slayerTasks slayerTask = SlayerTask.slayerTasks.getSlayerNpc(npcId);
        boolean onTask = slayerTask != null && slayerTask.getTextRepresentation().equals(taskName) && getSlayerData().get(3) > 0;
        int itemId = getEquipment()[Equipment.Slot.HEAD.getId()];
        boolean maskEquip = (itemId >= 8905 && itemId <= 8921) || itemId == 11864;
        return maskEquip && onTask;
    }
    public boolean blackMaskImbueEffect(int npcId) {
        String taskName = getSlayerData().get(0) == -1 || getSlayerData().get(3) <= 0 ? "" : "" + Objects.requireNonNull(SlayerTask.slayerTasks.getTask(getSlayerData().get(1))).getTextRepresentation();
        SlayerTask.slayerTasks slayerTask = SlayerTask.slayerTasks.getSlayerNpc(npcId);
        boolean onTask = slayerTask != null && slayerTask.getTextRepresentation().equals(taskName) && getSlayerData().get(3) > 0;
        int itemId = getEquipment()[Equipment.Slot.HEAD.getId()];
        boolean maskEquip = (itemId >= 11774 && itemId <= 11784) || itemId == 11865;
        return maskEquip && onTask;
    }

    public boolean checkObsidianBonus(int id) {
        int[] acceptedItems = {
        11128, 6585, 6568, 6570, //Berserker necklace, fury, obsidian cape, fire cape,
        6522, 6523, 6525, 6526, 6527, 6528, 6526, //Obsidian weapons
        6524, 21298, 21301, 21304 //Obsidian armour
        };
        boolean inArea = getPositionName(getPosition()) == positions.TZHAAR  || getPositionName(getPosition()) == positions.JAD;
        for(int i = 0; i < acceptedItems.length; i++)
            if(inArea && id == acceptedItems[i])
                return true;
        return false;
    }

    public boolean areAllSongsUnlocked() {
        for (boolean unlocked : songUnlocked) {
            if (!unlocked)
                return false;
        }
        return true;
    }

    public void setSongUnlocked(int songId, boolean unlocked) {
        this.songUnlocked[songId] = unlocked;
    }

    /**
     * Gets the hash collection of the local npcs.
     *
     * @return the local npcs.
     */
    public Set<Npc> getLocalNpcs() {
        return localNpcs;
    }

    public boolean didTeleport() {
        return this.didTeleport;
    }

    public boolean didMapRegionChange() {
        return this.mapRegionDidChange;
    }

    public int getPrimaryDirection() {
        return this.primaryDirection;
    }

    public int getSecondaryDirection() {
        return this.secondaryDirection;
    }

    public int getCurrentX() {
        return this.currentX;
    }

    public int getCurrentY() {
        return this.currentY;
    }

    public void setGraphic(int graphicId, int graphicHeight) {
        this.graphicId = graphicId;
        this.graphicHeight = graphicHeight;
    }

    public int getGraphicId() {
        return this.graphicId;
    }

    public int getGraphicHeight() {
        return this.graphicHeight;
    }

    public String getForcedChat() {
        return this.forcedChat;
    }

    public int[] getEquipment() {
        return this.playerEquipment;
    }

    public int[] getEquipmentN() {
        return this.playerEquipmentN;
    }

    public int getGender() {
        return this.pGender;
    }

    public void setGender(int pGender) {
        this.pGender = pGender;
    }

    public int getTorso() {
        return this.pTorso;
    }

    public void setTorso(int pTorso) {
        this.pTorso = pTorso;
    }

    public int getArms() {
        return this.pArms;
    }

    public void setArms(int pArms) {
        this.pArms = pArms;
    }

    public int getLegs() {
        return this.pLegs;
    }

    public void setLegs(int pLegs) {
        this.pLegs = pLegs;
    }

    public int getHands() {
        return this.pHands;
    }

    public void setHands(int pHands) {
        this.pHands = pHands;
    }

    public int getFeet() {
        return this.pFeet;
    }

    public void setFeet(int pFeet) {
        this.pFeet = pFeet;
    }

    public int getBeard() {
        return this.pBeard;
    }

    public void setBeard(int pBeard) {
        this.pBeard = pBeard;
    }

    public int getHead() {
        return this.pHead;
    }

    public void setHead(int pHead) {
        this.pHead = pHead;
    }

    public int getHeadIcon() {
        return this.headIcon;
    }

    public void setHeadIcon(int headIcon) {
        this.headIcon = headIcon;
    }

    public int getSkullIcon() {
        return this.skullIcon;
    }

    public int getStandAnim() {
        return this.playerSE;
    }

    public void setStandAnim(int playerSE) {
        this.playerSE = playerSE;
    }

    public int getWalkAnim() {
        return this.playerSEW;
    }

    public void setAgilityEmote(int walk, int run) {
        setWalkAnim(walk);
        setRunAnim(run);
        getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }

    public void setWalkAnim(int playerSEW) {
        this.playerSEW = playerSEW;
    }

    public int getRunAnim() {
        return this.playerSER;
    }

    public void setRunAnim(int playerSER) {
        this.playerSER = playerSER;
    }

    public boolean isNpc() {
        return this.isNpc;
    }

    public void setNpcMode(boolean isNpc) {
        this.isNpc = isNpc;
    }

    public int getPlayerNpc() {
        return this.playerNpc;
    }

    public void setPlayerNpc(int playerNpc) {
        this.playerNpc = playerNpc;
    }

    public int getHitDiff() {
        return this.hitDiff;
    }

    public void setHitDiff(int hitDiff) {
        this.hitDiff = hitDiff;
    }

    public boolean isCrit() {
        return this.crit;
    }

    public int getCurrentHealth() {
        return this.currentHealth;
    }
    public int getMaxHealth() {
        return this.maxHealth;
    }
    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }
    public void heal(int healing) {
        Client c = (Client) this;
        int maxLevel = getMaxHealth();
        setCurrentHealth(getCurrentHealth() + healing > maxLevel ? maxLevel : getCurrentHealth() + healing);
        c.refreshSkill(Skill.HITPOINTS);
    }
    public void eat(int healing, int removeId, int removeSlot) {
        Client c = (Client) this;
        if (c.deathStage > 0 || c.deathTimer > 0 || c.getCurrentHealth() < 1) {
            return;
        }
        if(getCurrentHealth() < getMaxHealth()) {
            c.requestAnim(829, 0);
            c.deleteItem(removeId, removeSlot, 1);
            c.send(new SendMessage("You eat the " + Server.itemManager.getName(removeId).toLowerCase() + "."));
            heal(healing);
        } else c.send(new SendMessage("You have full health already, so you spare the "+ Server.itemManager.getName(removeId).toLowerCase() +" for later."));
    }

    public void boost(int boosted, Skill skill) {
        if(skill.getId() == 3 || skill.getId() == 5) { //Cant do health or prayer with this method!
            return;
        }
        Client c = (Client) this;
        int lvl = Skills.getLevelForExperience(getExperience(skill));
        int currentLevel = c.getLevel(skill);
        boosted = currentLevel >= lvl + boosted ? currentLevel - lvl : boosted;
        boostedLevel[skill.getId()] = boosted;
        c.refreshSkill(skill);
    }

    public int getCurrentPrayer() {
        return this.currentPrayer;
    }
    public int getMaxPrayer() {
        return this.maxPrayer;
    }
    public void setCurrentPrayer(int amount) {
        this.currentPrayer = amount;
    }
    public void drainPrayer(int amount) {
        pray(-amount);
        if(getCurrentPrayer() <= 0) {
            setCurrentPrayer(0);
            prayers.reset();
            ((Client) this).send(new SendMessage("<col=8B8000>Your prayer has ran out! Please recharge at a nearby altar!"));
        }
    }
    public void pray(int healing) {
        Client c = (Client) this;
        int maxLevel = getMaxPrayer();
        setCurrentPrayer(getCurrentPrayer() + healing > maxLevel ? maxLevel : getCurrentPrayer() + healing);
        c.refreshSkill(Skill.PRAYER);
    }

    public void setCrit(boolean crit) {
        this.crit = crit;
    }

    public boolean isInCombat() {
        return this.inCombat;
    }

    public void setInCombat(boolean inCombat) {
        this.inCombat = inCombat;
    }

    public long getLastCombat() {
        return this.lastCombat;
    }

    public void setLastCombat(long lastCombat) {
        this.lastCombat = lastCombat;
    }

    /**
     * Calculates and returns the combat level for this player.
     *
     * @return the combat level.
     */
    public int determineCombatLevel() {
        int magLvl = Skills.getLevelForExperience(getExperience(Skill.MAGIC));
        int ranLvl = Skills.getLevelForExperience(getExperience(Skill.RANGED));
        int attLvl = Skills.getLevelForExperience(getExperience(Skill.ATTACK));
        int strLvl = Skills.getLevelForExperience(getExperience(Skill.STRENGTH));
        int defLvl = Skills.getLevelForExperience(getExperience(Skill.DEFENCE));
        int hitLvl = Skills.getLevelForExperience(getExperience(Skill.HITPOINTS));
        int prayLvl = Skills.getLevelForExperience(getExperience(Skill.PRAYER));
        double mag = magLvl * 1.5;
        double ran = ranLvl * 1.5;
        double attstr = attLvl + strLvl;
        double combatLevel;
        if (ran > attstr && ran > mag) { // player is ranged class
            combatLevel = ((double)(defLvl) * 0.25) + ((double)(hitLvl) * 0.25) + ((double)(prayLvl / 2) * 0.25) + ((double)(ranLvl) * 0.4875);
        } else if (mag > attstr) { // player is mage class
            combatLevel = (((double)(defLvl) * 0.25) + ((double)(hitLvl) * 0.25) + ((double)(prayLvl / 2) * 0.25) + ((double)(magLvl) * 0.4875));
        } else {
            combatLevel = (((double)(defLvl) * 0.25) + ((double)(hitLvl) * 0.25) + ((double)(prayLvl / 2) * 0.25) + ((double)(attLvl) * 0.325) + ((double)(strLvl) * 0.325));
        }
        return customCombat > 0 ? customCombat : (int)combatLevel;
    }

    public int getSkillLevel(Skill skill) {
        return Skills.getLevelForExperience(getExperience(skill));
    }

    public enum positions {
        //name, x1, x2, y1, y2
        YANILLE("in Yanille.", 2562, 2620, 3072, 3110),
        WESTYANILLE("in West Yanille.", 2525, 2561, 3072, 3110),
        SEERS("in Seer's Village.", 2686, 2742, 3449, 3531),
        EASTARDY("in East Ardougne.", 2559, 2688, 3260, 3345),
        WESTARDY("in West Ardougne.", 2437, 2559, 3260, 3337),
        CATHERBY("in Catherby.", 2789, 2840, 3434, 3469),
        CATHDOCKS("at Catherby docks.", 2802, 2806, 3417, 3430),
        CATHBEACH("at Catherby beach.", 2833, 2859, 3418, 3436),
        LEGENDSGUILD("in Legend's Guild.", 2726, 2731, 3350, 3361),
        LEGENDSGUILD2("in Legend's Guild.", 2719, 2738, 3362, 3386),
        LEGENDSDUNG("in Legend's dungeon.", 2688, 2742, 9725, 9786),
        TAVERLY("in Taverly.", 2876, 2936, 3405, 3490),
        TAVDUNG("in Taverly dungeon.", 2813, 2970, 9669, 9856),
        FISHGUILD("in Fishing Guild.", 2577, 2642, 3390, 3446),
        GNOMECOURSE("at Gnome course.", 2469, 2490, 3414, 3440),
        GNOMEHIDDENCAVE_WEST("in the Gnome hidden cave.", 2434, 2452, 9896, 9918),
        GNOMEHIDDENCAVE_MIDDLE("in the Gnome hidden cave.", 2453, 2478, 9901, 9918),
        GNOMEHIDDENCAVE_EAST("in the Gnome hidden cave.", 2479, 2495, 9907, 9918),
        BARBCOURSE("at Barbarian course.", 2528, 2553, 3541, 3559),
        WILDERNESSCOURSE("at Wilderness course.", 2987, 3009, 3931, 3966),
        HEROESGUILD("in Heroes Guild.", 2883, 2901, 3503, 3518),
        HEROESDUNG("in Heroes dungeon.", 2882, 2944, 9878, 9920),
        DRAGCAVE("in Dragon cave.", 3200, 3304, 9342, 9406),
        DRAGCAVE2("in Dragon cave.", 3303, 3326, 9394, 9406),
        DRAGCAVE3("in Dragon cave.", 3303, 3326, 9342, 9357),
        KBDLAIR("in the Dragon's den!", 3303, 3326, 9360, 9394),
        STAFFZONE("in the Staffzone.", 2880, 2943, 4672, 4735),
        EDGEVILLE("in the Edgeville.", 3065, 3133, 3463, 3521),
        SHILO("in the Shilo village.", 2817, 2878, 2945, 3006),
        TZHAAR("in the Tzhaar cave.", 2397, 2494, 5120, 5183),
        JAD("in the Jad cave.", 2368, 2428, 5057, 5118),
        BRIMHAVEN_DOCKS("in the Brimhaven docks.", 2758, 2787, 3218, 3240),
        BRIMHAVEN_WEST("in the Brimhaven west.", 2689, 2757, 3137, 3247),
        BRIMHAVEN_EAST("in the Brimhaven east.", 2757, 2815, 3137, 3217),
        BRIMHAVEN_DUNGEON("in the Brimhaven dungeon.", 3713, 3837, 9346, 9469),
        KEYDUNG("in Key dungeon.", 2559, 2622, 9475, 9534),
        IKOVDUNG("in the Temple of Ikov.", 2626, 2750, 9784, 9918),
        PARTYROOM("in the Partyroom.", 3035, 3055, 3370, 3385),
        ;
        final String name;
        final int[] coordValue;

        positions(String name, int... coordValue) {
            this.name = name;
            this.coordValue = coordValue;
        }
	  /*
	  2619, 3073
	  2539, 3073
	  2539, 3108
	  */ //Test Yanille coords.
    }

    public int getSkillId(String name) {
        for (int i = 0; i < Skill.values().length; i++)
            if (Skill.values()[i].getName().startsWith(name.toLowerCase()))
                return i;
        return -1;
    }

    public positions getPositionName(Position current) {
        for (positions pos : positions.values()) {
            if (current.getX() >= pos.coordValue[0] && current.getX() <= pos.coordValue[1] && current.getY() >= pos.coordValue[2] && current.getY() <= pos.coordValue[3])
                return pos;
        }
        return null;
    }

    public String getPositionName() {
        Position current = getPosition().copy();
        for (positions pos : positions.values()) {
            if (current.getX() >= pos.coordValue[0] && current.getX() <= pos.coordValue[1] && current.getY() >= pos.coordValue[2] && current.getY() <= pos.coordValue[3])
                return pos.name;
        }
        return "exploring the world.";
    }

    public static void openPage(Client c, String pageName) {
        try {
            c.send(new SendMessage(pageName + "#url#"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean inWildy() {
        // if(absX > 2600 && absY > 8900) return true;
        return wildyLevel > 0 || (getPosition().getY() == 3523);
    }

    public boolean inEdgeville() {
        return getPositionName(getPosition()) == positions.EDGEVILLE;
    }

    public void respawnBoss(int id) {
        Client c = (Client) this;
        int status = -1;
        for(Npc n : Server.npcManager.getNpcs()) {
            if(n.getId() == id && status == -1) {
                n.respawn();
                status = !n.alive ? 0 : 1;
            }
        }
        String npcName = Server.npcManager.getName(id);
        c.send(new SendMessage(status == -1 ? "Could not found the npc with the name of '" + npcName + "'." : status == 0 ? "You respawn " + npcName + "!" : npcName + " is already alive!"));
    }

}