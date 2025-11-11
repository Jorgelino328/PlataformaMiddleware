package imd.ufrn.br.remoting;

import java.lang.reflect.Method;

public record Request(
    Object instance,
    Method method,
    Object[] params
) {}
