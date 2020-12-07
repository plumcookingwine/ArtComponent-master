package com.kangf.router.api

/**
 * Created by kangf on 2020/12/4.
 */
interface IRouterGroup {

    /**
     * Map
     *      key ->>>>> group
     *
     *     value ->>>>> path集合
     */
    fun getGroupMap(): Map<String, Class<out IRouterPath>>
}