class Pcm16CaptureProcessor extends AudioWorkletProcessor {
  constructor() {
    super();
    this.samplesPerChunk = 1600;
    this.samples = new Float32Array(this.samplesPerChunk);
    this.samplesWritten = 0;
  }

  process(inputs) {
    const channels = inputs[0];
    if (!channels || channels.length === 0) return true;

    const frameCount = channels[0].length;
    for (let frame = 0; frame < frameCount; frame += 1) {
      let sample = 0;
      for (let channel = 0; channel < channels.length; channel += 1) {
        sample += channels[channel][frame] || 0;
      }
      this.samples[this.samplesWritten] = sample / channels.length;
      this.samplesWritten += 1;

      if (this.samplesWritten === this.samplesPerChunk) {
        const pcm = new Int16Array(this.samplesPerChunk);
        for (let index = 0; index < this.samplesPerChunk; index += 1) {
          const clipped = Math.max(-1, Math.min(1, this.samples[index]));
          pcm[index] = clipped < 0 ? Math.round(clipped * 0x8000) : Math.round(clipped * 0x7fff);
        }
        this.port.postMessage(pcm.buffer, [pcm.buffer]);
        this.samplesWritten = 0;
      }
    }

    return true;
  }
}

registerProcessor('pcm16-capture', Pcm16CaptureProcessor);
