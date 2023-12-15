package com.example.doan_chuyennganh.encryptimport

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.squareup.picasso.Transformation

class BlurTransformation(private val context: Context) : Transformation {

    override fun key(): String {
        return "blur"
    }

    override fun transform(source: Bitmap): Bitmap {
        val width = Math.round(source.width.toFloat() * 0.1f)
        val height = Math.round(source.height.toFloat() * 0.1f)

        val inputBitmap = Bitmap.createScaledBitmap(source, width, height, false)
        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        val rs = RenderScript.create(context)
        val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        val inputAllocation = Allocation.createFromBitmap(rs, inputBitmap)
        val outputAllocation = Allocation.createFromBitmap(rs, outputBitmap)

        blurScript.setRadius(25f) //

        blurScript.setInput(inputAllocation)
        blurScript.forEach(outputAllocation)

        outputAllocation.copyTo(outputBitmap)

        source.recycle()
        inputBitmap.recycle()

        rs.destroy()

        return outputBitmap
    }
}
