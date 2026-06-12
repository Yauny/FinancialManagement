# 个人财务管理系统 — 后端完整实现计划

## Context

项目已完成账户/交易/资产/导入四大业务模块的 CRUD 代码（PROGRESS.md 标记为"已完成"），但代码中存在两类问题：

1. **缺失模块**：`task/` 目录为空，定时任务完全未实现。PRD 明确要求"定期数据同步和更新"，而 PROGRESS.md 漏掉了这块。
2. **违反规范**：现有 service 层大量使用 `BeanUtils.copyProperties`，原地修改目标对象，违反 `rules/coding-style.md` 中"始终创建新对象，切勿修改现有对象"的不可变性原则。

本次实现目标：
- 补全 `task/` 模块：每日资产快照 + 资产/持仓缓存预热
- 重构所有 `BeanUtils.copyProperties` 为手动字段复制（不可变）
- 增强现有模块（资产趋势、批量导入、级联删除）
- 启动服务 + curl 端到端联调，验证所有 API

## 用户已确认的关键决策

| 决策点 | 选择 |
|---|---|
| 定时任务内容 | 每日资产快照 + 缓存预热 |
| 测试要求 | 只做端到端 API 联调（不写单元测试） |
| 不可变性 | 严格遵守 - 重构所有 BeanUtils 调用 |
| 数据库 | 保持 MySQL 3306（运行时）/ H2（测试） |

## 实施步骤

### 阶段 1：基础设施准备

**1.1 引入缓存依赖** — `backend/pom.xml`
- 新增 `spring-boot-starter-cache`
- 新增 `com.github.ben-manes.caffeine:caffeine`（本地内存缓存）

**1.2 启用调度与缓存** — `FinancialManagementApplication.java`
- 添加 `@EnableScheduling`
- 添加 `@EnableCaching`

**1.3 缓存配置** — `common/config/CacheConfig.java`（新建）
- 配置 Caffeine：资产总览 5 分钟过期、持仓明细 5 分钟过期

**1.4 新增资产快照表** — `db/init.sql` 追加
```sql
CREATE TABLE asset_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_date DATE NOT NULL UNIQUE,
    total_asset DECIMAL(18,2) NOT NULL,
    allocation_json TEXT COMMENT 'JSON: [{type,amount,ratio}]',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_snapshot_date (snapshot_date)
);
```

**1.5 新增资产快照实体/Mapper/Repository** — `asset/repository/`
- `entity/AssetSnapshotDO.java`
- `mapper/AssetSnapshotMapper.java`
- `service/IAssetSnapshotService.java` + `IAssetSnapshotServiceImpl.java`

### 阶段 2：定时任务模块

**2.1 资产快照任务** — `task/AssetSnapshotTask.java`（新建）
- 每日 02:00 执行（`@Scheduled(cron = "0 0 2 * * ?")`）
- 调用 `AssetService.getSummary()` 获取当前资产
- 写入 `asset_snapshot` 表
- 失败时记录日志，不中断

**2.2 缓存预热任务** — `task/CacheWarmupTask.java`（新建）
- 每日 02:30 执行（`@Scheduled(cron = "0 30 2 * * ?")`）
- 调用 `AssetService.getSummary()` 和 `getHoldings()`，触发 @Cacheable
- 同步执行，避免缓存击穿

**2.3 任务配置常量** — `common/Constants.java` 追加
- 嵌套类 `Constants.TaskCron`：定义所有 cron 表达式

### 阶段 3：资产模块重构 + 增强

**3.1 重构 AssetServiceImpl**（不可变性）
- 替换 `BeanUtils.copyProperties` → 手动 `setXxx()` 逐字段复制
- 在 `getSummary()`、`getHoldings()` 上加 `@Cacheable`

**3.2 新增资产趋势 API** — `asset/service/AssetService.java` 追加
- `List<AssetSnapshotRespVO> getSnapshots(Integer days)`：查询最近 N 天快照
- `AssetSnapshotRespVO`：`date / totalAsset / allocationList`

**3.3 Controller 暴露新接口** — `AssetController.java`
- `POST /api/assets/snapshots`：接收 `{days: 30}`

### 阶段 4：账户模块重构 + 增强

**4.1 重构 AccountServiceImpl**（不可变性）
- 替换 `BeanUtils.copyProperties` → 手动 setXxx
- `update()` 方法不再修改入参 `saveVO`，改为创建新 DO

**4.2 新增批量 API**
- `batchDelete(List<Long> ids)`：批量删除账户
- 删除账户时级联软删除其下交易（同一事务）

### 阶段 5：交易模块重构 + 增强

**5.1 重构 TransactionServiceImpl**（不可变性）
- 替换 `BeanUtils.copyProperties` → 手动 setXxx

**5.2 新增批量保存 API**
- `batchSave(List<TransactionSaveVO> saveVOs)`：导入的二次封装，校验后批量入库

**5.3 新增统计 API** — `TransactionService.java` 追加
- `TransactionStatisticsRespVO getStatistics(LocalDate start, LocalDate end)`
- 包含：总笔数、总金额、买入/卖出/分红/转账各类型小计

### 阶段 6：导入模块重构 + 增强

**6.1 重构 ImportServiceImpl**（不可变性）
- 替换 `BeanUtils.copyProperties` → 手动 setXxx

**6.2 解析器抽离** — `import_data/service/MarkdownTableParser.java`（新建）
- 纯函数式解析：输入 Markdown 字符串 → `List<ParsedRow>`
- 提升可测试性（即使本轮不写测试，结构上更清晰）

**6.3 解析格式增强**
- 兼容 "元" 后缀缺失（`11,543.93` 也能识别）
- 兼容千分位逗号
- 兼容百分比列
- 错误行记录到 `failReasons: List<String>` 返回给前端

### 阶段 7：端到端 API 联调

**7.1 启动服务**
```bash
cd /Users/yauny/Desktop/FinancialManagement/backend
mvn clean compile                                    # 编译验证
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # 启动
```
若 MySQL 不可用，临时用 H2 启动验证：
```bash
mvn spring-boot:run -Dspring.datasource.url='jdbc:h2:mem:fm' \
                   -Dspring.datasource.driver-class-name=org.h2.Driver \
                   -Dspring.datasource.username=sa \
                   -Dspring.datasource.password=
```

**7.2 curl 验证清单**（按 PRD 顺序）

| 接口 | curl 命令 |
|---|---|
| 启动后初始化数据库 | 执行 `db/init.sql` 到 MySQL |
| 账户新增 | `POST /api/accounts/save` |
| 账户列表 | `POST /api/accounts/list` |
| 账户更新 | `POST /api/accounts/update` |
| 账户删除 | `POST /api/accounts/delete` |
| 交易新增 | `POST /api/transactions/save` |
| 交易列表 | `POST /api/transactions/list`（带筛选） |
| 交易更新 | `POST /api/transactions/update` |
| 交易删除 | `POST /api/transactions/delete` |
| 资产总览 | `POST /api/assets/summary` |
| 持仓明细 | `POST /api/assets/holdings` |
| 资产趋势 | `POST /api/assets/snapshots` |
| Markdown 导入 | `POST /api/import/markdown` |

**7.3 定时任务手动验证**
- 不等凌晨 cron，直接临时改 cron 到下一分钟执行
- 验证 `asset_snapshot` 表有新记录
- 验证日志看到 `cache warmup` 痕迹

**7.4 更新 PROGRESS.md**
- 标记所有新增/重构项为已完成
- 记录端到端联调结果
- 添加"运行指南"小节

## 关键文件路径

| 路径 | 变更类型 |
|---|---|
| `backend/pom.xml` | 新增依赖 |
| `backend/src/main/resources/application.yml` | 缓存配置 |
| `backend/src/main/resources/db/init.sql` | 新增 asset_snapshot 表 |
| `backend/src/main/java/com/fm/FinancialManagementApplication.java` | 启用 @EnableScheduling @EnableCaching |
| `backend/src/main/java/com/fm/common/config/CacheConfig.java` | **新建** |
| `backend/src/main/java/com/fm/common/Constants.java` | 追加 TaskCron |
| `backend/src/main/java/com/fm/task/AssetSnapshotTask.java` | **新建** |
| `backend/src/main/java/com/fm/task/CacheWarmupTask.java` | **新建** |
| `backend/src/main/java/com/fm/asset/repository/entity/AssetSnapshotDO.java` | **新建** |
| `backend/src/main/java/com/fm/asset/repository/mapper/AssetSnapshotMapper.java` | **新建** |
| `backend/src/main/java/com/fm/asset/repository/service/IAssetSnapshotService.java` | **新建** |
| `backend/src/main/java/com/fm/asset/repository/service/IAssetSnapshotServiceImpl.java` | **新建** |
| `backend/src/main/java/com/fm/asset/service/AssetService.java` | 接口扩展（getSnapshots） |
| `backend/src/main/java/com/fm/asset/service/AssetServiceImpl.java` | 重构 BeanUtils + @Cacheable |
| `backend/src/main/java/com/fm/asset/controller/AssetController.java` | 追加 snapshots 接口 |
| `backend/src/main/java/com/fm/asset/controller/vo/AssetSnapshotRespVO.java` | **新建** |
| `backend/src/main/java/com/fm/account/service/AccountService.java` | 接口扩展（batchDelete） |
| `backend/src/main/java/com/fm/account/service/AccountServiceImpl.java` | 重构 BeanUtils + 级联删除 |
| `backend/src/main/java/com/fm/account/controller/AccountController.java` | 追加 batchDelete 接口 |
| `backend/src/main/java/com/fm/transaction/service/TransactionService.java` | 接口扩展（batchSave/statistics） |
| `backend/src/main/java/com/fm/transaction/service/TransactionServiceImpl.java` | 重构 BeanUtils + 批量/统计 |
| `backend/src/main/java/com/fm/transaction/controller/TransactionController.java` | 追加 batchSave/statistics 接口 |
| `backend/src/main/java/com/fm/import_data/service/ImportService.java` | 接口扩展（增强解析） |
| `backend/src/main/java/com/fm/import_data/service/ImportServiceImpl.java` | 重构 BeanUtils + 解析增强 |
| `backend/src/main/java/com/fm/import_data/service/MarkdownTableParser.java` | **新建** |
| `PROGRESS.md` | 更新进度 |

## 验收标准

- [ ] `mvn clean compile` 无任何错误
- [ ] `mvn spring-boot:run` 启动成功（日志看到 "Started FinancialManagementApplication"）
- [ ] 13 个核心 API 全部 curl 联调通过
- [ ] 定时任务：临时改 cron 后能在指定时间执行，写入 `asset_snapshot` 表
- [ ] 所有 `BeanUtils.copyProperties` 已替换为手动 setXxx
- [ ] 所有 controller 日志包含 traceId + [methodName] 前缀
- [ ] 不可变性：grep 验证 service 层无 `BeanUtils.copyProperties`
- [ ] PROGRESS.md 同步更新

## 关键风险与兜底

| 风险 | 兜底方案 |
|---|---|
| MySQL 不可用 | 临时切 H2 启动验证（保持代码默认 MySQL） |
| 缓存雪崩 | Caffeine 单机缓存，预热任务分散时间 |
| 资产快照表数据膨胀 | 加唯一索引 `snapshot_date`，upsert 避免重复 |
| 批量删除关联交易过多导致超时 | 分批 100 条一次删除 |
| Markdown 解析边界 case | 解析失败不影响整体，错误行入 `failReasons` |
| cron 触发后服务重启导致漏执行 | 启动时检查当日是否已有快照，无则补建 |
