# Gateway 核心模块骨架 — 设计文档

## 范围
搭建 `gateway-common`、`gateway-spi`、`gateway-core` 三个 Maven 子模块的骨架。

## 深度
- 模块目录结构、pom.xml 配置
- 基础接口和抽象类定义（不实现业务逻辑）
- JUnit 5 测试依赖

## Maven 多模块结构

```
traffic/
├── pom.xml                       # 父 pom (io.homeey.traffic:traffic, packaging=pom)
├── gateway-common/
│   ├── pom.xml
│   └── src/main/java/io/homeey/traffic/common/
│       ├── enums/Phase.java
│       └── util/
├── gateway-spi/
│   ├── pom.xml
│   └── src/main/java/io/homeey/traffic/spi/
│       ├── extension/             # SPI 基础设施
│       │   ├── ExtensionLoader.java
│       │   ├── SPI.java           # @SPI 注解
│       │   └── Activate.java      # @Activate 注解
│       ├── contract/              # 能力型 SPI 接口
│       │   ├── traffic/RateLimiter.java
│       │   ├── traffic/CircuitBreaker.java
│       │   ├── cluster/LoadBalancer.java
│       │   ├── config/ConfigCenter.java
│       │   ├── security/Authenticator.java
│       │   ├── forward/Forwarder.java
│       │   ├── observability/MetricsCollector.java
│       │   └── transport/TransportServer.java
│       └── context/               # SPI 共享上下文
│           ├── SpiRequestContext.java
│           └── SpiResponseContext.java
├── gateway-core/
│   ├── pom.xml
│   └── src/main/java/io/homeey/traffic/core/
│       ├── engine/
│       │   ├── GatewayEngine.java
│       │   ├── ExecutionChain.java
│       │   └── LifecycleManager.java
│       ├── lifecycle/
│       │   └── PhaseRegistry.java
│       └── context/
│           └── GatewayContext.java
```

## 依赖关系

```
gateway-common  →  无依赖
gateway-spi     →  depends on gateway-common
gateway-core    →  depends on gateway-spi (传递依赖 common)
```

## 各模块详细设计

### gateway-common
- Phase 枚举：PRE_ROUTE, ROUTE, PRE_FORWARD, FORWARD, POST_FORWARD, RESPONSE

### gateway-spi
- `@SPI` 注解：标记可扩展接口，可选默认值
- `@Activate` 注解：标记实现类，支持 group 和 order
- ExtensionLoader：加载 META-INF/gateway/ 下的 SPI 配置
- SPI 契约接口均为标记型接口，不包含实现

### gateway-core
- GatewayEngine：网关入口，协调生命周期和 SPI
- ExecutionChain：执行链抽象
- LifecycleManager：生命周期阶段调度
- PhaseRegistry：Phase 注册与管理
- GatewayContext：贯穿整个请求的上下文

## 测试
- JUnit 5 + AssertJ
- 每个模块均有 src/test/java 目录
- 初始只包含 pom 结构验证的占位测试

## 技术约定
- Java 17：使用 records、sealed classes、text blocks
- SPI 注册路径：META-INF/gateway/
- 包根：io.homeey.traffic
- UTF-8 编码
