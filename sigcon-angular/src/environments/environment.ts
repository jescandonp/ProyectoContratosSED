import type { Configuration } from '@azure/msal-browser';

interface Environment {
  production: boolean;
  apiUrl: string;
  useDevSession: boolean;
  msalConfig: Configuration | null;
  apiScopes: string[];
}

export const environment: Environment = {
  production: true,
  apiUrl: '/api',
  useDevSession: false,
  msalConfig: null,
  apiScopes: []
};
