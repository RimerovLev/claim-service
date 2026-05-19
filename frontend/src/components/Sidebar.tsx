import { NavLink, useNavigate } from 'react-router-dom';
import { getSession, hasRole } from '../api/session';
import { logout } from '../api/auth';

export default function Sidebar() {
    const session = getSession();
    const navigate = useNavigate();

    function handleLogout() {
        logout();
        navigate('/login', { replace: true });
    }

    return (
        <aside className="sidebar">
            <div className="sidebar-logo">
                <div className="logo-icon">✦</div>
                ClaimsMVP
            </div>
            <nav className="sidebar-nav">
                <NavLink to="/" end className={({ isActive }) => isActive ? 'active' : ''}>
                    <span className="nav-dot"></span>Dashboard
                </NavLink>
                {hasRole('MODERTOR', 'ADMIN') && (
                    <NavLink to="/claims" className={({ isActive }) => isActive ? 'active' : ''}>
                        <span className="nav-dot"></span>Claims
                    </NavLink>
                )}
                <NavLink to="/claims/new" className={({ isActive }) => isActive ? 'active' : ''}>
                    <span className="nav-dot"></span>New Claim
                </NavLink>
                {hasRole('ADMIN') && (
                    <NavLink to="/admin/users" className={({ isActive }) => isActive ? 'active' : ''}>
                        <span className="nav-dot"></span>Admin
                    </NavLink>
                )}
            </nav>
            {session && (
                <div className="sidebar-user">
                    <div className="sidebar-user-email">{session.email}</div>
                    <div className="sidebar-user-role">{session.role}</div>
                    <button className="sidebar-logout" onClick={handleLogout}>
                        Logout
                    </button>
                </div>
            )}
            <div className="sidebar-footer">© 2026 ClaimsMVP</div>
        </aside>
    );
}