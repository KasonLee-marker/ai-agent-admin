# Agent Harness 工作流规范

## 开发流程

### 1. 任务创建 (Human)
- 在 `tasks/active/` 创建任务文件
- 明确需求、约束、完成标准
- 指定检查点 (Checkpoint)

### 2. 分支创建 (Human/Agent)
```bash
# 从 main 创建功能分支
git checkout -b feature/<task-name>
```

### 3. 隔离开发 (Agent)
- Agent 在 feature 分支上工作
- 定期提交进度
- 每个检查点报告状态

### 4. 自检 (Agent)
```bash
# 运行测试
mvn test

# 检查代码质量
mvn spotbugs:check
mvn checkstyle:check

# 检查测试覆盖率
mvn jacoco:report
```

### 5. 代码审查 (Review Agent)
- 创建 PR (Pull Request)
- Review Agent 审查代码
- 检查约束合规性

### 6. 合并 (Human)
- 审查通过后合并到 main
- 删除 feature 分支
- 更新任务状态

## 目录结构

```
.agent-harness/
├── config.yaml          # 项目配置
├── workflow.md          # 本文件
├── constraints/         # 约束规则
│   └── architecture.yaml
└── templates/           # 代码模板
    ├── AGENTS.md
    └── task-template.md

work/                    # Agent 工作区 (gitignored)
├── feature-prompt-service/
├── feature-model-service/
└── ...
```

## 检查点报告格式

```markdown
## Progress Log

### Checkpoint N - <timestamp>
Status: [COMPLETE|IN_PROGRESS|BLOCKED]
Branch: feature/<name>
Completed:
- <item 1>
- <item 2>
Next: <next step>
Blockers: <blockers or "None">
```

## 质量门禁

### 代码质量
- [ ] 测试覆盖率 >= 70%
- [ ] 无 SpotBugs 高危警告
- [ ] 代码风格合规
- [ ] API 文档完整

### 架构合规
- [ ] 分层架构正确
- [ ] 无禁止的依赖关系
- [ ] 配置外置（无硬编码）

## 沟通规则

- **进度更新**: 追加到任务文件 Progress Log
- **阻塞问题**: 标记 [BLOCKER] 前缀
- **疑问**: 标记 [QUESTION] 前缀
- **完成**: 标记 [READY_FOR_REVIEW]
