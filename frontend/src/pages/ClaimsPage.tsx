
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {getClaims, getClaimLetter, type Claim, type Letter, transitionClaim} from '../api/claims'

const STATUS_BADGE: Record<string, string> = {
    NEW: 'badge-gray',
    DOCS_REQUESTED: 'badge-yellow',
    READY_TO_SUBMIT: 'badge-blue',
    SUBMITTED: 'badge-indigo',
    FOLLOW_UP_SENT: 'badge-purple',
    APPROVED: 'badge-green',
    REJECTED: 'badge-red',
    PAID: 'badge-emerald',
    CLOSED: 'badge-gray',
}

const STATUS_DOT: Record<string, string> = {
    NEW: 'gray', DOCS_REQUESTED: 'yellow', READY_TO_SUBMIT: 'blue',
    SUBMITTED: 'indigo', FOLLOW_UP_SENT: 'purple', APPROVED: 'green',
    REJECTED: 'red', PAID: 'emerald', CLOSED: 'gray',
}

export default function ClaimsPage() {
    const [claims, setClaims] = useState<Claim[]>([])
    const [selected, setSelected] = useState<Claim | null>(null)
    const [letter, setLetter] = useState<Letter | null>(null)
    const [loading, setLoading] = useState(true)
    const navigate = useNavigate()

    useEffect(() => {
        getClaims().then(p => {
            setClaims(p.content)
            setLoading(false)
        })
    }, [])

    function selectClaim(claim: Claim) {
        setSelected(claim)
        setLetter(null)
        getClaimLetter(claim.id).then(setLetter)
    }

    return (
        <>
            <div className="section-header">
                <div>
                    <h2 className="section-title">Claims</h2>
                    <p className="section-subtitle">Manage and review all compensation claims</p>
                </div>
                <button className="refresh-btn" onClick={() => getClaims().then(p => setClaims(p.content))}>⟳</button>
            </div>

            <div className="claims-layout">
                <div className="claims-table-area">
                    <div className="search-box">
                        <span className="search-icon">🔍</span>
                        <input type="text" placeholder="Search by flight number or status…" />
                    </div>
                    <div className="table-wrapper">
                        {loading ? <p style={{padding:'20px', color:'var(--text-tertiary)'}}>Loading...</p> : (
                            <table>
                                <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Passenger</th>
                                    <th>Flight</th>
                                    <th>Issue Type</th>
                                    <th>Status</th>
                                    <th>Compensation</th>
                                </tr>
                                </thead>
                                <tbody>
                                {claims.map(c => (
                                    <tr
                                        key={c.id}
                                        onClick={() => selectClaim(c)}
                                        className={selected?.id === c.id ? 'highlight-row' : ''}
                                    >
                                        <td className="cell-id">#{c.id}</td>
                                        <td>{c.user.fullName}</td>
                                        <td className="cell-mono">{c.flight.flightNumber}</td>
                                        <td>{c.issue.type}</td>
                                        <td>
                        <span className={`badge ${STATUS_BADGE[c.status] ?? 'badge-gray'}`}>
                          <span className={`badge-dot ${STATUS_DOT[c.status] ?? 'gray'}`}></span>
                            {c.status}
                        </span>
                                        </td>
                                        <td className="cell-mono">€{c.compensationAmount}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>

                {selected && (
                    <aside className="detail-panel">
                        <div className="detail-panel-header">
                            <span className="detail-panel-id">CLAIM #{selected.id}</span>
                            <button className="detail-panel-close" onClick={() => setSelected(null)}>✕</button>
                        </div>

                        <div>
                            <span className="detail-section-label">Passenger</span>
                            <div className="detail-info-row">
                                <span className="label">Name</span>
                                <span className="value">{selected.user.fullName}</span>
                            </div>
                            <div className="detail-info-row">
                                <span className="label">Email</span>
                                <span className="value text-secondary">{selected.user.email}</span>
                            </div>
                        </div>

                        <div>
                            <span className="detail-section-label">Flight</span>
                            <div className="detail-info-row">
                                <span className="label">Flight</span>
                                <span className="value cell-mono">{selected.flight.flightNumber}</span>
                            </div>
                            <div className="detail-info-row">
                                <span className="label">Route</span>
                                <span className="value">{selected.flight.routeFrom} → {selected.flight.routeTo}</span>
                            </div>
                            <div className="detail-info-row">
                                <span className="label">Date</span>
                                <span className="value">{selected.flight.flightDate}</span>
                            </div>
                            <div className="detail-info-row">
                                <span className="label">Airline</span>
                                <span className="value">{selected.flight.airline}</span>
                            </div>
                        </div>

                        <div className={`detail-eligibility ${selected.eligible ? 'eligible' : ''}`}>
                            <span className="eligibility-dot"></span>
                            {selected.eligible ? 'Eligible — Compensation Due' : 'Not Eligible'}
                        </div>
                        <div className="detail-compensation">€{selected.compensationAmount}</div>

                        <div>
                            <span className="detail-section-label">Claim Letter</span>
                            <div className="letter-preview-box">
                                {letter ? letter.body : 'Loading...'}
                            </div>
                        </div>

                        <div className="detail-actions">
                            {selected.status === 'READY_TO_SUBMIT' && (
                                <button
                                    className="btn btn-primary btn-full btn-lg"
                                    onClick={() =>
                                        transitionClaim(selected.id, 'SUBMITTED', 'submitted from UI')
                                            .then(updated => {
                                                setSelected(updated)
                                                setClaims(prev => prev.map(c => c.id === updated.id ? updated : c))
                                            }).catch(err => alert(`Error: ${(err as Error).message}`))
                                    }
                                >
                                    Submit to Airline →
                                </button>
                            )}
                            {selected.status === 'SUBMITTED' && (
                                <button
                                    className="btn btn-primary btn-full btn-lg"
                                    onClick={() =>
                                        transitionClaim(selected.id, 'FOLLOW_UP_SENT', 'follow-up from UI')
                                            .then(updated => {
                                                setSelected(updated)
                                                setClaims(prev => prev.map(c => c.id === updated.id ? updated : c))
                                            }).catch(err => alert(`Error: ${(err as Error).message}`))
                                    }
                                >
                                    Send Follow-up →
                                </button>
                            )}
                            <button
                                className="btn btn-secondary btn-full"
                                onClick={() => navigate(`/claims/${selected.id}`)}
                            >
                                View Full Details →
                            </button>

                        </div>
                    </aside>
                )}
            </div>
        </>
    )
}