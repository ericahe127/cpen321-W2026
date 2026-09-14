import { createApp } from './app';
import { env } from './config/env';
import { attachPixelRelay } from './websocket/pixelRelay';

const app = createApp();

const server = app.listen(env.port, () => {
  console.log(`Server listening on port ${env.port}`);
});
const shutdownPixelRelay = attachPixelRelay(server);

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    shutdownPixelRelay();
    server.close(() => {
      process.exit(0);
    });
  });
}
