package com.kangf.art.order.router

import com.kangf.router.api.IRouterGroup
import com.kangf.router.api.IRouterPath

/**
 * Created by kangf on 2020/12/4.
 */
class OrderRouterGroup : IRouterGroup {

    override fun getGroupMap(): MutableMap<String, Class<out IRouterPath>> {
        val map = mutableMapOf<String, Class<out IRouterPath>>()
        map["component_order"] = OrderRouterPath::class.java
        return map

    }

}