package com.kangf.router.api

import com.kangf.router.annotation.bean.RouteBean

/**
 * Created by kangf on 2020/12/4.
 */
interface IRouterPath {

    /**
     * Map ：
     *      key  -》》》》 gourp
     *      value -》》》》 path， class， 等信息
     */
    fun getPath(): Map<String, RouteBean>
}