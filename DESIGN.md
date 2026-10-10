# Neon glass widget design

The widget is a native cypherpunk data display: a graphite/navy surface, Kaspa mint and cyan illumination, digital monospace typography, and frosted-glass panels containing price, DAA score and a launcher-driven clock. The Kaspa media-kit mark sits at the top; its geometry and mint/white colors are preserved. See [BRAND_ASSETS.md](BRAND_ASSETS.md).

## Layers

1. Rounded native bitmap surface: graphite gradient, subtle grid and static etched scan lines.
2. Actual price observations, mapped by timestamps and values, with cyan bloom, a fine electric core and a terminal point. A flat series stays flat. Missing, invalid or single-point data draws no price line.
3. Translucent glass panels with a bright upper reflection and fine borders.
4. Live TextClock, price/change, DAA score, refresh/freshness labels, and expanded BPS/hashrate readings.

The chart is part of the background rather than a separate strip. Both compact and expanded widgets support it at their declared minimum sizes. History remains optional; new installations enable it, while an explicitly saved off preference is respected. Missing/stale history has visible text and a fuller accessibility description. Long metadata lines ellipsize to protect the primary readings while retaining their text for accessibility.

The frosted-glass look uses gradients, transparency and a pre-rendered chart bloom. It does not request wallpaper access or claim to blur another app’s wallpaper. It works with RemoteViews on API 26+ without a continuously running animation or a new permission. Light appearance uses a pale glass surface and darker teal line/text for readability.

## Rendering limits

Widget option dimensions select a bounded 2x bitmap (maximum 700 × 900 pixels). Native vector logos and TextClock stay separate from that bitmap. Resizing redraws the surface; network scheduling and historical caches retain their existing behavior. No chart is fabricated for decoration.

## Validation

Native Robolectric graphics on API 26 and 34 exercise RemoteViews inflation, clocks, both themes, glass/chart visibility, missing data, flat prices and actual rendering bounds. Full builds assemble debug/release and run Android lint. Preview images are generated from those native layouts with clearly identified sample data; they are not screenshots of current market prices.
