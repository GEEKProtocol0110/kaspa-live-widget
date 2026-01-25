# Widget Design Mockups

## 2x2 Widget (Compact)

```
┌──────────────────────────┐
│                          │
│         15:42           │
│                          │
│     KASPA (KAS)         │
│                          │
│      $0.1234            │
│                          │
│   Block 45,678,901       │
│                          │
└──────────────────────────┘
```

### Layout Details
- **Size**: 120dp x 120dp (2x2 cells)
- **Background**: Dark (#1a1a1a) with rounded corners (16dp)
- **Border**: 1dp Kaspa green (#00D4AA)
- **Padding**: 8dp

### Text Elements
1. **Time** (top)
   - Size: 18sp
   - Color: White (#FFFFFF)
   - Style: Bold
   - Format: HH:mm

2. **Label** (center-top)
   - Text: "KASPA (KAS)"
   - Size: 12sp
   - Color: Gray (#B0B0B0)

3. **Price** (center)
   - Size: 20sp
   - Color: Kaspa Green (#00D4AA)
   - Style: Bold
   - Format: $X.XXXX

4. **Block Height** (bottom)
   - Label: "Block" (10sp, gray)
   - Value: 12sp, white
   - Format: XXX,XXX (comma-separated)

---

## 4x2 Widget (Extended)

```
┌─────────────────────────────────────────────┐
│                                             │
│                 15:42                       │
│                                             │
│             KASPA (KAS)                     │
│                                             │
│              $0.1234                        │
│                                             │
│  ┌──────────────┬──────────────┐            │
│  │    Block     │     BPS      │            │
│  │  45,678,901  │   1.0 BPS    │            │
│  └──────────────┴──────────────┘            │
│                                             │
│            Hashrate                         │
│          156.23 PH/s                        │
│                                             │
└─────────────────────────────────────────────┘
```

### Layout Details
- **Size**: 250dp x 120dp (4x2 cells)
- **Background**: Dark (#1a1a1a) with rounded corners (16dp)
- **Border**: 1dp Kaspa green (#00D4AA)
- **Padding**: 12dp

### Text Elements
1. **Time** (top)
   - Size: 18sp
   - Color: White (#FFFFFF)
   - Style: Bold
   - Format: HH:mm

2. **Label** (center-top)
   - Text: "KASPA (KAS)"
   - Size: 14sp
   - Color: Gray (#B0B0B0)

3. **Price** (center)
   - Size: 24sp
   - Color: Kaspa Green (#00D4AA)
   - Style: Bold
   - Format: $X.XXXX

4. **Network Stats Row** (2 columns)
   - **Block Height**:
     - Label: "Block" (10sp, gray)
     - Value: 13sp, white, bold
     - Format: XXX,XXX
   - **BPS**:
     - Label: "BPS" (10sp, gray)
     - Value: 13sp, white, bold
     - Format: X.X BPS

5. **Hashrate** (bottom)
   - Label: "Hashrate" (10sp, gray)
   - Value: 13sp, white, bold
   - Format: Auto-scaled (PH/s, TH/s, etc.)

---

## Color Palette

### Primary Colors
- **Background**: `#CC1a1a1a` (Dark with 80% opacity)
- **Border**: `#00D4AA` (Kaspa Brand Green)
- **Primary Text**: `#FFFFFF` (White)
- **Secondary Text**: `#B0B0B0` (Gray)
- **Accent**: `#00D4AA` (Kaspa Green for price)

### Text Colors by Element
```
Element         | Color    | Usage
----------------|----------|------------------
Time            | #FFFFFF  | Main time display
Label           | #B0B0B0  | Section labels
Price           | #00D4AA  | KAS price (highlight)
Block Height    | #FFFFFF  | Network data
BPS             | #FFFFFF  | Network data
Hashrate        | #FFFFFF  | Network data
Stat Labels     | #B0B0B0  | "Block", "BPS", etc.
```

---

## Typography

### Font Family
- System default (Roboto on most devices)

### Font Sizes
```
Element         | Size  | Weight
----------------|-------|-------
Time            | 18sp  | Bold
Main Label      | 12sp  | Regular
Main Label (L)  | 14sp  | Regular
Price (S)       | 20sp  | Bold
Price (L)       | 24sp  | Bold
Stat Labels     | 10sp  | Regular
Stat Values (S) | 12sp  | Bold
Stat Values (L) | 13sp  | Bold
```

---

## Spacing & Layout

### 2x2 Widget Spacing
```
┌─────────────────────── 8dp padding ───────────────────────┐
│                                                            │
│  Time                          (marginBottom: 4dp)         │
│  Label                         (marginBottom: 2dp)         │
│  Price                         (marginBottom: 8dp)         │
│  Block Height                  (centered)                  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

### 4x2 Widget Spacing
```
┌─────────────────────── 12dp padding ──────────────────────┐
│                                                            │
│  Time                          (marginBottom: 4dp)         │
│  Label                         (marginBottom: 2dp)         │
│  Price                         (marginBottom: 8dp)         │
│  Network Stats Row             (marginBottom: 4dp)         │
│  Hashrate                      (centered)                  │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## States

### Normal State
- All data populated
- Full color display
- Regular opacity

### Loading State (First Time)
```
┌──────────────────────────┐
│                          │
│         15:42           │
│                          │
│     KASPA (KAS)         │
│                          │
│         $--             │
│                          │
│       Block --           │
│                          │
└──────────────────────────┘
```

### Error State (No Data)
- Shows "--" for missing values
- Maintains structure
- Shows last known time
- Price shows "$--"
- Block shows "--"
- Hashrate shows "N/A"

### Cached Data State
- Normal display
- Uses last successful fetch
- Age: < 14 minutes

---

## Interaction

### User Actions
- **Tap**: No action (future: open Kaspa explorer)
- **Long Press**: Standard widget options (Remove, Resize)
- **Resize**: Supports horizontal/vertical resizing

### Update Behavior
- **Automatic**: Every 15 minutes
- **Manual**: Not supported (no refresh button)
- **On Screen Wake**: Uses cached data, waits for schedule

---

## Accessibility

### Content Descriptions
- Time: "Current time: 15:42"
- Price: "Kaspa price: 0.1234 dollars"
- Block: "Block height: 45,678,901"
- BPS: "Blocks per second: 1.0"
- Hashrate: "Network hashrate: 156.23 petahashes per second"

### Contrast Ratios
- White on Dark: 15.6:1 (WCAG AAA)
- Gray on Dark: 4.8:1 (WCAG AA)
- Green on Dark: 7.2:1 (WCAG AA)

### Text Sizing
- All text uses sp (scalable pixels)
- Respects system font size settings
- Minimum touch target: N/A (widget is view-only)

---

## Implementation Notes

### RemoteViews Limitations
- Limited view types (TextView, ImageView, etc.)
- No custom fonts (uses system default)
- No animations
- No click listeners (in current implementation)

### Future Visual Enhancements
1. Add Kaspa logo icon
2. Price trend indicator (↑/↓)
3. Color-coded price changes (green/red)
4. Mini chart for price history
5. Different themes (light/dark)
6. Custom backgrounds
