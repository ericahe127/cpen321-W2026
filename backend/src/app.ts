import express, { type Express, type Request } from 'express';
import os from 'node:os';

const OWNER_NAME = {
  firstName: 'Erica',
  lastName: 'He',
} as const;

function normalizeIpAddress(ipAddress: string): string {
  return ipAddress.trim().replace(/^::ffff:/, '');
}

function getServerIp(req: Request): string {
  const configuredIp = process.env.SERVER_PUBLIC_IP?.trim();
  if (configuredIp) {
    return normalizeIpAddress(configuredIp);
  }

  const forwardedHost = req.get('x-forwarded-host') ?? req.get('host');
  if (forwardedHost) {
    const host = (forwardedHost.split(',')[0] ?? forwardedHost).trim();
    return normalizeIpAddress(host.replace(/:\d+$/, ''));
  }

  for (const interfaces of Object.values(os.networkInterfaces())) {
    for (const networkInterface of interfaces ?? []) {
      if (networkInterface.family === 'IPv4' && !networkInterface.internal) {
        return normalizeIpAddress(networkInterface.address);
      }
    }
  }

  return normalizeIpAddress(req.socket.localAddress ?? 'unknown');
}

function getClientIp(req: Request): string {
  const forwardedFor = req.get('x-forwarded-for');
  if (forwardedFor) {
    return normalizeIpAddress((forwardedFor.split(',')[0] ?? forwardedFor).trim());
  }

  return normalizeIpAddress(req.ip ?? req.socket.remoteAddress ?? 'unknown');
}

function getServerTime(): { localTime: string; gmtOffset: string } {
  const now = new Date();
  const offsetMinutes = -now.getTimezoneOffset();
  const offsetSign = offsetMinutes >= 0 ? '+' : '-';
  const absoluteOffsetMinutes = Math.abs(offsetMinutes);
  const offsetHours = Math.floor(absoluteOffsetMinutes / 60)
    .toString()
    .padStart(2, '0');
  const offsetRemainderMinutes = (absoluteOffsetMinutes % 60)
    .toString()
    .padStart(2, '0');
  const offset = `${offsetSign}${offsetHours}:${offsetRemainderMinutes}`;

  const dateParts = [
    now.getFullYear().toString().padStart(4, '0'),
    (now.getMonth() + 1).toString().padStart(2, '0'),
    now.getDate().toString().padStart(2, '0'),
  ];
  const timeParts = [
    now.getHours().toString().padStart(2, '0'),
    now.getMinutes().toString().padStart(2, '0'),
    now.getSeconds().toString().padStart(2, '0'),
  ];

  return {
    localTime: `${dateParts.join('-')}T${timeParts.join(':')}${offset}`,
    gmtOffset: `GMT${offset}`,
  };
}

export function createApp(): Express {
  const app = express();
  app.set('trust proxy', true);

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.get('/api/name', (_req, res) => {
    res.json(OWNER_NAME);
  });

  app.get('/api/server-ip', (req, res) => {
    res.json({ ipAddress: getServerIp(req) });
  });

  app.get('/api/server-time', (_req, res) => {
    res.json(getServerTime());
  });

  app.get('/api/client-ip', (req, res) => {
    res.json({ ipAddress: getClientIp(req) });
  });

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
