-- V23: Add TRIAL_EXPIRED status to subscription_status enum

ALTER TYPE subscription_status ADD VALUE IF NOT EXISTS 'TRIAL_EXPIRED';
