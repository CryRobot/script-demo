# 



# Java语言的动态性支持

### 引言

在Java项目中，或多或少我们有动态执行代码的需求，比如：

- 系统中有一个规则验证需求，但规则经常改变
- 代码热更新，热修复

笔者也在目前参与的一个项目中遇到了动态执行代码的需求：项目需要一个自动审核模块，但是审核规则根据相关书面文件制定，如果写死在.java文件里，那么当新的书面文件下发时，就要系统停机更新系统，然后才能继续使用，其中存在着很多不稳定因素，也很麻烦。因此在设计上就有动态执行代码的需求。好在这个需求只是审核一个表单，并没有对系统的操作和IO操作，输入参数也很固定。

笔者上网查阅了大量资料，发现网上大致流传三种动态执行代码方式，笔者经过全面比较，选择了其中一种。这里将几种方法列举如下。

### 方法

#### 1.使用JEXL动态执行表达式

参考[利用JEXL实现动态表达式编译 - Narcssus的专栏 - CSDN博客](https://blog.csdn.net/u012468264/article/details/56679802)

JEXL支持两种循环方式:

```java
for(item : list) {
    x = x + item;
}
```


和

和

```
while (x lt 10) {
    x = x + 2;
}
```

- 优点：可以动态执行Java代码，调用Java Function（Function需先传入JexlContext）
- 缺点：只能执行一个“表达式”，而不是Function，所以有很多语法局限，不是真正执行一个Function





#### 2.使用Java动态编译

参考[[改善Java代码\]慎用动态编译 - SummerChill - 博客园](https://www.cnblogs.com/DreamDrive/p/5417431.html)

```
动态编译一直是Java的梦想，从Java 6版本它开始支持动态编译了，可以在运行期直接编译.java文件，执行.class，并且能够获得相关的输入输出，甚至还能监听相关的事件。不过，我们最期望的还是给定一段代码，直接编译，然后运行，也就是空中编译执行（on-the-fly）。
```

- 优点：功能强大，能够真正实现完整的动态执行功能，能够动态调用全部系统功能和IO操作。
- 缺点：虽然功能强大，可以编译.java文件，但是还是很难在运行时替换框架级的类文件，但是相比于上述方法已经有过之而无不及了；能动态调用全部系统功能和IO操作，与一般代码环境没有隔离，从而会成为项目中一个非常严重的安全隐患处。





#### 3.使用Java ScriptEngine

使用Java自带的ScriptEngine可以说是最完美的Java动态执行代码方案之一（不考虑代码热更新等场景），关于ScriptEngine网上有大量资料可供参考，这里就不附参考资料了，简单提供下一个使用JS Engine的例子：

```java
String regular="function regular(args1,args2,args3){................}";
ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
try {
	engine.eval(regular);
	if (engine instanceof Invocable) {
		Invocable invoke = (Invocable) engine;
		String result = (String) invoke.invokeFunction(
				"regular", 
				args1,
				args2,
				args3);
		System.out.println(result);
		} else {
			System.out.println("error");
		}
	}
} catch (ScriptException e) {
	System.out.println("表达式runtime错误:" + e.getMessage());
}
```


使用eval()，动态执行一遍JS代码（包含一个JS function），然后利用Java的Invoke传入参数，最后获取返回值。

- 优点：可以执行完整的JS方法，并且获取返回值；在虚拟的Context中执行，无法调用系统操作和IO操作，非常安全；可以有多种优化方式，可以预编译，编译后可以复用，效率接近原生Java；所有实现ScriptEngine接口的语言都可以使用，并不仅限于JS，如Groovy，Ruby等语言都可以动态执行。
- 缺点：无法调用系统和IO操作 ，也不能使用相关js库，只能使用js的标准语法。更新：可以使用scriptengine.put()将Java原生Object传入Context，从而拓展实现调用系统和IO等操作。
  对于一般的动态执行代码需求，建议使用最后一种方法。



## ScriptManager

### JAVA脚本API

JDK提供了一套标准的脚本语言接口，方便Java应用与各种脚本引擎的交互。**javax.script** 包定义了这些接口，即 Java 脚本编程 API

### 脚本引擎

　脚本引擎就是脚本解释器，负责运行脚本，获取运行结果。JDK提供了脚本引擎的接口ScriptEngine ，Java 应用程序通过这个接口调用脚本引擎运行脚本程序，并将运行结果返回给虚拟机。

　　JDK提供ScriptEngineManager发现和创建脚本引擎，ScriptEngineManager 通过Java的服务发现机制Service Provider发现服务，过程如下：

1. 首先，ScriptEngineManager 实例在当前 classpath 中搜索所有可见的 Jar 包，查看每个 Jar 包中的 META -INF/services/ 目录下的是否包含 javax.script.ScriptEngineFactory 文件，脚本引擎的开发者会提供在 Jar 包中包含一个 ScriptEngineFactory 接口的实现类，这个文件内容即是这个实现类的完整名字；
2. ScriptEngineManager 根据这个类名，创建一个 ScriptEngineFactory 接口的实例；
3. 最后，通过ScriptEngineFactory 实例创建脚本引擎，返回给用户。
   ScriptEngineManager被实例化的时候，服务发现的动作被执行：



![è¿éåå¾çæè¿°](https://img-blog.csdn.net/20171030123812024?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQveWFuZ2d1b3Ni/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)



### 发现和创建脚本引擎



　在 javax.script 支持的每个脚本引擎也有各自对应的执行的环境，脚本引擎可以共享同样的环境，也可以有各自不同的上下文。通过脚本运行时的上下文，脚本程序就能自由的和 Java 平台交互。javax.script.ScriptContext 接口和 javax.script.Bindings 接口定义了脚本引擎的上下文。
Bindings： map容器，存储各种属性值；
ScriptContext： 不同scope的Bindings的集合。ScriptContext 接口默认包含了两个级别的 Bindings 实例的引用，分别是全局级别GLOBAL_SCOPE 和引擎级别ENGINE_SCOPE 。全局级别指的是 Bindings 里的属性都是“全局变量”，只要是同一个 ScriptEngineMananger 返回的脚本引擎都可以共享这些属性；对应的，引擎级别的 Bindings 里的属性则是“局部变量”，它们只对同一个引擎实例可见，从而能为不同的引擎设置独特的环境，通过同一个脚本引擎运行的脚本运行时能共享这些属性。

![è¿éåå¾çæè¿°](https://img-blog.csdn.net/20171030114949427?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQveWFuZ2d1b3Ni/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)



### 脚本引擎可选接口

- Invocable 接口：允许 Java 平台调用脚本程序中的函数或方法。它提供了两个函数 invokeFunction 和 invokeMethod，分别允许 Java 应用程序直接调用脚本中的一个全局性的过程以及对象中的方法，调用后者时，除了指定函数名字和参数外，还需要传入要调用的对象引用。
- Compilable 接口：允许 Java 平台编译脚本程序，供多次调用。

### **脚本引擎带来问题**

1. **安全控制：** 如死循环、调用System.exit(0)导致JVM退出；
2. **资源隔离：** CPU、内存等进行限制和隔离；
3. **性能问题：** 对脚本执行时间进行控制，有超时中断机制；