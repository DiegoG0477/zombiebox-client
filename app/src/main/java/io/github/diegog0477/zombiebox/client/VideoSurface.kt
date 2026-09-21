package io.github.diegog0477.zombiebox.client

import android.content.Context
import android.view.SurfaceView

/** Letterbox within the available viewport, including miniplayer transitions. */
class VideoSurface(context: Context) : SurfaceView(context) {
    private var videoWidth = 0
    private var videoHeight = 0
    fun setVideoSize(width: Int, height: Int) {
        videoWidth = width; videoHeight = height; requestLayout()
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = MeasureSpec.getSize(widthMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)
        if (videoWidth > 0 && videoHeight > 0) {
            if (width.toLong() * videoHeight > height.toLong() * videoWidth) width = (height.toLong() * videoWidth / videoHeight).toInt()
            else height = (width.toLong() * videoHeight / videoWidth).toInt()
        }
        setMeasuredDimension(width, height)
    }
}
