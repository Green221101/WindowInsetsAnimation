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

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout.LayoutParams
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.samples.insetsanimation.databinding.FragmentConversationBinding

/**
 * The main entry point for the sample. See [onViewCreated] for more information on how
 * the sample works.
 */
class ConversationFragment : Fragment() {
    private val TAG = "FragmentTag"
    private var _binding: FragmentConversationBinding? = null
    private val binding: FragmentConversationBinding get() = _binding!!

    //patch
    private lateinit var inputMethodManager: InputMethodManager

    private val translateInsetsCallback2 by lazy {
        TranslateDeferringInsetsAnimationCallback2(
            view = binding.content,
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime(),
            // We explicitly allow dispatch to continue down to binding.messageHolder's
            // child views, so that step 2.5 below receives the call
            dispatchMode = WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
        )
    }

    /*
     *  监听RecycleView touch 处理逻辑。主要是隐藏内容
     */
    private val scrollListener by lazy {
        object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val isSoftShowing = isSoftShowing()
                Log.i(TAG, "newState=$newState" + "isSoftShowing=$isSoftShowing")
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    if (isSoftShowing) {
                        //软键盘隐藏
                        translateInsetsCallback2.isBackHide = true
                        requireActivity().currentFocus?.let {
                            inputMethodManager.hideSoftInputFromWindow(
                                it.windowToken,
                                InputMethodManager.RESULT_UNCHANGED_SHOWN
                            )
                        }
                    } else if (binding.content.isVisible) {
                        //content隐藏
                        animationTranslate(false)
                        binding.imageView1.setImageResource(R.drawable.ic_android_gray_24dp)

                    }

                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                //Log.i(LOG_TAG, "dx=$dx,dy=$dy")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputMethodManager =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initView() {
        binding.imageView1.setOnClickListener {
            val inputSoftShowing = isSoftShowing()
            Log.i(TAG, "isSoftShowing=$inputSoftShowing")
            if (inputSoftShowing) {
                //隐藏软键盘
                binding.imageView1.setImageResource(R.drawable.ic_android_blue_24dp)
                translateInsetsCallback2.isBackHide = false
                requireActivity().currentFocus?.let {
                    inputMethodManager.hideSoftInputFromWindow(
                        it.windowToken,
                        InputMethodManager.RESULT_UNCHANGED_SHOWN
                    )
                }

            } else {
                //显示软键盘
                if (binding.content.isVisible) {
                    binding.messageEdittext.requestFocus()
                    inputMethodManager.showSoftInput(
                        binding.messageEdittext, InputMethodManager.SHOW_IMPLICIT
                    )
                    // 显示content
                } else {
                    showImageView()
                }
            }
        }

        binding.messageEdittext.setOnFocusChangeListener { _, isFocus ->
            Log.i(TAG, "editText isFocus:$isFocus")
            if (isFocus) {
                translateInsetsCallback2.isBackHide = true
                binding.imageView1.setImageResource(R.drawable.ic_android_gray_24dp)
            }
        }

//        val observer = binding.root.viewTreeObserver
//        observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener{
//            override fun onGlobalLayout() {
//                val inputSoftShowing = isSoftShowing()
//                Log.i(LOG_TAG, "onGlobalLayout,isSoftShowing = $inputSoftShowing")
//                if(inputSoftShowing){
//                    val softHeight = getSoftHeight()
//                    if(mHeight != softHeight){
//                        setImageViewHeight(softHeight- 130)
//                            mHeight = softHeight
//                    }
//                }
//
//            }
//
//        })
        /*
         * 监听返回按键
         */
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.i(TAG, "back key call back")
                }

            })

        binding.conversationRecyclerview.addOnScrollListener(scrollListener)


    }

    /*
     * 设置content view 高度
     */
    private fun setContentViewHeight(height: Int) {
        Log.i(TAG, "setContentViewHeight=$height")
        val imageParams = binding.content.layoutParams
        imageParams.height = height
        binding.content.layoutParams = imageParams
    }

    private fun showImageView() {
        animationTranslate(true)
        binding.imageView1.setImageResource(R.drawable.ic_android_blue_24dp)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //patch
        initView()
        //patch
        // Set our conversation adapter on the RecyclerView
        binding.conversationRecyclerview.adapter = ConversationAdapter()

        // There are three steps to WindowInsetsAnimations:

        /**
         * 1) Since our Activity has declared `window.setDecorFitsSystemWindows(false)`, we need to
         * handle any [WindowInsetsCompat] as appropriate.
         *
         * Our [RootViewDeferringInsetsCallback] will update our attached view's padding to match
         * the combination of the [WindowInsetsCompat.Type.systemBars], and selectively apply the
         * [WindowInsetsCompat.Type.ime] insets, depending on any ongoing WindowInsetAnimations
         * (see that class for more information).
         */
        val deferringInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime()
        )
        // RootViewDeferringInsetsCallback is both an WindowInsetsAnimation.Callback and an
        // OnApplyWindowInsetsListener, so needs to be set as so.
        ViewCompat.setWindowInsetsAnimationCallback(binding.root, deferringInsetsListener)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, deferringInsetsListener)

        /**
         * 2) The second step is reacting to any animations which run. This can be system driven,
         * such as the user focusing on an EditText and on-screen keyboard (IME) coming on screen,
         * or app driven (more on that in step 3).
         *
         * To react to animations, we set an [android.view.WindowInsetsAnimation.Callback] on any
         * views which we wish to react to inset animations. In this example, we want our
         * EditText holder view, and the conversation RecyclerView to react.
         *
         * We use our [TranslateDeferringInsetsAnimationCallback] class, bundled in this sample,
         * which will automatically move each view as the IME animates.
         *
         * Note about [TranslateDeferringInsetsAnimationCallback], it relies on the behavior of
         * [RootViewDeferringInsetsCallback] on the layout's root view.
         */
        /**
         * 设置输入框实时监听软键盘高度变化
         */
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.messageHolder,
            TranslateDeferringInsetsAnimationCallback(
                view = binding.messageHolder,
                persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
                deferredInsetTypes = WindowInsetsCompat.Type.ime(),
                // We explicitly allow dispatch to continue down to binding.messageHolder's
                // child views, so that step 2.5 below receives the call
                dispatchMode = WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
            )
        )

        /**
         * 设置RecycleView实时监听软键盘高度变化
         */
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.conversationRecyclerview,
            TranslateDeferringInsetsAnimationCallback(
                view = binding.conversationRecyclerview,
                persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
                deferredInsetTypes = WindowInsetsCompat.Type.ime()
            )
        )

        //patch
        /**
         * 输入框下内容view 监听软键盘高度变化
         */
        ViewCompat.setWindowInsetsAnimationCallback(binding.content, translateInsetsCallback2)
        //patch
        /**
         * 2.5) We also want to make sure that our EditText is focused once the IME
         * is animated in, to enable it to accept input. Similarly, if the IME is animated
         * off screen and the EditText is focused, we should clear that focus.
         *
         * The bundled [ControlFocusInsetsAnimationCallback] callback will automatically request
         * and clear focus for us.
         *
         * Since `binding.messageEdittext` is a child of `binding.messageHolder`, this
         * [WindowInsetsAnimationCompat.Callback] will only work if the ancestor view's callback uses the
         * [WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE] dispatch mode, which
         * we have done above.
         */
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.messageEdittext,
            ControlFocusInsetsAnimationCallback(binding.messageEdittext)
        )

        /**
         * 3) The third step is when the app wants to control and drive an inset animation.
         * This is an optional step, but suits many types of input UIs. The example scenario we
         * use in this sample is that the user can drag open the IME, by over-scrolling the
         * conversation RecyclerView. To enable this, we use a [InsetsAnimationLinearLayout] as a
         * root view in our layout which handles this automatically for scrolling views,
         * through nested scrolling.
         *
         * Alternatively, this sample also contains [InsetsAnimationTouchListener],
         * which is a [android.view.View.OnTouchListener] which does similar for non-scrolling
         * views, detecting raw drag events rather than scroll events to open/close the IME.
         *
         * Internally, both [InsetsAnimationLinearLayout] & [InsetsAnimationTouchListener] use a
         * class bundled in this sample called [SimpleImeAnimationController], which simplifies
         * much of the mechanics for controlling a [WindowInsetsAnimationCompat].
         */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 判断软键盘是否弹出
     * 主要是通过判断decorView 和 可见window 高度 不同来判断软键盘是否显示。
     */

    private fun isSoftShowing(): Boolean {
        val screenHeight = requireActivity().window.decorView.height
        val rect = Rect()
        requireActivity().window.decorView.getWindowVisibleDisplayFrame(rect)
        Log.i(TAG, "height=" + screenHeight + "bottom=" + rect.bottom)
        // 这里可以优化
        return screenHeight - rect.bottom > 300
    }

    /*
     *  获取键盘最后高度，目前没有使用。
     */
    private fun getSoftHeight(): Int {
        val screenHeight = requireActivity().window.decorView.height
        val rect = Rect()
        requireActivity().window.decorView.getWindowVisibleDisplayFrame(rect)
        Log.i(TAG, "height=" + screenHeight + "bottom:" + rect.bottom)
        return screenHeight - rect.bottom
    }

    /**
     * 动态显示 or 隐藏 输入法下面content展示
     * 核心逻辑就是实时设置view bottomMargin value
     */
    private fun animationTranslate(isMovingUp: Boolean) {
        val imageHeight = if (binding.content.height > 100) binding.content.height else 700
        Log.i(TAG, "imageHeight=$imageHeight,isMovingUp:$isMovingUp")

        val animation = ValueAnimator().apply {
            if (isMovingUp) {
                setIntValues(imageHeight * (-1), 0)
            } else {
                setIntValues(0, imageHeight * (-1))
            }
            duration = 200
        }
        animation.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                val value = animation.animatedValue as Int
                Log.i(TAG, "value=$value")
                val layoutParams = binding.content.layoutParams as LayoutParams
                layoutParams.bottomMargin = value
                binding.content.layoutParams = layoutParams
            }
        })


        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                Log.i(TAG, "onAnimationStart")
                if (isMovingUp) {
                    val layoutParams = binding.content.layoutParams as LayoutParams
                    layoutParams.bottomMargin = -1 * imageHeight
                    layoutParams.height = imageHeight
                    binding.content.layoutParams = layoutParams
                    binding.content.visibility = View.VISIBLE
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                Log.i(TAG, "onAnimationEnd")
                val layoutParams = binding.content.layoutParams as LayoutParams
                layoutParams.bottomMargin = 0
                binding.content.layoutParams = layoutParams
                if (!isMovingUp) {
                    binding.content.visibility = View.GONE
                }
            }

            override fun onAnimationCancel(animation: Animator) {
                //Log.i(LOG_TAG, "onAnimationCancel")
            }

            override fun onAnimationRepeat(animation: Animator) {
                //Log.i(LOG_TAG, "onAnimationRepeat")
            }

        })

        animation.start()

    }
}
