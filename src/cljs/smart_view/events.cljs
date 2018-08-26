(ns smart-view.events
  (:require [re-frame.core :as re-frame]
            [smart-view.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [datascript.core :as d]))


(re-frame/reg-event-fx
 ::initialize-db
 (fn [_ _]
   {:db db/default-db
    :dispatch [::reload-db]}))
 
(re-frame/reg-event-fx
 ::reload-db
 (fn [cofxs [_]]
   {:http-xhrio {:method          :get
                 :uri             "http://localhost:3001/db"
                 :timeout         8000 ;; optional see API docs
                 :response-format (ajax/raw-response-format)  ;; IMPORTANT!: You must provide this.
                 :on-success      [::db-loaded]
                 :on-failure      [:bad-http-result]}}))

(re-frame/reg-event-db
 ::db-loaded
 (fn [db [_ new-db]]
   (let [datascript-db (cljs.reader/read-string new-db)]
     (assoc db
            :datascript/db datascript-db
            :selected-smart-contract-id (d/q '[:find ?c-id .
                                               :where
                                               [?c-id :contract/name]]
                                             datascript-db)))))

(re-frame/reg-event-db
 ::select-smart-contract
 (fn [db [_ cid]]
   (assoc db :selected-smart-contract-id cid)))

(re-frame/reg-event-db
 ::select-tab
 (fn [db [_ tid]]
   (assoc db :selected-tab-id tid)))


(re-frame/reg-event-db
 :zoom-in
 (fn  [db _]
   (update db :zoom #(+ % 0.02))))

(re-frame/reg-event-db
 :zoom-out
 (fn  [db _]
   (if (< 0 (:zoom db))
     (update db :zoom #(- % 0.02))
     db)))

(re-frame/reg-event-db
 :contracts-map/set-control
 (fn [db [_ control v?]]
   (assoc-in db [:contracts-map/controls control] v?)))
