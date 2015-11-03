(ns re-frame.middleware
  (:require [clojure.data :as data]
            [re-frame.logging :refer [log warn error group group-end]]))


; See docs in the Wiki: https://github.com/Day8/re-frame/wiki

(defn log-ex
  "Middleware which catches and prints any handler-generated exceptions to console.
  Handlers are called from within a core.async go-loop, and core.async produces
  a special kind of hell when in comes to stacktraces. By the time an exception
  has passed through a go-loop its stack is mangled beyond repair and you'll
  have no idea where the exception was thrown.
  So this middleware catches and prints to stacktrace before the core.async sausage
  machine has done its work.
  "
  [frame-atom]
  (fn [handler]
    (fn log-ex-handler
      [db v]
      (warn @frame-atom
        (str
          "re-frame: use of \"log-ex\" is deprecated. You don't need it any more IF YOU ARE USING CHROME 44."
          " Chrome now seems to now produce good stack traces."))
      (try
        (handler db v)
        (catch :default e                                                                                             ; ooops, handler threw
          (do
            (.error js/console (.-stack e))
            (throw e)))))))


(defn debug
  "Middleware which logs debug information to js/console for each event.
  Includes a clojure.data/diff of the db, before vs after, showing the changes
  caused by the event."
  [frame-atom]
  (fn [handler]
    (fn debug-handler [db v]
      (let [frame @frame-atom]
        (log frame "-- New Event ----------------------------------------------------")
        (group frame "re-frame event: " v)
        (let [new-db (handler db v)
              diff (data/diff db new-db)]
          (log frame "only before: " (first diff))
          (log frame "only after : " (second diff))
          (group-end frame)
          new-db)))))



(defn trim-v
  "Middleware which removes the first element of v, allowing you to write
  more aesthetically pleasing handlers. No leading underscore on the event-v!
  Your handlers will look like this:
      (defn my-handler
        [db [x y z]]    ; <-- instead of [_ x y z]
        ....)
  "
  [_frame-atom]
  (fn [handler]
    (fn trim-v-handler
      [db v]
      (handler db (vec (rest v))))))


; -- Middleware Factories -------------------------------------------------------------------------------------------

(defn path
  "A middleware factory which supplies a sub-tree of `db` to the handler.
   Works a bit like update-in. Supplies a narrowed data structure for the handler.
   Afterwards, grafts the result of the handler back into db.
   Usage:
     (path :some :path)
     (path [:some :path])
     (path [:some :path] :to :here)
     (path [:some :path] [:to] :here)
  "
  [frame-atom]
  (fn path
    [& args]
    (let [path (flatten args)]
      (when (empty? path)
        (error @frame-atom "re-frame: \"path\" middleware given no params."))
      (fn path-middleware
        [handler]
        (fn path-handler
          [db v]
          (update-in db path handler v))))))

(defn enrich
  "Middleware factory which runs a given function \"f\" in the after position.
  \"f\" is (db v) -> db
  Unlike \"after\" which is about side effects, \"enrich\" expects f to process and alter
  db in some useful way, contributing to the derived data, flowing vibe.
  Imagine that todomvc needed to do duplicate detection - if any two todos had
  the same text, then highlight their background, and report them in a warning
  down the bottom.
  Almost any action (edit text, add new todo, remove a todo) requires a
  complete reassesment of duplication errors and warnings. Eg: that edit
  update might have introduced a new duplicate or removed one. Same with a
  todo removal.
  And to perform this enrichment, a function has to inspect all the todos,
  possibly set flags on each, and set some overall list of duplicates.
  And this duplication check might just be one check amoung many.
  \"f\" would need to be both adding and removing the duplicate warnings.
  By applying \"f\" in middleware, we keep the handlers simple and yet we
  ensure this important step is not missed."
  [_frame-atom]
  (fn enrich
    [f]
    (fn enrich-middleware
      [handler]
      (fn enrich-handler
        [db v]
        (f (handler db v) v)))))


(defn after
  "Middleware factory which runs a function \"f\" in the \"after handler\"
  position presumably for side effects.
  \"f\" is given the new value of \"db\". It's return value is ignored.
  Examples: \"f\" can run schema validation. Or write current state to localstorage. etc.
  In effect, \"f\" is meant to sideeffect. It gets no chance to change db. See \"enrich\"
  (if you need that.)"
  [_frame-atom]
  (fn after
    [f]
    (fn after-middleware
      [handler]
      (fn after-handler
        [db v]
        (let [new-db (handler db v)]
          (f new-db v)                                                                                                ; call f for side effects
          new-db)))))


; EXPERIMENTAL

(defn on-changes
  "Middleware factory which acts a bit like \"reaction\"  (but it flows into db , rather than out)
  It observes N  inputs (paths into db) and if any of them change (as a result of the
  handler being run) then it runs 'f' to compute a new value, which is
  then assoced into the given out-path within app-db.

  Usage:

  (defn my-f
    [a-val b-val]
    ... some computation on a and b in here)

  (on-changes my-f [:c]  [:a] [:b])

  Put the middlware above on the right handlers (ones which might change :a or :b).
  It will:
     - call 'f' each time the value at path [:a] or [:b] changes
     - call 'f' with the values extracted from [:a] [:b]
     - assoc the return value from 'f' into the path  [:c]
  "
  [_frame-atom]
  (fn on-changes
    [f out-path & in-paths]
    (fn on-changed-middleware
      [handler]
      (fn on-changed-handler
        [db v]
        (let [; run the handler, computing a new generation of db
              new-db (handler db v)

              ; work out if any "inputs" have changed
              new-ins (map #(get-in new-db %) in-paths)
              old-ins (map #(get-in db %) in-paths)
              changed-ins? (some false? (map identical? new-ins old-ins))]

          ; if one of the inputs has changed, then run 'f'
          (if changed-ins?
            (assoc-in new-db out-path (apply f new-ins))
            new-db))))))