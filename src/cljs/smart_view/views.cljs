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

(defn all-projects [& {:keys [on-change selected-id]}]
  (let [all @(re-frame/subscribe [::subs/all-projects])]
    [:div
     [ui/select-field {:floating-label-text "Projects"
                       :value selected-id
                       :on-change (fn [ev idx val] (on-change val))}
      (for [{:keys [:project/name :db/id]} all]
        ^{:key id}
        [ui/menu-item {:value id
                       :primary-text name}])]]))

(defn dependency-explorer []
  (let [tree (re-frame/subscribe [::subs/dependency-tree])
        selected-project-id @(re-frame/subscribe [::subs/selected-project-id])]
    [:div.dependency-explorer {:style {:margin 10}}
     [all-projects
      :on-change #(re-frame/dispatch [::events/select-project %])
      :selected-id selected-project-id]
     [:div.tree-panel 
      (when-let [t @tree]
        [flowgraph t
         :layout-width 10500
         :layout-height 500
         :branch-fn :project/dependency
         :childs-fn :project/dependency
         :line-styles {:stroke-width 2
                       :stroke (color :pink500)}
         :render-fn (fn [n]
                      [:div.node {}
                       (if (str/includes? (:project/name n) "/")
                         (let [[ns name] (str/split (:project/name n) #"/")]
                           [:div [:span {:style {:color "#999"}}(str ns "/")] [:b name]])
                         [:b (:project/name n)])])])]]))

(defn header []
  [ui/app-bar
   {:title "Explorer"
    :class-name "header"
    :icon-element-right (r/as-element
                         [ui/raised-button
                          {:secondary true
                           :label "Refresh index"
                           :on-click #(re-frame/dispatch [::events/re-index-all])}])}])

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

(defn feature-explorer [type]
  (let [features @(re-frame/subscribe [::subs/features type])] 
    [ui/grid-list {:cols 3
                   :padding 20
                   :style {:margin 20}
                   :cell-height "auto"}
     (for [[p namespaces-map] features]
       ^{:key p}
       [ui/paper {:style {:padding 10
                          :height "100%"}}
        [:h4 {:style {:color (color :blueGrey600)}} (str p)]
        [:div
         (for [[n feats] namespaces-map]
           ^{:key n} 
           [:div 
            [:h6 {:style {:color (color :blueGrey500)}} (str n)]
            [:div 
             (for [f feats]
               ^{:key (str f)}
               [link (:feature/name f) (:namespace/path f) (:feature/line f)])]])]])]))

(defn smart-contracts-explorer []
  (let [smart-contracts @(re-frame/subscribe [::subs/smart-contracts])]
    [ui/grid-list {:cols 1 :cell-height "auto"}
     (for [[pname contracts] smart-contracts]
       ^{:key pname}
       [ui/paper {:style {:padding 30 :margin 10}}
        [:h4 {:style {:color (color :blueGrey600)}} pname]
        [ui/grid-list {:cols 6 :cell-height "auto" :padding 10}
         (for [{:keys [:smart-contract/path]} contracts]
           ^{:key path}
           [:div {}
            [link (subs path (inc (str/last-index-of path "/"))) path 0]])]])]))

(defn specs-explorer []
  (let [all-specs @(re-frame/subscribe [::subs/specs type])] 
    [ui/grid-list {:cols 3
                   :padding 20
                   :style {:margin 20}
                   :cell-height "auto"}
     (for [[p namespaces-map] all-specs]
       ^{:key p}
       [ui/paper {:style {:padding 10
                          :height "100%"}} 
        [:h4 {:style {:color (color :blueGrey600)}} (str p)]
        [:div
         (for [[n specs] namespaces-map]
           ^{:key n} 
           [:div 
            [:h6 {:style {:color (color :blueGrey500)}} (str n)]
            [:div 
             (for [s specs]
               ^{:key (str s)}
               [link (:spec/name s) (:namespace/path s) (:spec/line s)])]])]])]))

(defn tabs []
  (let [selected-tab @(re-frame/subscribe [::subs/selected-tab-id])]
    [ui/tabs {:value selected-tab} 
    [ui/tab {:label "Dependencies"
             :on-active #(re-frame/dispatch [::events/select-tab "tab-dependencies"])
             :value "tab-dependencies"}
     [dependency-explorer]]
    [ui/tab {:label "Events"
             :on-active #(re-frame/dispatch [::events/select-tab "tab-events"])
             :value "tab-events"}
     [feature-explorer :event]]
    [ui/tab {:label "Subscriptions" 
             :on-active #(re-frame/dispatch [::events/select-tab "tab-subscriptions"])
             :value "tab-subscriptions"}
     [feature-explorer :subscription]]
    [ui/tab {:label "Effects"
             :on-active #(re-frame/dispatch [::events/select-tab "tab-effects"])
             :value "tab-effects"} 
     [feature-explorer :fx]]
    [ui/tab {:label "Coeffects"
             :on-active #(re-frame/dispatch [::events/select-tab "tab-coeffects"])
             :value "tab-coeffects"}
     [feature-explorer :cofx]]
    [ui/tab {:label "Specs"
             :on-active #(re-frame/dispatch [::events/select-tab "tab-specs"])
             :value "tab-specs"}
     [specs-explorer]]
    [ui/tab {:label "Smart Contracts"
             :on-active #(re-frame/dispatch [::events/select-tab "tab-smart"])
             :value "tab-smart"}
     [smart-contracts-explorer]]]))

(defn main-panel []
  [ui/mui-theme-provider
   {:mui-theme (get-mui-theme (aget js/MaterialUIStyles "lightBaseTheme"))}
   [:div.main-panel 
    [header]
    [tabs]]])
