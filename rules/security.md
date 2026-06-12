# 安全规范

> 适用于所有 kww-framework + Spring Boot + MyBatis Plus 后端项目。
> 项目特定实现（鉴权 filter、数据权限 service、审计字段填充）由各项目自己的 rule 文件覆盖。

## 强制安全检查（提交前自检）

- [ ] 无硬编码密钥（appId / appSecret / privateKey / aesKey / token）
- [ ] 无 SQL 字符串拼接 / `${...}` 字面量
- [ ] 所有 controller 入口有必填参数校验
- [ ] 日志中没有打印 `password` / `appSecret` / `privateKey` 完整内容
- [ ] CORS 没有配置 `Access-Control-Allow-Origin: *`（按白名单返回）
- [ ] Feign 客户端对密钥交换类接口未启用 FULL 日志级别
- [ ] 错误响应不暴露栈帧
- [ ] 新增依赖走内网仓库 + license 检查

## 1. 密钥管理

### 规则 1：密钥从配置中心读取，绝不硬编码

所有第三方凭证（appId / appSecret / privateKey / token / aesKey）必须从 Nacos / Apollo / Vault 等配置中心下发，**禁止**写在 `application.yml` 或 Java 源码中。

```java
// ✅ 正确：@Value 注入
@Value("${third-party.app-secret}")
private String appSecret;

// ❌ 错误：硬编码（即使在 yml 里也错）
private String appSecret = "67e8240c68264c6c2dee840ff59df46a";
```

```yaml
# ❌ 错误：业务密钥与代码同库
third-party:
  web:
    app-secret: 67e8240c68264c6c2dee840ff59df46a
```

### 规则 2：密钥使用边界

- 第三方 SDK 调用密钥只在对应的 `XxxClient` / `XxxServiceImpl` 内部使用，**不外传到 controller、不入日志**
- 私钥、token 等敏感值**进 Redis 带过期时间**，不塞进内存 Map
- 加密工具（如 ``XxxCryptUtil``）封装加解密细节，业务代码不直接 `new SecretKeySpec(...)`

### 规则 3：密钥泄露后的轮换

发现密钥可能泄露（提交到 git、日志泄露、第三方平台事故）：

1. 立即在配置中心轮换
2. 通知所有调用方刷配置
3. 全仓库 `git log -p | grep` 排查是否曾误提交
4. 旧密钥立即作废

## 2. SQL 注入防御

### 规则 4：只使用 ORM Wrapper / Mapper 接口

**禁止**任何形式的 SQL 字符串拼接或 `${}` 占位符。

```java
// ✅ 正确：参数化（MyBatis Plus Wrapper / JPA Specification / Room Query）
wrapper.eq("code", code).like("name", name);

// ❌ 错误：字符串拼接
String sql = "SELECT * FROM xxx WHERE code = '" + code + "'";
```

XML mapper 中所有参数必须用 `#{param}` 占位符。`${param}` 仅用于排序字段名、表名等**无法参数化**的场景，传入前**必须白名单校验**。

```java
// ✅ 正确：白名单校验排序字段
private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("create_time", "update_time", "id");

public void list(String sortField, String order) {
    if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
        throw new IllegalArgumentException("invalid sort field");
    }
    // 用 ${sortField} 安全
}
```

## 3. 鉴权与数据权限

### 规则 5：用户上下文从统一入口获取

每个框架/项目都应有一个**唯一的鉴权 filter**，解析 token、设置用户上下文。**禁止**在 controller 里自行解析 token 或重新从 header 读用户信息。

```java
// ✅ 正确：从已注入的用户上下文取
String operator = request.getUserInfo().getUserNo();

// ❌ 错误：自行解析
String token = request.getHeader("X-Token");
String operator = jwtUtil.parse(token).getSubject();
```

### 规则 6：数据权限走 service 层

权限判断**不写在 SQL 拼接里**，不写在 if/else 角色判断里。统一通过 `XxxPermitService`（或框架提供的数据权限机制）注入。

```java
// ✅ 正确：分页查询时通过 service 注入权限
if (userInfo.getDataPerm().enablePermCtl()) {
    dataPermUtil.setPermIds(reqVO, "permDeptIds", userInfo, permType);
}
return xxxService.queryPage(reqVO);

// ❌ 错误：私自写角色判断
if ("admin".equals(currentUser.getRole())) {
    return xxxMapper.selectAll();
}
```

### 规则 7：CORS 按白名单返回

**禁止**配置 `Access-Control-Allow-Origin: *`（除非明确是公开 API）。

```java
// ✅ 正确：白名单
private static final List<String> ALLOWED_ORIGINS = List.of(
    "https://app.example.com",
    "https://admin.example.com"
);

String origin = request.getHeader("Origin");
if (ALLOWED_ORIGINS.contains(origin)) {
    response.setHeader("Access-Control-Allow-Origin", origin);
    response.setHeader("Vary", "Origin");
}

// ❌ 错误：通配
response.setHeader("Access-Control-Allow-Origin", "*");
```

带 cookie 的请求还需要 `Access-Control-Allow-Credentials: true` + 不能用 `*`。

## 4. 输入校验

### 规则 8：controller 入口校验必填项

必填参数在 controller 入口校验，**不依赖 service 层抛异常**。

```java
if (reqVO == null || reqVO.getKeyField() == null) {
    log.warn("[methodName] 参数校验失败 traceId={}, reason=keyField 不能为空", traceId);
    KwwResponse response = KwwResponse.newInstance().fail(traceId, "keyField 不能为空");
    return new ResponseEntity<>(response, HttpStatus.OK);
}
```

### 规则 9：富文本与文件上传

- 富文本：用 `HTMLFilter` / `JSoup` 做 XSS 过滤后再入库
- 文件上传：限制大小（`spring.servlet.multipart.max-file-size`），校验扩展名白名单
- 上传文件**不保存到 webroot**，统一走 OSS / 对象存储

## 5. 审计日志

### 规则 10：所有数据修改记录操作人

通过 ORM 字段填充（MyBatis Plus MetaObjectHandler / JPA Auditing / Hibernate Envers）自动写入 `create_user` / `update_user` / `create_time` / `update_time`。**禁止**业务代码里手写这些字段。

### 规则 11：敏感数据导出走审计通道

员工花名册、薪资、绩效、考勤明细等敏感数据导出，统一调用 `XxxDataLogService`（或框架的审计模块）记录操作人、时间、条件、数据量。

## 6. 日志安全

### 规则 12：禁止打印密钥 / 全量请求体

```java
// ❌ 错误
log.info("请求参数：{}", requestBody);  // body 里可能含 appSecret、身份证
log.info("Feign 响应：{}", response);   // Authorization 头等敏感

// ✅ 正确：脱敏 + 关键参数
log.info("[methodName] 请求进入 traceId={}, keyParam={}", traceId, mask(keyParam));
```

特别注意：
- 鉴权 filter **不打印全量请求体**，只记 `URI + traceId + 用户标识`
- Feign `Logger.Level.FULL` **不用于密钥交换类接口**（OAuth、token 换取、签名生成等）

### 规则 13：错误消息不泄露栈帧

```java
// ✅ 正确：响应体只给原因摘要
KwwResponse response = KwwResponse.newInstance().fail(traceId, "操作失败");
return new ResponseEntity<>(response, HttpStatus.OK);

// ❌ 错误：把堆栈塞进响应
response.fail(traceId, "操作失败：" + ExceptionUtils.getStackTrace(e));
```

栈帧可写日志（内部排查用），但不进 `KwwResponse.data` / `reason`。

## 7. 依赖与第三方

### 规则 14：新增依赖前确认

- 必须从内网 Maven 仓库（Nexus / 阿里云镜像）拉取
- 检查 license 冲突（不允许 GPL 等强传染协议进入闭源项目）
- 不引入"看起来很好但团队无人维护"的库（GitHub stars < 100、半年无 commit）
- 升级父 POM / BOM / Spring Boot 走团队评审（影响所有下游服务）

## 安全响应协议

如果发现安全问题（密钥泄露、SQL 注入、越权访问、XSS）：

1. **立即停止**当前操作，保留现场
2. 评估影响范围（哪些数据 / 接口 / 用户受影响）
3. 轮换已泄露的密钥（通知配置中心 owner + 第三方平台）
4. 全仓库 `git grep` / `git log -p` 排查是否曾误提交
5. 修复后补回归测试 + 提 PR + 至少一名"非作者" reviewer
6. 出具事故复盘（时间线、根因、改进项）