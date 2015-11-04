(defproject binaryage/pure-frame "0.1.0"
  :description "A Clojurescript MVC-like Framework For Writing SPAs Using Reagent."
  :url "https://github.com/binaryage/pure-frame.git"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145"]
                 [org.clojure/core.async "0.2.371"]
                 [reagent "0.5.1"]]

  :profiles {:debug {:debug true}
             :dev   {:dependencies [[spellhouse/clairvoyant "0.0-48-gf5e59d3"]]
                     :plugins      [[lein-cljsbuild "1.0.5"]
                                    [com.cemerick/clojurescript.test "0.3.3"]]}}

  :clean-targets [:target-path "run/compiled"]

  :resource-paths ["run/resources"]
  :jvm-opts ["-Xmx1g" "-XX:+UseConcMarkSweepGC"]
  :source-paths []
  :test-paths ["test"]

  :cljsbuild {:builds        [{:id           "test"                                                                   ;; currently bogus, there is no demo or tests
                               :source-paths ["src" "test"]
                               :compiler     {:output-to     "run/compiled/test.js"
                                              :source-map    "run/compiled/test.js.map"
                                              :output-dir    "run/compiled/test"
                                              :optimizations :simple                                                  ;; https://github.com/cemerick/clojurescript.test/issues/68
                                              :pretty-print  true}}]

              :test-commands {"phantom" ["phantomjs" :runner "run/compiled/test.js"]}}                                ; doesn't work with phantomjs < 2.0.0

  :aliases {"auto" ["do" "clean," "cljsbuild" "clean," "cljsbuild" "auto" "demo,"]
            "once" ["do" "clean," "cljsbuild" "clean," "cljsbuild" "once" "demo,"]
            "test" ["do" "clean," "cljsbuild" "once," "cljsbuild" "test" "phantom"]})