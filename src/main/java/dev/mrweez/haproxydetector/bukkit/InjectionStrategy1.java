package dev.mrweez.haproxydetector.bukkit;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.reflect.FuzzyReflection;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import dev.mrweez.haproxydetector.HAProxyDetectorHandler;
import dev.mrweez.haproxydetector.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

public class InjectionStrategy1 implements InjectionStrategy {
    private final Logger logger;

    private Field injectorFactoryField;
    private Object injector;
    private Object oldFactory;
    private Class<?> injectionFactoryClass;

    public InjectionStrategy1(Logger logger) {this.logger = logger;}

    @Override
    public void inject() throws ReflectiveOperationException {
        try {
            this.uninject();
        } catch (Throwable ignored) {
        }

        // ProtocolLib 5.x moved InjectionFactory to .channel subpackage
        try {
            injectionFactoryClass = Class.forName("com.comphenix.protocol.injector.netty.channel.InjectionFactory");
        } catch (ClassNotFoundException e) {
            injectionFactoryClass = Class.forName("com.comphenix.protocol.injector.netty.InjectionFactory");
        }

        Class<?> protocolInjectorClass;
        try {
            protocolInjectorClass = Class.forName("com.comphenix.protocol.injector.netty.ProtocolInjector");
        } catch (ClassNotFoundException e) {
            // In some versions it might be different, but typically it's this.
            // If it's missing, we are likely on a version that should use Strategy 2.
            throw e;
        }

        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        Field injectorField = FuzzyReflection.fromObject(pm, true)
                .getFieldByType("nettyInjector", protocolInjectorClass);
        injectorField.setAccessible(true);
        injector = injectorField.get(pm);

        injectorFactoryField = FuzzyReflection.fromObject(injector, true)
                .getFieldByType("factory", injectionFactoryClass);
        injectorFactoryField.setAccessible(true);

        oldFactory = injectorFactoryField.get(injector);

        Object newFactory = Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{injectionFactoryClass},
                (proxy, method, args) -> {
                    if ("fromChannel".equals(method.getName())) {
                        Channel channel = (Channel) args[0];
                        ChannelPipeline pipeline = channel.pipeline();
                        if (channel.isOpen() && pipeline.get("haproxy-detector") == null) {
                            ChannelHandler networkManager = BukkitMain.getNetworkManager(pipeline);
                            injectDetector(pipeline, networkManager);
                        }
                    }
                    return method.invoke(oldFactory, args);
                });

        injectorFactoryField.set(injector, newFactory);
    }

    private void injectDetector(ChannelPipeline pipeline, ChannelHandler networkManager) {
        synchronized (networkManager) {
            HAProxyDetectorHandler detectorHandler = new HAProxyDetectorHandler(BukkitMain.logger,
                    new HAProxyMessageHandler(networkManager));
            try {
                pipeline.addAfter("timeout", "haproxy-detector", detectorHandler);
            } catch (NoSuchElementException e) {
                pipeline.addFirst("haproxy-detector", detectorHandler);
            }
        }
    }

    @Override
    public void uninject() throws ReflectiveOperationException {
        if (injectorFactoryField != null && injector != null && oldFactory != null) {
            injectorFactoryField.set(injector, oldFactory);
            oldFactory = null;
        }
    }
}
