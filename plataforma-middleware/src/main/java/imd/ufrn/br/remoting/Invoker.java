package imd.ufrn.br.remoting;

import imd.ufrn.br.exceptions.InvocationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Invoker {

    public Invoker() {
        // Constructor is now empty as LookupService is no longer needed for direct invocation.
    }

    public Object invoke(Object targetObject, Method method, Object[] args) throws Throwable {
        if (targetObject == null || method == null) {
            throw new IllegalArgumentException("Target object and method cannot be null for invocation.");
        }

        System.out.println("Invoker: Directly invoking " + targetObject.getClass().getName() +
                "#" + method.getName() + " with args: " + Arrays.toString(args));

        try {
            Object result = method.invoke(targetObject, args);
            System.out.println("Invoker: Method " + method.getName() + " executed successfully. Result: " + result);
            return result;
        } catch (IllegalAccessException e) {
            System.err.println("Invoker: Illegal access while invoking method - " + e.getMessage());
            throw new InvocationException("Illegal access during method invocation: " + method.getName(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            System.err.println("Invoker: Exception thrown by target method " + method.getName() + " - " + cause.getMessage());
            throw cause;
        }
    }
}