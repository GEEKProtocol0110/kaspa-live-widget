import Foundation
import XCTest
@testable import KaspaCore

final class KaspaCoreTests: XCTestCase {
    let now = Date(timeIntervalSince1970: 1_790_000_000)
    func data(_ json: String) -> Data { Data(json.utf8) }
    func testQuotesRequireProviderTimesAndPositivePrices() {
        let valid = data("{\"kaspa\":{\"usd\":0.03,\"usd_24h_change\":2.5,\"last_updated_at\":1790000000}}")
        XCTAssertEqual(FeedParser.quote(valid, at: now)?.price, 0.03)
        for payload in ["{}", "{\"kaspa\":{\"usd\":0,\"last_updated_at\":1790000000}}", "{\"kaspa\":{\"usd\":true,\"last_updated_at\":1790000000}}", "{\"kaspa\":{\"usd\":0.03,\"last_updated_at\":1790009999}}"] { XCTAssertNil(FeedParser.quote(data(payload), at: now)) }
    }
    func testExchangeFallbackIsLabeledAndConvertsFractionalChange() {
        let q = FeedParser.exchange(data("{\"code\":\"200000\",\"data\":{\"last\":\"0.03\",\"changeRate\":\"0.02\",\"time\":1790000000000}}"), at: now)
        XCTAssertEqual(q?.change, 2); XCTAssertTrue(q?.source.contains("USDT ≈ USD") == true)
        XCTAssertNil(FeedParser.exchange(data("{\"code\":\"bad\",\"data\":{\"last\":\"0.03\",\"time\":1790000000000}}"), at: now))
    }
    func testHistoryNeedsActualFreshDayAndRejectsInvalidRows() {
        let rows = [[(now.timeIntervalSince1970 - 86_400) * 1_000, 0.03], [now.timeIntervalSince1970 * 1_000, 0.04]]
        let valid = try! JSONSerialization.data(withJSONObject: ["prices": rows])
        XCTAssertEqual(FeedParser.history(valid, at: now)?.count, 2)
        XCTAssertNil(FeedParser.history(data("{\"prices\":[[1790000000000,0.03],[1790000001000,0.04]]}"), at: now))
        XCTAssertNil(FeedParser.history(data("{\"prices\":[[1790000000000,-1],[1790000001000,0.04]]}"), at: now))
        XCTAssertNil(FeedParser.history(data("{\"prices\":[[1790000000000,0.03,8],[1790000001000,0.04]]}"), at: now))
    }
    func testMainnetOnlyAndBPSNeedsTwoBoundedSamples() {
        let previous = NetworkReading(score: 1, count: 100, time: now.addingTimeInterval(-60), bps: nil)
        let n = FeedParser.network(data("{\"networkName\":\"mainnet\",\"blockCount\":700,\"virtualDaaScore\":999}"), previous: previous, at: now)
        XCTAssertEqual(n?.bps, 10); XCTAssertEqual(n?.score, 999)
        let official = FeedParser.network(data("{\"networkName\":\"kaspa-mainnet\",\"blockCount\":\"700\",\"virtualDaaScore\":\"999\"}"), previous: previous, at: now)
        XCTAssertEqual(official?.bps, 10); XCTAssertEqual(official?.score, 999)
        XCTAssertNil(FeedParser.network(data("{\"networkName\":\"testnet-10\",\"blockCount\":700,\"virtualDaaScore\":999}"), previous: previous, at: now))
        XCTAssertNil(FeedParser.network(data("{\"networkName\":\"kaspa-testnet-10\",\"blockCount\":700,\"virtualDaaScore\":999}"), previous: previous, at: now))
        XCTAssertNil(NetworkReading.rate(previous: nil, count: 700, at: now))
        XCTAssertNil(NetworkReading.rate(previous: previous, count: 90, at: now))
        XCTAssertNil(NetworkReading.rate(previous: previous, count: 700, at: now.addingTimeInterval(86_400)))
        XCTAssertNil(NetworkReading.rate(previous: previous, count: 700, at: previous.time.addingTimeInterval(1)))
    }
    func testRESTDecimalCountersRejectSignsFractionsBooleansAndOverflow() {
        for value in ["-1", "+1", "1.5", "1e3", "18446744073709551616", "", " 700", "７００"] {
            let payload = try! JSONSerialization.data(withJSONObject: ["networkName": "kaspa-mainnet", "blockCount": value, "virtualDaaScore": "999"])
            XCTAssertNil(FeedParser.network(payload, previous: nil, at: now))
        }
        XCTAssertNil(FeedParser.network(data("{\"networkName\":\"kaspa-mainnet\",\"blockCount\":true,\"virtualDaaScore\":\"999\"}"), previous: nil, at: now))
        let max = FeedParser.network(data("{\"networkName\":\"kaspa-mainnet\",\"blockCount\":\"18446744073709551615\",\"virtualDaaScore\":\"18446744073709551615\"}"), previous: nil, at: now)
        XCTAssertEqual(max?.count, UInt64.max); XCTAssertEqual(max?.score, UInt64.max)
    }
    func testHashrateUnitsAndMissingValues() {
        XCTAssertEqual(FeedParser.hashrate(data("{\"hashrate\":344150}"), at: now)?.display, "344.15 PH/s")
        XCTAssertNil(FeedParser.hashrate(data("{\"hashrate\":-1}"), at: now))
        XCTAssertNil(FeedParser.hashrate(data("{}"), at: now))
    }
    func testCachedReadingsKeepIndependentStaleClocks() {
        var s = Snapshot()
        s.quote = Quote(price: 0.03, change: nil, time: now.addingTimeInterval(-3_600), source: "CoinGecko · USD")
        s.network = NetworkReading(score: 1, count: 2, time: now, bps: nil)
        s.hashrate = HashReading(thPerSecond: 1, time: now)
        XCTAssertEqual(s.status(at: now), "Price stale")
        s.quote = Quote(price: 0.03, change: nil, time: now, source: "CoinGecko · USD")
        s.delayed = true; XCTAssertEqual(s.status(at: now), "Some feeds delayed")
        s.quote = Quote(price: 0.03, change: nil, time: now.addingTimeInterval(999), source: "x")
        XCTAssertNil(s.validated(at: now).quote)
        XCTAssertEqual(Snapshot().priceDisplay, "—")
    }
    func testNamedLooksAreBoundedAndLoadingDoesNotChangeRefreshInterval() {
        var s = Settings(); s.appearance.theme = "custom"; s.appearance.price = "FF0000"; s.intervalMinutes = 60
        XCTAssertFalse(s.saveLook(name: " "))
        XCTAssertTrue(s.saveLook(name: "Night")); s.appearance.intensity = 0.2
        XCTAssertTrue(s.saveLook(name: "NIGHT")); XCTAssertEqual(s.looks.count, 1)
        for i in 1...19 { XCTAssertTrue(s.saveLook(name: "Look \(i)")) }
        XCTAssertFalse(s.saveLook(name: "Overflow"))
        s.appearance = s.looks[0].appearance; XCTAssertEqual(s.intervalMinutes, 60)
        XCTAssertEqual(s.appearance.price, "FF0000"); XCTAssertEqual(s.appearance.intensity, 0.2)
    }
    func testCorruptPreferencesAndAppearanceAreNormalized() {
        var a = Appearance(); a.background = "bad"; a.intensity = .infinity; a.theme = "unknown"
        XCTAssertEqual(a.normalized().background, "050C18"); XCTAssertEqual(a.normalized().intensity, 0.5); XCTAssertEqual(a.normalized().theme, "dark")
        var s = Settings(); s.intervalMinutes = 1; XCTAssertEqual(s.normalized().intervalMinutes, 30)
        let suite = "kaspa-tests-\(UUID())"; let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        defaults.set(data("broken"), forKey: "settings.v1")
        XCTAssertEqual(SharedStore(defaults: defaults).settings(), Settings())
    }
    func testSettingsAndReadingsCannotOverwriteEachOther() {
        let suite = "kaspa-tests-\(UUID())"; let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let store = SharedStore(defaults: defaults); var settings = Settings(); settings.appearance.price = "FF0000"
        store.save(settings: settings)
        var s = Snapshot(); s.quote = Quote(price: 0.03, change: nil, time: Date(), source: "CoinGecko · USD")
        store.save(snapshot: s)
        XCTAssertEqual(store.settings(), settings)
        store.save(settings: Settings()); XCTAssertEqual(store.snapshot().quote?.price, 0.03)
        var older = s; older.quote = Quote(price: 0.01, change: nil, time: Date().addingTimeInterval(-100), source: "CoinGecko · USD")
        store.save(snapshot: older); XCTAssertEqual(store.snapshot().quote?.price, 0.03)
    }
    func testOutageKeepsProviderTimesAndDoesNotInventAChart() async {
        let config = URLSessionConfiguration.ephemeral; config.protocolClasses = [FixtureProtocol.self]
        let session = URLSession(configuration: config); defer { session.invalidateAndCancel() }
        FixtureProtocol.configure([:])
        var old = Snapshot(); old.quote = Quote(price: 0.03, change: nil, time: now.addingTimeInterval(-3_600), source: "CoinGecko · USD")
        old.network = NetworkReading(score: 10, count: 100, time: now.addingTimeInterval(-3_600), bps: nil)
        let result = await FeedClient(session: session).refresh(previous: old, showChart: true, now: now)
        XCTAssertEqual(result.quote, old.quote); XCTAssertEqual(result.network, old.network)
        XCTAssertTrue(result.history.isEmpty); XCTAssertTrue(result.delayed)
        XCTAssertEqual(result.attempted, now)
    }
    func testFeedFailureIsIndependentAndFreshHistoryIsNotFetchedAgain() async {
        let config = URLSessionConfiguration.ephemeral; config.protocolClasses = [FixtureProtocol.self]
        let session = URLSession(configuration: config); defer { session.invalidateAndCancel() }
        FixtureProtocol.configure([
            "/info/blockdag": data("{\"networkName\":\"kaspa-mainnet\",\"blockCount\":\"700\",\"virtualDaaScore\":\"999\"}"),
            "/info/hashrate": data("{\"hashrate\":344150}")])
        var old = Snapshot(); old.quote = Quote(price: 0.03, change: nil, time: now.addingTimeInterval(-100), source: "CoinGecko · USD")
        old.network = NetworkReading(score: 1, count: 100, time: now.addingTimeInterval(-60), bps: nil)
        old.history = [PricePoint(time: now.addingTimeInterval(-86_400), price: 0.03), PricePoint(time: now, price: 0.04)]
        old.historyFetched = now.addingTimeInterval(-60)
        let result = await FeedClient(session: session).refresh(previous: old, showChart: true, now: now)
        XCTAssertEqual(result.quote?.time, old.quote?.time); XCTAssertEqual(result.network?.score, 999)
        XCTAssertEqual(result.network?.bps, 10); XCTAssertEqual(result.hashrate?.display, "344.15 PH/s")
        XCTAssertEqual(result.historyFetched, old.historyFetched)
        XCTAssertFalse(FixtureProtocol.paths().contains("/api/v3/coins/kaspa/market_chart"))
        XCTAssertTrue(result.delayed)
    }
}

private final class FixtureProtocol: URLProtocol {
    private static let lock = NSLock()
    private static var fixtures: [String: Data] = [:]
    private static var requested: [String] = []
    static func configure(_ values: [String: Data]) { lock.lock(); defer { lock.unlock() }; fixtures = values; requested = [] }
    static func paths() -> [String] { lock.lock(); defer { lock.unlock() }; return requested }
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func startLoading() {
        let path = request.url!.path
        Self.lock.lock(); let value = Self.fixtures[path]; Self.requested.append(path); Self.lock.unlock()
        let response = HTTPURLResponse(url: request.url!, statusCode: value == nil ? 503 : 200, httpVersion: "HTTP/1.1", headerFields: ["Content-Type": "application/json"])!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: value ?? Data())
        client?.urlProtocolDidFinishLoading(self)
    }
    override func stopLoading() {}
}
