package com.aiagent.admin.service.tool.executor;

import com.aiagent.admin.service.tool.ExecutionContext;
import com.aiagent.admin.service.tool.ToolExecutor;
import com.aiagent.admin.service.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 计算器工具执行器
 * <p>
 * 执行数学表达式计算，支持基本算术运算。
 * 使用 JavaScript 引擎（Nashorn/GraalJS）计算表达式。
 * </p>
 */
@Slf4j
@Component
public class CalculatorExecutor implements ToolExecutor {

    @Override
    public String getName() {
        return "calculatorExecutor";
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ExecutionContext context) {
        long startTime = System.currentTimeMillis();

        try {
            String expression = (String) args.get("expression");
            if (expression == null || expression.isEmpty()) {
                return ToolResult.failure("Expression is required");
            }

            // 安全校验：只允许数字和基本运算符
            if (!isValidExpression(expression)) {
                return ToolResult.failure("Invalid expression: only numbers and basic operators (+-*/^%().) allowed");
            }

            // 使用简单解析计算（避免引入 JS 引擎的复杂性）
            double result = evaluateExpression(expression);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Calculator executed: {} = {} ({}ms)", expression, result, duration);

            return ToolResult.success(Map.of(
                    "expression", expression,
                    "result", result
            ), duration);

        } catch (Exception e) {
            log.error("Calculator execution failed: {}", e.getMessage());
            return ToolResult.failure("Calculation failed: " + e.getMessage());
        }
    }

    @Override
    public String getSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "expression": {
                      "type": "string",
                      "description": "Mathematical expression to evaluate, e.g., '2 + 3 * 4'"
                    }
                  },
                  "required": ["expression"]
                }
                """;
    }

    /**
     * 校验表达式是否合法
     * <p>
     * 只允许数字、基本运算符和括号。
     * </p>
     */
    private boolean isValidExpression(String expression) {
        // 移除空格
        String clean = expression.replaceAll("\\s+", "");
        // 只允许数字、运算符、括号、小数点
        return clean.matches("^[0-9+\\-*/^%().]+$");
    }

    /**
     * 计算表达式
     * <p>
     * 使用递归下降解析器计算，支持加减乘除和括号。
     * </p>
     */
    private double evaluateExpression(String expression) {
        // 移除空格
        String clean = expression.replaceAll("\\s+", "");
        return new ExpressionParser(clean).parse();
    }

    /**
     * 简单表达式解析器
     * <p>
     * 支持加减乘除和括号，使用递归下降算法。
     * </p>
     */
    private static class ExpressionParser {
        private final String expr;
        private int pos = 0;

        public ExpressionParser(String expr) {
            this.expr = expr;
        }

        public double parse() {
            return parseExpression();
        }

        private double parseExpression() {
            double result = parseTerm();
            while (pos < expr.length()) {
                char op = expr.charAt(pos);
                if (op == '+' || op == '-') {
                    pos++;
                    double term = parseTerm();
                    if (op == '+') {
                        result += term;
                    } else {
                        result -= term;
                    }
                } else {
                    break;
                }
            }
            return result;
        }

        private double parseTerm() {
            double result = parseFactor();
            while (pos < expr.length()) {
                char op = expr.charAt(pos);
                if (op == '*' || op == '/') {
                    pos++;
                    double factor = parseFactor();
                    if (op == '*') {
                        result *= factor;
                    } else {
                        result /= factor;
                    }
                } else {
                    break;
                }
            }
            return result;
        }

        private double parseFactor() {
            // 处理负号
            if (pos < expr.length() && expr.charAt(pos) == '-') {
                pos++;
                return -parseFactor();
            }

            // 处理括号
            if (pos < expr.length() && expr.charAt(pos) == '(') {
                pos++;
                double result = parseExpression();
                if (pos < expr.length() && expr.charAt(pos) == ')') {
                    pos++;
                }
                return result;
            }

            // 解析数字
            StringBuilder num = new StringBuilder();
            while (pos < expr.length() && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) {
                num.append(expr.charAt(pos++));
            }
            return Double.parseDouble(num.toString());
        }
    }
}