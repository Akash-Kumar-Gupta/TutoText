export interface LiveSessionToken {
  token: string;
  expiresAt: string;
  model: string;
}

export type LiveConnectionStatus =
  | 'idle'
  | 'requesting-microphone'
  | 'connecting'
  | 'recording'
  | 'paused'
  | 'stopping'
  | 'error';
