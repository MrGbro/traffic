# gateway-cluster MVP — 设计文档

## 范围
在已完成 `routing + forward` 的基础上，实现 `gateway-cluster` MVP，支持逻辑服务目标（`svc://service-name`）解析为具体实例，并通过负载均衡选择目标地址。

## 目标

1. 新建 `gateway-cluster` 模块
2. 在 `gateway-spi` 完整定义 cluster 契约
3. 提供 `InMemoryServiceDiscovery` 与 `RoundRobinLoadBalancer`
4. 在 `gateway-core` 的 FORWARD 阶段增加目标解析流程
5. 新增无实例 `503 Service Unavailable` 语义
6. 覆盖单测与集成测试

## 非目标

- 不实现注册中心适配（nacos/eureka 等）
- 不实现动态实例健康检查
- 不实现权重路由与一致性哈希

## 架构对齐

- `gateway-routing`：仅输出路由 `target`（可为 `svc://`）
- `gateway-cluster`：服务发现 + 负载均衡
- `gateway-core`：阶段编排与解析适配
- `gateway-forward`：只处理“已解析目标”转发

## SPI 契约

### ServiceInstance

```java
public record ServiceInstance(
    String serviceName,
    String host,
    int port,
    Map<String, String> metadata
) {
    String toUrl(); // http://host:port
}
```

### ServiceDiscovery

```java
public interface ServiceDiscovery {
    List<ServiceInstance> getInstances(String serviceName);
}
```

### LoadBalancer

```java
public interface LoadBalancer {
    Optional<ServiceInstance> choose(String serviceName, List<ServiceInstance> instances);
}
```

## cluster 实现

### InMemoryServiceDiscovery

- 构造函数注入 `Map<String, List<ServiceInstance>>`
- `getInstances` 返回不可变拷贝

### RoundRobinLoadBalancer

- 每个 serviceName 独立计数器
- 线程安全（`ConcurrentHashMap + AtomicInteger`）
- instances 为空返回 `Optional.empty()`

## core 目标解析

在 `ForwardPhaseFilter` 内：

1. 读取 `ROUTE_TARGET`
2. 若为 `svc://service-name`：
   - discovery 查询实例
   - lb 选择实例
   - 解析为 `http://host:port`
3. 若无可用实例：`terminate(503, "Service Unavailable")`
4. 解析成功后写入：`route.resolvedTarget`
5. Forwarder 仅消费解析后的目标

新增 routing 属性：
- `ROUTE_RESOLVED_TARGET`

## 测试计划

### gateway-cluster

1. `InMemoryServiceDiscoveryTest`
2. `RoundRobinLoadBalancerTest`

### gateway-core

1. `ForwardPhaseFilterTest`
   - 静态 URL 仍可转发
   - `svc://` 可解析并写入 `ROUTE_RESOLVED_TARGET`
   - 无实例映射 503
2. `ForwardPhaseIntegrationTest`
   - 多实例 round-robin
   - 无实例仅进入 RESPONSE 且 503

## 完成判定

1. `gateway-cluster` 模块测试通过
2. `svc://` 目标可解析并成功转发
3. 无实例返回 503
4. 全项目 `mvn test` 与 `mvn package` 通过
