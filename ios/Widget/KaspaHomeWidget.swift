import SwiftUI
import WidgetKit
import KaspaCore

struct KaspaEntry: TimelineEntry {
    let date: Date
    let snapshot: Snapshot
    let appearance: Appearance
}
struct KaspaProvider: TimelineProvider {
    func placeholder(in context: Context) -> KaspaEntry {
        KaspaEntry(date: Date(), snapshot: Snapshot(), appearance: Appearance())
    }
    func getSnapshot(in context: Context, completion: @escaping (KaspaEntry) -> Void) {
        let store = SharedStore()
        completion(KaspaEntry(date: Date(), snapshot: store.snapshot(), appearance: store.settings().appearance))
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<KaspaEntry>) -> Void) {
        Task {
            let store = SharedStore(), settings = store.settings()
            let latest = await FeedClient().refresh(previous: store.snapshot(), showChart: settings.appearance.showChart)
            store.save(snapshot: latest)
            let now = Date(), snapshot = store.snapshot()
            // Minute entries update the clock and stale status, not network values or timestamps.
            let entries = (0..<settings.intervalMinutes).map { minute in
                KaspaEntry(date: now.addingTimeInterval(Double(minute * 60)), snapshot: snapshot, appearance: settings.appearance)
            }
            completion(Timeline(entries: entries, policy: .after(now.addingTimeInterval(Double(settings.intervalMinutes * 60)))))
        }
    }
}
struct KaspaWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: KaspaEntry
    private var size: CardSize { family == .systemSmall ? .small : family == .systemLarge ? .large : .medium }
    var body: some View {
        KaspaCard(snapshot: entry.snapshot, appearance: entry.appearance, date: entry.date, size: size)
            .containerBackground(for: .widget) { GlassSurface(snapshot: entry.snapshot, appearance: entry.appearance) }
            .widgetURL(URL(string: "kaspawidget://setup"))
    }
}
@main struct KaspaHomeWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "KaspaHomeWidget", provider: KaspaProvider()) { entry in KaspaWidgetView(entry: entry) }
            .configurationDisplayName("Kaspa Live Widget")
            .description("USD price, DAA score and observed network readings in neon glass.")
            .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
            .contentMarginsDisabled()
    }
}
