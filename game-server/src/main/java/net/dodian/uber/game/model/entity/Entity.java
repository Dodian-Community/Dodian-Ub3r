package net.dodian.uber.game.model.entity;

import net.dodian.cache.region.Region;
import net.dodian.jobs.JobScheduler;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.UpdateFlags;
import net.dodian.uber.game.model.combat.impl.CombatStyleHandler;
import net.dodian.uber.game.model.entity.npc.Npc;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public abstract class Entity {

    private final Position position;
    private final Position originalPosition;
    private final Position facePosition;
    private final int slot;
    private int gfxId, gfxHeight;
    private final Type type;

    private final UpdateFlags updateFlags;

    private CombatStyleHandler.CombatStyles combatStyle = CombatStyleHandler.CombatStyles.ACCURATE_MELEE;

    private int animationDelay;
    private int animationId;
    private String text;

    private Map<Entity, Integer> damage = new HashMap<Entity, Integer>();

    private JobScheduler jobScheduler = new JobScheduler(this);

    public Entity(Position position, int slot, Type type) {
        this.position = position.copy();
        this.originalPosition = position.copy();
        this.facePosition = new Position(0, 0);
        this.updateFlags = new UpdateFlags();
        this.slot = slot;
        this.type = type;
    }

    public void requestAnim(int id, int delay) {
        setAnimationId(id);
        setAnimationDelay(delay * 10);
        getUpdateFlags().setRequired(UpdateFlag.ANIM, true);

    }

    public void setGfx(int id, int height) {
        this.gfxId = id;
        this.gfxHeight = height;
        getUpdateFlags().setRequired(UpdateFlag.GRAPHICS, true);
    }
    public int getGfxHeight() {
        return this.gfxHeight;
    }
    public int getGfxId() {
        return this.gfxId;
    }

    public void setText(String text) {
        this.text = text;
        getUpdateFlags().setRequired(UpdateFlag.FORCED_CHAT, true);
    }
    public String getText() {
        return text;
    }

    public boolean GoodDistance(int entityX, int entityY, int otherX, int otherY, int distance) {
        int dist = (int) Math.sqrt(Math.pow(entityX - otherX, 2) + Math.pow(entityY - otherY, 2));
        if (dist <= distance) {
            return true;
        }
        return false;
    }

    public int getSlot() {
        return slot;
    }

    public JobScheduler getJobScheduler() {
        return jobScheduler;
    }

    public void setFocus(int focusPointX, int focusPointY) {
        facePosition.moveTo(2 * focusPointX + 1, 2 * focusPointY + 1);
        getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);
    }

    public Position getFacePosition() {
        return this.facePosition;
    }

    public int getAnimationDelay() {
        return animationDelay;
    }

    public void setAnimationDelay(int animationDelay) {
        this.animationDelay = animationDelay;
    }

    public int getAnimationId() {
        return animationId;
    }

    public void setAnimationId(int animationId) {
        this.animationId = animationId;
    }

    public Type getType() {
        return type;
    }

    public int getSize() {
        if (type != null && type == Type.NPC && ((Npc) this) != null && Server.npcManager.getData(((Npc) this).getId()) != null) {
            return Server.npcManager.getData(((Npc) this).getId()).getSize();
        }
        return 1;
    }

    public int getSizeMinusOne() {
        return getSize() - 1;
    }

    public Position getPosition() {
        return this.position;
    }

    public Position getOriginalPosition() {
        return this.originalPosition;
    }

    public void moveTo(int x, int y, int z) {
        position.moveTo(x, y, z);
    }

    public boolean goodDistanceEntity(Entity other, int distance) {
        Rectangle thisArea = new Rectangle(getPosition().getX() - distance, getPosition().getY() - distance,
                2 * distance + getSize(), 2 * distance + getSize());
        Rectangle otherArea = new Rectangle(other.getPosition().getX(), other.getPosition().getY(), other.getSize(),
                other.getSize());
        return thisArea.intersects(otherArea);
    }

    public boolean canMove(int x, int y) {
        return Region.canMove(getPosition().getX(), getPosition().getY(), getPosition().getX() + x,
                getPosition().getY() + y, getPosition().getZ(), getSize(), getSize());
    }

    public CombatStyleHandler.CombatStyles getCombatStyle() {
        return combatStyle;
    }

    public void setCombatStyle(CombatStyleHandler.CombatStyles combatStyle) {
        this.combatStyle = combatStyle;
    }

    public Map<Entity, Integer> getDamage() {
        return damage;
    }

    public UpdateFlags getUpdateFlags() {
        return this.updateFlags;
    }

    public enum Type {
        NPC, PLAYER;
    }

    public enum damageType {
        MELEE, RANGED, MAGIC, //Standard
        FIRE_BREATH, JAD_MAGIC, JAD_RANGED, //
        TRUEDAMAGE
        ;
    }

}