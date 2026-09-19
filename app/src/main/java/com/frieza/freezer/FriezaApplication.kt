package com.frieza.freezer

import android.app.Application
import com.frieza.freezer.data.FriezaDatabase
import com.frieza.freezer.data.FriezaRepository

class FriezaApplication : Application() {
    val repository: FriezaRepository by lazy {
        FriezaRepository(FriezaDatabase.getInstance(this))
    }
}
