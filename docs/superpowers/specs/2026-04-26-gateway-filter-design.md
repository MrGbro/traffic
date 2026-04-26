# gateway-filter 模块 — Phase2 设计文档

## 范围
在 phase1 已完成的 `gateway-common`、`gateway-spi`、`gateway-core` 骨架之上，进入 phase2：建立符合总体架构的 Filter 生命周期执行体系。

phase2 的重点不是一次性把所有能力全部落完，而是：

1. 保留 filter 体系的最终能力方向
2. 修正模块边界，确保与 `docs/arhctechture.md` 一致
3. 把实现拆成小阶段，逐步收敛到完整生命周期执行模型

## Phase2 总目标

phase2 的最终目标保持不变：

- `gateway-filter` 成为流程型扩展模型
- 生命周期执行模型变成：`Phase 分段 + 每个 Phase 内独立 FilterChain + gateway-core 统一编排`
- 支持有序执行、短路终止、异常进入统一响应阶段
- 为后续 `gateway-plugin` 组合 SPI + Filter 留出稳定扩展点

phase2 的设计原则：

- `gateway-core` 仍然是微内核，负责调度，不下沉到 filter 模块
- `gateway-filter` 只定义 Filter 扩展模型，不复制或覆盖 core 引擎
- `gateway-spi` 继续只承载能力型 SPI 和扩展元信息

## 与总体架构的对齐

根据 `docs/arhctechture.md`：

- `gateway-core` 是微内核，职责是生命周期调度
- `gateway-filter` 是流程扩展体系，职责是生命周期拦截模型
- `gateway-plugin` 未来组合 Filter + SPI

根据 `AGENTS.md`：

- `gateway-core` 调用 SPI + Filter，不能反向依赖

因此 phase2 的核心调整是：

- 保留当前设计文档描述的能力目标
- 取消 “gateway-filter 覆盖 gateway-core 执行引擎” 的做法
- 改成 “gateway-filter 定义模型，gateway-core 负责集成和调度”

## 依赖关系

phase2 调整后的依赖关系：

```text
gateway-common  -> 无依赖
gateway-spi     -> depends on gateway-common
gateway-filter  -> depends on gateway-common + gateway-spi
gateway-core    -> depends on gateway-filter + gateway-spi
```

说明：

- `gateway-filter` 不能依赖 `gateway-core`
- `gateway-core` 作为微内核，负责统一编排 filter 生命周期
- `gateway-spi` 中的 `@Activate` 可以在 phase2 被复用为 filter 的排序元数据

## 模块职责

### gateway-filter

职责：

- 定义 Filter 契约
- 定义 FilterChain 与调用模型
- 定义 phase-specific Filter 类型
- 提供抽象基类和排序辅助模型

不负责：

- 不拥有 `GatewayEngine`
- 不拥有 `ExecutionChain`
- 不拥有生命周期全局调度能力

### gateway-core

职责：

- 按 Phase 编排生命周期执行
- 为每个 Phase 调度独立 FilterChain
- 管理 terminate / error / RESPONSE 跳转
- 提供 Filter 注册入口和执行入口

### gateway-spi

职责保持 phase1 不变：

- 定义能力型 SPI
- 提供 `@Activate(group, order)` 等通用扩展元信息

phase2 内不新增 filter 专属 SPI 扫描能力。

## Phase2 分阶段实现

phase2 拆成四个子阶段，最终收敛到完整能力。

---

## Phase2-1：Filter 模型层落地

### 目标

先建立 `gateway-filter` 模块本身，让 Filter 体系的术语、结构和职责与架构文档对齐。

### 交付物

新增 `gateway-filter` 子模块。

目录结构采用与架构文档一致的形式：

```text
gateway-filter/
└── src/main/java/io/homeey/traffic/filter/
    ├── core/
    │   ├── GatewayFilter.java
    │   ├── FilterChain.java
    │   ├── DefaultFilterChain.java
    │   └── FilterInvoker.java
    ├── support/
    │   ├── AbstractFilter.java
    │   └── OrderedFilter.java
    └── phase/
        ├── PreRouteFilter.java
        ├── RouteFilter.java
        ├── PreForwardFilter.java
        ├── PostForwardFilter.java
        └── ResponseFilter.java
```

### 核心接口方向

phase2-1 建议避免 `gateway-filter` 直接依赖 core 的 `GatewayContext`，因此在 `gateway-filter` 内定义一个窄上下文接口，由 `gateway-core` 中的 `GatewayContext` 实现。

建议接口方向：

```java
public interface FilterContext {
    String requestId();
    Phase currentPhase();
    void currentPhase(Phase phase);

    <T> T attribute(String key);
    void attribute(String key, Object value);

    boolean isTerminated();
    void terminate(int statusCode, String reason);

    boolean hasError();
    Throwable error();
    void error(Throwable error);
}
```

```java
@FunctionalInterface
public interface GatewayFilter {
    void filter(FilterContext context, FilterChain chain) throws Exception;
}
```

```java
@FunctionalInterface
public interface FilterChain {
    void filter(FilterContext context) throws Exception;
}
```

### 排序能力范围

phase2-1 只支持：

- `group`
- `order`

即复用 phase1 中 `gateway-spi` 已存在的 `@Activate(group, order)`。

本阶段明确不做：

- 条件激活
- SPI 自动发现 filter
- 插件装配

### 测试

- `DefaultFilterChainTest`：顺序执行
- `DefaultFilterChainShortCircuitTest`：不调用 next 时短路
- `FilterInvokerTest`：统一调用包装行为
- `AbstractFilterTest`：模板基类行为

---

## Phase2-2：gateway-core 接入 Filter 执行模型

### 目标

把 phase1 中基于 `Runnable` 的生命周期骨架，升级为按 Phase 执行 FilterChain 的生命周期内核。

### 改造原则

- 改造发生在 `gateway-core`
- 不在 `gateway-filter` 中复制或覆盖 `GatewayEngine` / `ExecutionChain` / `LifecycleManager`
- `gateway-core` 通过依赖 `gateway-filter` 获得流程扩展能力

### 交付物

#### GatewayContext 扩展

phase1 的 `GatewayContext` 仅持有 requestId、phase、attributes。phase2 需要增加执行状态，并实现 `FilterContext`：

```java
boolean isTerminated();
void terminate(int statusCode, String reason);
Optional<Integer> statusCode();
Optional<String> terminateReason();

boolean hasError();
Throwable error();
void error(Throwable error);
```

#### LifecycleManager 升级

phase1 的 `LifecycleManager` 当前是：

```java
void onPhase(Phase phase, Runnable handler)
```

phase2 中升级为 Filter 注册入口：

```java
LifecycleManager registerFilter(Phase phase, GatewayFilter filter)
LifecycleManager registerFilter(Phase phase, GatewayFilter filter, Activate metadata)
```

`onPhase(Phase, Runnable)` 的处理方式有两种：

- 方案 A：保留作为过渡兼容接口
- 方案 B：phase2 明确废弃，测试同步升级为 Filter 模型

建议采用方案 B，使 phase2 成为真正的生命周期 Filter 升级节点。

#### ExecutionChain 升级

phase1 的 `ExecutionChain` 是：

- 遍历 `Phase.values()`
- 执行每个 Phase 下的 `Runnable`

phase2 中升级为：

- 遍历生命周期 Phase
- 取出该 Phase 对应的 Filter 列表
- 构造并执行独立 `FilterChain`

这里的 Phase -> Filter 列表映射属于 core 的编排准备工作，因此应由 `gateway-core` 持有，而不是放入 `gateway-filter` 作为全局引擎组件。

#### GatewayEngine 升级

phase2 中 `GatewayEngine` 负责：

- 注册 Filter
- 驱动 `ExecutionChain`
- 在异常/终止情况下统一管理执行流转

### 测试

- `GatewayEngineLifecycleTest`：多 Phase 顺序执行
- `LifecycleManagerRegisterFilterTest`：Filter 注册正确进入对应 Phase
- `GatewayContextStateTest`：terminate/error 状态更新正确

---

## Phase2-3：生命周期闭环补全

### 目标

让 Filter 调度模型真正符合网关生命周期语义，而不是仅仅“能跑链”。

### 关键语义

#### 1. 正常流

```text
PRE_ROUTE -> ROUTE -> PRE_FORWARD -> FORWARD -> POST_FORWARD -> RESPONSE
```

#### 2. terminate 流

- 某个 Filter 内调用 `context.terminate(statusCode, reason)`
- 当前 Filter 不再调用 `next`
- 当前 Phase 结束
- 中间剩余普通 Phase 被跳过
- 生命周期仍然进入 `RESPONSE`

这意味着 terminate 后的行为不是简单 `break`，而是“折叠到 RESPONSE Phase”。

#### 3. exception 流

- Filter 抛出异常
- core 捕获异常并写入 `context.error(...)`
- 同时标记终止状态
- 生命周期进入 `RESPONSE`

### 设计约束

- `RESPONSE` 不能因为 terminate/exception 被一起跳过
- `ExecutionChain` 要显式区分“停止整个生命周期”和“跳转到 RESPONSE”

### 测试

- `TerminateShouldStillEnterResponseTest`
- `ExceptionShouldStillEnterResponseTest`
- `PhaseOrderExecutionTest`

---

## Phase2-4：为 plugin 化预留稳定扩展点

### 目标

不提前实现 `gateway-plugin`，但让 phase2 形成的 Filter 模型可以自然支撑后续 plugin 化。

### 本阶段做什么

- 稳定 Filter 注册元信息模型
- 继续复用 `@Activate(group, order)`
- 规定无注解 Filter 的默认行为：默认组、默认顺序
- 为未来插件装配保留接入点

### 本阶段不做什么

- 不实现 Filter SPI 自动扫描
- 不实现 plugin 模块加载器
- 不引入复杂条件激活表达式

### 测试

- `ActivateMetadataOrderingTest`
- `DefaultActivateBehaviorTest`

## phase2 最终退出条件

当以下能力全部满足时，可以认为 phase2 完成：

1. `gateway-filter` 模块建立完成，结构与架构稿对齐
2. `gateway-core` 能按 Phase 调度独立 FilterChain
3. 同一 Phase 内 Filter 能按顺序执行
4. Filter 可通过不调用 next 短路链路
5. terminate 后后续普通 Phase 被跳过，但 `RESPONSE` 仍执行
6. exception 后上下文记录错误，并进入 `RESPONSE`
7. Filter 排序至少支持 `group + order`
8. 为后续 `gateway-plugin` 保留稳定的接入点

## 非目标

phase2 明确不包含以下内容：

- routing 能力实现
- forward 能力实现
- cluster/service discovery 能力实现
- plugin 模块自动装配
- 异步 Filter 语义
- 真正的条件激活表达式系统

这些属于 phase3 及后续阶段。

## 测试总览

phase2 建议测试覆盖：

- FilterChain 顺序执行
- FilterChain 短路
- Phase 生命周期顺序
- terminate -> RESPONSE
- exception -> RESPONSE
- group + order 排序
- 默认 metadata 行为
- GatewayContext 状态更新

## 技术约定

- Java 17：records、sealed classes、text blocks 可按需使用
- 包根：`io.homeey.traffic`
- `gateway-filter` 包建议为：`io.homeey.traffic.filter`
- phase2 排序只使用 `@Activate(group, order)`
- phase2 不声明支持条件激活
- UTF-8 编码

## 影响的现有文件

| 文件 | 操作 |
|------|------|
| `pom.xml` | modules 中增加 `gateway-filter`，并补充依赖关系 |
| `gateway-core/.../GatewayContext.java` | 扩展为实现 `FilterContext`，增加 terminate/error 状态 |
| `gateway-core/.../ExecutionChain.java` | 从 Runnable 调度升级为按 Phase 执行 FilterChain |
| `gateway-core/.../GatewayEngine.java` | 增加 Filter 注册与执行入口 |
| `gateway-core/.../LifecycleManager.java` | 升级为 Filter 注册入口 |
| `gateway-core` 测试 | 从 phase1 骨架测试升级为生命周期 Filter 测试 |

## 评审后的调整结论

phase2 保留当前 filter 设计文档想表达的能力方向，但不再采用 “gateway-filter 覆盖 gateway-core 执行引擎” 的实现策略。

新的实现策略是：

- `gateway-filter` 负责定义流程扩展模型
- `gateway-core` 负责集成并调度这些模型
- phase2 通过四个子阶段渐进实现最终能力闭环

这套方案与 `docs/arhctechture.md`、phase1 已完成内容和项目边界约束保持一致。
