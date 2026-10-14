// Optional real-feed evidence. Outages are reported without failing offline regressions.
const fs = require('node:fs');
const path = require('node:path');
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;

async function main() {
  const source = fs.readFileSync(path.join(__dirname, 'Geek-Kaspa-Widget.js'), 'utf8').replace(/\nawait run\(\);\s*$/, '');
  const f = await new AsyncFunction(source + '\nreturn {settings, refresh, ENDPOINTS};')();
  const evidence = { checkedAt: new Date().toISOString(), feeds: {} };
  const get = async url => {
    const name = Object.keys(f.ENDPOINTS).find(key => f.ENDPOINTS[key] === url);
    try {
      const response = await fetch(url, { headers: { Accept: 'application/json' }, signal: AbortSignal.timeout(8000) });
      evidence.feeds[name] = { status: response.status };
      if (!response.ok) return null;
      const body = await response.text();
      if (body.length > 1000000) throw new Error('Oversized response');
      const json = JSON.parse(body);
      evidence.feeds[name].keys = Object.keys(json);
      return json;
    } catch (error) { evidence.feeds[name] = { ...evidence.feeds[name], error: error.message }; return null; }
  };
  const snapshot = await f.refresh(null, f.settings(''), Date.now(), get);
  evidence.accepted = {
    price: !!snapshot.quote, network: !!snapshot.network, hashrate: !!snapshot.hash,
    history: snapshot.history.length >= 2
  };
  evidence.readings = { quote: snapshot.quote, network: snapshot.network, hashrate: snapshot.hash,
    historyPoints: snapshot.history.length, delayed: snapshot.delayed };
  const directory = path.resolve('scriptable-evidence');
  fs.mkdirSync(directory, { recursive: true });
  fs.writeFileSync(path.join(directory, 'live-feeds.json'), JSON.stringify(evidence, null, 2) + '\n');
  console.log(JSON.stringify(evidence, null, 2));
}
main().catch(error => { console.error(error); process.exitCode = 1; });
