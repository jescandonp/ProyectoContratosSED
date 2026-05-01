import type { Configuration } from '@azure/msal-browser';

interface Environment {
  production: boolean;
  apiUrl: string;
  useDevSession: boolean;
  msalConfig: Configuration | null;
  apiScopes: string[];
}

export const environment: Environment = {
  production: false,
  apiUrl: '/api',
  useDevSession: true,
  msalConfig: null,
  apiScopes: []
};
