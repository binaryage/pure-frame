# pure-frame: a re-frame fork

[![Build Status](https://travis-ci.org/darwin/pure-frame.svg)](https://travis-ci.org/binaryage/pure-frame)

This is a fork of [re-frame](https://github.com/Day8/re-frame) originated in [PR #107](https://github.com/Day8/re-frame/pull/107)

My initial goal was to allow multiple re-frame instances hosted in a single javascript context.
But during rewrite I realized that proper decoupling of re-frame from reagent and core.async will be useful
for other scenarios as well. For example with pure-frame you can easily replace router-loop or
implement underlying storage for app-db in a different way.

I ended up implementing re-frame instance as a value with a set of pure functions to transform it. Event processor
is expressed as a tranducer which allows great flexibility.

In your project you should require `re-frame.frame` and call `make-frame` to create your own re-frame instance(s).

For backward compatibility you can require `re-frame.core` you get compatible interface to the re-frame v0.4.1.
In [v041_api.cljs](blob/master/src/re_frame/v041_api.cljs) you can see how was old re-frame implemented on top of `re-frame.frame`:

* there is one global app-db and one global app-frame
* app-db is backed by reagent/atom
* app-frame has default loggers
* router event queue is implemented using core.async channel
* familiar original re-frame api shim is provided in `re-frame.core`

I decided to remove some functionality of original re-frame, because I don't personally use it and didn't want to
port it over:

* removed undoable middleware and related functionality
* removed pure middleware, because it makes no sense in the new model
* removed some sanity checks of wrong usage of middle-ware factories to simplify the code a bit

Also I have added some tests.