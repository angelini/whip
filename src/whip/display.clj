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

(defn parse-key-body [body]
  (let [c (:c body)]
    (if (= \: (.charAt c 0))
      (assoc body :c (keyword (subs c 1)))
      body)))

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
                                                :body (parse-key-body body)})
      :else (map->MessageWrapper {:type :unknown}))))

(defn generate-blank-row [x]
  (vec (map (constantly (map->Cell {:c \space :fg :white :bg :black})) (range 0 x))))

(defn generate-blank-content [x y]
  (vec (map (constantly (generate-blank-row x)) (range 0 y))))

(defn content-size [content]
  {:height (count content)
   :width (count (nth content 0))})

(defrecord Display [in out buffer]
  component/Lifecycle

  (start [this]
    (println "; Starting display")
    (if buffer
      this
      (assoc this :in (async/map< parse-message in)
                  :out out
                  :buffer (atom (DisplayBuffer. {:x 0 :y 0}
                                                (generate-blank-content 10 10))))))

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

(defn set-cursor [display x y]
  (swap! (:buffer display) (fn [buffer]
                             (assoc buffer :cursor {:x x :y y}))))

(defn put-cell [display x y cell]
  (swap! (:buffer display)
         (fn [buffer]
           (let [{:keys [width height]} (content-size (:content buffer))]
             (if (and (<= 0 x (- width 1))
                      (<= 0 y (- height 1))) (assoc-in buffer [:content y x] cell)
                 buffer)))))

(defn put-char [display x y c]
  (put-cell display x y (map->Cell {:c c :fg :white :bg :black})))

(defn sync-display [display]
  (async/>!! (:out display) @(:buffer display)))

(defn size [display]
  (let [buffer @(:buffer display)]
    (content-size (:content buffer))))

(defn adjust-size [display size]
  (let [{:keys [width height]} size]
    (swap! (:buffer display)
           (fn [buffer]
             (assoc buffer :content (generate-blank-content width height))))))

(defn display-handler [display message]
  (do (case (:type message)
        :size (adjust-size display (:body message))
        nil)
      message))

(defn message-chan [display]
  (async/map< #(display-handler display %) (:in display)))
