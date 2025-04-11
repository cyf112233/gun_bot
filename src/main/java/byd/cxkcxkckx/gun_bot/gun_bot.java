package byd.cxkcxkckx.gun_bot;
        
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.EconomyHolder;
import byd.cxkcxkckx.gun_bot.func.AntiBotModule;

public class gun_bot extends BukkitPlugin {
    private AntiBotModule antiBotModule;

    public static gun_bot getInstance() {
        return (gun_bot) BukkitPlugin.getInstance();
    }

    public gun_bot() {
        super(options()
                .bungee(false)
                .adventure(false)
                .database(false)
                .reconnectDatabaseWhenReloadConfig(false)
                .vaultEconomy(false)
                .scanIgnore("top.mrxiaom.example.libs")
        );
    }

    @Override
    protected void afterEnable() {
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化防机器人攻击模块
        antiBotModule = new AntiBotModule();
        antiBotModule.onEnable();
        
        getLogger().info("gun_bot 加载完毕");
    }

    @Override
    protected void beforeDisable() {
        if (antiBotModule != null) {
            antiBotModule.onDisable();
        }
    }
}
