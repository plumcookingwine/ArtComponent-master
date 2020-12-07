package com.kangf.art.order.router

import com.kangf.art.order.OrderActivity
import com.kangf.art.order.OrderDetailActivity
import com.kangf.router.api.IRouterPath
import com.kangf.router.annotation.bean.RouteBean

/**
 * Created by kangf on 2020/12/4.
 */
class OrderRouterPath : IRouterPath {

    override fun getPath(): Map<String, RouteBean> {

        val map = mutableMapOf<String, RouteBean>()

        map["component_order/list"] = RouteBean().apply {
            group = "component_order"
            path = "component_order/list"
            clazz = OrderActivity::class.java
            typeEnum = RouteBean.TypeEnum.ACTIVITY
        }

        map["component_order/detail"] = RouteBean().apply {
            group = "component_order"
            path = "component_order/detail"
            clazz = OrderDetailActivity::class.java
            typeEnum = RouteBean.TypeEnum.ACTIVITY
        }

        return map
    }
}