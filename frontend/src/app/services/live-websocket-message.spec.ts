import { isLiveSetupCompleteMessage, normalizeLiveSocketMessage } from './live-websocket-message';

describe('Live WebSocket message helpers', () => {
  it('uses string messages directly and recognizes setupComplete', async () => {
    const message = '{"setupComplete":{}}';

    await expect(normalizeLiveSocketMessage(message)).resolves.toBe(message);
    expect(isLiveSetupCompleteMessage(message)).toBe(true);
  });

  it('converts Blob messages to text and recognizes setupComplete', async () => {
    const message = '{"setupComplete":{}}';
    const normalized = await normalizeLiveSocketMessage(new Blob([message], { type: 'application/json' }));

    expect(normalized).toBe(message);
    if (typeof normalized === 'string') {
      expect(isLiveSetupCompleteMessage(normalized)).toBe(true);
    }
  });

  it('rejects malformed setup JSON with the existing safe error', () => {
    expect(() => isLiveSetupCompleteMessage('{malformed')).toThrow(
      'Gemini Live API returned an unreadable setup response.',
    );
  });

  it('returns null for unsupported message data without throwing', async () => {
    await expect(normalizeLiveSocketMessage(new ArrayBuffer(4))).resolves.toBeNull();
    await expect(normalizeLiveSocketMessage({})).resolves.toBeNull();
  });
});
