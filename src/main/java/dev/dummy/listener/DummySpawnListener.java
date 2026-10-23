package dev.dummy.listener;

import com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent;
import dev.dummy.dummy.DummyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 让 {@code spawning: false} 的假人不再作为天然刷怪的“上下文玩家”。
 *
 * <p>Paper 每个 tick 会为每个玩家触发一次 {@link PlayerNaturallySpawnCreaturesEvent} 并缓存实例，
 * 区块是否可刷怪取决于该缓存实例是否被取消以及其刷怪半径（服务端会跳过被取消的玩家）。
 * 取消假人的事件即可让假人周围不再因为它的存在而刷怪，且不需要遍历实体或拦截刷怪结果；
 * 真实玩家的刷怪、刷怪笼、繁殖与刷怪蛋等其它来源都不受影响。</p>
 */
public final class DummySpawnListener implements Listener {
    private final DummyManager dummyManager;

    public DummySpawnListener(DummyManager dummyManager) {
        this.dummyManager = dummyManager;
    }

    @EventHandler
    public void onNaturallySpawnCreatures(PlayerNaturallySpawnCreaturesEvent event) {
        if (dummyManager.suppressesSpawning(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
