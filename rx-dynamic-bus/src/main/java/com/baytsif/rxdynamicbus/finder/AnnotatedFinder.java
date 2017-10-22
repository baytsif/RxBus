package com.baytsif.rxdynamicbus.finder;

import com.baytsif.rxdynamicbus.annotation.Produce;
import com.baytsif.rxdynamicbus.annotation.Subscribe;
import com.baytsif.rxdynamicbus.annotation.Tag;
import com.baytsif.rxdynamicbus.annotation.TagDynamic;
import com.baytsif.rxdynamicbus.entity.EventType;
import com.baytsif.rxdynamicbus.entity.ProducerEvent;
import com.baytsif.rxdynamicbus.entity.SubscriberEvent;
import com.baytsif.rxdynamicbus.thread.EventThread;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static android.R.attr.tag;
import static android.R.attr.type;

/**
 * Helper methods for finding methods annotated with {@link Produce} and {@link Subscribe}.
 */
public final class AnnotatedFinder {

    /**
     * Cache event bus producer methods for each class.
     */
    private static final ConcurrentMap<Class<?>, Map<EventType, SourceMethod>> PRODUCERS_CACHE =
            new ConcurrentHashMap<>();

    /**
     * Cache event bus subscriber methods for each class.
     */
    private static final ConcurrentMap<Class<?>, Map<EventType, Set<SourceMethod>>> SUBSCRIBERS_CACHE =
            new ConcurrentHashMap<>();

    private static void loadAnnotatedProducerMethods(Class<?> listenerClass,
                                                     Map<EventType, SourceMethod> producerMethods,String tag, String suffix) {
        Map<EventType, Set<SourceMethod>> subscriberMethods = new HashMap<>();
        loadAnnotatedMethods(listenerClass, producerMethods, subscriberMethods,tag,suffix);
    }

    private static void loadAnnotatedSubscriberMethods(Class<?> listenerClass,
                                                       Map<EventType, Set<SourceMethod>> subscriberMethods,String tag, String suffix) {
        Map<EventType, SourceMethod> producerMethods = new HashMap<>();
        loadAnnotatedMethods(listenerClass, producerMethods, subscriberMethods,tag,suffix);
    }

    /**
     * Load all methods annotated with {@link Produce} or {@link Subscribe} into their respective caches for the
     * specified class.
     */
    private static void loadAnnotatedMethods(Class<?> listenerClass,
                                             Map<EventType, SourceMethod> producerMethods, Map<EventType, Set<SourceMethod>> subscriberMethods,String dynamicTag, String suffix) {
        for (Method method : listenerClass.getDeclaredMethods()) {
            // The compiler sometimes creates synthetic bridge methods as part of the
            // type erasure process. As of JDK8 these methods now include the same
            // annotations as the original declarations. They should be ignored for
            // subscribe/produce.
            if (method.isBridge()) {
                continue;
            }
            if (method.isAnnotationPresent(Subscribe.class)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw new IllegalArgumentException("Method " + method + " has @Subscribe annotation but requires "
                            + parameterTypes.length + " arguments.  Methods must require a single argument.");
                }

                Class<?> parameterClazz = parameterTypes[0];
                if (parameterClazz.isInterface()) {
                    throw new IllegalArgumentException("Method " + method + " has @Subscribe annotation on " + parameterClazz
                            + " which is an interface.  Subscription must be on a concrete class type.");
                }

                if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
                    throw new IllegalArgumentException("Method " + method + " has @Subscribe annotation on " + parameterClazz
                            + " but is not 'public'.");
                }

                Subscribe annotation = method.getAnnotation(Subscribe.class);
                EventThread thread = annotation.thread();
                Tag[] tags = annotation.tags();
                int tagLength = (tags == null ? 0 : tags.length);
                if (annotation.dynamicTags() == null || annotation.dynamicTags().length == 0) {
                    do {
                        String tag = Tag.DEFAULT;
                        if (tagLength > 0) {
                            tag = tags[tagLength - 1].value();
                        }

                        EventType type = new EventType(tag, parameterClazz);
                        Set<SourceMethod> methods = subscriberMethods.get(type);
                        if (methods == null) {
                            methods = new HashSet<>();
                            subscriberMethods.put(type, methods);
                        }
                        methods.add(new SourceMethod(thread, method));
                        tagLength--;
                    } while (tagLength > 0);
                }


                if (annotation.tags() == null || annotation.tags().length == 0) {

                    TagDynamic[] tagsDynamic = annotation.dynamicTags();
                    int tagDynamicLength = (tagsDynamic == null ? 0 : tagsDynamic.length);
                    do {
                        String tagDynamic = TagDynamic.DEFAULT;
                        if (tagDynamicLength > 0) {
                            tagDynamic = tagsDynamic[tagDynamicLength - 1].value();
                        }
//
                        if (!tagDynamic.equals(dynamicTag)) {
                            continue;
                        }
                        EventType type = new EventType(tagDynamic + suffix, parameterClazz);
                        Set<SourceMethod> methods = subscriberMethods.get(type);
                        if (methods == null) {
                            methods = new HashSet<>();
                            subscriberMethods.put(type, methods);
                        }
                        if (!isContained(methods,new SourceMethod(thread, method))) {
                            methods.add(new SourceMethod(thread, method));
                        }
                        tagDynamicLength--;
                    } while (tagDynamicLength > 0);
                }





            } else if (method.isAnnotationPresent(Produce.class)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 0) {
                    throw new IllegalArgumentException("Method " + method + "has @Produce annotation but requires "
                            + parameterTypes.length + " arguments.  Methods must require zero arguments.");
                }
                if (method.getReturnType() == Void.class) {
                    throw new IllegalArgumentException("Method " + method
                            + " has a return type of void.  Must declare a non-void type.");
                }

                Class<?> parameterClazz = method.getReturnType();
                if (parameterClazz.isInterface()) {
                    throw new IllegalArgumentException("Method " + method + " has @Produce annotation on " + parameterClazz
                            + " which is an interface.  Producers must return a concrete class type.");
                }
                if (parameterClazz.equals(Void.TYPE)) {
                    throw new IllegalArgumentException("Method " + method + " has @Produce annotation but has no return type.");
                }

                if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
                    throw new IllegalArgumentException("Method " + method + " has @Produce annotation on " + parameterClazz
                            + " but is not 'public'.");
                }

                Produce annotation = method.getAnnotation(Produce.class);
                EventThread thread = annotation.thread();
                Tag[] tags = annotation.tags();
                int tagLength = (tags == null ? 0 : tags.length);
                do {
                    String tag = Tag.DEFAULT;
                    if (tagLength > 0) {
                        tag = tags[tagLength - 1].value();
                    }
                    EventType type = new EventType(tag, parameterClazz);
                    if (producerMethods.containsKey(type)) {
                        throw new IllegalArgumentException("Producer for type " + type + " has already been registered.");
                    }
                    producerMethods.put(type, new SourceMethod(thread, method));
                    tagLength--;
                } while (tagLength > 0);
            }
        }

        PRODUCERS_CACHE.put(listenerClass, producerMethods);
        SUBSCRIBERS_CACHE.put(listenerClass, subscriberMethods);
    }

    /**
     * This implementation finds all methods marked with a {@link Produce} annotation.
     */
    static Map<EventType, ProducerEvent> findAllProducers(Object listener,String dynamicTag, String suffix) {
        final Class<?> listenerClass = listener.getClass();
        Map<EventType, ProducerEvent> producersInMethod = new HashMap<>();

        Map<EventType, SourceMethod> methods = PRODUCERS_CACHE.get(listenerClass);
        if (null == methods) {
            methods = new HashMap<>();
            loadAnnotatedProducerMethods(listenerClass, methods,dynamicTag,suffix);
        }
        if (!methods.isEmpty()) {
            for (Map.Entry<EventType, SourceMethod> e : methods.entrySet()) {
                ProducerEvent producer = new ProducerEvent(listener, e.getValue().method, e.getValue().thread);
                producersInMethod.put(e.getKey(), producer);
            }
        }

        return producersInMethod;
    }

    /**
     * This implementation finds all methods marked with a {@link Subscribe} annotation.
     */
    static Map<EventType, Set<SubscriberEvent>> findAllSubscribers(Object listener,String tag, String suffix) {
        Class<?> listenerClass = listener.getClass();
        Map<EventType, Set<SubscriberEvent>> subscribersInMethod = new HashMap<>();

        Map<EventType, Set<SourceMethod>> methods = SUBSCRIBERS_CACHE.get(listenerClass);
        if (null == methods) {
            methods = new HashMap<>();
        }
        loadAnnotatedSubscriberMethods(listenerClass, methods,tag,suffix);
        if (!methods.isEmpty()) {
            for (Map.Entry<EventType, Set<SourceMethod>> e : methods.entrySet()) {
                Set<SubscriberEvent> subscribers = new HashSet<>();
                for (SourceMethod m : e.getValue()) {
//                    Subscribe annotation = m.method.getAnnotation(Subscribe.class);
//                    TagDynamic[] tagDynamics = annotation.dynamicTags();
//                    if (tagDynamics.length > 0)
                    if (!isSubscriberContained(subscribers,new SubscriberEvent(listener, m.method, m.thread))) {
                        subscribers.add(new SubscriberEvent(listener, m.method, m.thread));
                    }
                }
                subscribersInMethod.put(e.getKey(), subscribers);
            }
        }

        return subscribersInMethod;
    }

    private AnnotatedFinder() {
        // No instances.
    }

    private static class SourceMethod {
        private EventThread thread;
        private Method method;

        private SourceMethod(EventThread thread, Method method) {
            this.thread = thread;
            this.method = method;
        }
    }

    private static boolean isContained(Set<SourceMethod> methods,SourceMethod sourceMethod) {
        boolean ans = false;
        Iterator<SourceMethod> iterator = methods.iterator();
        while (iterator.hasNext()) {
            SourceMethod next = iterator.next();
            if (next.method.getName().equals(sourceMethod.method.getName()) &&
                    next.method.getDeclaringClass().equals(sourceMethod.method.getDeclaringClass()) &&
                    next.thread.getDeclaringClass().equals(sourceMethod.thread.getDeclaringClass())) {
                ans = true;
                break;
            }
        }
        return ans;
    }

    private static boolean isSubscriberContained(Set<SubscriberEvent> subscribers,SubscriberEvent subscriberEvent) {
        boolean ans = false;
        Iterator<SubscriberEvent> iterator = subscribers.iterator();
        while (iterator.hasNext()) {
            SubscriberEvent next = iterator.next();
            if (next.equals(subscriberEvent)) {
                ans = true;
                break;
            }
        }
        return ans;
    }
}
