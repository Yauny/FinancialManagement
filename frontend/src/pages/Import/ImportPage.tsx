import { useState, useEffect } from 'react';
import { Card, Form, Input, Select, Button, Result, message } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { importMarkdown } from '../../api/import';
import { listAccounts } from '../../api/account';
import type { ImportResult, Account } from '../../types';

const { TextArea } = Input;

export default function ImportPage() {
  const [accountId, setAccountId] = useState<number | null>(null);
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [accounts, setAccounts] = useState<Account[]>([]);

  useEffect(() => {
    listAccounts().then(setAccounts).catch(() => {});
  }, []);

  const handleImport = async () => {
    if (!accountId) { message.error('请选择目标账户'); return; }
    if (!content.trim()) { message.error('请输入Markdown内容'); return; }
    setLoading(true);
    try {
      const res = await importMarkdown(accountId, content);
      setResult(res);
      message.success('导入完成');
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Card title="从 Obsidian Markdown 导入">
        <p style={{ marginBottom: 16, color: '#666' }}>
          选择目标账户，粘贴 Obsidian 导出的 Markdown 表格，系统将自动解析品种和金额并批量创建买入交易记录。
        </p>
        <Form layout="vertical">
          <Form.Item label="目标账户" required>
            <Select
              placeholder="请选择目标账户"
              value={accountId}
              onChange={v => setAccountId(v)}
              style={{ width: 300 }}
              options={accounts.map(a => ({ label: `${a.name}（${a.platform || a.type}）`, value: a.id }))}
            />
          </Form.Item>
          <Form.Item label="Markdown 内容">
            <TextArea rows={12} value={content} onChange={e => setContent(e.target.value)} placeholder={`| 品种 | 代码 | 金额 | 占比 | 费率 | 评价 |
|------|------|------|------|------|------|
| 医疗基金（小荷包） | - | 11,543.93 元 | 41% | 0 | ✅ 存钱账户，继续攒 |`} />
          </Form.Item>
          <Button type="primary" icon={<UploadOutlined />} loading={loading} onClick={handleImport}>开始导入</Button>
        </Form>
      </Card>

      {result && (
        <Card title="导入结果" style={{ marginTop: 16 }}>
          <Result
            status={result.failCount === 0 ? 'success' : 'warning'}
            title={`导入完成：${result.successCount} 条成功，${result.failCount} 条失败`}
            subTitle={`共解析 ${result.totalRows} 行数据`}
            extra={<Button onClick={() => { setResult(null); setContent(''); }}>继续导入</Button>}
          />
        </Card>
      )}
    </div>
  );
}
