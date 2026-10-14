import SwiftUI
import KaspaCore

enum CardSize: Hashable { case small, medium, large }

extension Color {
    init(hex: String) {
        let value = UInt64(hex, radix: 16) ?? 0x78FFE0
        self.init(.sRGB, red: Double((value >> 16) & 255) / 255,
            green: Double((value >> 8) & 255) / 255, blue: Double(value & 255) / 255, opacity: 1)
    }
}

struct GlassSurface: View {
    let snapshot: Snapshot
    let appearance: Appearance
    var body: some View {
        let a = appearance.resolved
        ZStack {
            LinearGradient(colors: [Color(hex: a.background), Color(hex: a.background).opacity(0.94), Color(hex: a.background)], startPoint: .topLeading, endPoint: .bottomTrailing)
            Canvas { context, size in
                var grid = Path()
                for x in stride(from: CGFloat(0), to: size.width, by: 24) { grid.move(to: CGPoint(x: x, y: 0)); grid.addLine(to: CGPoint(x: x, y: size.height)) }
                for y in stride(from: CGFloat(0), to: size.height, by: 24) { grid.move(to: CGPoint(x: 0, y: y)); grid.addLine(to: CGPoint(x: size.width, y: y)) }
                context.stroke(grid, with: .color(Color(hex: a.accent).opacity(0.10)), lineWidth: 0.5)
                let points = snapshot.history
                guard a.showChart, a.intensity > 0, points.count >= 2, let first = points.first, let last = points.last, last.time > first.time else { return }
                let low = points.map(\.price).min()!, high = points.map(\.price).max()!
                var line = Path()
                for (index, point) in points.enumerated() {
                    let x = 8 + point.time.timeIntervalSince(first.time) / last.time.timeIntervalSince(first.time) * (size.width - 16)
                    let y = high == low ? size.height / 2 : size.height * 0.18 + (high - point.price) / (high - low) * size.height * 0.64
                    if index == 0 { line.move(to: CGPoint(x: x, y: y)) } else { line.addLine(to: CGPoint(x: x, y: y)) }
                }
                context.drawLayer { glow in
                    glow.addFilter(.blur(radius: 5))
                    glow.stroke(line, with: .color(Color(hex: a.chart).opacity(a.intensity * 0.55)), lineWidth: 7)
                }
                context.stroke(line, with: .color(Color(hex: a.chart).opacity(a.intensity)), lineWidth: 1.5)
            }.accessibilityHidden(true)
        }
    }
}

struct KaspaCard: View {
    let snapshot: Snapshot
    let appearance: Appearance
    let date: Date
    let size: CardSize
    var body: some View {
        let a = appearance.resolved
        VStack(alignment: .leading, spacing: size == .small ? 5 : 10) {
            HStack(spacing: 5) {
                Image("KaspaMark").resizable().scaledToFit().frame(width: 21, height: 21).accessibilityLabel("Kaspa")
                Text("KASPA").font(.system(.caption2, design: .monospaced).weight(.bold))
                Spacer(minLength: 0)
                Text(date, style: .time).font(.system(.caption2, design: .monospaced)).foregroundStyle(Color(hex: a.clock))
            }
            if size == .medium {
                HStack(alignment: .top, spacing: 10) { price(a); Spacer(minLength: 0); network(a) }
            } else {
                price(a)
                network(a)
            }
            Spacer(minLength: 0)
            VStack(alignment: .leading, spacing: 2) {
                Text(snapshot.status(at: date)).font(.system(size: size == .small ? 9 : 11, design: .monospaced)).lineLimit(2)
                if let time = snapshot.quote?.time { Text("Price \(time.formatted(date: .omitted, time: .shortened))").font(.system(size: 9, design: .monospaced)).opacity(0.8) }
                if appearance.showChart && size != .small {
                    Text(snapshot.history.isEmpty ? "24h chart unavailable" : "24h USD observations").font(.system(size: 9, design: .monospaced)).opacity(0.8)
                }
            }
        }
        .foregroundStyle(Color(hex: a.text)).padding(size == .small ? 10 : 14)
        .minimumScaleFactor(0.75)
        .accessibilityElement(children: .contain)
    }
    @ViewBuilder private func price(_ a: Appearance) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(snapshot.priceDisplay).font(.system(size: size == .large ? 42 : size == .medium ? 30 : 25, weight: .bold, design: .monospaced))
                .foregroundStyle(Color(hex: a.price)).lineLimit(1).minimumScaleFactor(0.55).accessibilityIdentifier("kaspa-price")
            if let change = snapshot.quote?.change {
                Text(String(format: "24h %+.2f%% · USD", change)).font(.system(.caption2, design: .monospaced)).foregroundStyle(Color(hex: a.accent)).lineLimit(1)
            } else { Text("USD · change unavailable").font(.system(size: 9, design: .monospaced)).lineLimit(1) }
        }.frame(maxWidth: .infinity, alignment: .leading)
    }
    @ViewBuilder private func network(_ a: Appearance) -> some View {
        VStack(alignment: .leading, spacing: size == .small ? 2 : 5) {
            Text("DAA SCORE").font(.system(size: 9, design: .monospaced)).opacity(0.85)
            Text(snapshot.network.map { $0.score.formatted() } ?? "—").font(.system(size: size == .small ? 14 : 18, weight: .semibold, design: .monospaced)).lineLimit(1)
            if size != .small {
                Text("OBSERVED AVG BPS").font(.system(size: 9, design: .monospaced)).opacity(0.85)
                Text(snapshot.network?.bps.map { String(format: "%.1f", $0) } ?? "Sampling…").font(.system(.caption, design: .monospaced)).lineLimit(1)
                Text(snapshot.hashrate?.display ?? "Hashrate unavailable").font(.system(.caption2, design: .monospaced)).lineLimit(1)
            }
        }.foregroundStyle(Color(hex: a.network))
    }
}
