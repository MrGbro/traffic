# Archtechture
## 1. 生命周期驱动（比 Filter 更高一层）

网关不是简单 Filter 链，而是：
```text

REQUEST IN
  ↓
[PRE_ROUTE]
  ↓
[ROUTE]
  ↓
[PRE_FORWARD]
  ↓
[FORWARD]
  ↓
[POST_FORWARD]
  ↓
[RESPONSE]
```
SPI 必须“挂在生命周期阶段”，而不是简单链
## 2.双模型扩展体系

需要两类 SPI：

| 类型 | 作用 |   
| -- | -- |
| 能力型 SPI |	限流、鉴权、负载均衡 |
| 流程型 SPI（Filter） |	生命周期拦截 |

## 3. 最终扩展体系结构
```text
                ┌──────────────┐
                │  Gateway Core│
                └──────┬───────┘
                       │
        ┌──────────────┼──────────────┐
        │                              │
   Filter体系                     SPI体系
 (流程控制)                   (能力/集成扩展)
        │                              │
  鉴权/日志/...          配置中心/注册中心/限流...
  ```
## 4.项目骨架设计
### 顶层结构
```text
gateway/
|-- gateway-admin                    # 控制面服务
├── gateway-bom                      # 依赖版本统一（必须有）
├── gateway-dependencies             # 第三方依赖收敛

├── gateway-core                     # ★微内核（最核心，绝对稳定）
├── gateway-spi                      # ★所有SPI定义（唯一出口）
├── gateway-common                   # 通用模型/工具（无业务）

├── gateway-filter                   # Filter体系（生命周期执行模型）
├── gateway-plugin                   # 官方插件集合（组合SPI+Filter）

├── gateway-routing                  # 路由子系统
├── gateway-cluster                  # 服务发现 + 注册 + LB
├── gateway-forward                  # 请求转发（协议层）

├── gateway-config                   # 配置中心抽象 + 实现
├── gateway-registry                 # 注册中心抽象 + 实现

├── gateway-observability            # 日志/指标/追踪
├── gateway-security                 # 安全体系（认证/鉴权）

├── gateway-traffic                  # 流量治理（限流/熔断/重试/灰度）

├── gateway-transport                # 网络层抽象
│   ├── gateway-transport-netty
│   ├── gateway-transport-reactor
│   └── gateway-transport-vertx

├── gateway-runtime                  # 启动器（类似Spring Boot）
├── gateway-control-plane            # 控制面（管理API）

└── gateway-example                  # 示例 & Demo
```
### 核心职责拆解
#### gateway-core
微内核：调度不做业务
```text
gateway-core/
├── engine/
│   ├── GatewayEngine               # 请求入口
│   ├── ExecutionChain             # 执行链
│   └── LifecycleManager           # 生命周期调度
│
├── lifecycle/
│   ├── Phase                      # PRE_ROUTE / POST...
│   └── PhaseRegistry
│
└── context/
    ├── GatewayContext
    ├── RequestContext
    └── ResponseContext
```
#### gateway-spi
所有spi必须在这里定义，不散落;扩展点唯一出口
```text
gateway-spi/
├── extension/                     # SPI基础设施（类Dubbo）
│   ├── ExtensionLoader
│   ├── ExtensionFactory
│   ├── @SPI
│   ├── @Adaptive
│   └── @Activate
│
├── contract/                      # 能力型SPI
│   ├── traffic/
│   │   ├── RateLimiter
│   │   ├── CircuitBreaker
│   │   ├── RetryPolicy
│   │   └── TrafficShaper
│   │
│   ├── cluster/
│   │   ├── LoadBalancer
│   │   ├── ServiceDiscovery
│   │   └── ServiceRegistry
│   │
│   ├── config/
│   │   ├── ConfigCenter
│   │   └── ConfigParser
│   │
│   ├── security/
│   │   ├── Authenticator
│   │   └── Authorizer
│   │
│   ├── forward/
│   │   └── Forwarder
│   │
│   ├── observability/
│   │   ├── MetricsCollector
│   │   ├── Tracer
│   │   └── AccessLogger
│   │
│   └── infra/
│   |   ├── Cache
│   |   ├── Serializer
│   |   └── PluginLoader
|   |── transport/
│       ├── TransportServer
│
└── context/                       # SPI共享上下文接口
    ├── RequestContext
    └── ResponseContext

```
####  gateway-filter
流程扩展体系：生命周期执行模型
```text
gateway-filter/
├── core/
│   ├── GatewayFilter
│   ├── FilterChain
│   ├── DefaultFilterChain
│   └── FilterInvoker
│
├── support/
│   ├── AbstractFilter
│   └── OrderedFilter
│
└── phase/
    ├── PreRouteFilter
    ├── RouteFilter
    ├── PreForwardFilter
    ├── PostForwardFilter
    └── ResponseFilter
```
#### gateway-plugin
Filter+SPI
```text
gateway-plugin/
├── gateway-plugin-auth
├── gateway-plugin-ratelimit
├── gateway-plugin-circuitbreaker
├── gateway-plugin-retry
├── gateway-plugin-logging
├── gateway-plugin-metrics
├── gateway-plugin-tracing
└── gateway-plugin-gray
```
每个插件内部实现如：
```text
gateway-plugin-ratelimit/
├── limiter/                      # SPI实现
│   ├── TokenBucketLimiter
│   └── RedisRateLimiter
│
├── filter/
│   └── RateLimitFilter           # 调用SPI
│
└── META-INF/gateway/             # SPI注册
```
#### gateway-routing（路由系统）
```text
gateway-routing/
├── model/
│   ├── RouteDefinition
│   └── PredicateDefinition
│
├── matcher/
│   ├── PathMatcher
│   ├── HeaderMatcher
│   └── MethodMatcher
│
└── locator/
    └── RouteLocator
```
#### gateway-cluster（服务集群）
```text
gateway-cluster/
├── discovery/
│   └── DefaultServiceDiscovery
│
├── registry/
│   └── DefaultServiceRegistry
│
└── loadbalance/
    ├── RoundRobinLoadBalancer
    └── ConsistentHashLoadBalancer
```
#### gateway-forward
```text
gateway-forward/
├── http/
│   └── HttpForwarder
│
├── grpc/
│   └── GrpcForwarder
│
└── websocket/
    └── WsForwarder
```
#### gateway-traffic（流量治理）

和 plugin 区别：
plugin：入口控制
traffic：能力实现
```text
gateway-traffic/
├── ratelimit/
├── circuitbreaker/
├── retry/
└── gray/
```
#### gateway-runtime
```text
gateway-runtime/
├── bootstrap/
│   └── GatewayBootstrap
│
├── autoconfigure/
└── starter/
```
#### gateway-control-plane
```text
gateway-control-plane/
├── api/
├── service/
└── dashboard/
```

## 5. 依赖关系
```text
           +-------------------+
           |   control-plane   |
           +---------+---------+
                     |
                gateway-config
                     |
+---------+----------+-----------+
|     gateway-core (内核)        |
+---------+----------+-----------+
          |          |
   gateway-filter   gateway-spi
          |
     gateway-plugin
          |
+---------+----------------------+
| routing / cluster / forward   |
+--------------------------------
```