# pure-frame: a re-frame fork

[![Build Status](https://travis-ci.org/binaryage/pure-frame.svg?branch=master)](https://travis-ci.org/binaryage/pure-frame)

This is a fork of [re-frame](https://github.com/Day8/re-frame) originated in [PR #107](https://github.com/Day8/re-frame/pull/107)

My initial goal was to allow multiple re-frame instances hosted in a single javascript context.
But during rewrite I realized that proper decoupling of re-frame from reagent and core.async will be useful
for other scenarios as well. For example with pure-frame you can easily replace router-loop or
implement underlying storage for app-db in a different way.

I ended up implementing re-frame instance as a value with a set of pure functions to transform it. Event processor
is expressed as a tranducer which allows great flexibility.

In your project you should require `re-frame.frame` and call `make-frame` to create your own re-frame instance(s).

For backward compatibility you can require `re-frame.core` where you get compatible interface to original re-frame.

Pure-frame is pretty flexible, so I have implemented two compatibility modes
* v041 - mode implements re-frame 0.4.1 (event queue is a core.async channel)
* v050 - (default) mode implements re-frame 0.5.0 (event queue is a custom finite state machine)

You can specify closure-define in your project.clj to get 0.4.1 behaviour:
```clojure
:closure-defines {"re_frame.config.core_compatible_with" "v041"} ; add this to your cljsbuild :compiler options
```

Those implementations can serve as examples how to use low-level re-frame.frame parts.

[v041_api.cljs](src/re_frame/v041_api.cljs)
[v041_router.cljs](src/re_frame/v041_router.cljs)
[router.cljs](src/re_frame/router.cljs)

* there is one global app-db, one global app-frame and one global event queue
* app-db is backed by reagent/atom
* app-frame has default loggers
* familiar original re-frame api shim is provided in `re-frame.core`

I decided to remove some functionality of original re-frame, because I don't personally use it and didn't want to
port it over:

* removed undoable middleware and related functionality
* removed pure middleware, because it makes no sense in the new model
* removed some sanity checks of wrong usage of middle-ware factories to simplify the code a bit

Also I have added [some tests](test/re_frame/test).