import SwiftUI
import WidgetKit
import KaspaCore
import UIKit

@MainActor final class WidgetModel: ObservableObject {
    @Published var snapshot = Snapshot()
    @Published var saved = Settings()
    @Published var refreshing = false
    private let store = SharedStore()
    init() { snapshot = store.snapshot(); saved = store.settings() }
    func apply(_ settings: Settings) {
        store.save(settings: settings); saved = store.settings()
        WidgetCenter.shared.reloadTimelines(ofKind: "KaspaHomeWidget")
    }
    func refresh() async {
        guard !refreshing else { return }
        refreshing = true; defer { refreshing = false }
        let latest = await FeedClient().refresh(previous: store.snapshot(), showChart: saved.appearance.showChart)
        store.save(snapshot: latest); snapshot = store.snapshot()
        WidgetCenter.shared.reloadTimelines(ofKind: "KaspaHomeWidget")
    }
}

@main struct KaspaWidgetApp: App {
    @StateObject private var model = WidgetModel()
    var body: some Scene { WindowGroup { SetupView().environmentObject(model) } }
}

struct SetupView: View {
    @EnvironmentObject private var model: WidgetModel
    @State private var draft = Settings()
    @State private var size: CardSize = .medium
    @State private var name = ""
    @State private var feedback = ""
    @State private var loaded = false
    @State private var help = false
    private var changed: Bool { draft != model.saved }
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    HStack {
                        Image("KaspaMark").resizable().scaledToFit().frame(width: 36, height: 36)
                        VStack(alignment: .leading) { Text("Kaspa Live Widget").font(.title2.bold()); Text("Your iPhone. Your Kaspa.").font(.subheadline).foregroundStyle(.secondary) }
                    }
                    VStack(alignment: .leading, spacing: 10) {
                        Text("YOUR WIDGET").font(.caption.bold()).foregroundStyle(.mint)
                        Picker("Preview size", selection: $size) {
                            Text("Small").tag(CardSize.small); Text("Medium").tag(CardSize.medium); Text("Large").tag(CardSize.large)
                        }.pickerStyle(.segmented)
                        KaspaCard(snapshot: model.snapshot, appearance: draft.appearance, date: Date(), size: size)
                            .frame(height: size == .large ? 340 : size == .small ? 165 : 180)
                            .frame(maxWidth: size == .small ? 180 : .infinity)
                            .background { GlassSurface(snapshot: model.snapshot, appearance: draft.appearance) }
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                            .overlay { RoundedRectangle(cornerRadius: 24).stroke(Color(hex: draft.appearance.resolved.accent).opacity(0.5), lineWidth: 1) }
                        Text("Preview uses saved readings. Changing appearance does not fetch a price.").font(.caption).foregroundStyle(.secondary)
                    }
                    GroupBox("Make it yours") {
                        VStack(alignment: .leading, spacing: 14) {
                            Picker("Appearance", selection: $draft.appearance.theme) {
                                Text("Neon dark").tag("dark"); Text("Frosted light").tag("light"); Text("X-ray").tag("xray"); Text("Custom colors").tag("custom")
                            }
                            if draft.appearance.theme == "custom" {
                                colorPicker("Background", \.background); colorPicker("Text", \.text); colorPicker("Accent", \.accent)
                                colorPicker("Clock", \.clock); colorPicker("Price", \.price); colorPicker("Chart", \.chart); colorPicker("Network", \.network)
                            }
                            Toggle("24-hour chart", isOn: $draft.appearance.showChart)
                            Text("Chart intensity · \(Int(draft.appearance.intensity * 100))%").font(.subheadline)
                            Slider(value: $draft.appearance.intensity, in: 0...1).accessibilityLabel("Chart intensity")
                            Picker("Refresh request", selection: $draft.intervalMinutes) { ForEach([15, 30, 60], id: \.self) { Text("\($0) minutes").tag($0) } }
                            Text("Apple schedules widget refreshes; this interval is a request. BPS averages two successful samples and may be unavailable after a long gap.").font(.caption).foregroundStyle(.secondary)
                            Button(changed ? "Apply settings" : "Settings applied") { model.apply(draft); draft = model.saved; feedback = "Your widgets will use this look on their next reload." }
                                .buttonStyle(.borderedProminent).disabled(!changed).accessibilityIdentifier("apply-settings")
                        }.padding(.top, 8)
                    }
                    GroupBox("Saved looks") {
                        VStack(alignment: .leading, spacing: 12) {
                            TextField("Look name", text: $name).textFieldStyle(.roundedBorder).accessibilityIdentifier("look-name")
                            Button("Save this look") {
                                guard draft.saveLook(name: name) else { feedback = "Use a name of 1–32 characters. At 20 looks, replace a name or delete one."; return }
                                // Store named drafts without applying the current draft appearance.
                                var saved = model.saved; saved.looks = draft.looks; model.apply(saved)
                                name = ""; feedback = "Look saved. Apply settings to update your widgets."
                            }.accessibilityIdentifier("save-look")
                            ForEach(draft.looks) { look in
                                HStack {
                                    Button(look.name) { draft.appearance = look.appearance; feedback = "Look loaded as a preview. Tap Apply settings to use it." }
                                    Spacer()
                                    Button("Delete", role: .destructive) {
                                        draft.looks.removeAll { $0.id == look.id }
                                        var saved = model.saved; saved.looks = draft.looks; model.apply(saved)
                                    }.accessibilityLabel("Delete \(look.name)")
                                }
                            }
                            Text("\(draft.looks.count)/20 looks saved on this device.").font(.caption).foregroundStyle(.secondary)
                        }.padding(.top, 8)
                    }
                    if !feedback.isEmpty { Text(feedback).font(.footnote).foregroundStyle(.mint).accessibilityIdentifier("settings-feedback") }
                    GroupBox("Saved readings") {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(model.snapshot.status(at: Date()))
                            age("Price", model.snapshot.quote?.time); age("Network", model.snapshot.network?.time); age("Hashrate", model.snapshot.hashrate?.time)
                            Text(model.snapshot.quote?.source ?? "Price source unavailable").font(.caption).foregroundStyle(.secondary)
                            Button { Task { await model.refresh() } } label: {
                                if model.refreshing { ProgressView().frame(maxWidth: .infinity) } else { Label("Refresh readings", systemImage: "arrow.clockwise") }
                            }.buttonStyle(.bordered).disabled(model.refreshing).accessibilityIdentifier("refresh-readings")
                        }.frame(maxWidth: .infinity, alignment: .leading).padding(.top, 8)
                    }
                    DisclosureGroup("Add to your home screen & data sources", isExpanded: $help) {
                        Text("Touch and hold the home screen, choose Edit → Add Widget, search Kaspa Live Widget, then choose a size. Tap a widget to open this app.\n\nUSD prices: CoinGecko with a labeled KuCoin KAS/USDT approximation fallback. Chart: actual CoinGecko observations; no synthetic line when unavailable. DAA score and hashrate: Kaspa mainnet REST. BPS is an average across successful network samples.\n\nThis community app is provided by Geek Protocol and is not affiliated with Kaspa or a price provider.").font(.footnote).foregroundStyle(.secondary).padding(.top, 8)
                    }
                    Link("Geek Protocol", destination: URL(string: "https://www.geekprotocol.xyz")!)
                    Link("Privacy policy", destination: URL(string: "https://www.geekprotocol.xyz/tools/kaspa-widget/privacy/")!)
                }.padding(20)
            }.background(Color(hex: "050C18")).preferredColorScheme(.dark).tint(.mint)
        }.task {
            if !loaded { draft = model.saved; loaded = true; await model.refresh() }
        }
    }
    private func age(_ label: String, _ date: Date?) -> some View {
        HStack { Text(label); Spacer(); if let date { Text(date, style: .relative) } else { Text("Not saved") } }.font(.caption).foregroundStyle(.secondary)
    }
    private func colorPicker(_ label: String, _ key: WritableKeyPath<Appearance, String>) -> some View {
        ColorPicker(label, selection: Binding(get: { Color(hex: draft.appearance[keyPath: key]) }, set: { value in
            var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, alpha: CGFloat = 0
            guard UIColor(value).getRed(&r, green: &g, blue: &b, alpha: &alpha) else { return }
            draft.appearance[keyPath: key] = String(format: "%02X%02X%02X", Int((r * 255).rounded()), Int((g * 255).rounded()), Int((b * 255).rounded()))
        }), supportsOpacity: false)
    }
}
