(ns re-frame.utils
  (:require [re-frame.logging :refer [log warn error]]))

(defn get-event-id
  [v]
  (if (vector? v)
    (first v)
    (throw (js/Error. (str "re-frame: expected a vector event, but got: " v)))))

(defn get-subscription-id
  [v]
  (if (vector? v)
    (first v)
    (throw (js/Error. (str "re-frame: expected a vector subscription, but got: " v)))))

(defn simple-inflection [base n]
  (if (= n 1) base (str base "s")))

(defn frame-summary-description [frame]
  (let [handlers-count (count (:handlers frame))
        subscriptions-count (count (:subscriptions frame))]
    (str
      handlers-count " " (simple-inflection "handler" handlers-count) ", "
      subscriptions-count " " (simple-inflection "subscription" subscriptions-count))))

(defn reset-if-changed! [db-atom new-db-state]
  (if-not (identical? @db-atom new-db-state)
    (reset! db-atom new-db-state)))

; -- composing middleware  -------------------------------------------------------------------------------------------

(defn compose-middleware
  "Given a vector of middleware, filter out any nils, and use \"comp\" to compose the elements.
  v can have nested vectors, and will be flattened before \"comp\" is applied.
  For convienience, if v is a function (assumed to be middleware already), just return it.
  Filtering out nils allows us to create Middleware conditionally like this:
     (comp-middleware [pure (when debug? debug)])  ; that 'when' might leave a nil
  "
  [frame what]
  (let [spec (if (seqable? what) (seq what) what)]
    (cond
      (fn? spec) spec                                                                                                 ; assumed to be existing middleware
      (seq? spec) (let [middlewares (remove nil? (flatten spec))]
                    (apply comp middlewares))
      :else (do
              (warn frame "re-frame: comp-middleware expects a vector, got: " what)
              nil))))