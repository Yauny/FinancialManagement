# 个人财务管理系统 - 开发计划

> 版本：v1.0 | 日期：2025-06-12

---

## 一、技术栈

| 层级 | 技术选型 |
|------|----------|
| 前端 | React + Ant Design（推荐，中文文档完善，企业级组件全） |
| 后端 | Java + Spring Boot |
| 数据库 | MySQL（端口 3306，本地） |
| 架构 | 前后端分离，单体应用 |

---

## 二、MVP 功能范围

### 第一版

| 模块 | 功能 |
|------|------|
| 账户管理 | 新增 / 编辑 / 删除账户（名称、类型、平台） |
| 交易记录 | 录入 / 编辑 / 删除交易（金额、日期、品种、类型、备注） |
| 资产总览 | 配置占比图（饼图 / 条形图） |
| 持仓明细 | 各账户持仓列表 |
| 数据导入 | 解析 Obsidian Markdown 表格，一次性导入 |

### 后续迭代

| 模块 | 功能 |
|------|------|
| 历史趋势 | 净值曲线、月度收益走势 |
| 分析指标 | 收益率、费率对比、配置偏离预警 |
| 提醒功能 | 定投日提醒、配置偏离告警 |

---

## 三、数据模型

### 账户表（account）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 账户名称 |
| type | ENUM('platform','account','bank','card') | 账户类型 |
| platform | VARCHAR(50) | 平台来源 |
| created_at | DATETIME | 创建时间 |

### 交易表（transaction）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| account_id | BIGINT | 外键，关联账户 |
| amount | DECIMAL(12,2) | 金额 |
| date | DATE | 交易日期 |
| symbol | VARCHAR(100) | 品种名称/代码 |
| type | ENUM('buy','sell','dividend','transfer') | 交易类型 |
| note | TEXT | 备注 |
| created_at | DATETIME | 创建时间 |

---

## 四、接口设计（REST API）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/accounts | 获取账户列表 |
| POST | /api/accounts | 新增账户 |
| PUT | /api/accounts/{id} | 编辑账户 |
| DELETE | /api/accounts/{id} | 删除账户 |
| GET | /api/transactions | 获取交易列表（支持按账户筛选） |
| POST | /api/transactions | 录入交易 |
| PUT | /api/transactions/{id} | 编辑交易 |
| DELETE | /api/transactions/{id} | 删除交易 |
| GET | /api/assets/summary | 资产总览（聚合计算） |
| GET | /api/assets/holdings | 持仓明细（按账户聚合） |
| POST | /api/import | 导入 Obsidian Markdown 数据 |

---

## 五、前端页面结构

```
/                    → 仪表盘（资产总览）
/accounts            → 账户管理列表
/accounts/new        → 新增账户
/accounts/:id        → 账户详情
/transactions        → 交易记录列表
/transactions/new    → 新增交易
/import              → 数据导入页
```

---

## 六、Obsidian 导入格式约定

系统解析 Markdown 表格，预期格式：

```markdown
| 品种 | 代码 | 金额 | 占比 | 费率 | 评价 |
|------|------|------|------|------|------|
| 医疗基金（小荷包） | - | 11,543.93 元 | 41% | 0 | ✅ 存钱账户，继续攒 |
```

导入时用户选择对应账户，然后批量导入品种和金额。

---

## 七、验收标准

- [ ] Spring Boot 后端启动成功，连接本地 MySQL
- [ ] 账户 CRUD 操作正常
- [ ] 交易 CRUD 操作正常
- [ ] 资产总览展示饼图/占比
- [ ] 持仓明细按账户聚合显示
- [ ] Obsidian Markdown 表格解析并导入
- [ ] 前端页面可正常访问

---

## 八、假设与约束

1. 单用户本地使用，暂不考虑权限系统
2. 第一版不含分析指标，后续迭代
3. 数据全手动录入/导入，暂不接入券商 API
4. 前后端分离部署，本地启动两个服务（前端 3000，后端 8080）
