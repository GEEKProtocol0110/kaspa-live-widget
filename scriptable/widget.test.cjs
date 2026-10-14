const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, 'Geek-Kaspa-Widget.js'), 'utf8');
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
const now = Date.UTC(2026, 9, 14, 7, 0);
const minute = 60000, hour = 60 * minute;
const names = ['settings', 'unsigned', 'counterDelta', 'parseQuote', 'parseNetwork', 'parseHash',
  'parseHistory', 'validHistory', 'validateCache', 'refresh', 'getJSON', 'loadCache', 'saveCache',
  'ageLabel', 'chartLabel', 'priceText', 'scoreText', 'bpsText', 'buildWidget', 'ENDPOINTS'];

// A deliberately bounded adapter for APIs actually documented by Scriptable.
// It verifies execution and rendering inputs, not native fonts/placement on iOS.
function adapter({ family = 'medium', parameter = '', inWidget = true, choice = 0,
  response = () => ({ status: 200, body: 'null' }), cache, failSave = false } = {}) {
  const state = { widgets: [], requests: [], previews: [], drawings: [], writes: [], complete: 0 };
  const files = new Map();
  if (cache) files.set('/documents/geek-kaspa-cache-v1.json', JSON.stringify(cache));
  class Color {
    constructor(hex, alpha = 1) {
      assert.match(hex, /^[0-9A-F]{6}$/); assert.ok(alpha >= 0 && alpha <= 1);
      this.hex = hex; this.alpha = alpha;
    }
  }
  class Size { constructor(w, h) { assert.ok(Number.isFinite(w) && Number.isFinite(h)); this.width = w; this.height = h; } }
  class Point { constructor(x, y) { assert.ok(Number.isFinite(x) && Number.isFinite(y)); this.x = x; this.y = y; } }
  class Rect extends Point { constructor(x, y, w, h) { super(x, y); this.width = w; this.height = h; } }
  class Path { constructor() { this.points = []; } move(p) { this.points.push(p); } addLine(p) { this.points.push(p); } }
  class DrawContext {
    constructor() { this.paths = []; state.drawings.push(this); }
    setFillColor(c) { assert.ok(c instanceof Color); }
    setStrokeColor(c) { assert.ok(c instanceof Color); }
    setLineWidth(w) { assert.ok(w > 0); }
    fillRect(r) { assert.ok(r instanceof Rect); }
    addPath(p) { assert.ok(p instanceof Path); this.paths.push(p); }
    strokePath() { assert.ok(this.paths.length); }
    getImage() { assert.ok(this.size instanceof Size); return { drawing: this }; }
  }
  class Element {
    constructor(type, value) { this.type = type; this.value = value; this.children = []; }
    addText(value) { assert.equal(typeof value, 'string'); const e = new Element('text', value); this.children.push(e); return e; }
    addDate(date) { assert.ok(date instanceof Date); const e = new Element('date', date); e.applyTimeStyle = () => { e.style = 'time'; }; this.children.push(e); return e; }
    addStack() { const e = new Element('stack'); this.children.push(e); return e; }
    addImage(value) { const e = new Element('image', value); this.children.push(e); return e; }
    addSpacer(value) { assert.ok(value === undefined || value >= 0); this.children.push(new Element('spacer', value)); }
    setPadding(...p) { assert.equal(p.length, 4); this.padding = p; }
    centerAlignContent() { this.align = 'center'; }
    layoutVertically() { this.direction = 'vertical'; }
    layoutHorizontally() { this.direction = 'horizontal'; }
  }
  class ListWidget extends Element {
    constructor() { super('widget'); }
    async presentSmall() { state.previews.push('small'); }
    async presentMedium() { state.previews.push('medium'); }
    async presentLarge() { state.previews.push('large'); }
  }
  class Request {
    constructor(url) { this.url = url; state.requests.push(this); assert.match(url, /^https:\/\//); }
    async loadString() {
      const r = response(this.url);
      if (r.error) throw new Error('offline');
      this.response = { statusCode: r.status }; return r.body;
    }
  }
  const fm = {
    documentsDirectory: () => '/documents', joinPath: (a, b) => a + '/' + b,
    readString: p => { if (!files.has(p)) throw new Error('missing'); return files.get(p); },
    writeString: (p, text) => { if (failSave) throw new Error('full'); files.set(p, text); state.writes.push(JSON.parse(text)); }
  };
  class Alert {
    addAction() {} addCancelAction() {}
    async presentSheet() { return choice; }
  }
  const api = {
    args: { widgetParameter: parameter }, config: { widgetFamily: family, runsInWidget: inWidget },
    Color, Size, Point, Rect, Path, DrawContext, ListWidget, Request, Alert,
    Font: { boldMonospacedSystemFont: size => ({ size, bold: true }), mediumSystemFont: size => ({ size }) },
    Data: { fromBase64String: s => Buffer.from(s, 'base64') },
    Image: { fromData: b => { assert.equal(b.subarray(0, 8).toString('hex'), '89504e470d0a1a0a'); return { png: b }; } },
    FileManager: { local: () => fm },
    Script: { setWidget: w => state.widgets.push(w), complete: () => state.complete++ }
  };
  return { state, api, fm };
}
async function functions(api = adapter().api) {
  assert.match(source, /\nawait run\(\);\s*$/);
  const definitions = source.replace(/\nawait run\(\);\s*$/, '');
  return new AsyncFunction(...Object.keys(api), definitions + '\nreturn {' + names.join(',') + '};')(...Object.values(api));
}
async function execute(a) { await new AsyncFunction(...Object.keys(a.api), source)(...Object.values(a.api)); return a.state; }
function history(at = now, flat = false) {
  return { prices: Array.from({ length: 97 }, (_, i) => [at - 24 * hour + i * 15 * minute, flat ? 0.04 : 0.04 + Math.sin(i / 7) * 0.002]) };
}
function quote(at = now) { return { kaspa: { usd: 0.0391, usd_24h_change: -1.23, last_updated_at: at / 1000 } }; }
function dag(count = '600000000', score = '565321249') { return { networkName: 'kaspa-mainnet', blockCount: count, virtualDaaScore: score }; }
async function snapshot(at = now) {
  const f = await functions();
  return { version: 1, quote: f.parseQuote(quote(at), at, false), network: f.parseNetwork(dag(), null, at),
    hash: f.parseHash({ hashrate: 342330 }, at), history: f.parseHistory(history(at), at), historyFetched: at };
}
function texts(element) { return [element.type === 'text' ? element.value : '', ...element.children.flatMap(texts)].filter(Boolean); }

test('uint64 decimal bounds preserve values beyond Number precision', async () => {
  const f = await functions();
  assert.equal(f.unsigned('18446744073709551615'), '18446744073709551615');
  assert.equal(f.unsigned('00000000000000000001'), '1');
  for (const v of ['18446744073709551616', '-1', '1e3', '1.0', '', ' 12', '1'.repeat(21), 9007199254740992, NaN, true]) assert.equal(f.unsigned(v), null);
  assert.equal(f.counterDelta('18446744073709551615', '18446744073709551015'), 600);
  assert.equal(f.counterDelta('10000', '9999'), 1);
  assert.equal(f.counterDelta('9999', '10000'), null);
  assert.equal(f.counterDelta('18446744073709551615', '1'), null);
});
test('only mainnet aliases and positive exact counters are accepted', async () => {
  const f = await functions();
  for (const name of ['mainnet', 'kaspa-mainnet']) assert.ok(f.parseNetwork({ ...dag(), networkName: name }, null, now));
  assert.equal(f.parseNetwork({ ...dag(), networkName: 'kaspa-testnet' }, null, now), null);
  assert.equal(f.parseNetwork(dag('0'), null, now), null);
  assert.equal(f.parseNetwork(dag('1', '9007199254740993'), null, now).score, '9007199254740993');
  assert.equal(f.parseNetwork(dag(9007199254740993), null, now), null);
});
test('BPS averages exact differences and retains the baseline across rapid runs', async () => {
  const f = await functions(), first = f.parseNetwork(dag('18446744073709550000'), null, now);
  const fast = f.parseNetwork(dag('18446744073709550150'), first, now + 15000);
  assert.equal(fast.bps, null); assert.equal(fast.sample.time, now);
  const next = f.parseNetwork(dag('18446744073709550600'), fast, now + 60000);
  assert.equal(next.bps, 10); assert.equal(next.sample.time, now + 60000);
});
test('BPS requires bounded intervals and handles rollback or implausible counters', async () => {
  const f = await functions(), old = f.parseNetwork(dag('1000000'), null, now);
  assert.equal(f.parseNetwork(dag('1000300'), old, now + 30000).bps, 10);
  assert.equal(f.parseNetwork(dag('1216000'), old, now + 6 * hour).bps, 10);
  for (const [count, time] of [['1000300', now + 6 * hour + 1], ['999999', now + minute], ['999999999', now + minute]]) {
    const n = f.parseNetwork(dag(count), old, time); assert.equal(n.bps, null); assert.equal(n.sample.time, time);
  }
});
test('quotes reject missing, future, and invalid prices without fabricating change', async () => {
  const f = await functions();
  assert.equal(f.parseQuote(quote(), now, false).price, 0.0391);
  assert.equal(f.parseQuote({ kaspa: { usd: 1, last_updated_at: now / 1000 } }, now, false).change, null);
  for (const data of [null, {}, { kaspa: { usd: '1', last_updated_at: now / 1000 } }, quote(now + 6 * minute),
    { kaspa: { usd: -1, last_updated_at: now / 1000 } }, { kaspa: { usd: 1 } }]) assert.equal(f.parseQuote(data, now, false), null);
});
test('USDT fallback is explicitly approximate and does not inherit another change', async () => {
  const f = await functions(), data = { code: '200000', data: { last: '0.04', time: now, changeRate: '-0.02' } };
  const q = f.parseQuote(data, now, true); assert.equal(q.change, -2); assert.match(q.source, /USDT ≈ USD/);
  assert.equal(f.parseQuote({ ...data, data: { last: '0.04', time: now } }, now, true).change, null);
  assert.equal(f.parseQuote({ ...data, code: '429' }, now, true), null);
  assert.equal(f.parseQuote({ ...data, data: { last: '', time: now } }, now, true), null);
  assert.notEqual(f.priceText({ price: 0.0000001 }), '$0.0000');
});
test('real history sorts/deduplicates and requires a full recent series', async () => {
  const f = await functions(), data = history();
  data.prices.reverse(); data.prices.push(data.prices[0]);
  assert.equal(f.parseHistory(data, now).length, 97);
  assert.equal(f.parseHistory({ prices: [[now - hour, 1], [now, 2]] }, now), null);
  assert.equal(f.parseHistory(history(now - 2 * hour), now), null);
  assert.equal(f.parseHistory({ prices: Array(1001).fill([now, 1]) }, now), null);
  for (const v of [[now, '1'], [now, -1], [now, NaN], [now, 1, 2]]) {
    const h = history(); h.prices[40] = v; assert.equal(f.parseHistory(h, now), null);
  }
});
test('cached history keeps the real old line with a cached label, then expires', async () => {
  const f = await functions(), old = await snapshot(now - 2 * hour), cache = f.validateCache(old, now);
  assert.equal(cache.history.length, 97); assert.equal(f.chartLabel(cache, f.settings(''), now), '24h chart · cached');
  assert.equal(f.validateCache(await snapshot(now - 25 * hour), now).history.length, 0);
});
test('malformed caches discard only invalid fields and never import unknown versions', async () => {
  const f = await functions(), old = await snapshot();
  const bad = { ...old, quote: { ...old.quote, price: '0.04' }, hash: { th: 1, time: now + 10 * minute },
    network: { ...old.network, bps: Infinity } };
  const clean = f.validateCache(bad, now);
  assert.equal(clean.quote, undefined); assert.equal(clean.hash, undefined); assert.equal(clean.network.bps, null);
  assert.equal(clean.network.score, '565321249'); assert.equal(clean.history.length, 97);
  assert.equal(f.validateCache({ ...old, version: 99 }, now).quote, undefined);
});
test('partial outages keep old per-feed timestamps while successful feeds advance', async () => {
  const f = await functions(), old = await snapshot(now - 40 * minute);
  const result = await f.refresh(old, f.settings('chart=off'), now, async url => url === f.ENDPOINTS.hash ? { hashrate: 400000 } : null);
  assert.equal(result.quote.time, old.quote.time); assert.equal(result.network.time, old.network.time);
  assert.equal(result.hash.time, now); assert.equal(result.hash.th, 400000); assert.equal(result.delayed, true);
  assert.match(f.ageLabel(result.quote, now), /^Stale/);
});
test('complete outage preserves valid cache without re-timestamping any feed', async () => {
  const f = await functions(), old = await snapshot(now - 2 * hour);
  const result = await f.refresh(old, f.settings(''), now, async () => null);
  assert.deepEqual(result.quote, old.quote); assert.deepEqual(result.network, old.network); assert.deepEqual(result.hash, old.hash);
  assert.deepEqual(result.history, old.history); assert.equal(result.historyFetched, old.historyFetched); assert.equal(result.delayed, true);
});
test('refresh falls back only for price and rate-limits history', async () => {
  const f = await functions(), calls = [], old = await snapshot(now - 10 * minute);
  const get = async url => {
    calls.push(url);
    if (url === f.ENDPOINTS.fallback) return { code: '200000', data: { last: '0.04', time: now } };
    if (url === f.ENDPOINTS.network) return dag();
    if (url === f.ENDPOINTS.hash) return { hashrate: 10 };
    return null;
  };
  const result = await f.refresh(old, f.settings(''), now, get);
  assert.match(result.quote.source, /KuCoin/); assert.equal(result.quote.change, null);
  assert.equal(calls.filter(u => u === f.ENDPOINTS.history).length, 0); assert.equal(result.delayed, false);
});
test('fresh older source response never replaces a newer cached quote', async () => {
  const f = await functions(), old = await snapshot();
  const result = await f.refresh(old, f.settings('chart=off'), now + minute, async u => u === f.ENDPOINTS.price ? quote(now - minute) : null);
  assert.equal(result.quote.time, now); assert.equal(result.delayed, true);
});
test('invalid widget parameters are bounded, including inherited object names', async () => {
  const f = await functions();
  assert.deepEqual(f.settings('theme=violet;chart=off;refresh=15'), { theme: 'violet', chart: false, refresh: 15 });
  for (const s of ['__proto__', 'constructor', 'theme=constructor;refresh=1', 'theme=red;chart=yes', null, 123]) {
    assert.deepEqual(f.settings(s), { theme: 'mint', chart: true, refresh: 30 });
  }
});
test('HTTP failures, malformed/oversized JSON and offline errors degrade to null', async () => {
  for (const r of [{ status: 429, body: '{}' }, { status: 200, body: '<html>' }, { status: 200, body: ' '.repeat(1000001) }, { error: true }]) {
    const a = adapter({ response: () => r }), f = await functions(a.api);
    assert.equal(await f.getJSON('https://api.kaspa.org/info/blockdag'), null);
    assert.equal(a.state.requests[0].timeoutInterval, 8);
  }
});
test('cache corruption and write failure are recoverable', async () => {
  const a = adapter({ failSave: true }), f = await functions(a.api);
  assert.equal(f.loadCache(a.fm, '/missing', now).quote, undefined);
  assert.equal(f.saveCache(a.fm, '/file', await snapshot()), false);
});
test('all sizes and themes construct bounded canvases and real chart paths', async () => {
  const old = await snapshot();
  for (const family of ['small', 'medium', 'large']) for (const theme of ['mint', 'cyan', 'violet', 'xray']) {
    const a = adapter(), f = await functions(a.api), w = f.buildWidget(old, f.settings(theme), family, now);
    assert.ok(texts(w).includes('$0.0391')); assert.ok(texts(w).some(s => s.includes('565,321,249')));
    assert.ok(texts(w).some(s => s.includes('Sampling')));
    assert.equal(w.refreshAfterDate.getTime(), now + 30 * minute);
    assert.equal(a.state.drawings[0].respectScreenScale, false);
    assert.ok(a.state.drawings[0].size.width * a.state.drawings[0].size.height <= 340 * 340);
    assert.equal(a.state.drawings[0].paths.filter(p => p.points.length === 97).length, 3);
    if (family !== 'small') assert.ok(texts(w).includes('342.33 PH/s'));
  }
});
test('absent/disabled history draws no synthetic chart and flat history stays finite', async () => {
  for (const mode of ['missing', 'disabled', 'flat']) {
    const a = adapter(), f = await functions(a.api), s = await snapshot();
    if (mode === 'missing') s.history = [];
    if (mode === 'flat') s.history = f.parseHistory(history(now, true), now);
    const options = f.settings(mode === 'disabled' ? 'chart=off' : '');
    const w = f.buildWidget(s, options, 'medium', now);
    assert.equal(a.state.drawings[0].paths.filter(p => p.points.length === 97).length, mode === 'flat' ? 3 : 0);
    if (mode === 'missing') assert.ok(texts(w).includes('24h chart unavailable'));
  }
});
test('oversized DAA score uses an explicit compact unit without lossy conversion', async () => {
  const f = await functions();
  assert.equal(f.scoreText({ score: '18446744073709551615' }, false), '18,446,744,073,709,551,615');
  assert.equal(f.scoreText({ score: '18446744073709551615' }, true), '18446744073.70B');
});
test('cached chart and approximate quote stay labeled in small/medium outage layouts', async () => {
  const old = await snapshot(now - 2 * hour);
  old.quote.source = 'KuCoin · USDT ≈ USD'; old.delayed = true;
  for (const family of ['small', 'medium']) {
    const a = adapter(), f = await functions(a.api), w = f.buildWidget(old, f.settings(''), family, now);
    const content = texts(w).join(' ');
    assert.match(content, /chart · cached/); assert.match(content, /Feeds delayed/); assert.match(content, /USDT ≈ USD/);
  }
});
test('full installed script completes every Home Screen size offline with cache', async () => {
  const cache = await snapshot(Date.now() - 40 * minute);
  for (const family of ['small', 'medium', 'large']) {
    const a = adapter({ family, cache, response: () => ({ error: true }) });
    const state = await execute(a);
    assert.equal(state.complete, 1); assert.equal(state.widgets.length, 1); assert.equal(state.writes.length, 1);
    assert.ok(texts(state.widgets[0]).some(s => s.includes('Stale')));
    assert.ok(texts(state.widgets[0]).some(s => s.includes('Feeds delayed')));
    assert.equal(state.writes[0].quote.time, cache.quote.time); assert.equal(state.previews.length, 0);
  }
});
test('full installed script handles first install and successful live-shaped payloads', async () => {
  const at = Date.now();
  const a = adapter({ response: url => ({ status: 200, body: JSON.stringify(url.includes('/simple/price') ? quote(at) :
    url.includes('market_chart') ? history(at) : url.includes('blockdag') ? dag() : { hashrate: 342330 }) }) });
  const state = await execute(a);
  assert.equal(state.complete, 1); assert.equal(state.requests.length, 4);
  assert.equal(state.writes[0].network.score, '565321249'); assert.equal(state.writes[0].history.length, 97);
  assert.equal(state.writes[0].delayed, false); assert.equal(state.writes[0].network.bps, null);
});
test('unsupported accessory family shows setup guidance without network requests', async () => {
  const a = adapter({ family: 'accessoryCircular' }), state = await execute(a);
  assert.equal(state.requests.length, 0); assert.equal(state.complete, 1);
  assert.match(texts(state.widgets[0])[0], /Home Screen/);
});
test('preview selection runs the requested size and cancel fetches nothing', async () => {
  for (const choice of [-1, 0, 1, 2]) {
    const a = adapter({ inWidget: false, choice }), state = await execute(a);
    assert.equal(state.complete, 1);
    if (choice < 0) { assert.equal(state.requests.length, 0); assert.equal(state.widgets.length, 0); }
    else assert.deepEqual(state.previews, [['medium'], ['small'], ['large']][choice]);
  }
});
test('full script reports cache-save failure instead of silently promising retained data', async () => {
  const a = adapter({ failSave: true }), state = await execute(a);
  assert.ok(texts(state.widgets[0]).some(s => s.startsWith('Cache unavailable')));
  assert.equal(state.complete, 1);
});
