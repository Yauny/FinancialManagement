import { useState, useEffect } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, DatePicker, message, InputNumber } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { listTransactions, saveTransaction, updateTransaction, deleteTransaction } from '../../api/transaction';
import { listAccounts } from '../../api/account';
import type { Transaction, TransactionSavePayload, Account } from '../../types';

const TRANS_TYPES = [
  { label: '买入', value: 'buy' },
  { label: '卖出', value: 'sell' },
  { label: '分红', value: 'dividend' },
  { label: '转账', value: 'transfer' },
];

export default function TransactionList() {
  const [data, setData] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<Transaction | null>(null);
  const [form] = Form.useForm();
  const [accounts, setAccounts] = useState<Account[]>([]);

  const fetchData = async () => {
    setLoading(true);
    try { setData(await listTransactions()); } 
    catch (e: any) { message.error(e.message); } 
    finally { setLoading(false); }
  };

  const fetchAccounts = async () => {
    try { setAccounts(await listAccounts()); }
    catch { /* ignore */ }
  };

  useEffect(() => { fetchData(); fetchAccounts(); }, []);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    const payload: TransactionSavePayload = {
      ...values,
      date: values.date.format('YYYY-MM-DD'),
    };
    try {
      if (editing) { await updateTransaction({ ...payload, id: editing.id }); message.success('更新成功'); } 
      else { await saveTransaction(payload); message.success('新增成功'); }
      setModalVisible(false); fetchData();
    } catch (e: any) { message.error(e.message); }
  };

  const openEdit = (record: Transaction) => {
    setEditing(record);
    form.setFieldsValue({ ...record, date: dayjs(record.date) });
    setModalVisible(true);
  };

  const columns: ColumnsType<Transaction> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '账户', dataIndex: 'accountName', width: 120 },
    { title: '金额', dataIndex: 'amount', width: 120, render: v => `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` },
    { title: '日期', dataIndex: 'date', width: 100 },
    { title: '品种', dataIndex: 'symbol' },
    { title: '类型', dataIndex: 'type', width: 80, render: v => TRANS_TYPES.find(t => t.value === v)?.label || v },
    { title: '备注', dataIndex: 'note', ellipsis: true },
    {
      title: '操作', width: 120, render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>编辑</Button>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => {
            Modal.confirm({ title: '确认删除该交易？', onOk: async () => {
              try { await deleteTransaction(record.id); message.success('删除成功'); fetchData(); }
              catch (e: any) { message.error(e.message); }
            }});
          }}>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditing(null); form.resetFields(); setModalVisible(true); }}>新增交易</Button>
      </div>
      <Table columns={columns} dataSource={data} rowKey="id" loading={loading} bordered pagination={{ pageSize: 20 }} />
      <Modal title={editing ? '编辑交易' : '新增交易'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)} width={600}>
        <Form form={form} layout="vertical" initialValues={{ type: 'buy', note: '' }}>
          <Form.Item name="accountId" label="账户" rules={[{ required: true, message: '请选择账户' }]}>
            <Select
              placeholder="请选择账户"
              options={accounts.map(a => ({ label: `${a.name}（${a.platform || a.type}）`, value: a.id }))}
              showSearch
              optionFilterProp="label"
            />
          </Form.Item>
          <Form.Item name="amount" label="金额" rules={[{ required: true, message: '请输入金额' }]}>
            <InputNumber min={0} style={{ width: '100%' }} prefix="¥" precision={2} />
          </Form.Item>
          <Form.Item name="date" label="交易日期" rules={[{ required: true, message: '请选择日期' }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="symbol" label="品种名称" rules={[{ required: true, message: '请输入品种' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="type" label="交易类型" rules={[{ required: true, message: '请选择类型' }]}>
            <Select options={TRANS_TYPES} />
          </Form.Item>
          <Form.Item name="note" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
