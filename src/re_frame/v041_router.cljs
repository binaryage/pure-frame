(ns re-frame.v041-router
  (:refer-clojure :exclude [flush])
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [chan put! <! timeout close! poll!]]
            [goog.async.nextTick]
            [reagent.core :as reagent]
            [re-frame.frame :as frame]
            [re-frame.logging :refer [error]]))

; implement re-frame 0.4.1 router using core.async channel

(defn make-event-chan [& args]
  (apply chan args))                                                                                                  ; TODO: set buffer size?

(defn purge-chan
  "Read all pending events from the channel and drop them on the floor."
  [event-chan]
  (go-loop []
    (if-not (nil? (poll! event-chan))
      (recur))))

; -- router loop ----------------------------------------------------------------------------------------------------
;
; In a perpetual loop, read events from "event-chan", and call the right handler.
;
; Because handlers occupy the CPU, before each event is handled, hand
; back control to the browser, via a (<! (yield 0)) call.
;
; In some cases, we need to pause for an entire animationFrame, to ensure that
; the DOM is fully flushed, before then calling a handler known to hog the CPU
; for an extended period.  In such a case, the event should be laballed with metadata
; Example usage (notice the ":flush-dom" metadata):
;   (dispatch ^:flush-dom  [:event-id other params])
;

(defn yield
  "Yields control to the browser. Faster than (timeout 0).
  See http://dev.clojure.org/jira/browse/ASYNC-137"
  []
  (let [ch (chan)]
    (goog.async.nextTick #(close! ch))
    ch))

(defn run-router-loop [event-chan db-atom frame-atom]
  (go-loop []
    (let [event (<! event-chan)                                                                                       ; wait for an event
          _ (if (:flush-dom (meta event))                                                                             ; check the event for metadata
              (do (reagent/flush) (<! (timeout 20)))                                                                  ; wait just over one annimation frame (16ms), to rensure all pending GUI work is flushed to the DOM.
              (<! (yield)))]                                                                                          ; just in case we are handling one dispatch after an other, give the browser back control to do its stuff
      (try
        (frame/process-event-on-atom! @frame-atom db-atom event)

        ; If the handler throws:
        ;   - allow the exception to bubble up because the app, in production,
        ;     may have hooked window.onerror and perform special processing.
        ;   - But an exception which bubbles up will break the enclosing go-loop.
        ;     So we'll need to start another one.
        ;   - purge any pending events, because they are probably related to the
        ;     event which just fell in a screaming heap. Not sane to handle further
        ;     events if the prior event failed.
        (catch js/Object e
          (purge-chan event-chan)                                                                                     ; get rid of any pending events
          (run-router-loop event-chan db-atom frame-atom)                                                             ; Exception throw will cause termination of go-loop. So, start another.
          (throw e))))                                                                                                ; re-throw so the rest of the app's infrastructure (window.onerror?) gets told
    (recur)))

; -- dispatch -------------------------------------------------------------------------------------------------------

(defn dispatch [event-chan frame-atom event]
  (if (nil? event)
    (error @frame-atom "re-frame: \"dispatch\" is ignoring a nil event.")                                             ; nil would close the channel
    (put! event-chan event))
  nil)                                                                                                                ; Ensure nil return. See https://github.com/Day8/re-frame/wiki/Beware-Returning-False
