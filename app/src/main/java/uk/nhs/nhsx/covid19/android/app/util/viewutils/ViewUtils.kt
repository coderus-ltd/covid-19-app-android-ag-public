package uk.nhs.nhsx.covid19.android.app.util.viewutils

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.core.widget.NestedScrollView
import com.google.android.material.snackbar.Snackbar

fun showSnackBarShort(container: ViewGroup, message: String) {
    Snackbar.make(container, message, Snackbar.LENGTH_SHORT).show()
}

fun View.gone() {
    visibility = View.GONE
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.setOnSingleClickListener(listener: () -> Unit) {
    setOnClickListener(object : OnSingleClickListener() {
        override fun onSingleClick(v: View?) {
            listener()
        }
    })
}

fun NestedScrollView.smoothScrollToAndThen(
    x: Int,
    y: Int,
    scrollDuration: Int = 250,
    runAfterScroll: () -> Unit
) {
    smoothScrollTo(x, y, scrollDuration)
    postDelayed(runAfterScroll, scrollDuration.toLong())
}

fun LinearLayout.smoothScrollToAndThen(
    x: Int,
    y: Int,
    scrollDuration: Int = 250,
    runAfterScroll: () -> Unit
) {
    scrollTo(x, y)
    postDelayed(runAfterScroll, scrollDuration.toLong())
}

fun ViewGroup.inflate(layoutRes: Int): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, false)
}

val Int.pxToDp: Float
    get() = (this / Resources.getSystem().displayMetrics.density)

val Int.dpToPx: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

fun RadioButton.mirrorSystemLayoutDirection() {
    val oppositeDirection = if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
        View.LAYOUT_DIRECTION_LTR
    } else {
        View.LAYOUT_DIRECTION_RTL
    }
    layoutDirection = oppositeDirection
}
