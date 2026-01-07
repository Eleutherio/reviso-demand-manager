export type UserRole = 'AGENCY_ADMIN' | 'AGENCY_USER' | 'CLIENT_USER';

export function isAgencyRole(role: string | null | undefined): role is UserRole {
  return role === 'AGENCY_ADMIN' || role === 'AGENCY_USER';
}

export function isAdminRole(role: string | null | undefined): role is UserRole {
  return role === 'AGENCY_ADMIN';
}
