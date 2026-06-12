package com.fm.import_data.service;

import com.fm.common.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obsidian Markdown 表格解析器
 *
 * 支持格式：
 * | 品种 | 代码 | 金额 | 占比 | 费率 | 评价 |
 * | 医疗基金（小荷包） | - | 11,543.93 元 | 41% | 0 | ✅ 存钱账户，继续攒 |
 *
 * 兼容：
 * - "元" 后缀可选
 * - 千分位逗号
 * - 代码列允许 "-" 表示无
 */
@Slf4j
@Component
public class MarkdownTableParser {

    /**
     * 单行匹配模式：| 品种 | 代码 | 金额(可带"元"后缀) | ...
     */
    private static final Pattern ROW_PATTERN = Pattern.compile(
            "\\|\\s*([^|]+?)\\s*\\|\\s*([^|]*?)\\s*\\|\\s*([\\d,]+\\.?\\d*)\\s*元?\\s*\\|"
    );

    /**
     * 解析 Markdown 文本为 ParsedRow 列表（纯函数，无副作用）
     */
    public List<ParsedRow> parse(String markdownContent) {
        List<ParsedRow> rows = new ArrayList<>();
        if (markdownContent == null || markdownContent.isBlank()) {
            return rows;
        }

        Matcher matcher = ROW_PATTERN.matcher(markdownContent);
        while (matcher.find()) {
            ParsedRow row = new ParsedRow();
            String symbol = matcher.group(1) == null ? "" : matcher.group(1).trim();
            String code = matcher.group(2) == null ? "" : matcher.group(2).trim();
            String amountStr = matcher.group(3) == null ? "" : matcher.group(3).trim();

            row.setSymbol(symbol);
            row.setCode("-".equals(code) ? "" : code);

            if (symbol.isEmpty()) {
                row.setSuccess(false);
                row.setErrorReason("品种名称为空");
                rows.add(row);
                continue;
            }
            if (amountStr.isEmpty()) {
                row.setSuccess(false);
                row.setErrorReason("金额为空");
                rows.add(row);
                continue;
            }

            try {
                BigDecimal amount = new BigDecimal(amountStr.replace(",", ""));
                row.setAmount(amount);
                row.setSuccess(true);
            } catch (NumberFormatException e) {
                row.setSuccess(false);
                row.setErrorReason("金额解析失败：" + amountStr);
            }
            rows.add(row);
        }
        return rows;
    }
}
