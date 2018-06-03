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
            [cljs-react-material-ui.icons :as ic]
            [reagent.core :as reagent]
            [goog.events]
            [goog.events.EventType]))  

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

(defn link [{:keys [path line]} child]
  [:a {:href (str "/open-file?path=" path "&line=" line "#line-tag") :target "_blank"} 
   child])

(defn render-contract-node [n]
  (if (= (:contract/name n) "_ROOT")
    [:div {:dangerouslySetInnerHTML {:__html "&#x25FC;"}}]
    (let [file-full-path (-> n :file/_contracts first :file/full-path)]
     [:div.contract-node {}
      (link {:path file-full-path
             :line (inc (:token/row n))}
       [:span.contract-header (str (:contract/name n) " @ " (-> n :file/_contracts first :file/name))])
      [:div.contract-vars
       [:div.sub-title "Vars"]
       [:ul
        (for [{:keys [:var/type :var/name :var/public?] :as var} (:contract/vars n)]
          ^{:key name} [:li
                        [:span {:dangerouslySetInnerHTML {:__html "&#x25FC;"}
                                :class (if public? "public" "private")}]
                        [:span.type type] (link {:path file-full-path
                                                 :line (inc (:token/row var))}
                                                [:span.name name])])]]
      [:div.contract-functions
       [:div.sub-title "Functions"]
       [:ul
        (for [{:keys [:function/name :function/public? :function/vars] :as function} (:contract/functions n)]
          ^{:key name} [:li
                        [:span.public {:dangerouslySetInnerHTML {:__html "&#x25FC;"}
                                       :class (if public? "public" "private")}]
                        [:span.function "function"] (link {:path file-full-path
                                                           :line (inc (:token/row function))}
                                                          [:span.name name])
                        [:ol.parameters (for [{:keys [:var/name :var/type :var/parameter?]} vars]
                                          (when parameter?
                                            [:li [:span.type type] [:span.name name]]))]])]]
      [:div.contract-events
       [:div.sub-title "Events"]
       [:ul
        (for [{:keys [:event/name :event/vars]} (:contract/events n)]
          ^{:key name} [:li
                        [:span.name name]
                        [:ol.parameters (for [{:keys [:var/name :var/type]} vars]
                                          [:li [:span.type type] [:span.name name]])]])]]])))

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
         :render-fn render-contract-node])]]))

(defn contracts-map []
  (reagent/create-class
   {:component-did-mount (fn [c]
                           #_(.listen goog.events (reagent/dom-node c) goog.events.EventType/WHEEL
                                    (fn [e] (let [down? (-> e .-event_ .-deltaY pos?)]
                                              (re-frame/dispatch (if down? [:zoom-out] [:zoom-in]))))))
    :reagent-render
    (fn []
      (let [tree (re-frame/subscribe [::subs/all-smart-contracts-tree])
            zoom (re-frame/subscribe [:zoom])]
        [:div.smart-contract-explorer {:style {:margin 10}}
         [:div {:style {:overflow :scroll
                        :height 700 
                        :width "100%"}} 
          [:div.tree-panel #_{:style {:transform (str "scale(" @zoom ")")}}
           (when-let [t @tree]
             [flowgraph t
              :layout-width 10000
              :layout-height 10000
              :branch-fn :contract/_inherits
              :childs-fn :contract/_inherits
              :line-styles {:stroke-width 2
                            :stroke (color :pink500)}
              :render-fn render-contract-node])]]]))}))

(defn header []
  [ui/app-bar
   {:title "SmartView"
    :class-name "header"}])

(defn tabs []
  (let [selected-tab @(re-frame/subscribe [::subs/selected-tab-id])]
    [ui/tabs {:value selected-tab} 
     [ui/tab {:label "Contracts Map"
              :on-active #(re-frame/dispatch [::events/select-tab "tab-contracts-map"])
              :value "tab-contracts-map"}
      [contracts-map]]
     [ui/tab {:label "Single contract"
              :on-active #(re-frame/dispatch [::events/select-tab "tab-single-contract"])
              :value "tab-single-contract"}
      [explorer-one]]]))

(defn main-panel []
  [ui/mui-theme-provider
   {:mui-theme (get-mui-theme (aget js/MaterialUIStyles "lightBaseTheme"))}
   [:div.main-panel 
    [header]
    [tabs]]])
