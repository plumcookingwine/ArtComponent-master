package com.kangf.router.api

import android.content.Context
import android.content.Intent

/**
 * Created by kangf on 2020/12/7.
 */
class IRouterUtils {

    private var mPath = ""

    companion object {

        val instance by lazy { IRouterUtils() }

        fun build(path: String): IRouterUtils {
            val utils = instance
            utils.mPath = path
            return utils
        }
    }

    fun navigation(context: Context) {

        val finalGroup: String = mPath.split("/")[0] // finalGroup = order

        // 找到组map
        val groupClazz =
            Class.forName("com.kangf.route.generate.RouteGroup_component_${finalGroup}")
        val groupInstance = groupClazz.newInstance() as IRouterGroup
        // 通过组找到路径的map
        val pathInstance = (groupInstance.getGroupMap()["component_${finalGroup}"]
            ?: error("")).newInstance() as IRouterPath
        // 通过路径的map找到组对应的routeBean
        val routeBean = pathInstance.getPath()[mPath]
        // 找到对应的class进行跳转
        val clazz = routeBean!!.clazz
        context.startActivity(Intent(context, clazz))
    }
}