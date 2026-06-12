import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Layout, Menu } from 'antd';
import { BankOutlined, SwapOutlined, PieChartOutlined, UploadOutlined } from '@ant-design/icons';
import AccountList from './pages/Accounts/AccountList';
import TransactionList from './pages/Transactions/TransactionList';
import AssetOverview from './pages/Assets/AssetOverview';
import ImportPage from './pages/Import/ImportPage';
import { useNavigate, useLocation } from 'react-router-dom';
import type { ItemType } from 'antd/es/menu/interface';

const { Header, Content } = Layout;

function AppLayout({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const location = useLocation();

  const items: ItemType[] = [
    { key: '/', icon: <PieChartOutlined />, label: '资产总览' },
    { key: '/accounts', icon: <BankOutlined />, label: '账户管理' },
    { key: '/transactions', icon: <SwapOutlined />, label: '交易记录' },
    { key: '/import', icon: <UploadOutlined />, label: '数据导入' },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center' }}>
        <div style={{ color: 'white', fontSize: 18, fontWeight: 'bold', marginRight: 40 }}>
          个人财务管理系统
        </div>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[location.pathname]}
          items={items}
          onClick={({ key }) => navigate(key)}
        />
      </Header>
      <Content style={{ padding: '24px 24px' }}>
        <div style={{ background: '#fff', padding: 24, borderRadius: 8, minHeight: 'calc(100vh - 112px)' }}>
          {children}
        </div>
      </Content>
    </Layout>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AppLayout>
        <Routes>
          <Route path="/" element={<AssetOverview />} />
          <Route path="/accounts" element={<AccountList />} />
          <Route path="/transactions" element={<TransactionList />} />
          <Route path="/import" element={<ImportPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AppLayout>
    </BrowserRouter>
  );
}
