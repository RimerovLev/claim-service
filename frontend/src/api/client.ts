const BASE = '/api';
const TOKEN_KEY = 'jwt';

export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
    localStorage.removeItem(TOKEN_KEY);
}

export interface ApiError extends Error {
    status: number;
}

export async function api<T>(
    path: string,
    options: RequestInit = {}
): Promise<T> {
    const token = getToken();
    const headers: Record<string, string> = {
        ...(options.headers as Record<string, string> ?? {}),
    };

    if (options.body && !(options.body instanceof FormData)) {
        headers['Content-Type'] = headers['Content-Type'] ?? 'application/json';
    }
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const res = await fetch(`${BASE}${path}`, { ...options, headers });

    if (res.status === 401) {
        clearToken();
        window.location.href = '/login';
        const err = new Error('Unauthorized') as ApiError;
        err.status = 401;
        throw err;
    }
    if (!res.ok) {
        const text = await res.text().catch(() => '');
        const err = new Error(text || `HTTP ${res.status}`) as ApiError;
        err.status = res.status;
        throw err;
    }
    if (res.status === 204) {
        return undefined as T;
    }
    return res.json();
}