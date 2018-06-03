(ns smart-view.styles.core
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px]]))


(defstyles main
  [:div.contract-node
   {:border "1px solid black"
    :padding (px 10)
    :border-radius (px 10)
    :font-size (px 12)}])
