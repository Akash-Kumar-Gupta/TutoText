export async function normalizeLiveSocketMessage(data: unknown): Promise<string | null> {
  if (typeof data === 'string') return data;
  if (typeof Blob !== 'undefined' && data instanceof Blob) return data.text();
  return null;
}

export function isLiveSetupCompleteMessage(message: string): boolean {
  let response: unknown;
  try {
    response = JSON.parse(message);
  } catch {
    throw new Error('Gemini Live API returned an unreadable setup response.');
  }
  if (!response || typeof response !== 'object') {
    throw new Error('Gemini Live API returned an unreadable setup response.');
  }
  const setupResponse = response as Record<string, unknown>;
  if (setupResponse['error']) {
    throw new Error('Gemini Live API rejected the session setup. Check API access and usage limits.');
  }
  return setupResponse['setupComplete'] !== undefined;
}
