# 项目进度

## 当前阶段
后端 5 大模块全部完成，端到端 API 联调 + 定时任务验证通过 ✅
前端页面与后端 API 完整对接 ✅

## 已完成

### 阶段一：基础设施与缓存
- [x] pom.xml 新增 spring-boot-starter-data-redis + commons-pool2 依赖（替换 Caffeine）
- [x] FinancialManagementApplication 启用 @EnableScheduling + @EnableCaching
- [x] 创建 CacheConfig.java（Redis：JSON 序列化、key 前缀 fm:、5 分钟 TTL）
- [x] db/init.sql 追加 asset_snapshot 表（H2 MySQL 模式 + MySQL 8 双兼容）
- [x] application.yml 启用 spring.data.redis + spring.cache.type=redis
- [x] 编译验证阶段一

### 阶段二：定时任务模块
- [x] AssetSnapshotTask（每日 02:00 写入资产快照，cron 可 yml 覆盖）
- [x] CacheWarmupTask（每日 02:30 预热资产缓存）
- [x] Constants 任务 cron 集中管理

### 阶段三：资产模块重构
- [x] 重构 AssetServiceImpl（去除 BeanUtils.copyProperties，改手动 setXxx）
- [x] @Cacheable 注解集成（CACHE_ASSET_SUMMARY / CACHE_ASSET_HOLDINGS）
- [x] Spring 事件驱动缓存失效：AssetCacheEvictEvent + @TransactionalEventListener(AFTER_COMMIT)
- [x] CacheManager.clear() 手动清空（避免内部调用 AOP 不生效问题）
- [x] 新增 getSnapshots API + AssetSnapshotRespVO

### 阶段四：账户模块重构
- [x] AccountServiceImpl 重构为不可变模式
- [x] batchDelete 批量删除（分区 + 整批）
- [x] 级联软删除（删除账户时同步软删交易）
- [x] AccountController /batchDelete 端点

### 阶段五：交易模块重构
- [x] TransactionServiceImpl 重构为不可变模式
- [x] batchSave 批量保存
- [x] getStatistics 统计 API（按时间区间聚合 buy/sell/dividend/transfer）
- [x] N+1 查询修复（accountNameMap 预加载）
- [x] TransactionController /batchSave + /statistics 端点

### 阶段六：导入模块重构
- [x] 抽离 MarkdownTableParser（独立解析器）
- [x] 解析增强：支持空行、错误行容错
- [x] ImportRespVO 增加 failReasons 字段
- [x] 业务规则校验（账户存在性、类型合法、日期格式）

### 阶段七：端到端联调 ✅
- [x] 创建 application-test.yml（H2 MySQL 模式）
- [x] H2 scope 改为 compile（解决 spring-boot:run 类路径问题）
- [x] 修复 init.sql H2 兼容性（去掉 ENUM / ENGINE=InnoDB / COLLATE）
- [x] 定时任务 cron 改为 @Value 注入（test profile 下每 10/20 秒触发）
- [x] 服务启动成功（端口 8080，1.099 秒）
- [x] 账户 CRUD API 验证（list / save / get / update / delete / batchDelete）
- [x] 交易 CRUD API 验证（list / save / get / update / delete / batchSave / statistics）
- [x] 资产 API 验证（summary / holdings / snapshots）
- [x] Markdown 导入 API 验证（successCount=1, failCount=0）
- [x] 批量删除账户级联软删交易验证
- [x] 定时任务执行验证（快照自动写入，totalAsset=8888.88）

### 阶段八：缓存层切换为 Redis ✅
- [x] pom.xml 移除 Caffeine，新增 spring-boot-starter-data-redis + commons-pool2
- [x] CacheConfig.java 重写为 RedisCacheManager（GenericJackson2JsonRedisSerializer + String key）
- [x] key 命名 `fm:{cacheName}:{key}`，value 带 @class 类型信息
- [x] 启动本地 Redis 8.0.1（127.0.0.1:6379，daemonize）
- [x] application.yml 配置 spring.data.redis（host/port/pool）
- [x] 修复一个隐藏 Bug：@CacheEvict 在类内调用 AOP 不生效 → 改用注入 CacheManager.clear()
- [x] 修复事务提交后缓存不刷新：@TransactionalEventListener(AFTER_COMMIT) 替换 @EventListener
- [x] 事件机制解耦：AssetCacheEvictEvent 监听 AccountService / TransactionService / ImportService 的变更
- [x] 验证 Redis 缓存：key `fm:assetSummary:all`，TTL 287s，JSON 序列化
- [x] 验证缓存命中与失效全链路：建账户/交易 → 清缓存 → summary 重新计算 → 8988.88

### 阶段九：前端 UI 完善 ✅
- [x] 交易表单：accountId 从手动输入改为 Select 下拉选择（显示账户名称）
- [x] 交易表单：getFieldsValue 改为 validateFields 启用前端表单校验
- [x] 交易删除操作：增加 Modal.confirm 确认弹窗（防止误删）
- [x] 导入页：目标账户从 InputNumber 改为 Select 下拉选择
- [x] 全页面 CRUD 操作验证通过（新增账户/编辑/删除、新增交易、数据导入）
- [x] 资产总览、账户管理、交易记录、数据导入 4 个页面全部正常运行

## 待处理
- [ ] 真实 MySQL 3306 部署（修改 application.yml 连接信息）
- [x] 前端页面与后端 API 对接（4 个页面：资产总览、账户管理、交易记录、数据导入）
- [ ] 单元测试补充（端到端已通，单元测试覆盖率 0%）
- [ ] 业务规则完善（如 sell 金额正负处理、跨日分红累计等）

## 技术说明
- JDK 17：/Users/yauny/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home
- Maven：/Users/yauny/kwwProjects/tool/apache-maven-3.9.9/bin/mvn
- Lombok 1.18.34 + 显式 annotation processor 配置
- Spring Boot 3.3.1 + MyBatis Plus 3.5.6
- **Redis 8.0.1**（本地 127.0.0.1:6379，daemonize，所有环境统一使用）
- H2 (test) 2.2.224，MySQL 8.3.0 (prod)
- 不可变模式：所有 DO→VO/BO 转换均手动 setXxx，不使用 BeanUtils
- 缓存：Redis 5 分钟 TTL，key 格式 `fm:{cacheName}:{key}`，JSON 序列化
- 任务：@Scheduled + cron，test 模式 10/20 秒，prod 模式 2:00/2:30
- 全部 POST + JSON body 接口，日志带 traceId + [methodName] 前缀
