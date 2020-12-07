package com.kangf.art

import android.app.Application
import com.kangf.art.goods.GoodsActivity
import com.kangf.art.order.OrderActivity
import com.kangf.base.route.RecordPathManager

/**
 * Created by kangf on 2020/11/27.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

//        RecordPathManager.addRoutePath("order", "order/list", OrderActivity::class.java)
//        RecordPathManager.addRoutePath("goods", "goods/list", GoodsActivity::class.java)
    }
}