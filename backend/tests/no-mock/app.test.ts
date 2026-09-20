import request from 'supertest';

import { createApp } from '../../src/app';

// Interface GET /health
describe('Unmocked: GET /health', () => {
  // Input: GET request to /health
  // Expected status code: 200
  // Expected behavior: service reports healthy status
  // Expected output: { status: "ok" }
  test('Healthy service', async () => {
    const response = await request(createApp()).get('/health');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ status: 'ok' });
  });
});

// Interface GET /does-not-exist
describe('Unmocked: GET /does-not-exist', () => {
  // Input: GET request to an unregistered route
  // Expected status code: 404
  // Expected behavior: request is rejected; no state is changed
  // Expected output: { error: "Not Found" }
  test('Unregistered route', async () => {
    const response = await request(createApp()).get('/does-not-exist');

    expect(response.status).toBe(404);
    expect(response.body).toEqual({ error: 'Not Found' });
  });
});

// Interface GET /api/name
describe('Unmocked: GET /api/name', () => {
  // Input: GET request to /api/name
  // Expected status code: 200
  // Expected behavior: returns the project owner's name from the backend
  // Expected output: { firstName: "Erica", lastName: "He" }
  test('Owner name', async () => {
    const response = await request(createApp()).get('/api/name');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ firstName: 'Erica', lastName: 'He' });
  });
});

// Interface GET /api/server-time
describe('Unmocked: GET /api/server-time', () => {
  // Input: GET request to /api/server-time
  // Expected status code: 200
  // Expected behavior: returns local server time and GMT offset
  // Expected output: ISO-like localTime plus GMT offset string
  test('Server time', async () => {
    const response = await request(createApp()).get('/api/server-time');

    expect(response.status).toBe(200);
    expect(response.body.localTime).toMatch(
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}$/
    );
    expect(response.body.gmtOffset).toMatch(/^GMT[+-]\d{2}:\d{2}$/);
  });
});

// Interface GET /api/server-ip
describe('Unmocked: GET /api/server-ip', () => {
  // Input: GET request to /api/server-ip with Host header
  // Expected status code: 200
  // Expected behavior: returns the public host/IP the server believes represents it
  // Expected output: { ipAddress: "203.0.113.10" }
  test('Server IP', async () => {
    const response = await request(createApp())
      .get('/api/server-ip')
      .set('Host', '203.0.113.10:3000');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ ipAddress: '203.0.113.10' });
  });
});

// Interface GET /api/client-ip
describe('Unmocked: GET /api/client-ip', () => {
  // Input: GET request to /api/client-ip through a proxy
  // Expected status code: 200
  // Expected behavior: returns the original client IP from proxy headers
  // Expected output: { ipAddress: "198.51.100.7" }
  test('Client IP', async () => {
    const response = await request(createApp())
      .get('/api/client-ip')
      .set('X-Forwarded-For', '198.51.100.7, 10.0.0.2');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ ipAddress: '198.51.100.7' });
  });
});

// Interface POST /api/auth/google
describe('Unmocked: POST /api/auth/google', () => {
  const originalGoogleClientId = process.env.GOOGLE_CLIENT_ID;

  afterEach(() => {
    if (originalGoogleClientId === undefined) {
      delete process.env.GOOGLE_CLIENT_ID;
    } else {
      process.env.GOOGLE_CLIENT_ID = originalGoogleClientId;
    }
  });

  // Input: POST request without a Google ID token
  // Expected status code: 400
  // Expected behavior: request is rejected before any token verification
  // Expected output: { error: "Missing Google ID token" }
  test('Rejects missing Google ID token', async () => {
    const response = await request(createApp()).post('/api/auth/google').send({});

    expect(response.status).toBe(400);
    expect(response.body).toEqual({ error: 'Missing Google ID token' });
  });

  // Input: POST request with a token while backend GOOGLE_CLIENT_ID is unset
  // Expected status code: 500
  // Expected behavior: backend reports deployment configuration problem
  // Expected output: { error: "GOOGLE_CLIENT_ID is not configured on the backend" }
  test('Reports missing backend Google client ID', async () => {
    delete process.env.GOOGLE_CLIENT_ID;

    const response = await request(createApp())
      .post('/api/auth/google')
      .send({ idToken: 'not-a-real-token' });

    expect(response.status).toBe(500);
    expect(response.body).toEqual({
      error: 'GOOGLE_CLIENT_ID is not configured on the backend',
    });
  });
});
