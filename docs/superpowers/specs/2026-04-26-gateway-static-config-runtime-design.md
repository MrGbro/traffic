# 静态资源访问 + 配置驱动启动（Phase6 增强）

## 范围
在 `gateway-transport + gateway-runtime` MVP 基础上增强两项能力：

1. 支持静态资源访问（便于联调与测试）
2. 提供配置驱动的启动类（可直接启动网关进程）

## 目标

- `JdkHttpTransportServer` 支持静态文件目录映射
- 新增 `GatewayRuntimeConfigLoader`，从 `.properties` 加载配置
- 新增 `GatewayApplication` 启动类，按配置装配并启动网关

## 配置格式（properties）

```properties
gateway.server.port=8080

gateway.static.enabled=true
gateway.static.uriPrefix=/static/
gateway.static.rootDir=./public

gateway.routes=orders

gateway.route.orders.target=svc://orders
gateway.route.orders.order=10
gateway.route.orders.path=/orders
gateway.route.orders.method=GET

gateway.services=orders
gateway.service.orders.instances=127.0.0.1:9001,127.0.0.1:9002
```

## 运行行为

- 请求路径命中 `gateway.static.uriPrefix` 时，优先由 transport 返回静态文件
- 其他请求走 `GatewayBootstrap` 生命周期链

## 安全约束

- 静态文件路径防目录穿越（`normalize + startsWith(root)`）
- 文件不存在返回 404

## 完成判定

1. 可通过配置文件启动网关
2. 可访问静态资源文件
3. 网关动态请求链路行为不回归
