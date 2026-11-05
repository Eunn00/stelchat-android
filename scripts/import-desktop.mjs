// Explicit, reproducible copy. Never copies Electron main/preload, secrets or settings.
import fs from 'node:fs/promises';
import path from 'node:path';
const source=path.resolve('../stelchatapp');
await fs.mkdir('public/desktop/assets',{recursive:true});
for(const file of ['renderer.js','styles.css','emoji-renderer.js','realtime-merge-policy.js','renderer-state-policy.js','THIRD_PARTY_NOTICES.md'])
  await fs.copyFile(path.join(source,file),path.join('public/desktop',file));
const renderer=await fs.readFile('public/desktop/renderer.js','utf8');
await fs.writeFile('public/desktop/renderer.js',renderer.replaceAll('Windows','시스템').replace(
 'function renderNotificationSettings() {',
 'function renderNotificationSettings() { if (window.renderMobileNotifications) { window.renderMobileNotifications(state.streamers, state.settings); return; }'
));
for(const file of ['favicon.svg','icon.png']) await fs.copyFile(path.join(source,'assets',file),path.join('public/desktop/assets',file));
let html=await fs.readFile(path.join(source,'index.html'),'utf8');
html=html.replace(/<script src="[^"]+"><\/script>/g,'')
 .replace(/content="default-src[^\"]*"/,`content="default-src 'self'; connect-src 'self' https://stelchat.xyz; img-src 'self' https://stelchat.xyz data:; style-src 'self' 'unsafe-inline'; script-src 'self'"`)
 .replace('href="styles.css"','href="/desktop/styles.css"')
 .replace('src="assets/favicon.svg"','src="/desktop/assets/favicon.svg"')
 .replaceAll('Windows','시스템').replaceAll('이 PC','이 기기')
 .replace('</head>','<link rel="stylesheet" href="/src/mobile.css" /></head>')
 .replace('</body>','<script type="module" src="/src/main.js"></script></body>');
await fs.writeFile('index.html',html);
console.log('Imported desktop presentation only. Android bridge remains separate.');
