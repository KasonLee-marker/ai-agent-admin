# Spec 工作流程

## 流程图

```
需求提出 → 写 Spec → Review → 修改 → Approve → 实现 → 验证 → Done
```

## 详细步骤

### Step 1: 创建 Spec 文件

```bash
# 在 docs/specs/ 目录下创建
cp docs/spec-template.md docs/specs/xxx-feature-spec.md
```

文件命名规范：`{功能名}-spec.md`

### Step 2: 编写 Spec

按照模板填写各章节，重点关注：

- **需求分析** - 明确输入输出
- **设计方案** - 架构、数据模型、API
- **交互设计** - 用户操作流程
- **测试计划** - 测试场景和边界条件

### Step 3: Review

提交给相关人员 Review（可以是：

- 技术负责人
- 产品负责人
- 或者直接在对话中让 Claude Review

### Step 4: Approve

Review 通过后，更新状态为 `APPROVED`

### Step 5: 实现

按照 Spec 实现，实现过程中如发现设计问题：

- 更新 Spec（记录变更）
- 继续实现

### Step 6: 验证

实现完成后，按 Spec 的测试计划验证功能

### Step 7: Done

验证通过后，状态更新为 `DONE`

---

## Spec 存放位置

```
docs/
├── spec-template.md      # 模板
├── specs/                # 所有 spec 文件
│   ├── semantic-chunking-spec.md
│   ├── rag-evaluation-spec.md
│   └── ...
└── changelog.md          # 变更日志
```

---

## 与 Claude 的协作方式

### 方式 1: Claude 帮你写 Spec

```
用户: 我要做一个 XXX 功能，帮我写个 spec
Claude: [基于模板生成 spec 文档]
用户: Review 并修改
Claude: [更新 spec]
用户: 开始实现
Claude: [按 spec 实现]
```

### 方式 2: 你写 Spec，Claude 实现

```
用户: [已写好的 spec]
Claude: 我来按这个 spec 实现
Claude: [实现功能]
```

### 方式 3: 边讨论边写 Spec

```
用户: 我想做个 XXX 功能，先讨论下设计
Claude: [讨论需求和技术方案]
用户: 好的，把这些讨论结果写成 spec
Claude: [生成 spec 文档]
用户: 确认 spec，开始实现
```

---

## Spec 的价值

| 阶段      | 价值          |
|---------|-------------|
| 编写时     | 理清思路，发现遗漏   |
| Review时 | 发现设计问题，避免返工 |
| 实现时     | 有明确指引，不偏离   |
| 测试时     | 有测试计划，验证完整  |
| 维护时     | 有文档可查，理解历史  |