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
                     :plugins      [[lein-cljsbuild "1.1.0"]
                                    [lein-figwheel "0.3.8"]
                                    [com.cemerick/clojurescript.test "0.3.3"]]}}

  :clean-targets [:target-path "run/compiled"]
  :resource-paths ["run/resources"]
  :jvm-opts ["-Xmx1g" "-XX:+UseConcMarkSweepGC"]
  :source-paths ["src"]
  :test-paths ["test"]

  :cljsbuild {:builds        [{:id           "test_latest"                                                            ;; currently bogus, there is no demo or tests
                               :source-paths ["src" "test"]
                               :compiler     {:output-to     "run/compiled/latest/test.js"
                                              :source-map    "run/compiled/latest/test.js.map"
                                              :output-dir    "run/compiled/latest/test"
                                              :optimizations :simple                                                  ;; https://github.com/cemerick/clojurescript.test/issues/68
                                              :pretty-print  true}}
                              {:id           "test_v041"                                                              ;; currently bogus, there is no demo or tests
                               :source-paths ["src" "test"]
                               :compiler     {:closure-defines {"re_frame.config.core_compatible_with" "v041"}
                                              :output-to       "run/compiled/v041/test.js"
                                              :source-map      "run/compiled/v041/test.js.map"
                                              :output-dir      "run/compiled/v041/test"
                                              :optimizations   :simple                                                ;; https://github.com/cemerick/clojurescript.test/issues/68
                                              :pretty-print    true}}]

              :test-commands {"phantom_latest" ["phantomjs" :runner "run/compiled/latest/test.js"]
                              "phantom_v041"   ["phantomjs" :runner "run/compiled/v041/test.js"]}}                    ; doesn't work with phantomjs < 2.0.0

  :aliases {"auto" ["do" "clean," "cljsbuild" "clean," "cljsbuild" "auto" "demo,"]
            "once" ["do" "clean," "cljsbuild" "clean," "cljsbuild" "once" "demo,"]
            "test" ["do" "clean," "cljsbuild" "test" "phantom_latest," "cljsbuild" "test" "phantom_v041"]})