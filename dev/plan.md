# Dummy 项目规划

## 项目定位

Dummy 是一个面向高版本 Paper 服务端的假人插件，最低支持版本为 Paper 1.21.11。首版目标同时支持 Paper 1.21.11 和 26.1.2。

项目不优先兼容 Spigot/Bukkit 旧实现，核心能力围绕 Paper 高版本和 NMS 假玩家实现设计。

## 版本策略

- 最低版本：Paper 1.21.11。
- 首版支持：Paper 1.21.11、Paper 26.1.2。
- 26.1.x 视为 1.21 之后的新版本命名规则，是连续版本线。
- Java 基线：Java 21。
- 构建工具：Gradle Kotlin DSL。
- NMS 访问：优先使用 paperweight-userdev。

## 技术路线

插件以真实服务端假玩家为主，packet-only 外观假人为辅。

真实假人需要具备玩家级行为能力，包括背包、装备、经验、攻击、挖掘、使用、睡觉、传送、区块加载等。因此核心实现不能只做客户端 NPC 包，而应创建服务端可追踪的假玩家实体。

幽灵模式用于只保留外观，不作为真实实体，不加载区块，不执行真实交互动作。

## 推荐目录结构

```text
Dummy/
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  src/main/java/
    .../DummyPlugin.java
    .../command/
    .../dummy/
    .../action/
    .../config/
    .../gui/
    .../skin/
    .../storage/
    .../nms/
  src/main/resources/
    paper-plugin.yml
    config.yml
    messages.yml
```

## 模块边界

NMS 相关逻辑必须集中隔离，命令、配置、GUI、存储和业务逻辑不直接依赖具体 NMS 类。

建议抽象接口：

```java
interface FakePlayerAdapter {
    DummyHandle spawn(DummyCreateRequest request);
    void remove(DummyHandle handle);
    void teleport(DummyHandle handle, Location location);
    void performAction(DummyHandle handle, DummyAction action);
}
```

首版可以先做单模块项目，后续如果需要长期跨版本维护，再拆分为 core、api、nms-v1_21_11、nms-26_1_2 等模块。

## 指令规划

基础指令组为 `/dummy`。

首批核心指令：

- `/dummy spawn <name>`：召唤假人。
- `/dummy remove <name|all>`：移除指定假人或全部假人。
- `/dummy list`：查看假人列表。
- `/dummy reload`：重载配置文件。

管理类指令：

- `/dummy config <name> <key> <value>`：配置假人功能。
- `/dummy skin <name> ...`：修改假人皮肤。
- `/dummy exp <name> [amount|all]`：转移假人经验到玩家。
- `/dummy inv <name>`：查看和管理假人背包、装备栏等。
- `/dummy tpto <name>`：将玩家传送到假人处。
- `/dummy tphere <name>`：将假人传送到当前玩家处。
- `/dummy tps <name>`：交换玩家和假人的位置。

动作类指令：

- `/dummy actions <name> attack`：攻击。
- `/dummy actions <name> chat <message>`：聊天。
- `/dummy actions <name> command <command>`：执行命令。
- `/dummy actions <name> drop`：丢弃物品。
- `/dummy actions <name> hold <slot>`：切换手持槽位。
- `/dummy actions <name> jump`：跳跃。
- `/dummy actions <name> look <yaw> <pitch>`：转向。
- `/dummy actions <name> mine`：挖掘。
- `/dummy actions <name> mount`：骑乘。
- `/dummy actions <name> move`：移动。
- `/dummy actions <name> sleep`：睡觉。
- `/dummy actions <name> sneak`：潜行。
- `/dummy actions <name> sprint`：疾跑。
- `/dummy actions <name> swap`：交换主副手。
- `/dummy actions <name> use`：使用物品或交互方块。
- `/dummy actions <name> wakeup`：起床。
- `/dummy actions <name> stop [action]`：停止当前动作或指定动作。

动作执行参数建议统一为：

```text
/dummy actions <name> <action> once [args...]
/dummy actions <name> <action> repeat <intervalTicks> [durationTicks] [args...]
/dummy actions <name> stop [action]
```

## 假人功能规划

基础功能：

- 召唤、移除、列表、重载。
- 假人持久化。
- 插件关闭时安全移除假人。
- 服务器启动后按配置恢复假人。

交互功能：

- 右键假人打开假人容器 GUI。
- GUI 支持假人配置。
- GUI 支持管理假人背包、装备栏等。
- GUI 支持管理假人皮肤。

配置功能：

- 无敌模式。
- 是否启用碰撞箱。
- 幽灵模式。
- 是否加载区块。
- 是否显示在 Tab 列表。
- 名字显示格式。

皮肤功能：

- 使用正版玩家名拉取皮肤。
- 使用 texture value/signature 设置皮肤。
- 本地缓存皮肤数据，减少 Mojang API 请求。

## 数据设计

每个假人建议保存以下数据：

```yaml
id: uuid
name: dummy_name
world: world
x: 0.0
y: 64.0
z: 0.0
yaw: 0.0
pitch: 0.0
skin:
  type: player|texture|none
  value: ""
  signature: ""
settings:
  invulnerable: false
  collision: true
  ghost: false
  chunk-loader: false
  show-in-tab: true
inventory: []
experience:
  level: 0
  progress: 0.0
  total: 0
```

首版可以使用 YAML 保存配置和假人数据。背包数据先使用 Bukkit ItemStack 序列化，后续如果数据量或性能有需求，再改为 SQLite 或独立二进制数据。

## 动作系统设计

动作系统由 ActionScheduler 统一调度，所有假人动作在主线程执行。

动作类型：

- 单次动作：执行一次后结束。
- 周期动作：按自定义 tick 间隔持续执行。
- 有限持续动作：运行指定 tick 后自动停止。
- 可停止动作：支持通过 stop 停止。

互斥规则：

- 移动、挖掘、使用等需要状态连续性的动作应互斥。
- 聊天、切换手持、转向等瞬时动作可以与部分动作并行。
- stop 默认停止当前主要动作，也可以指定动作类型停止。

## 实施顺序

第一阶段：项目骨架

- 初始化 Gradle Kotlin DSL 项目。
- 接入 Paper API 和 paperweight-userdev。
- 创建 paper-plugin.yml、config.yml、messages.yml。
- 建立命令框架和权限节点。

第二阶段：假人核心闭环

- 实现 `/dummy spawn`。
- 实现 `/dummy remove`。
- 实现 `/dummy list`。
- 实现插件关闭时清理假人。
- 验证假人能被真实玩家看见。

第三阶段：持久化和基础管理

- 保存假人位置、皮肤、配置、经验、背包。
- 服务器启动后恢复假人。
- 实现 `/dummy reload`。
- 实现传送类指令。

第四阶段：GUI 和背包

- 实现右键假人打开 GUI。
- 实现 `/dummy inv`。
- 支持背包、装备栏管理。
- 支持经验转移。

第五阶段：动作系统

- 建立 ActionScheduler。
- 实现 stop、look、hold、chat、command 等简单动作。
- 实现 attack、use、mine、move 等复杂动作。
- 确保动作尽量走服务端原生交互逻辑，触发事件并兼容保护插件。

第六阶段：高级功能

- 实现皮肤管理。
- 实现区块加载。
- 实现无敌模式、碰撞箱开关。
- 实现幽灵模式。
- 完成配置、消息、权限和文档。

## 主要风险

- Paper 高版本和 NMS 构造器可能变化，必须隔离 NMS 实现。
- 真实假人可能影响在线玩家列表、Tab 列表、玩家事件和其他插件逻辑，需要明确过滤策略。
- 攻击、使用、挖掘等动作如果绕过原生逻辑，会导致事件、权限、保护插件和物品耐久不一致。
- 幽灵模式与真实假人能力不同，需要在命令和 GUI 中明确限制不可用功能。

## 首版完成标准

- 插件可在 Paper 1.21.11 和 26.1.2 启动。
- 可以召唤、移除、列出假人。
- 假人重启后可恢复。
- 玩家可右键假人打开管理 GUI。
- 可以管理假人背包和装备。
- 可以执行基础动作。
- 可以安全关闭插件且不残留异常实体。
