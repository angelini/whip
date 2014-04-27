(ns whip.server
  (:require [clojure.core.async :as async]
            [com.stuartsierra.component :as component]
            [cheshire.core :as json]
            [cheshire.generate :refer (add-encoder encode-str)])
  (:import [java.net Socket ServerSocket InetSocketAddress]
           [java.io InputStreamReader BufferedReader DataOutputStream]))

(add-encoder Character encode-str)

(defn listen [socket in]
  (let [stream (InputStreamReader. (.getInputStream ^Socket socket))
        reader (BufferedReader. stream)]
    (async/go-loop []
      (if-let [message (-> (.readLine reader)
                           (json/parse-string true))]
        (do (async/>! in message)
            (recur))
        (do (.close socket)
            (async/close! in))))))

(defn output [socket out]
  (let [stream (DataOutputStream. (.getOutputStream ^Socket socket))]
    (async/go-loop []
      (let [message (async/<! out)]
        (cond (.isClosed socket) (async/close! out)
              (nil? message) (.close socket)
              :else (do (.writeBytes stream (-> message
                                                (json/generate-string)
                                                (str "\n")))
                        (recur)))))))

(defn create-io-chans [socket]
  (let [in (async/chan 5)
        out (async/chan 5)]
    (listen socket in)
    (output socket out)
    [in out]))

(defn emit-connections [socket-server chan]
  (async/go
    (loop []
      (do (async/>! chan (.accept ^ServerSocket socket-server))
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
      (do (.close ^ServerSocket socket-server)
          (async/close! chan)
          (assoc this :socket-server nil
                      :chan nil)))))

(defn create-server [port]
  (map->Server {:port port}))

(defn connections-chan [server]
  (async/map< create-io-chans (:chan server)))
