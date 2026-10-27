package dev.dummy.listener;

import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent;
import com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent;
import dev.dummy.dummy.DummyManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 让 {@code spawning: false} 的假人不再成为刷怪来源。
 *
 * <p>服务端以玩家为判据的刷怪有两条互不相干的路径，需要分别拦截：</p>
 *
 * <ul>
 *   <li>区块刷怪：Paper 每个 tick 会为每个玩家触发一次 {@link PlayerNaturallySpawnCreaturesEvent} 并缓存实例，
 *       区块是否可刷怪取决于该缓存实例是否被取消以及其刷怪半径（服务端会跳过被取消的玩家）。
 *       取消假人的事件即可让假人周围不再因为它的存在而刷怪，且不需要遍历实体或拦截刷怪结果。</li>
 *   <li>幻翼：服务端的幻翼生成器不看区块刷怪数据，而是遍历在线玩家读取其“未睡觉天数”
 *       （{@code Stats.TIME_SINCE_REST}，非睡眠玩家每 tick 累积）。假人只要在 tick 计时就会一直增长，
 *       所以即使取消了上面的刷怪事件，假人附近仍会不断刷出幻翼；只有取消 {@link PhantomPreSpawnEvent}
 *       （其 {@code getSpawningEntity()} 即该幻翼归属的玩家）才能掐断这条路径。</li>
 * </ul>
 *
 * <p>两项都只作用于 {@code spawning: false} 的假人，真实玩家的刷怪、刷怪笼、繁殖与刷怪蛋等
 * 其它来源都不受影响。</p>
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

    @EventHandler
    public void onPhantomPreSpawn(PhantomPreSpawnEvent event) {
        Entity spawnSource = event.getSpawningEntity();
        if (spawnSource instanceof Player player && dummyManager.suppressesSpawning(player)) {
            event.setCancelled(true);
        }
    }
}
