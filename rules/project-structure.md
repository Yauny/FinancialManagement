# 项目结构规范

> 适用项目: Spring Boot 4 + MyBatis Plus + Nacos + RocketMQ 技术栈
> 不适用于: 多模块 Maven 工程（这里是单模块多限界上下文）

## 核心原则

**单 Maven 模块 + 多限界上下文**。不同业务域通过**包名**隔离，共享同一套部署单元。**禁止**为业务子域拆 Maven 子模块。

```
<group>.<context>/
├── XxxServerApplication.java    # 模块入口（项目根包，scanBasePackages = "<group>"）
├── controller/                    # REST 入口
│   ├── XxxController.java
│   ├── filter/                    # Servlet Filter（如鉴权、灰度）
│   └── vo/<context>/              # VO 按业务域分包（详见 api-design.md）
├── service/                       # 业务逻辑
│   ├── XxxService.java
│   ├── XxxServiceImpl.java
│   ├── bo/                        # 跨服务边界返回的内部 DTO
│   ├── process/                   # MQ/事件处理器（实现 Processer 接口）
│   ├── producer/                  # MQ 生产者
│   ├── consumer/                  # MQ 消费者
│   ├── event/                     # 事件定义
│   ├── validator/                 # 自定义校验器
│   └── flowstrategy/              # 流程策略
├── repository/                    # 数据访问（MyBatis Plus）
│   ├── entity/                    # 表实体（XxxDO）
│   ├── mapper/                    # MyBatis Plus mapper
│   ├── service/                   # I*Service + I*ServiceImpl 封装
│   └── handler/                   # 字段填充等 Handler
├── feign/                         # 远程服务客户端
│   ├── XxxClient.java             # 接口
│   ├── service/                   # XxxClientImpl 实现
│   ├── bo/                        # Feign 返回的内部 DTO
│   └── model/                     # Feign 调用的 Rst/Req 模型
├── common/                        # 项目级公共代码
│   ├── Constants.java             # 业务枚举常量
│   ├── config/                    # Spring @Configuration 类
│   └── util/                      # 工具类
└── task/                          # @Scheduled 定时任务
```

## 具体规则

### 规则 1：多业务域作为同级包

不同业务域放在 `<group>.<context>/` 同一层，而不是 `<group>.<root>.<context>/`：

```text
✅ 正确
<group>.employee/
<group>.contextA/
<group>.contextB/
<group>.contextC/

❌ 错误（人为制造层级）
<group>.business.employee/
<group>.business.contextA/
```

入口类用 `scanBasePackages = "<group>"` 一次性扫描所有同级包。

### 规则 2：每层一个顶级包，禁止跨层

- `controller/` 不允许出现 `service`、`repository`、`feign` 关键字（除了 `@Resource` 注入）
- `service/` 不允许出现 `controller` 关键字
- `repository/` 不允许出现 `controller` 关键字

跨层调用通过 `@Resource` 注入下一层的 service，**禁止**直接 `new` 下一层对象或调用下下层。

```java
// ✅ 正确：controller → service
@Resource
private XxxService xxxService;

// ❌ 错误：controller 直接调 repository
@Resource
private XxxMapper xxxMapper;

// ❌ 错误：controller 直接调 feign
@Resource
private XxxClient xxxClient;  // 应封装在 service 层
```

### 规则 3：Service 接口/实现命名

业务 service 走 `XxxService` + `XxxServiceImpl`（**无 `I` 前缀**），仅限"可被同包其它 service 实现/扩展"的横切关注点才用 `I*` 前缀：

```java
// ✅ 业务 service（无 I 前缀）
public class XxxService {}
public class XxxServiceImpl implements XxxService {}

// ✅ 横切关注点（带 I 前缀，可被多实现）
public interface ICrossContextService {}
public class CrossContextServiceImpl implements ICrossContextService {}
```

跨上下文共享的 service 优先用 `I*` 前缀（如 `ICrossContextService`），便于识别"这是一个被多个 service 复用的接口"。

### 规则 4：Repository 层统一用 I*Service 前缀

MyBatis Plus 风格，**所有 repository service 接口必须带 `I` 前缀**，与业务 service 区分：

```java
// ✅ 正确
public interface IAlertInfoService extends IService<AlertInfoDO> {}
public class IAlertInfoServiceImpl extends ServiceImpl<AlertInfoMapper, AlertInfoDO> implements IAlertInfoService {}

// ❌ 错误
public interface AlertInfoService {}
public interface AlertInfoServiceImpl {}
```

这样读代码时看到 `IXxxService` 一眼就知道是数据访问层，`XxxService` 是业务层。

### 规则 5：BO/VO/DO 三层对象各司其职

| 后缀 | 所在层 | 用途 |
|---|---|---|
| `XxxDO` | `repository/entity/` | 表实体，与数据库字段一一对应 |
| `XxxBO` | `service/bo/` | 跨 service/feign 边界返回的内部 DTO（不在 controller 暴露） |
| `XxxReqVO` / `XxxRespVO` / `XxxPageVO` / `XxxSaveVO` | `controller/vo/<context>/` | HTTP 入参/出参，详见 api-design.md |

**禁止**让 controller 直接返回 `XxxDO`（暴露数据库结构）或 `XxxBO`（泄露内部逻辑）。

### 规则 6：Feign 客户端成对出现

每个远程服务调用必须同时定义接口和实现，包路径 `feign/<domain>/`：

```text
feign/
├── HireServiceClient.java          # 接口
├── service/HireServiceClientImpl.java  # 实现
├── bo/                             # 自定义内部 DTO
└── model/                          # 第三方 SDK 模型（命名 XxxRst / XxxReq）
```

实现放在 `feign/service/` 子包，BO 放在 `feign/bo/`，与 controller 的 BO 隔离。

### 规则 7：定时任务统一放 task/

所有 `@Scheduled` 任务集中到 `task/XxxTasks.java`，不要散落在 service 里：

```java
// ✅ 正确：统一集中
public class ScheduledTasks {
    @Scheduled(cron = "0 0 0 1 1 ?")
    public void executeAnnualSettlement() { ... }
}

// ❌ 错误：散落在 service
public class XxxServiceImpl {
    @Scheduled(cron = "0 0 0 1 1 ?")
    public void run() { ... }
}
```

### 规则 8：常量和配置

- 业务枚举常量集中到 `common/Constants.java`（含 `Constants.Flow.State`、`Constants.TransType` 等嵌套类）
- Spring `@Configuration` 类放 `common/config/`，命名 `XxxConfig` 或 `XxxConfiguration`
- 项目级配置（`@ConfigurationProperties`）放 `common/config/XxxProperties.java`

### 规则 9：配置文件按 profile 拆分

```text
src/main/resources/
├── application.yml              # 公共基础配置 + spring.profiles.active 提示
├── application-dev.yml          # 开发环境
├── application-uat.yml          # UAT
├── application-tj.yml          
└── ...
```

- `application.yml` 不承载业务配置（业务配置走 Nacos）
- profile 命名以**基地代号**为准（dev / uat / 基地缩写）