package imd.ufrn.br.registry;

import imd.ufrn.br.annotations.HttpVerb;
import imd.ufrn.br.annotations.MethodMapping;
import imd.ufrn.br.annotations.RequestMapping;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RouteRegistry {
    private static RouteRegistry instance;
    private final Map<String, RouteInfo> routes = new ConcurrentHashMap<>();

    private RouteRegistry() {
    }

    public static synchronized RouteRegistry getInstance() {
        if (instance == null) {
            instance = new RouteRegistry();
        }
        return instance;
    }

    public void register(Object serviceInstance) {
        Class<?> serviceClass = serviceInstance.getClass();
        String basePath = "";

        if (serviceClass.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping requestMapping = serviceClass.getAnnotation(RequestMapping.class);
            basePath = requestMapping.path();
        }

        for (Method method : serviceClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(MethodMapping.class)) {
                MethodMapping methodMapping = method.getAnnotation(MethodMapping.class);
                String finalPath = basePath + methodMapping.path();
                HttpVerb verb = methodMapping.verb();
                String routeKey = verb.name() + " " + finalPath;

                RouteInfo routeInfo = new RouteInfo(
                        finalPath,
                        verb,
                        serviceInstance,
                        method,
                        method.getParameterTypes()
                );

                if (routes.containsKey(routeKey)) {
                    System.err.println("Warning: Duplicate route detected! " + routeKey + ". Overwriting.");
                }
                routes.put(routeKey, routeInfo);
                System.out.println("Route registered: " + routeKey + " -> " + serviceClass.getName() + "." + method.getName());
            }
        }
    }

    public RouteInfo findRoute(HttpVerb verb, String path) {
        String routeKey = verb.name() + " " + path;
        return routes.get(routeKey);
    }

    public Map<String, RouteInfo> getRoutes() {
        return routes;
    }
    
    public Set<String> getAllServiceNames() {
        Set<String> serviceNames = new HashSet<>();
        for (RouteInfo routeInfo : routes.values()) {
            Object serviceInstance = routeInfo.instance();
            if (serviceInstance != null) {
                Class<?> serviceClass = serviceInstance.getClass();
                String serviceName = serviceClass.getSimpleName();
                
                if (serviceClass.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping mapping = serviceClass.getAnnotation(RequestMapping.class);
                    String path = mapping.path();
                    if (path != null && !path.isEmpty()) {
                        serviceName = path.startsWith("/") ? path.substring(1) : path;
                    }
                }
                
                serviceNames.add(serviceName);
            }
        }
        return serviceNames;
    }
}
