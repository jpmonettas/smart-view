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

(def schema {:file/imports       {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :file/contracts     {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :contract/enums     {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :contract/functions {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :contract/events    {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :contract/inherits  {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :contract/modifiers {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :contract/structs   {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :contract/usings    {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :contract/vars      {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :struct/vars        {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :function/vars      {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :event/vars         {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref}
             :enum/values        {:db/cardinality :db.cardinality/many}})

(def db-conn (d/create-conn schema))


(defn get-all-files [base-dir pred?]
  (with-open [childs (files/walk (files/as-path base-dir))]
    (->> (stream/stream-seq childs)
         (filter pred?)
         (mapv (fn [p] (File. (str p)))))))

(def parse-solidity (-> (io/resource "grammars/Solidity.g4")
                        slurp
                        (antlr/parser)))

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

(defn using-for-declaration-facts [[_ & statements :as using]]
  (let [[utype uname] (parse-variable statements)
        id (d/tempid :db.part/user)]
    (into
     [[*current-contract-id* :contract/usings id]
      [id :using/type uname]
      [id :using/for-type utype]]
     (token-facts id using))))

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
      [id :var/public? (boolean public?)]]
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
    (-> [[*current-contract-id* :contract/structs sid]
         [sid :struct/name sname]]
        (into vars-facts)
        (into (token-facts sid struct)))))

(defn modifier-definition-facts [[_ & statements :as modifier]]
  (let [mname (second (some #(when (= (first %) :identifier) %) statements))
        mid (d/tempid :db.part/user)]
    (-> [[*current-contract-id* :contract/modifiers mid]
         [mid :modifier/name mname]]
        (into (token-facts mid modifier)))))

(defn function-definition-facts [[_ & statements :as function]]
  (let [fname (second (some #(when (= (first %) :identifier) %) statements))
        fid (d/tempid :db.part/user)
        plist (rest (some #(when (= (first %) :parameterList) %) statements))
        public? (some #(and (= (first %) :modifierList) (= (second %) "public")) statements)
        parameters-facts (->> plist
                              (keep (fn [[t & vs]]
                                      (when (= t :parameter)
                                        (let [[vtype vname] (parse-variable vs)
                                              vid (d/tempid :db.part/user)]
                                          [[fid :function/vars vid]
                                           [vid :var/type vtype]
                                           [vid :var/name (or vname "_UNNAMED_VAR")]
                                           [vid :var/parameter? true]]))))
                              (reduce concat))]
    (-> [[*current-contract-id* :contract/functions fid]
         [fid :function/name (or fname "_ANONYMOUS_FUNCTION")]
         [fid :function/public? (boolean public?)]]
        (into parameters-facts)
        (into (token-facts fid function)))))

(defn contract-event-definition-facts [[_ & statements :as event]]
  (let [ename (second (some #(when (= (first %) :identifier) %) statements))
        eid (d/tempid :db.part/user)
        plist (rest (some #(when (= (first %) :eventParameterList) %) statements))
        parameters-facts (->> plist
                              (keep (fn [[t & vs]]
                                      (when (= t :eventParameter)
                                        (let [[vtype vname] (parse-variable vs)
                                              vid (d/tempid :db.part/user)]
                                          [[eid :event/vars vid]
                                           [vid :var/type vtype]
                                           [vid :var/name (or vname "_UNNAMED_VAR")]]))))
                              (reduce concat))]
    (-> [[*current-contract-id* :contract/events eid]
         [eid :event/name ename]]
        (into parameters-facts)
        (into (token-facts eid event)))))

(defn contract-part-facts [[_ [part-type :as part]]]
  (case part-type
    :usingForDeclaration      (using-for-declaration-facts part)
    :stateVariableDeclaration (state-variable-declaration-facts part)
    :enumDefinition           (enum-definition-facts part)
    :structDefinition         (struct-definition-facts part)
    :modifierDefinition       (modifier-definition-facts part)
    :functionDefinition       (function-definition-facts part)
    :eventDefinition          (contract-event-definition-facts part)
    (do (println "WARNING unmanaged contract part " part-type)
        [])))

(defn contract-id-from-name [contract-name]
  (or
   (d/q '[:find ?cid .
          :in $ ?cname
          :where
          [?cid :contract/name ?cname]]
        @db-conn
        contract-name)
   (d/tempid :db.part/user)))

(defn file-id-from-name [file-name]
  (or
   (d/q '[:find ?fid .
          :in $ ?fname
          :where
          [?fid :file/name ?fname]]
        @db-conn
        file-name)
   (d/tempid :db.part/user)))


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
                   :pragmaDirective    (pragma-directive-facts child)
                   :importDirective    (import-directive-facts child)
                   :contractDefinition (contract-definition-facts child)
                   (do (when (not= (str node-id) "<")
                         (println "WARNING unmanaged source unit directive " node-id))
                       []))))))

(defn solidity-file-simple-facts [{:keys [relative-path full-path]}]
  (let [file-facts [[(d/tempid :db.part/user) :file/name relative-path]]
        [_ & statements] (parse-solidity (slurp full-path))
        contract-facts (->> statements
                            (keep (fn [[t & r]]
                                    (when (= t :contractDefinition)
                                      (let [cname (second (some #(when (= (first %) :identifier) %) r))]
                                        [(d/tempid :db.part/user) :contract/name cname])))))]
    (into contract-facts file-facts)))

(defn re-index-all
  ([] (re-index-all nil))
  ([folder]
   (d/reset-conn! db-conn (d/empty-db schema))
   
   (let [all-files (->> (get-all-files folder #(str/ends-with? (str %) ".sol"))
                        (map (fn [f]
                               (let [file-full-path (.toPath f)]
                                {:relative-path (.toString (.relativize (.toPath (File. folder)) file-full-path))
                                 :full-path (.toString file-full-path)}))))]
     
     ;; First pass, transact all files and contracts ids and names
     (doseq [fpath all-files]
       (let [facts (->> (solidity-file-simple-facts fpath)
                        (mapv (fn [[e a v]] [:db/add e a v])))]
         (prn "Transacting first pass for file" (:full-path fpath) "facts :" facts)
         (d/transact! db-conn facts)))

     ;; Second pass, transact every fact
     (doseq [{:keys [relative-path full-path]} all-files]
       (binding [*current-file-id* (file-id-from-name relative-path)]
         (let [facts (->> full-path
                          slurp
                          parse-solidity
                          source-unit-facts
                          (mapv (fn [[e a v]] [:db/add e a v])))]
           (prn "Transacting second pass for file" full-path "facts :" facts)
           (d/transact! db-conn facts)))))))

#_(def re-contract (slurp "/home/jmonetta/my-projects/district0x/memefactory/resources/public/contracts/src/Registry.sol"))
#_(def parsed (let [parsed-contract (parse-solidity re-contract)]
              parsed-contract))

#_(def file-path (.toPath (File. "/home/jmonetta/my-projects/district0x/memefactory/resources/public/contracts/src/db/EternalDb.sol")))
#_(def folder-path (.toPath (File. "/home/jmonetta/my-projects/district0x/memefactory/resources/public/contracts/src/")))
#_(.toString (.relativize folder-path file-path))
#_(solidity-file-facts "/home/jmonetta/my-projects/district0x/memefactory/resources/public/contracts/src/RegistryEntry.sol")

#_(binding [*current-project-id* "memefactory-id"
            *current-file-id* "RegistryEntry.sol-id"]
    (source-unit-facts parsed))

(defn db-edn []
  (pr-str @db-conn))

(comment

  ;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; All public functions ;;
  ;;;;;;;;;;;;;;;;;;;;;;;;;;

  (d/q '[:find ?file-name ?c-name ?fn-name
         :where
         [?file-id :file/name ?file-name]
         [?c-id :contract/name ?c-name]
         [?fn-id :function/name ?fn-name]
         [?file-id :file/contracts ?c-id]
         [?c-id :contract/functions ?fn-id]
         [?fn-id :function/public? true]]
       @db-conn)


  (re-index-all (.getAbsolutePath (io/file (io/resource "test-smart-contracts"))))

  
  (def contract "contract test {
        uint256 a;
        function f() {}
    }")

  )

