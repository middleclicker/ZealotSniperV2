package cf.vaccat.catclient.module;

import cf.vaccat.catclient.module.misc.ZealotSniper;
import cf.vaccat.catclient.module.movement.Sprint;
import cf.vaccat.catclient.module.render.ClickGUI;
import cf.vaccat.catclient.module.render.Fullbright;
import cf.vaccat.catclient.module.render.HUD;

import java.util.ArrayList;

public class ModuleManager {

	public ArrayList<Module> modules;
	
	public ModuleManager() {
		(modules = new ArrayList<Module>()).clear();
		this.modules.add(new ClickGUI());
		this.modules.add(new HUD());
		this.modules.add(new Fullbright());
		this.modules.add(new Sprint());
		this.modules.add(new ZealotSniper());
	}
	
	public Module getModule(String name) {
		for (Module m : this.modules) {
			if (m.getName().equalsIgnoreCase(name)) {
				return m;
			}
		}
		return null;
	}
	
	public ArrayList<Module> getModuleList() {
		return this.modules;
	}
	
	public ArrayList<Module> getModulesInCategory(Category c) {
		ArrayList<Module> mods = new ArrayList<Module>();
		for (Module m : this.modules) {
			if (m.getCategory() == c) {
				mods.add(m);
			}
		}
		return mods;
	}
}
