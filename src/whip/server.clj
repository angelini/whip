(ns whip.server
  (:require [zeromq.zmq :as zmq]
            [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [cheshire.core :as json]))

(defn listen [socket in]
  (async/go
    (loop []
      (do (async/>! in (zmq/receive socket))
          (recur)))))

(defn output [socket out]
  (async/go
    (loop []
      (do (zmq/send-str socket (async/<! out))
          (recur)))))

(defrecord Server [port context socket in out]
  component/Lifecycle

  (start [this]
    (println "; Starting server")
    (if context
      this
      (let [context (zmq/context 1)
            socket (zmq/socket context :rep)
            in (async/chan 5)
            out (async/chan 5)]
        (zmq/bind socket (str "tcp://*:" port))
        (listen socket in)
        (output socket out)
        (assoc this :context (zmq/context 1)
                    :socket socket
                    :in in :out out))))

  (stop [this]
    (println "; Stopping server")
    (if-not context
      this
      (do (zmq/unbind socket (str "tcp://*:" port))
          (map #(.close %) [socket context])
          (map async/close! [in out])
          (assoc this :context nil
                      :socket nil
                      :in nil :out nil)))))

(defn create-server [port]
  (map->Server {:port port}))

(defn input-chan [server]
  (:in server))

(defn emit [server message]
  (async/>!! (:out server) (json/generate-string message)))
