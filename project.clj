(defproject seshat "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure        "1.9.0-alpha10"]
                 [org.clojure/clojurescript  "1.9.229"]
                 [com.datomic/datomic-pro "0.9.5404"]
                 [org.mindrot/jbcrypt "0.3m"]
                 [cheshire "5.6.3"]
                 [reagent "0.6.0-rc"]
                 [binaryage/devtools "0.6.1"]
                 [re-frame "0.8.0"]
                 [day8.re-frame/http-fx "0.0.4"]
                 [fogus/ring-edn "0.3.0"]
                 [compojure "1.5.0"]
                 [yogthos/config "0.8"]
                 [ring "1.4.0"]]

  :plugins [[lein-cljsbuild "1.1.3"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljc"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler seshat.dev.server/handler}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username :env/DATOMIC_USERNAME
                                   :password :env/DATOMIC_PASSWORD}}

  :profiles
  {:dev
   {:dependencies [
                   [figwheel-sidecar "0.5.4-3"]
                   [com.cemerick/piggieback "0.2.1"]]

    :source-paths ["test/cljc" "test/clj" "devsrc/clj"]
    
    :plugins      [[lein-figwheel "0.5.4-3"]
                   [lein-doo "0.1.6"]
                   [cider/cider-nrepl "0.13.0"]]
    }}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs" "src/cljc"]
     :figwheel     {:on-jsload "seshat.core/mount-root"}
     :compiler     {:main                 seshat.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true}}

    {:id           "min"
     :source-paths ["src/cljs" "src/cljc"]
     :jar true
     :compiler     {:main            seshat.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}
    {:id           "test"
     :source-paths ["src/cljs" "test/cljs"]
     :compiler     {:output-to     "resources/public/js/compiled/test.js"
                    :main          seshat.runner
                    :optimizations :none}}
    ]}

  :main seshat.server

  :aot [seshat.server]

  :uberjar-name "seshat.jar"

;  :prep-tasks [["cljsbuild" "once" "min"] "compile"]
  )
