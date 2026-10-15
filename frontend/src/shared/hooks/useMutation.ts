import { useCallback, useRef, useState } from 'react';
export function useMutation() {
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const pending = useRef(false);
    const reportError = useCallback((error: unknown) => setError(error instanceof Error ? error.message : 'Сталася помилка'), []);
    async function mutate(action: () => Promise<void>) {
        if (pending.current)
            return false;
        pending.current = true;
        setBusy(true);
        setError('');
        try {
            await action();
            return true;
        }
        catch (error) {
            reportError(error);
            return false;
        }
        finally {
            pending.current = false;
            setBusy(false);
        }
    }
    return { busy, error, setError, reportError, mutate };
}
