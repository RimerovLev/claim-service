import {api, setToken, clearToken} from "./client.ts";

export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    email: string;
    password: string;
    fullName: string;
}

export interface AuthResponse {
    token: string;
}

export async function login(req: LoginRequest): Promise<AuthResponse> {
    const res = await api<AuthResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify(req)
    })
    setToken(res.token)
    return res;
}

export async function register(req: RegisterRequest): Promise<AuthResponse> {
    const res = await api<AuthResponse>('/auth/register', {
        method: 'POST',
        body: JSON.stringify(req)
    })
    setToken(res.token)
    return res;
}

export function logout(): void{
    clearToken();
    window.location.href = '/login';
}