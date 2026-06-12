# API 设计规范 — Controller 层

> 适用文件: `src/main/java/**/controller/**/*.java`
> 适用于所有使用 kww-framework 的 Spring Boot 项目。

## 核心原则

**全 POST + KwwRequest 入参 + KwwResponse 出参**（强制性）。不暴露 GET 接口，所有端点统一走 POST + JSON body。

```java
// ✅ 标准写法
@Slf4j
@RestController
@RequestMapping("/business/domain")
public class XxxController {

    @Resource
    private XxxService xxxService;

    @PostMapping("/query")
    public ResponseEntity<KwwResponse> query(@RequestBody KwwRequest request) {
        String traceId = request.getTraceId();
        log.info("[query] 请求进入 traceId={}", traceId);

        XxxReqVO reqVO = BeanUtils.toBean(request.getData(), XxxReqVO.class);
        if (reqVO == null || reqVO.getKeyField() == null) {
            log.warn("[query] 参数校验失败 traceId={}, reason=keyField 不能为空", traceId);
            KwwResponse response = KwwResponse.newInstance().fail(traceId, "keyField 不能为空");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        long startTime = System.currentTimeMillis();
        List<XxxBO> result = xxxService.query(reqVO.getKeyField());
        long cost = System.currentTimeMillis() - startTime;

        log.info("[query] 查询完成 traceId={}, keyField={}, resultCount={}, cost={}ms",
            traceId, reqVO.getKeyField(), result != null ? result.size() : 0, cost);

        KwwResponse response = KwwResponse.newInstance().success(traceId, JSONUtil.toJsonStr(result));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
```

## 具体规则

### 规则 1：traceId 日志格式

每条日志必须带 `traceId` 和 `[methodName]` 前缀，贯穿 4 个关键节点：

```java
String traceId = request.getTraceId();

// 1. 请求进入
log.info("[methodName] 请求进入 traceId={}", traceId);

// 2. 参数校验失败
log.warn("[methodName] 参数校验失败 traceId={}, reason=具体原因", traceId);

// 3. 执行业务（带关键参数）
log.info("[methodName] 开始执行 traceId={}, param={}", traceId, param);

// 4. 完成（含耗时 + 结果摘要）
log.info("[methodName] 执行完成 traceId={}, param={}, cost={}ms, resultCount={}",
    traceId, param, cost, resultCount);
```

```java
// ❌ 错误：缺少 [methodName] 前缀
log.info("操作成功, traceId: {}", request.getTraceId());

// ❌ 错误：连 traceId 都没有
log.error("操作异常！", e);
```

### 规则 2：KwwResponse 成功/失败范式

HTTP 状态码始终返回 200，业务状态通过 `KwwResponse` 承载。

```java
// ✅ 成功（有数据）
KwwResponse response = KwwResponse.newInstance().success(traceId, JSONUtil.toJsonStr(result));
return new ResponseEntity<>(response, HttpStatus.OK);

// ✅ 成功（无数据）
return new ResponseEntity<>(KwwResponse.newInstance().success(traceId), HttpStatus.OK);

// ✅ 失败（参数校验/业务错误）
KwwResponse response = KwwResponse.newInstance().fail(traceId, "错误原因");
return new ResponseEntity<>(response, HttpStatus.OK);

// ✅ 失败（异常）
response.fail(new CommonException(ErrorCode.BUSINESS_COMMON_EX, e.getMessage()));
return new ResponseEntity<>(response, HttpStatus.OK);
```

### 规则 3：请求体解析

从 `KwwRequest` 提取业务 VO：

```java
// 首选：BeanUtils.toBean（类型安全，有编译期检查）
XxxReqVO reqVO = BeanUtils.toBean(request.getData(), XxxReqVO.class);

// 备选：getDataObject（JSONObject，用于动态字段/条件分支场景）
JSONObject dataObject = request.getDataObject(JSONObject.class);

// BaseVo：只含 id 字段，用于按 ID 删除/查询单条等简单操作
BaseVo reqVO = BeanUtils.toBean(request.getData(), BaseVo.class);
```

### 规则 4：VO 命名与分包

| 后缀 | 用途 | 说明 |
|---|---|---|
| `*ReqVO` | 请求参数 | 查询/校验等只读操作的入参 |
| `*RespVO` | 响应数据 | 返回给前端的数据对象 |
| `*PageVO` | 分页查询 | 必须 `extends PageVO`（来自框架） |
| `*SaveVO` | 创建/更新 | 含完整字段，与数据库实体一一对应 |
| `*ExportVO` | 导出文件 | 文件下载相关 |

- 按业务域分包：`controller/vo/<context>/`（不要全部堆在 `vo/` 根目录）
- 字段不加业务前缀（`reqUserId` → 直接用 `userId`，VO 类名已经表达了身份）
- 不允许使用上述后缀之外的裸名

### 规则 5：参数校验

必填参数在 controller 入口处校验，不在 service 层才报错：

```java
if (reqVO == null || reqVO.getKeyField() == null) {
    log.warn("[methodName] 参数校验失败 traceId={}, reason=keyField 不能为空", traceId);
    KwwResponse response = KwwResponse.newInstance().fail(traceId, "keyField 不能为空");
    return new ResponseEntity<>(response, HttpStatus.OK);
}
```

### 规则 6：Controller 职责边界

Controller 只做 4 件事：
1. 解析 `KwwRequest` → VO
2. 参数校验（必填项）
3. 调用一个 service 方法
4. 组装 `KwwResponse` 返回

**禁止**在 controller 里：写业务逻辑、直接调多个 service 串联流程、直接操作数据库/缓存。
