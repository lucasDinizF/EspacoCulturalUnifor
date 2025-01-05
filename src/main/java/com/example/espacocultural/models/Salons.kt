package com.example.espacocultural.models

import android.graphics.drawable.Drawable
import android.widget.ImageView

data class Salons(
    var id: Int = 0,
    var name: String = "",
    var image: Drawable? = null,
    var showOptions: Boolean = false
)
