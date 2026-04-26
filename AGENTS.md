# traffic — Agent Instructions

## Project
Java 17 API gateway. GroupId `io.homeey.traffic`, artifact `traffic`.

- **Build**: `mvn compile` / `mvn test` / `mvn package` (Maven, no wrapper checked in)
- **No source yet** — planned multi-module structure, currently single-module `pom.xml`
- **Package root**: `io.homeey.traffic`

## Architecture (planned)
Lifecycle-driven gateway — not a flat filter chain:

```
REQUEST → PRE_ROUTE → ROUTE → PRE_FORWARD → FORWARD → POST_FORWARD → RESPONSE
```

Dual extension model:
- **SPI** (gateway-spi): capability extensions (rate limiter, load balancer, config center, auth)
- **Filter** (gateway-filter): lifecycle phase interceptors, ordered chain

Core module `gateway-core` is the microkernel — calls SPI + Filter, never vice versa. All SPI definitions live only in `gateway-spi`, not scattered across modules.

Full design doc: `docs/arhctechture.md` (sic).

## Conventions
- **Java 17** — use records, sealed classes, text blocks where appropriate
- Maven multi-module layout (all modules under one parent pom)
- SPI registration via `META-INF/gateway/` (not Java SPI `META-INF/services/`)
- No dependencies yet — add to `pom.xml` as needed

## Critical
使用中文做项目或者方案的沟通
