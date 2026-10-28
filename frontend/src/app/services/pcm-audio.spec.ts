import { floatToPcm16, pcm16BufferToBase64 } from './pcm-audio';

describe('PCM audio helpers', () => {
  it('converts float samples to clipped signed 16-bit PCM', () => {
    expect(Array.from(floatToPcm16(new Float32Array([-1, -0.5, 0, 0.5, 1, 2])))).toEqual([
      -32768,
      -16384,
      0,
      16384,
      32767,
      32767,
    ]);
  });

  it('encodes a PCM buffer as base64', () => {
    const bytes = new Uint8Array([0, 1, 127, 255]);
    expect(pcm16BufferToBase64(bytes.buffer)).toBe('AAF//w==');
  });
});
