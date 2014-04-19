(ns whip.loader
  (:require [com.stuartsierra.component :as component]))

(defn load-plugin [name]
  (let [file (str "src/whip/plugins/" name ".clj")
        sym (symbol (str "whip.plugins." name))]
    (load-file file)
    sym))

(defrecord Loader [plugins loaded]
  component/Lifecycle

  (start [this]
    (println "; Starting loader")
    (if loaded
      this
      (assoc this :loaded (map load-plugin plugins))))

  (stop [this]
    (println "; Stopping loader")
    (if-not loaded
      this
      (do
        (map remove-ns loaded)
        (assoc this :loaded nil)))))

(defn create-loader [plugins]
  (map->Loader {:plugins plugins}))

(defn plugin [loader name]
  (nth (:loaded loader) (.indexOf ^clojure.lang.PersistentVector (:plugins loader) name)))
