import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '../api/auth';

export default function RegisterPage() {
    const [fullName, setFullName] = useState('');
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
            await register({ fullName, email, password });
            navigate('/', { replace: true });
        } catch (err) {
            setError((err as Error).message || 'Registration failed');
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="auth-container">
            <form onSubmit={handleSubmit} className="auth-form">
                <h1>Create account</h1>
                {error && <div className="error-message">{error}</div>}
                <label>
                    Full name
                    <input type="text" value={fullName}
                           onChange={e => setFullName(e.target.value)}
                           required autoComplete="name" />
                </label>
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
                           required minLength={8}
                           autoComplete="new-password" />
                </label>
                <button type="submit" disabled={loading}>
                    {loading ? 'Creating...' : 'Create account'}
                </button>
                <p>Already have an account? <Link to="/login">Sign in</Link></p>
            </form>
        </div>
    );
}