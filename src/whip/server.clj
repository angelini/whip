(ns whip.server
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [cheshire.core :as json])
  (:import [java.net ServerSocket InetSocketAddress]
           [java.io InputStreamReader BufferedReader DataOutputStream]))

(defn listen [socket in]
  (let [stream (InputStreamReader. (.getInputStream socket))
        reader (BufferedReader. stream)]
    (async/go
      (loop []
        (do (async/>! in (.readLine reader))
            (recur))))))

(defn output [socket out]
  (let [stream (DataOutputStream. (.getOutputStream socket))]
    (async/go
      (loop []
        (do (.writeBytes stream (async/<! out))
            (recur))))))

(defrecord Server [port socket-server socket in out]
  component/Lifecycle

  (start [this]
    (println "; Starting server")
    (if socket
      this
      (let [socket-server (doto (ServerSocket.)
                                (.setReuseAddress true)
                                (.bind (InetSocketAddress. port)))
            socket (.accept socket-server)
            in (async/chan 5)
            out (async/chan 5)]
        (listen socket in)
        (output socket out)
        (assoc this :socket-server socket-server
                    :socket socket
                    :in in :out out))))

  (stop [this]
    (println "; Stopping server")
    (if-not socket
      this
      (do (map #(.close %) [socket socket-server])
          (map async/close! [in out])
          (assoc this :socket nil
                      :in nil :out nil)))))

(defn create-server [port]
  (map->Server {:port port}))

(defn input-chan [server]
  (async/map< json/parse-string (:in server)))

(defn emit [server message]
  (async/>!! (:out server) (json/generate-string message)))
