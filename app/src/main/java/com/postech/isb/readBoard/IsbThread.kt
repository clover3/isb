package com.postech.isb.readBoard

// This is the corrected data class that matches the fields in your ThreadList.java
data class IsbThread(
    val key: Int,               // from num
    val title: String,            // from title
    val author: String,           // from writer
    val date: String,             // from date
    val hasComment: Boolean,      // from comment
    val isNotice: Boolean,        // from notdel
    val isNew: Boolean,           // from newt
    val isHighlighted: Boolean    // from highlight
)
