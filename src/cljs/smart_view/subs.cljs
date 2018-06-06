(ns smart-view.subs
  (:require [re-frame.core :as re-frame]
            [datascript.core :as d]))

(re-frame/reg-sub
 ::inheritance-tree
 (fn [{:keys [:datascript/db :selected-smart-contract-id]} _]
   (when selected-smart-contract-id
     (let [tree (d/pull db '[:db/id
                             :contract/name
                             {:contract/vars [:var/type :var/name :var/public?]}
                             {:contract/functions [:function/name
                                                   :function/public?
                                                   {:function/vars [:var/type :var/name :var/parameter?]}]}
                             {:contract/inherits 6}]
                        selected-smart-contract-id)]
       tree))))

(re-frame/reg-sub
 ::all-smart-contracts
 (fn [{:keys [:datascript/db]} _]
   (if db
     (let [all (d/q '[:find ?c-id ?c-name
                      :where
                      [?c-id :contract/name ?c-name]]
                    db)]
       (->> all
            (map (fn [[id name]]
                   {:db/id id :contract/name name}))
            (sort-by :contract/name)))
     [])))

(re-frame/reg-sub
 ::all-smart-contracts-tree
 (fn [{:keys [:datascript/db]} _]
   (when db
     (let [all-root-ids  (d/q '[:find ?c-id
                                :where
                                [?c-id :contract/name ?c-name]
                                [(missing? $ ?c-id :contract/inherits)]]
                              db)
           all-contracts (map (fn [[cid]]
                                (d/pull db '[:db/id
                                             :contract/name
                                             :token/row
                                             {:file/_contracts [:file/name :file/full-path]}
                                             {:contract/vars [:var/type :var/name :var/public? :token/row]}
                                             {:contract/functions [:function/name
                                                                   :token/row
                                                                   :function/public?
                                                                   {:function/vars [:var/type :var/name :var/parameter?]}]}
                                             {:contract/events [:event/name
                                                                :token/row
                                                                {:event/vars [:var/type :var/name :var/parameter?]}]}
                                             {:contract/_inherits 10}]
                                        cid))
                              all-root-ids)]
       {:contract/_inherits all-contracts
        :contract/name "_ROOT"}))))

(re-frame/reg-sub
 ::selected-smart-contract-id
 (fn [{:keys [:selected-smart-contract-id]} _]
   selected-smart-contract-id))

(re-frame/reg-sub
 ::selected-tab-id
 (fn [{:keys [:selected-tab-id]} _]
   selected-tab-id))

(re-frame/reg-sub
 :zoom
 (fn [db]
   (:zoom db)))

(re-frame/reg-sub
 :contracts-map/controls
 (fn [db]
   (:contracts-map/controls db)))
