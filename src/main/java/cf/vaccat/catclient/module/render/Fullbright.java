package cf.vaccat.catclient.module.render;

import cf.vaccat.catclient.module.Category;
import cf.vaccat.catclient.module.Module;

public class Fullbright extends Module {

    public Fullbright() {
        super("Fullbright", "makes world brighter", Category.RENDER);
        this.setToggled(true);
    }

    float originalSetting;

    @Override
    public void onEnable() {
        super.onEnable();
        originalSetting = mc.gameSettings.gammaSetting;
        mc.gameSettings.gammaSetting = 10000f;
    }

    @Override
    public void onDisable() {
        mc.gameSettings.gammaSetting = originalSetting;
        super.onDisable();
    }
}
