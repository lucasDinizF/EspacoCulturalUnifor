package com.example.espacocultural.models

import android.graphics.drawable.Drawable

data class Artists(
    var name: String = "",
    var biography: String = "",
    var image: Drawable? = null,
    var showOptions: Boolean = false
)