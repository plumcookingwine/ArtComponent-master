# ArtComponent-master
# Android组件化方案（二）-注解处理器（APT重磅干货）

## 前情回顾

上一篇文章我们主要讲的gradle的统一管理，组件之间的通信方案，以及如何使用全局map进行页面跳转。每个页面都需要在application中进行注册，这样肯定是不行的，根据面向对象的思想， 我们先对其进行一层封装，做到在编译器自动进行注册。

## 封装全局Map

首先我们将通信方案作为一个组件，创建一个irouter_api的module，由base去依赖它，然后把RouteBean实体类挪进来，为了方便以后扩展，我们在RouteBean中增加几个属性

```kotlin
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
}
```

组名，路径和类不变， 增加了一个enum的标识，用来标记是个Fragment，还是一个Activity，这时候项目结构应该是这样的

![在这里插入图片描述](https://img-blog.csdnimg.cn/2020120409313630.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzIyMDkwMDcz,size_16,color_FFFFFF,t_70)

不妨再考虑一下，只使用一个全局map的话，随着项目越来越大，activity越来越多，查找肯定会带来一定的效率问题，如果我们一个模块使用一个map呢？这样可以在很大程度上解决这种性能影响。怎么实现呢？我们先画画图，理清思路

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201204111614613.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzIyMDkwMDcz,size_16,color_FFFFFF,t_70)

首先是一个全局的`IRouterGroup`,  里面有一个全局map，key就是module的group， value用于存放每个模块的路径，我们把value进一步封装成`IRouterPath`，` IRouterPath`通过组名存放组下面的路径。思路都理清了，下面进入撸码阶段。

基于面向接口编程，我们定义一个path接口，用于存放某一个模块中的所有的路径：

```kotlin
interface IRouterPath {

    /**
     * Map ：
     *      key  -》》》》 gourp
     *      value -》》》》 path， class， 等信息
     */
    fun getPath(): Map<String, RouteBean>
}
```

再定义一个group接口，用于存放某个group的所有的path，因为class是要实现`IRouterPath`接口的，所以使用out限定符：

```kotlin
interface IRouterGroup {

    /**
     * Map
     *      key ->>>>> group
     *
     *     value ->>>>> path集合
     */
    fun getGroupMap(map: Map<String, Class<out IRouterPath>>)
}
```

下面我们基于order模块进行实现，比如我们订单模块有一个订单列表和一个订单详情，这时候path的实现应该是这样的：

```kotlin
class OrderRouterPath : IRouterPath {

    override fun getPath(): Map<String, RouteBean> {

        val map = mutableMapOf<String, RouteBean>()

        // 订单列表
        map["component_order/list"] = RouteBean().apply {
            group = "component_order"
            path = "component_order/list"
            clazz = OrderActivity::class.java
            typeEnum = RouteBean.TypeEnum.ACTIVITY
        }

        // 订单详情
        map["component_order/detail"] = RouteBean().apply {
            group = "component_order"
            path = "component_order/detail"
            clazz = OrderDetailActivity::class.java
            typeEnum = RouteBean.TypeEnum.ACTIVITY
        }

        return map
    }
}
```

组用于包装path的实现：

```kotlin
class OrderRouterGroup : IRouterGroup {

    override fun getGroupMap(): MutableMap<String, Class<out IRouterPath>> {
        val map = mutableMapOf<String, Class<out IRouterPath>>()
        map["component_order"] = OrderRouterPath::class.java
        return map

    }

}
```

这样处理之后，就可以从其他组件跳转了，比如从goods模块跳转到订单列表，之前的逻辑就应该改成这样：

```kotlin
find<TextView>(R.id.tvGoods).setOnClickListener {

            // 找到组map
            val groupClazz = Class.forName("com.kangf.art.order.router.OrderRouterGroup")
            val groupInstance = groupClazz.newInstance() as IRouterGroup
            // 通过组找到路径的map
            val pathInstance = groupInstance.getGroupMap()["component_order"]!!.newInstance() as IRouterPath
            // 通过路径的map找到组对应的routeBean
            val routeBean = pathInstance.getPath()["component_order/list"]
            // 找到对应的class进行跳转
            val clazz = routeBean!!.clazz
            startActivity(Intent(this, clazz))

           
        }
```

我们来看一下运行效果

<img src="https://img-blog.csdnimg.cn/20201204132414396.gif" alt="在这里插入图片描述" style="zoom: 25%;" />

WTF？ 我只要一个跳转页面，你给我搞这么一堆逻辑？还好意思叫封装？各位请收好手中的菜叶子臭鸡蛋，我们下一步才是真正的开始，好戏还在后头呢！

## 注解处理器

上面的做法显然 是不行的，在页面增多的情况下，我们将会做更多的重复性的工作，那何不将这些工作完全交给编译器来解决呢？这时候就用到了**APT**技术。 APT可以在编译时检查所有的注解，而我们正好可以借助它生成代码，来替我们完成这些重复的工作。

### APT是什么？

APT(Annotation Processing Tool) 是一种处理注释的工具，它对源代码文件进行检测找出其中的Annotation，根据注解自动生成代码，如果想要自定义的注解处理器能够正常运行，必须要通过APT工具来进行处理。 也可以样理解，只有通过声明APT工具后，程序在编译期间自定义注解解释器才能执行。 **通俗理解**：根据规则，帮我们生成代码、生成类文件。

### 注解

有注解处理器，就不得不提到注解，对注解不了解的，可以先看一下这篇文章:

<a href="https://blog.csdn.net/qq_22090073/article/details/104476822">IOC依赖注入（一）— 手写ButterKnife框架</a>

讲的是运行时注解，而我们现在用到的，是编译时注解。



### 注解处理器的API

首先我们创建一个java/kotlin的module作为注解处理器，新建一个类继承`javax.annotation.processing.AbstractProcessor`

```kotlin
class IRouterProcessor : AbstractProcessor() {

    override fun process(
        annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?
    ): Boolean {


        return true
    }
}
```

我们需要了解注解处理器用到的API，这对大部分人来说可能是陌生的，不过这很简单，大家用过一次之后都会对其有所了解。

#### Element

可以看到process方法中有一个`MutableSet`，这个就是所有注解的元素的集合，它的泛型是`TypeElement`的下限类型。

对于java源文件来说，它其实是一种结构体语言，这些结构组成就是一个个的Element组成的，在注解处理器中，Element是一个非常重要的元素。而我们注解的每一个元素，其实就是被包装成了一个个的Element放进了`MutableSet`集合中。Element有以下几个实现类，代表了不同的元素：

```kotlin
PackageElement 			表示一个包程序元素。提供对有关包及其成员的信息的访问 
ExecutableElement 		表示某个类或接口的方法、构造方法或初始化程序（静态或实例） 
TypeElement 			表示一个类或接口程序元素。提供对有关类型及其成员的信息的访问 
VariableElement 		表示一个字段、enum 常量、方法或构造方法参数、局部变量或异常参数
```

#### Element节点中的API

```
getEnclosedElements() 	返回该元素直接包含的子元素 
getEnclosingElement() 	返回包含该element的父element，与上一个方法相反 
getKind() 				返回element的类型，判断是哪种element 
getModifiers() 			获取修饰关键字,入public static final等关键字 
getSimpleName()			获取名字，不带包名 
getQualifiedName() 		获取全名，如果是类的话，包含完整的包名路径 
getParameters() 		获取方法的参数元素，每个元素是一个VariableElement 
getReturnType() 		获取方法元素的返回值 
getConstantValue() 		如果属性变量被final修饰，则可以使用该方法获取它的值
```

Element中有以上几个方法，我们一会将会频繁的用到。

### kotlinpoet

javapoet是square推出的开源java代码生成框架，提供Java Api生成.java源文件 这个框架功能非常实用，也是我们习惯的Java面向对象OOP语法 可以很方便的使用它根据注解生成对应代码通过这种自动化生成代码的方式， 可以让我们用更加简洁优雅的方式要替代繁琐冗杂的重复工作**。kotlinpoet顾名思义，是针对kotlin的一套框架，我们今天要用到的就是kotlinpoet，它可以帮助我们生成kotlin文件。**

[kotlinpoet项目主页](https://github.com/square/kotlinpoet)

#### kotlinpoet  API

在kotlinpoet中，每一个节点都对应一个Spec

```
类对象 					说明 

MethodSpec 			代表一个构造函数或方法声明 
TypeSpec 			代表一个类，接口，或者枚举声明 
FieldSpec 			代表一个成员变量，一个字段声明 
JavaFile 			包含一个顶级类的Java文件 
ParameterSpec 		用来创建参数 
AnnotationSpec 		用来创建注解 
ClassName 			用来包装一个类 
TypeName 			类型，如在添加返回值类型是使用 TypeName.VOID

通配符：
%S 字符串，如：%S, ”hello” 
%T 类、接口，如：%T, MainActivity
```

比如要生成以下代码：

```kotlin
class Greeter(val name: String) {
  fun greet() {
    println("""Hello, $name""")
  }
}

fun main(vararg args: String) {
  Greeter(args[0]).greet()
}
```

那么在注解处理器中，就要这样实现：

```kotlin
// 创建一个类类型
val greeterClass = ClassName("", "Greeter")
// 创建名为HelloWorld的文件
val file = FileSpec.builder("", "HelloWorld")
	// 文件中添加一个Greeter类
    .addType(TypeSpec.classBuilder("Greeter")
         // 类中的构造方法中，增加一个name属性
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("name", String::class)
            .build())
         // 类中增加一个方法
        .addFunction(FunSpec.builder("greet")
            // 方法中的语句，%P通配符代表了字符串模板
            .addStatement("println(%P)", "Hello, \$name")
            .build())
        .build())
	// 在HelloWorld的文件中增加一个main方法
    .addFunction(FunSpec.builder("main")
        // main方法中增加一个args的可变参数
        .addParameter("args", String::class, VARARG)
        // main方法中调用Greeter类中的greet方法
        .addStatement("%T(args[0]).greet()", greeterClass)
        .build())
    .build()
// 将文件写入输出流。
file.writeTo(System.out)
```

关于kotlin的详细使用，可以看它的[官方文档](https://square.github.io/kotlinpoet/)。

### 撸码

好了，以上就是关键的API了，下面正式进入撸码阶段：

首先创建注解module，用于存放注解，同时我们需要将RouteBean对象移动到注解module中，因为注解处理器和router_api模块需要这个实体类，同时再次对RouteBean进行扩展：

```kotlin
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
```

我们增加一个类节点信息，方便以后使用。 下面定义一个编译时注解，将作用在Activity或Fragment中：

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class IRouter(val path: String)
```

这个注解需要出入path。万事俱备，接下来就轮到注解处理器了。

首先在注解处理器模块的gradle中引入AutoService，用于帮我们生成MATE-INF.services下的文件，需要这个文件系统才能帮我们识别是一个注解处理器

```kotlin
plugins {
    id 'java-library'
    id 'kotlin'
    id 'kotlin-kapt'
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    //noinspection AnnotationProcessorOnCompilePath
    compileOnly "com.google.auto.service:auto-service:1.0-rc7"
    kapt "com.google.auto.service:auto-service:1.0-rc7"
    implementation project(path: ':network_annotation')
    implementation 'com.squareup:kotlinpoet:1.7.2'
}
```

工欲善其事必先利其器，编译完成之后，完善一下我们的注解处理器 ：

```kotlin
@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(ProcessorConfig.OPTIONS)
@SupportedAnnotationTypes(ProcessorConfig.ROUTER_NAME)
class IRouterProcessor : AbstractProcessor() {

    // 打印日志工具类
    private lateinit var mMessage: Messager

    // 文件操作类，我们将通过此类生成kotlin文件
    private lateinit var mFiler: Filer

    // 类型工具类，处理Element的类型
    private lateinit var mTypeTools: Types

    private lateinit var mElementUtils: Elements
    
    // gradle传进来的模块名
    private var mModuleName: String? = null

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        if (processingEnv == null) return
        mMessage = processingEnv.messager
        mFiler = processingEnv.filer
        mElementUtils = processingEnv.elementUtils
        mTypeTools = processingEnv.typeUtils
        
        mModuleName = processingEnv.getOptions().get(ProcessorConfig.OPTIONS);

        mMessage.printMessage(Diagnostic.Kind.NOTE, "processor 初始化完成.....${mModuleName}")
    }

    override fun process(
        annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?
    ): Boolean {

        return true
    }
}
```

在每个业务模块中引入我们的注解处理器：

```groovy
apply plugin: 'kotlin-kapt'

dependencies {
    // ....
    
	kapt project(":irouter_processor")
}

```

注解处理器上面的每个注解又有什么含义呢？

#### @AutoService

用于帮我们生成META-INF.services文件，有了这个文件才能识别出来注解处理器，那么这个文件在哪呢：

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201204160346700.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzIyMDkwMDcz,size_16,color_FFFFFF,t_70)

我们可以看到，这个文件名是Processor的全类名，文件内容是注解处理器的全类名。

#### @SupportedAnnotationTypes

支持的注解，这个注解内部传入的是IRouter注解的全类名，表示我们要处理哪个注解。

#### @SupportedSourceVersion

支持的java版本，这里 传入1.8即可

#### @SupportedOptions

gradle工程的配置，gradle中如果需要动态的传入某个变量，我们在这里可以接收，比如我们需要传入模块名moduleName， 那么这个注解参数就传入`moduleName`，然后在每个模块里面传入参数：

```kotlin
kapt {
    arguments {
        arg("moduleName", project.getName())
    }
}
```

这样可以动态的将模块名传递到注解处理器中，需要注意的是，每个模块都会执行一次注解处理器。

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201204161353729.jpg?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzIyMDkwMDcz,size_16,color_FFFFFF,t_70)

编译 一下可以看到，打印了我们的模块名，这个将会作为组名应用到工程中。最后别忘了在Activity中 应用我们的注解，否则process方法将不会执行。

```kotlin
@IRouter("order/list")
class OrderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)
    }
}
```

```kotlin
@IRouter("order/detail")
class OrderDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)
    }
}
```

这时我们可以在process方法中打印一下：

```kotlin
override fun process(
    annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?
): Boolean {

    if (annotations.isNullOrEmpty() || roundEnv == null) {
        mMessage.printMessage(Diagnostic.Kind.NOTE, "没有地方使用注解")
        return false
    }

    // 获取所有的被注解的节点
    val elements = roundEnv.getElementsAnnotatedWith(IRouter::class.java)

    elements.forEach {
        mMessage.printMessage(Diagnostic.Kind.NOTE, "类名：${it}")
    }

    return true
}
```

可以看到以下打印，说明我们已经配置成功啦。

```
注: 类名：com.kangf.art.order.OrderActivity
注: 类名：com.kangf.art.order.OrderDetailActivity
```

#### 生成path

下面开始生成path，因为每一个模块都生成一个pathMap，map中有多个path，所以我们定义一个map进行分类：

```kotlin
private var mModuleName: String? = null

override fun process(
        annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?
    ): Boolean {

        if (annotations.isNullOrEmpty() || roundEnv == null) {
            mMessage.printMessage(Diagnostic.Kind.NOTE, "没有地方使用注解")
            return false
        }

        // 获取所有的被注解的节点
        val elements = roundEnv.getElementsAnnotatedWith(IRouter::class.java)

        // 获取activity的类型，转换成TypeMirror，用于判断
        val activityType = mElementUtils.getTypeElement(ProcessorConfig.ACTIVITY_PACKAGE).asType()
        // 获取fragment的类型，转换成TypeMirror，用于判断
        val fragmentType = mElementUtils.getTypeElement(ProcessorConfig.FRAGMENT_PACKAGE).asType()

        elements.forEach {
            val className = it.simpleName.toString()
            mMessage.printMessage(Diagnostic.Kind.NOTE, "类名：${className}")

            // 获取注解的path变量
            val iRouter = it.getAnnotation(IRouter::class.java)
            val path = iRouter.path

            // 严谨性，进行判空
            if (path.isEmpty()) {
                mMessage.printMessage(Diagnostic.Kind.NOTE, "${className}中path不能为空")
            }

            // 严谨性，进行判空
            if(mModuleName.isNullOrEmpty()) {
                mMessage.printMessage(Diagnostic.Kind.NOTE,
                    """
                        |请在gradle中进行配置
                        |kapt {
                        |    arguments {
                        |        arg("moduleName", project.getName())
                        |     }
                        |}
                    """.trimMargin())
            }

            // 生成RouteBean
            val routeBean = RouteBean().apply {
                this.group = mModuleName
                this.path = iRouter.path
                this.element = it
            }

            when {
                mTypeTools.isSubtype(it.asType(), activityType) -> {
                    // 如果被注解的类型是Activity
                    routeBean.typeEnum = RouteBean.TypeEnum.ACTIVITY
                }
                mTypeTools.isSubtype(it.asType(), fragmentType) -> {
                    // 如果被注解的类型是Fragment
                    routeBean.typeEnum = RouteBean.TypeEnum.FRAGMENT
                }
                else -> {
                    // 否则报错
                    mMessage.printMessage(
                        Diagnostic.Kind.ERROR,
                        "@IRouter注解目前仅限用于Activity和Fragment类之上"
                    )
                }
            }

            // 在mPathMap集合中塞数据
            val routeBeanList = mPathMap[routeBean.group]

            if (routeBeanList.isNullOrEmpty()) {
                val list = mutableListOf<RouteBean>()
                list.add(routeBean)
                mPathMap[routeBean.group!!] = list
            } else {
                routeBeanList.add(routeBean)
            }
        }
    
    	// 打印map
        mMessage.printMessage(Diagnostic.Kind.NOTE, "$mPathMap")

        return true
    }
```

上面代码很简单，就是为了在map中塞数据，如果还有不明白的，我们看一下map的打印结果：

```
map component_goods--- > [com.kangf.router.annotation.bean.RouteBean@4039151c]

map component_order--- > [com.kangf.router.annotation.bean.RouteBean@6c3ea76c, 			com.kangf.router.annotation.bean.RouteBean@c2ce562]
```

可以看到，goods模块中有一个注解，order模块中有两个注解。OK，接下来我们要根据map生成path文件，这个path应该是什么样子呢？我们再把上面的代码拿过来参考：

```kotlin
class OrderRouterPath : IRouterPath {

    override fun getPath(): Map<String, RouteBean> {

        val map = mutableMapOf<String, RouteBean>()

        // 订单列表
        map["component_order/list"] = RouteBean().apply {
            group = "component_order"
            path = "component_order/list"
            clazz = OrderActivity::class.java
            typeEnum = RouteBean.TypeEnum.ACTIVITY
        }

        // 订单详情
        map["component_order/detail"] = RouteBean().apply {
            group = "component_order"
            path = "component_order/detail"
            clazz = OrderDetailActivity::class.java
            typeEnum = RouteBean.TypeEnum.ACTIVITY
        }

        return map
    }
}
```

好，那么我们就动态的生成这类文件，根据kotlinpoet官网学到的，首先创建方法，再创建类，再把方法加入到类中，大致上就是这么一个流程：

```kotlin
private fun generatePathFile() {

//        class OrderRouterPath : IRouterPath {
//
//            override fun getPath(): Map<String, RouteBean> {
//                val map = mutableMapOf<String, RouteBean>()
//                // 订单详情
//                map["component_order/detail"] = RouteBean().apply {
//                    group = "component_order"
//                    path = "component_order/detail"
//                    clazz = OrderDetailActivity::class.java
//                    typeEnum = RouteBean.TypeEnum.ACTIVITY
//                }
//
//                return map
//            }
//        }

        // --------------------------- 方法创建开始 --------------------------- //

        // 获取 某个模块的List<RouteBean>
        val routeList = mPathMap[mModuleName]
        if (routeList.isNullOrEmpty()) {
            mMessage.printMessage(Diagnostic.Kind.NOTE, "${mModuleName}中没有地方使用注解")
            return
        }

        // 方法返回类型，泛型为String，RouteBean
        val returnType = Map::class.java.asClassName().parameterizedBy(
            String::class.java.asTypeName().javaToKotlinType(),
            RouteBean::class.asTypeName().javaToKotlinType()
        ).javaToKotlinType()

        // 创建方法，方法名为 getPath
        val funcSpecBuilder = FunSpec.builder(ProcessorConfig.PATH_METHOD_NAME)
            // override关键字
            .addModifiers(KModifier.OVERRIDE)
            // 返回map
            .returns(returnType)
            .addStatement(
                "val %N = mutableMapOf<%T, %T>()",
                ProcessorConfig.PATH_VAR_MAP,
                String::class.java.asTypeName().javaToKotlinType(),
                RouteBean::class.java
            )

        // 添加语句
        routeList.forEach {
            funcSpecBuilder.addStatement(
                """
                    |%N[%S] = %T().apply { 
                    |   group = %S
                    |   path = %S
                    |   clazz = %T::class.java
                    |   typeEnum = %T.%L
                    |}
                    |
                """.trimMargin(),

                ProcessorConfig.PATH_VAR_MAP,
                it.path ?: "",
                RouteBean::class.java,
                it.group ?: "",
                it.path ?: "",
                it.element!!.asType().asTypeName(),
                RouteBean.TypeEnum::class.java,
                it.typeEnum!!
            )
        }

        funcSpecBuilder.addStatement("return %N", ProcessorConfig.PATH_VAR_MAP)
        // --------------------------- 方法创建完成 --------------------------- //

        // --------------------------- 类创建开始 --------------------------- //
        val superInter = ClassName("com.kangf.router.api", "IRouterPath")
        val fileName = "RouterPath_${mModuleName}"
        val typeSpec = TypeSpec.classBuilder(fileName)
    		// 类中添加方法
            .addFunction(funcSpecBuilder.build())
    		// 实现IRouterPath
            .addSuperinterface(superInter)
            .build()

        // 创建文件
        FileSpec.builder(mGeneratePackage, fileName)
            .addType(typeSpec)
            .build()
    		// 写入文件
            .writeTo(mFiler)

        // --------------------------- 类创建结束 --------------------------- //
    
    	mGrop
    }
```

上面的代码看着很多，其实很简单，每个方法看名字都能大概知道什么意思，每一行我都有注释，有兴趣的可以自己玩玩。最后生成出来的文件是这样的：

```kotlin
package com.kangf.route.generate

import com.kangf.router.`annotation`.bean.RouteBean
import com.kangf.router.api.IRouterPath
import kotlin.String
import kotlin.collections.Map

public class RouterPath_component_goods : IRouterPath {
  public override fun getPath(): Map<String, RouteBean> {
    val pathMap = mutableMapOf<String, RouteBean>()
    pathMap["goods/list"] = RouteBean().apply { 
           group = "component_goods"
           path = "goods/list"
           clazz = RouteBean::class.java
           typeEnum = RouteBean.TypeEnum.ACTIVITY
        }

    return pathMap
  }
}
```

#### 生成group

这样路径对应的封装就完成了，下面还有group封装，就相对简单多了，还是一样的道理，先看看我们上面封装的模板：

```kotlin
class OrderRouterGroup : IRouterGroup {

    override fun getGroupMap(): MutableMap<String, Class<out IRouterPath>> {
        val map = mutableMapOf<String, Class<out IRouterPath>>()
        map["component_order"] = OrderRouterPath::class.java
        return map

    }

}
```

下面是生成GroupFile的方法：

```kotlin
private fun generateGroupFile() {
//        class OrderRouterGroup : IRouterGroup {
//
//            override fun getGroupMap(): MutableMap<String, Class<out IRouterPath>> {
//                val map = mutableMapOf<String, Class<out IRouterPath>>()
//                map["component_order"] = OrderRouterPath::class.java
//                return map
//
//            }
//
//        }

        // 方法返回类型，泛型为String，RouteBean

        val routePathInter = ClassName("com.kangf.router.api", "IRouterPath")

        val returnType = MutableMap::class.java.asClassName().parameterizedBy(
            String::class.java.asTypeName().javaToKotlinType(),
            Class::class.java.asClassName().parameterizedBy(
                WildcardTypeName.producerOf(routePathInter)
            )
        ).javaToKotlinType()

        // path对应的类名
        val putClazz = ClassName(mGeneratePackage, "RouterPath_${mModuleName}")

        val funSpec = FunSpec.builder(ProcessorConfig.GROUP_METHOD_NAME)
            .returns(returnType)
            .addModifiers(KModifier.OVERRIDE)
            .addStatement(
                "val %N = mutableMapOf<%T, %T>()",
                ProcessorConfig.GROUP_VAR_MAP,
                String::class.java.asTypeName().javaToKotlinType(),
                Class::class.java.asClassName().parameterizedBy(
                    WildcardTypeName.producerOf(routePathInter)
                )
            )
            .addStatement(
                "%N[%S] = %T::class.java",
                ProcessorConfig.GROUP_VAR_MAP,
                mModuleName ?: "",
                putClazz
            )
            .addStatement("return %N", ProcessorConfig.GROUP_VAR_MAP)
            .build()

        val superInter = ClassName("com.kangf.router.api", "IRouterGroup")
        val fileName = "RouteGroup_${mModuleName}"

        val typeSpec = TypeSpec.classBuilder(fileName)
            .addSuperinterface(superInter)
            .addFunction(funSpec)
            .build()

        FileSpec.builder(mGeneratePackage, fileName)
            .addType(typeSpec)
            .build()
            .writeTo(mFiler)


    }
```

最终生成的文件是这样的：

```kotlin
package com.kangf.route.generate

import com.kangf.router.api.IRouterGroup
import com.kangf.router.api.IRouterPath
import java.lang.Class
import kotlin.String
import kotlin.collections.Map

public class RouteGroup_component_order : IRouterGroup {
  public override fun getGroupMap(): Map<String, Class<out IRouterPath>> {
    val groupMap = mutableMapOf<String, Class<out IRouterPath>>()
    groupMap["component_order"] = RouterPath_component_order::class.java
    return groupMap
  }
}
```

这样我们的注解处理器就算完成了~！接下来再次修改跳转逻辑：

```kotlin
find<TextView>(R.id.tvGoods).setOnClickListener {

            /**
             * 经过注解处理器封装
             */
             // 找到组map
             val groupClazz = Class.forName("com.kangf.route.generate.RouteGroup_component_order")
             val groupInstance = groupClazz.newInstance() as IRouterGroup
             // 通过组找到路径的map
             val pathInstance = (groupInstance.getGroupMap()["component_order"] ?: error("")).newInstance() as IRouterPath
             // 通过路径的map找到组对应的routeBean
             val routeBean = pathInstance.getPath()["order/list"]
             // 找到对应的class进行跳转
             val clazz = routeBean!!.clazz
             startActivity(Intent(this, clazz))


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

```

## 封装跳转逻辑

效果这里就不演示了，跟上面是一样的。当然这还不够，跳转逻辑也是一堆重复性工作，我们何不再封装一层呢？说干就干：

```kotlin
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
```

跳转逻辑再次优化，实现一行代码即可跳转，我们添加个按钮，改造一下：

```kotlin
find<TextView>(R.id.tvGoods).setOnClickListener {
	// 跳转到订单列表
    IRouterUtils.build("order/list").navigation(this)
}

find<TextView>(R.id.tvDetail).setOnClickListener {
	// 跳转到订单详情
    IRouterUtils.build("order/detail").navigation(this)
}
```

看一下效果吧

<img src="https://img-blog.csdnimg.cn/20201207104722490.gif" alt="在这里插入图片描述" style="zoom:25%;" />

## 总结

上面讲的其实就是ARouter的组件化原理 ，当然我们许多逻辑尚不完善，但研究ARouter源码已经足够了，项目已经上传到GitHub，有兴趣的过来看看吧！
