import Foundation

public enum FeedParser {
    private struct CG: Decodable { let kaspa: CGQuote }
    private struct CGQuote: Decodable { let usd: Double; let usd_24h_change: Double?; let last_updated_at: Double }
    private struct Exchange: Decodable { let code: String; let data: Ticker }
    private struct Ticker: Decodable { let last: String; let changeRate: String?; let time: Double }
    private struct History: Decodable { let prices: [[Double]] }
    private struct DAG: Decodable { let networkName: String; let blockCount: UInt64; let virtualDaaScore: UInt64 }
    private struct Hash: Decodable { let hashrate: Double }
    public static func quote(_ data: Data, at now: Date) -> Quote? {
        guard let q = try? JSONDecoder().decode(CG.self, from: data).kaspa else { return nil }
        let change = q.usd_24h_change.flatMap { $0.isFinite && $0 >= -100 ? $0 : nil }
        let quote = Quote(price: q.usd, change: change, time: Date(timeIntervalSince1970: q.last_updated_at), source: "CoinGecko · USD")
        return quote.valid(at: now) ? quote : nil
    }
    public static func exchange(_ data: Data, at now: Date) -> Quote? {
        guard let response = try? JSONDecoder().decode(Exchange.self, from: data), response.code == "200000", let price = Double(response.data.last) else { return nil }
        let q = response.data
        let change = q.changeRate.flatMap(Double.init).flatMap { value -> Double? in
            let percent = value * 100; return percent.isFinite && percent >= -100 ? percent : nil
        }
        let quote = Quote(price: price, change: change, time: Date(timeIntervalSince1970: q.time / 1_000), source: "KuCoin · KAS/USDT ≈ USD")
        return quote.valid(at: now) ? quote : nil
    }
    public static func history(_ data: Data, at now: Date) -> [PricePoint]? {
        guard let rows = try? JSONDecoder().decode(History.self, from: data).prices, (2...1_000).contains(rows.count) else { return nil }
        var points: [PricePoint] = []
        for row in rows {
            guard row.count == 2, row[0].isFinite, row[1].isFinite, row[1] > 0 else { return nil }
            points.append(PricePoint(time: Date(timeIntervalSince1970: row[0] / 1_000), price: row[1]))
        }
        var seen = Set<Date>()
        points = points.sorted { $0.time < $1.time }.filter { seen.insert($0.time).inserted }
        return validHistory(points, at: now) ? points : nil
    }
    public static func validHistory(_ points: [PricePoint], at now: Date) -> Bool {
        guard (2...1_000).contains(points.count), let first = points.first, let last = points.last,
              last.time.timeIntervalSince(first.time) >= 22 * 3_600, last.time >= now.addingTimeInterval(-3_600) else { return false }
        var previous: Date?
        for p in points {
            guard p.price.isFinite, p.price > 0, p.time >= now.addingTimeInterval(-26 * 3_600), p.time <= now.addingTimeInterval(300), previous.map({ p.time > $0 }) ?? true else { return false }
            previous = p.time
        }
        return true
    }
    public static func network(_ data: Data, previous: NetworkReading?, at now: Date) -> NetworkReading? {
        guard let n = try? JSONDecoder().decode(DAG.self, from: data), ["mainnet", "kaspa-mainnet"].contains(n.networkName), n.blockCount > 0, n.virtualDaaScore > 0 else { return nil }
        return NetworkReading(score: n.virtualDaaScore, count: n.blockCount, time: now,
            bps: NetworkReading.rate(previous: previous, count: n.blockCount, at: now))
    }
    public static func hashrate(_ data: Data, at now: Date) -> HashReading? {
        guard let hash = try? JSONDecoder().decode(Hash.self, from: data).hashrate, hash.isFinite, hash > 0 else { return nil }
        return HashReading(thPerSecond: hash, time: now)
    }
}
