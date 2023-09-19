package net.dodian.uber.game.model.combat;

import net.dodian.uber.game.model.combat.impl.CombatStyleHandler;
import net.dodian.uber.game.model.combat.impl.styles.*;

public enum WeaponData {
    UNARMED(new String[]{"unarmed"}, 5855, new UnarmedHandler()),
    WHIP(new String[]{"whip", "scythe"}, 12290, new WhipHandler()),
    BOW(new String[]{"bow", "seercull"}, 1764, new BowHandler()),
    WAND(new String[]{"wand", "staff", "toktz-mej-tal"}, 328, new WandHandler()),
    THROWABLE(new String[]{"dart", "knife", "javelin"}, 4446, new ThrowableHandler()),
    SWORDS(new String[]{"dagger", "sword", "toktz-xil-ak", "wolfbane"}, 2276, new SwordsHandler()),
    LONGSWORDS(new String[]{"scimitar", "longsword", "toktz-xil-ek"}, 2423, new LongSwordsHandler()),
    PICKAXE(new String[]{"pickaxe"}, 5570, new PickaxeHandler()),
    AXES(new String[]{"axe", "battleaxe"}, 1698, new AxeHandler()),
    HALBERD(new String[]{"halberd"}, 8460, new HalberdHandler()),
    SPEAR(new String[]{"spear"}, 4679, new SpearHandler()),
    MACE(new String[]{"mace", "flail"}, 3796, new MaceHandler()),
    HAMMERS(new String[]{"warhammer", "maul", "chicken", "tzhaar-ket-om", "tzhaar-ket-em"}, 425, new HammerHandler());

    private String name[];
    private int combatInterface;
    private CombatStyleHandler style;

    WeaponData(String[] name, int combatInterface, CombatStyleHandler style) {
        this.name = name;
        this.combatInterface = combatInterface;
        this.style = style;
    }

    public String getName()[] {
        return name;
    }

    public int getInterface() {
        return combatInterface;
    }

    public CombatStyleHandler getStyle() {
        return style;
    }
}
