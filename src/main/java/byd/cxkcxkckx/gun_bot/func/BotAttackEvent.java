package byd.cxkcxkckx.gun_bot.func;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BotAttackEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String attackerIP;
    private final long blockEndTime;

    public BotAttackEvent(String attackerIP, long blockEndTime) {
        this.attackerIP = attackerIP;
        this.blockEndTime = blockEndTime;
    }

    public String getAttackerIP() {
        return attackerIP;
    }

    public long getBlockEndTime() {
        return blockEndTime;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
} 