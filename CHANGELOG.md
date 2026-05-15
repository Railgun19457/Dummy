# 更新日志

## 0.2.0 - 2026-05-15

- 兼容 Paper 26.1.2 的 `ClientboundSetEntityMotionPacket` record accessor。
- 修复 26.1.2 下假人连接监听器未完全替换导致的重力异常。
- 修复 26.1.2 下假人受击后速度被服务端恢复覆盖，导致击退不生效的问题。
- 改进假人移除流程，显式移除实体、Tab 列表项并关闭 fake connection。
- 为 fake channel/connection 增加关闭状态，避免移除后仍被视为连接中。
- 忽略 IDE/Java 输出目录 `bin/`。

## 0.1.0-SNAPSHOT - 2026-05-15

- 初始快照版本。
- 实现基于 Paper + NMS `ServerPlayer` 的假人创建、移除、列表、复活和传送。
- 实现背包、装备、副手、经验、皮肤、动作、持久化、GUI 和区块加载。
- 提供 `zh_CN` / `en_US` 双语消息和中英文 README。
