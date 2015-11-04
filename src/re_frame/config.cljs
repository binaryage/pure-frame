(ns re-frame.config)

; -------------------------------------------------------------------------------------------------------------------
; you can override these defaults from project.clj via :closure-defines
;   see http://www.martinklepsch.org/posts/parameterizing-clojurescript-builds.html
;
; -------------------------------------------------------------------------------------------------------------------

; "v041" implements core.async event loop, see v041_router.cljs
; "v050" implements mini-FSM event loop, see router.cljs
(goog-define core-compatible-with "v050")

(goog-define run-loop-automatically true)