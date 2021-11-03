// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2021 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.buildserver

import org.intellij.lang.annotations.Language

object AnimationXmlConstants {
    @Language("XML")
    const val FADE_IN_XML = """<?xml version="1.0" encoding="utf-8"?>

<alpha xmlns:android="http://schemas.android.com/apk/res/android"
       android:fromAlpha="0.0" android:toAlpha="1.0"
       android:duration="@android:integer/config_longAnimTime"/>
"""

    @Language("XML")
    const val FADE_OUT_XML = """<?xml version="1.0" encoding="utf-8"?>

<alpha xmlns:android="http://schemas.android.com/apk/res/android"
       android:fromAlpha="1.0" android:toAlpha="0.0"
       android:duration="@android:integer/config_longAnimTime"/>"""

    @Language("XML")
    const val HOLD_XML = """<?xml version="1.0" encoding="utf-8"?>

<translate xmlns:android="http://schemas.android.com/apk/res/android"
           android:interpolator="@android:anim/accelerate_interpolator"
           android:fromXDelta="0" android:toXDelta="0"
           android:duration="@android:integer/config_longAnimTime"/>"""

    @Language("XML")
    const val SLIDE_EXIT = """<?xml version="1.0" encoding="utf-8"?>

<translate xmlns:android="http://schemas.android.com/apk/res/android"
           android:interpolator="@android:anim/overshoot_interpolator"
           android:fromXDelta="0%" android:toXDelta="-100%"
           android:duration="@android:integer/config_mediumAnimTime"/>"""

    @Language("XML")
    const val SLIDE_ENTER = """<?xml version="1.0" encoding="utf-8"?>

<translate xmlns:android="http://schemas.android.com/apk/res/android"
           android:interpolator="@android:anim/overshoot_interpolator"
           android:fromXDelta="100%" android:toXDelta="0%"
           android:duration="@android:integer/config_mediumAnimTime"/>"""

    @Language("XML")
    const val SLIDE_EXIT_REVERSE = """<?xml version="1.0" encoding="utf-8"?>

<translate xmlns:android="http://schemas.android.com/apk/res/android"
           android:interpolator="@android:anim/overshoot_interpolator"
           android:fromXDelta="0%" android:toXDelta="100%"
           android:duration="@android:integer/config_mediumAnimTime"/>"""

    @Language("XML")
    const val SLIDE_ENTER_REVERSE = """<?xml version="1.0" encoding="utf-8"?>

<translate xmlns:android="http://schemas.android.com/apk/res/android"
           android:interpolator="@android:anim/overshoot_interpolator"
           android:fromXDelta="-100%" android:toXDelta="0%"
           android:duration="@android:integer/config_mediumAnimTime"/>"""

    @Language("XML")
    const val SLIDE_V_EXIT = """<?xml version="1.0" encoding="utf-8"?>

<translate xmlns:android="http://schemas.android.com/apk/res/android"
           android:interpolator="@android:anim/decelerate_interpolator"
           android:fromYDelta="0%" android:toYDelta="100%"
           android:duration="@android:integer/config_mediumAnimTime"/>"""

    @Language("XML")
    const val SLIDE_V_ENTER = """<?xml version="1.0" encoding="utf-8"?>

<translate xmlns:android="http://schemas.android.com/apk/res/android"
           android:interpolator="@android:anim/decelerate_interpolator"
           android:fromYDelta="-100%" android:toYDelta="0%"
           android:duration="@android:integer/config_mediumAnimTime"/>"""

    @Language("XML")
    const val SLIDE_V_EXIT_REVERSE = """<?xml version="1.0" encoding="utf-8"?>

<translate xmlns:android="http://schemas.android.com/apk/res/android"
           android:interpolator="@android:anim/decelerate_interpolator"
           android:fromYDelta="0%" android:toYDelta="-100%"
           android:duration="@android:integer/config_mediumAnimTime"/>"""

    @Language("XML")
    const val SLIDE_V_ENTER_REVERSE = """<?xml version="1.0" encoding="utf-8"?>

<translate xmlns:android="http://schemas.android.com/apk/res/android"
           android:interpolator="@android:anim/decelerate_interpolator"
           android:fromYDelta="100%" android:toYDelta="0%"
           android:duration="@android:integer/config_mediumAnimTime"/>"""

    @Language("XML")
    const val ZOOM_ENTER = """<?xml version="1.0" encoding="utf-8"?>

<set xmlns:android="http://schemas.android.com/apk/res/android"
     android:interpolator="@android:anim/decelerate_interpolator">
    <scale android:fromXScale="2.0" android:toXScale="1.0"
           android:fromYScale="2.0" android:toYScale="1.0"
           android:pivotX="50%p" android:pivotY="50%p"
           android:duration="@android:integer/config_mediumAnimTime"/>
</set>"""

    @Language("XML")
    const val ZOOM_ENTER_REVERSE = """<?xml version="1.0" encoding="utf-8"?>

<set xmlns:android="http://schemas.android.com/apk/res/android"
     android:interpolator="@android:anim/decelerate_interpolator">
    <scale android:fromXScale="0.5" android:toXScale="1.0"
           android:fromYScale="0.5" android:toYScale="1.0"
           android:pivotX="50%p" android:pivotY="50%p"
           android:duration="@android:integer/config_mediumAnimTime"/>
</set>"""

    @Language("XML")
    const val ZOOM_EXIT = """<?xml version="1.0" encoding="utf-8"?>

<set xmlns:android="http://schemas.android.com/apk/res/android"
     android:interpolator="@android:anim/decelerate_interpolator"
     android:zAdjustment="top">
    <scale android:fromXScale="1.0" android:toXScale=".5"
           android:fromYScale="1.0" android:toYScale=".5"
           android:pivotX="50%p" android:pivotY="50%p"
           android:duration="@android:integer/config_mediumAnimTime"/>
    <alpha android:fromAlpha="1.0" android:toAlpha="0"
           android:duration="@android:integer/config_mediumAnimTime"/>
</set>"""

    @Language("XML")
    const val ZOOM_EXIT_REVERSE = """<?xml version="1.0" encoding="utf-8"?>

<set xmlns:android="http://schemas.android.com/apk/res/android"
     android:interpolator="@android:anim/decelerate_interpolator"
     android:zAdjustment="top">
    <scale android:fromXScale="1.0" android:toXScale="2.0"
           android:fromYScale="1.0" android:toYScale="2.0"
           android:pivotX="50%p" android:pivotY="50%p"
           android:duration="@android:integer/config_mediumAnimTime"/>
    <alpha android:fromAlpha="1.0" android:toAlpha="0"
           android:duration="@android:integer/config_mediumAnimTime"/>
</set>"""
}