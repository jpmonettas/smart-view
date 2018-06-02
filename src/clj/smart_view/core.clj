(ns smart-view.core
  (:require [clj-antlr.core :as antlr]
            [cljs.analyzer.api :as ana]
            [cljs.tagged-literals :as tags]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.reader-types :as reader-types]
            [clojure.zip :as zip]
            [datascript.core :as d]
            [ike.cljj.file :as files]
            [ike.cljj.stream :as stream])
  (:import java.io.File))

(def projects-folder (atom nil))

(def schema {:project/dependency {:db/valueType :db.type/ref
                                  :db/cardinality :db.cardinality/many}
             :namespace/project {:db/valueType :db.type/ref}
             :feature/namespace {:db/valueType :db.type/ref}
             :feature/project {:db/valueType :db.type/ref}
             :spec/namespace {:db/valueType :db.type/ref}
             :spec/project {:db/valueType :db.type/ref}
             :smart-contract/project {:db/valueType :db.type/ref}})

(def db-conn (d/create-conn schema))


(defn get-all-files [base-dir pred?]
  (with-open [childs (files/walk (files/as-path base-dir))]
    (->> (stream/stream-seq childs)
         (filter pred?)
         (mapv (fn [p] (File. (str p)))))))

(def parse-solidity (-> (io/resource "grammars/Solidity.g4")
                        slurp
                        (antlr/parser)))

(def re-contract (slurp "/home/jmonetta/my-projects/district0x/memefactory/resources/public/contracts/src/RegistryEntry.sol"))
(def parsed (let [parsed-contract (parse-solidity re-contract)]
              parsed-contract))

(def ^:dynamic *current-project-id*)
(def ^:dynamic *current-file-id*)
(def ^:dynamic *current-contract-id*)

(defn token-facts [ent-id token]
  (let [{:keys [row column]} (:clj-antlr/position (meta token))]
   [[ent-id :token/row row]
    [ent-id :token/column column]]))

(defn import-directive-facts [[_ _ file :as import]]
  (let [id (d/tempid :db.part/user)]
    (into
     [[*current-file-id* :file/imports id]
      [id :import/name (second (re-find #"\"(.*)\"" file))]]
     (token-facts id import))))

(defn pragma-directive-facts [[_ _ _ [_ [_ [_ [_ version-op] version]]] :as pragma]]
  [[*current-file-id* :file/pragma-version (str version-op version)]])

(defn using-for-declaration-facts [part]
  [])

(defn parse-variable [statements]
  (let [[_ [t v]] (some #(when (= (first %) :typeName) %) statements)
        vtype (if (= t :elementaryTypeName) v (second v))
        vname (second (some #(when (= (first %) :identifier) %) statements))]
    [vtype vname]))

(defn state-variable-declaration-facts [[_ & statements :as state-var]]
  (let [[vtype vname] (parse-variable statements)
        public? (some #(= % "public") statements)
        id (d/tempid :db.part/user)]
    (into
     [[*current-contract-id* :contract/vars id]
      [id :var/type vtype]
      [id :var/name vname]
      [id :var/public? public?]]
     (token-facts id state-var))))

(defn enum-definition-facts [[_ & statements :as enum]]
  (let [ename (second (some #(when (= (first %) :identifier) %) statements))
        values (->> statements
                    (keep (fn [[t v]]
                            (when (= t :enumValue)
                              (second v)))))
        id (d/tempid :db.part/user)]
    (-> [[*current-contract-id* :contract/enums id]
         [id :enum/name ename]]
        (into (map (fn [v] [id :enum/values v]) values))
        (into (token-facts id enum)))))



(defn struct-definition-facts [[_ & statements :as struct]]
  (let [sname (second (some #(when (= (first %) :identifier) %) statements))
        sid (d/tempid :db.part/user)
        vars-facts (->> statements
                        (keep (fn [[t & vs]]
                                (when (= t :variableDeclaration)
                                  (let [[vtype vname] (parse-variable vs)
                                        vid (d/tempid :db.part/user)]
                                    [[sid :struct/vars vid]
                                     [vid :var/type vtype]
                                     [vid :var/name vname]]))))
                        (reduce concat))]
    vars-facts
    (-> [[*current-contract-id* :contract/structs sid]
         [sid :struct/name sname]]
        (into vars-facts)
        (into (token-facts sid struct)))))

(defn modifier-definition-facts [part]
  [])

(defn function-definition-facts [part]
  [])

(defn contract-part-facts [[_ [part-type :as part]]]
  (case part-type
    ;; :usingForDeclaration (using-for-declaration-facts part)
    :stateVariableDeclaration (state-variable-declaration-facts part)
    :enumDefinition (enum-definition-facts part)
    ;; :structDefinition (struct-definition-facts part)
    ;; :modifierDefinition (modifier-definition-facts part)
    ;; :functionDefinition (function-definition-facts part)
    (do (println "Warning not doing anything with " part-type)
                       [])))

(defn contract-id-from-name [contract-name]
  -1)

(defn contract-definition-facts [[_ _ [_ contract-name] & r]]
  (binding [*current-contract-id* (contract-id-from-name contract-name)]
    (let [inherits-facts (when (= (first r) "is")
                           (->> r second rest
                                (map (fn [[_ [_ super-cn]]]
                                       [*current-contract-id* :contract/inherits (contract-id-from-name super-cn)]))))
          contract-parts (filter (fn [x] (= (first x) :contractPart)) r)
          contract-facts [[*current-contract-id* :contract/name contract-name]
                          [*current-file-id* :file/contracts *current-contract-id*]]]
      (-> contract-facts
          (into inherits-facts)
          (into (mapcat contract-part-facts contract-parts))))))

(defn source-unit-facts [[_ & source-unit-childs]]
  (->> source-unit-childs
       (mapcat (fn [[node-id :as child]]
                 (case node-id
                   :pragmaDirective (pragma-directive-facts child)
                   :importDirective (import-directive-facts child)
                   :contractDefinition (contract-definition-facts child)
                   (do (println "Warning not doing anything with " node-id)
                       []))))))

#_(binding [*current-project-id* "memefactory-id"
          *current-file-id* "RegistryEntry.sol-id"]
  (source-unit-facts parsed))

#_(-> (parse-solidity re-contract)
    (analyze-contract-file))


(defn re-index-all
  ([] (re-index-all nil))
  ([folder]
   ;; (d/reset-conn! db-conn (d/empty-db schema))
   ;; (let [all-projects (get-all-projects (or folder @projects-folder))]
   ;;   (transact-projects db-conn all-projects)
   ;;   (transact-solidity db-conn all-projects))
   ))

(defn db-edn []
  (pr-str @db-conn))

(comment
  (d/q '[:find ?pid ?pname
         :where
         [?pid :project/name ?pname]]
       @db-conn)
  
  
  (re-index-all "/home/jmonetta/my-projects/district0x")

  (d/reset-conn! db-conn (d/empty-db))

  (def contract "contract test {
        uint256 a;
        function f() {}
    }")

  )

