package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.item.GroundItem;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.packets.outgoing.SendString;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask;
import net.dodian.uber.game.security.DropLog;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

/**
 * @author Owner
 */
public class Npc extends Entity {
    public long inFrenzy = -1;
    public boolean hadFrenzy = false;
    private int id, currentHealth = 10, maxHealth = 10, respawn = 60, combat = 0, lastAttack = 0, maxHit;
    public boolean alive, visible = true, boss = false;
    private long deathTime = 0;
    public int[] boostedStat = {0, 0, 0, 0, 0}; //defence, attack, strength, magic dmg, range
    public int[] boostedStatOrig = {0, 0, 0, 0, 0}; //defence, attack, strength, magic dmg, range
    public long lastBoostedStat = System.currentTimeMillis();
    private int direction = -1;
    private int defaultFace;
    public int viewX;
    public int viewY;
    private int damageDealt = 0;
    private int deathEmote;
    public NpcData data;
    private boolean fighting = false;
    private final int[] level = new int[7];
    private int[] bossesId = {
            4130, //Dad
            3957, //Ungadulu
            2585, //Abyssal Guardian
            3964, //San Tajalon
            4067, //Black Knight Titan
            1443, //Jungle demon
            8, //Nechrayel
            4922, //Ice queen
            239, //King black dragon
            5311, //Head Mourner
            1432, //Black demon
            2266, //Daganoth prime
            2267, //Testing new boss mechanic!
            2261, //Rock crab boss
            3127, //Jad
            99999 // Dummy!
    };
    public enum weaponWeakness {
            NORMAL, SILVERWEAPON, DRAGONWEAPON;
    }
    public weaponWeakness weakness = weaponWeakness.NORMAL;
    private int[] silverWeapon = {
        3137 /* Vampyre */, 5635, /* Vampyre */ 2593 /* Werewolf */
    };

    public Npc(int slot, int id, Position position, int face) {
        super(position.copy(), slot, Entity.Type.NPC);
        this.id = id;
        this.defaultFace = face;
        data = Server.npcManager.getData(id);
        if (data != null) {
            deathEmote = data.getDeathEmote();
            respawn = data.getRespawn();
            combat = data.getCombat();
            for (int i = 0; i < data.getLevel().length; i++) {
                level[i] = data.getLevel()[i];
            }
            CalculateMaxHit(true);
            this.currentHealth = data.getHP();
            this.maxHealth = data.getHP();
        }
        alive = true;
        /* Set boss properties */
        for (int i = 0; i < bossesId.length; i++) {
            if (bossesId[i] == this.id)
                boss = true;
        }
        for (int i = 0; i < silverWeapon.length; i++) {
            if (silverWeapon[i] == this.id)
                weakness = weaponWeakness.SILVERWEAPON;
        }
    }

    public void reloadData() {
        data = Server.npcManager.getData(this.getId());
        if (data != null) {
            deathEmote = data.getDeathEmote();
            respawn = data.getRespawn();
            combat = data.getCombat();
            for (int i = 0; i < data.getLevel().length; i++) {
                level[i] = data.getLevel()[i];
            }
            CalculateMaxHit(true);
            this.currentHealth = data.getHP() < 1 ? 0 : data.getHP() > this.currentHealth ? this.currentHealth + (data.getHP() - this.currentHealth) :
                    data.getHP() < this.currentHealth ? this.currentHealth - (this.currentHealth - data.getHP()) : this.currentHealth;
            this.maxHealth = data.getHP();
        }
    }

    public void CalculateMaxHit(boolean melee) {
        double effectiveStrength = melee ? ((getStrength() + 1) + 8) : ((getRange() + 1) + 8);
        double maximumHit = 0.5 + effectiveStrength * (64 / 640D);
        maxHit = (int) Math.floor(maximumHit);
    }

    public Client getClient(int index) {
        return ((Client) PlayerHandler.players[index]);
    }

    public boolean validClient(int index) {
        Client p = (Client) PlayerHandler.players[index];
        return p != null && !p.disconnected && p.dbId > 0;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getFace() {
        return defaultFace;
    }
    public void setFace(int face) {
        defaultFace = face;
    }

    public void clearUpdateFlags() {
        direction = -1;
        crit = false;
        getUpdateFlags().clear();
    }

    public int getViewX() {
        return this.viewX;
    }

    public int getViewY() {
        return this.viewY;
    }

    public int getDirection() {
        return this.direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public boolean isWalking() {
        return false;
    }

    private boolean crit;

    public void showConfig(Client client) {
        int magicDamage = (int) Math.floor(maxHit * this.getMagic());
        CalculateMaxHit(false);
        int rangeMaxHit = maxHit;
        CalculateMaxHit(true);
        String[] commando = {
                "id - " + this.getId(),
                "name - " + data.getName(),
                "combat - " + data.getCombat(),
                "attackEmote - " + data.getAttackEmote(),
                "deathEmote - " + data.getDeathEmote(),
                "hitpoints - " + data.getHP(),
                "respawn - " + data.getRespawn(),
                "size - " + data.getSize(),
                "attack - " + this.getAttack(),
                "strength - " + this.getStrength(),
                "defence - " + this.getDefence(),
                "magic dmg - " + String.format("%3.1f", this.getMagic() * 100D) + "% (maxHit: " + magicDamage + ")",
                "ranged - " + this.getRange(),
                "Max hit Range: " + rangeMaxHit,
                "Max hit Melee: " + this.maxHit
        };
        client.send(new SendString("@dre@               Uber 3.0 Npc Configs", 8144));
        client.clearQuestInterface();
        int line = 8145;
        int count = 0;
        for (String s : commando) {
            client.send(new SendString(s, line));
            line++;
            count++;
            if (line == 8146)
                line = 8147;
            if (line == 8196)
                line = 12174;
            if (count > 100)
                break;
        }
        client.sendQuestSomething(8143);
        client.showInterface(8134);
        //client.flushOutStream();
    }
    public void showGemConfig(Client client) {
        int magicDamage = (int) Math.floor(maxHit * this.getMagic());
        CalculateMaxHit(false);
        int rangeMaxHit = maxHit;
        CalculateMaxHit(true);
        String[] commando = {
                "combat - " + getCombatLevel(),
                "hitpoints - " + data.getHP(),
                "respawn - " + data.getRespawn() + " seconds",
                "attack - " + this.getAttack(),
                "strength - " + this.getStrength(),
                "defence - " + this.getDefence(),
                "ranged - " + this.getRange(),
                "magic dmg - " + String.format("%3.1f", this.getMagic() * 100D) + "% (maxHit: " + magicDamage + ")",
                "Max hit Range: " + rangeMaxHit,
                "Max hit Melee: " + this.maxHit
        };
        client.send(new SendString("@dre@               Data for " + npcName(), 8144));
        client.clearQuestInterface();
        int line = 8145;
        int count = 0;
        for (String s : commando) {
            client.send(new SendString(s, line));
            line++;
            count++;
            if (line == 8146)
                line = 8147;
            if (line == 8196)
                line = 12174;
            if (count > 100)
                break;
        }
        client.sendQuestSomething(8143);
        client.showInterface(8134);
        //client.flushOutStream();
    }

    public void dealDamage(Client client, int hitDiff, boolean crit) {
        if (!alive || (currentHealth < 1 && maxHealth > 0))
            hitDiff = 0;
        else if (hitDiff > currentHealth)
            hitDiff = currentHealth;
        this.crit = crit;
        getUpdateFlags().setRequired(UpdateFlag.HIT, true);
        currentHealth -= hitDiff;
        damageDealt = hitDiff;
        int dmg = damageDealt;
        if (validClient(client)) {
            if (getDamage().containsKey(client)) {
                dmg += getDamage().get(client);
                getDamage().remove(client);
            }
            getDamage().put(client, dmg);
            fighting = true;
        }
        if (currentHealth <= 0 && maxHealth > 0) {
            alive = false;
            if (currentHealth < 0)
                currentHealth = 0;
            die();
        }
    }

    public void attack() {
        CalculateMaxHit(true);
        boolean multiAttack = getId() == 3127;
        if(!multiAttack) {
            Client enemy = getTarget(true);
            if (enemy == null) {
                fighting = false;
                return;
            } else {
                setFocus(enemy.getPosition().getX(), enemy.getPosition().getY());
                getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);
            }
            if(!specialCondition(enemy)) {
                int hitDiff = landHit(enemy, true) ? Utils.random(maxHit) : 0;
                requestAnim(data.getAttackEmote(), 0);
                enemy.dealDamage(hitDiff, false, this, damageType.MELEE);
                setLastAttack(getAttackTimer());
            }
        } else { //Jad!
            Client enemy = null;
            Client target = getTarget(true);
            if(target != null) {
                setFocus(target.getPosition().getX(), target.getPosition().getY());
                getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);
            }
            if(getId() == 3127) {
                int type = 0, chance = Misc.chance(6);
                type = chance == 6 ? Misc.chance(2) : type;
                for (Entity e : getDamage().keySet()) {
                    if (e instanceof Player) {
                        if (fighting && (e == null || !getPosition().withinDistance(e.getPosition(), 6) || ((Player) e).getCurrentHealth() < 1 || ((Client) e).deathStage > 0))
                            continue;
                        if(((Client) e).attackingNpc && ((Client) e).target instanceof Npc) { //Test?
                            enemy = Server.playerHandler.getClient(e.getSlot());
                            int hitDiff = 0;
                            if (type == 1) {
                                delayGfx(enemy, 2656, 446, 3, Utils.random((int) Math.floor(maxHit * this.getMagic())), false, this, damageType.JAD_MAGIC);
                                setGfx(444, 0);
                            } else if (type == 2) {
                                CalculateMaxHit(false);
                                delayGfx(enemy, 2652, 451, 3, Utils.random(maxHit), false, this, damageType.JAD_RANGED);
                            } else {
                                requestAnim(data.getAttackEmote(), 0);
                                enemy.dealDamage(landHit(enemy, true) ? Utils.random(maxHit) : hitDiff, false, this, damageType.MELEE);
                            }
                        }
                    }
                }
            } else {
                /* Not added yet! */
                System.out.println("Npcid " + getId() + " sends a multi attack!");
                /*requestAnim(data.getAttackEmote(), 0);
                for (Entity e : getDamage().keySet()) {
                    if (e instanceof Player) {
                        if (fighting && (!getPosition().withinDistance(e.getPosition(), 6) || ((Player) e).getCurrentHealth() < 1 || ((Client) e).deathStage > 0))
                            continue;
                        enemy = Server.playerHandler.getClient(e.getSlot());
                        int hitDiff = landHit(enemy, true) ? Utils.random(maxHit) : 0;
                        enemy.dealDamage(hitDiff, false, this, damageType.MELEE);
                    }
                }*/
            }
            if(enemy == null) fighting = false;
            setLastAttack(getAttackTimer());
        }
    }

    public boolean landHit(Client p, boolean melee) {
        double defLevel = p.getLevel(Skill.DEFENCE);
        double defBonus = 0.0;
        double atkLevel = (melee ? getAttack() : getRange()) * (getId() == 2261 && enraged(20000) ? 1.15 : 1.0);
        double atkBonus = 0.0;
        double NpcHitChance;
        for(int i = 5; i <= 7; i++)
            if(p.playerBonus[i] > defBonus)
                defBonus = p.checkObsidianWeapons() ? (int) (p.playerBonus[i] * 0.9) : p.playerBonus[i];
        defBonus = !melee ? p.playerBonus[9] : defBonus;
        double playerDef = defLevel * (defBonus + 64D);
        double npcAccuracy = atkLevel * (atkBonus + 64D);
        if (npcAccuracy > playerDef)
            NpcHitChance = 1 - ((playerDef + 2) / (2 * (npcAccuracy + 1)));
        else
            NpcHitChance = npcAccuracy / (2 * (playerDef + 1));
        double chance = Misc.chance(100000) / 1000D;
        p.debug("Npc Accuracy Hit: " + (NpcHitChance*100) + "% out of " + chance + "%");
        return chance < (NpcHitChance*100);
    }

    public void addBossCount(Player p) {
        for (int i = 0; i < p.boss_name.length; i++) {
            if (npcName().equalsIgnoreCase(p.boss_name[i].replace("_", " "))) {
                if (p.boss_amount[i] >= 100000)
                    return;
                p.boss_amount[i] += 1;
            }
        }
    }

    public int killCount(Player p) {
        for (int i = 0; i < p.boss_name.length; i++)
            if (npcName().equalsIgnoreCase(p.boss_name[i].replace("_", " ")))
                return p.boss_amount[i];
        return 0;
    }

    public void die() {
        alive = false;
        fighting = false;
        deathTime = System.currentTimeMillis();
        requestAnim(deathEmote, 0);
        Client p = getTarget(false);
        if (p == null)
            return;
        if (boss) {
            addBossCount(p);
            if (id != 2266 && id != 1432 && id != 5311 && id != 2585) {
                String yell = npcName() + " has been slain by " + p.getPlayerName() + " (level-" + p.determineCombatLevel() + ")";
                p.yell("<col=FFFF00>System<col=000000> <col=292BA3>" + yell);
            }
        }

        SlayerTask.slayerTasks task = SlayerTask.slayerTasks.getSlayerNpc(id);
        if (task != null) {
            if (task.ordinal() == p.getSlayerData().get(1) && p.getSlayerData().get(3) > 0) {
                p.getSlayerData().set(3, p.getSlayerData().get(3) - 1);
                p.giveExperience(maxHealth * 11, Skill.SLAYER);
                p.triggerRandom(maxHealth * 11);
                    if(p.getSlayerData().get(3) == 0) { // Finish task!
                        p.getSlayerData().set(4, p.getSlayerData().get(4) + 1);
                        /* Bonus slayer experience 1k, 500, 250, 100, 50 and 10 tasks! */
                        int[] taskStreak = {1000, 500, 250, 100, 50, 10};
                        int[] experience = {50, 30, 20, 11, 6, 2};
                        int bonusXp = -1;
                        p.send(new SendMessage("You have completed your slayer task!"));
                        for(int i = 0; i < taskStreak.length && bonusXp == -1; i++)
                            if(p.getSlayerData().get(4)%taskStreak[i] == 0) {
                                bonusXp = experience[i] * p.getSlayerData().get(2) * maxHealth;
                                p.giveExperience(bonusXp, Skill.SLAYER);
                                p.send(new SendMessage("You have gained some bonus experience from finishing your " + taskStreak[i] + " task in a row."));
                            }
                    }
                }
        }
    }

    public void drop() {
        Client target = getTarget(false);
        Position pos = getPosition().copy();
        if (target == null) {
            System.out.println("Target null.. Please investigate for " + getId() + " at position " + pos.toString());
            return;
        }
        int pid = target.getSlot();
        double rolledChance, currentChance, checkChance;
        int roll = 1;
        boolean wealth = target.getEquipment()[Equipment.Slot.RING.getId()] == 2572;
        boolean itemDropped;
        pos = getId() == 33333 ? new Position(1,2,0) : pos;
        //System.out.println("dropped.." + getId() + ", at " + pos.toString());

        for (int rolls = 0; rolls < roll; rolls++) {
            rolledChance = Misc.chance(100000) / 1000D; //100% = 100000 (1000 * 100)
            itemDropped = false;
            currentChance = 0.0;
            for (NpcDrop drop : data.getDrops()) {
                if (drop == null) continue;
                checkChance = drop.getChance();
                if (wealth && drop.getChance() < 10.0)
                    checkChance *= drop.getId() >= 5509 && drop.getId() <= 5515 ? 1.0 : drop.getChance() <= 0.1 ? 1.25 : drop.getChance() <= 1.0 ? 1.15 : 1.05;

                if (checkChance >= 100.0 || (checkChance + currentChance >= rolledChance && !itemDropped)) { // 100% items!
                    if (drop.getId() >= 5509 && drop.getId() <= 5515) //Just incase shiet!
                        if (target.checkItem(drop.getId()))
                            continue;
                    if (Server.itemManager.isStackable(drop.getId())) {
                        GroundItem item = new GroundItem(pos, drop.getId(), drop.getAmount(), pid, getId());
                        Ground.items.add(item);
                    } else {
                        for (int i = 0; i < drop.getAmount(); i++) {
                            GroundItem item = new GroundItem(pos, drop.getId(), 1, pid, getId());
                            Ground.items.add(item);
                        }
                    }
                    if (checkChance < 100.0)
                        itemDropped = true;
                    if (wealth && drop.getChance() < 10.0)
                        target.send(new SendMessage("<col=FF6347>Your ring of wealth shines more brightly!"));
                    if (drop.rareShout()) {
                        String yell = "<col=292BA3>" + target.getPlayerName() + " has recieved a "
                                + target.GetItemName(drop.getId()).toLowerCase() + " from " + npcName().toLowerCase() + (killCount(target) > 0 && boss ? " (Kill: " + killCount(target) + ")" : "");
                        target.yell("<col=FFFF00>System<col=000000> <col=FFFF00>" + yell);
                    }
                    DropLog.recordDrop(target, drop.getId(), drop.getAmount(), Server.npcManager.getName(id), pos, "Npc Drop");
                } else if (!itemDropped && checkChance < 100.0)
                    currentChance += checkChance;
            }
        }
    }

    public void respawn() {
        getPosition().moveTo(getOriginalPosition().getX(), getOriginalPosition().getY());
        setAlive(true);
        visible = true;
        currentHealth = maxHealth;
        hadFrenzy = false;
        inFrenzy = -1;
        setLastAttack(0);
        getDamage().clear();
        for(int i = 0; i < boostedStat.length; i++) {
            boostedStatOrig[i] = 0;
            boostedStat[i] = 0;
        }
    }

    public Client getTarget(boolean fighting) {
        int highest = -1;
        Client killer = null;
        if (getDamage().isEmpty())
            return null;
        for (Entity e : getDamage().keySet()) {
            if (e instanceof Player) {
                if (fighting && (!getPosition().withinDistance(e.getPosition(), 6) || ((Player) e).getCurrentHealth() < 1))
                    continue;
                int damage = getDamage().get(e);
                if (damage > highest) {
                    highest = damage == 0 ? -1 : damage;
                    killer = (Client) e;
                }
            }
        }
        return killer;
    }

    public int getNextWalkingDirection() {
        int dir;
        int moveY = 0;
        int moveX = 0;
        dir = Utils.direction(getPosition().getX(), getPosition().getY(), (getPosition().getX() + moveX),
                (getPosition().getY() + moveY));
        if (dir == -1) {
            System.out.println("returning -1");
            return -1;
        }
        dir >>= 1;
        return dir;
    }

    /**
     * @return the alive
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * @param alive the alive to set
     */
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    /**
     * @return the currentHealth
     */
    public int getCurrentHealth() {
        return currentHealth;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @return the deathtime
     */
    public long getDeathTime() {
        return deathTime;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * @return the respawn
     */
    public int getRespawn() {
        return boss ? respawn - Math.min(30, PlayerHandler.getPlayerCount() - 1) : respawn;
    }

    /**
     * @return the fighting
     */
    public boolean isFighting() {
        return fighting;
    }

    public boolean validClient(Client c) {
        return c != null && !c.disconnected && c.dbId > 0;
    }

    /**
     * @return the combat
     */
    public int getCombatLevel() {
        return combat;
    }

    /**
     * @return the lastAttack
     */
    public void setLastAttack(int lastAttack) {
        this.lastAttack = lastAttack;
    }
    public int getLastAttack() {
        return lastAttack;
    }

    public int getAttackTimer() {
        return getId() == 2261 && enraged(20000) ? 2 : getId() == 3127 ? 5 : 4;
    }

    public void removeEnemy(Client enemy) {
        getDamage().remove(enemy);
    }

    public int getHealth() {
        return maxHealth;
    }

    public int getAttack() {
        return level[1] + boostedStat[1];
    }

    public int getStrength() {
        return level[2] + boostedStat[2];
    }

    public int getDefence() {
        return level[0] + boostedStat[0];
    }

    public int getRange() {
        return level[4] + boostedStat[4];
    }

    public double getMagic() {
        double bonus = (level[6] + boostedStat[3]) / 10D;
        return bonus <= 0.0 ? 1.0 : (1.0 + (bonus / 100D));
    }

    public void boostedStat(int stat, int amount) {
        boostedStatOrig[stat] = amount;
        boostedStat[stat] = amount;
    }
    public void changeStat() {
        for(int i = 0; i < boostedStat.length; i++)
            boostedStat[i] = boostedStat[i] > 0 ? --boostedStat[i] : boostedStat[i] < 0 ? ++boostedStat[i] : 0;
        lastBoostedStat = System.currentTimeMillis();
    }

    public String npcName() {
        return Server.npcManager.getName(id).replace("_", " ");
    }

    public int getDamageDealt() {
        return this.damageDealt;
    }

    public boolean isCrit() {
        return this.crit;
    }

    public int getMaxHealth() {
        return this.maxHealth;
    }

    public int getTimeOnFloor() {
        int TIME_ON_FLOOR = 600;
        return id == 3127 ? 2400 : TIME_ON_FLOOR;
    }

    public NpcData getData() {
        return data;
    }

    public boolean enraged(int timer) {
        return inFrenzy != -1 && !(System.currentTimeMillis() - inFrenzy >= timer);
    }
    public void calmedDown() {
        inFrenzy = -1;
        hadFrenzy = true;
    }

    public void sendFightMessage(String msg) {
        for (Entity e : getDamage().keySet()) {
            if (e instanceof Player) {
                if (fighting && !getPosition().withinDistance(e.getPosition(), 12))
                    continue;
                ((Client) e).send(new SendMessage(msg));
            }
        }
    }

    public void heal(int heal) {
        int maxHealth = getMaxHealth();
        currentHealth = Math.min(currentHealth + heal, maxHealth);
    }

    public boolean specialCondition(Client c) {
        boolean attack = true;
        int hitDiff;
        switch(getId()) {
            case 260: //Green dragon
            case 265: //Blue Dragon
            case 247: //Red dragon
            case 252: //Black dragon
            case 2919: //Mithril dragon
                if(Misc.chance(6) == 1) {
                    hitDiff = Utils.random(50);
                    requestAnim(82, 0);
                    c.callGfxMask(438, 100);
                    c.dealDamage(hitDiff, false, this, damageType.FIRE_BREATH);
                    setLastAttack(getAttackTimer());
                } else attack = false;
            break;
            case 1611: //Battle mage
            case 2585: // Abyssal guardian boss!
                if(Misc.chance(5) == 1) {
                    hitDiff = Utils.random((int)Math.floor(maxHit * this.getMagic()));
                    requestAnim(data.getAttackEmote() + 1, 0);
                    c.callGfxMask(getId() == 2585 ? 89 : 76, 100);
                    c.dealDamage(hitDiff, false, this, damageType.MAGIC);
                    setLastAttack(getAttackTimer());
                } else attack = false;
            break;
            case 1432: //Black demon
                if(Misc.chance(6) == 1) { //Cast magic attack!
                    hitDiff = Utils.random((int)Math.floor(maxHit * this.getMagic()));
                    requestAnim(69, 0);
                    c.stillgfx(381, c.getPosition().getY(), c.getPosition().getX());
                    c.dealDamage(hitDiff, false, this, damageType.MAGIC);
                    setLastAttack(getAttackTimer());
                } else attack = false;
                break;
            case 1443: //Jungle demon
                if(Misc.chance(5) == 1) { //Cast magic attack!
                    boolean heal = Misc.random(2) == 1;
                    hitDiff = Utils.random((int)Math.floor(maxHit * this.getMagic() * (heal ? 1.1 : 0.9)));
                    requestAnim(69, 0);
                    c.stillgfx(heal ? 377 : 78, c.getPosition().getY(), c.getPosition().getX());
                    if(heal)
                        heal(hitDiff);
                    c.dealDamage(hitDiff, false, this, damageType.MAGIC);
                    setLastAttack(getAttackTimer());
                } else attack = false;
            break;
            case 4922: //Ice queen
                if(Misc.chance(6) == 1) { //Cast magic attack!
                    hitDiff = Utils.random((int)Math.floor(maxHit * this.getMagic()));
                    requestAnim(1979, 0);
                    c.stillgfx(369, c.getPosition().getY(), c.getPosition().getX());
                    c.dealDamage(hitDiff, false, this,  damageType.MAGIC);
                    setLastAttack(getAttackTimer());
                } else attack = false;
            break;
            case 3209: //Cave Horror
                if(Misc.chance(5) == 1) { //Cast range Attack
                    CalculateMaxHit(false);
                    hitDiff = landHit(c, false) ? Utils.random(maxHit) : 0;
                    requestAnim(4237, 0);
                    c.stillgfx(378, c.getPosition().getY(), c.getPosition().getX());
                    c.dealDamage(hitDiff, false, this, damageType.RANGED);
                    setLastAttack(getAttackTimer());
                } else attack = false;
                break;
            case 3957: //Ungadulu
                if(Misc.chance(4) == 1) { //Cast range Attack
                    CalculateMaxHit(false);
                    hitDiff = landHit(c, false) ? Utils.random(maxHit) : 0;
                    requestAnim(1978, 0);
                    c.stillgfx(180, c.getPosition().getY(), c.getPosition().getX());
                    c.dealDamage(hitDiff, false, this, damageType.RANGED);
                    setLastAttack(getAttackTimer());
                } else attack = false;
                break;
            case 2193: //Tok-Xil
                CalculateMaxHit(false);
                requestAnim(data.getAttackEmote(), 0);
                hitDiff = landHit(c, false) ? Utils.random(maxHit) : 0;
                c.stillgfx(441, c.getPosition().getY(), c.getPosition().getX());
                c.dealDamage(hitDiff, false, this, damageType.RANGED);
                setLastAttack(getAttackTimer());
                break;
            case 2154: // TzHaar-Mej
                if(Misc.chance(3) == 1) {
                    hitDiff = Utils.random((int)Math.floor(maxHit * this.getMagic()));
                    requestAnim(data.getAttackEmote() + 1, 0);
                    c.callGfxMask(440, 100);
                    c.dealDamage(hitDiff, false, this, damageType.MAGIC);
                    setLastAttack(getAttackTimer());
                } else attack = false;
                break;
            case 3964: //San Tojalon
                if(Misc.chance(5) == 1 && (boostedStat[0] <= boostedStatOrig[0] / 2)) { //Cast boost
                    setText("Let the shadow protect me!");
                    requestAnim(5489, 0);
                    boostedStat(0, 60);
                    setLastAttack(getAttackTimer() + 1);
                } else attack = false;
                break;
            case 4067: //Black knight titan
                if(Misc.chance(5) == 1 && (boostedStat[2] <= boostedStatOrig[2] / 2)) { //Cast boost
                    setText("GRRRRR!");
                    requestAnim(129, 0);
                    boostedStat(2, 60);
                    setLastAttack(getAttackTimer() + 1);
                } else attack = false;
                break;
            case 8: //Nechrayel
                if(Misc.chance(6) == 1 && (boostedStat[0] <= boostedStatOrig[0] / 2)) { //Cast boost
                    setText("I HAVE AWAKEN!");
                    requestAnim(1529, 0);
                    for(int i = 0; i <= 2; i++)
                        boostedStat(i, 40);
                    setLastAttack(getAttackTimer() + 1);
                } else attack = false;
                break;
            case 2266: //Prime
                if(Misc.chance(5) == 1 && (boostedStat[4] <= boostedStatOrig[4] / 2)) { //Cast boost
                    setText("Rawr!");
                    requestAnim(2855, 0);
                    boostedStat(1, 60);
                    boostedStat(4, 60);
                    setLastAttack(getAttackTimer() + 1);
                } else if(Misc.chance(5) == 1) {
                    CalculateMaxHit(false);
                    hitDiff = landHit(c, false) ? Utils.random(maxHit) : 0;
                    requestAnim(2854, 0);
                    c.stillgfx(406, c.getPosition().getY(), c.getPosition().getX());
                    c.dealDamage(hitDiff, false, this, damageType.RANGED);
                    setLastAttack(getAttackTimer());
                } else attack = false;
                break;
            case 5311: //Head Mourner
                if(Misc.chance(5) == 1) { //True melee damage!
                    setText("Get out of my farm!");
                    requestAnim(1203, 0);
                    c.dealDamage((int)(maxHit * 0.8), true, this, damageType.MELEE);
                } else attack = false;
                break;
            case 239: //King black dragon
                int landChance = Misc.chance(16);
                if(landChance == 1) { //Fire breath, guarantee hit as crit with 50% reduce dmg as melee
                    setText("Grrr!");
                    delayGfx(c, 81, 393, 2, (int)(maxHit * 0.5), true, this, damageType.FIRE_BREATH);
                } else if(landChance == 5) { //Blue breath, magic dmg
                    setText("Tsss!");
                    hitDiff = Utils.random((int)Math.floor(maxHit * this.getMagic()));
                    delayGfx(c, 82, 396, 2, hitDiff, false, this, damageType.FIRE_BREATH);
                } else if(landChance == 10) { //Green breath, range dmg
                    setText("Rawr!!");
                    CalculateMaxHit(false);
                    hitDiff = Utils.random(maxHit);
                    delayGfx(c, 83, 394, 2, landHit(c, false) ? hitDiff : 0, false, this, damageType.FIRE_BREATH);
                } else if(landChance == 16) { //White breath, melee dmg check with 20% increase dmg
                    setText("Tss rawr!!");
                    CalculateMaxHit(true);
                    hitDiff = Utils.random((int)Math.floor(maxHit * 1.2));
                    delayGfx(c, 84, 395, 2, landHit(c, true) ? hitDiff : 0, false, this, damageType.FIRE_BREATH);
                } else attack = false;
                break;
            case 3137: //77 vampyre!
            case 5635: //90 vampyre!
                if(c.playerBonus[8] < 15) {
                    hitDiff = 8 + Utils.random(8);
                    requestAnim(1263, 0);
                    heal(hitDiff);
                    c.dealDamage(hitDiff, false, this, damageType.TRUEDAMAGE);
                    c.send(new SendMessage("*The " + npcName().toLowerCase() + " bites you* Ouch!!"));
                    setLastAttack(getAttackTimer());
                } else attack = false;
            break;
            default: attack = false;
        }
        if(!attack)
            setLastAttack(getAttackTimer());
        return attack;
    }

    public void delayGfx(Client c, int anim, int gfx, int delay, int dmg, boolean crit, Entity npc, damageType type) {
        requestAnim(anim, 0);
        EventManager.getInstance().registerEvent(new Event(delay * 600) {

            public void execute() {
                if(c.disconnected) {
                    stop();
                    return;
                }
                if(getId() == 3127) c.stillgfx(gfx, c.getPosition().getY(), c.getPosition().getX());
                else c.stillgfx(gfx, getPosition().getY(), getPosition().getX());
                c.dealDamage(dmg, crit, npc, type);
                setLastAttack(getAttackTimer() - delay + 1);
                stop();
            }

        });
    }
}