import Foundation

public struct PricePoint: Codable, Equatable, Sendable {
    public let time: Date
    public let price: Double
    public init(time: Date, price: Double) { self.time = time; self.price = price }
}
public struct Quote: Codable, Equatable, Sendable {
    public let price: Double
    public let change: Double?
    public let time: Date
    public let source: String
    public init(price: Double, change: Double?, time: Date, source: String) {
        self.price = price; self.change = change; self.time = time; self.source = source
    }
    public func valid(at now: Date) -> Bool {
        price.isFinite && price > 0 && time.timeIntervalSince1970 > 0 && time <= now.addingTimeInterval(300)
    }
}
public struct NetworkReading: Codable, Equatable, Sendable {
    public let score: UInt64
    public let count: UInt64
    public let time: Date
    public let bps: Double?
    public init(score: UInt64, count: UInt64, time: Date, bps: Double?) {
        self.score = score; self.count = count; self.time = time; self.bps = bps
    }
    public func valid(at now: Date) -> Bool {
        score > 0 && count > 0 && time.timeIntervalSince1970 > 0 && time <= now.addingTimeInterval(300)
    }
    public static func rate(previous: NetworkReading?, count: UInt64, at now: Date) -> Double? {
        guard let old = previous, count >= old.count else { return nil }
        let seconds = now.timeIntervalSince(old.time)
        guard seconds >= 30, seconds <= 21_600 else { return nil }
        let value = Double(count - old.count) / seconds
        return value.isFinite && value <= 10_000 ? value : nil
    }
}
public struct HashReading: Codable, Equatable, Sendable {
    public let thPerSecond: Double
    public let time: Date
    public init(thPerSecond: Double, time: Date) { self.thPerSecond = thPerSecond; self.time = time }
    public var display: String {
        thPerSecond >= 1_000 ? String(format: "%.2f PH/s", thPerSecond / 1_000) : String(format: "%.2f TH/s", thPerSecond)
    }
}
public struct Snapshot: Codable, Equatable, Sendable {
    public var quote: Quote?
    public var network: NetworkReading?
    public var hashrate: HashReading?
    public var history: [PricePoint] = []
    public var historyFetched: Date?
    public var attempted: Date?
    public var delayed: Bool = false
    public init() {}
    public func validated(at now: Date) -> Snapshot {
        var result = self
        if quote?.valid(at: now) != true { result.quote = nil }
        if network?.valid(at: now) != true { result.network = nil }
        if let hash = hashrate, !hash.thPerSecond.isFinite || hash.thPerSecond <= 0 || hash.time > now.addingTimeInterval(300) || hash.time.timeIntervalSince1970 <= 0 { result.hashrate = nil }
        if !FeedParser.validHistory(history, at: now) { result.history = []; result.historyFetched = nil }
        return result
    }
    public func status(at now: Date) -> String {
        guard let quote, let network, let hashrate else { return delayed ? "Feeds delayed · cached readings kept" : "Waiting for readings" }
        if now.timeIntervalSince(quote.time) > 1_800 { return "Price stale" }
        if now.timeIntervalSince(network.time) > 1_800 { return "Network stale" }
        if now.timeIntervalSince(hashrate.time) > 1_800 { return "Hashrate stale" }
        return delayed ? "Some feeds delayed" : "Readings updated"
    }
    public var priceDisplay: String { quote.map { String(format: "$%.4f", $0.price) } ?? "—" }
}

public struct Appearance: Codable, Equatable, Sendable {
    public var theme = "dark"
    public var background = "050C18"
    public var text = "DEFFF9"
    public var accent = "78FFE0"
    public var clock = "78FFE0"
    public var price = "78FFE0"
    public var chart = "78FFE0"
    public var network = "DEFFF9"
    public var intensity: Double = 0.5
    public var showChart = true
    public init() {}
    public func normalized() -> Appearance {
        var a = self
        if !["dark", "light", "xray", "custom"].contains(a.theme) { a.theme = "dark" }
        let defaults = Appearance()
        for key in [\Appearance.background, \.text, \.accent, \.clock, \.price, \.chart, \.network] {
            let hex = a[keyPath: key].uppercased()
            a[keyPath: key] = hex.count == 6 && hex.allSatisfy { "0123456789ABCDEF".contains($0) } ? hex : defaults[keyPath: key]
        }
        a.intensity = a.intensity.isFinite ? min(1, max(0, a.intensity)) : 0.5
        return a
    }
    public var resolved: Appearance {
        var a = normalized()
        switch a.theme {
        case "light": a.background = "EDF9FA"; a.text = "0C2B37"; a.accent = "09565B"; a.clock = a.accent; a.price = a.accent; a.chart = "087F8B"; a.network = a.text
        case "xray": a.background = "000000"; a.text = "FFFFFF"; a.accent = "FFFFFF"; a.clock = "FFFFFF"; a.price = "FFFFFF"; a.chart = "FFFFFF"; a.network = "FFFFFF"
        case "dark": let d = Appearance(); a.background = d.background; a.text = d.text; a.accent = d.accent; a.clock = d.clock; a.price = d.price; a.chart = d.chart; a.network = d.network
        default: break
        }
        return a
    }
}
public struct SavedLook: Codable, Equatable, Identifiable, Sendable {
    public let name: String
    public let appearance: Appearance
    public var id: String { name.lowercased() }
    public init(name: String, appearance: Appearance) { self.name = name; self.appearance = appearance.normalized() }
}
public struct Settings: Codable, Equatable, Sendable {
    public var appearance = Appearance()
    public var intervalMinutes = 30
    public var looks: [SavedLook] = []
    public init() {}
    public func normalized() -> Settings {
        var s = self; s.appearance = appearance.normalized()
        if ![15, 30, 60].contains(s.intervalMinutes) { s.intervalMinutes = 30 }
        var seen = Set<String>()
        s.looks = Array(looks.filter { !$0.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && $0.name.count <= 32 && seen.insert($0.id).inserted }.prefix(20))
        return s
    }
    public mutating func saveLook(name: String) -> Bool {
        let title = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty, title.count <= 32 else { return false }
        let look = SavedLook(name: title, appearance: appearance)
        if let index = looks.firstIndex(where: { $0.id == look.id }) { looks[index] = look; return true }
        guard looks.count < 20 else { return false }
        looks.append(look); return true
    }
}
