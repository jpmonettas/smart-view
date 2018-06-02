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


(deftest enum-definition-facts-test 
 (binding [*current-contract-id* -1]
   (is (= (enum-definition-facts (with-meta
                                   '(:enumDefinition
                                     "enum"
                                     (:identifier "VoteOption")
                                     "{"
                                     (:enumValue (:identifier "NoVote"))
                                     ","
                                     (:enumValue (:identifier "VoteFor"))
                                     ","
                                     (:enumValue (:identifier "VoteAgainst"))
                                     "}")
                                   {:clj-antlr/position {:row 1 :column 1}}))
          [[-1 :contract/enums temp-id-1]
           [temp-id-1 :enum/name "VoteOption"]
           [temp-id-1 :enum/values "NoVote"]
           [temp-id-1 :enum/values "VoteFor"]
           [temp-id-1 :enum/values "VoteAgainst"]
           [temp-id-1 :token/row 1]
           [temp-id-1 :token/column 1]]))))

(deftest struct-definition-facts-test
  (binding [*current-contract-id* -1]
    (is (= (struct-definition-facts (with-meta
                                      '(:structDefinition
                                        "struct"
                                        (:identifier "Vote")
                                        "{"
                                        (:variableDeclaration
                                         (:typeName (:elementaryTypeName "bytes32"))
                                         (:identifier "secretHash"))
                                        ";"
                                        (:variableDeclaration
                                         (:typeName (:userDefinedTypeName (:identifier "VoteOption")))
                                         (:identifier "option"))
                                        ";"
                                        "}")
                                      {:clj-antlr/position {:row 1 :column 1}}))
           [[-1 :contract/structs temp-id-1]
            [temp-id-1 :struct/name "Vote"]
            [temp-id-1 :struct/vars temp-id-2]
            [temp-id-2 :var/type "bytes32"]
            [temp-id-2 :var/name "secretHash"]
            [temp-id-1 :struct/vars temp-id-3]
            [temp-id-3 :var/type "VoteOption"]
            [temp-id-3 :var/name "option"]
            [temp-id-1 :token/row 1]
            [temp-id-1 :token/column 1]]))))


(deftest modifier-definition-facts-test
  (binding [*current-contract-id* -1]
    (is (= (modifier-definition-facts
            (with-meta '(:modifierDefinition
                         "modifier"
                         (:identifier "notEmergency")
                         (:parameterList "(" ")")
                         )
              {:clj-antlr/position {:row 1 :column 1}}))
           [[-1 :contract/modifiers temp-id-1]
            [temp-id-1 :modifier/name "notEmergency"]
            [temp-id-1 :token/row 1]
            [temp-id-1 :token/column 1]]))))

(deftest function-definition-facts-test
 (binding [*current-contract-id* -1]
   (is (= (function-definition-facts
           (with-meta
             '(:functionDefinition
               "function"
               (:identifier "construct")
               (:parameterList
                "("
                (:parameter
                 (:typeName (:elementaryTypeName "address"))
                 (:identifier "_creator"))
                ","
                (:parameter
                 (:typeName (:elementaryTypeName "uint"))
                 (:identifier "_version"))
                ")")
               (:modifierList "public"))
             {:clj-antlr/position {:row 1 :column 1}}))
          
          [[-1 :contract/functions temp-id-1]
           [temp-id-1 :function/name "construct"]
           [temp-id-1 :function/public? true]
           [temp-id-1 :function/vars temp-id-2]
           [temp-id-2 :var/type "address"]
           [temp-id-2 :var/name "_creator"]
           [temp-id-2 :var/parameter? true]
           [temp-id-1 :function/vars temp-id-3]
           [temp-id-3 :var/type "uint"]
           [temp-id-3 :var/name "_version"]
           [temp-id-3 :var/parameter? true]
           [temp-id-1 :token/row 1]
           [temp-id-1 :token/column 1]]))))


(deftest using-for-declaration-facts-test
  (binding [*current-contract-id* -1]
    (is (= (using-for-declaration-facts
            (with-meta
              '(:usingForDeclaration
                "using"
                (:identifier "SafeMath")
                "for"
                (:typeName (:elementaryTypeName "uint"))
                ";")
              {:clj-antlr/position {:row 1 :column 1}}))

           [[-1 :contract/using temp-id-1]
            [temp-id-1 :using/type "SafeMath"]
            [temp-id-1 :using/for-type "uint"]
            [temp-id-1 :token/row 1]
            [temp-id-1 :token/column 1]]))))
