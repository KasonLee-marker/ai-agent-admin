# AGENTS.md - AI Agent Admin 项目导航

## 项目概述

- **名称**: AI Agent Admin
- **技术栈**: Spring Boot 3.x + Spring AI + React 18 + MySQL/H2
- **目的**: 企业内网 AI Agent 管理平台（Prompt 管理、模型调用、对话调试、数据集管理、评估系统）

## 快速链接

- 架构文档: docs/architecture.md
- API 文档: docs/api/
- 活跃任务: tasks/active/
- 约束规则: .agent-harness/constraints/

## 关键约束

1. **内网部署**: 不使用 Nacos、服务注册等分布式组件
2. **百炼 API**: 使用 DashScope (qwen3.5-omni-plus) 作为默认模型
3. **简化架构**: 单节点部署，配置本地化
4. **质量门禁**: 所有代码需通过单元测试和代码审查

## 模块清单

| 模块 | 状态 | 优先级 |
|------|------|--------|
| Prompt 管理 | 🔄 待开始 | P0 |
| 模型管理 | 🔄 待开始 | P0 |
| 对话调试 | 🔄 待开始 | P0 |
| 数据集管理 | 🔄 待开始 | P1 |
| 评估系统 | 🔄 待开始 | P1 |
| 可观测性 | 🔄 待开始 | P2 |

## 任务工作流

1. 读取 tasks/active/<task>.md 获取上下文
2. 按照约束规则实现功能
3. 自审查通过质量门禁
4. 更新任务状态并报告完成
5. 将任务移动到 tasks/completed/

## 百炼 API 配置

```yaml
model: qwen3.5-omni-plus-2026-03-15
api-key: sk-852f050ac5514871b39b3e8d7ffcc490
base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
```

## 沟通方式

- 进度更新: 追加到活跃任务文件
- 阻塞问题: 立即标记 [BLOCKER] 前缀
- 疑问: 在任务文件的 [QUESTION] 部分提出
