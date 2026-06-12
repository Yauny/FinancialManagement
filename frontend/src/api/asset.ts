import { post } from './request';
import type { AssetSummary, Holdings } from '../types';

export async function getAssetSummary() {
  return post<AssetSummary>('/assets/summary', {});
}

export async function getHoldings() {
  return post<Holdings>('/assets/holdings', {});
}
