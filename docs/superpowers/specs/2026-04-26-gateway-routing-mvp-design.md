# gateway-routing MVP — 设计文档

## 范围
在 phase2 已完成的 `gateway-core + gateway-filter` 生命周期执行模型上，新增 `gateway-routing` 子模块，实现最小可运行路由能力（Routing MVP）。

本阶段目标：

- 在 `ROUTE` Phase 做路由匹配
- 匹配成功写入路由结果到 `GatewayContext`
- 匹配失败终止请求并进入 `RESPONSE`

## 目标与非目标

### 目标

1. 新建 `gateway-routing` 模块，落地路由模型、匹配器、定位器
2. 支持 `Path + Method` 的 AND 匹配
3. 支持路由优先级 `order`（升序，首个命中）
4. 与 `gateway-core` 在 ROUTE 阶段集成
5. 提供完整单元测试与集成测试

### 非目标

- 不实现真实网络转发（`gateway-forward` 后续阶段）
- 不实现服务发现/LB（`gateway-cluster` 后续阶段）
- 不实现动态配置中心加载（先用内存路由）
- 不实现复杂 Path 模板表达式（如 `/a/{id}`）

## 架构对齐

与 `docs/arhctechture.md` 对齐：

- `gateway-routing` 负责“路由决策”
- `gateway-core` 负责“生命周期编排”
- `gateway-filter` 负责“流程拦截模型”

职责边界：

- `gateway-routing` 不依赖 `gateway-core`
- `gateway-core` 通过 ROUTE Phase Filter 适配调用 `RouteLocator`
- 不在 routing 模块中实现转发逻辑

## 依赖关系

```text
gateway-common  -> 无依赖
gateway-spi     -> gateway-common
gateway-filter  -> gateway-common + gateway-spi
gateway-routing -> gateway-common
gateway-core    -> gateway-filter + gateway-spi + gateway-routing
```

## 模块结构

```text
gateway-routing/
├── pom.xml
└── src/main/java/io/homeey/traffic/routing/
    ├── model/
    │   ├── RouteDefinition.java
    │   └── PredicateDefinition.java
    ├── request/
    │   └── RouteRequest.java
    ├── matcher/
    │   ├── PathMatcher.java
    │   └── MethodMatcher.java
    ├── locator/
    │   ├── RouteLocator.java
    │   └── InMemoryRouteLocator.java
    └── context/
        └── RoutingAttributes.java
```

`gateway-core` 新增适配类：

```text
gateway-core/src/main/java/io/homeey/traffic/core/engine/route/
└── RoutePhaseFilter.java
```

## 数据模型

### RouteDefinition

建议使用 record：

```java
public record RouteDefinition(
        String id,
        String target,
        int order,
        List<PredicateDefinition> predicates
) {}
```

- `id`: 路由唯一标识
- `target`: 目标服务标识或目标 URI（MVP 仅作为数据，不执行转发）
- `order`: 优先级，越小越先匹配
- `predicates`: 断言集合，全部命中才算路由命中

### PredicateDefinition

```java
public record PredicateDefinition(
        String name,
        Map<String, String> args
) {}
```

MVP 支持两种 `name`：

- `Path`：`args["value"]`，精确匹配
- `Method`：`args["value"]`，忽略大小写匹配

## 请求模型

为了避免 `gateway-routing` 反向依赖 `gateway-core`，定义独立请求对象：

```java
public record RouteRequest(
        String path,
        String method
) {}
```

## 匹配器设计

### PathMatcher

```java
boolean matches(String actualPath, String expectedPath)
```

MVP 规则：

- 两者都非空
- 精确字符串匹配

### MethodMatcher

```java
boolean matches(String actualMethod, String expectedMethod)
```

MVP 规则：

- 两者都非空
- 忽略大小写比较

## RouteLocator 设计

### RouteLocator

```java
public interface RouteLocator {
    Optional<RouteDefinition> locate(RouteRequest request);
}
```

### InMemoryRouteLocator

- 构造时接收 `List<RouteDefinition>`
- 初始化阶段执行校验：
  - `id` 非空
  - `target` 非空
  - `predicates` 非空
- 按 `order` 升序内部排序
- 执行匹配时遍历路由，返回首个命中的 `RouteDefinition`

匹配语义：

- 路由命中 = 该路由所有 predicates 命中（AND）
- 未命中返回 `Optional.empty()`

## 与 core 集成（ROUTE Phase）

在 `gateway-core` 新增 `RoutePhaseFilter`（适配层）：

职责：

1. 从 `GatewayContext` 属性读取请求信息
   - `request.path`
   - `request.method`
2. 构造 `RouteRequest`
3. 调用 `RouteLocator#locate`
4. 命中时写入路由结果属性并继续链
5. 未命中时 `context.terminate(404, "Route not found")` 并短路

路由结果属性键统一定义在 `RoutingAttributes`：

- `ROUTE_MATCHED`
- `ROUTE_ID`
- `ROUTE_TARGET`

## 执行流

### 命中

```text
PRE_ROUTE -> ROUTE(RoutePhaseFilter 命中) -> PRE_FORWARD -> FORWARD -> POST_FORWARD -> RESPONSE
```

### 未命中

```text
PRE_ROUTE -> ROUTE(RoutePhaseFilter 未命中, terminate 404) -> RESPONSE
```

## 错误处理

- 路由配置非法：`InMemoryRouteLocator` 初始化阶段抛 `IllegalArgumentException`
- 匹配输入非法（path/method 缺失）：视为未命中（返回 empty）
- ROUTE 过滤器不吞异常，交由现有 `ExecutionChain` 统一处理（进入 RESPONSE）

## 测试计划

### gateway-routing

1. `PathMatcherTest`
   - 精确匹配成功
   - 不同路径失败
   - 空值失败
2. `MethodMatcherTest`
   - 大小写不敏感匹配成功
   - 不同 method 失败
3. `InMemoryRouteLocatorTest`
   - 单路由命中
   - 多路由按 order 首命中
   - 未命中返回 empty
   - 非法 RouteDefinition 抛异常

### gateway-core

1. `RoutePhaseIntegrationTest`
   - 命中：写入 `ROUTE_ID/ROUTE_TARGET`，继续后续 phase
   - 未命中：terminate(404) 并进入 RESPONSE

## 实施顺序（MVP）

1. 新建 `gateway-routing` 模块与 pom
2. 完成路由模型与 matcher
3. 完成 `RouteLocator` + `InMemoryRouteLocator`
4. 完成 core 的 `RoutePhaseFilter` 适配
5. 添加 ROUTE 集成测试与全量回归

## 风险与缓解

1. **风险：Context 属性键散落导致不一致**
   - 缓解：统一由 `RoutingAttributes` 常量管理
2. **风险：后续 forward 阶段字段不兼容**
   - 缓解：`target` 先保留为字符串语义，后续通过适配层演进，不直接改 core 执行模型
3. **风险：Path 匹配过于简单**
   - 缓解：MVP 明确仅精确匹配，后续阶段扩展前缀/模板匹配

## 完成判定

满足以下条件即视为 Routing MVP 完成：

1. `gateway-routing` 模块可独立编译并通过测试
2. ROUTE Phase 可根据 path+method 决策路由
3. 命中时能写入路由结果到 `GatewayContext`
4. 未命中时 terminate 404 并进入 RESPONSE
5. 全项目 `mvn test` 通过
