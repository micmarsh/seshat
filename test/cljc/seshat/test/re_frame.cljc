(ns seshat.test.re-frame
  "Utilities for easy testing of re-frame events. Most logic stolen directly from
   https://github.com/Day8/re-frame-test/blob/d3ecda2b18d7a0a62203c8716bebfeecc05014c9/src/day8/re_frame/test.cljc"
  (:require [re-frame.core :as rf]
            [re-frame.interop :as rf-int]
            [re-frame.router :as rf-router]))


(def my-queue (atom rf-int/empty-queue))

(def ^:dynamic *handling* false)

(defn- dequeue!
  "Dequeue an item from a persistent queue which is stored as the value in
  queue-atom. Returns the item, and updates the atom with the new queue
  value. If the queue is empty, does not alter it and returns nil."
  [queue-atom]
  (let [queue @queue-atom]
    (when (seq queue)
      (if (compare-and-set! queue-atom queue (pop queue))
        (peek queue)
        (recur queue-atom)))))

(defn new-dispatch
  "Dispatch synchronously using our own queue"
  [argv]
  (swap! my-queue conj argv)
  (when-not *handling*
    (binding [*handling* true]
      (loop []
        (when-let [queue-head (dequeue! my-queue)]
          (rf/dispatch-sync queue-head)
          (recur))))))

(defmacro with-sync-dispatches [& body]
  `(with-redefs [rf/dispatch new-dispatch
                 rf-router/dispatch new-dispatch]
     ~@body))

(defn sync-dispatch-fixture
  [test]
  (with-sync-dispatches (test)))
