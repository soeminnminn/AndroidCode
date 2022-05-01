package com.s16.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.Scroller
import androidx.appcompat.widget.AppCompatImageView
import androidx.collection.LruCache
import com.s16.android.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.lang.reflect.Field
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


class ImageMap : AppCompatImageView {

    // mFitImageToScreen
    // if true - initial image resized to fit the screen, aspect ratio may be broken
    // if false- initial image resized so that no empty screen is visible, aspect ratio maintained
    //           image size will likely be larger than screen

    // mFitImageToScreen
    // if true - initial image resized to fit the screen, aspect ratio may be broken
    // if false- initial image resized so that no empty screen is visible, aspect ratio maintained
    //           image size will likely be larger than screen
    // by default, this is true
    private var mFitImageToScreen = true

    // For certain images, it is best to always resize using the original
    // image bits. This requires keeping the original image in memory along with the
    // current sized version and thus takes extra memory.
    // If you always want to resize using the original, set mScaleFromOriginal to true
    // If you want to use less memory, and the image scaling up and down repeatedly
    // does not blur or loose quality, set mScaleFromOriginal to false

    // For certain images, it is best to always resize using the original
    // image bits. This requires keeping the original image in memory along with the
    // current sized version and thus takes extra memory.
    // If you always want to resize using the original, set mScaleFromOriginal to true
    // If you want to use less memory, and the image scaling up and down repeatedly
    // does not blur or loose quality, set mScaleFromOriginal to false
    // by default, this is false
    private var mScaleFromOriginal = false

    // mMaxSize controls the maximum zoom size as a multiplier of the initial size.
    // Allowing size to go too large may result in memory problems.
    //  set this to 1.0f to disable resizing
    // by default, this is 1.5f
    private val defaultMaxSize = 1.5f
    private var mMaxSize = 1.5f

    /* Touch event handling variables */
    private var mVelocityTracker: VelocityTracker? = null

    private var mTouchSlop = 0
    private var mMinimumVelocity = 0
    private var mMaximumVelocity = 0

    private var mScroller: Scroller? = null

    private var mIsBeingDragged = false

    @SuppressLint("UseSparseArrays")
    private val mTouchPoints: HashMap<Int, TouchPoint> = HashMap()
    private var mMainTouch: TouchPoint? = null
    private var mPinchTouch: TouchPoint? = null

    /* Pinch zoom */
    private var mInitialDistance = 0f
    private var mZoomEstablished = false
    private var mLastDistanceChange = 0
    private var mZoomPending = false

    /* Paint objects for drawing info bubbles */
    private var textPaint: Paint? = null
    private var textOutlinePaint: Paint? = null
    private var bubblePaint: Paint? = null
    private var bubbleShadowPaint: Paint? = null

    private var mBubbleTextColor = -0x1000000
    private var mBubbleBkgColor = -0x1

    /*
	 * Bitmap handling
	 */
    private var mImage: Bitmap? = null
    private var mOriginal: Bitmap? = null

    // Info about the bitmap (sizes, scroll bounds)
    // initial size
    private var mImageHeight = 0
    private var mImageWidth = 0
    private var mAspect = 0f

    // scaled size
    private var mExpandWidth = 0
    private var mExpandHeight = 0

    // the right and bottom edges (for scroll restriction)
    private var mRightBound = 0
    private var mBottomBound = 0

    // the current zoom scaling (X and Y kept separate)
    private var mResizeFactorX = 0f
    private var mResizeFactorY = 0f

    // minimum height/width for the image
    private var mMinWidth = -1
    private var mMinHeight = -1

    // maximum height/width for the image
    private var mMaxWidth = -1
    private var mMaxHeight = -1

    // the position of the top left corner relative to the view
    private var mScrollTop = 0
    private var mScrollLeft = 0

    // view height and width
    private var mViewHeight = -1
    private var mViewWidth = -1

    /*
	 * containers for the image map areas
	 * using SparseArray<Area> instead of HashMap for the sake of performance
	 */
    private val mAreaList: ArrayList<Area> = ArrayList<Area>()
    private val mIdToArea: SparseArray<Area> = SparseArray<Area>()

    // click handler list
    private var mCallbackList: ArrayList<OnImageMapClickedHandler>? = null

    // list of open info bubbles
    private val mBubbleMap: SparseArray<Bubble> = SparseArray<Bubble>()

    private var mMapXmlResId = 0

    // changed this from local variable to class field
    private var mapName: String? = null

    // accounting for screen density
    private var densityFactor = 0f

    //possible to reduce memory consumption
    private var options: BitmapFactory.Options? = null

    /*
	 * A class to track touches
	 */
    class TouchPoint(id: Int) {
        val _id = id
        var _x = 0f
        var _y = 0f

        fun getTrackingPointer(): Int {
            return _id
        }

        fun setPosition(x: Float, y: Float) {
            if (_x != x || _y != y) {
                _x = x
                _y = y
            }
        }

        fun getX(): Float {
            return _x
        }

        fun getY(): Float {
            return _y
        }
    }

    /*
     * Begin map area support
     */
    /**
     *  Area is abstract Base for tappable map areas
     *   descendants provide hit test and focal point
     */
    abstract inner class Area(id: Int, name: String?) {
        var _id = id
        var _name: String? = name
        var _values: HashMap<String, String>? = null
        var _decoration: Bitmap? = null

        open fun getId(): Int {
            return _id
        }

        open fun getName(): String? {
            return _name
        }

        // all xml values for the area are passed to the object
        // the default impl just puts them into a hashmap for
        // retrieval later
        open fun addValue(key: String?, value: String?) {
            if (_values == null) {
                _values = HashMap()
            }
            _values!![key!!] = value!!
        }

        open fun getValue(key: String?): String? {
            var value: String? = null
            if (_values != null) {
                value = _values!![key]
            }
            return value
        }

        // a method for setting a simple decorator for the area
        open fun setBitmap(b: Bitmap) {
            _decoration = b
        }

        // an onDraw is set up to provide an extensible way to
        // decorate an area.  When drawing remember to take the
        // scaling and translation into account
        open fun onDraw(canvas: Canvas) {
            if (_decoration != null) {
                val x: Float = getOriginX() * mResizeFactorX + mScrollLeft - 17
                val y: Float = getOriginY() * mResizeFactorY + mScrollTop - 17
                canvas.drawBitmap(_decoration!!, x, y, null)
            }
        }

        abstract fun isInArea(x: Float, y: Float): Boolean
        abstract fun getOriginX(): Float
        abstract fun getOriginY(): Float
    }

    /**
     * Rectangle Area
     */
    private inner class RectArea(
        id: Int,
        name: String?,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) : Area(id, name) {

        var _left = left
        var _top = top
        var _right = right
        var _bottom = bottom

        override fun isInArea(x: Float, y: Float): Boolean {
            var ret = false
            if (x > _left && x < _right) {
                if (y > _top && y < _bottom) {
                    ret = true
                }
            }
            return ret
        }

        override fun getOriginX(): Float {
            return _left
        }

        override fun getOriginY(): Float {
            return _top
        }
    }

    /**
     * Polygon area
     */
    // split the list of coordinates into points of the
    // polygon and compute a bounding box

    // add point zero to the end to make
    // computing area and centroid easier
    private inner class PolyArea(id: Int, name: String?, coords: String) : Area(id, name) {
        var xpoints = ArrayList<Int>()
        var ypoints = ArrayList<Int>()

        // centroid point for this poly
        var _x = 0f
        var _y = 0f

        // number of points (don't rely on array size)
        var _points = 0

        // bounding box
        var top = -1
        var bottom = -1
        var left = -1
        var right = -1

        init {
            val v = coords.split(",").toTypedArray()
            var i = 0
            while (i + 1 < v.size) {
                val x = v[i].toInt()
                val y = v[i + 1].toInt()
                xpoints.add(x)
                ypoints.add(y)
                top = if (top == -1) y else min(top, y)
                bottom = if (bottom == -1) y else max(bottom, y)
                left = if (left == -1) x else min(left, x)
                right = if (right == -1) x else max(right, x)
                i += 2
            }
            _points = xpoints.size
            xpoints.add(xpoints[0])
            ypoints.add(ypoints[0])
            computeCentroid()
        }

        /**
         * area() and computeCentroid() are adapted from the implementation
         * of polygon.java  published from a princeton case study
         * The study is here: http://introcs.cs.princeton.edu/java/35purple/
         * The polygon.java source is here: http://introcs.cs.princeton.edu/java/35purple/Polygon.java.html
         */
        // return area of polygon
        fun area(): Double {
            var sum = 0.0
            for (i in 0 until _points) {
                sum = sum + xpoints[i] * ypoints[i + 1] - ypoints[i] * xpoints[i + 1]
            }
            sum *= 0.5
            return abs(sum)
        }

        // compute the centroid of the polygon
        fun computeCentroid() {
            var cx = 0.0
            var cy = 0.0
            for (i in 0 until _points) {
                cx += (xpoints[i] + xpoints[i + 1]) * (ypoints[i] * xpoints[i + 1] - xpoints[i] * ypoints[i + 1])
                cy += (ypoints[i] + ypoints[i + 1]) * (ypoints[i] * xpoints[i + 1] - xpoints[i] * ypoints[i + 1])
            }
            cx /= 6 * area()
            cy /= 6 * area()
            _x = abs(cx.toInt()).toFloat()
            _y = abs(cy.toInt()).toFloat()
        }


        override fun getOriginX(): Float {
            return _x
        }

        override fun getOriginY(): Float {
            return _y
        }

        /**
         * This is a java port of the
         * W. Randolph Franklin algorithm explained here
         * http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
         */
        override fun isInArea(testx: Float, testy: Float): Boolean {
            var c = false
            var i = 0
            var j = _points - 1
            while (i < _points) {
                if (ypoints[i] > testy != ypoints[j] > testy &&
                    testx < (xpoints[j] - xpoints[i]) * (testy - ypoints[i]) / (ypoints[j] - ypoints[i]) + xpoints[i]
                ) c = !c
                j = i++
            }
            return c
        }
    }

    /**
     * Circle Area
     */
    private inner class CircleArea(id: Int, name: String?, x: Float, y: Float, radius: Float) :
        Area(id, name) {
        var _x = x
        var _y = y
        var _radius = radius

        override fun isInArea(x: Float, y: Float): Boolean {
            var ret = false
            val dx = _x - x
            val dy = _y - y

            // if tap is less than radius distance from the center
            val d = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (d < _radius) {
                ret = true
            }
            return ret
        }

        override fun getOriginX(): Float {
            return _x
        }

        override fun getOriginY(): Float {
            return _y
        }
    }

    /**
     * information bubble class
     */
    private inner class Bubble {
        var _a: Area? = null
        var _text: String? = null
        var _x = 0f
        var _y = 0f
        var _h = 0
        var _w = 0
        var _baseline = 0
        var _top = 0f
        var _left = 0f

        constructor(text: String, x: Float, y: Float) {
            init(text, x, y)
        }

        constructor(text: String, areaId: Int) {
            _a = mIdToArea.get(areaId)
            if (_a != null) {
                val x = _a!!.getOriginX()
                val y = _a!!.getOriginY()
                init(text, x, y)
            }
        }

        fun init(text: String, x: Float, y: Float) {
            _text = text
            _x = x * mResizeFactorX
            _y = y * mResizeFactorY
            val bounds = Rect()
            textPaint?.textScaleX = 1.0f
            textPaint?.getTextBounds(text, 0, _text!!.length, bounds)
            _h = bounds.bottom - bounds.top + 20
            _w = bounds.right - bounds.left + 20
            if (_w > mViewWidth) {
                // too long for the display width...need to scale down
                val newscale = mViewWidth.toFloat() / _w.toFloat()
                textPaint?.textScaleX = newscale
                textPaint?.getTextBounds(text, 0, _text!!.length, bounds)
                _h = bounds.bottom - bounds.top + 20
                _w = bounds.right - bounds.left + 20
            }
            _baseline = _h - bounds.bottom
            _left = _x - _w / 2
            _top = _y - _h - 30

            // try to keep the bubble on screen
            if (_left < 0) {
                _left = 0f
            }
            if (_left + _w > mExpandWidth) {
                _left = (mExpandWidth - _w).toFloat()
            }
            if (_top < 0) {
                _top = _y + 20
            }
        }

        fun isInArea(x: Float, y: Float): Boolean {
            var ret = false
            if (x > _left && x < _left + _w) {
                if (y > _top && y < _top + _h) {
                    ret = true
                }
            }
            return ret
        }

        @SuppressLint("DrawAllocation")
        fun onDraw(canvas: Canvas) {
            if (_a != null) {
                // Draw a shadow of the bubble
                var l: Float = _left + mScrollLeft + 4
                var t: Float = _top + mScrollTop + 4
                canvas.drawRoundRect(RectF(l, t, l + _w, t + _h), 20.0f, 20.0f, bubbleShadowPaint!!)
                var path = Path()
                var ox: Float = _x + mScrollLeft + 1
                var oy: Float = _y + mScrollTop + 1
                var yoffset = -35
                if (_top > _y) {
                    yoffset = 35
                }
                // draw shadow of pointer to origin
                path.moveTo(ox, oy)
                path.lineTo(ox - 5, oy + yoffset)
                path.lineTo(ox + 5 + 4, oy + yoffset)
                path.lineTo(ox, oy)
                path.close()
                canvas.drawPath(path, bubbleShadowPaint!!)

                // draw the bubble
                l = _left + mScrollLeft
                t = _top + mScrollTop
                canvas.drawRoundRect(RectF(l, t, l + _w, t + _h), 20.0f, 20.0f, bubblePaint!!)
                path = Path()
                ox = _x + mScrollLeft
                oy = _y + mScrollTop
                yoffset = -35
                if (_top > _y) {
                    yoffset = 35
                }
                // draw pointer to origin
                path.moveTo(ox, oy)
                path.lineTo(ox - 5, oy + yoffset)
                path.lineTo(ox + 5, oy + yoffset)
                path.lineTo(ox, oy)
                path.close()
                canvas.drawPath(path, bubblePaint!!)

                // draw the message
                canvas.drawText(_text!!, l + _w / 2, t + _baseline - 10, textPaint!!)
            }
        }

        fun onTapped() {
            // bubble was tapped, notify listeners
            if (mCallbackList != null) {
                for (h in mCallbackList!!) {
                    h.onBubbleClicked(_a!!.getId())
                }
            }
        }
    }

    /**
     * Map tapped callback interface
     */
    interface OnImageMapClickedHandler {
        /**
         * Area with 'id' has been tapped
         * @param id
         */
        fun onImageMapClicked(id: Int, imageMap: ImageMap?)

        /**
         * Info bubble associated with area 'id' has been tapped
         * @param id
         */
        fun onBubbleClicked(id: Int)
    }

    private object BitmapHelper {
        private var mMemoryCache: LruCache<String, Bitmap>

        init {
            //trying to implement image caching
            // Get max available VM memory, exceeding this amount will throw an
            // OutOfMemory exception. Stored in kilobytes as LruCache takes an
            // int in its constructor.

            //trying to implement image caching
            // Get max available VM memory, exceeding this amount will throw an
            // OutOfMemory exception. Stored in kilobytes as LruCache takes an
            // int in its constructor.
            val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()

            // Use 1/2nd of the available memory for this memory cache.

            // Use 1/2nd of the available memory for this memory cache.
            val cacheSize = maxMemory / 2
            mMemoryCache = LruCache<String, Bitmap>(cacheSize)
        }

        fun addBitmapToMemoryCache(key: String, bitmap: Bitmap?) {
            if (getBitmapFromMemCache(key) == null) {
                mMemoryCache.put(key, bitmap!!)
            }
        }

        fun getBitmapFromMemCache(key: String): Bitmap? {
            return mMemoryCache[key]
        }
    }

    /*
	 * Constructors
	 */
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        loadAttributes(attrs)
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        loadAttributes(attrs)
        init()
    }

    /**
     * get the map name from the attributes and load areas from xml
     * @param attrs
     */
    private fun loadAttributes(attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ImageMap)
        mFitImageToScreen = a.getBoolean(R.styleable.ImageMap_fitImageToScreen, true)
        mScaleFromOriginal = a.getBoolean(R.styleable.ImageMap_scaleFromOriginal, false)
        mMaxSize = a.getFloat(R.styleable.ImageMap_maxSizeFactor, defaultMaxSize)
        mBubbleTextColor = a.getColor(R.styleable.ImageMap_bubbleTextColor, -0x1000000)
        mBubbleBkgColor = a.getColor(R.styleable.ImageMap_bubbleBackgroundColor, -0x1)
        mMapXmlResId = a.getResourceId(R.styleable.ImageMap_mapxml, 0)
        mapName = a.getString(R.styleable.ImageMap_map)
        if (mMapXmlResId != 0 && mapName != null) {
            loadMap(mapName!!)
        }
        a.recycle()
    }

    /**
     * parse the maps.xml resource and pull out the areas
     * @param map - the name of the map to load
     */
    private fun loadMap(map: String) {
        var loading = false
        try {
            val xpp = resources.getXml(mMapXmlResId)
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    // Start document
                    //  This is a useful branch for a debug log if
                    //  parsing is not working
                } else if (eventType == XmlPullParser.START_TAG) {
                    val tag = xpp.name
                    if (tag.equals("map", ignoreCase = true)) {
                        val mapname = xpp.getAttributeValue(null, "name")
                        if (mapname != null) {
                            if (mapname.equals(map, ignoreCase = true)) {
                                loading = true
                            }
                        }
                    }
                    if (loading) {
                        if (tag.equals("area", ignoreCase = true)) {
                            var a: Area? = null
                            val shape = xpp.getAttributeValue(null, "shape")
                            val coords = xpp.getAttributeValue(null, "coords")
                            val id = xpp.getAttributeValue(null, "id")

                            // as a name for this area, try to find any of these
                            // attributes
                            //  name attribute is custom to this impl (not standard in html area tag)
                            var name = xpp.getAttributeValue(null, "name")
                            if (name == null) {
                                name = xpp.getAttributeValue(null, "title")
                            }
                            if (name == null) {
                                name = xpp.getAttributeValue(null, "alt")
                            }
                            if (shape != null && coords != null) {
                                a = addShape(shape, name, coords, id)
                                if (a != null) {
                                    // add all of the area tag attributes
                                    // so that they are available to the
                                    // implementation if needed (see getAreaAttribute)
                                    for (i in 0 until xpp.attributeCount) {
                                        val attrName = xpp.getAttributeName(i)
                                        val attrVal = xpp.getAttributeValue(null, attrName)
                                        a.addValue(attrName, attrVal)
                                    }
                                }
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    val tag = xpp.name
                    if (tag.equals("map", ignoreCase = true)) {
                        loading = false
                    }
                }
                eventType = xpp.next()
            }
        } catch (xppe: XmlPullParserException) {
            // Having trouble loading? Log this exception
        } catch (ioe: IOException) {
            // Having trouble loading? Log this exception
        }
    }

    /**
     * Create a new area and add to tracking
     * Changed this from private to protected!
     * @param shape
     * @param name
     * @param coords
     * @param id
     * @return
     */
    private fun addShape(shape: String, name: String?, coords: String, id: String): Area? {
        var a: Area? = null
        val rid = id.replace("@+id/", "")
        val _id = try {
            val res: Class<R.id> = R.id::class.java
            val field: Field = res.getField(rid)
            field.getInt(null)
        } catch (e: Exception) {
            0
        }
        if (_id != 0) {
            if (shape.equals("rect", ignoreCase = true)) {
                val v = coords.split(",").toTypedArray()
                if (v.size == 4) {
                    a = RectArea(
                        _id,
                        name,
                        v[0].toFloat(),
                        v[1].toFloat(),
                        v[2].toFloat(),
                        v[3].toFloat()
                    )
                }
            }
            if (shape.equals("circle", ignoreCase = true)) {
                val v = coords.split(",").toTypedArray()
                if (v.size == 3) {
                    a = CircleArea(_id, name, v[0].toFloat(), v[1].toFloat(), v[2].toFloat())
                }
            }
            if (shape.equals("poly", ignoreCase = true)) {
                a = PolyArea(_id, name, coords)
            }
            if (a != null) {
                addArea(a)
            }
        }
        return a
    }

    fun addArea(a: Area) {
        mAreaList.add(a)
        mIdToArea.put(a.getId(), a)
    }

    fun addBubble(text: String?, areaId: Int) {
        if (mBubbleMap[areaId] == null) {
            val b = Bubble(text!!, areaId)
            mBubbleMap.put(areaId, b)
        }
    }

    fun showBubble(text: String?, areaId: Int) {
        mBubbleMap.clear()
        addBubble(text, areaId)
        invalidate()
    }

    fun showBubble(areaId: Int) {
        mBubbleMap.clear()
        val a = mIdToArea[areaId]
        if (a != null) {
            addBubble(a.getName(), areaId)
        }
        invalidate()
    }

    fun hideBubble() {
        mBubbleMap.clear()
        invalidate()
    }

    fun isBubbleShowing(): Boolean {
        return mBubbleMap.size() > 0
    }

    fun centerArea(areaId: Int) {
        val a = mIdToArea[areaId]
        if (a != null) {
            val x = a.getOriginX() * mResizeFactorX
            val y = a.getOriginY() * mResizeFactorY
            val left = (mViewWidth / 2 - x).toInt()
            val top = (mViewHeight / 2 - y).toInt()
            moveTo(left, top)
        }
    }

    fun centerAndShowArea(text: String?, areaId: Int) {
        centerArea(areaId)
        showBubble(text, areaId)
    }

    fun centerAndShowArea(areaId: Int) {
        val a = mIdToArea[areaId]
        if (a != null) {
            centerAndShowArea(a.getName(), areaId)
        }
    }

    fun getAreaAttribute(areaId: Int, key: String?): String? {
        var value: String? = null
        val a = mIdToArea[areaId]
        if (a != null) {
            value = a.getValue(key)
        }
        return value
    }

    /**
     * initialize the view
     */
    private fun init() {
        // set up paint objects
        initDrawingTools()

        // create a scroller for flinging
        mScroller = Scroller(context)

        // get some default values from the system for touch/drag/fling
        val configuration = ViewConfiguration
            .get(context)
        mTouchSlop = configuration.scaledTouchSlop
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumVelocity = configuration.scaledMaximumFlingVelocity

        //find out the screen density
        densityFactor = resources.displayMetrics.density
    }

    /*
	 * These methods will be called when images or drawables are set
	 * in the XML for the view.  We handle either bitmaps or drawables
	 * @see android.widget.ImageView#setImageBitmap(android.graphics.Bitmap)
	 */
    override fun setImageBitmap(bm: Bitmap) {
        mOriginal = if (mImage == mOriginal) {
            null
        } else {
            mOriginal!!.recycle()
            null
        }
        if (mImage != null) {
            mImage!!.recycle()
            mImage = null
        }
        mImage = bm
        mOriginal = bm
        mImageHeight = mImage!!.height
        mImageWidth = mImage!!.width
        mAspect = mImageWidth.toFloat() / mImageHeight
        setInitialImageBounds()
    }

    override fun setImageResource(resId: Int) {
        val imageKey = resId.toString()
        var bitmap: Bitmap? = BitmapHelper.getBitmapFromMemCache(imageKey)

        // 1 is the default setting, powers of 2 used to decrease image quality (and memory consumption)
        // TODO: enable variable inSampleSize for low-memory devices
        options = BitmapFactory.Options()
        options!!.inSampleSize = 1
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(resources, resId, options)
            BitmapHelper.addBitmapToMemoryCache(imageKey, bitmap)
        }
        setImageBitmap(bitmap!!)
    }

    /*
		setImageDrawable() is called by Android when the android:src attribute is set.
		To avoid this and use the more flexible setImageResource(),
		it is advised to omit the android:src attribute and call setImageResource() directly from code.
	 */
    override fun setImageDrawable(drawable: Drawable?) {
        if (drawable is BitmapDrawable) {
            setImageBitmap(drawable.bitmap)
        }
    }

    /**
     * setup the paint objects for drawing bubbles
     */
    private fun initDrawingTools() {
        textPaint = Paint()
        textPaint!!.color = mBubbleTextColor
        textPaint!!.textSize = 30f
        textPaint!!.typeface = Typeface.SERIF
        textPaint!!.textAlign = Paint.Align.CENTER
        textPaint!!.isAntiAlias = true
        textOutlinePaint = Paint()
        textOutlinePaint!!.color = -0x1000000
        textOutlinePaint!!.textSize = 18f
        textOutlinePaint!!.typeface = Typeface.SERIF
        textOutlinePaint!!.textAlign = Paint.Align.CENTER
        textOutlinePaint!!.style = Paint.Style.STROKE
        textOutlinePaint!!.strokeWidth = 2f
        bubblePaint = Paint()
        bubblePaint!!.color = mBubbleBkgColor
        bubbleShadowPaint = Paint()
        bubbleShadowPaint!!.color = -0x60000000
    }

    /*
	 * Called by the scroller when flinging
	 * @see android.view.View#computeScroll()
	 */
    override fun computeScroll() {
        if (mScroller!!.computeScrollOffset()) {
            val oldX = mScrollLeft
            val oldY = mScrollTop
            val x = mScroller!!.currX
            val y = mScroller!!.currY
            if (oldX != x) {
                moveX(x - oldX)
            }
            if (oldY != y) {
                moveY(y - oldY)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val chosenWidth: Int = chooseDimension(widthMode, widthSize)
        val chosenHeight: Int = chooseDimension(heightMode, heightSize)
        if (mFitImageToScreen && mScaleFromOriginal) {
            val targetRatio = chosenWidth.toFloat() / chosenHeight.toFloat()
            val sourceRatio = mImageWidth.toFloat() / mImageHeight.toFloat()
            val width: Int
            val height: Int
            if (sourceRatio >= targetRatio) { // source is wider than target in proportion
                width = chosenWidth
                height = (width.toFloat() / sourceRatio).toInt()
            } else { // source is higher than target in proportion
                height = chosenHeight
                width = (height.toFloat() * sourceRatio).toInt()
            }
            setMeasuredDimension(width, height)
        } else {
            setMeasuredDimension(chosenWidth, chosenHeight)
        }
    }

    private fun chooseDimension(mode: Int, size: Int): Int {
        return if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            size
        } else {
            // (mode == MeasureSpec.UNSPECIFIED)
            getPreferredSize()
        }
    }


    /**
     * set the initial bounds of the image
     */
    private fun setInitialImageBounds() {
        if (mFitImageToScreen) {
            setInitialImageBoundsFitImage()
        } else {
            setInitialImageBoundsFillScreen()
        }
    }

    /**
     * setInitialImageBoundsFitImage sets the initial image size to match the
     * screen size.  aspect ratio may be broken
     */
    private fun setInitialImageBoundsFitImage() {
        if (mImage != null) {
            if (mViewWidth > 0) {
                if (mScaleFromOriginal) {
                    val targetRatio = mViewWidth.toFloat() / mViewHeight.toFloat()
                    val sourceRatio = mImageWidth.toFloat() / mImageHeight.toFloat()
                    if (sourceRatio >= targetRatio) { // source is wider than target in proportion
                        mMinWidth = mViewWidth
                        mMinHeight = (mMinWidth.toFloat() / sourceRatio).toInt()
                    } else { // source is higher than target in proportion
                        mMinHeight = mViewHeight
                        mMinWidth = (mMinHeight.toFloat() * sourceRatio).toInt()
                    }
                } else {
                    mMinHeight = mViewHeight
                    mMinWidth = mViewWidth
                }
                mMaxWidth = (mMinWidth * mMaxSize).toInt()
                mMaxHeight = (mMinHeight * mMaxSize).toInt()
                mScrollTop = 0
                mScrollLeft = 0
                scaleBitmap(mMinWidth, mMinHeight)
            }
        }
    }

    /**
     * setInitialImageBoundsFillScreen sets the initial image size to so that there
     * is no uncovered area of the device
     */
    private fun setInitialImageBoundsFillScreen() {
        if (mImage != null) {
            if (mViewWidth > 0) {
                var resize = false
                var newWidth = mImageWidth
                var newHeight = mImageHeight

                // The setting of these max sizes is very arbitrary
                // Need to find a better way to determine max size
                // to avoid attempts too big a bitmap and throw OOM
                if (mMinWidth == -1) {
                    // set minimums so that the largest
                    // direction we always filled (no empty view space)
                    // this maintains initial aspect ratio
                    if (mViewWidth > mViewHeight) {
                        mMinWidth = mViewWidth
                        mMinHeight = (mMinWidth / mAspect).toInt()
                    } else {
                        mMinHeight = mViewHeight
                        mMinWidth = (mAspect * mViewHeight).toInt()
                    }
                    mMaxWidth = (mMinWidth * 1.5f).toInt()
                    mMaxHeight = (mMinHeight * 1.5f).toInt()
                }
                if (newWidth < mMinWidth) {
                    newWidth = mMinWidth
                    newHeight = (mMinWidth.toFloat() / mImageWidth * mImageHeight).toInt()
                    resize = true
                }
                if (newHeight < mMinHeight) {
                    newHeight = mMinHeight
                    newWidth = (mMinHeight.toFloat() / mImageHeight * mImageWidth).toInt()
                    resize = true
                }
                mScrollTop = 0
                mScrollLeft = 0

                // scale the bitmap
                if (resize) {
                    scaleBitmap(newWidth, newHeight)
                } else {
                    mExpandWidth = newWidth
                    mExpandHeight = newHeight
                    mResizeFactorX = newWidth.toFloat() / mImageWidth
                    mResizeFactorY = newHeight.toFloat() / mImageHeight
                    mRightBound = 0 - (mExpandWidth - mViewWidth)
                    mBottomBound = 0 - (mExpandHeight - mViewHeight)
                }
            }
        }
    }

    /**
     * Set the image to new width and height
     * create a new scaled bitmap and dispose of the previous one
     * recalculate scaling factor and right and bottom bounds
     * @param newWidth
     * @param newHeight
     */
    fun scaleBitmap(newWidth: Int, newHeight: Int) {
        // Technically since we always keep aspect ratio intact
        // we should only need to check one dimension.
        // Need to investigate and fix
        var lNewWidth = newWidth
        var lNewHeight = newHeight
        if (lNewWidth > mMaxWidth || lNewHeight > mMaxHeight) {
            lNewWidth = mMaxWidth
            lNewHeight = mMaxHeight
        }
        if (lNewWidth < mMinWidth || lNewHeight < mMinHeight) {
            lNewWidth = mMinWidth
            lNewHeight = mMinHeight
        }
        if (lNewWidth != mExpandWidth || lNewHeight != mExpandHeight) {
            // NOTE: depending on the image being used, it may be
            //       better to keep the original image available and
            //       use those bits for resize.  Repeated grow/shrink
            //       can render some images visually non-appealing
            //       see comments at top of file for mScaleFromOriginal
            // try to create a new bitmap
            // If you get a recycled bitmap exception here, check to make sure
            // you are not setting the bitmap both from XML and in code
            val newbits = Bitmap.createScaledBitmap(
                (if (mScaleFromOriginal) mOriginal else mImage)!!,
                lNewWidth,
                lNewHeight,
                true
            )
            // if successful, fix up all the tracking variables
            if (newbits != null) {
                if (mImage != mOriginal) {
                    mImage!!.recycle()
                }
                mImage = newbits
                mExpandWidth = lNewWidth
                mExpandHeight = lNewHeight
                mResizeFactorX = lNewWidth.toFloat() / mImageWidth
                mResizeFactorY = lNewHeight.toFloat() / mImageHeight
                mRightBound = if (mExpandWidth > mViewWidth) 0 - (mExpandWidth - mViewWidth) else 0
                mBottomBound =
                    if (mExpandHeight > mViewHeight) 0 - (mExpandHeight - mViewHeight) else 0
            }
        }
    }

    fun resizeBitmap(amount: Int) {
        val adjustHeight = (amount / mAspect).toInt()
        scaleBitmap(mExpandWidth + amount, mExpandHeight + adjustHeight)
    }

    /**
     * watch for screen size changes and reset the background image
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // save device height width, we use it a lot of places
        mViewHeight = h
        mViewWidth = w

        // fix up the image
        setInitialImageBounds()
    }

    private fun getPreferredSize(): Int {
        return 300
    }

    /**
     * the onDraw routine when we are using a background image
     *
     * @param canvas
     */
    private fun drawMap(canvas: Canvas) {
        canvas.save()
        if (mImage != null) {
            if (!mImage!!.isRecycled) {
                canvas.drawBitmap(mImage!!, mScrollLeft.toFloat(), mScrollTop.toFloat(), null)
            }
        }
        canvas.restore()
    }

    @SuppressLint("WrongCall")
    private fun drawBubbles(canvas: Canvas?) {
        for (i in 0 until mBubbleMap.size()) {
            val key = mBubbleMap.keyAt(i)
            val b = mBubbleMap[key]
            b?.onDraw(canvas!!)
        }
    }

    @SuppressLint("WrongCall")
    private fun drawLocations(canvas: Canvas?) {
        for (a in mAreaList) {
            a.onDraw(canvas!!)
        }
    }

    /**
     * Paint the view
     * image first, location decorations next, bubbles on top
     */
    override fun onDraw(canvas: Canvas) {
        drawMap(canvas)
        drawLocations(canvas)
        drawBubbles(canvas)
    }

    /*
	 * Touch handler
	 *   This handler manages an arbitrary number of points
	 *   and detects taps, moves, flings, and zooms
	 */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) {
            return super.onTouchEvent(ev)
        }
        var id: Int
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker!!.addMovement(ev)
        val action = ev.action
        val pointerCount = ev.pointerCount
        var index = 0
        if (pointerCount > 1) {
            // If you are using new API (level 8+) use these constants
            // instead as they are much better names
            index = action and MotionEvent.ACTION_POINTER_INDEX_MASK
            index = index shr MotionEvent.ACTION_POINTER_INDEX_SHIFT

            // for api 7 and earlier we are stuck with these
            // constants which are poorly named
            // ID refers to INDEX, not the actual ID of the pointer
            // index = (action & MotionEvent.ACTION_POINTER_ID_MASK);
            // index = index >> MotionEvent.ACTION_POINTER_ID_SHIFT;
        }
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                // Clear all touch points
                // In the case where some view up chain is messing with our
                // touch events, it is possible to miss UP and POINTER_UP
                // events.  Whenever ACTION_DOWN happens, it is intended
                // to always be the first touch, so we will drop tracking
                // for any points that may have been orphaned
                for (t in mTouchPoints.values) {
                    onLostTouch(t.getTrackingPointer())
                }
                id = ev.getPointerId(index)
                onTouchDown(id, ev.getX(index), ev.getY(index))
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                id = ev.getPointerId(index)
                onTouchDown(id, ev.getX(index), ev.getY(index))
            }
            MotionEvent.ACTION_MOVE -> {
                var p = 0
                while (p < pointerCount) {
                    id = ev.getPointerId(p)
                    val t = mTouchPoints[id]
                    if (t != null) {
                        onTouchMove(t, ev.getX(p), ev.getY(p))
                    }
                    // after all moves, check to see if we need
                    // to process a zoom
                    processZoom()
                    p++
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                id = ev.getPointerId(index)
                onTouchUp(id)
            }
            MotionEvent.ACTION_CANCEL -> {
                // Clear all touch points on ACTION_CANCEL
                // according to the google devs, CANCEL means cancel
                // tracking every touch.
                // cf: http://groups.google.com/group/android-developers/browse_thread/thread/8b14591ead5608a0/ad711bf24520e5c4?pli=1
                for (t in mTouchPoints.values) {
                    onLostTouch(t.getTrackingPointer())
                }
                // let go of the velocity tracker per API Docs
                if (mVelocityTracker != null) {
                    mVelocityTracker!!.recycle()
                    mVelocityTracker = null
                }
            }
        }
        return true
    }


    private fun onTouchDown(id: Int, x: Float, y: Float) {
        // create a new touch point to track this ID
        var t: TouchPoint? = null
        synchronized(mTouchPoints) {

            // This test is a bit paranoid and research should
            // be done sot that it can be removed.  We should
            // not find a touch point for the id
            t = mTouchPoints[id]
            if (t == null) {
                t = TouchPoint(id)
                mTouchPoints[id] = t!!
            }

            // for pinch zoom, we need to pick two touch points
            // they will be called Main and Pinch
            if (mMainTouch == null) {
                mMainTouch = t
            } else {
                if (mPinchTouch == null) {
                    mPinchTouch = t
                    // second point established, set up to
                    // handle pinch zoom
                    startZoom()
                }
            }
        }
        t!!.setPosition(x, y)
    }

    /*
	 * Track pointer moves
	 */
    private fun onTouchMove(t: TouchPoint, x: Float, y: Float) {
        // mMainTouch will drag the view, be part of a
        // pinch zoom, or trigger a tap
        if (t === mMainTouch) {
            if (mPinchTouch == null) {
                // only on point down, this is a move
                val deltaX = (t.getX() - x).toInt()
                val xDiff = abs(t.getX() - x).toInt()
                val deltaY = (t.getY() - y).toInt()
                val yDiff = abs(t.getY() - y).toInt()
                if (!mIsBeingDragged) {
                    if (xDiff > mTouchSlop || yDiff > mTouchSlop) {
                        // start dragging about once the user has
                        // moved the point far enough
                        mIsBeingDragged = true
                    }
                } else {
                    // being dragged, move the image
                    if (xDiff > 0) {
                        moveX(-deltaX)
                    }
                    if (yDiff > 0) {
                        moveY(-deltaY)
                    }
                    t.setPosition(x, y)
                }
            } else {
                // two fingers down means zoom
                t.setPosition(x, y)
                onZoom()
            }
        } else {
            if (t === mPinchTouch) {
                // two fingers down means zoom
                t.setPosition(x, y)
                onZoom()
            }
        }
    }

    /*
	 * touch point released
	 */
    private fun onTouchUp(id: Int) {
        synchronized(mTouchPoints) {
            val t = mTouchPoints[id]
            if (t != null) {
                if (t === mMainTouch) {
                    if (mPinchTouch == null) {
                        // This is either a fling or tap
                        if (mIsBeingDragged) {
                            // view was being dragged means this is a fling
                            val velocityTracker = mVelocityTracker!!
                            velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                            val xVelocity = velocityTracker.xVelocity.toInt()
                            val yVelocity = velocityTracker.yVelocity.toInt()
                            val xfling =
                                if (abs(xVelocity) > mMinimumVelocity) xVelocity else 0
                            val yfling =
                                if (abs(yVelocity) > mMinimumVelocity) yVelocity else 0
                            if (xfling != 0 || yfling != 0) {
                                fling(-xfling, -yfling)
                            }
                            mIsBeingDragged = false

                            // let go of the velocity tracker
                            if (mVelocityTracker != null) {
                                mVelocityTracker!!.recycle()
                                mVelocityTracker = null
                            }
                        } else {
                            // no movement - this was a tap
                            onScreenTapped(mMainTouch!!.getX().toInt(), mMainTouch!!.getY().toInt())
                        }
                    }
                    mMainTouch = null
                    mZoomEstablished = false
                }
                if (t === mPinchTouch) {
                    // lost the 2nd pointer
                    mPinchTouch = null
                    mZoomEstablished = false
                }
                mTouchPoints.remove(id)
                // shuffle remaining pointers so that we are still
                // tracking.  This is necessary for proper action
                // on devices that support > 2 touches
                regroupTouches()
            } else {
                // lost this ID somehow
                // This happens sometimes due to the way some
                // devices manage touch
            }
        }
    }

    /*
	 * Touch handling varies from device to device, we may think we
	 * are tracking an id which goes missing
	 */
    private fun onLostTouch(id: Int) {
        synchronized(mTouchPoints) {
            val t = mTouchPoints[id]
            if (t != null) {
                if (t === mMainTouch) {
                    mMainTouch = null
                }
                if (t === mPinchTouch) {
                    mPinchTouch = null
                }
                mTouchPoints.remove(id)
                regroupTouches()
            }
        }
    }

    /*
	 * find a touch pointer that is not being used as main or pinch
	 */
    private fun getUnboundPoint(): TouchPoint? {
        var ret: TouchPoint? = null
        for (i in mTouchPoints.keys) {
            val p = mTouchPoints[i]
            if (p !== mMainTouch && p !== mPinchTouch) {
                ret = p
                break
            }
        }
        return ret
    }

    /*
	 * go through remaining pointers and try to have
	 * MainTouch and then PinchTouch if possible
	 */
    private fun regroupTouches() {
        val s = mTouchPoints.size
        if (s > 0) {
            if (mMainTouch == null) {
                if (mPinchTouch != null) {
                    mMainTouch = mPinchTouch
                    mPinchTouch = null
                } else {
                    mMainTouch = getUnboundPoint()
                }
            }
            if (s > 1) {
                if (mPinchTouch == null) {
                    mPinchTouch = getUnboundPoint()
                    startZoom()
                }
            }
        }
    }

    /*
	 * Called when the second pointer is down indicating that we
	 * want to do a pinch-zoom action
	 */
    private fun startZoom() {
        // this boolean tells the system that it needs to
        // initialize itself before trying to zoom
        // This is cleaner than duplicating code
        // see processZoom
        mZoomEstablished = false
    }

    /*
	 * one of the pointers for our pinch-zoom action has moved
	 * Remember this until after all touch move actions are processed.
	 */
    private fun onZoom() {
        mZoomPending = true
    }

    /*
	 * All touch move actions are done, do we need to zoom?
	 */
    private fun processZoom() {
        if (mZoomPending) {
            // check pinch distance, set new scale factor
            val dx = mMainTouch!!.getX() - mPinchTouch!!.getX()
            val dy = mMainTouch!!.getY() - mPinchTouch!!.getY()
            val newDistance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (mZoomEstablished) {
                // baseline was set, check to see if there is enough
                // movement to resize
                val distanceChange = (newDistance - mInitialDistance).toInt()
                val delta = distanceChange - mLastDistanceChange
                if (Math.abs(delta) > mTouchSlop) {
                    mLastDistanceChange = distanceChange
                    resizeBitmap(delta)
                    invalidate()
                }
            } else {
                // first run through after touches established
                // just set baseline
                mLastDistanceChange = 0
                mInitialDistance = newDistance
                mZoomEstablished = true
            }
            mZoomPending = false
        }
    }

    /*
	 * Screen tapped x, y is screen coord from upper left and does not account
	 * for scroll
	 */
    private fun onScreenTapped(x: Int, y: Int) {
        var missed = true
        var bubble = false
        // adjust for scroll
        var testx = x - mScrollLeft
        var testy = y - mScrollTop

        /*
			Empirically, this works, but it's not guaranteed to be correct.
			Seems that we need to divide by densityFactor only if the picture is larger than the screen.
			When it is smaller than the screen, we don't need to do that.

			TODO: investigate this in detail.
		 */testx = if (mResizeFactorX > 1) {
            (testx.toFloat() / mResizeFactorX).toInt()
        } else {
            (testx.toFloat() / mResizeFactorX / densityFactor).toInt()
        }
        testy = if (mResizeFactorY > 1) {
            (testy.toFloat() / mResizeFactorY).toInt()
        } else {
            (testy.toFloat() / mResizeFactorY / densityFactor).toInt()
        }

        // check if bubble tapped first
        // in case a bubble covers an area we want it to
        // have precedent
        for (i in 0 until mBubbleMap.size()) {
            val key = mBubbleMap.keyAt(i)
            val b = mBubbleMap[key]
            //it can still be null if there are no bubbles at all
            if (b != null) {
                if (b.isInArea(x.toFloat() - mScrollLeft, y.toFloat() - mScrollTop)) {
                    b.onTapped()
                    bubble = true
                    missed = false
                    // only fire tapped for one bubble
                    break
                }
            }
        }
        if (!bubble) {
            // then check for area taps
            for (a in mAreaList) {
                if (a.isInArea(testx.toFloat(), testy.toFloat())) {
                    if (mCallbackList != null) {
                        for (h in mCallbackList!!) {
                            h.onImageMapClicked(a.getId(), this)
                        }
                    }
                    missed = false
                    // only fire clicked for one area
                    break
                }
            }
        }
        if (missed) {
            // managed to miss everything, clear bubbles
            mBubbleMap.clear()
            invalidate()
        }
    }

    // process a fling by kicking off the scroller
    private fun fling(velocityX: Int, velocityY: Int) {
        val startX = mScrollLeft
        val startY = mScrollTop
        mScroller!!.fling(startX, startY, -velocityX, -velocityY, mRightBound, 0, mBottomBound, 0)
        invalidate()
    }

    /*
	 * move the view to this x, y
	 */
    private fun moveTo(x: Int, y: Int) {
        mScrollLeft = x
        if (mScrollLeft > 0) {
            mScrollLeft = 0
        }
        if (mScrollLeft < mRightBound) {
            mScrollLeft = mRightBound
        }
        mScrollTop = y
        if (mScrollTop > 0) {
            mScrollTop = 0
        }
        if (mScrollTop < mBottomBound) {
            mScrollTop = mBottomBound
        }
        invalidate()
    }

    /*
	 * move the view by this delta in X direction
	 */
    private fun moveX(deltaX: Int) {
        mScrollLeft += deltaX
        if (mScrollLeft > 0) {
            mScrollLeft = 0
        }
        if (mScrollLeft < mRightBound) {
            mScrollLeft = mRightBound
        }
        invalidate()
    }

    /*
	 * move the view by this delta in Y direction
	 */
    private fun moveY(deltaY: Int) {
        mScrollTop += deltaY
        if (mScrollTop > 0) {
            mScrollTop = 0
        }
        if (mScrollTop < mBottomBound) {
            mScrollTop = mBottomBound
        }
        invalidate()
    }

    /*
	 * on clicked handler add/remove support
	 */
    fun addOnImageMapClickedHandler(h: OnImageMapClickedHandler?) {
        if (h != null) {
            if (mCallbackList == null) {
                mCallbackList = ArrayList()
            }
            mCallbackList!!.add(h)
        }
    }

    fun removeOnImageMapClickedHandler(h: OnImageMapClickedHandler?) {
        if (mCallbackList != null) {
            if (h != null) {
                mCallbackList!!.remove(h)
            }
        }
    }

    /*
	* Misc getters
	* TODO: setters for there?
	*/
    fun getMaxSize(): Float {
        return mMaxSize
    }

    fun isScaleFromOriginal(): Boolean {
        return mScaleFromOriginal
    }

    fun isFitImageToScreen(): Boolean {
        return mFitImageToScreen
    }
}