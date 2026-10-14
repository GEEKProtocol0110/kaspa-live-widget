import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

public struct FeedClient {
    private let session: URLSession
    public init(session: URLSession = .shared) { self.session = session }
    private func get(_ url: String) async -> Data? {
        guard let url = URL(string: url), url.scheme == "https" else { return nil }
        var request = URLRequest(url: url); request.timeoutInterval = 12
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        do {
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200, data.count <= 1_000_000 else { return nil }
            return data
        } catch { return nil }
    }
    private func quote(at now: Date) async -> Quote? {
        if let data = await get("https://api.coingecko.com/api/v3/simple/price?ids=kaspa&vs_currencies=usd&include_24hr_change=true&include_last_updated_at=true"), let q = FeedParser.quote(data, at: now) { return q }
        if Task.isCancelled { return nil }
        if let data = await get("https://api.kucoin.com/api/v1/market/stats?symbol=KAS-USDT") { return FeedParser.exchange(data, at: now) }
        return nil
    }
    private func historyIfNeeded(_ needed: Bool) async -> Data? {
        guard needed else { return nil }
        return await get("https://api.coingecko.com/api/v3/coins/kaspa/market_chart?vs_currency=usd&days=1")
    }
    public func refresh(previous: Snapshot, showChart: Bool, now: Date = Date()) async -> Snapshot {
        let old = previous.validated(at: now)
        async let price = quote(at: now)
        async let dag = get("https://api.kaspa.org/info/blockdag")
        async let hash = get("https://api.kaspa.org/info/hashrate")
        let needsHistory = showChart && (old.history.isEmpty || old.historyFetched.map { now.timeIntervalSince($0) >= 3_600 } ?? true)
        async let historyData = historyIfNeeded(needsHistory)
        let (q, n, h, chartData) = await (price, dag, hash, historyData)
        if Task.isCancelled { return old }
        var result = old
        let network = n.flatMap { FeedParser.network($0, previous: old.network, at: now) }
        let hashrate = h.flatMap { FeedParser.hashrate($0, at: now) }
        if let q, old.quote.map({ q.time >= $0.time }) ?? true { result.quote = q }
        if let network { result.network = network }
        if let hashrate { result.hashrate = hashrate }
        let history = chartData.flatMap { FeedParser.history($0, at: now) }
        if let history { result.history = history; result.historyFetched = now }
        result.attempted = now
        result.delayed = q == nil || network == nil || hashrate == nil || (needsHistory && history == nil)
        return result
    }
}
