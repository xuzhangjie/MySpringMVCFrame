package cn.edu.guet.mvc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) //将来Controller注解可以作用在类上
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {

}
