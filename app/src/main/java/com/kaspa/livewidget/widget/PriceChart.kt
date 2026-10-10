package com.kaspa.livewidget.widget

import android.graphics.*
import com.kaspa.livewidget.data.PricePoint

/** Draws actual timestamped observations. Empty data never produces a price line. */
internal object PriceChart {
    private fun observations(points: List<PricePoint>) = points
        .filter { it.timestamp > 0 && it.price.isFinite() && it.price > 0 }
        .sortedBy { it.timestamp }.distinctBy { it.timestamp }

    fun bitmap(points: List<PricePoint>, color: Int, width: Int = 600, height: Int = 100): Bitmap {
        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawLine(Canvas(image), observations(points), color, width.toFloat(), 6f, height-6f, false)
        return image
    }

    /** Full-widget, bounded-size surface: grid and bloom under the translucent panels. */
    fun glass(points: List<PricePoint>, light: Boolean, width: Int, height: Int): Bitmap {
        require(width in 120..700 && height in 120..900)
        val image = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        val w = width.toFloat(); val h = height.toFloat()
        val shape = Path().apply { addRoundRect(RectF(1f,1f,w-1,h-1),24f,24f,Path.Direction.CW) }
        canvas.save(); canvas.clipPath(shape)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f,0f,w,h,
            if(light) intArrayOf(0xFFF1FBFA.toInt(),0xFFB7D9DD.toInt(),0xFFDCE7F4.toInt())
            else intArrayOf(0xFF050C18.toInt(),0xFF122835.toInt(),0xFF10182F.toInt()),null,Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,w,h,paint)
        paint.shader = RadialGradient(w*.8f,h*.2f,w*.9f,
            if(light) 0x3051B8C3 else 0x3822CDAF, Color.TRANSPARENT,Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,w,h,paint)
        paint.shader = null; paint.color = if(light) 0x14345568 else 0x1846F3D9
        paint.strokeWidth = 1f
        val gap = w/9f
        var x = 0f
        while(x<w) { canvas.drawLine(x,0f,x,h,paint); x+=gap }
        var y = 0f
        while(y<h) { canvas.drawLine(0f,y,w,y,paint); y+=gap }
        // Thin etched scan lines, never animated (no extra network/battery load).
        paint.color = if(light) 0x08607685 else 0x08C2F6FF.toInt()
        y=0f
        while(y<h) { canvas.drawLine(0f,y,w,y,paint); y+=5f }
        drawLine(canvas,observations(points), if(light) 0xFF087F8B.toInt() else 0xFF64FFE2.toInt(),w,h*.27f,h*.72f,true)
        canvas.restore()
        paint.shader = LinearGradient(0f,0f,w,h,
            if(light) intArrayOf(0xCC388D9A.toInt(),0x4422859B,0xAA517BA1.toInt())
            else intArrayOf(0xDD9BFFF1.toInt(),0x3348B5B3,0xAA638FE0.toInt()),null,Shader.TileMode.CLAMP)
        paint.style=Paint.Style.STROKE; paint.strokeWidth=2f
        canvas.drawPath(shape,paint)
        return image
    }

    private fun drawLine(canvas: Canvas, points: List<PricePoint>, color: Int,
                         width: Float, top: Float, bottom: Float, neon: Boolean) {
        if(points.size<2 || points.last().timestamp<=points.first().timestamp) return
        val low=points.minOf { it.price }; val high=points.maxOf { it.price }
        val path=Path()
        var lastX=0f; var lastY=0f
        points.forEachIndexed { index,p ->
            val x=12f+((p.timestamp-points.first().timestamp).toDouble()/
                (points.last().timestamp-points.first().timestamp)*(width-24)).toFloat()
            val y=if(high==low) (top+bottom)/2 else top+((high-p.price)/(high-low)*(bottom-top)).toFloat()
            if(index==0) path.moveTo(x,y) else path.lineTo(x,y)
            lastX=x;lastY=y
        }
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.STROKE; strokeJoin=Paint.Join.ROUND; strokeCap=Paint.Cap.ROUND }
        if(neon) {
            paint.color=color;paint.alpha=65;paint.strokeWidth=16f;paint.maskFilter=BlurMaskFilter(10f,BlurMaskFilter.Blur.NORMAL)
            canvas.drawPath(path,paint)
            paint.alpha=140;paint.strokeWidth=7f;paint.maskFilter=BlurMaskFilter(4f,BlurMaskFilter.Blur.NORMAL)
            canvas.drawPath(path,paint)
        }
        paint.maskFilter=null;paint.color=color;paint.strokeWidth=if(neon) 2.5f else 3f
        canvas.drawPath(path,paint)
        if(neon) {
            paint.color=Color.WHITE;paint.alpha=170;paint.strokeWidth=.8f
            canvas.drawPath(path,paint)
            paint.style=Paint.Style.FILL;paint.color=color;paint.alpha=90;paint.maskFilter=BlurMaskFilter(5f,BlurMaskFilter.Blur.NORMAL)
            canvas.drawCircle(lastX,lastY,7f,paint)
            paint.maskFilter=null;paint.alpha=255;canvas.drawCircle(lastX,lastY,3f,paint)
        }
    }
}
