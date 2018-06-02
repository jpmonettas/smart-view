(ns smart-view.server
  (:require [smart-view.handler :refer [handler]]
            [config.core :refer [env]]
            [smart-view.core :as core]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main [& [folder]]
  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (reset! core/projects-folder folder)
    (core/re-index-all)
    (run-jetty handler {:port port :join? false})))
