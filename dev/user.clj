(ns user
  (:require
         ;; External
            [clojure.tools.namespace.repl :refer (refresh)]
            [clojure.test :refer (run-tests)]
            [clojure.core.async :as async]
            [clojure.math.numeric-tower :as math]
            [com.stuartsierra.component :as component]
            [schema.core :as schema]
            [schema.macros :as sm]
            [cheshire.core :as json]

         ;; Local
            [whip.base.layout :refer :all]
            [whip.base.state :refer :all]
            [whip.base.macros :refer :all]
            [whip.plugins.plugin :refer :all]
            [whip.server :refer :all]
            [whip.display :refer :all]
            [whip.loader :refer :all]
            [whip.core :refer :all]

         ;; Tests
            [whip.display-test :as display-test]
            [whip.base.layout-test :as layout-test]
            [whip.base.state-test :as state-test]))

(def system nil)

(defn init []
  (alter-var-root #'system
                  (constantly (create-system))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system component/stop))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
