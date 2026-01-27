export type UserRole = 'AGENCY_ADMIN' | 'AGENCY_USER' | 'CLIENT_USER';

export interface UserDto {
  id: string;
  fullName: string;
  email: string;
  role: UserRole;
  agencyId: string | null;
  companyId: string | null;
  companyCode: string | null;
  accessProfileId?: string | null;
  active: boolean;
  createdAt: string;
  lastLoginAt?: string | null;
  lastSeenAt?: string | null;
}
