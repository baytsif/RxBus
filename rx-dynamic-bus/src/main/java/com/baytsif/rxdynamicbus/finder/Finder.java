package com.baytsif.rxdynamicbus.finder;

import com.baytsif.rxdynamicbus.entity.EventType;
import com.baytsif.rxdynamicbus.entity.SubscriberEvent;
import com.baytsif.rxdynamicbus.entity.ProducerEvent;

import java.util.Map;
import java.util.Set;

/**
 * Finds producer and subscriber methods.
 */
public interface Finder {

    Map<EventType, ProducerEvent> findAllProducers(Object listener,String tag, String suffix);

    Map<EventType, Set<SubscriberEvent>> findAllSubscribers(Object listener,String tag, String suffix);


    Finder ANNOTATED = new Finder() {
        @Override
        public Map<EventType, ProducerEvent> findAllProducers(Object listener,String tag, String suffix) {
            return AnnotatedFinder.findAllProducers(listener,tag,suffix);
        }

        @Override
        public Map<EventType, Set<SubscriberEvent>> findAllSubscribers(Object listener,String tag, String suffix) {
            return AnnotatedFinder.findAllSubscribers(listener,tag,suffix);
        }
    };
}
