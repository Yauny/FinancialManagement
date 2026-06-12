import { post } from './request';
import type { Account, AccountSavePayload, AccountQuery } from '../types';

export async function listAccounts(query?: AccountQuery) {
  return post<Account[]>('/accounts/list', query || {});
}

export async function getAccount(id: number) {
  return post<Account | null>('/accounts/get', { id });
}

export async function saveAccount(payload: AccountSavePayload) {
  return post('/accounts/save', payload);
}

export async function updateAccount(payload: AccountSavePayload) {
  return post('/accounts/update', payload);
}

export async function deleteAccount(id: number) {
  return post('/accounts/delete', { id });
}
