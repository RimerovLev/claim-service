import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login} from "../api/auth.ts";

export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();


    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            await login({ email, password });
            navigate('/', { replace: true });
        } catch (err) {
            setError((err as Error).message ||'Invalid email or password');
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="auth-container">
        <form onSubmit={handleSubmit} className="auth-form">
            <h1>Sign in</h1>
            {error && <div className="error-message">{error}</div>}
            <label>
                Email
                <input type="email" value={email}
                       onChange={e => setEmail(e.target.value)}
                       required autoComplete="email" />
            </label>
            <label>
                Password
                <input type="password" value={password}
                       onChange={e => setPassword(e.target.value)}
                       required autoComplete="current-password" />
            </label>
            <button type="submit" disabled={loading}>
                {loading ? 'Signing in...' : 'Sign in'}
            </button>
            <p>No account? <Link to="/register">Register</Link></p>
        </form>
    </div>
    );
}