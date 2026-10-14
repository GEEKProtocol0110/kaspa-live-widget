import Foundation

public final class SharedStore {
    public static let group = "group.xyz.geekprotocol.kaspawidget"
    private let defaults: UserDefaults
    public init(defaults: UserDefaults? = UserDefaults(suiteName: SharedStore.group)) {
        precondition(defaults != nil, "Shared App Group preferences unavailable")
        self.defaults = defaults!
    }
    public func settings() -> Settings {
        decode(Settings.self, key: "settings.v1")?.normalized() ?? Settings()
    }
    public func save(settings: Settings) { encode(settings.normalized(), key: "settings.v1") }
    public func snapshot(now: Date = Date()) -> Snapshot {
        (decode(Snapshot.self, key: "snapshot.v1") ?? Snapshot()).validated(at: now)
    }
    public func save(snapshot incoming: Snapshot) {
        // Settings and feed caches use separate keys; extension refreshes cannot overwrite a look.
        // Merge by each feed's observation time so overlapping refreshes do not regress readings.
        var s = incoming
        let old = snapshot()
        if let q = old.quote, incoming.quote.map({ q.time > $0.time }) ?? true { s.quote = q }
        if let n = old.network, incoming.network.map({ n.time > $0.time }) ?? true { s.network = n }
        if let h = old.hashrate, incoming.hashrate.map({ h.time > $0.time }) ?? true { s.hashrate = h }
        if let date = old.historyFetched, incoming.historyFetched.map({ date > $0 }) ?? true { s.history = old.history; s.historyFetched = date }
        encode(s, key: "snapshot.v1")
    }
    private func decode<T: Decodable>(_ type: T.Type, key: String) -> T? {
        guard let data = defaults.data(forKey: key), data.count <= 1_000_000 else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }
    private func encode<T: Encodable>(_ value: T, key: String) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        defaults.set(data, forKey: key)
    }
}
