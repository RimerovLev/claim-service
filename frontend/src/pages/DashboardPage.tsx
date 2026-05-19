export default function DashboardPage() {
    return (
        <>
            <div className="section-header">
                <div>
                    <h1 className="section-title">Dashboard</h1>
                    <p className="section-subtitle">Overview of all claims activity</p>
                </div>
                <button className="refresh-btn" title="Refresh data">⟳</button>
            </div>

            <div className="stats-row">
                <div className="stat-card">
                    <span className="stat-label">Total Claims</span>
                    <span className="stat-value">1,247</span>
                    <span className="stat-sub">+12 this week</span>
                </div>
                <div className="stat-card">
                    <span className="stat-label">Eligible</span>
                    <span className="stat-value accent">892</span>
                    <span className="stat-sub">71.5% of total</span>
                </div>
                <div className="stat-card">
                    <span className="stat-label">Avg Compensation</span>
                    <span className="stat-value">€438</span>
                    <span className="stat-sub">Across all eligible claims</span>
                </div>
                <div className="stat-card">
                    <span className="stat-label">Submitted</span>
                    <span className="stat-value">356</span>
                    <span className="stat-sub">28.5% submission rate</span>
                </div>
            </div>

            <div className="section-block">
                <div style={{padding:'16px 20px', borderBottom:'1px solid var(--border)'}}>
                    <span style={{fontWeight:600,fontSize:'0.9rem'}}>Recent Claims</span>
                    <span style={{fontSize:'0.75rem',color:'var(--text-tertiary)',marginLeft:'8px'}}>Last 3</span>
                </div>
                <div className="table-wrapper">
                    <table>
                        <thead>
                        <tr>
                            <th>ID</th>
                            <th>Passenger</th>
                            <th>Flight</th>
                            <th>Issue Type</th>
                            <th>Status</th>
                            <th>Compensation</th>
                            <th>Date</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr>
                            <td className="cell-id">#CL-1247</td>
                            <td>Emma Wilson</td>
                            <td className="cell-mono">BA2492</td>
                            <td>Delay</td>
                            <td><span className="badge badge-green"><span className="badge-dot green"></span>APPROVED</span></td>
                            <td className="cell-mono">€600</td>
                            <td className="cell-secondary">Jan 22, 2026</td>
                        </tr>
                        <tr>
                            <td className="cell-id">#CL-1246</td>
                            <td>James Müller</td>
                            <td className="cell-mono">LH1044</td>
                            <td>Cancellation</td>
                            <td><span className="badge badge-indigo"><span className="badge-dot indigo"></span>SUBMITTED</span></td>
                            <td className="cell-mono">€400</td>
                            <td className="cell-secondary">Jan 20, 2026</td>
                        </tr>
                        <tr>
                            <td className="cell-id">#CL-1245</td>
                            <td>Sofia Ricci</td>
                            <td className="cell-mono">AF1782</td>
                            <td>Baggage Lost</td>
                            <td><span className="badge badge-yellow"><span className="badge-dot yellow"></span>DOCS_REQUESTED</span></td>
                            <td className="cell-mono">€600</td>
                            <td className="cell-secondary">Jan 18, 2026</td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </>
    )
}