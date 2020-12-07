package com.kangf.router.annotation.bean

import javax.lang.model.element.Element

/**
 * Created by kangf on 2020/11/27.
 */
class RouteBean {

    /**
     * 为了方便扩展，这里做一个标识
     */
    enum class TypeEnum {
        ACTIVITY,
        FRAGMENT
    }

    // 组名：  order  |   goods ...
    var group: String? = null

    // 路径：  order/order_list
    var path: String? = null

    // 类：  OrderActivity.class
    var clazz: Class<*>? = null

    // 标识是Activity， Fragment，或是其他
    var typeEnum: TypeEnum? = null

    // 类节点信息
    var element: Element? = null
}