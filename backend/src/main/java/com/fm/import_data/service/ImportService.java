package com.fm.import_data.service;

import com.fm.import_data.controller.vo.ImportRespVO;

/**
 * Obsidian Markdown 数据导入服务
 */
public interface ImportService {

    ImportRespVO importFromMarkdown(Long accountId, String markdownContent);
}
