export const environment = {
  production: true,
  apiUrl: '/api/v1',
  wsUrl: `${typeof window !== 'undefined' && window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${typeof window !== 'undefined' ? window.location.host : ''}/ws`,
};
