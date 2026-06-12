import { useState, useEffect } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { listAccounts, saveAccount, updateAccount, deleteAccount } from '../../api/account';
import type { Account, AccountSavePayload } from '../../types';

const ACCOUNT_TYPES = [
  { label: '平台', value: 'platform' },
  { label: '账户', value: 'account' },
  { label: '银行', value: 'bank' },
  { label: '卡片', value: 'card' },
];

export default function AccountList() {
  const [data, setData] = useState<Account[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<Account | null>(null);
  const [form] = Form.useForm<AccountSavePayload>();

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await listAccounts();
      setData(res);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleAdd = () => { setEditing(null); form.resetFields(); setModalVisible(true); };
  const handleEdit = (record: Account) => { setEditing(record); form.setFieldsValue(record); setModalVisible(true); };
  const handleDelete = async (id: number) => {
    Modal.confirm({ title: '确认删除？', onOk: async () => {
      try { await deleteAccount(id); message.success('删除成功'); fetchData(); } 
      catch (e: any) { message.error(e.message); }
    }});
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    try {
      if (editing) { await updateAccount({ ...values, id: editing.id }); message.success('更新成功'); } 
      else { await saveAccount(values); message.success('新增成功'); }
      setModalVisible(false); fetchData();
    } catch (e: any) { message.error(e.message); }
  };

  const columns: ColumnsType<Account> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '账户名称', dataIndex: 'name' },
    { title: '类型', dataIndex: 'type', render: v => ACCOUNT_TYPES.find(t => t.value === v)?.label || v },
    { title: '平台', dataIndex: 'platform' },
    { title: '创建时间', dataIndex: 'createdAt', render: v => v ? v.slice(0, 16) : '' },
    {
      title: '操作', render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Button size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新增账户</Button>
      </div>
      <Table columns={columns} dataSource={data} rowKey="id" loading={loading} bordered />
      <Modal title={editing ? '编辑账户' : '新增账户'} open={modalVisible} onOk={handleSubmit} onCancel={() => setModalVisible(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="账户名称" rules={[{ required: true, message: '请输入账户名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="type" label="账户类型" rules={[{ required: true, message: '请选择类型' }]}>
            <Select options={ACCOUNT_TYPES} />
          </Form.Item>
          <Form.Item name="platform" label="平台来源">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
