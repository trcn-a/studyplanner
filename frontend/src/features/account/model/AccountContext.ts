import { createContext, useContext } from 'react';
import type { Account } from '../api/accountApi';
export const AccountContext = createContext<{ account: Account; update: (account: Account) => void; logout: () => Promise<void> } | null>(null);
export function useAccount() { const value = useContext(AccountContext); if (!value) throw new Error('Account context is required'); return value; }
