/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.proxy;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.bytecode.ClassGenerator;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;

import java.lang.reflect.InvocationTargetException;

/**
 * InvokerWrapper
 */
public abstract class AbstractProxyInvoker<T> implements Invoker<T> {

    private final T proxy;

    private final Class<T> type;

    private final URL url;

    public AbstractProxyInvoker(T proxy, Class<T> type, URL url) {
        if (proxy == null) {
            throw new IllegalArgumentException("proxy == null");
        }
        if (type == null) {
            throw new IllegalArgumentException("interface == null");
        }
        if (!type.isInstance(proxy)) {
            throw new IllegalArgumentException(proxy.getClass().getName() + " not implement interface " + type);
        }
        this.proxy = proxy;
        this.type = type;
        this.url = url;
    }

    @Override
    public Class<T> getInterface() {
        return type;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        try {
            // 调用 doInvoke 执行后续的调用，并将调用结果封装到 RpcResult 中
            /**
             * doInvoke 是一个抽象方法，这个需要由具体的 Invoker 实例实现。Invoker 实例是在运行时通过 JavassistProxyFactory 创建的
             * public class JavassistProxyFactory extends AbstractProxyFactory {
             *
             *     // 省略其他方法
             *
             *     @Override
             *     public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
             *         final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
             *         // 创建匿名类对象
             *         return new AbstractProxyInvoker<T>(proxy, type, url) {
             *             @Override
             *             protected Object doInvoke(T proxy, String methodName,
             *                                       Class<?>[] parameterTypes,
             *                                       Object[] arguments) throws Throwable {
             *                 // 调用 invokeMethod 方法进行后续的调用
             *                 return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
             *             }
             *         };
             *     }
             * }
             *
             *
             * Wrapper 是一个抽象类，其中 invokeMethod 是一个抽象方法。Dubbo 会在运行时通过 Javassist
             * 框架为 Wrapper 生成实现类，并实现 invokeMethod 方法，该方法最终会根据调用信息调用具体的服务。
             * 以 DemoServiceImpl 为例，Javassist 为其生成的代理类如下。
             *
             * Wrapper0 是在运行时生成的，大家可使用 Arthas 进行反编译
             * public class Wrapper0 extends Wrapper implements ClassGenerator.DC {
             *     public static String[] pns;
             *     public static Map pts;
             *     public static String[] mns;
             *     public static String[] dmns;
             *     public static Class[] mts0;
             *
             *     // 省略其他方法
             *
             *     public Object invokeMethod(Object object, String string, Class[] arrclass, Object[] arrobject) throws InvocationTargetException {
             *         DemoService demoService;
             *         try {
             *             // 类型转换
             *             demoService = (DemoService)object;
             *         }
             *         catch (Throwable throwable) {
             *             throw new IllegalArgumentException(throwable);
             *         }
             *         try {
             *             // 根据方法名调用指定的方法
             *             if ("sayHello".equals(string) && arrclass.length == 1) {
             *                 return demoService.sayHello((String)arrobject[0]);
             *             }
             *         }
             *         catch (Throwable throwable) {
             *             throw new InvocationTargetException(throwable);
             *         }
             *         throw new NoSuchMethodException(new StringBuffer().append("Not found method \"").append(string).append("\" in class com.alibaba.dubbo.demo.DemoService.").toString());
             *     }
             * }
             */

            return new RpcResult(doInvoke(proxy, invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArguments()));
        } catch (InvocationTargetException e) {
            return new RpcResult(e.getTargetException());
        } catch (Throwable e) {
            throw new RpcException("Failed to invoke remote proxy method " + invocation.getMethodName() + " to " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    protected abstract Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable;

    @Override
    public String toString() {
        return getInterface() + " -> " + (getUrl() == null ? " " : getUrl().toString());
    }


}
