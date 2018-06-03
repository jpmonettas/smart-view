(ns smart-view.views
  (:require [cljsjs.material-ui]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [re-com.core :as re-com]
            [smart-view.subs :as subs]
            [smart-view.events :as events]
            [reagent-flowgraph.core :refer [flowgraph]]
            [clojure.string :as str]
            [cljs-react-material-ui.core :refer [get-mui-theme color]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]))  

(defn all-contracts [& {:keys [on-change selected-id]}]
  (let [all @(re-frame/subscribe [::subs/all-smart-contracts])]
    [:div
     [ui/select-field {:floating-label-text "Contracts"
                       :value selected-id
                       :on-change (fn [ev idx val] (on-change val))}
      (for [{:keys [:contract/name :db/id]} all]
        ^{:key id}
        [ui/menu-item {:value id
                       :primary-text name}])]]))

(defn explorer-one []
  (let [selected-smart-contract-id @(re-frame/subscribe [::subs/selected-smart-contract-id])
        tree (re-frame/subscribe [::subs/inheritance-tree])]
    [:div.inheritance-explorer {:style {:margin 10}}
     [all-contracts
      :on-change #(re-frame/dispatch [::events/select-smart-contract %])
      :selected-id selected-smart-contract-id]
     [:div.tree-panel 
      (when-let [t @tree]
        [flowgraph t
         :layout-width 1000
         :layout-height 10000
         :branch-fn :contract/inherits
         :childs-fn :contract/inherits
         :line-styles {:stroke-width 2
                       :stroke (color :pink500)}
         :render-fn (fn [n]
                      [:div.contract-node {}
                       [:div.contract-header (:contract/name n)]
                       [:div.contract-vars
                        [:ul
                         (for [{:keys [:var/type :var/name :var/public?]} (:contract/vars n)]
                           ^{:key name} [:li.var
                                         [:span.public (str public?)]
                                         [:span.type type] [:span.name name]])]]
                       [:div.contract-functions
                        (for [{:keys [:function/name :function/public?]} (:contract/functions n)]
                          ^{:key name} [:li.var
                                        [:span (str public?)]
                                        [:span name]
                                        [:ul]])]])])]]))

(defn explorer-two []
  [:div "Something great comming soon"])

(defn header []
  [ui/app-bar
   {:title "SmartView"
    :class-name "header"}])

(defn link [full-name path line]
  (let [full-name (str full-name)
        name-style {:font-weight :bold
                    :color (color :blue200)}]
   [:a {:href (str "/open-file?path=" path "&line=" line "#line-tag") :target "_blank"} 
    (if (str/index-of full-name "/")
      (let [[_ ns name] (str/split full-name #"(.+)/(.+)")]
        [:div {:style {:font-size 12}}
         [:span {:style {:color "#bbb"}}
          (str ns "/")]
         [:span.name {:style name-style} name]])
      [:div
       [:span.name {:style name-style} full-name]])]))


(defn tabs []
  (let [selected-tab @(re-frame/subscribe [::subs/selected-tab-id])]
    [ui/tabs {:value selected-tab} 
     [ui/tab {:label "One"
              :on-active #(re-frame/dispatch [::events/select-tab "tab-one"])
              :value "tab-one"}
      [explorer-one]]
     [ui/tab {:label "Two"
              :on-active #(re-frame/dispatch [::events/select-tab "tab-two"])
              :value "tab-two"}
      [explorer-two]]]))

(defn main-panel []
  [ui/mui-theme-provider
   {:mui-theme (get-mui-theme (aget js/MaterialUIStyles "lightBaseTheme"))}
   [:div.main-panel 
    [header]
    [tabs]]])
