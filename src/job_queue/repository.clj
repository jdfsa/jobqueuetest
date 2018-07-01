(ns job-queue.repository)

(defprotocol Repository
    (get-data [this] [this predicate])
    (get-some [this predicate])
    (set-data [this content])
    (push-data [this content])
    (remove-data [this predicate]))

(defrecord AppStore [data])
(extend-protocol Repository AppStore
    (get-data 
        ([this] @(:data this))
        ([this predicate] (filter predicate (get-data this))))
    (get-some [this predicate] (some predicate @(:data this)))
    (set-data [this content] (dosync (ref-set (:data this) content)))
    (push-data [this content] (dosync (alter (:data this) into content)))
    (remove-data [this predicate] (set-data this (into [] (remove predicate @(:data this))))))
