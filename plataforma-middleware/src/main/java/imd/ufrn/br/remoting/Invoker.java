package imd.ufrn.br.remoting;

import imd.ufrn.br.exceptions.InvocationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Invoker {

    public Object invoke(Object targetObject, Method method, Object[] args) throws Throwable {
        if (targetObject == null || method == null) {
            throw new IllegalArgumentException("Target object and method cannot be null for invocation.");
        }

        try {
            return method.invoke(targetObject, args);
        } catch (IllegalAccessException e) {
            throw new InvocationException("Illegal access during method invocation: " + method.getName(), e);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}