# Phase B — 内置默认模式的配置驱动选择策略设计

## 1. 背景

Phase A 已完成运行时扩展装配内核，当前仍存在两个问题：

1. `RuntimeExtensionResolver` 内部维护硬编码内置默认映射，配置驱动能力不完整。
2. 默认值来源未外置，无法清晰表达“用户配置缺失时的系统兜底”。

Phase B 聚焦“内置默认模式”下的配置驱动选择，不引入外部 jar 动态加载与新插件契约。

---

## 2. 目标与非目标

### 2.1 目标

1. 保持现有 SPI 机制与契约不变（`@SPI` + `META-INF/gateway`）。
2. 通过默认配置文件提供系统内置默认实现名。
3. 用独立策略组件统一选择优先级，移除 resolver 硬编码默认映射。
4. 保持强兼容：已有 `gateway.properties` 可继续运行。

### 2.2 非目标

1. 不新增 `GatewayBuiltinPlugin` 或其他插件契约接口。
2. 不做外部插件包加载、隔离类加载器、热插拔。
3. 不进入 Phase C 的配置 DSL/Schema 重构。

---

## 3. 选择优先级（最终规则）

对每个扩展点（`transport/forwarder/serviceDiscovery/loadBalancer/routeLocator`）统一执行：

1. `gateway.properties` 显式值
2. `gateway-defaults.properties` 默认值
3. `@SPI` 默认值
4. 启动失败（fail-fast）

说明：

1. 显式值或默认值存在但无法解析到实现时，直接失败。
2. 失败信息必须包含：扩展点类型、给定实现名、可用实现列表。

---

## 4. 架构改动

### 4.1 新增策略组件 `SpiSelectionPolicy`

职责：

1. 接收三层候选来源（显式/默认配置/@SPI）并给出最终实现名。
2. 不负责类加载和实例化。

建议接口：

```java
public interface SpiSelectionPolicy {
    <T> String selectName(
            Class<T> spiType,
            String explicitName,
            String defaultConfigName,
            java.util.Optional<String> spiDefaultName
    );
}
```

默认实现：`DefaultSpiSelectionPolicy`，按第 3 节优先级返回。

### 4.2 调整 `RuntimeExtensionResolver`

1. 删除内部硬编码 `builtinDefaultNames`。
2. 新增对 `SpiSelectionPolicy` 的依赖。
3. `resolve(...)` 接收 `explicitName` 与 `defaultConfigName`，调用策略获得最终实现名后完成实例化。

### 4.3 默认配置文件

新增：

`gateway-runtime/src/main/resources/gateway-defaults.properties`

内容示例：

```properties
gateway.spi.transport=jdkHttp
gateway.spi.forwarder=http
gateway.spi.serviceDiscovery=inMemory
gateway.spi.loadBalancer=roundRobin
gateway.spi.routeLocator=inMemory
```

---

## 5. 配置加载设计

`GatewayRuntimeConfigLoader` 增量改造：

1. 先加载 `gateway-defaults.properties`（classpath，可选但建议存在）。
2. 再加载用户 `gateway.properties`。
3. 在 `GatewayRuntimeConfig` 中同时保留：
   - `spiConfig`（用户显式层）
   - `spiDefaults`（默认配置层）

示意结构：

```java
public record GatewayRuntimeConfig(
        int port,
        StaticConfig staticConfig,
        SpiConfig spiConfig,
        SpiConfig spiDefaults,
        List<RouteConfig> routes,
        Map<String, List<String>> serviceInstances
) { ... }
```

---

## 6. 启动链路调整

`GatewayApplication` 在调用 resolver 时传入两层值：

1. `explicitName = config.spiConfig().transport()`（示例）
2. `defaultName = config.spiDefaults().transport()`

其余扩展点同理，保证选择决策全部由策略组件统一处理。

---

## 7. 错误处理

1. 默认配置文件缺失：允许启动，但仅回退 `@SPI`（记录 warn）。
2. 默认配置值非法（空白/不存在实现）：启动失败。
3. 最终无可用实现名：启动失败并提示缺失来源。

---

## 8. 测试策略

### 8.1 新增测试

1. `gateway-runtime`：
   - `DefaultSpiSelectionPolicyTest`
   - 扩展 `RuntimeExtensionResolverTest`（显式 > 默认配置 > @SPI）

### 8.2 更新测试

1. `GatewayRuntimeConfigLoaderTest`：
   - 覆盖默认配置文件加载
   - 覆盖用户显式配置覆盖默认配置
2. 维持 Phase A 解析与 fail-fast 测试不回退。

---

## 9. 完成判定（Phase B DoD）

1. resolver 不再包含硬编码默认映射。
2. 默认实现由 `gateway-defaults.properties` 驱动。
3. 选择优先级符合：显式 > 默认配置 > `@SPI` > 失败。
4. 旧配置不改可运行（默认配置存在时稳定兜底）。
5. Phase B 新增/更新测试通过。

