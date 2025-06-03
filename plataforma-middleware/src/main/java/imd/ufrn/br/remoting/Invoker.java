package imd.ufrn.br.remoting;

import imd.ufrn.br.identification.LookupService;
import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.exceptions.InvocationException;
import imd.ufrn.br.exceptions.ObjectNotFoundException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * The Invoker is responsible for executing a method on a target remote object.
 * It uses a {@link LookupService} to find the object instance based on its
 * {@link ObjectId} and then uses Java Reflection to invoke the specified method.
 */
public class Invoker {

    private final LookupService lookupService;

    /**
     * Constructs a new Invoker with the specified LookupService.
     *
     * @param lookupService The {@link LookupService} to be used for finding remote objects.
     *                      Cannot be null.
     */
    public Invoker(LookupService lookupService) {
        if (lookupService == null) {
            throw new IllegalArgumentException("LookupService cannot be null.");
        }
        this.lookupService = lookupService;
    }

    /**
     * Invokes a method on a remote object.
     *
     * @param objectId The {@link ObjectId} of the target remote object.
     * @param methodName The name of the method to invoke.
     * @param args An array of arguments for the method. The types of these arguments
     *             must match the parameter types of the target method.
     * @return The result of the method invocation.
     * @throws ObjectNotFoundException if the object with the given ObjectId cannot be found.
     * @throws NoSuchMethodException if a method with the specified name and parameter types
     *                               cannot be found on the target object.
     * @throws InvocationException if the invoked method throws an exception, or if there's
     *                             an issue with accessing the method.
     * @throws Throwable for other reflection-related errors or unexpected issues.
     */
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

    /**
     * Finds a compatible method in the given class.
     * This is more flexible than Class.getMethod() as it considers assignability
     * and handles primitive/wrapper type conversions for parameters.
     */
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

    /**
     * Checks if a wrapper class corresponds to a given primitive type.
     */
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