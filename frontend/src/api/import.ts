import { post } from './request';
import type { ImportResult } from '../types';

export async function importMarkdown(accountId: number, markdownContent: string) {
  return post<ImportResult>('/import/markdown', { accountId, markdownContent });
}
