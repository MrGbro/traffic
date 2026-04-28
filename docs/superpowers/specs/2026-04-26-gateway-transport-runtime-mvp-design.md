# gateway-transport + gateway-runtime MVP — 设计文档

## 范围
在已完成 `core + routing + forward + cluster` 的基础上，实现真实入站网络层与启动器，打通“网关进程可启动并接收 HTTP 请求”。

## 目标

1. 完整化 `TransportServer` SPI 契约（start/stop）
2. 新建 `gateway-transport` 模块，实现 `JdkHttpTransportServer`
3. 新建 `gateway-runtime` 模块，实现 `GatewayBootstrap`
4. 将入站 HTTP 请求映射为 `GatewayContext` 属性并调用 `GatewayEngine`
5. 将 `GatewayContext` 响应属性映射回 HTTP 响应
6. 提供端到端测试

## 非目标

- 不引入 Netty/Reactor 等复杂 transport
- 不实现配置中心驱动的动态路由
- 不实现 TLS/认证/限流

## 架构对齐

- `gateway-transport`：只负责网络接入与协议映射
- `gateway-runtime`：只负责模块装配与生命周期管理
- `gateway-core`：继续负责阶段编排

## SPI 契约

### TransportRequestHandler

```java
@FunctionalInterface
public interface TransportRequestHandler {
    SpiResponseContext handle(SpiRequestContext request);
}
```

### TransportServer

```java
public interface TransportServer {
    void start(int port, TransportRequestHandler handler) throws Exception;
    void stop();
}
```

## gateway-transport 实现

### JdkHttpTransportServer

- 基于 `com.sun.net.httpserver.HttpServer`
- `start(port, handler)` 绑定端口并注册 `"/"` handler
- 读取 method/path/headers/body 构造 `SpiRequestContext`
- 调用 `TransportRequestHandler`
- 将 `SpiResponseContext` 回写为 HTTP 响应

## gateway-runtime 实现

### GatewayBootstrap

- 构造参数：`RouteLocator`, `ServiceDiscovery`, `LoadBalancer`, `Forwarder`, `TransportServer`
- 内部初始化 `GatewayEngine` 并注册：
  - ROUTE: `RoutePhaseFilter`
  - FORWARD: `ForwardPhaseFilter`
- 通过 `TransportServer.start` 绑定入站处理函数：
  - `SpiRequestContext -> GatewayContext`
  - 设置 `ExchangeAttributes.REQUEST_*`
  - 调用 `engine.execute`
  - 读取 `ExchangeAttributes.RESPONSE_*` / `context.statusCode()`

## 状态码语义

- 路由未命中：404
- 服务不可用：503
- 转发失败：502
- 转发超时：504
- 未处理异常：500
- 正常成功：使用 `RESPONSE_STATUS`（默认 200）

## 测试计划

1. `JdkHttpTransportServerTest`：验证请求/响应映射
2. `GatewayBootstrapIntegrationTest`：启动网关，真实发请求，验证
   - 静态 target 转发
   - svc:// + rr 转发
   - 无实例 503

## 完成判定

1. 网关可启动并监听端口
2. 真实 HTTP 请求可走通完整生命周期
3. `mvn test` 与 `mvn package` 通过
