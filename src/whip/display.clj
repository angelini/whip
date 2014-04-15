(ns whip.display
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [lanterna.screen :as s]))

(defn listen-for-input [screen chan]
  (async/go
    (loop []
      (async/>! chan (s/get-key-blocking screen))
      (recur))))

(defrecord Display [console-type screen input-chan]
  component/Lifecycle

  (start [this]
    (println "; Starting display")
    (if screen
      this
      (let [scr (s/get-screen console-type)
            chan (async/chan)]
        (listen-for-input scr chan)
        (s/start scr)
        (assoc this :screen scr
                    :input-chan chan))))

  (stop [this]
    (println "; Stopping display")
    (if-not screen
      this
      (do
        (s/stop screen)
        (async/close! input-chan)
        (assoc this :screen nil)))))

(defn create-display [console-type]
  (map->Display {:console-type console-type}))

(defn input-chan [display]
  (:input-chan display))

(defn set-cursor [display x y]
  (s/move-cursor (:screen display) x y))

(defn redraw [display]
  (s/redraw (:screen display)))
