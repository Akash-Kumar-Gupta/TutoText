export function floatToPcm16(samples: Float32Array): Int16Array {
  const pcm = new Int16Array(samples.length);
  for (let index = 0; index < samples.length; index += 1) {
    const clipped = Math.max(-1, Math.min(1, samples[index]));
    pcm[index] = clipped < 0 ? Math.round(clipped * 0x8000) : Math.round(clipped * 0x7fff);
  }
  return pcm;
}

export function pcm16BufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let index = 0; index < bytes.length; index += 1) {
    binary += String.fromCharCode(bytes[index]);
  }
  return btoa(binary);
}
