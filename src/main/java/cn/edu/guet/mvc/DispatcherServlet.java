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
        controllerMapping = (Map<String, ControllerMapping>) config.getServletContext().getAttribute("cn.guet.web.controller");/* 进行遍历 */
        System.out.println(controllerMapping);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String uri = request.getRequestURI();
            System.out.println("请求的uri（1):" + uri);
            uri = uri.substring(uri.indexOf("/", 1) + 1);
            ControllerMapping mapping = null;
            System.out.println("请求的uri(2):" + uri);
            if (controllerMapping.containsKey(uri))
                mapping = controllerMapping.get(uri);/* System.out.print(mapping.getControllerClass().getSimpleName()); System.out.println("\t的"+mapping.getHandleMethod().getName()+"方法处理"+uri+"这个请求");*/
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
            for (int i = 0; i < parameterType.length; i++) { /* 8种基本类型 Boolean、byte、 short、int、long、double、flaot、char */
                if (parameterType[i].isPrimitive()) {   /*若参数的类型为基本类型*/
                    if (parameterType[i].getTypeName().equals("int"))
                        parameterValues[i] = Integer.parseInt(request.getParameter(parameterList.get(i)));
                    else if (parameterType[i].getTypeName().equals("double"))
                        parameterValues[i] = Double.parseDouble(request.getParameter(parameterList.get(i)));
                } else if (ClassUtils.isAssignable(parameterType[i], String.class)) {  /*若参数为String*/
                    parameterValues[i] = request.getParameter(parameterList.get(i));
                } else {   /*bean*/
                    Object pojo = parameterType[i].newInstance();
                    Map<String, String[]> parameterMap = request.getParameterMap();
                    BeanUtils.populate(pojo, parameterMap);
                    parameterValues[i] = pojo;
                }
            }
            Object obj = controllerMappingClass.newInstance();
            Object returnValue = method.invoke(obj, parameterValues);/*返回值处理*/
            String path = returnValue.toString();
            if (returnValue != null && returnValue instanceof String) if (((String) returnValue).startsWith("forward:"))
                request.getRequestDispatcher(StringUtils.substringAfter(path, "forward:")).forward(request, response);
            else if (((String) returnValue).startsWith("redirect:"))
                response.sendRedirect(StringUtils.substringAfter(path, "redirect"));
            else if (returnValue != null && !(returnValue instanceof String)) {
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
