import { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, Table, Spin, message } from 'antd';
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { getAssetSummary, getHoldings } from '../../api/asset';
import type { AssetSummary, Holdings, AllocationItem, AccountHoldings } from '../../types';

const COLORS = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4'];

const TYPE_LABELS: Record<string, string> = {
  platform: '平台账户',
  account: '普通账户',
  bank: '银行卡',
  card: '信用卡',
};

export default function AssetOverview() {
  const [summary, setSummary] = useState<AssetSummary | null>(null);
  const [holdings, setHoldings] = useState<Holdings | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [s, h] = await Promise.all([getAssetSummary(), getHoldings()]);
      setSummary(s);
      setHoldings(h);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  if (loading) return <Spin size="large" style={{ display: 'flex', justifyContent: 'center', marginTop: 100 }} />;

  const pieData = summary?.allocationList?.map((item: AllocationItem) => ({
    name: TYPE_LABELS[item.type] || item.type,
    value: Number(item.amount),
    ratio: Number(item.ratio),
  })) || [];

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={24}>
          <Card title="总资产">
            <Statistic value={summary?.totalAsset || 0} precision={2} prefix="¥" />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="资产配置（饼图）">
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie 
                  data={pieData} 
                  dataKey="value" 
                  nameKey="name" 
                  cx="50%" 
                  cy="50%" 
                  outerRadius={100}
                  label={({ name, payload }) => `${name} ${(payload as any).ratio?.toFixed(1)}%`}
                >
                  {pieData.map((_: unknown, i: number) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip formatter={(v: unknown) => `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`} />
              </PieChart>
            </ResponsiveContainer>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="资产配置（条形图）">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={pieData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis />
                <Tooltip formatter={(v: unknown) => `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`} />
                <Bar dataKey="value" fill="#5470c6" />
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </Col>
      </Row>

      <Card title="持仓明细">
        {holdings?.accountHoldings?.map((ah: AccountHoldings) => (
          <div key={ah.accountId} style={{ marginBottom: 16 }}>
            <h4>{ah.accountName}（{TYPE_LABELS[ah.accountType] || ah.accountType}）- ¥{Number(ah.totalAmount).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}</h4>
            <Table
              dataSource={ah.holdings}
              rowKey="symbol"
              size="small"
              pagination={false}
              columns={[
                { title: '品种', dataIndex: 'symbol' },
                { title: '持仓金额', dataIndex: 'totalAmount', render: v => `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` },
                { title: '交易次数', dataIndex: 'transactionCount' },
              ]}
            />
          </div>
        ))}
        {(!holdings?.accountHoldings || holdings.accountHoldings.length === 0) && (
          <div style={{ textAlign: 'center', color: '#999', padding: 20 }}>暂无持仓数据</div>
        )}
      </Card>
    </div>
  );
}
