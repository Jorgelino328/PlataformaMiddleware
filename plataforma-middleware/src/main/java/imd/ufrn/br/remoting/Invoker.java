package imd.ufrn.br.remoting;

import imd.ufrn.br.identification.LookupService;
import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.exceptions.InvocationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Invoker {

    private final LookupService lookupService;

    public Invoker(LookupService lookupService) {
        if (lookupService == null) {
            throw new IllegalArgumentException("LookupService cannot be null.");
        }
        this.lookupService = lookupService;
    }

    public Object invoke(ObjectId objectId, String methodName, Object[] args) throws Throwable {
        if (objectId == null) {
            throw new IllegalArgumentException("ObjectId cannot be null for invocation.");
        }
        if (methodName == null || methodName.trim().isEmpty()) {
            throw new IllegalArgumentException("Method name cannot be null or empty for invocation.");
        }

        Object targetObject = lookupService.findObject(objectId);

        System.out.println("Invoker: Attempting to invoke " + targetObject.getClass().getName() +
                "#" + methodName + " with args: " + Arrays.toString(args));

        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                throw new IllegalArgumentException("Null arguments are not robustly supported for method signature matching in this simplified Invoker. " +
                        "Parameter at index " + i + " is null.");
            } else {
                paramTypes[i] = args[i].getClass();
            }
        }

        Method methodToInvoke;
        try {

            methodToInvoke = findCompatibleMethod(targetObject.getClass(), methodName, paramTypes, args);
            if (methodToInvoke == null) {
                throw new NoSuchMethodException("No compatible method '" + methodName +
                        "' found in class " + targetObject.getClass().getName() +
                        " for argument types " + Arrays.toString(paramTypes));
            }

        } catch (NoSuchMethodException e) {
            System.err.println("Invoker: Method not found - " + e.getMessage());
            throw e;
        } catch (SecurityException e) {
            System.err.println("Invoker: Security exception while accessing method - " + e.getMessage());
            throw new InvocationException("Security error accessing method: " + methodName, e);
        }

        try {
            Object result = methodToInvoke.invoke(targetObject, args);
            System.out.println("Invoker: Method " + methodName + " executed successfully. Result: " + result);
            return result;
        } catch (IllegalAccessException e) {
            System.err.println("Invoker: Illegal access while invoking method - " + e.getMessage());
            throw new InvocationException("Illegal access during method invocation: " + methodName, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            System.err.println("Invoker: Exception thrown by target method " + methodName + " - " + cause.getMessage());
            throw cause;
        }
    }

    private Method findCompatibleMethod(Class<?> targetClass, String methodName, Class<?>[] argTypes, Object[] args) {
        Method[] methods = targetClass.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterCount() == argTypes.length) {
                Class<?>[] Kpes = method.getParameterTypes();
                boolean compatible = true;
                for (int i = 0; i < Kpes.length; i++) {
                    Class<?> expectedType = Kpes[i];
                    Class<?> actualType = argTypes[i];

                    if (args[i] == null) {
                        if (expectedType.isPrimitive()) {
                            compatible = false;
                            break;
                        }
                    } else if (expectedType.isPrimitive()) {
                        if (!isWrapperForPrimitive(actualType, expectedType)) {
                            compatible = false;
                            break;
                        }
                    } else if (!expectedType.isAssignableFrom(actualType)) {
                        compatible = false;
                        break;
                    }
                }
                if (compatible) {
                    return method;
                }
            }
        }
        return null;
    }

    private boolean isWrapperForPrimitive(Class<?> wrapper, Class<?> primitive) {
        if (!primitive.isPrimitive()) return false;
        if (primitive == int.class) return wrapper == Integer.class;
        if (primitive == long.class) return wrapper == Long.class;
        if (primitive == double.class) return wrapper == Double.class;
        if (primitive == float.class) return wrapper == Float.class;
        if (primitive == boolean.class) return wrapper == Boolean.class;
        if (primitive == char.class) return wrapper == Character.class;
        if (primitive == byte.class) return wrapper == Byte.class;
        if (primitive == short.class) return wrapper == Short.class;
        return false;
    }
}