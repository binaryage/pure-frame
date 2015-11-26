(ns re-frame.core
  (:require [re-frame.v041-api :as v041-api]
            [re-frame.v041-router :as v041-router]
            [re-frame.router :as router]
            [re-frame.config :as config]
            [re-frame.middleware :as middleware]))

; This file provides public API to re-frame compatible setup. It is inteded to help you switch to pure-frame or
; as an example how to use low-level frame library.
;
; Normally you are expected not to include this namespace and use re-frame.frame namespace directly.
;
; This namespace provides backward compatibility with re-frame 0.5.0 (or 0.4.1).
; By including this namespace, you will create global app-db, app-frame and event queue and use them by default.
;
; * app-db is backed by reagent/atom
; * app-frame has default loggers
; * event queue is
;   * mini finite-state-machine as implemented by re-frame 0.5.0
;   * or core.async channel as implementeted by re-frame 0.4.1

; the default instance of app-db
(def app-db (v041-api/make-app-db-atom))

; the default instance of re-frame
(def app-frame (v041-api/make-frame-atom))

; --  API  ------------------------------------------------------------------------------------------------------------------

(def set-loggers! (partial v041-api/set-loggers! app-frame))
(def register-sub (partial v041-api/register-sub app-frame))
(def unregister-sub (partial v041-api/unregister-sub app-frame))
(def clear-sub-handlers! (partial v041-api/clear-sub-handlers! app-frame))
(def subscribe (partial v041-api/subscribe app-frame app-db))
(def clear-event-handlers! (partial v041-api/clear-event-handlers! app-frame))
(def register-handler
  "register a handler for an event.
  This is low level and it is expected that \"re-frame.core/register-handler\" would
  generally be used."
  (partial v041-api/register-handler app-frame))
(def unregister-handler (partial v041-api/unregister-handler app-frame))

(def dispatch-sync
  "Send an event to be processed by the registered handler, but avoid the async-inducing
use of core.async/chan.

Usage example:
   (dispatch-sync [:delete-item 42])"
  (partial v041-api/dispatch-sync app-db app-frame))

; --  router  ---------------------------------------------------------------------------------------------------------------

(when (= config/core-implements "v041")
  ; the default event queue
  (def event-queue (v041-router/make-event-chan))
  (def dispatch (partial v041-router/dispatch event-queue app-frame))
  (def run-router-loop (partial v041-router/run-router-loop event-queue app-db app-frame)))

(when (= config/core-implements "v050")
  ; the default event queue
  (def event-queue (router/make-event-queue app-frame app-db))
  (def dispatch (partial router/dispatch event-queue app-frame)))

; --  middleware  -----------------------------------------------------------------------------------------------------------

(def debug (middleware/debug app-frame))
(def path (middleware/path app-frame))
(def enrich (middleware/enrich app-frame))
(def trim-v (middleware/trim-v app-frame))
(def after (middleware/after app-frame))
(def log-ex (middleware/log-ex app-frame))
(def on-changes (middleware/on-changes app-frame))

; --  event processing  -----------------------------------------------------------------------------------------------------

(when (= config/core-implements "v041")
  (if config/run-loop-automatically
    (run-router-loop)))
