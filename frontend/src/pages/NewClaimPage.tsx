export default function NewClaimPage() {
    return (
        <>
            <div className="section-header">
                <div>
                    <h2 className="section-title">New Claim</h2>
                    <p className="section-subtitle">Create a new compensation claim — Step 2 of 5</p>
                </div>
            </div>

            <div className="progress-stepper">
                <div className="progress-step completed">
                    <div className="progress-dot">1</div>
                    <span className="progress-label">Passenger</span>
                </div>
                <div className="progress-step active">
                    <div className="progress-dot">2</div>
                    <span className="progress-label">Flight</span>
                </div>
                <div className="progress-step">
                    <div className="progress-dot">3</div>
                    <span className="progress-label">Issue</span>
                </div>
                <div className="progress-step">
                    <div className="progress-dot">4</div>
                    <span className="progress-label">EU Context</span>
                </div>
                <div className="progress-step">
                    <div className="progress-dot">5</div>
                    <span className="progress-label">Documents</span>
                </div>
            </div>

            <div className="section-block" style={{padding:'24px 28px'}}>
        <span className="detail-section-label" style={{display:'block', marginBottom:'16px'}}>
          Flight Information
        </span>
                <div className="form-grid">
                    <div className="form-field">
                        <label className="form-label">Flight Number</label>
                        <input className="form-input" type="text" placeholder="e.g. BA2492" />
                    </div>
                    <div className="form-field">
                        <label className="form-label">Flight Date</label>
                        <input className="form-input" type="date" />
                    </div>
                    <div className="form-field">
                        <label className="form-label">Route From</label>
                        <input className="form-input" type="text" placeholder="Departure airport" />
                    </div>
                    <div className="form-field">
                        <label className="form-label">Route To</label>
                        <input className="form-input" type="text" placeholder="Arrival airport" />
                    </div>
                    <div className="form-field">
                        <label className="form-label">Airline</label>
                        <input className="form-input" type="text" placeholder="Airline name" />
                    </div>
                    <div className="form-field">
                        <label className="form-label">Booking Reference</label>
                        <input className="form-input" type="text" placeholder="PNR / Booking ref" />
                    </div>
                    <div className="form-field full-width">
                        <label className="form-label">Distance (km)</label>
                        <input className="form-input" type="number" placeholder="Flight distance in kilometers" />
                    </div>
                </div>
                <div className="form-actions">
                    <button className="btn btn-secondary">← Back</button>
                    <button className="btn btn-primary">Next →</button>
                </div>
            </div>
        </>
    )
}