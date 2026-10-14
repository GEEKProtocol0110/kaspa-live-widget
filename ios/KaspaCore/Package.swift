// swift-tools-version: 5.9
import PackageDescription

let package = Package(name: "KaspaCore", platforms: [.iOS(.v17), .macOS(.v13)],
    products: [.library(name: "KaspaCore", targets: ["KaspaCore"])],
    targets: [.target(name: "KaspaCore"), .testTarget(name: "KaspaCoreTests", dependencies: ["KaspaCore"])])
