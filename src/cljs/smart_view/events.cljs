(ns smart-view.events
  (:require [re-frame.core :as re-frame]
            [smart-view.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [datascript.core :as d]))


;; (d/pull @db-conn '[:project/name {:project/dependency 6}] 2)
;; (d/pull @db-conn '[:project/name :project/dependency] 1)

(re-frame/reg-event-fx
 ::initialize-db
 (fn [_ _]
   {:db db/default-db
    :dispatch [::reload-db]}))
 
(re-frame/reg-event-fx
 ::reload-db
 (fn [cofxs [_]]
   {:http-xhrio {:method          :get
                 :uri             "http://localhost:3000/db"
                 :timeout         8000                                           ;; optional see API docs
                 :response-format (ajax/raw-response-format)  ;; IMPORTANT!: You must provide this.
                 :on-success      [::db-loaded]
                 :on-failure      [:bad-http-result]}}))

(re-frame/reg-event-fx
 ::re-index-all
 (fn [cofxs [_]]
   {:http-xhrio {:method          :get
                 :uri             "http://localhost:3000/re-index-all"
                 :timeout         8000                                           ;; optional see API docs
                 :response-format (ajax/raw-response-format)  ;; IMPORTANT!: You must provide this.
                 :on-success      [::reload-db]
                 :on-failure      [:bad-http-result]}}))


(re-frame/reg-event-db
 ::db-loaded
 (fn [db [_ new-db]]
   (let [datascript-db (cljs.reader/read-string new-db)]
     (assoc db
            :datascript/db datascript-db
            :selected-project-id (d/q '[:find ?pid .
                                        :where
                                        [?pid :project/name]
                                        [?pid :project/ours?]]
                                      datascript-db)))))

(re-frame/reg-event-fx
 ::everything-re-indexed
 (fn [cofx [_ msg]]
   (.log js/console msg)))

(re-frame/reg-event-db
 ::select-project
 (fn [db [_ pid]]
   (assoc db :selected-project-id pid)))

(re-frame/reg-event-db
 ::select-tab
 (fn [db [_ tid]]
   (assoc db :selected-tab-id tid)))


