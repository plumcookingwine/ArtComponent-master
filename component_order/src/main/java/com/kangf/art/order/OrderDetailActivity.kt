package com.kangf.art.order

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kangf.router.annotation.IRouter

@IRouter("order/detail")
class OrderDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)
    }
}