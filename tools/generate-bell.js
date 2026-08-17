const fs = require('fs');

const SR = 44100;
const DURATION = 30;
const F0 = 392;
const FADE = 2.0;

// Measured singing bowls sit close to these ratios, and the higher a partial is
// the faster it dies away — which is what makes a bowl brighten at the strike
// and settle into a hum.
const PARTIALS = [
  { ratio: 1.00, amp: 1.00, tau: 6.00, beat: 0.5 },
  { ratio: 2.71, amp: 0.50, tau: 3.60, beat: 0.8 },
  { ratio: 5.18, amp: 0.26, tau: 1.90, beat: 1.2 },
  { ratio: 8.16, amp: 0.12, tau: 1.00, beat: 1.6 },
  { ratio: 12.3, amp: 0.06, tau: 0.50, beat: 2.0 },
  { ratio: 16.9, amp: 0.03, tau: 0.22, beat: 2.4 },
];

const n = Math.floor(SR * DURATION);
const buf = new Float64Array(n);

PARTIALS.forEach((p, idx) => {
  const f = F0 * p.ratio;
  const w1 = 2 * Math.PI * (f - p.beat / 2) / SR;
  const w2 = 2 * Math.PI * (f + p.beat / 2) / SR;
  const phase = idx * 0.7;
  for (let i = 0; i < n; i++) {
    const t = i / SR;
    const env = (1 - Math.exp(-t / 0.004)) * Math.exp(-t / p.tau);
    buf[i] += p.amp * env * (Math.sin(w1 * i + phase) + Math.sin(w2 * i + phase)) * 0.5;
  }
});

let peak = 0;
for (let i = 0; i < n; i++) peak = Math.max(peak, Math.abs(buf[i]));
const gain = 0.89 / peak;

const rms = (from, to) => {
  let sum = 0;
  for (let i = from; i < to; i++) sum += (buf[i] * gain) ** 2;
  return Math.sqrt(sum / (to - from));
};

const fadeStart = Math.floor(n - SR * FADE);
const db = v => (20 * Math.log10(Math.max(v, 1e-12))).toFixed(1);
console.log('level at 10s     ', db(rms(SR * 10, SR * 10.5)), 'dB');
console.log('level at 20s     ', db(rms(SR * 20, SR * 20.5)), 'dB');
console.log('level where the fade starts', db(rms(fadeStart, fadeStart + SR * 0.5)), 'dB');

for (let i = fadeStart; i < n; i++) {
  const x = (i - fadeStart) / (n - fadeStart);
  buf[i] *= (1 - x) * (1 - x); // squared, so the fade itself is inaudible
}

const pcm = Buffer.alloc(n * 2);
for (let i = 0; i < n; i++) pcm.writeInt16LE(Math.round(buf[i] * gain * 32767), i * 2);

const head = Buffer.alloc(44);
head.write('RIFF', 0);
head.writeUInt32LE(36 + pcm.length, 4);
head.write('WAVE', 8);
head.write('fmt ', 12);
head.writeUInt32LE(16, 16);
head.writeUInt16LE(1, 20);
head.writeUInt16LE(1, 22);
head.writeUInt32LE(SR, 24);
head.writeUInt32LE(SR * 2, 28);
head.writeUInt16LE(2, 32);
head.writeUInt16LE(16, 34);
head.write('data', 36);
head.writeUInt32LE(pcm.length, 40);

fs.writeFileSync(process.argv[2], Buffer.concat([head, pcm]));
console.log('written', process.argv[2], DURATION + 's', F0 + 'Hz');
