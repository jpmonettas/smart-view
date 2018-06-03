(ns smart-view.styles.core
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px]]))

(defstyles main
  [:div.contract-node
   {:border "1px solid black"
    :padding (px 10)
    :border-radius (px 10)
    :font-size (px 12)}
   [:ol {:list-style-type :none
         :display :inline-block
         :padding-left 0}
    [:li {:display :inline}]]

   [:span.type {:font-style :italic}]
   [:ul {:padding-left (px 4)}]
   [:.sub-title {:color "#aaa"}]
   [:.public {:color :green
              :font-size (px 12)}]
   [:.private  {:color :red
                :font-size (px 12)}]
   
   [:.contract-header
    {:font-weight :bold
     :font-size (px 13)
     :margin-bottom (px 5)}]

   [:.contract-vars
    {:border-top "1px solid #ccc"
     :border-bottom "1px solid #ccc"
     :padding-top (px 10)}
    [:ul
     {:list-style-type :none}
     [:li
      [:span.name {:margin-left (px 5)}]
      [:span.type {:margin-left (px 5)}]]]]

   [:.contract-functions
    {:margin-top (px 10)
     :border-bottom "1px solid #ccc"}

    [:ul
     {:list-style-type :none}
     [:li
      [:span {:margin-left (px 5)}]]]]

   [:.contract-events
    {:margin-top (px 10)}
    [:ul {:list-style-type :none}
     [:li
      [:span.name {:margin-left (px 5)}]
      [:span.type {:margin-left (px 5)}]]]]])
