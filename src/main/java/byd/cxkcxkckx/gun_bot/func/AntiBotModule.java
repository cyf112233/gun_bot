package byd.cxkcxkckx.gun_bot.func;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AntiBotModule extends AbstractModule implements Listener {
    private final Map<String, Integer> connectionCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastResetTime = new ConcurrentHashMap<>();
    private final Set<String> onlinePlayersWhenBlocked = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isBlocked = false;
    private long blockEndTime = 0;
    private String originalMotd = null;
    private Plugin captchaPlugin = null;

    private int maxConnectionsPerMinute;
    private int banDuration;
    private boolean enabled;
    private boolean debug;
    private String kickMessage;
    private String consoleAttackMessage;
    private String consoleBanDurationMessage;
    private String motdBanMessage;

    public AntiBotModule() {
        super("anti-bot");
    }

    @Override
    public void onEnable() {
        loadConfig();
        getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
        
        // 检查captcha插件是否存在
        checkCaptchaPlugin();
        
        // 保存原始 MOTD
        originalMotd = getPlugin().getServer().getMotd();
        
        // 每分钟清理一次连接计数
        scheduler.scheduleAtFixedRate(this::cleanupConnectionCounts, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void onDisable() {
        scheduler.shutdown();
        // 恢复原始 MOTD
        if (originalMotd != null && (captchaPlugin == null || !captchaPlugin.isEnabled())) {
            getPlugin().getServer().setMotd(originalMotd);
        }
    }

    private void loadConfig() {
        FileConfiguration config = getPlugin().getConfig();
        maxConnectionsPerMinute = config.getInt("anti-bot.max-connections-per-minute", 10);
        banDuration = config.getInt("anti-bot.ban-duration", 5);
        enabled = config.getBoolean("anti-bot.enabled", true);
        debug = config.getBoolean("anti-bot.debug", true);
        
        // 加载消息配置
        kickMessage = config.getString("anti-bot.messages.kick-message", "§c服务器正在遭受攻击，请稍后再试！");
        consoleAttackMessage = config.getString("anti-bot.messages.console-attack-message", "检测到可能的机器人攻击，来自IP: {ip}");
        consoleBanDurationMessage = config.getString("anti-bot.messages.console-ban-duration-message", "服务器将在 {time} 后拒绝新的连接");
        motdBanMessage = config.getString("anti-bot.messages.motd-ban-message", "§c服务器正在遭受攻击，将在 {time} 后恢复");
    }

    private void checkCaptchaPlugin() {
        captchaPlugin = getPlugin().getServer().getPluginManager().getPlugin("captcha");
    }

    /**
     * 将毫秒时间转换为可读的时间格式
     * @param millis 毫秒时间
     * @return 格式化的时间字符串，例如：1分30秒
     */
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return minutes + "分" + seconds + "秒";
        } else {
            return seconds + "秒";
        }
    }

    /**
     * 获取连接次数最多的 IP
     * @return 连接次数最多的 IP，如果没有则返回 null
     */
    private String getMostConnectionsIP() {
        String mostConnectionsIP = null;
        int maxConnections = 0;
        
        for (Map.Entry<String, Integer> entry : connectionCounts.entrySet()) {
            if (entry.getValue() > maxConnections) {
                maxConnections = entry.getValue();
                mostConnectionsIP = entry.getKey();
            }
        }
        
        return mostConnectionsIP;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerListPing(ServerListPingEvent event) {
        // 如果captcha插件存在，不修改MOTD
        if (captchaPlugin != null && captchaPlugin.isEnabled()) {
            return;
        }

        if (isBlocked && System.currentTimeMillis() < blockEndTime) {
            // 计算剩余时间
            long remainingTime = blockEndTime - System.currentTimeMillis();
            // 更新 MOTD
            event.setMotd(motdBanMessage.replace("{time}", formatTime(remainingTime)));
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!enabled) return;

        // 每次玩家连接时都检查 captcha 插件状态
        checkCaptchaPlugin();

        // 获取玩家的真实 IP 地址
        String ip = event.getAddress().getHostAddress();
        String playerName = event.getPlayer().getName();
        
        // 检查是否是封禁时在线的玩家
        if (isBlocked && !onlinePlayersWhenBlocked.contains(playerName)) {
            if (System.currentTimeMillis() < blockEndTime) {
                // 如果captcha插件存在，不直接踢出玩家，而是让captcha插件处理
                if (captchaPlugin == null || !captchaPlugin.isEnabled()) {
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMessage);
                }
                return;
            } else {
                isBlocked = false;
                onlinePlayersWhenBlocked.clear();
            }
        }

        long currentTime = System.currentTimeMillis();
        lastResetTime.putIfAbsent(ip, currentTime);

        // 检查是否需要重置计数器（超过1分钟）
        if (currentTime - lastResetTime.get(ip) >= 60000) {
            connectionCounts.put(ip, 1);
            lastResetTime.put(ip, currentTime);
        } else {
            // 增加连接计数
            connectionCounts.merge(ip, 1, Integer::sum);
            
            // 检查是否超过限制
            if (connectionCounts.get(ip) > maxConnectionsPerMinute) {
                isBlocked = true;
                blockEndTime = System.currentTimeMillis() + (banDuration * 60 * 1000L);
                // 记录当前在线的玩家
                getPlugin().getServer().getOnlinePlayers().forEach(player -> 
                    onlinePlayersWhenBlocked.add(player.getName()));
                
                // 如果captcha插件存在，通知它开始验证模式
                if (captchaPlugin != null && captchaPlugin.isEnabled()) {
                    try {
                        Class<?> captchaListenerClass = Class.forName("byd.cxkcxkckx.captcha.func.CaptchaListener");
                        Object captchaListener = captchaPlugin.getServer().getPluginManager().getPlugin("captcha")
                            .getClass().getMethod("getCaptchaListener").invoke(captchaPlugin);
                        captchaListenerClass.getMethod("startAttackMode", long.class)
                            .invoke(captchaListener, blockEndTime);
                    } catch (Exception e) {
                        getPlugin().getLogger().warning("无法通知captcha插件开始验证模式: " + e.getMessage());
                    }
                }
                
                if (debug) {
                    // 获取连接次数最多的 IP
                    String mostConnectionsIP = getMostConnectionsIP();
                    if (mostConnectionsIP != null) {
                        getPlugin().getLogger().warning(consoleAttackMessage.replace("{ip}", mostConnectionsIP));
                    } else {
                        getPlugin().getLogger().warning(consoleAttackMessage.replace("{ip}", ip));
                    }
                    getPlugin().getLogger().warning(consoleBanDurationMessage.replace("{time}", formatTime(banDuration * 60 * 1000L)));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!enabled) return;
        
        String ip = event.getPlayer().getAddress().getAddress().getHostAddress();
        long currentTime = System.currentTimeMillis();
        
        // 记录连接次数
        if (currentTime - lastResetTime.getOrDefault(ip, 0L) < 60000) {
            connectionCounts.merge(ip, 1, Integer::sum);
        }
    }

    private void cleanupConnectionCounts() {
        long currentTime = System.currentTimeMillis();
        connectionCounts.entrySet().removeIf(entry -> 
            currentTime - lastResetTime.getOrDefault(entry.getKey(), 0L) >= 60000);
    }
} 