// 账户类型
export type AccountType = 'platform' | 'account' | 'bank' | 'card';

// 交易类型
export type TransType = 'buy' | 'sell' | 'dividend' | 'transfer';

// 账户
export interface Account {
  id: number;
  name: string;
  type: AccountType;
  platform: string;
  createdAt: string;
}

// 账户保存/更新
export interface AccountSavePayload {
  id?: number;
  name: string;
  type: AccountType;
  platform: string;
}

// 账户查询
export interface AccountQuery {
  type?: AccountType;
  platform?: string;
}

// 交易
export interface Transaction {
  id: number;
  accountId: number;
  accountName: string;
  amount: number;
  date: string;
  symbol: string;
  type: TransType;
  note: string;
  createdAt: string;
}

// 交易保存/更新
export interface TransactionSavePayload {
  id?: number;
  accountId: number;
  amount: number;
  date: string;
  symbol: string;
  type: TransType;
  note?: string;
}

// 交易查询
export interface TransactionQuery {
  accountId?: number;
  symbol?: string;
  type?: TransType;
  startDate?: string;
  endDate?: string;
}

// 资产配置项
export interface AllocationItem {
  type: string;
  amount: number;
  ratio: number;
}

// 资产总览
export interface AssetSummary {
  totalAsset: number;
  allocationList: AllocationItem[];
}

// 持仓明细
export interface SymbolHolding {
  symbol: string;
  totalAmount: number;
  transactionCount: number;
}

export interface AccountHoldings {
  accountId: number;
  accountName: string;
  accountType: string;
  totalAmount: number;
  holdings: SymbolHolding[];
}

export interface Holdings {
  accountHoldings: AccountHoldings[];
}

// 导入结果
export interface ImportResult {
  totalRows: number;
  successCount: number;
  failCount: number;
  message: string;
}

// API 统一响应
export interface ApiResponse<T> {
  success: boolean;
  traceId: string;
  message?: string;
  data: T;
}
