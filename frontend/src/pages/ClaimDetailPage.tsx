import {useNavigate, useParams} from "react-router-dom";
import {useEffect, useState} from "react";
import {
    type Claim,
    type ClaimEvent,
    getClaimById,
    getClaimEvents,
    getClaimLetter,
    type Letter,
    transitionClaim
} from "../api/claims.ts";
import {hasRole} from "../api/session.ts";

export default function ClaimDetailPage() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const claimId = Number(id);

    const [claim, setClaim] = useState<Claim | null>(null);
    const [events, setEvents] = useState<ClaimEvent[]>([]);
    const [letter, setLetter] = useState<Letter | null>(null);
    const [error, setError] = useState<string | null>(null);

    const isPrivileged = hasRole('MODERATOR', 'ADMIN');

    useEffect(() => {
        getClaimById(claimId).then(setClaim).catch(() => setError('Claim not found'));
        getClaimEvents(claimId).then(setEvents).catch(() => {});
        getClaimLetter(claimId).then(data => setLetter(data)).catch(() => {});
    }, [claimId]);

    function doTransition(status: string) {
        transitionClaim(claimId, status, '')
            .then(() => getClaimById(claimId).then((setClaim)))
            .catch(err => setError(err.message))
    }

    if (error) return <div className="section-block" style={{padding: 24}}>{error}</div>
    if (!claim) return <div className="section-block" style={{padding: 24}}>Loading...</div>

    return (
        <>
            <div className="section-header">
                <div>
                    <h2 className="section-title">Claim #{claim.id}</h2>
                    <p className="section-subtitle">
                        {claim.flight.airline} · {claim.flight.routeFrom} → {claim.flight.routeTo}
                    </p>
                </div>
                <button className="btn btn-secondary" onClick={() => navigate('/claims')}>← Back</button>
            </div>

            {/* Header info */}
            <div className="section-block" style={{padding: '16px 24px', display: 'flex', gap: 32}}>
                <div><span className="detail-section-label">Status</span><div>{claim.status}</div></div>
                <div><span className="detail-section-label">Eligible</span><div>{claim.eligible ? 'Yes' : 'No'}</div></div>
                <div><span className="detail-section-label">Compensation</span><div>€{claim.compensationAmount}</div></div>
                <div><span className="detail-section-label">Flight</span><div>{claim.flight.flightNumber} · {claim.flight.flightDate}</div></div>
            </div>

            {/* FSM buttons — only for MODERATOR/ADMIN */}
            {isPrivileged && (
                <div className="section-block" style={{padding: '12px 24px', display: 'flex', gap: 8}}>
                    <span className="detail-section-label" style={{alignSelf: 'center'}}>Transition:</span>
                    {['DOCS_REQUESTED','READY_TO_SUBMIT','SUBMITTED','FOLLOW_UP_SENT','APPROVED','REJECTED','PAID','CLOSED'].map(s => (
                        <button key={s} className="btn btn-secondary" style={{fontSize: 12}}
                                onClick={() => doTransition(s)}>
                            {s}
                        </button>
                    ))}
                </div>
            )}

            {/* Timeline */}
            <div className="section-block" style={{padding: '16px 24px'}}>
                <span className="detail-section-label" style={{display: 'block', marginBottom: 12}}>Timeline</span>
                {events.length === 0 && <div style={{color: 'var(--text-secondary)'}}>No events yet</div>}
                {events.map(e => (
                    <div key={e.id} style={{marginBottom: 8}}>
                        <span style={{fontWeight: 600}}>{e.type}</span>
                        <span style={{marginLeft: 12, color: 'var(--text-secondary)', fontSize: 13}}>
                            {new Date(e.createdAt).toLocaleString()}
                        </span>
                        {e.payload && (
                            <div style={{fontSize: 12, color: 'var(--text-secondary)', marginTop: 2}}>{e.payload}</div>
                        )}
                    </div>
                ))}
            </div>

            {/* Letter */}
            {letter && (
                <div className="section-block" style={{padding: '16px 24px'}}>
                    <span className="detail-section-label" style={{display: 'block', marginBottom: 8}}>Letter</span>
                    <div style={{fontWeight: 600, marginBottom: 8}}>{letter.subject}</div>
                    <pre style={{whiteSpace: 'pre-wrap', fontSize: 13}}>{letter.body}</pre>
                </div>
            )}
        </>
    )
}