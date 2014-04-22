(ns whip.display
  (:require [whip.server :refer :all]
            [whip.base.layout :refer :all]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [schema.macros :as sm])
  (:import [whip.base.layout Cell]))

(sm/defrecord DisplayBuffer
  [cursor :- {:x s/Int :y s/Int}
   content :- [[Cell]]])

(defn parse-event [e] e)

(defrecord Display [server in buffer]
  component/Lifecycle

  (start [this]
    (println "; Starting display")
    (if buffer
      this
      (assoc this :in (async/map< parse-event (input-chan server))
                  :buffer (atom (DisplayBuffer. {:x 0 :y 0} [[]])))))

  (stop [this]
    (println "; Stopping display")
    (if-not buffer
      this
      (do
        (async/close! in)
        (assoc this :in nil :buffer nil)))))

(defn create-display []
  (map->Display {}))

(defn event-chan [display]
  (:in display))

(defn set-cursor [display x y]
  (swap! (:buffer display) (fn [buffer]
                             (assoc buffer :cursor {:x x :y y}))))

(defn put-char [display x y c]
  (swap! (:buffer display) (fn [buffer]
                             (assoc-in buffer [y x] map->Cell {:c c
                                                               :fg :white
                                                               :bg :black}))))

(defn sync-display [display]
  (emit (:server display) @(:buffer display)))

(defn size [display] [0 0])
