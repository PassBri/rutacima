const { chromium } = require('playwright'); const fs = require('fs');
(async () => {
  const D = process.argv[2], FPS = 30, DESDE = Number(process.argv[3] || 0), HASTA = Number(process.argv[4] || 1e9);
  const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: 1280, height: 720 } });
  const errs = []; p.on('pageerror', e => errs.push(e.message));
  await p.addInitScript({ path: D + '/config.js' });
  await p.goto('file://' + D + '/escena.html'); await p.waitForTimeout(800);
  const fin = await p.evaluate(() => ESCENAS.fin); fs.writeFileSync(D + '/voz.json', JSON.stringify(await p.evaluate(() => ESCENAS.voz)));
  fs.mkdirSync(D + '/cuadros', { recursive: true });
  const n = Math.round(fin * FPS);
  for (let i = Math.round(DESDE * FPS); i < Math.min(n, HASTA * FPS); i++) {
    await p.evaluate(t => render(t), i / FPS);
    await p.screenshot({ path: `${D}/cuadros/f${String(i).padStart(5, '0')}.jpg`, type: 'jpeg', quality: 92 });
  }
  console.log('cuadros', n, errs); await b.close();
})();
