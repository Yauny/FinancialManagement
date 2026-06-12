import { post } from './request';
import type { Transaction, TransactionSavePayload, TransactionQuery } from '../types';

export async function listTransactions(query?: TransactionQuery) {
  return post<Transaction[]>('/transactions/list', query || {});
}

export async function getTransaction(id: number) {
  return post<Transaction | null>('/transactions/get', { id });
}

export async function saveTransaction(payload: TransactionSavePayload) {
  return post('/transactions/save', payload);
}

export async function updateTransaction(payload: TransactionSavePayload) {
  return post('/transactions/update', payload);
}

export async function deleteTransaction(id: number) {
  return post('/transactions/delete', { id });
}
