package net.dodian.uber.game.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.Entity
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.item.SpecialsHandler
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.prayer.Prayers
import net.dodian.utilities.Misc
import net.dodian.utilities.Range
import net.dodian.utilities.Utils

fun Client.handleMelee(): Int {
    if (!canReach(target, 1))
        return 0

    val time = System.currentTimeMillis()

        if (time - lastAttack > getbattleTimer(equipment[Equipment.Slot.WEAPON.id])) {
            isInCombat = true
            lastCombat = System.currentTimeMillis()
        } else return 0

    if (target is Npc && Server.npcManager.getNpc(target.slot).id == 2267) {
        send(SendMessage("Cant use melee on this monster!"))
        resetAttack()
        return 0
    }

        val emote = Server.itemManager.getAttackAnim(equipment[Equipment.Slot.WEAPON.id])
        setFocus(target.position.x, target.position.y)
        if (target is Player && duelFight && duelRule[1]) {
            send(SendMessage("Melee has been disabled for this duel!"))
            resetAttack()
            return 0
        }
        sendAnimation(emote)
        var maxHit = meleeMaxHit().toDouble()
         if (target is Npc) { // Slayer damage!
             val npcId = Server.npcManager.getNpc(target.slot).id
             if(getSlayerDamage(npcId, false) == 1)
                 maxHit *= 1.15
             else if(getSlayerDamage(npcId, false) == 2)
                 maxHit *= 1.2
         }
        /* Silver weakness! */
        if (target is Npc && Server.npcManager.getNpc(target.slot).weakness == Npc.weaponWeakness.SILVERWEAPON) {
            val silverWeapon = listOf(2952)
            if (equipment[Equipment.Slot.WEAPON.id] in silverWeapon) { //Wolfbane!
                maxHit *= 2;
                if(Misc.chance(8) == 1) {
                    maxHit *= 2
                    //Server.npcManager.getNpc(target.slot).setGfx(274, 50) //TODO: fix gfx for npc!
                    send(SendMessage("<col=8B0000>You put a deep wound into your target!"))
                }
            }
        }
        if(target is Player) {
            val player = Server.playerHandler.getClient(target.slot)
            if (player.prayerManager.isPrayerOn(Prayers.Prayer.PROTECT_MELEE)) maxHit /= 2.0
        }
        var hit = Utils.random(maxHit.toInt())
        val criticalChance = getLevel(Skill.AGILITY) / 9
        if(equipment[Equipment.Slot.SHIELD.id]==4224)
            criticalChance * 1.5
        val extra = getLevel(Skill.STRENGTH) * 0.195
        val landCrit = Math.random() * 100 <= criticalChance
        val landHit = landHit(this, target)
        if (target is Npc) {
            val npc = Server.npcManager.getNpc(target.slot)
            if (landCrit && landHit)
                hit + Utils.dRandom2(extra).toInt()
            else if(!landHit) hit = 0
            if(hit >= npc.currentHealth)
                hit = npc.currentHealth
            npc.dealDamage(this, hit, landCrit && landHit)
        }
        if (target is Player) {
            val player = Server.playerHandler.getClient(target.slot)
            if (landCrit && landHit)
                hit + Utils.dRandom2(extra).toInt()
            else if(!landHit) hit = 0
            if (player.prayerManager.isPrayerOn(Prayers.Prayer.PROTECT_MELEE)) hit /= 2
            if(hit >= player.currentHealth)
                hit = player.currentHealth
            player.dealDamage(hit, landCrit && landHit)
        }

        if (target is Npc) {
            if (FightType == 3) {
                val xp = (15 * hit) * CombatExpRate
                giveExperience(xp, Skill.ATTACK)
                giveExperience(xp, Skill.DEFENCE)
                giveExperience(xp, Skill.STRENGTH)
            } else giveExperience((40 * hit) * CombatExpRate, Skill.getSkill(FightType))

            giveExperience((15 * hit) * CombatExpRate, Skill.HITPOINTS)
        }
        if (debug) send(SendMessage("hit = $hit, elapsed = ${time - lastAttack}"))
    lastAttack = System.currentTimeMillis()

    return 1
}

fun highestAttackBonus(p: Client): Int {
    var bonus = 0
    for (i in 0..2) {
        if (p.playerBonus[i] > bonus)
            bonus = p.playerBonus[i]
    }
    return bonus
    }
    fun highestDefensiveBonus(p: Client): Int {
        var bonus = 0
        for (i in 5..7) {
            if (p.playerBonus[i] > bonus)
                bonus = p.playerBonus[i]
        }
            return bonus
        }
fun landHit(p: Client, t: Entity): Boolean {
    val hitChance: Double
    val chance = Misc.chance(100000) / 1000
    val prayerBonus = if(p.prayerManager.isPrayerOn(Prayers.Prayer.CLARITY_OF_THOUGHT)) 1.05
    else if(p.prayerManager.isPrayerOn(Prayers.Prayer.IMPROVED_REFLEXES)) 1.1
    else if(p.prayerManager.isPrayerOn(Prayers.Prayer.INCREDIBLE_REFLEXES)) 1.15
    else if(p.prayerManager.isPrayerOn(Prayers.Prayer.CHIVALRY)) 1.18
    else if(p.prayerManager.isPrayerOn(Prayers.Prayer.PIETY)) 1.22
    else 1.0
    if(t is Client) { //Pvp
        val atkLevel = p.getLevel(Skill.ATTACK)
        val atkBonus = highestAttackBonus(p)
        val defLevel = t.getLevel(Skill.DEFENCE)
        val defBonus = highestDefensiveBonus(t)
        val prayerDefBonus = if(t.prayerManager.isPrayerOn(Prayers.Prayer.THICK_SKIN)) 1.05
        else if(p.prayerManager.isPrayerOn(Prayers.Prayer.ROCK_SKIN)) 1.1
        else if(p.prayerManager.isPrayerOn(Prayers.Prayer.STEEL_SKIN)) 1.15
        else if(p.prayerManager.isPrayerOn(Prayers.Prayer.CHIVALRY)) 1.18
        else if(p.prayerManager.isPrayerOn(Prayers.Prayer.PIETY)) 1.22
        else 1.0
        val playerDef = (defLevel * (defBonus + 64.0)) * prayerDefBonus
        val playerAccuracy = (atkLevel * (atkBonus + 64.0)) * prayerBonus
        hitChance = if (playerAccuracy > playerDef)
            1 - ((playerDef + 2) / (2 * (playerAccuracy + 1)))
        else
            playerAccuracy / (2 * (playerDef + 1))
        p.debug("Melee Accuracy Hit: " + (hitChance * 100.0) + "% out of " + chance.toDouble() + "%")
        return chance < (hitChance*100)
    } else if(t is Npc) { //Pve
        val atkBonus = highestAttackBonus(p)
        val atkLevel = p.getLevel(Skill.ATTACK)
        val defLevel = t.defence
        val defBonus = 0.0
        val npcDef = defLevel * (defBonus + 64.0)
        var playerAccuracy = (atkLevel * (atkBonus + 64.0)) * prayerBonus
        playerAccuracy = if(p.getSlayerDamage(t.id, false) == 1) playerAccuracy * 1.15
        else if(p.getSlayerDamage(t.id, false) == 2) playerAccuracy * 1.20 else playerAccuracy
        hitChance = if (playerAccuracy > npcDef)
            1 - ((npcDef + 2) / (2 * (playerAccuracy + 1)))
        else
            playerAccuracy / (2 * (npcDef + 1))
        p.debug("Melee Accuracy Hit: " + (hitChance * 100.0) + "% out of " + chance.toDouble() + "%%")
        return chance < (hitChance*100)
    }
    return true
}

fun Client.handleSpecial(hit: Int): Int {
    var newHit = hit
    val emote = Server.itemManager.getAttackAnim(equipment[Equipment.Slot.WEAPON.id])
    if(target is Client) {

    }
    if (target is Npc) {
        val chance = Range(1, 8).value
        if (chance == 1 && specsOn) {
            when (equipment[Equipment.Slot.WEAPON.id]) {
                4151, 7158 -> {
                    SpecialsHandler.specAction(this, equipment[Equipment.Slot.WEAPON.id], hitDiff)
                    newHit += bonusSpec
                    requestAnim(emoteSpec, 0)
                    // TODO: Uhm? Why y then x?
                    animation(animationSpec, target.position)
                }
            }
        } else requestAnim(emote, 0)
    } else resetAttack()
    return newHit
}