# LggGoodFriday / The Jinx

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-green.svg)](https://www.minecraft.net/)
[![Loader](https://img.shields.io/badge/Loader-Fabric-DB2338.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)

> 仓库名 **LggGoodFriday**，模组 ID **`thejinx`**，展示名 **The Jinx**。  
> Fabric 服务端/客户端玩法向模组：开启「倒霉蛋游戏」后，一名或多名玩家会获得大量负面与整活规则，同时也有少量「以毒攻毒」式的补偿机制。

---

## 目录

- [依赖](#依赖)
- [安装](#安装)
- [管理员指令](#管理员指令)
- [玩法与机制](#玩法与机制)
- [从源码构建](#从源码构建)
- [许可证](#许可证)

---

## 依赖

| 依赖 | 说明 |
|------|------|
| [Fabric Loader](https://fabricmc.net/use/installer/) | `>= 0.18.6` |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 与 1.21.11 匹配的版本 |
| [Cardinal Components API](https://github.com/LadysnakeMC/Cardinal-Components-API) | Entity 组件，用于玩家数据同步（`cca_version` 见 `gradle.properties`） |
| **Java** | **21+** |

---

## 安装

1. 安装对应版本的 **Fabric Loader**（Minecraft **1.21.11**）。
2. 将 **Fabric API** 与本模组 jar 放入 `.minecraft/mods`（单机）或服务器 `mods` 目录。
3. 确保已安装 **Cardinal Components**（本模组 `fabric.mod.json` 中 `include` 了 CCA，一般玩家只需拖入本模组 jar；若使用精简打包请自行确认依赖完整）。

---

## 管理员指令

需要 **游戏管理员（gamemaster）级别** 或等效权限（控制台/命令方块等视为满足）。

| 指令 | 作用 |
|------|------|
| `/jinx start` | 开启倒霉蛋游戏；若当前无人带倒霉蛋标记，会优先选在线玩家 **`TheLgg_`**，否则随机一名在线玩家并设为倒霉蛋；**自动显示侧边栏死亡榜**。 |
| `/jinx stop` | 关闭游戏，清除所有倒霉蛋身份，**移除本模组创建的死亡榜**。 |
| `/jinx status` | 查看游戏开关、**当前所有倒霉蛋**、交互飞升开关。 |
| `/jinx set` | 指定 **唯一倒霉蛋**（先清空其他人倒霉蛋身份，再随机默认名或随机玩家）。 |
| `/jinx set <player>` | 将指定玩家设为 **唯一倒霉蛋**。 |
| `/jinx screenlift` | 查询「交互飞升」是否开启。 |
| `/jinx screenlift <true\|false>` | 开关倒霉蛋打开容器界面时的抬升效果。 |

---

## 玩法与机制

以下效果在 **`/jinx start` 之后** 且玩家处于「倒霉蛋」身份时生效（`JinxRuleHelper`：`游戏已开` + `玩家 isJinx`）。

### 倒霉蛋身份（支持多人）

- 身份保存在玩家 **CCA 组件**中，**可同时存在多名倒霉蛋**。
- **`/jinx set`** 会强制回到 **唯一倒霉蛋**（管理员控盘用）。

### 同化之卵（`thejinx:assimilation`）

- 外观使用 **鸡蛋** 模型；**丢在地上会自动消失**（防止囤积转移）。
- **倒霉蛋死亡次数 ≥ 100** 且尚未使用过同化时，会获得「同化之卵」提示与物品（背包满则掉落）。
- **主手持同化之卵攻击另一名玩家**时：  
  - **不会**转移你的倒霉蛋身份；  
  - 会将对方 **同步为倒霉蛋**（双方同时承担倒霉蛋规则）。  
- 若目标 **已是倒霉蛋**：提示且 **不消耗** 物品。

### 侧边栏死亡榜

- **`/jinx start`** 后自动创建记分项 **`thejinx_deaths`**（原版死亡计数），标题「死亡榜」。  
- **`/jinx stop`** 时若侧边栏正是该目标则清空并删除，避免误删他人记分板。

### 交互飞升与重力

- 倒霉蛋 **打开任意界面**（背包/容器等）时，若开启「交互飞升」：每 tick **抬升位置**、**无重力**、并给竖直速度，避免被其他逻辑覆盖。  
- 刚打开界面时有数 tick **更强上冲**。  
- **关闭界面后不再给予缓降药水**（当前版本已取消）。  
- 倒霉蛋下落时 **额外重力**（未处于缓降时更明显）。  
- **被举起**或 **人体 TNT 飞行中**：不应用飞升改写，避免抵消投掷初速度。

### 人体 TNT

- **非倒霉蛋**对倒霉蛋 **使用（交互）** 可将其 **举起**（优先骑乘，失败则跟随抬升）。  
- 举起后，**客户端在无前 GUI 时按下攻击键** 会发包触发 **掷出倒霉蛋**；掷出方向与举者朝向相关，带 **多 tick 速度强化** 与 **前上方出生点**，防止「原地弹」。  
- 飞行中 **撞到方块** 触发 **TNT 类型爆炸**（破坏/伤害规则与原版 TNT 爆炸一致）；倒霉蛋本人在爆炸后 **额外补一次爆炸类型伤害**（解决中心实体常不吃爆炸伤害的问题）。  
- **被举期间**：倒霉蛋 **免疫摔落伤害**，并持续 **清零 fallDistance**。

### 矿物三倍掉落

- 倒霉蛋破坏 **常规矿物标签**（`#c:ores`）方块时：在原版掉落基础上 **再掉 2 倍**，合计 **3 倍**。

### 断肢与冲刺

- 倒霉蛋 **持续冲刺** 约每 **5 秒** 随机 **失去** 一个肢体部位；**不冲刺** 约每 **5 秒** 随机 **恢复** 一个已缺部位。  
- 缺失部位会触发 **反胃 / 挖掘疲劳+虚弱 / 缓慢** 等；缺腿会 **缩小碰撞箱**；客户端 **隐藏对应模型部位** 并调整上身位置。

### 全局挖掘疲劳与死亡狂欢

- 游戏进行中：**非倒霉蛋** 会被周期性刷新 **挖掘疲劳 V**；倒霉蛋会 **清除** 挖掘疲劳。  
- **任意倒霉蛋死亡**：若当前不在狂欢中，则进入 **30 秒死亡狂欢**——**全员清除挖掘疲劳**，并每人获得 **随机一种正面效果**（速度、急迫、力量等池子）。狂欢内倒霉蛋再次死亡 **不会** 重置计时。  
- 狂欢结束后：**非倒霉蛋** 重新获得挖掘疲劳 V。

### 跳跃召雷

- 倒霉蛋每次 **跳跃** 会在脚下 **召唤闪电**（注意防火与建筑安全）。

### 客户端同步

- 打开/关闭界面、攻击键掷出等通过 **Fabric Networking** 与服务器同步（详见 `JinxNetworking` / `JinxClientNetworking`）。

---

## 从源码构建

```bash
./gradlew build
```

产物位于 `build/libs/`。

**JDK 路径**：请勿把本机 `org.gradle.java.home` 写进仓库内的 `gradle.properties`。可写到用户目录 `~/.gradle/gradle.properties`（Windows 即 `C:\Users\<你>\.gradle\gradle.properties`）。

---

## 许可证

**All Rights Reserved**（见 `fabric.mod.json` 的 `license` 字段）。  
未获作者书面许可前，请勿擅自再分发、商用或修改后公开发布；个人学习、私有服务器使用请自行与作者约定。

---

## 相关链接

- 仓库：[https://github.com/ShuangJinl/LggGoodFriday](https://github.com/ShuangJinl/LggGoodFriday)
