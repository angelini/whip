(ns whip.display
  (:require [whip.base.layout :refer :all]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [schema.macros :as sm])
  (:import [whip.base.layout Cell]))

(def SizeMessage
  {:width s/Int
   :height s/Int})

(def KeyMessage
  {:c s/Str
   :alt? s/Bool
   :ctrl? s/Bool})

(sm/defrecord MessageWrapper
  [type :- s/Str
   body :- (s/either SizeMessage KeyMessage)])

(sm/defrecord DisplayBuffer
  [cursor :- {:x s/Int :y s/Int}
   content :- [[Cell]]])

(defn parse-message [message]
  (let [{:keys [type body]} message]
    (cond
      (and (= type "size")
           (nil? (s/check SizeMessage body))) (map->MessageWrapper
                                                {:type :size
                                                 :body body})
      (and (= type "key")
           (nil? (s/check KeyMessage body))) (map->MessageWrapper
                                               {:type :key
                                                :body body})
      :else (map->MessageWrapper {:type :unknown}))))

(defrecord Display [in out buffer]
  component/Lifecycle

  (start [this]
    (println "; Starting display")
    (if buffer
      this
      (assoc this :in (async/map< parse-message in)
                  :out out
                  :buffer (atom (DisplayBuffer. {:x 0 :y 0} [[]])))))

  (stop [this]
    (println "; Stopping display")
    (if-not buffer
      this
      (do
        (map async/close! [in out])
        (assoc this :in nil :out nil :buffer nil)))))

(defn create-display [in out]
  (map->Display {:in in
                 :out out}))

(defn message-chan [display]
  (:in display))

(defn set-cursor [display x y]
  (swap! (:buffer display) (fn [buffer]
                             (assoc buffer :cursor {:x x :y y}))))

(defn put-cell [display x y cell]
  (swap! (:buffer display) (fn [buffer]
                             (assoc-in buffer [y x] cell))))

(defn put-char [display x y c]
  (put-cell display x y (map->Cell {:c c :fg :white :bg :black})))

(defn sync-display [display]
  (async/>!! (:out display) @(:buffer display)))

(defn size [display] [10 10])
