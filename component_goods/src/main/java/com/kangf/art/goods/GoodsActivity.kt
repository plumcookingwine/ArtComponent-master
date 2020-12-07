package com.kangf.art.goods

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.kangf.router.annotation.IRouter
import com.kangf.router.api.IRouterUtils
import org.jetbrains.anko.find

@IRouter("goods/list")
class GoodsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goods)

        find<TextView>(R.id.tvGoods).setOnClickListener {

            IRouterUtils.build("order/list").navigation(this)

            /**
             * 经过注解处理器封装
             */
//             // 找到组map
//             val groupClazz = Class.forName("com.kangf.route.generate.RouteGroup_component_order")
//             val groupInstance = groupClazz.newInstance() as IRouterGroup
//             // 通过组找到路径的map
//             val pathInstance = (groupInstance.getGroupMap()["component_order"] ?: error("")).newInstance() as IRouterPath
//             // 通过路径的map找到组对应的routeBean
//             val routeBean = pathInstance.getPath()["order/list"]
//             // 找到对应的class进行跳转
//             val clazz = routeBean!!.clazz
//             startActivity(Intent(this, clazz))


            /**
             * 第1次封装
             */
            // // 找到组map
            // val groupClazz = Class.forName("com.kangf.art.order.router.OrderRouterGroup")
            // val groupInstance = groupClazz.newInstance() as IRouterGroup
            // // 通过组找到路径的map
            // val pathInstance = groupInstance.getGroupMap()["component_order"]!!.newInstance() as IRouterPath
            // // 通过路径的map找到组对应的routeBean
            // val routeBean = pathInstance.getPath()["component_order/list"]
            // // 找到对应的class进行跳转
            // val clazz = routeBean!!.clazz
            // startActivity(Intent(this, clazz))

            /**
             * 使用全局map
             */
            // val clazz = RecordPathManager.startActivity("order", "order/list")
            // startActivity(Intent(this, clazz))


            /**
             * 类加载
             */
//            val clazz = Class.forName("com.kangf.art.order.OrderActivity")
//            startActivity(Intent(this, clazz))
        }

        find<TextView>(R.id.tvDetail).setOnClickListener {

            IRouterUtils.build("order/detail").navigation(this)
        }
    }
}