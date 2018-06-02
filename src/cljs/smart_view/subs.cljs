(ns smart-view.subs
  (:require [re-frame.core :as re-frame]
            [datascript.core :as d]))

(defn only-ours [tree]
  (update tree :project/dependency (fn [deps]
                                     (->> deps
                                          (filter :project/ours?)
                                          (map only-ours)))))

(re-frame/reg-sub
 ::dependency-tree
 (fn [{:keys [:datascript/db :selected-project-id]} _]
   (when selected-project-id
     (let [tree (d/pull db '[:project/name :project/ours?  {:project/dependency 6}] selected-project-id)]
      (only-ours tree)))))

(re-frame/reg-sub
 ::all-projects
 (fn [{:keys [:datascript/db]} _]
   (if db
    (let [all-projects (d/q '[:find ?pid ?pname
                              :where
                              [?pid :project/name ?pname]
                              [?pid :project/ours? true]]
                            db)]
      (->> all-projects
           (map (fn [[id name]]
                  {:db/id id :project/name name}))
           (sort-by :project/name)))
    [])))

(re-frame/reg-sub
 ::selected-project-id
 (fn [{:keys [:selected-project-id]} _]
   selected-project-id))

(re-frame/reg-sub
 ::selected-tab-id
 (fn [{:keys [:selected-tab-id]} _]
   selected-tab-id))

(re-frame/reg-sub
 ::features
 (fn [{:keys [:datascript/db :selected-project-id]} [_ type]]
   (let [features (d/q '[:find ?fns ?fp ?fname ?fnspath ?fline
                         :in $ ?ftype
                         :where
                         [?fid :feature/namespace ?fnsid]
                         [?fid :feature/line ?fline]
                         [?fnsid :namespace/name ?fns]
                         [?fnsid :namespace/path ?fnspath]
                         [?fid :feature/project ?fpid]
                         [?fpid :project/name ?fp]
                         [?fid :feature/name ?fname]
                         [?fid :feature/type ?ftype]]
                       db
                       type)]
     (->> features
          (map (fn [[fns fp fname fnspath fline]]
                 {:feature/name fname
                  :namespace/path fnspath
                  :feature/line fline
                  :namespace/name fns
                  :feature/project fp}))
          (group-by :feature/project)
          (map (fn [[p features]]
                 [p (group-by :namespace/name features)]))
          (into {})))))

(re-frame/reg-sub
 ::specs
 (fn [{:keys [:datascript/db]} _]
   (let [specs (d/q '[:find ?sns ?sp ?sname ?snspath ?sline ?sform
                      :where
                      [?sid :spec/namespace ?snsid]
                      [?sid :spec/line ?sline]
                      [?sid :spec/project ?spid]
                      [?sid :spec/form ?sform]
                      [?sid :spec/name ?sname]
                      [?snsid :namespace/name ?sns]
                      [?snsid :namespace/path ?snspath]
                      [?spid :project/name ?sp]]
                    db)
         ret (->> specs
                  (map (fn [[sns sp sname snspath sline sform]]
                         {:spec/name sname
                          :namespace/path snspath
                          :spec/line sline
                          :spec/form sform
                          :namespace/name sns
                          :spec/project sp}))
                  (group-by :spec/project)
                  (map (fn [[p spcs]]
                           [p (group-by :namespace/name spcs)]))
                  (into {}))]
     ret)))

(re-frame/reg-sub
 ::smart-contracts
 (fn [{:keys [:datascript/db]} _]
   (let [contracts (d/q '[:find ?pid ?pname ?sid ?spath
                          :where
                          [?pid :project/name ?pname]
                          [?sid :smart-contract/project ?pid]
                          [?sid :smart-contract/path ?spath]]
                        db)]
     (->> contracts
          (map (fn [[pid pname sid spath]]
                 {:project/name pname
                  :smart-contract/path spath}))
          (group-by :project/name))))) 

