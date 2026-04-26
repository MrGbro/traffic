# gateway-forward MVP — 设计文档

## 范围
在已完成 `gateway-core + gateway-filter + gateway-routing` 的基础上，新增 `gateway-forward` 模块，实现最小可运行真实转发能力（Forward MVP）。

生命周期目标：

- 在 `FORWARD` Phase 执行真实 HTTP 转发
- 转发成功后将响应写回 `GatewayContext`
- 转发异常按网关语义映射为 502/504

## 目标与非目标

### 目标

1. 新建 `gateway-forward` 模块并实现 `HttpForwarder`
2. 在 `gateway-spi` 定义可执行 `Forwarder` 契约
3. 在 `gateway-core` 新增 `ForwardPhaseFilter` 完成 FORWARD 阶段适配
4. 打通 `ROUTE -> FORWARD -> RESPONSE` 的端到端执行流
5. 提供单测与集成测试

### 非目标

- 不实现服务发现与负载均衡（`gateway-cluster` 后续阶段）
- 不实现连接池/重试/熔断（`gateway-traffic` 后续阶段）
- 不实现动态路由配置热更新

## 架构对齐

对齐 `docs/archtechture.md`：

- `gateway-core`：生命周期编排与 Phase 适配
- `gateway-routing`：路由决策（输出 target）
- `gateway-forward`：执行真实转发
- `gateway-spi`：定义前向 SPI 契约与上下文模型

依赖关系：

```text
gateway-forward -> gateway-spi
gateway-core    -> gateway-spi + gateway-filter + gateway-routing
```

`gateway-core` 不依赖 `gateway-forward` 实现模块（测试可使用 test scope 依赖）。

## 模块结构

```text
gateway-forward/
├── pom.xml
└── src/main/java/io/homeey/traffic/forward/
    └── http/
        └── HttpForwarder.java

gateway-core/src/main/java/io/homeey/traffic/core/engine/forward/
└── ForwardPhaseFilter.java

gateway-spi/src/main/java/io/homeey/traffic/spi/
├── contract/forward/Forwarder.java
└── context/
    ├── SpiRequestContext.java
    ├── SpiResponseContext.java
    └── ExchangeAttributes.java
```

## SPI 契约与上下文

### Forwarder

```java
public interface Forwarder {
    SpiResponseContext forward(String target, SpiRequestContext request) throws Exception;
}
```

### SpiRequestContext（MVP）

字段：
- `requestId`
- `path`
- `method`
- `headers`
- `body`
- `attributes`（扩展字段）

### SpiResponseContext

字段：
- `statusCode`
- `headers`
- `body`

### ExchangeAttributes

统一 `GatewayContext` 键：
- `request.path`
- `request.method`
- `request.headers`
- `request.body`
- `response.status`
- `response.headers`
- `response.body`

## FORWARD Phase 设计

`ForwardPhaseFilter` 职责：

1. 从 `GatewayContext` 读取 `ROUTE_TARGET` 与请求属性
2. 构造 `SpiRequestContext`
3. 调用 `Forwarder#forward`
4. 成功时写入响应属性并继续链
5. 异常映射：
   - 连接失败 / IO 异常 -> `502 Bad Gateway`
   - 超时 -> `504 Gateway Timeout`

## HttpForwarder 设计（MVP）

- 基于 JDK `java.net.http.HttpClient`
- `target` 作为后端 base URL，`path` 按 URI resolve 规则拼接
- 请求头透传（MVP 不做 header 过滤）
- 支持 body 转发与字节流响应回传

## 执行流

### 成功

```text
PRE_ROUTE -> ROUTE(命中) -> PRE_FORWARD -> FORWARD(转发成功) -> POST_FORWARD -> RESPONSE
```

### 失败

```text
... -> FORWARD(超时/连接失败 terminate) -> RESPONSE
```

## 测试计划

### gateway-forward

1. `HttpForwarderTest`
   - 转发 GET 成功并返回状态/头/体
   - 转发 POST body 成功透传
   - 超时抛 `HttpTimeoutException`

### gateway-core

1. `ForwardPhaseFilterTest`
   - 成功写入 response 属性并继续链
   - target 缺失 terminate 502
   - timeout 映射 504
   - io/connect 异常映射 502

2. `ForwardPhaseIntegrationTest`
   - `ROUTE + FORWARD + RESPONSE` 端到端跑通（本地 mock backend）

## 风险与缓解

1. **风险：请求/响应键散落导致耦合**
   - 缓解：统一由 `ExchangeAttributes` 管理
2. **风险：target 拼接不一致**
   - 缓解：统一 `HttpForwarder` URI resolve 策略与测试覆盖
3. **风险：异常语义不稳定**
   - 缓解：`ForwardPhaseFilter` 显式异常分类与断言测试

## 完成判定

满足以下条件即视为 Forward MVP 完成：

1. `gateway-forward` 模块编译与测试通过
2. `Forwarder` SPI 可执行并返回响应上下文
3. `FORWARD` Phase 能完成真实转发并写入响应属性
4. 连接失败映射 502，超时映射 504
5. 全项目 `mvn test` 通过
