package com.kangf.router.annotation

/**
 * Created by kangf on 2020/12/4.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class IRouter(val path: String)