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
        (do (async/>! in (-> (.readLine reader)
                             (json/parse-string true)))
            (recur))))))

(defn output [socket out]
  (let [stream (DataOutputStream. (.getOutputStream socket))]
    (async/go
      (loop []
        (do (.writeBytes stream (-> (async/<! out)
                                    (json/generate-string)))
            (recur))))))

(defn create-io-chans [socket]
  (let [in (async/chan 5)
        out (async/chan 5)]
    (listen socket in)
    (output socket out)
    [in out]))

(defn emit-connections [socket-server chan]
  (async/go
    (loop []
      (do (async/>! chan (.accept socket-server))
          (recur)))))

(defrecord Server [port socket-server chan]
  component/Lifecycle

  (start [this]
    (println "; Starting server")
    (if socket-server
      this
      (let [socket-server (doto (ServerSocket.)
                                (.setReuseAddress true)
                                (.bind (InetSocketAddress. port)))
            chan (async/chan 5)]
        (emit-connections socket-server chan)
        (assoc this :socket-server socket-server
                    :chan chan))))

  (stop [this]
    (println "; Stopping server")
    (if-not socket-server
      this
      (do (.close socket-server)
          (async/close! chan)
          (assoc this :socket-server nil
                      :chan nil)))))

(defn create-server [port]
  (map->Server {:port port}))

(defn connections-chan [server]
  (async/map< create-io-chans (:chan server)))
