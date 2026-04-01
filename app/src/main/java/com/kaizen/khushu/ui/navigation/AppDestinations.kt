package com.kaizen.khushu.ui.navigation

import com.kaizen.khushu.R

const val LEARN_DETAIL_ROUTE = "learn_detail/{sectionTitle}"

enum class AppDestinations(val label: String, val icon: Int, val route: String) {
    SALAH("Salah", R.drawable.ic_salah, "salah"),
    TASBEEH("Tasbih", R.drawable.ic_tasbeeh, "tasbeeh"),
    LEARN("Learn", R.drawable.ic_learn, "learn"),
    ;
    companion object {
        fun fromRoute(route: String?) = entries.find { it.route == route }
    }
}
