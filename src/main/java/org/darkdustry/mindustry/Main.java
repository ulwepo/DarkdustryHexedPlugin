package org.darkdustry.mindustry;

import arc.util.CommandHandler;
import mindustry.mod.Plugin;

public class Main extends Plugin {

	public static void main(String[] args) {
		throw new Error("This project can't be used as executable!");
	}

	@Override
	public void init() {
		super.init();
	}

	@Override
	public void registerClientCommands(CommandHandler handler) {
		super.registerClientCommands(handler);
	}

	@Override
	public void registerServerCommands(CommandHandler handler) {
		super.registerServerCommands(handler);
	}
}
