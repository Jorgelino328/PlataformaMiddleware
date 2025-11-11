package imd.ufrn.br.registry;

import imd.ufrn.br.annotations.HttpVerb;
import java.lang.reflect.Method;

public record RouteInfo(
    String path,
    HttpVerb verb,
    Object instance,
    Method method,
    Class<?>[] parameterTypes
) {}
