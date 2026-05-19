import {api} from './client'

export interface Claim {
    id: number;
    status: string;
    eligible: boolean;
    compensationAmount: number;
    user: {
        id: number;
        fullName: string;
        email: string;
    };
    flight:{
        flightNumber: string;
        flightDate: string;
        routeFrom: string;
        routeTo: string;
        airline: string;
        bookingReference: string;
        distance: number;
    };
    issue: {
        type: string;
        delayMinutes: number;
        daysSinceDelivery: number;
        extraordinaryCircumstances: boolean;
    };
    documents: {
        id: string;
        type: string;
        url: string;
    }[];
}

export interface ClaimPage {
    content: Claim[];
    totalElements: number;
    totalPages: number;
}
export interface Letter {
    subject: string;
    body: string;
}

export interface ClaimEvent {
    id: number;
    type: string;
    payload: string;
    createdAt: string;
}

export function getClaimEvents(id: number): Promise<ClaimEvent[]> {
    return api(`/claims/${id}/events`)
}

export function getClaims(page = 0, size = 20): Promise<ClaimPage> {
    return api(`/claims?page=${page}&size=${size}`)
}

export function getClaimById(id: number): Promise<Claim> {
    return api(`/claims/${id}`)
}
export function getClaimLetter(id: number): Promise<Letter> {
    return api(`/claims/${id}/letter`)
}
export function transitionClaim(id: number, status: string,  note: string){
    return api(`/claims/${id}/transition`, {
        method: 'POST',
        body: JSON.stringify({status, note})
    })
}
export function createClaim(data: unknown): Promise<Claim> {
    return api('/claims', {
        method: 'POST',
        body: JSON.stringify(data)
    })
}