package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.EntityUpdating;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Stream;
import net.dodian.utilities.Utils;

import java.util.Iterator;

/**
 * @author Dashboard
 */
public class NpcUpdating extends EntityUpdating<Npc> {

    private static final NpcUpdating instance = new NpcUpdating();

    public static NpcUpdating getInstance() {
        return instance;
    }

    @Override
    public void update(Player player, Stream stream) {
        Stream block = new Stream(new byte[10000]);
        block.currentOffset = 0;
        stream.createFrameVarSizeWord(65);
        stream.initBitAccess();

        stream.writeBits(8, player.getLocalNpcs().size());
        for (Iterator<Npc> i = player.getLocalNpcs().iterator(); i.hasNext(); ) {
            Npc npc = i.next();
            boolean exceptions = removeNpc(player, npc);
            if (player.withinDistance(npc) && npc.isVisible() && !exceptions) {
                updateNPCMovement(npc, stream);
                appendBlockUpdate(npc, block);
            } else {
                stream.writeBits(1, 1);
                stream.writeBits(2, 3); // tells client to remove this npc from list
                i.remove();
            }
        }

        for (Npc npc : Server.npcManager.getNpcs()) {
            boolean exceptions = removeNpc(player, npc);
            if (npc == null || !(player.withinDistance(npc) && npc.isVisible()) || !npc.isVisible() || exceptions) continue;
            if (player.getLocalNpcs().add(npc)) {
                if(npc.getId() == 1306 || npc.getId() == 1307) //Makeover mage!
                    npc.setId(player.getGender() == 0 ? 1306 : 1307);
                addNpc(player, npc, stream);
                appendBlockUpdate(npc, block);
            }
        }
        if (block.currentOffset > 0) {
            stream.writeBits(14, 16383);
            stream.finishBitAccess();
            stream.writeBytes(block.buffer, block.currentOffset, 0);
        } else {
            stream.finishBitAccess();
        }
        stream.endFrameVarSizeWord();
    }

    public static boolean removeNpc(Player player, Npc npc) {
        Client c = ((Client) player);
        if(c == null || npc == null) return false;
        boolean check = c.quests[0] > 0 && npc.getId() == 555 && npc.getPosition().getX() == 2604 && npc.getPosition().getY() == 3092;
        if(c.quests[1] > 0 && npc.getId() == 999 && npc.getPosition().getX() == 2 && npc.getPosition().getY() == 2)
            check = true;
        return check;
    }


    public void addNpc(Player player, Npc npc, Stream stream) {
        stream.writeBits(14, npc.getSlot());
        /* Position */
        Position npcPos = npc.getPosition(), plrPos = player.getPosition();
        int z = npcPos.getY() - plrPos.getY();
        if(z < 0)
            z += 32;
        stream.writeBits(5, z); // y coordinate relative to thisPlayer
        z = npcPos.getX() - plrPos.getX();
        if(z < 0)
            z += 32;
        stream.writeBits(5, z); // y coordinate relative to thisPlayer

        stream.writeBits(1, 0); // something??
        stream.writeBits(14, npc.getId());
        stream.writeBits(1, npc.getUpdateFlags().isUpdateRequired() ? 1 : 0);
    }

    @Override
    public void appendBlockUpdate(Npc npc, Stream stream) {
        if(!npc.getUpdateFlags().isUpdateRequired())
            return;
        int updateMask = 0;
        for (UpdateFlag flag : npc.getUpdateFlags().keySet()) {
            if (npc.getUpdateFlags().isRequired(flag)) {
                updateMask |= flag.getMask(npc.getType());
            }
        }
        stream.writeByte(updateMask);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.ANIM))
            appendAnimationRequest(npc, stream);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT))
            appendPrimaryHit(npc, stream);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS))
            appendGfxUpdate(npc, stream);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT))
            appendTextUpdate(npc, stream);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER))
            appendFaceCharacter(npc, stream);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.HIT2))
            appendPrimaryHit2(npc, stream);
        if (npc.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE))
            appendFaceCoordinates(npc, stream);
    }

    public void appendTextUpdate(Npc npc, Stream stream) {
        stream.writeString(npc.getText());
        //stream.writeDWord(0); //Do we need?!
    }

    public void appendGfxUpdate(Npc npc, Stream stream) {
        stream.writeWord(npc.getGfxId());
        stream.writeDWord(npc.getGfxHeight() << 16);
    }

    @Override
    public void appendAnimationRequest(Npc npc, Stream stream) {
        stream.writeWordBigEndian(npc.getAnimationId());
        stream.writeByte(npc.getAnimationDelay());
    }

    @Override
    public void appendPrimaryHit(Npc npc, Stream stream) {
        stream.writeByteC(Math.min(npc.getDamageDealt(), 255));
        if (npc.getDamageDealt() == 0) {
            stream.writeByteS(0);
        } else if (!npc.isCrit()) {
            stream.writeByteS(1);
        } else {
            stream.writeByteS(3);
        }
        double hp = Misc.getCurrentHP(npc.getCurrentHealth(), npc.getMaxHealth());
        int value = hp > 4.00 ? (int) hp : hp != 0.0 ? 4 : 0;
        stream.writeByteS(value);
        stream.writeByteC(100);
    }

    public void appendPrimaryHit2(Npc npc, Stream stream) {
        stream.writeByteA(Math.min(npc.getDamageDealt(), 255));
        if (npc.getDamageDealt() == 0) {
            stream.writeByteC(0);
        } else if (!npc.isCrit()) {
            stream.writeByteC(1);
        } else {
            stream.writeByteC(3);
        }
        double hp = Misc.getCurrentHP(npc.getCurrentHealth(), npc.getMaxHealth());
        int value = hp > 4.00 ? (int) hp : hp != 0.0 ? 4 : 0;
        stream.writeByteA(value);
        stream.writeByte(100);
    }
    @Override
    public void appendFaceCoordinates(Npc npc, Stream stream) {
        stream.writeWordBigEndian(npc.getFacePosition().getX());
        stream.writeWordBigEndian(npc.getFacePosition().getY());
    }

    @Override
    public void appendFaceCharacter(Npc npc, Stream stream) {
        stream.writeWordBigEndian(npc.getViewX());
        stream.writeWordBigEndian(npc.getViewY());
    }

    public void updateNPCMovement(Npc npc, Stream stream) {
        if (!npc.isWalking() && npc.getDirection() == -1) {
            if (npc.getUpdateFlags().isUpdateRequired()) {
                stream.writeBits(1, 1);
                stream.writeBits(2, 0);
            } else {
                stream.writeBits(1, 0);
            }
        } else {
            npc.setDirection(npc.getNextWalkingDirection());
            if (npc.getDirection() == -1) {
                System.out.println("aborting walk");
                stream.writeBits(1, 1);
                stream.writeBits(2, 0);
                return;
            }
            stream.writeBits(1, 1);
            stream.writeBits(2, 1);
            stream.writeBits(3, Utils.xlateDirectionToClient[npc.getDirection()]);
            if (npc.getUpdateFlags().isUpdateRequired()) {
                stream.writeBits(1, 1);
            } else {
                stream.writeBits(1, 0);
            }
        }
    }

}
