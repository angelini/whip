(ns whip.display
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component])
  (:import [com.googlecode.lanterna TerminalFacade]))

(defn parse-key [k]
  {:char (.getCharacter k)
   :alt? (.isAltPressed k)
   :ctrl? (.isCtrlPressed k)})

(defn listen-for-input [screen chan]
  (async/go
    (loop []
      (let [_ (<! (async/timeout 100))
            k (.readInput screen)]
        (if (nil? k)
          (recur)
          (do (async/>! chan (parse-key k))
              (recur)))))))

(defrecord Display [screen input-chan]
  component/Lifecycle

  (start [this]
    (println "; Starting display")
    (if screen
      this
      (let [scr (TerminalFacade/createScreen)
            chan (async/chan)]
        (listen-for-input scr chan)
        (.startScreen scr)
        (assoc this :screen scr
                    :input-chan chan))))

  (stop [this]
    (println "; Stopping display")
    (if-not screen
      this
      (do
        (.stopScreen screen)
        (async/close! input-chan)
        (assoc this :screen nil)))))

(defn create-display []
  (map->Display {}))

(defn input-chan [display]
  (:input-chan display))

(defn set-cursor [display x y]
  (.moveCursor (:screen display) x y))

(defn put-char [display x y c]
  (.putString (:screen display) x y c))

(defn refresh-screen [display]
  (.refresh (:screen display)))

(defn size [display]
  (let [s (.getTerminalSize (:screen display))]
    [(.getColumns s) (.getRows s)]))
