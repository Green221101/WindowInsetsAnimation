/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.samples.insetsanimation

import android.util.Log
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone

/**
 * A [WindowInsetsAnimationCompat.Callback] which will translate/move the given view during any
 * inset animations of the given inset type.
 *
 * This class works in tandem with [RootViewDeferringInsetsCallback] to support the deferring of
 * certain [WindowInsetsCompat.Type] values during a [WindowInsetsAnimationCompat], provided in
 * [deferredInsetTypes]. The values passed into this constructor should match those which
 * the [RootViewDeferringInsetsCallback] is created with.
 *
 * @param view the view to translate from it's start to end state
 * @param persistentInsetTypes the bitmask of any inset types which were handled as part of the
 * layout
 * @param deferredInsetTypes the bitmask of insets types which should be deferred until after
 * any [WindowInsetsAnimationCompat]s have ended
 * @param dispatchMode The dispatch mode for this callback.
 * See [WindowInsetsAnimationCompat.Callback.getDispatchMode].
 */
class TranslateDeferringInsetsAnimationCallback2(
    private val view: View,
    val persistentInsetTypes: Int,
    val deferredInsetTypes: Int,
    dispatchMode: Int = DISPATCH_MODE_STOP
) : WindowInsetsAnimationCompat.Callback(dispatchMode) {
    private val TAG = "AnimationCallback2"

    /*
     * isBackHide
     * true: 按返回键退出软键盘
     * false：其他按键退出软键盘
     */
    var isBackHide = true
    private var tempBottom = -1
    private var isAnimationUp = false  // false:软键盘隐藏 true:软键盘上升
    private var keyBottomHeight = 0
    private var keyHeight = 0  //软键盘移动时候的高度
    private var keyMaxHeight = 0

    init {
        require(persistentInsetTypes and deferredInsetTypes == 0) {
            "persistentInsetTypes and deferredInsetTypes can not contain any of " +
                    " same WindowInsetsCompat.Type values"
        }
    }

    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ): WindowInsetsAnimationCompat.BoundsCompat {
        Log.i(TAG, "onStart,upperBound=" + bounds.upperBound + ",lowerBound=" + bounds.lowerBound)
        //Log.i(TAG, "millis=" + animation.durationMillis)
        Log.i(TAG, "isBackHide=$isBackHide")
        keyBottomHeight = bounds.upperBound.bottom - bounds.lowerBound.bottom
        return super.onStart(animation, bounds)
    }

    override fun onProgress(
        insets: WindowInsetsCompat,
        runningAnimations: List<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat {
        // onProgress() is called when any of the running animations progress...

        // First we get the insets which are potentially deferred
        val typesInset = insets.getInsets(deferredInsetTypes)
        // Then we get the persistent inset types which are applied as padding during layout
        val otherInset = insets.getInsets(persistentInsetTypes)

        if (tempBottom < 0) {
            tempBottom = typesInset.bottom
        } else {
            val temp = typesInset.bottom - tempBottom
            keyMaxHeight = keyBottomHeight - otherInset.bottom
            keyHeight = typesInset.bottom - otherInset.bottom

            if (temp > 0) {
                isAnimationUp = true;
            } else if (temp < 0) {
                isAnimationUp = false
            }
            tempBottom = typesInset.bottom
        }
        Log.i(TAG, "typesInset=$typesInset,otherInset=$otherInset")
        Log.i(TAG, "isAnimationUp=$isAnimationUp,keyMaxHeight=$keyMaxHeight,keyHeight=$keyHeight")
        // Now that we subtract the two insets, to calculate the difference. We also coerce
        // the insets to be >= 0, to make sure we don't use negative insets.
        val diff = Insets.subtract(typesInset, otherInset).let {
            Insets.max(it, Insets.NONE)
        }

        // The resulting `diff` insets contain the values for us to apply as a translation
        // to the view
        if (isAnimationUp) {
            if (!view.isGone) {
                view.visibility = View.INVISIBLE
                if (keyHeight in 1 until keyMaxHeight) {
                    setViewHeight(keyMaxHeight - keyHeight)
                }
            }
        } else {
            if (isBackHide) {
                view.translationX = (diff.left - diff.right).toFloat()
                view.translationY = (diff.top - diff.bottom).toFloat()
            } else {
                if (keyHeight > 0) {
                    view.visibility = View.INVISIBLE
                    setViewHeight(keyMaxHeight - keyHeight)
                } else {
                    setViewHeight(keyMaxHeight)
                }
            }

        }

        return insets
    }


    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        // Once the animation has ended, reset the translation values
        Log.i(TAG, "onEnd")
        view.translationX = 0f
        view.translationY = 0f
        if (!isAnimationUp && !isBackHide) {
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }


    private fun setViewHeight(height: Int) {
        Log.i(TAG, "setViewHeight=$height")
        val imageParams = view.layoutParams
        imageParams.height = height
        view.layoutParams = imageParams
    }
}
