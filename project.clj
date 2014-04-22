(defproject whip "0.1.0-SNAPSHOT"
  :description "Text engine - experimental"
  :url "https://github.com/angelini/whip"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :jvm-opts ["-Djava.library.path=/usr/lib:/usr/local/lib"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [com.stuartsierra/component "0.2.1"]
                 [prismatic/schema "0.2.1"]
                 [org.zeromq/cljzmq "0.1.4"]
                 [cheshire "5.3.1"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]]}})
