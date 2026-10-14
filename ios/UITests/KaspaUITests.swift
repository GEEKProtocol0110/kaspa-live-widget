import XCTest

final class KaspaUITests: XCTestCase {
    func testLaunchAndSaveLookWithoutChangingAppliedAppearance() {
        let app = XCUIApplication(); app.launch()
        XCTAssertTrue(app.staticTexts["Kaspa Live Widget"].waitForExistence(timeout: 15))
        XCTAssertTrue(app.staticTexts["kaspa-price"].exists)
        let before = XCTAttachment(screenshot: app.screenshot()); before.name = "iPhone-medium-preview"; before.lifetime = .keepAlways; add(before)
        let field = app.textFields["look-name"]
        for _ in 0..<8 { if field.isHittable { break }; app.swipeUp() }
        XCTAssertTrue(field.isHittable); field.tap(); field.typeText("My neon look\n")
        let save = app.buttons["save-look"]
        if !save.isHittable { app.swipeUp() }
        save.tap()
        XCTAssertTrue(app.buttons["My neon look"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["settings-feedback"].label.contains("Look saved"))
        app.terminate(); app.launch()
        for _ in 0..<8 { if app.buttons["My neon look"].isHittable { break }; app.swipeUp() }
        XCTAssertTrue(app.buttons["My neon look"].isHittable)
        app.buttons["My neon look"].tap()
        XCTAssertTrue(app.staticTexts["settings-feedback"].label.contains("preview"))
        let after = XCTAttachment(screenshot: app.screenshot()); after.name = "iPhone-saved-looks"; after.lifetime = .keepAlways; add(after)
    }
}
