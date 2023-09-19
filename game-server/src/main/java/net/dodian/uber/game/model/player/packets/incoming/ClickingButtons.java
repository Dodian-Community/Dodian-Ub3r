package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.combat.impl.CombatStyleHandler;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Emotes;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.item.GroundItem;
import net.dodian.uber.game.model.player.content.Skillcape;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveInterfaces;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.packets.outgoing.SendString;
import net.dodian.uber.game.model.player.quests.QuestSend;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.prayer.Prayers;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Utils;

import java.io.IOException;

import static net.dodian.utilities.DotEnvKt.getServerDebugMode;

public class ClickingButtons implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int actionButton = Utils.HexToInt(client.getInputStream().buffer, 0, packetSize);
        if (getServerDebugMode()) {
            client.println("button=" + actionButton);
        }
        if (System.currentTimeMillis() - client.lastButton < 600 || !client.validClient) { //To prevent some shiez!
            client.lastButton = System.currentTimeMillis();
            return;
        }
        if(!(actionButton >= 9157 && actionButton <= 9194))
            client.actionButtonId = actionButton;
        client.resetAction(false);
        CombatStyleHandler.setWeaponHandler(client, actionButton);
        if (client.duelButton(actionButton)) {
            return;
        }
        Prayers.Prayer prayer = Prayers.Prayer.forButton(actionButton);
        if (prayer != null) {
            client.getPrayerManager().togglePrayer(prayer);
            return;
        }
        if(QuestSend.questMenu(client)) {
            return;
        }
        if(client.refundSlot != -1) { //Refund code!
            int size = client.rewardList.size();
            int checkSlot = 1;
            int position = size - client.refundSlot;
            if(actionButton == 9158 || actionButton == 9168 || actionButton == 9179 || actionButton == 9191)
                checkSlot = 2;
            else if(actionButton == 9169 || actionButton == 9180 || actionButton == 9192)
                checkSlot = 3;
            else if(actionButton == 9181 || actionButton == 9193)
                checkSlot = 4;
            else if(actionButton == 9194)
                checkSlot = 5;
            if(client.refundSlot == 0 && ((size > 3 && checkSlot == 5) || (size == 3 && checkSlot == 4) || (size == 1 && checkSlot == 2) || (size == 2 && checkSlot == 3))) { //Close!
                client.refundSlot = -1;
                client.send(new RemoveInterfaces());
            } else if(((client.refundSlot == 0 && position > 3) || position > 3) && checkSlot == 4)
                client.refundSlot += 3;
            else if(client.refundSlot != 0 && ((position <= 3 && checkSlot == position + 1) || (position > 3 && checkSlot == 5)))
                client.refundSlot -= 3;
            else client.reclaim(checkSlot);
            if(!client.rewardList.isEmpty())
                client.setRefundOptions();
            return;
        }
        Emotes.doEmote(actionButton, client);
        switch (actionButton) {
            case 58073:
                client.send(new SendMessage("Visit the Dodian.net UserCP and click edit pin to remove your pin"));
                break;
            case 151:
                client.NpcDialogue = 27;
                client.NpcDialogueSend = false;
                break;
            case 150:
                client.NpcDialogue = 26;
                client.NpcDialogueSend = false;
                break;
            case 8198:
                Balloons.acceptItems(client);
                break;
            case 83093:
                client.sendFrame248(21172, 3213);
                break;
            case 83051:
            case 9118:
            case 19022:
                client.send(new RemoveInterfaces());
                break;
            case 24136:
                client.yellOn = true;
                client.send(new SendMessage("You enabled the boss yell messages."));
                break;
            case 24137:
                client.yellOn = false;
                client.send(new SendMessage("You disabled the boss yell messages."));
                break;
            case 89223: // TODO: Check what this button do!
                for (int i = 0; i < client.playerItems.length; i++) {
                    client.bankItem(client.playerItems[i], i, client.playerItemsN[i]);
                }
		    break;
            case 3056: //Small tree
            case 3057: //Big Tree
            case 3058: //Mountain
            case 3059: //Castle
            case 3060: //Tent
            case 48054: //totem!
                int pos = client.skillX == 2772 && client.skillY == 3235 ? 5:
                client.skillX == 2864 && client.skillY == 2971 ? 4:
                client.skillX == 3511 && client.skillY == 3505 ? 2: 0;
                client.travelTrigger(pos);
                break;
            case 84237: //Home teleport aka Yanille
                client.triggerTele(2606, 3102, 0, false);
                break;
            case 50235: //Seers
                client.triggerTele(2723, 3485, 0, false);
                break;
            case 50245: //Ardougne
                client.triggerTele(2662, 3309, 0, false);
                break;
            case 50253: // Catherby
                client.triggerTele(2804, 3434, 0, false);
                break;
            case 51005: //Legends guild
                client.triggerTele(2728, 3346, 0, false);
                break;
            case 51013: //Taverly
                client.triggerTele(2895, 3457, 0, false);
                break;
            case 51023: //Fishing guild
                client.triggerTele(2597, 3409, 0, true);
                break;
            case 51031: //Gnome village
                client.triggerTele(2472, 3438, 0, false);
                break;
            case 51039: //Empty teleport
                client.triggerTele(3087, 3492, 0, false);
                break;
            case 74212:
            case 49047: // old magic on
            case 49046: // old magic off
                if (client.ancients == 1) {
                    client.setSidebarInterface(6, 1151); // magic tab (ancient =
                    // 12855);
                    client.ancients = 0;
                    client.send(new SendMessage("Normal magic enabled"));
                } else {
                    client.setSidebarInterface(6, 12855); // magic tab (ancient =
                    // 12855);
                    client.ancients = 1;
                    client.send(new SendMessage("Ancient magic enabled"));
                }
                break;
            case 26076:
                // frame36(6575, 1);
                break;
            case 53245:
            case 53246:
            case 53247:
            case 53248:
            case 53249:
            case 53250:
            case 53251:
            case 53252:
            case 53253:
            case 53254:
            case 53255:
                client.duelButton2(client.actionButtonId - 53245);
                break;
            case 54074:
                Server.slots.playSlots(client, -1);
                break;
            case 25120:
                if (System.currentTimeMillis() - client.lastButton < 1000) {
                    client.lastButton = System.currentTimeMillis();
                    break;
                } else {
                    client.lastButton = System.currentTimeMillis();
                }
                Client dw = client.getClient(client.duel_with);
                /*
                 * Danno: Sometimes dcs a player. So we break if other player is null.
                 */
                if (dw == null)
                    break;
                client.canOffer = false;
                if (!client.validClient(client.duel_with)) {
                    client.declineDuel();
                }
                if (client.duelConfirmed2) {
                    break;
                }
                client.duelConfirmed2 = true;
                if (dw.duelConfirmed2) {
                    client.removeEquipment();
                    dw.removeEquipment();
                    client.startDuel();
                    dw.startDuel();
                } else {
                    client.send(new SendString("Waiting for other player...", 6571));
                    dw.send(new SendString("Other player has accepted", 6571));
                }
                break;
            case 15147: // bronze
            case 15146:
            case 10247:
            case 9110:
            case 15151: // iron
            case 15150:
            case 15149:
            case 15148:
            case 15155: // silver
            case 15154:
            case 15153:
            case 15152:
            case 15159: // steel
            case 15158:
            case 15157:
            case 15156:
            case 15163: // gold
            case 15162:
            case 15161:
            case 15160:
            case 29017: // mithril
            case 29016:
            case 24253:
            case 16062:
            case 29022: // addy
            case 29020:
            case 29019:
            case 29018:
            case 29026: // rune
            case 29025:
            case 29024:
            case 29023:
                client.startSmelt(client.actionButtonId);
                break;
            case 34185:
            case 34184: // vamps
            case 34183:
            case 34182:
            case 34189: // chaps
            case 34188:
            case 34187:
            case 34186:
            case 34193:
            case 34192:
            case 34191:
            case 34190:
                client.startHideCraft(client.actionButtonId);
                break;
            case 33187: // armor
            case 33186:
            case 33185:
            case 33190: // gloves
            case 33189:
            case 33188:
            case 33193: // boots
            case 33192:
            case 33191:
            case 33196: // vamps
            case 33195:
            case 33194:
            case 33199: // chaps
            case 33198:
            case 33197:
            case 33202: // coif
            case 33201:
            case 33200:
            case 33205:// cowl
            case 33204:
            case 33203:
                client.startCraft(client.actionButtonId);
            break;
            case 57225:
                client.startTan(1, 0);
            break;
            case 57217:
                client.startTan(5, 0);
                break;
            case 57201:
            case 57209:
                client.startTan(27, 0);
            break;
            case 57229: //Hard leather!
                client.startTan(1, 1);
            break;
            case 57221:
                client.startTan(5, 1);
            break;
            case 57205:
            case 57213:
                client.startTan(27, 1);
            break;
            case 57227:
                client.startTan(1, 2);
                break;
            case 57219:
                client.startTan(5, 2);
                break;
            case 57211:
            case 57203:
                client.startTan(27, 2);
                break;
            case 57228:
                client.startTan(1, 3);
                break;
            case 57220:
                client.startTan(5, 3);
                break;
            case 57212:
            case 57204:
                client.startTan(27, 3);
                break;
            case 57231:
                client.startTan(1, 4);
                break;
            case 57223:
                client.startTan(5, 4);
                break;
            case 57215:
            case 57207:
                client.startTan(27, 4);
                break;
            case 57232:
                client.startTan(1, 5);
                break;
            case 57224:
                client.startTan(5, 5);
                break;
            case 57216:
            case 57208:
                client.startTan(27, 5);
                break;
            case 10239: //make stuff 1
                client.fletching.fletchOther(client, 1);
                break;
            case 10238: //make stuff 5
                client.fletching.fletchOther(client, 5);
                break;
            case 6212: //make stuff 10 (x)
                client.fletching.fletchOther(client, 10);
                break;
            case 6211: //make stuff 28 (all)
                client.fletching.fletchOther(client, 28);
                break;
            case 34170:
                client.fletching.fletchBow(client, true, 1);
                break;
            case 34169:
                client.fletching.fletchBow(client, true, 5);
                break;
            case 34168:
                client.fletching.fletchBow(client, true, 10);
                break;
            case 34167:
                client.fletching.fletchBow(client, true, 27);
                break;
            case 34174: // 1
                client.fletching.fletchBow(client, false, 1);
                break;
            case 34173: // 5
                client.fletching.fletchBow(client, false, 5);
                break;
            case 34172: // 10
                client.fletching.fletchBow(client, false, 10);
                break;
            case 34171:
                client.fletching.fletchBow(client, false, 27);
                break;
            case 10252:
            case 11000:
            case 10253:
            case 11001:
            case 10254:
            case 10255:
            case 11002:
            case 11011:
            case 11013:
            case 11014:
            case 11010:
            case 11012:
            case 11006:
            case 11009:
            case 11008:
            case 11004:
            case 11003:
            case 11005:
            case 47002:
            case 54090:
            case 11007:
                if (client.genie) {
                    int[] skillTrain = {
                            10252, 11000, 10253, 11001, 10254, 11002, 10255, 11011,
                            11013, 11014, 11010, 11012, 11006, 11009, 11008, 11004,
                            11003, 11005, 47002, 54090, 11007
                    };
                    client.send(new RemoveInterfaces());
                    client.genie = false;
                    if (client.inDuel || client.duelFight || client.IsBanking || client.checkBankInterface || !client.playerHasItem(2528)) //To prevent stuff!
                        break;
                    for (int i = 0; i < skillTrain.length; i++) {
                        Skill trainedSkill = Skill.getSkill(i);
                        if (trainedSkill != null && skillTrain[i] == client.actionButtonId && client.actionButtonId != 54090) {
                            client.deleteItem(2528, 1);
                            int level = Skills.getLevelForExperience(client.getExperience(trainedSkill));
                            int experience = 250 * level;
                            client.giveExperience(experience, trainedSkill);
                            client.send(new SendMessage("You rub the lamp and gained " + experience + " experience in " + trainedSkill.getName() + "."));
                        }
                    }
                } else if (client.randomed && client.actionButtonId == client.statId[client.random_skill]) {
                    client.randomed = false;
                    client.send(new RemoveInterfaces());
                    if (!client.addItem(2528, 1)) {
                        GroundItem item = new GroundItem(client.getPosition(), 2528, 1, client.getSlot(), -1);
                        Ground.items.add(item);
                        client.send(new SendMessage("You dropped the lamp on the floor!"));
                    }
                }
                break;
            case 1097:
            case 1094:
            case 1093:
                client.setSidebarInterface(0, 1689);
                break;
            case 51133:
            case 51185:
            case 51091:
            case 24018:
            case 51159:
            case 51211:
            case 51111:
            case 51069:
            case 51146:
            case 51198:
            case 51102:
            case 51058:
            case 51172:
            case 51224:
            case 51122:
            case 51080:
                for (int index = 0; index < client.ancientButton.length; index++) {
                    if (client.actionButtonId == client.ancientButton[index]) {
                        client.autocast_spellIndex = index;
                        CombatStyleHandler.setWeaponHandler(client, -1);
                        //client.debug("autocast_spellIndex=" + client.autocast_spellIndex);
                        break;
                    }
                }
                break;
            case 24017:
                CombatStyleHandler.setWeaponHandler(client, -1);
                break;

            case 2171: // Retribution
                break;

            case 14067: //Apperance accepted!
                client.send(new RemoveInterfaces());
                client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
                break;

            case 152:
                client.buttonOnRun = false;
                break;
            case 153:
                client.buttonOnRun = true;
                break;

            case 130: // close interface
                client.println_debug("Closing Interface");
                break;

            case 1177: // Gmaul!
            case 9125: // Accurate
            case 22228: // punch (unarmed)
            case 48010: // flick (whip)
            case 21200: // spike (pickaxe)
            case 1080: // bash (staff)
            case 6168: // chop (axe)
            case 6236: // accurate (long bow)
            case 17102: // accurate (darts)
            case 8234: // stab (dagger)
                client.FightType = 0;
                break;

            case 1175: // Gmaul!
            case 9126: // Defensive
            case 48008: // deflect (whip)
            case 22229: // block (unarmed)
            case 21201: // block (pickaxe)
            case 1078: // focus - block (staff)
            case 6169: // block (axe)
            case 33019: // fend (hally)
            case 18078: // block (spear)
            case 8235: // block (dagger)
                client.FightType = 1;
                break;

            case 9127: // Controlled
            case 48009: // lash (whip)
            case 33018: // jab (hally)
            case 6234: // longrange (long bow)
            case 18077: // lunge (spear)
            case 18080: // swipe (spear)
            case 18079: // pound (spear)
            case 17100: // longrange (darts)
                client.FightType = 3;
                // client.SkillID = 3;
                break;

            case 1176: // Gmaul!
            case 9128: // Aggressive
            case 22230: // kick (unarmed)
            case 21203: // impale (pickaxe)
            case 21202: // smash (pickaxe)
            case 1079: // pound (staff)
            case 6171: // hack (axe)
            case 6170: // smash (axe)
            case 33020: // swipe (hally)
            case 6235: // rapid (long bow)
            case 17101: // repid (darts)
            case 8237: // lunge (dagger)
            case 8236: // slash (dagger)
                client.FightType = 2;
                // client.SkillID = 2;
                break;

            case 9154: // Log out
                if (System.currentTimeMillis() < client.walkBlock) {
                    client.send(new SendMessage("You are unable to logout right now."));
                    break;
                }
                if (client.isInCombat()) {
                    client.send(new SendMessage("You must wait until you are out of combat before logging out!"));
                    break;
                }
                if (System.currentTimeMillis() - client.getLastCombat() <= 7000 && client.inWildy()) {
                    client.send(new SendMessage("You must wait 7 seconds after npc combat to logout."));
                    break;
                }
                if (System.currentTimeMillis() - client.lastPlayerCombat <= 30000 && client.inWildy()) {
                    client.send(new SendMessage("You must wait 30 seconds after combat in the wilderness to logout."));
                    client.send(new SendMessage("If you X out or disconnect you will stay online for up to a minute"));
                    break;
                }
                // if(currentHealth > 0)
                client.logout();
                break;

            case 21011:
                if(client.IsBanking) {
                    client.takeAsNote = !client.takeAsNote;
                    client.send(new SendString(client.takeAsNote ? "No Note" : "Note", 5389));
                    client.send(new SendMessage(client.takeAsNote ? "You can now note items." : "You can no longer note items."));
                }
                break;
            case 21010:
                if(client.IsBanking) {
                    if (client.freeSlots() < 28) {
                        for (int i = 0; i < 28; i++)
                            if (client.playerItems[i] > 0)
                                client.bankItem(client.playerItems[i] - 1, i, client.playerItemsN[i]);
                        client.send(new SendMessage("You bank all your items!"));
                    } else
                        client.send(new SendMessage("You do not have anything that can be banked!"));
                }
                break;

            case 13092:
                try {
                    Client other = client.getClient(client.trade_reqId);
                    if (other == null || !client.validClient(client.trade_reqId) || System.currentTimeMillis() - client.lastButton < 600 || !client.inTrade) {
                        break;
                    }
                    client.lastButton = System.currentTimeMillis();
                    if (client.inTrade && !client.tradeConfirmed) {
                        client.tradeConfirmed = true;
                        if (other != null && other.tradeConfirmed) {
                            if (!other.hasTradeSpace() || !client.hasTradeSpace()) {
                                client.send(new SendMessage(client.failer));
                                other.send(new SendMessage(client.failer));
                                client.declineTrade();
                                return;
                            }
                            client.confirmScreen();
                            other.confirmScreen();
                            break;
                        }
                        client.send(new SendString("Waiting for other player...", 3431));
                        if (other != null && client.validClient(client.trade_reqId)) {
                            other.send(new SendString("Other player has accepted", 3431));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case 13218:
                try {
                    Client other = client.getClient(client.trade_reqId);
                    if (other == null || !client.validClient(client.trade_reqId) || System.currentTimeMillis() - client.lastButton < 600 || !client.inTrade) {
                        break;
                    }
                    client.lastButton = System.currentTimeMillis();
                    if (client.inTrade && client.tradeConfirmed && other.tradeConfirmed && !client.tradeConfirmed2) {
                        client.tradeConfirmed2 = true;
                        if (other.tradeConfirmed2) {
                            client.giveItems();
                            other.giveItems();
                            break;
                        }
                        other.send(new SendString("Other player has accepted.", 3535));
                        client.send(new SendString("Waiting for other player...", 3535));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case 9157:
                client.triggerChat(1);
                if (client.NpcDialogue == 2) {
                    client.NpcDialogue = 0;
                    client.NpcDialogueSend = false;
                    client.openUpBank();
                } else if (client.NpcDialogue == 4) { // Aubury
                    client.NpcDialogue = 0;
                    client.NpcDialogueSend = false;
                    client.openUpShop(2);
                } else if (client.NpcDialogue == 1001) { // Aubury
                    client.getOutputStream().createFrame(27);
                } else if (client.NpcDialogue == 22) { // Makeover Mage
                    client.NpcDialogue = 23;
                    client.NpcDialogueSend = false;
                } else if (client.NpcDialogue == 26) {
                    client.specsOn = true;
                    client.send(new SendMessage("You have enabled specials."));
                    client.send(new RemoveInterfaces());
                    client.NpcDialogue = 0;
                    client.NpcDialogueSend = false;
                } else if (client.NpcDialogue == 27) {
                    client.yellOn = true;
                    client.send(new SendMessage("You have enabled boss yell messages."));
                    client.send(new RemoveInterfaces());
                    client.NpcDialogue = 0;
                    client.NpcDialogueSend = false;
                } else if (client.NpcDialogue == 9) { // mage arena
                    if (client.determineCombatLevel() >= 80) {
                        client.moveTo(3105, 3933, 0);
                        client.send(new RemoveInterfaces());
                    } else {
                        client.send(new SendMessage("You need to be level 80 or above to enter the mage arena."));
                        client.send(new SendMessage("The skeletons at the varrock castle are a good place until then."));
                    }
                }
                break;

            case 9158:
                client.triggerChat(2);
                if (client.NpcDialogue == 2) {
                    client.NpcDialogue = 0;
                    client.NpcDialogueSend = false;
                } else if (client.NpcDialogue == 4) {
                    client.NpcDialogue = 5;
                    client.NpcDialogueSend = false;
                } else if (client.NpcDialogue == 22) { // Makeover Mage
                    client.NpcDialogue = 24;
                    client.NpcDialogueSend = false;
                } else if (client.NpcDialogue == 1001) { // dice
                    client.setInterfaceWalkable(-1);
                    client.send(new RemoveInterfaces());
                } else if (client.NpcDialogue == 26) {
                    client.specsOn = false;
                    client.send(new SendMessage("You have disabled specials."));
                    client.send(new RemoveInterfaces());
                    client.NpcDialogue = 0;
                    client.NpcDialogueSend = false;
                } else if (client.NpcDialogue == 27) {
                    client.yellOn = false;
                    client.send(new SendMessage("You have disabled boss yell messages."));
                    client.send(new RemoveInterfaces());
                    client.NpcDialogue = 0;
                    client.NpcDialogueSend = false;
                }
                break;

            case 9167:
            case 9178:
            case 9190:
                client.triggerChat(1);
                break;
            case 9168:
            case 9179:
            case 9191:
                client.triggerChat(2);
                break;
            case 9169:
            case 9180:
            case 9192:
                client.triggerChat(3);
                break;
            case 9181:
            case 9193:
                client.triggerChat(4);
                break;
            case 9194:
                client.triggerChat(5);
                break;

            case 7212:
                client.setSidebarInterface(0, 328);
                break;
            case 26018:
                if (!client.inDuel || !client.validClient(client.duel_with)) {
                    break;
                }
                Client o = client.getClient(client.duel_with);
                boolean sendMsgToOther = client.getMaxHealth() - client.getCurrentHealth() == 0 && o.getMaxHealth() - o.getCurrentHealth() != 0;
                if (o.getMaxHealth() - o.getCurrentHealth() != 0 || client.getMaxHealth() - client.getCurrentHealth() != 0) {
                    client.send(new SendMessage(sendMsgToOther ? "Your opponent is low on health!" : "You are low on health, so please heal up!"));
                    if(sendMsgToOther)
                        o.send(new SendMessage("You are low on health, so please heal up!"));
                    break;
                }
                if (System.currentTimeMillis() - client.lastButton < 1000) {
                    client.lastButton = System.currentTimeMillis();
                    break;
                } else {
                    client.lastButton = System.currentTimeMillis();
                }
                if (client.duelConfirmed) {
                    break;
                }
                client.duelConfirmed = true;
                client.canOffer = false;
                if (o.duelConfirmed) {
                    /*
                     * Danno: Fix; stop a duel with all combat styles disabled.
                     */
                    if (client.duelRule[0] && client.duelRule[1] && client.duelRule[2]) {
                        client.declineDuel();
                        client.send(new SendMessage("At least one combat style must be enabled!"));
                        o.send(new SendMessage("At least one combat style must be enabled!"));
                        return;
                    }
                    if (!client.hasEnoughSpace() || !o.hasEnoughSpace()) {
                        client.send(new SendMessage(client.failer));
                        o.send(new SendMessage(client.failer));
                        client.declineDuel();
                        return;
                    }
                    client.canOffer = false;
                    o.canOffer = false;
                    client.confirmDuel();
                    o.confirmDuel();
                } else {
                    client.send(new SendString("Waiting for other player...", 6684));
                    o.send(new SendString("Other player has accepted.", 6684));
                }

                break;

            /*
             * case 33206: try { client.showSkillMenu(0, 0); } catch (IOException e) { }
             * break; case 33212: try { client.showSkillMenu(1, 0); } catch (IOException
             * e) { } break; case 33215: try { client.showSkillMenu(4, 0); } catch
             * (IOException e) { } break; case 33216: try { client.showSkillMenu(17, 0);
             * } catch (IOException e) { } break; case 34142: try { if
             * (client.currentSkill < 2) client.showSkillMenu(0, 0); else if
             * (client.currentSkill == 4) client.showSkillMenu(4, 0); } catch
             * (IOException e) { } break; case 34119: try { if (client.currentSkill < 2)
             * client.showSkillMenu(1, 0); else if (client.currentSkill == 4)
             * client.showSkillMenu(4, 1); } catch (IOException e) { } break; case
             * 34120: try { if (client.currentSkill < 2) client.showSkillMenu(4, 0); }
             * catch (IOException e) { } break; case 34123: // try { if
             * (client.currentSkill < 2) client.send(new SendMessage("Coming soon!"));
             * // showSkillMenu(6, 0); // } catch (IOException e) { } break;
             */

            case 33206:
                try {
                    client.showSkillMenu(0, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 33207:
                try {
                    client.showSkillMenu(3, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 33208:
                try {
                    client.showSkillMenu(Skill.MINING.getId(), 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 33209:
                try {
                    client.showSkillMenu(2, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 33210:
                try {
                    client.showSkillMenu(16, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 33212:
                try {
                    client.showSkillMenu(1, 0);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                break;
            case 33215:
                try {
                    client.showSkillMenu(4, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 33216:
                try {
                    client.showSkillMenu(17, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 33213:
                try {
                    client.showSkillMenu(Skill.HERBLORE.getId(), 0);
                } catch (IOException e5) {
                    e5.printStackTrace();
                }
                break;
            case 33219: //Crafting
                try {
                    client.showSkillMenu(Skill.CRAFTING.getId(), 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 33211: //Smithing
                try {
                    client.showSkillMenu(Skill.SMITHING.getId(), 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 33220:
                try {
                    client.showSkillMenu(8, 0);
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
                break;
            case 33221:
                try {
                    client.showSkillMenu(6, 0);
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
                break;
            case 33222:
                try {
                    client.showSkillMenu(11, 0);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                break;
            case 33223:
                try {
                    client.showSkillMenu(7, 0);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                break;
            case 33224:
                try {
                    client.showSkillMenu(22, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 33214:
                try {
                    client.showSkillMenu(9, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 33217:
                try {
                    client.showSkillMenu(10, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 34142:
                try {
                    if (client.currentSkill < 2)
                        client.showSkillMenu(0, 0);
                    else
                        client.showSkillMenu(client.currentSkill, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 34119:
                try {
                    if (client.currentSkill < 2)
                        client.showSkillMenu(1, 0);
                    else
                        client.showSkillMenu(client.currentSkill, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 34120:
                if (client.currentSkill < 2)
                    try {
                        client.showSkillMenu(4, 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                else
                    try {
                        client.showSkillMenu(client.currentSkill, 2);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                break;
            case 34123:
                if (client.currentSkill < 2)
                    client.send(new SendMessage("Coming soon!"));
                else
                    try {
                        client.showSkillMenu(client.currentSkill, 3);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                break;
            case 34133:
                try {
                    client.showSkillMenu(client.currentSkill, 4);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 34136:
                try {
                    client.showSkillMenu(client.currentSkill, 5);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 34139:
                try {
                    client.showSkillMenu(client.currentSkill, 6);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 34155:
                try {
                    client.showSkillMenu(client.currentSkill, 7);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 47130:
                try {
                    client.showSkillMenu(18, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case 88060: //Idea
                client.requestAnim(4276, 0);
                client.gfx0(712);
                break;
            case 88061: //Stomp
                client.requestAnim(4278, 0);
                client.gfx0(713);
                break;
            case 88062: //Flap
                client.requestAnim(4280, 0);
                break;
            case 88063: //Slap head
                client.requestAnim(4275, 0);
                break;
            case 59062: //Scared
                client.requestAnim(2836, 0);
                break;
            case 72254: //Bunny hop
                client.requestAnim(6111, 0);
                break;
            case 72033: //Zombie dance
                client.requestAnim(3543, 0);
                break;
            case 72032: //Zombie walk
                client.requestAnim(3544, 0);
            break;
            case 74108:
                int id = client.getEquipment()[Equipment.Slot.CAPE.getId()];
                Skillcape skillcape = Skillcape.getSkillCape(id);
                if (skillcape != null) {
                    client.requestAnim(skillcape.getEmote(), 0);
                    client.gfx0(skillcape.getGfx());
                } else if (id == 9813) { //Questpoint cape
                    client.requestAnim(4945, 0);
                    client.gfx0(816);
                } else if (client.GetItemName(id).toLowerCase().startsWith("max cape")) { //Max cape
                    skillcape = Skillcape.getRandomCape();
                    client.requestAnim(skillcape.getEmote(), 0);
                    client.gfx0(skillcape.getGfx());
                } else {
                    client.send(new SendMessage("You need to be wearing a skillcape to do that!"));
                }
                break;

            default:
                // System.out.println("Player stands in: X="+absX+" Y="+absY);
                if (client.playerRights > 1) {
                    client.println_debug("Case 185: Action Button: " + client.actionButtonId);
                }
                break;
        }
    }

}
