// Offscreen document: exists only to play a short chime, then closes itself.
chrome.runtime.onMessage.addListener((msg) => {
  if (msg?.type !== 'play-chime') return;
  const audio = new Audio(chrome.runtime.getURL('chime.wav'));
  audio.volume = 0.6;
  audio.addEventListener('ended', () => setTimeout(() => window.close(), 500));
  audio.play().catch(() => window.close());
});
