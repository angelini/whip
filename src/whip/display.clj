(ns whip.display
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component])
  (:import [com.googlecode.lanterna TerminalFacade]
           [com.googlecode.lanterna.input Key Key$Kind]
           [com.googlecode.lanterna.terminal Terminal$Color]))

(def key-codes
  {Key$Kind/NormalKey :normal
   Key$Kind/Escape :escape
   Key$Kind/Backspace :backspace
   Key$Kind/ArrowLeft :left
   Key$Kind/ArrowRight :right
   Key$Kind/ArrowUp :up
   Key$Kind/ArrowDown :down
   Key$Kind/Insert :insert
   Key$Kind/Delete :delete
   Key$Kind/Home :home
   Key$Kind/End :end
   Key$Kind/PageUp :page-up
   Key$Kind/PageDown :page-down
   Key$Kind/Tab :tab
   Key$Kind/ReverseTab :reverse-tab
   Key$Kind/Enter :enter
   Key$Kind/Unknown :unknown
   Key$Kind/CursorLocation :cursor-location})

(defn parse-key [k]
  (let [kind (key-codes (.getKind k))]
    {:char (if (= kind :normal) (.getCharacter k) kind)
     :alt? (.isAltPressed k)
     :ctrl? (.isCtrlPressed k)}))

(defn listen-for-input [screen chan]
  (async/go
    (loop []
      (let [k (.readInput screen)]
        (if (nil? k)
          (do (<! (async/timeout 100))
              (recur))
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
  (.setCursorPosition (:screen display) x y))

(defn put-char [display x y c]
  (.putString (:screen display) x y c
                                Terminal$Color/DEFAULT
                                Terminal$Color/DEFAULT #{}))

(defn refresh-screen [display]
  (.refresh (:screen display)))

(defn size [display]
  (let [s (.getTerminalSize (:screen display))]
    [(.getColumns s) (.getRows s)]))
