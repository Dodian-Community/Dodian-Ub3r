package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.EntityUpdating;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Stream;
import net.dodian.utilities.Utils;

/**
 * @author blakeman8192
 * @author lare96 <<a href="http://github.com/lare96">...</a>>
 * @author Dashboard
 */
public class PlayerUpdating extends EntityUpdating<Player> {

    private static final PlayerUpdating instance = new PlayerUpdating();

    public static PlayerUpdating getInstance() {
        return instance;
    }

    private Stream updateBlock = new Stream(new byte[10000]);
    @Override
    public void update(Player player, Stream stream) {
        updateBlock.currentOffset = 0;

        if (Server.updateRunning) {
            stream.createFrame(114);
            int seconds = Server.updateSeconds - (int)Server.updateElapsed;
            stream.writeWordBigEndian(seconds * 50 / 30);
        }

        updateLocalPlayerMovement(player, stream);
        boolean saveChatTextUpdate = player.getUpdateFlags().isRequired(UpdateFlag.CHAT);
        player.getUpdateFlags().setRequired(UpdateFlag.CHAT, false);
        appendBlockUpdate(player, updateBlock);
        player.getUpdateFlags().setRequired(UpdateFlag.CHAT, saveChatTextUpdate);
        if (player.loaded) {
            stream.writeBits(8, player.playerListSize);
            int size = player.playerListSize;
            player.playersUpdating.clear();
            player.playerListSize = 0;
            for (int i = 0; i < size; i++) {
                if (player.playerList[i] != null && player.loaded && !player.playerList[i].didTeleport() && !player.didTeleport()
                        && player.withinDistance(player.playerList[i])) {
                    player.playerList[i].updatePlayerMovement(stream);
                    appendBlockUpdate(player.playerList[i], updateBlock);
                    player.playerList[player.playerListSize++] = player.playerList[i];
                    player.playersUpdating.add(player.playerList[i]);
                } else {
                    stream.writeBits(1, 1);
                    stream.writeBits(2, 3);
                }
            }

            for (int i = 0; i < Constants.maxPlayers; i++) {
                if (PlayerHandler.players[i] == null || !PlayerHandler.players[i].isActive || PlayerHandler.players[i] == player || !player.loaded)
                    continue;
                if (!player.withinDistance(PlayerHandler.players[i]) || (!player.didTeleport() && player.playersUpdating.contains(PlayerHandler.players[i])))
                    continue;
                if ((PlayerHandler.players[i].invis && !player.invis)) //Instance check!
                    continue;
                player.addNewPlayer(PlayerHandler.players[i], stream, updateBlock);
            }
        } else {
            stream.writeBits(8, 0);

        }

        if (updateBlock.currentOffset > 0) {
            stream.writeBits(11, 2047);
            stream.finishBitAccess();
            stream.writeBytes(updateBlock.buffer, updateBlock.currentOffset, 0);
        } else {
            stream.finishBitAccess();
        }
        stream.endFrameVarSizeWord();
    }


    public void updateLocalPlayerMovement(Player player, Stream stream) {
        /* Noob! */
        if(player.didMapRegionChange()) {
            stream.createFrame(73);
            stream.writeWordA(player.mapRegionX + 6);
            stream.writeWord(player.mapRegionY + 6);
            ((Client)player).updateItems();
        }

        if (player.didTeleport()) {
            ((Client)player).updateItems(); //Need this?!
            stream.createFrameVarSizeWord(81);
            stream.initBitAccess();
            stream.writeBits(1, 1);
            stream.writeBits(2, 3); // updateType
            stream.writeBits(2, player.getPosition().getZ());
            stream.writeBits(1, 1);
            stream.writeBits(1, player.getUpdateFlags().isUpdateRequired() ? 1 : 0);
            stream.writeBits(7, player.getCurrentY());
            stream.writeBits(7, player.getCurrentX());
            return;
        } //Not sure if we need to stop here?!
        if (player.getPrimaryDirection() == -1) {
            stream.createFrameVarSizeWord(81);
            stream.initBitAccess();
            if (player.getUpdateFlags().isUpdateRequired()) {
                stream.writeBits(1, 1);
                stream.writeBits(2, 0);
            } else
                stream.writeBits(1, 0);
        } else {
            stream.createFrameVarSizeWord(81);
            stream.initBitAccess();
            stream.writeBits(1, 1);
            if (player.getSecondaryDirection() == -1) {
                stream.writeBits(2, 1);
                stream.writeBits(3, Utils.xlateDirectionToClient[player.getPrimaryDirection()]);
                stream.writeBits(1, player.getUpdateFlags().isUpdateRequired() ? 1 : 0);
            } else {
                stream.writeBits(2, 2);
                stream.writeBits(3, Utils.xlateDirectionToClient[player.getPrimaryDirection()]);
                stream.writeBits(3, Utils.xlateDirectionToClient[player.getSecondaryDirection()]);
                stream.writeBits(1, player.getUpdateFlags().isUpdateRequired() ? 1 : 0);
            }
        }
    }


    @Override
    public void appendBlockUpdate(Player player, Stream stream) {
        if (!player.getUpdateFlags().isUpdateRequired() && !player.getUpdateFlags().isRequired(UpdateFlag.CHAT))
            return; // nothing required

        int updateMask = 0;
        for (UpdateFlag flag : player.getUpdateFlags().keySet()) {
            if (player.getUpdateFlags().isRequired(flag)) {
                updateMask |= flag.getMask(player.getType());
            }
        }

        if (updateMask >= 0x100) {
            updateMask |= 0x40;
            stream.writeByte(updateMask & 0xFF);
            stream.writeByte(updateMask >> 8);
        } else
            stream.writeByte(updateMask);

        if (player.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS))
            appendGraphic(player, stream);
        if (player.getUpdateFlags().isRequired(UpdateFlag.ANIM))
            appendAnimationRequest(player, stream);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT))
            appendForcedChatText(player, stream);
        if (player.getUpdateFlags().isRequired(UpdateFlag.CHAT))
            appendPlayerChatText(player, stream);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_MOVEMENT))
            player.appendMask400Update(stream);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER))
            appendFaceCharacter(player, stream);
        if (player.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE))
            appendPlayerAppearance(player, stream);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE))
            appendFaceCoordinates(player, stream);
        if (player.getUpdateFlags().isRequired(UpdateFlag.HIT))
            appendPrimaryHit(player, stream);
        if (player.getUpdateFlags().isRequired(UpdateFlag.HIT2))
            appendPrimaryHit2(player, stream);
    }

    public void appendGraphic(Player player, Stream stream) {
        stream.writeWordBigEndian(player.getGraphicId());
        stream.writeDWord(player.getGraphicHeight());
    }

    @Override
    public void appendAnimationRequest(Player player, Stream stream) {
        stream.writeWordBigEndian(player.getAnimationId());
        stream.writeByteC(player.getAnimationDelay());
    }

    public static void appendForcedChatText(Player player, Stream stream) {
        stream.writeString(player.getForcedChat());
    }

    public static void appendPlayerChatText(Player player, Stream stream) {
        stream.writeWordBigEndian(((player.getChatTextColor() & 0xFF) << 8) + (player.getChatTextEffects() & 0xFF));
        stream.writeByte(player.playerRights);
        stream.writeByteC(player.getChatTextSize());
        stream.writeBytes_reverse(player.getChatText(), player.getChatTextSize(), 0);
    }

    @Override
    public void appendFaceCharacter(Player player, Stream stream) {
        stream.writeWordBigEndian(player.getFaceTarget());
    }

    public static void appendPlayerAppearance(Player player, Stream str) {
        Stream playerProps = new Stream(new byte[128]);
        playerProps.currentOffset = 0;
        playerProps.writeByte(player.getGender());
        playerProps.writeByte((byte) player.getHeadIcon()); // Head icon aka prayer over head
        playerProps.writeByte((byte) player.getSkullIcon()); // Skull icon
        if (!player.isNpc()) {
            if (player.getEquipment()[Equipment.Slot.HEAD.getId()] > 1) {
                playerProps.writeWord(0x200 + player.getEquipment()[Equipment.Slot.HEAD.getId()]);
            } else {
                playerProps.writeByte(0);
            }
            if (player.getEquipment()[Equipment.Slot.CAPE.getId()] > 1) {
                playerProps.writeWord(0x200 + player.getEquipment()[Equipment.Slot.CAPE.getId()]);
            } else {
                playerProps.writeByte(0);
            }
            if (player.getEquipment()[Equipment.Slot.NECK.getId()] > 1) {
                playerProps.writeWord(0x200 + player.getEquipment()[Equipment.Slot.NECK.getId()]);
            } else {
                playerProps.writeByte(0);
            }
            if (player.getEquipment()[Equipment.Slot.WEAPON.getId()] > 1 && !player.UsingAgility) {
                playerProps.writeWord(0x200 + player.getEquipment()[Equipment.Slot.WEAPON.getId()]);
            } else {
                playerProps.writeByte(0);
            }
            if (player.getEquipment()[Equipment.Slot.CHEST.getId()] > 1) {
                playerProps.writeWord(0x200 + player.getEquipment()[Equipment.Slot.CHEST.getId()]);
            } else {
                playerProps.writeWord(0x100 + player.getTorso());
            }
            if (player.getEquipment()[Equipment.Slot.SHIELD.getId()] > 1 && !player.UsingAgility) {
                playerProps.writeWord(0x200 + player.getEquipment()[Equipment.Slot.SHIELD.getId()]);
            } else {
                playerProps.writeByte(0);
            }
            if (!Server.itemManager.isFullBody(player.getEquipment()[Equipment.Slot.CHEST.getId()])) {
                playerProps.writeWord(0x100 + player.getArms());
            } else {
                playerProps.writeByte(0);
            }
            if (player.getEquipment()[Equipment.Slot.LEGS.getId()] > 1) {
                playerProps.writeWord(0x200 + player.getEquipment()[Equipment.Slot.LEGS.getId()]);
            } else {
                playerProps.writeWord(0x100 + player.getLegs());
            }
            if (!Server.itemManager.isFullHelm(player.getEquipment()[Equipment.Slot.HEAD.getId()]) && !Server.itemManager.isMask(player.getEquipment()[Equipment.Slot.HEAD.getId()])) {
                playerProps.writeWord(0x100 + player.getHead()); // head
            } else {
                playerProps.writeByte(0);
            }
            if (player.getEquipment()[Equipment.Slot.HANDS.getId()] > 1) {
                playerProps.writeWord(0x200 + player.getEquipment()[Equipment.Slot.HANDS.getId()]);
            } else {
                playerProps.writeWord(0x100 + player.getHands());
            }
            if (player.getEquipment()[Equipment.Slot.FEET.getId()] > 1) {
                playerProps.writeWord(0x200 + player.getEquipment()[Equipment.Slot.FEET.getId()]);
            } else {
                playerProps.writeWord(0x100 + player.getFeet());
            }
            if (!Server.itemManager.isMask(player.getEquipment()[Equipment.Slot.HEAD.getId()]) && (player.playerLooks[0] != 1)) {
                playerProps.writeWord(0x100 + player.getBeard());
            } else {
                playerProps.writeByte(0); // 0 = nothing on and girl don't have beard
                // so send 0. -bakatool
            }
        } else {
            playerProps.writeWord(-1);
            playerProps.writeWord(player.getPlayerNpc());
        }
        // array of 5 bytes defining the colors
        playerProps.writeByte(player.playerLooks[8]); // hair color
        playerProps.writeByte(player.playerLooks[9]); // torso color.
        playerProps.writeByte(player.playerLooks[10]); // leg color
        playerProps.writeByte(player.playerLooks[11]); // feet color
        playerProps.writeByte(player.playerLooks[12]); // skin color (0-6)
        playerProps.writeWord(player.getStandAnim()); // standAnimIndex
        playerProps.writeWord(player.getWalkAnim()); // standTurnAnimIndex, 823 default
        playerProps.writeWord(player.getWalkAnim()); // walkAnimIndex
        playerProps.writeWord(player.getWalkAnim()); // turn180AnimIndex, 820 default
        playerProps.writeWord(player.getWalkAnim()); // turn90CWAnimIndex, 821 default
        playerProps.writeWord(player.getWalkAnim()); // turn90CCWAnimIndex, 822 default
        playerProps.writeWord(player.getRunAnim()); // runAnimIndex

        playerProps.writeQWord(Utils.playerNameToInt64(player.getPlayerName()));
        playerProps.writeByte(player.determineCombatLevel()); // combat level
        playerProps.writeWord(0); // incase != 0, writes skill-%d
        str.writeByteC(playerProps.currentOffset);
        str.writeBytes(playerProps.buffer, playerProps.currentOffset, 0);
    }

    @Override
    public void appendFaceCoordinates(Player player, Stream stream) {
        stream.writeWordBigEndianA(player.getFacePosition().getX());
        stream.writeWordBigEndian(player.getFacePosition().getY());
    }

    @Override
    public void appendPrimaryHit(Player player, Stream stream) {
        synchronized(this) {
            stream.writeByte(Math.min(player.getHitDiff(), 255)); // What the perseon got 'hit' for
            if (player.getHitDiff() == 0) {
                stream.writeByteA(0);
            } else {
                stream.writeByteA(player.isCrit() ? 3 : 1);
            }
            double hp = Misc.getCurrentHP(player.getCurrentHealth(), player.getMaxHealth());
            int value = hp > 4.00 ? (int) hp : hp != 0.0 ? 4 : 0;
            stream.writeByteC(value);
            stream.writeByte(100);
            player.setCrit(false); // bar
            player.setInCombat(true);
            player.setLastCombat(System.currentTimeMillis());
        }
    }

    public void appendPrimaryHit2(Player player, Stream stream) {
        synchronized(this) {
            stream.writeByte(Math.min(player.getHitDiff(), 255)); // What the perseon got 'hit' for
            if (player.getHitDiff() == 0) {
                stream.writeByteS(0);
            } else {
                stream.writeByteS(player.isCrit() ? 3 : 1);
            }
            double hp = Misc.getCurrentHP(player.getCurrentHealth(), player.getMaxHealth());
            int value = hp > 4.00 ? (int) hp : hp != 0.0 ? 4 : 0;
            stream.writeByte(value);
            stream.writeByteC(100);
            player.setCrit(false); // bar
            player.setInCombat(true);
            player.setLastCombat(System.currentTimeMillis());
        }
    }

}
