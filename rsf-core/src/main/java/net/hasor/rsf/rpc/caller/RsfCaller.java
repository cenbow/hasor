/*
 * Copyright 2008-2009 the original 赵永春(zyc@hasor.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.rsf.rpc.caller;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.more.classcode.delegate.faces.MethodClassConfig;
import org.more.classcode.delegate.faces.MethodDelegate;
import org.more.future.FutureCallback;
import net.hasor.core.Provider;
import net.hasor.rsf.RsfBindInfo;
import net.hasor.rsf.RsfContext;
import net.hasor.rsf.RsfFuture;
import net.hasor.rsf.RsfResponse;
import net.hasor.rsf.RsfService;
import net.hasor.rsf.address.InterAddress;
import net.hasor.rsf.container.RsfBeanContainer;
import net.hasor.rsf.domain.RsfException;
import net.hasor.rsf.domain.RsfRuntimeUtils;
import net.hasor.rsf.domain.RsfServiceWrapper;
/**
 * 通过包装RSF请求响应，提供（同步、异步、回调、接口代理）四种远程调用方式的实现。
 * @version : 2015年12月8日
 * @author 赵永春(zyc@hasor.net)
 */
public abstract class RsfCaller extends RsfRequestManager {
    private final RsfBeanContainer container;
    public RsfCaller(RsfBeanContainer container, RsfContext rsfContext) {
        super(rsfContext);
        this.container = container;
    }
    /**
     * 根据服务注册的类型，将远程服务提供者包装成该类型表示的一个接口代理。<br>
     * 所有接口方法调用都映射为一个RPC请求响应。
     * @param target 目标RSF服务提供者地址。
     * @param serviceID 服务ID
     * @see {@link net.hasor.rsf.RsfBindInfo#getBindType()}
     */
    public Object getRemoteByID(Provider<InterAddress> target, String serviceID) throws RsfException {
        RsfBindInfo<?> bindInfo = this.getContainer().getRsfBindInfo(serviceID);
        if (bindInfo == null)
            throw new IllegalStateException("service " + serviceID + " is undefined.");
        return this.wrapper(target, bindInfo, bindInfo.getBindType());
    }
    /**
     * 根据服务注册的类型，将远程服务提供者包装成该类型表示的一个接口代理。<br>
     * 所有接口方法调用都映射为一个RPC请求响应。
     * @param target 目标RSF服务提供者地址。
     * @param group 服务分组
     * @param name 服务名
     * @param version 服务版本。
     * @see {@link net.hasor.rsf.RsfBindInfo#getBindType()}
     */
    public Object getRemote(Provider<InterAddress> target, String group, String name, String version) throws RsfException {
        RsfBindInfo<?> bindInfo = this.getContainer().getRsfBindInfo(group, name, version);
        if (bindInfo == null) {
            throw new IllegalStateException("the group=" + group + " ,name=" + name + " ,version=" + version + " is undefined.");
        }
        return this.getRemote(target, bindInfo);
    }
    /**
     * 根据服务注册的类型，将远程服务提供者包装成该类型表示的一个接口代理。<br>
     * 所有接口方法调用都映射为一个RPC请求响应。
     * @param target 目标RSF服务提供者地址。
     * @param bindInfo 服务元信息。
     * @see {@link net.hasor.rsf.RsfBindInfo#getBindType()}
     */
    public <T> T getRemote(Provider<InterAddress> target, RsfBindInfo<T> bindInfo) throws RsfException {
        if (bindInfo == null) {
            throw new NullPointerException("the bindInfo is null.");
        }
        return this.wrapper(target, bindInfo, bindInfo.getBindType());
    }
    /**
     * 忽略服务元信息上对接口类型的定义，使用指定的接口类型包装远程服务提供者。<br>
     * 请注意：当出现调用远程不存在的方法时会引发异常。虽然如此，但是该方法仍然有着自己的魅力。
     * @param target 目标RSF服务提供者地址。
     * @param serviceID 服务ID
     * @param interFace 要包装成为的那个接口。
     * @see {@link net.hasor.rsf.RsfBindInfo#getBindType()}
     */
    public <T> T wrapperByID(Provider<InterAddress> target, String serviceID, Class<T> interFace) throws RsfException {
        RsfBindInfo<?> bindInfo = this.getContainer().getRsfBindInfo(serviceID);
        if (bindInfo == null)
            throw new IllegalStateException("service " + serviceID + " is undefined.");
        return this.wrapper(target, bindInfo, interFace);
    }
    /**
     * 忽略服务元信息上对接口类型的定义，使用指定的接口类型包装远程服务提供者。<br>
     * 请注意：当出现调用远程不存在的方法时会引发异常。虽然如此，但是该方法仍然有着自己的魅力。<p>
     * @param target 目标RSF服务提供者地址。
     * @param interFace 要包装成为的那个接口，需要配合{@link RsfService @RsfService}注解一起使用。
     * @see {@link net.hasor.rsf.RsfBindInfo#getBindType()}
     */
    public <T> T wrapper(Provider<InterAddress> target, Class<T> interFace) throws RsfException {
        if (interFace == null) {
            throw new NullPointerException("the interFace is null.");
        }
        RsfBindInfo<T> bindInfo = this.getContainer().getRsfBindInfo(interFace);
        if (bindInfo == null)
            throw new IllegalStateException("service is undefined.");
        return this.wrapper(target, bindInfo, interFace);
    }
    /**
     * 忽略服务元信息上对接口类型的定义，使用指定的接口类型包装远程服务提供者。<br>
     * 请注意：当出现调用远程不存在的方法时会引发异常。虽然如此，但是该方法仍然有着自己的魅力。
     * @param target 目标RSF服务提供者地址。
     * @param group 服务分组
     * @param name 服务名称
     * @param version 服务版本
     * @param interFace 要包装成为的那个接口。
     * @see {@link net.hasor.rsf.RsfBindInfo#getBindType()}
     */
    public <T> T wrapper(Provider<InterAddress> target, String group, String name, String version, Class<T> interFace) throws RsfException {
        RsfBindInfo<?> bindInfo = this.getContainer().getRsfBindInfo(group, name, version);
        if (bindInfo == null)
            return null;
        return this.wrapper(target, bindInfo, interFace);
    }
    private final Object                                          LOCK_OBJECT = new Object();
    private final ConcurrentMap<String, Class<RsfServiceWrapper>> wrapperMap  = new ConcurrentHashMap<String, Class<RsfServiceWrapper>>();
    public <T> T wrapper(Provider<InterAddress> target, RsfBindInfo<?> bindInfo, Class<T> interFace) throws RsfException {
        if (bindInfo == null)
            throw new NullPointerException();
        if (interFace.isInterface() == false)
            throw new UnsupportedOperationException("interFace parameter must be an interFace.");
        //
        String bindID = bindInfo.getBindID();
        Class<RsfServiceWrapper> wrapperClass = this.wrapperMap.get(bindID);
        if (wrapperClass == null) {
            synchronized (LOCK_OBJECT) {
                wrapperClass = this.wrapperMap.get(bindID);
                if (wrapperClass == null) {
                    try {
                        MethodClassConfig classConfig = new MethodClassConfig(RsfServiceWrapper.class);
                        classConfig.addDelegate(interFace, new ServiceMethodDelegate(bindInfo));
                        wrapperClass = (Class<RsfServiceWrapper>) classConfig.toClass();
                        this.wrapperMap.put(bindID, wrapperClass);
                    } catch (Throwable e) {
                        throw new RsfException(e.getMessage(), e);
                    }
                }
            }
        }
        //
        try {
            RsfServiceWrapper wrapper = (RsfServiceWrapper) wrapperClass.newInstance();
            wrapper.setTarget(target);
            return (T) wrapper;
        } catch (Throwable e) {
            throw new RsfException(e.getMessage(), e);
        }
    }
    private class ServiceMethodDelegate implements MethodDelegate {
        private RsfBindInfo<?> bindInfo;
        public ServiceMethodDelegate(RsfBindInfo<?> bindInfo) {
            this.bindInfo = bindInfo;
        }
        public Object invoke(Method callMethod, Object target, Object[] params) throws Throwable {
            RsfServiceWrapper wrapper = (RsfServiceWrapper) target;
            Provider<InterAddress> targetAddress = wrapper.getTarget();
            return syncInvoke(targetAddress, this.bindInfo, callMethod.getName(), callMethod.getParameterTypes(), params);
        }
    }
    /**
     * 同步方式调用远程服务。
     * @param target 目标RSF服务提供者地址。
     * @param bindInfo 服务元信息。
     * @param methodName 远程服务方法名
     * @param parameterTypes 远程方法参数列表。
     * @param parameterObjects 参数值
     */
    public Object syncInvoke(Provider<InterAddress> target, RsfBindInfo<?> bindInfo, String methodName, Class<?>[] parameterTypes, Object[] parameterObjects) throws Throwable {
        //1.准备Request
        int timeout = validateTimeout(bindInfo.getClientTimeout());
        Method targetMethod = RsfRuntimeUtils.getServiceMethod(bindInfo.getBindType(), methodName, parameterTypes);
        RsfRequestFormLocal request = new RsfRequestFormLocal(target, bindInfo, targetMethod, parameterObjects, this);
        //2.发起Request
        RsfFuture rsfFuture = doSendRequest(request, null);
        //3.返回数据
        RsfResponse response = rsfFuture.get(timeout, TimeUnit.MILLISECONDS);
        return response.getData();
    }
    /**
     * 异步方式调用远程服务。
     * @param target 目标RSF服务提供者地址。
     * @param bindInfo 服务元信息。
     * @param methodName 远程服务方法名
     * @param parameterTypes 远程方法参数列表。
     * @param parameterObjects 参数值
     */
    public RsfFuture asyncInvoke(Provider<InterAddress> target, RsfBindInfo<?> bindInfo, String methodName, Class<?>[] parameterTypes, Object[] parameterObjects) {
        //1.准备Request
        Method targetMethod = RsfRuntimeUtils.getServiceMethod(bindInfo.getBindType(), methodName, parameterTypes);
        RsfRequestFormLocal request = new RsfRequestFormLocal(target, bindInfo, targetMethod, parameterObjects, this);
        //2.发起Request
        return doSendRequest(request, null);
    }
    /**
     * 回调方式调用远程服务，回调中返回的是结果。
     * @param target 目标RSF服务提供者地址。
     * @param bindInfo 服务元信息。
     * @param methodName 远程服务方法名
     * @param parameterTypes 远程方法参数列表。
     * @param parameterObjects 参数值
     * @param listener 回调接口。
     */
    public void callBackInvoke(Provider<InterAddress> target, RsfBindInfo<?> bindInfo, String methodName, Class<?>[] parameterTypes, Object[] parameterObjects, final FutureCallback<Object> listener) {
        this.callBackRequest(target, bindInfo, methodName, parameterTypes, parameterObjects, new FutureCallback<RsfResponse>() {
            public void completed(RsfResponse result) {
                listener.completed(result.getData());
            }
            public void failed(Throwable ex) {
                listener.failed(ex);
            }
            public void cancelled() {
                listener.cancelled();
            }
        });
    }
    /**
     * 回调方式调用远程服务，回调中返回的是{@link RsfResponse}。
     * @param target 目标RSF服务提供者地址。
     * @param bindInfo 服务元信息。
     * @param methodName 远程服务方法名
     * @param parameterTypes 远程方法参数列表。
     * @param parameterObjects 参数值
     * @param listener 回调接口。
     */
    public void callBackRequest(Provider<InterAddress> target, RsfBindInfo<?> bindInfo, String methodName, Class<?>[] parameterTypes, Object[] parameterObjects, FutureCallback<RsfResponse> listener) {
        //1.准备Request
        Method targetMethod = RsfRuntimeUtils.getServiceMethod(bindInfo.getBindType(), methodName, parameterTypes);
        RsfRequestFormLocal request = new RsfRequestFormLocal(target, bindInfo, targetMethod, parameterObjects, this);
        //2.发起Request
        doSendRequest(request, listener);
    }
    private int validateTimeout(int timeout) {
        if (timeout <= 0)
            timeout = this.getContext().getSettings().getDefaultTimeout();
        return timeout;
    }
    //
    @Override
    protected RsfBeanContainer getContainer() {
        return this.container;
    }
}