(ns user
  (:require [clojure.tools.namespace.repl :refer (refresh)]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [schema.core :as schema]
            [whip.layout :refer :all]
            [whip.display :refer :all]
            [whip.core :refer :all]))

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
