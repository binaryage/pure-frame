(ns re-frame.v041-api
  (:require [reagent.core :as reagent]
            [re-frame.frame :as frame]
            [re-frame.logging :refer [error]]
            [re-frame.utils :as utils]))

; implement re-frame 0.4.1 functionality on top reusable re-frame parts

(defn make-app-db-atom [& args]
  (apply reagent/atom args))

(defn make-frame-atom [& args]
  (atom (apply frame/make-frame args)))

; -- re-frame 0.4.1 interface  --------------------------------------------------------------------------------------

(defn set-loggers! [frame-atom new-loggers]
  (swap! frame-atom #(frame/set-loggers % new-loggers)))

(defn register-sub [frame-atom subscription-id handler-fn]
  (swap! frame-atom #(frame/register-subscription-handler % subscription-id handler-fn)))

(defn unregister-sub [frame-atom subscription-id]
  (swap! frame-atom #(frame/unregister-subscription-handler % subscription-id)))

(defn clear-sub-handlers! [frame-atom]
  (swap! frame-atom #(frame/clear-subscription-handlers %)))

(defn legacy-subscribe [frame-atom db-atom subscription-spec]
  (let [subscription-id (utils/get-subscription-id subscription-spec)
        handler-fn (get-in @frame-atom [:subscriptions subscription-id])]
    (if (nil? handler-fn)
      (error @frame-atom
        "re-frame: no subscription handler registered for: \"" subscription-id "\".  Returning a nil subscription.")
      (handler-fn db-atom subscription-spec))))

(def subscribe legacy-subscribe)

(defn clear-event-handlers! [frame-atom]
  (swap! frame-atom #(frame/clear-event-handlers %)))

(defn register-handler
  ([frame-atom event-id handler-fn]
   (swap! frame-atom #(frame/register-event-handler % event-id handler-fn)))
  ([frame-atom event-id middleware handler-fn]
   (if-let [mid-ware (utils/compose-middleware @frame-atom middleware)]                                               ; compose the middleware
     (register-handler frame-atom event-id (mid-ware handler-fn)))))                                                  ; wrap the handler in the middleware

(defn unregister-handler [frame-atom event-id]
  (swap! frame-atom #(frame/unregister-event-handler % event-id)))

(defn dispatch-sync [db-atom frame-atom event]
  (frame/process-event-on-atom! @frame-atom db-atom event)
  nil)                                                                                                                ; Ensure nil return. See https://github.com/Day8/re-frame/wiki/Beware-Returning-False