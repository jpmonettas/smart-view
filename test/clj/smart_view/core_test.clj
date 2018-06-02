(ns smart-view.core-test
  (:require [smart-view.core :refer :all]
            [clojure.test :as t :refer [deftest is use-fixtures]]))

(use-fixtures :each (fn [f]
                      (reset! @#'datascript.core/last-tempid -1000000)
                      (f)))

(def temp-id-1 -1000001)
(def temp-id-2 -1000002)
(def temp-id-3 -1000003)

(deftest import-directive-facts-test
  (binding [sut/*current-file-id* -1]
    (is (= (import-directive-facts (with-meta 
                                     '(:importDirective "import" "\"math/SafeMath.sol\"" ";")
                                     {:clj-antlr/position {:row 1 :column 1}}))
           [[-1 :file/imports temp-id-1]
            [temp-id-1 :import/name "math/SafeMath.sol"]
            [temp-id-1 :token/row 1]
            [temp-id-1 :token/column 1]]))))

(deftest pragma-directive-facts-test
 (binding [sut/*current-file-id* -1]
   (is (= (pragma-directive-facts '(:pragmaDirective
                                    "pragma"
                                    (:pragmaName (:identifier "solidity"))
                                    (:pragmaValue
                                     (:version (:versionConstraint (:versionOperator "^") "0.4.18")))
                                    ";"))
          [[-1 :file/pragma-version "^0.4.18"]]))))

(deftest state-variable-declaration-facts-test
  (binding [*current-contract-id* -1]
    (is (= (state-variable-declaration-facts (with-meta
                                               '(:stateVariableDeclaration
                                                 (:typeName (:userDefinedTypeName (:identifier "Registry")))
                                                 "public"
                                                 "constant"
                                                 (:identifier "registry")
                                                 ";")
                                               {:clj-antlr/position {:row 1 :column 1}}))
           [[-1 :contract/vars temp-id-1]
            [temp-id-1 :var/type "Registry"]
            [temp-id-1 :var/name "registry"]
            [temp-id-1 :var/public? true]
            [temp-id-1 :token/row 1]
            [temp-id-1 :token/column 1]]))
    
    (is (= (state-variable-declaration-facts (with-meta
                                               '(:stateVariableDeclaration
                                                 (:typeName (:elementaryTypeName "bytes32"))
                                                 "public"
                                                 "constant"
                                                 (:identifier "commitPeriodDurationKey")
                                                 ";")
                                               {:clj-antlr/position {:row 1 :column 1}}))
           [[-1 :contract/vars temp-id-2]
            [temp-id-2 :var/type "bytes32"]
            [temp-id-2 :var/name "commitPeriodDurationKey"]
            [temp-id-2 :var/public? true]
            [temp-id-2 :token/row 1]
            [temp-id-2 :token/column 1]]
           ))))

