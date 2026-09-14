import type { Server } from 'node:http';
import WebSocket, { WebSocketServer } from 'ws';

const COURSE_PIXEL_WEBSOCKET_URL = 'wss://8.229.22.124';
const PIXEL_RELAY_PATH = '/ws/pixels';
const RECONNECT_DELAY_MS = 2_000;

export function attachPixelRelay(server: Server): () => void {
  const clientServer = new WebSocketServer({
    server,
    path: PIXEL_RELAY_PATH,
  });

  let courseSocket: WebSocket | null = null;
  let reconnectTimer: NodeJS.Timeout | null = null;
  let isShuttingDown = false;
  let relayedMessageCount = 0;

  function broadcast(message: string): void {
    for (const client of clientServer.clients) {
      if (client.readyState === WebSocket.OPEN) {
        client.send(message);
      }
    }
  }

  function scheduleReconnect(): void {
    if (isShuttingDown || reconnectTimer !== null) {
      return;
    }

    console.log(`Reconnecting to course pixel WebSocket in ${RECONNECT_DELAY_MS}ms`);
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null;
      connectToCourseSocket();
    }, RECONNECT_DELAY_MS);
  }

  function connectToCourseSocket(): void {
    courseSocket = new WebSocket(COURSE_PIXEL_WEBSOCKET_URL, {
      rejectUnauthorized: false,
    });

    courseSocket.on('open', () => {
      console.log(`Connected to course pixel WebSocket: ${COURSE_PIXEL_WEBSOCKET_URL}`);
    });

    courseSocket.on('message', (message) => {
      const pixelMessage = message.toString();
      relayedMessageCount += 1;
      if (relayedMessageCount <= 5 || relayedMessageCount % 100 === 0) {
        console.log(
          `Relayed pixel message #${relayedMessageCount} to ${clientServer.clients.size} client(s): ${pixelMessage}`
        );
      }
      broadcast(pixelMessage);
    });

    courseSocket.on('close', (code, reason) => {
      console.log(
        `Course pixel WebSocket closed: ${code} ${reason.toString()}`
      );
      courseSocket = null;
      scheduleReconnect();
    });

    courseSocket.on('error', (error) => {
      console.error('Course pixel WebSocket error:', error.message);
      courseSocket?.close();
    });
  }

  clientServer.on('connection', (client) => {
    console.log(`Android pixel client connected on ${PIXEL_RELAY_PATH}`);
    client.on('close', () => {
      console.log('Android pixel client disconnected');
    });
  });

  connectToCourseSocket();

  return () => {
    isShuttingDown = true;
    if (reconnectTimer !== null) {
      clearTimeout(reconnectTimer);
    }
    courseSocket?.close();
    clientServer.close();
  };
}
