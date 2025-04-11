package byd.cxkcxkckx.gun_bot.func;
        
import byd.cxkcxkckx.gun_bot.gun_bot;

public abstract class AbstractModule {
    protected final String name;
    protected final gun_bot plugin;

    protected AbstractModule(String name) {
        this.name = name;
        this.plugin = gun_bot.getInstance();
    }

    public abstract void onEnable();
    public abstract void onDisable();

    protected gun_bot getPlugin() {
        return plugin;
    }
}
