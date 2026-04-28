# Phase A — SPI 装配内核重构设计

## 1. 背景与问题

当前 runtime 启动链路存在以下结构性问题：

1. `GatewayApplication` 直接 `new` 具体实现（`HttpForwarder`、`JdkHttpTransportServer`、`RoundRobinLoadBalancer`、`InMemoryServiceDiscovery`），SPI 机制未真正参与运行时装配。
2. `ExtensionLoader` 无法按 name 稳定选择实现，`META-INF/gateway` 注册约束不完整，难以配置驱动扩展替换。
3. 默认行为散落在业务代码中（如 no-op 回退），导致扩展选择逻辑不可观测、不可统一测试。

本阶段目标是先把“如何选择与装配实现”做正确，为后续 B（模块插件化）与 C（配置模型重构）建立稳定底座。

---

## 2. 目标与非目标

### 2.1 目标

1. 建立统一扩展选择优先级：
   - `配置显式实现名 > SPI 默认实现名(@SPI value) > 内置默认映射`
2. 实现运行时装配分层：
   - 注册读取层（ExtensionLoader）
   - 策略解析层（RuntimeExtensionResolver）
   - 实例化层（ExtensionInstantiator/缓存）
3. 启动链路去硬编码：
   - `GatewayApplication` 不再直接 `new` 具体实现。
4. 强兼容旧配置：
   - 既有配置可直接运行，不要求一次性改造。
5. 支持 `META-INF/gateway` 的新旧注册格式兼容解析。

### 2.2 非目标

1. 不在 A 阶段引入插件隔离类加载器。
2. 不在 A 阶段重写路由/转发业务逻辑。
3. 不在 A 阶段改造配置 DSL 语法结构（仅增量新增 SPI 选择键）。

---

## 3. 设计原则

1. 单一职责：扩展“读取、选择、实例化”三层分离。
2. 显式优先：配置显式指定永远覆盖默认链路。
3. 失败可诊断：配置错误、实现缺失、类型不匹配必须 fail-fast。
4. 向后兼容：旧配置与旧注册格式可继续运行。

---

## 4. 架构设计

### 4.1 分层模型

1. `ExtensionLoader<T>`（读取层）
- 责任：读取 `META-INF/gateway/<SPI接口全限定名>` 并解析候选实现。
- 输出：`Map<String, Class<? extends T>>`。
- 不负责优先级策略。

2. `RuntimeExtensionResolver`（策略层）
- 输入：扩展点类型、配置中的实现名（可空）。
- 决策顺序：
  1. 配置显式实现名
  2. `@SPI` 默认实现名
  3. runtime 内置默认映射
- 输出：最终选择的实现名。

3. `ExtensionInstantiator`（实例化层）
- 责任：根据“实现名 -> 实现类”创建实例并缓存（单例）。
- 校验：实现类必须实现目标 SPI 接口。

4. `GatewayApplication`（组合根）
- 责任：读取配置、调用 resolver 获取扩展实例、组装 bootstrap。
- 禁止直接 `new` 具体扩展实现（除 resolver/instantiator 内部）。

### 4.2 关键交互流程

1. 启动读取配置
2. 对每个扩展点调用 resolver：
   - `TransportServer`
   - `Forwarder`
   - `ServiceDiscovery`
   - `LoadBalancer`
   - `RouteLocator`（运行时扩展点）
3. resolver 结合 loader + SPI 默认 + 内置映射得出实现名
4. instantiator 创建实例并返回
5. 组合根装配 `GatewayBootstrap`

---

## 5. 配置与兼容策略

### 5.1 新增配置键（可选）

```properties
gateway.spi.transport=<name>
gateway.spi.forwarder=<name>
gateway.spi.serviceDiscovery=<name>
gateway.spi.loadBalancer=<name>
gateway.spi.routeLocator=<name>
```

### 5.2 强兼容语义

1. 若配置了 `gateway.spi.*`
- 必须按该 name 解析；解析失败则启动失败。

2. 若未配置 `gateway.spi.*`
- 使用 `@SPI("defaultName")`；若无，再落到内置默认映射。

3. 旧配置
- 原有业务配置（routes/services/static）继续生效，无需修改。

### 5.3 A 阶段内置默认映射

1. `TransportServer` -> `jdkHttp`
2. `Forwarder` -> `http`
3. `ServiceDiscovery` -> `inMemory`
4. `LoadBalancer` -> `roundRobin`
5. `RouteLocator` -> `inMemory`

---

## 6. SPI 注册规范

### 6.1 路径规范

`META-INF/gateway/<SPI接口全限定名>`

### 6.2 条目格式

推荐新格式（主格式）：

```text
name=fully.qualified.ImplClass
```

兼容旧格式（保留）：

```text
fully.qualified.ImplClass
```

旧格式解析时自动推导 name（规则在实现文档里固定化并测试覆盖）。

### 6.3 本阶段预期注册项

1. `io.homeey.traffic.spi.contract.transport.TransportServer`
2. `io.homeey.traffic.spi.contract.forward.Forwarder`
3. `io.homeey.traffic.spi.contract.cluster.ServiceDiscovery`
4. `io.homeey.traffic.spi.contract.cluster.LoadBalancer`
5. `io.homeey.traffic.routing.locator.RouteLocator`（若作为运行时扩展点接入）

---

## 7. 错误处理

1. 配置指定的实现名不存在
- 启动失败，错误包含扩展点类型、实现名、可用实现列表。

2. 注册文件存在非法行
- 启动失败，包含文件路径与原始行。

3. 类加载或构造失败
- 启动失败，保留根异常栈。

4. 实现类类型不匹配
- 启动失败，提示接口与实现类不兼容。

---

## 8. 测试策略

### 8.1 `gateway-spi` 单测

1. 新格式 `name=class` 可正确解析。
2. 旧格式 `class` 可兼容解析。
3. 重名冲突、非法行、类型不匹配触发 fail-fast。

### 8.2 `gateway-runtime` 单测

1. resolver 优先级验证：
   - 配置显式优先
   - `@SPI` 默认回退
   - 内置默认映射回退
2. 错误实现名触发启动失败。

### 8.3 启动集成验证

1. `GatewayApplication` 装配路径不再硬编码 `new` 具体实现。
2. 通过配置切换实现名能改变实际装配结果。

---

## 9. 范围分解与后续阶段关系

1. Phase A（本设计）：装配内核正确化与兼容接入。
2. Phase B：模块插件化（扩展实现模块进一步解耦与可插拔发布）。
3. Phase C：配置模型重构（从属性键升级为更系统的配置模型/校验体系）。

---

## 10. A 阶段完成判定（DoD）

满足以下条件即视为 A 阶段完成：

1. 启动链路不再硬编码具体扩展实现。
2. SPI 注册可驱动运行时实现选择。
3. 优先级严格符合：`配置 > SPI默认 > 内置默认`。
4. 旧配置保持可运行（强兼容）。
5. A 阶段新增测试通过。
