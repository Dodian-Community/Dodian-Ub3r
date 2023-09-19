package net.dodian.jobs.impl;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.item.GroundItem;
import net.dodian.uber.game.model.player.packets.outgoing.CreateGroundItem;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class EntityProcessor implements Job {

    public void execute(JobExecutionContext context) throws JobExecutionException {
        /* Npc! */
        npcProcess();
        /* Player */
        playerProcess();
        /* Items on ground */
        groundProcess();
        /* Clear npc update! */
        for (Npc npc : Server.npcManager.getNpcs()) {
            npc.clearUpdateFlags();
        }
    }

    public void npcProcess() {
        long now = System.currentTimeMillis();
        for (Npc npc : Server.npcManager.getNpcs()) {
            /* Facing of npc! */
            if(!npc.isFighting() && npc.isAlive())
                npc.setFocus(npc.getPosition().getX() + Utils.directionDeltaX[npc.getFace()], npc.getPosition().getY() + Utils.directionDeltaY[npc.getFace()]);
            /* Timer check for boosted stats!*/
            if(now - npc.lastBoostedStat >= 30000) { //Stat boost!
                npc.changeStat();
            }
            if (!npc.alive && npc.visible && (now - npc.getDeathTime() >= npc.getTimeOnFloor())) {
                npc.setVisible(false);
                npc.drop();
                Client p = npc.getTarget(false);
                npc.removeEnemy(p);
                /* Jad loot table, up to 5 players */
                if(npc.getId() == 3127)
                    for(int i = 1; i <= 4 && !npc.getDamage().isEmpty(); i++) { //4 more players if damage not empty.
                        p = npc.getTarget(false);
                        if(p != null) {
                            double chance = (0.1 + (npc.getDamage().get(p) / (double)npc.getMaxHealth())) * 100; //10% + your damage profile!
                            double rate = Misc.chance(100000) / 1000D;
                            if(chance - 10 >= 5 && rate <= chance) {
                                npc.drop();
                                p.send(new SendMessage("You managed to roll for the loot!"));
                            } else if(chance - 10 < 5) p.send(new SendMessage("You were not eligible for the drop!"));
                            else p.send(new SendMessage("Unlucky! Better luck next time."));
                        }
                        npc.removeEnemy(p); //Need to remove the enemy if we done with the check values!
                    }
            }
            if (!npc.alive && !npc.visible && (now - (npc.getDeathTime() + npc.getTimeOnFloor()) >= (npc.getRespawn() * 1000L))) {
                npc.respawn();
            }
            if(npc.getLastAttack() > 0)
                npc.setLastAttack(npc.getLastAttack() - 1); //Remove one tick of attack timer every 600 ms!
            if (npc.alive && npc.isFighting() && npc.getLastAttack() == 0) {
                npc.attack();
                if(npc.getId() == 2261) { //Dwayne effect
                    int hp = (int)(npc.getMaxHealth() * 0.40);
                    if(npc.inFrenzy != -1 && !npc.enraged(20000)) {
                        npc.calmedDown();
                        npc.sendFightMessage(npc.npcName() + " have calmed down.");
                    } else if(!npc.hadFrenzy && npc.inFrenzy == -1 && npc.getCurrentHealth() < hp) {
                        npc.inFrenzy = System.currentTimeMillis();
                        npc.sendFightMessage(npc.npcName() + " have become enraged!");
                    }
                }
            }
            if (npc.getId() == 3805 && Misc.chance(100) == 1) {
                int jackpot = Math.min(Server.slots.slotsJackpot + Server.slots.peteBalance, Integer.MAX_VALUE);
                npc.setText("Current Jackpot is " + jackpot + " coins!");
            }
            if (npc.getId() == 4218 && Misc.chance(8) == 1) {
                npc.setText("Watch out for the plague!!");
            }
            if (npc.getId() == 2805 && Misc.chance(50) == 1) {
                npc.setText(Misc.chance(2) == 1 ? "Moo" : "Moo!!");
            }
            if (npc.getId() == 555 && Misc.chance(10) == 1) {
                npc.setText(Misc.chance(2) == 1 ? "The plague is coming!" : "Watch out for the plague!!");
            }
            if (npc.getId() == 5792 && Balloons.eventActive()) {
                npc.requestAnim(866, 0);
                npc.setText(Balloons.spawnedBalloons() ? "A party is going on right now!" : "A party is about to Start!!!!");
            }
            if (npc.getId() == 3306 && Misc.chance(25) == 1) {
                int peopleInEdge = 0;
                int peopleInWild = 0;
                for (int a = 0; a < Constants.maxPlayers; a++) {
                    Client checkPlayer = (Client) PlayerHandler.players[a];
                    if (checkPlayer != null) {
                        if (checkPlayer.inWildy())
                            peopleInWild++;
                        else if (checkPlayer.inEdgeville())
                            peopleInEdge++;
                    }
                }
                npc.setText("There is currently " + peopleInWild + " player" + (peopleInWild != 1 ? "s" : "") + " in the wild and " + peopleInEdge + " player" + (peopleInEdge != 1 ? "s" : "") + " in Edgeville!");
            }
        }
    }

    public void playerProcess() {
        long currentTime = System.currentTimeMillis();
        Server.playerHandler.updatePlayerNames();
        /* Cycle to clear some ol' shiez */
        if (PlayerHandler.cycle % 10 == 0) {
            Server.connections.clear();
            Server.nullConnections = 0;
            // System.out.println("Clearing connections");
        }
        if (PlayerHandler.cycle % 100 == 0) {
            Server.banned.clear();
            // System.out.println("Clearing connection bans");
        }
        if (PlayerHandler.cycle > 10000) {
            PlayerHandler.cycle = 0;
        }
        PlayerHandler.cycle++;
        //Processing!
        for (int i = 0; i < Constants.maxPlayers; i++) {
            if (PlayerHandler.players[i] == null || !PlayerHandler.players[i].isActive) //Hate continue; in a loop! Dodian do it this way..*yikes*
                continue;
            /* Removing non-responding player */
            long lp = currentTime - PlayerHandler.players[i].lastPacket;
            if (PlayerHandler.players[i].dbId < 1 && lp >= 30000) { //Removing non-responding player
                PlayerHandler.players[i].disconnected = true;
                //System.out.println("Remove disconnect from main!.." + PlayerHandler.players[i].getPlayerName());
            }
            /* If not disconnected process a player! */
            if(!PlayerHandler.players[i].disconnected) {
                PlayerHandler.players[i].process();
                while (PlayerHandler.players[i].packetProcess()) ; //Dodian's way of handling packets..Omegalul!
                PlayerHandler.players[i].postProcessing();
                PlayerHandler.players[i].getNextPlayerMovement();
            }
        }
        // after processing update!
        for (int i = 0; i < Constants.maxPlayers; i++) {
            if (PlayerHandler.players[i] == null) //Hate continue; in a loop! Dodian do it this way..*yikes*
                continue;
            if (!PlayerHandler.players[i].isActive || PlayerHandler.players[i].getPlayerName() == null || PlayerHandler.players[i].getPlayerName().equals("null")) //Hate continue; in a loop! Dodian do it this way..*yikes*
                continue;
            /* Removing non-responding player */
            long lp = currentTime - PlayerHandler.players[i].lastPacket;
            if ((PlayerHandler.players[i].dbId < 1 && lp >= 15000) || (PlayerHandler.players[i].dbId > 0 && lp >= 60000)) { //Removing non-responding player
                PlayerHandler.players[i].disconnected = true;
                //System.out.println("Remove disconnect from update!.." + PlayerHandler.players[i].getPlayerName());
            }
            /* Disconnect a user check! If not disconnect update! */
            if (PlayerHandler.players[i].disconnected) {
                //System.out.println("Remove player!.." + PlayerHandler.players[i].getPlayerName());
                Server.playerHandler.removePlayer(PlayerHandler.players[i]);
                PlayerHandler.players[i] = null;
            } else {
                if(!PlayerHandler.players[i].initialized) {
                    PlayerHandler.players[i].initialize();
                    PlayerHandler.players[i].initialized = true;
                } else PlayerHandler.players[i].update();
                if (Server.updateRunning && PlayerHandler.players[i].updateAnnounced)
                    PlayerHandler.players[i].updateAnnounced = true;
            }
        }
        /* Server update! */
        if(Server.updateRunning) Server.updateElapsed += 0.6;
        if (Server.updateRunning
                && currentTime - Server.updateStartTime > (Server.updateSeconds * 1000L)) {
            if (PlayerHandler.getPlayerCount() < 1) {
                System.exit(0);
            }
        }
        /* Clear player update! */
        for (int i = 0; i < Constants.maxPlayers; i++) {
            if (PlayerHandler.players[i] == null || !PlayerHandler.players[i].isActive) //Hate continue; in a loop! Dodian do it this way..*yikes*
                continue;
            PlayerHandler.players[i].clearUpdateFlags();
        }
    }

    public void groundProcess() {
        if (Ground.items.size() < 0) {
            return;
        }
        long now = System.currentTimeMillis();
        for (GroundItem item : Ground.items) {
            if (!item.canDespawn && item.taken && now - item.dropped >= item.timeDisplay) {
                item.taken = false;
                item.visible = false;
            }
            if (!item.visible && (now - item.dropped >= item.timeDisplay || !item.canDespawn) || (!item.visible && !item.canDespawn && !item.taken) && (now - item.dropped >= item.timeDisplay || !item.canDespawn)) {
                for (int i = 0; i < PlayerHandler.players.length; i++) {
                    Client p = Server.playerHandler.getClient(i);
                    if (p != null && Server.itemManager.isTradable(item.id) && p.dbId != item.playerId
                            && Math.abs(p.getPosition().getX() - item.x) <= 114 && Math.abs(p.getPosition().getY() - item.y) <= 114 && p.getPosition().getZ() == item.z) {
                        p.send(new CreateGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
                    }
                }
                item.visible = true;
            }
            if (item.canDespawn && item.visible && now - item.dropped >= (item.timeDisplay + item.timeDespawn)) {
                Ground.deleteItem(item);
            }
        }
    }

}