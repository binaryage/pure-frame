(ns re-frame.test.core
  (:require-macros [cemerick.cljs.test :refer (is deftest testing done)]
                   [reagent.ratom :refer [reaction run!]])
  (:require [cemerick.cljs.test]
            [re-frame.core :as core]))

(defn reinitialize! []
  ; TODO: figure out, how to force channel flush
  (reset! core/app-db nil)
  (core/clear-sub-handlers! core/app-frame)
  (core/clear-event-handlers! core/app-frame))

(deftest modify-app-db-sync
  (testing "modify app-db via handler (sync)"
    (reinitialize!)
    (is (= @core/app-db nil))
    (core/register-handler :modify-app (fn [db [_ data]]
                                         (assoc db :modify-app-handler-was-here data)))
    (core/dispatch-sync [:modify-app "something"])
    (is (= @core/app-db {:modify-app-handler-was-here "something"}))))

(deftest ^:async modify-app-db-async
  (testing "modify app-db via handler (async)"
    (reinitialize!)
    (is (= @core/app-db nil))
    (core/register-handler :modify-app (fn [db [_ data]]
                                         (assoc db :modify-app-handler-was-here data)))
    (core/register-handler :check (fn [db]
                                    (is (= db @core/app-db))
                                    (is (= db {:modify-app-handler-was-here "something"}))
                                    (done)
                                    db))
    (core/dispatch [:modify-app "something"])
    (core/dispatch [:check])))

(deftest subscribing
  (testing "register subscription handler and trigger it"
    (reinitialize!)
    (reset! core/app-db 0)
    (let [target (atom nil)
          db-adder (fn [db [sub-id num]]
                     (is (= sub-id :db-adder))
                     (reaction (+ @db num)))
          _ (core/register-sub :db-adder db-adder)
          subscription (core/subscribe [:db-adder 10])]
      (is (= @target nil))
      (run! (reset! target @subscription))
      (is (= @target 10))
      (swap! core/app-db inc)
      (is (= @target 11)))))

(deftest ^:async check-expected-order
  (testing "dispatches should be executed in original order (async)"
    (reinitialize!)
    (reset! core/app-db [])
    (core/register-handler :conj (fn [db [_ val]] (conj db val)))
    (core/register-handler :check (fn [db [_ expected]]
                                    (is (= db expected))
                                    (done)
                                    db))
    (core/dispatch [:conj :first])
    (core/dispatch [:conj :second])
    (core/dispatch [:conj :third])
    (core/dispatch [:check [:first :second :third]])))

(deftest ^:async check-side-effecting-dispatches-order
  (testing "dispatches causing other dispatches should queue them as FIFO (async)"
    (reinitialize!)
    (reset! core/app-db [])
    (core/register-handler :conj (fn [db [_ val & events]]
                                   (doseq [event events]
                                     (core/dispatch event))
                                   (conj db val)))
    (core/register-handler :check (fn [db [_ expected]]
                                    (is (= db expected))
                                    (done)
                                    db))
    (core/dispatch [:conj :first-0
                    [:conj :first-1
                     [:conj :first-2]]])
    (core/dispatch [:conj :second-0
                    [:conj :second-1
                     [:conj :second-2]]])
    (core/dispatch [:conj :third-0
                    [:conj :third-1
                     [:conj :third-2]
                     [:check [:first-0 :second-0 :third-0
                              :first-1 :second-1 :third-1
                              :first-2 :second-2 :third-2]]]])))
