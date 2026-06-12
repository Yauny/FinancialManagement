# 项目进度

## 当前阶段
后端 MVP 功能模块开发

## 已完成
- [x] 搭建后端 Maven 项目结构（Spring Boot 3.3 + MyBatis Plus 3.5）
- [x] 编写 MySQL 数据库初始化脚本（account / transaction 两张表）
- [x] 实现 Account 模块（Entity + Mapper + IAccountService + AccountService + AccountController）
- [x] 实现 Transaction 模块（Entity + Mapper + ITransactionService + TransactionService + TransactionController）
- [x] 实现 Asset 模块（资产总览 / 持仓明细）
- [x] 实现 Obsidian Markdown 数据导入模块
- [x] 审计字段自动填充（AuditMetaObjectHandler）

## 进行中
- [ ] 编译验证后端项目
- [ ] 初始化 MySQL 数据库

## 待处理
- [ ] 搭建 React + Ant Design 前端项目
- [ ] 实现前端页面（账户管理、交易记录、资产总览、导入页）
- [ ] 端到端联调测试
