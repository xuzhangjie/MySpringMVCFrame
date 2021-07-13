# MySpringMVCFrame
自定义SpringMVC框架学习笔记
# 自定义Spring `MVC`框架



## 一、笔记



### 1.1 建立`Controller`和`RequestMapping`注解

以便我们可以使用自己的注释对控制器类和方法进行标记

- 创建`Controller`注解

  ```java
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface RequestMapping {
      String value() default "";
  }
  ```

- 创建`RequestMapping`注解

  ```java 
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface RequestMapping {
      String value() default "";
  }
  ```

### 1.2 创建ControllerMapping类

Controller Mapping类主要用来封装一个类及其一个方法，在后续的使用中是为了存储数据信息，以便根据浏览器的请求映射到相应的类的请求方法上，详细作用后续给出。

```java
package cn.edu.guet.mvc;

import java.lang.reflect.Method;

/**
 * Author liwei Date 2018/8/30 10:35 Version 1.0 控制器元数据信息类，封装了控制器类的相关信息: 
 1. 控制器类实例 
 2. 处理请求的方法对象
 */
public class ControllerMapping {
    /* 业务控制器类实例,  UserController, BookController ......*/
    private Class<?> controllerClass;
    
    /* 业务控制器类实例的目标方法，即标注了@RequestMapping的方法实例*/
    private Method handleMethod;

    public ControllerMapping() {
    }

    public ControllerMapping(Class<?> controllerClass, Method handleMethod) {
        this.controllerClass = controllerClass;
        this.handleMethod = handleMethod;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public void setControllerClass(Class<?> controllerClass) {
        this.controllerClass = controllerClass;
    }

    public Method getHandleMethod() {
        return handleMethod;
    }

    public void setHandleMethod(Method handleMethod) {
        this.handleMethod = handleMethod;
    }

    @Override
    public String toString() {/*UserController.login*/
        return "控制器类: " + controllerClass.getSimpleName() + "." + handleMethod.getName();
    }
}
```



### 1.3 创建ContextConfigListener监听器

上下文监听器、session监听器和session属性监听器

Sercvlet3.0版本后支持注解方式实现监听器@WebListener(),也可在web.xml里加入监听器配置,如果不配置，监听器不执行。

```xml
<listener>
    <listener-class>cn.edu.guet.mvc.ContextConfigListener</listener-class>
</listener>
```

该监听器的作用是在Tomcat启动的时候自动执行Configuration类的config方法,扫描包并获取ControllerMapping类的信息，组装成Map，以便请求到达时直接在Map中根据key值url去查询相应的方法响应。

该类的初始化代码如下：

```java
package cn.edu.guet.mvc;
/*
 * @Author liwei @Date 2020/9/5 10:57 @Version 1.0
 */

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.net.URISyntaxException;
import java.util.Map;

/** Servlet3.0版本后支持注解方式实现监听器 */ /*@WebListener()*/
public class ContextConfigListener implements ServletContextListener {
    public ContextConfigListener() {
    }

    public void contextInitialized(ServletContextEvent sce) {
        /* This method is called when the servlet context is initialized(when the Web application is deployed). You can initialize servlet context related data here. */  
        try {
            /*使用Configuration类的config()方法得到查询完成的ControllerMapping类封装成的Map数据，该Map数据的作用是当浏览器发来请求时，DispatcherServlet根据请求的uri为关键字查询该Map，然后使用反射机制调用相应Controller标记的类中使用RequestMapping标记的方法响应请求*/
            Map<String, ControllerMapping> controllerMapping = new Configuration().config();
          
            /*将map数据传到上下文（全局对象中），后期根据上下文得到该数据
            */
            sce.getServletContext().setAttribute("cn.guet.web.controller",            					controllerMapping);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void contextDestroyed(ServletContextEvent sce) {
        /* This method is invoked when the Servlet Context (the Web application) is 				   undeployed or Application Server shuts down. */
    }
}
```



### 1.4 创建Configuration类

该类即是1.2中提到的Configuration类，完成上一步中的任务，该类读取controller包中的class文件，加载所有使用了Controller注解标记的控制器类及其中使用了RequestMapping注解的请求方法，并将这些信息存入`ControllerMapping`的实例中,实现过程如下：

- 读取`config.propertis`,获取`controller.package`的内容

  ```java
    ResourceBundle bundle = ResourceBundle.getBundle("config");
          /*
          获取控制器包名称
          例如：cn.edu.guet.controller
           */
   String controllerPackageName = bundle.getString("controller.package");
  ```

- 把包名转换为路程

  ```java
   String path = controllerPackageName.replace(".", "/");
   URI uri= Configuration.class.getResource("/"+path).toURI();
   File controllerDirectory = new File(uri);
  ```

- 筛选出包路径中的class文件

  ```java
    String[] controllerFileNames = controllerDirectory.list();
  
          for (String className : controllerFileNames) {
             
              if (className.endsWith(".class")) {
             System.out.println("***:" +className);
              }
  ```

- 获取类名（包名+类名），然后拿到RequestMapping 标注的的方法并存入Controller Mapping中的实例中

  ```java
   for (String className : controllerFileNames) {
   if (className.endsWith(".class")) {
  	String fullClassName = controllerPackageName + "." + 		                                                StringUtils.substringBefore(className, ".class");
  	try {
              Class controllerClass = Class.forName(fullClassName);
  
              //找出哪些类上使用了Controller注解
  
             if (controllerClass.isAnnotationPresent(Controller.class)) {
  
              // 找出哪些方法使用了RequestMapping注解
   System.out.println("Controller注解的类："+controllerClass.getSimpleName());
   Method methods[] = MethodUtils.getMethodsWithAnnotation(controllerClass, RequestMapping.class);
   for (Method method : methods) {
  
  //    获取到RequestMapping注解的值：值就是url，相当于原来的web.xml中的url-pattern中的内容
    RequestMapping annotation = method.getAnnotation(RequestMapping.class);
   ControllerMapping mapping=new ControllerMapping(controllerClass,method);
    controllerMapping.put(annotation.value(),mapping);
      //    System.out.println("注解的值：" + annotation.value());
                          }
           }
        } catch (ClassNotFoundException e) {
                      e.printStackTrace();
                  }
              }
          }
  
  //     Map<String, ControllerMapping> controllerMapping = new HashMap<String, ControllerMapping>();
  ```

完整代码：

```java
package cn.edu.guet.mvc;

import cn.edu.guet.mvc.annotation.Controller;
import cn.edu.guet.mvc.annotation.RequestMapping;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class Configuration {
    public Map<String, ControllerMapping> config() throws URISyntaxException {
        Map<String, ControllerMapping> controllerMapping = new HashMap<String, 			             ControllerMapping>();
        
        //得到资源文件resources下的config.propertis文件，为了得到该文件里配置的controller包的地址信息
        ResourceBundle bundle = ResourceBundle.getBundle("config");
      
        //获取其中的参数信息
        String controllerPackageName = bundle.getString("controller.package");
     
        /* 把控制器包转成具体路径 */
        String path = controllerPackageName.replace(".", "/");
        URI uri = Configuration.class.getResource("/" + path).toURI();
        File controllerDirectory = new File(uri);
        
        /* 筛选出路径下所有的class文件和全类名*/
        String[] controllerFileNames = controllerDirectory.list();
        for (String className : controllerFileNames)
            if (className.endsWith(".class")) {
                String fullClassName = controllerPackageName + "." + StringUtils.substringBefore(className, ".class");
                try {
                    Class controllerClass = Class.forName(fullClassName);
                    
                    /*找出哪些类上使用了Controller注解*/
                    if (controllerClass.isAnnotationPresent(Controller.class)) {
                        
                        /* 找出哪些方法使用了RequestMapping注解*/
                        System.out.println("Controller注解的类：" + controllerClass.getSimpleName());
                        
                        Method methods[] = MethodUtils.getMethodsWithAnnotation(controllerClass, RequestMapping.class);
                       
                        for (Method method : methods) {
         /*    获取到RequestMapping注解的值：值就是url，相当于原来的web.xml中的url-pattern中的内容*/
                            RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                            
                            ControllerMapping mapping = new ControllerMapping(controllerClass, method);
                            
                            controllerMapping.put(annotation.value(), mapping);
                            /*    System.out.println("注解的值：" + annotation.value());*/
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        return controllerMapping;
    }
}

```

### 1.5 创建DispatcherSerclet类

在SpringMVC的框架中，不用给对应的请求响应再写一个的Servet类去解决，SpringMVC的原理中，DispatcherServlet类是唯一一个Servet，它处理来自前端的请求，再找到相应的相应方法，去回应浏览器，这里如何响应便是一个主要问题，前面提到的一个map数据里封装了一个String类关键字和Controller Mapping类的值，关键字就是请求的URI，值就是该请求对应的类的响应方法，在DispatcherServelt类中处理请求的URI,再去map数据中查找是否包含该URI的信息，再使用反射机制调用响应的方法去响应请求。在这过程中，有以下两点主要任务：

- 前端与方法的参数传递
- 方法的返回值响应

完整代码：

````java
package cn.edu.guet.mvc;

import com.google.gson.GsonBuilder;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet(name = "DispatcherServlet")
public class DispatcherServlet extends HttpServlet {
    Map<String, ControllerMapping> controllerMapping;

    public void init(ServletConfig config) {
        //从上下文中获取保存的请求响应数据map
        controllerMapping = (Map<String, ControllerMapping>) config.getServletContext().getAttribute("cn.guet.web.controller");
        /* 进行遍历 */
       // System.out.println(controllerMapping);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request,response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            //从浏览器请求得到URI
            String uri = request.getRequestURI();
         
            //将URI中的对应位置取出来和数据map对比
            uri = uri.substring(uri.indexOf("/", 1) + 1);
            ControllerMapping mapping = null;
         
            if (controllerMapping.containsKey(uri))
             mapping = controllerMapping.get(uri);//从数据map中得到请求对应的ControllerMapping
            /* System.out.print(mapping.getControllerClass().getSimpleName()); System.out.println("\t的"+mapping.getHandleMethod().getName()+"方法处理"+uri+"这个请求");*/
            Class controllerMappingClass = mapping.getControllerClass();
            Method method = mapping.getHandleMethod();/* 反射获取参数名称 */
            Class[] parameterType = method.getParameterTypes();
            List<String> parameterList = new ArrayList<String>();   /*list的特点：有序可重复，满足存储参数的条件*/
            Parameter[] params = method.getParameters();
            for (Parameter parameter : params) {
                System.out.println("参数名字：" + parameter.getName());
                parameterList.add(parameter.getName());
            }
            Object[] parameterValues = new Object[parameterType.length];
            for (int i = 0; i < parameterType.length; i++) { 
                /* 8种基本类型 Boolean、byte、 short、int、long、double、flaot、char */
                
                if (parameterType[i].isPrimitive()) {   /*若参数的类型为基本类型*/
                    if (parameterType[i].getTypeName().equals("int"))  //整型
                    {
                        parameterValues[i] = Integer.parseInt(request.getParameter(parameterList.get(i)));
                    }
                    else if (parameterType[i].getTypeName().equals("double")) //double型
                    {
                        
                     parameterValues[i] = Double.parseDouble(request.getParameter(parameterList.get(i)));
                    }
                    //······
                    
                } else if (ClassUtils.isAssignable(parameterType[i], String.class)) {  /*若参数为String*/
                    
                    parameterValues[i] = request.getParameter(parameterList.get(i));
                    
                } else {   /*bean*/
                    //创建该类型的实例
                    Object pojo = parameterType[i].newInstance();
                    //从浏览器请求中得到参数变量名和值
                    Map<String, String[]> parameterMap = request.getParameterMap();
                     //beanutils会自动将map里的key与bean的属性名进行反射赋值
                    BeanUtils.populate(pojo, parameterMap);
                    parameterValues[i] = pojo;
                }
            }
            //创建请求对应得类的实例
            Object obj = controllerMappingClass.newInstance();
            //调用该类里的方法并传参
            Object returnValue = method.invoke(obj, parameterValues);
            
            /*返回值处理*/
            String path = returnValue.toString();
            if (returnValue != null && returnValue instanceof String) if (((String) returnValue).startsWith("forward:"))   //转发
                request.getRequestDispatcher(StringUtils.substringAfter(path, "forward:")).forward(request, response);
            else if (((String) returnValue).startsWith("redirect:"))  //重定向
                response.sendRedirect(StringUtils.substringAfter(path, "redirect"));
            else if (returnValue != null && !(returnValue instanceof String)) {
                //将数据以JSON格式传给前端
                response.setContentType("application/json;charset=UTF-8");
                String json = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").setPrettyPrinting().create().toJson(returnValue);
                PrintWriter out = response.getWriter();
                out.write(json);
                out.flush();
                out.close();
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}

````

### 1.6 pom.xml中添加的依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>cd.edu.lanqiao</groupId>
    <artifactId>MySpringMVCFrame</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>
    <name>MySpringMVCFrame Maven Webapp</name><!-- FIXME change it to the project's website -->
    <url>http://www.example.com</url>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.7</maven.compiler.source>
        <maven.compiler.target>1.7</maven.compiler.target>
    </properties>
    <dependencies>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.11</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>3.1.0</version>
        </dependency>
        <dependency>
            <groupId>jstl</groupId>
            <artifactId>jstl</artifactId>
            <version>1.2</version>
        </dependency>
        <dependency>
            <groupId>taglibs</groupId>
            <artifactId>standard</artifactId>
            <version>1.1.2</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.15</version>
        </dependency>
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.8.7</version>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.9</version>
        </dependency>
        <dependency>
            <groupId>commons-beanutils</groupId>
            <artifactId>commons-beanutils</artifactId>
            <version>1.9.2</version>
        </dependency>
    </dependencies>
    <build>
        <finalName>MySpringMVCFrame</finalName>
        <pluginManagement><!-- lock down plugins versions to avoid using Maven defaults (may be moved to parent pom) -->
            <plugins>
                <plugin>
                    <artifactId>maven-clean-plugin</artifactId>
                    <version>3.1.0</version>
                </plugin><!-- see http://maven.apache.org/ref/current/maven-core/default-bindings.html#Plugin_bindings_for_war_packaging -->
                <plugin>
                    <artifactId>maven-resources-plugin</artifactId>
                    <version>3.0.2</version>
                </plugin>
                <plugin>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                </plugin>
                <plugin>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>2.22.1</version>
                </plugin>
                <plugin>
                    <artifactId>maven-war-plugin</artifactId>
                    <version>3.2.2</version>
                </plugin>
                <plugin>
                    <artifactId>maven-install-plugin</artifactId>
                    <version>2.5.2</version>
                </plugin>
                <plugin>
                    <artifactId>maven-deploy-plugin</artifactId>
                    <version>2.8.2</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <source>1.8</source>
                        <target>1.8</target>
                        <compilerArgs>
                            <arg>-parameters</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>

```




