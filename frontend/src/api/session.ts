import {getToken, clearToken} from "./client.ts";
export interface Session {
    email: string;
    role: 'USER' | 'MODERATOR' | 'ADMIN';
    exp: number;
}

interface JwtPayload {
    sub: string
    role: string
    exp: number
}

function decode(token: string): JwtPayload | null {
    try{
        const payloadB64 = token.split('.')[1];
        const base64 = payloadB64.replace(/-/g, '+')
            .replace(/_/g, '/');
        const json = atob(base64);
        return JSON.parse(json);
    }catch {
        return null;
    }
}

export function getSession(): Session | null{
    const token = getToken();
    if (!token) return null;
    const payload = decode(token);
    if (!payload){
        clearToken();
        return null;
    };
    if (payload.exp * 1000 < Date.now()){
        clearToken();
        return null;
    }
    return {
        email: payload.sub,
        role: payload.role as Session['role'],
        exp: payload.exp,
    }
}

export function isAuthenticated(): boolean{
    return getSession()!==null;
}

export function hasRole(...roles: Session['role'][]):boolean{
    const s = getSession();
    return s !==null && roles.includes(s.role   );
}