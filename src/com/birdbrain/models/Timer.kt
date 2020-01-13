package com.birdbrain.models

import com.google.gson.annotations.Expose

data class Timer(
    @Expose
    val remaining: Int,
    @Expose
    val total : Int)