(ns hoplon.attrs
  (:require [hoplon.core :refer (on!)]))

;; TODO: when do we remove the elements?
(def observed-elements (atom {}))

(defn- observed-attributes [elem]
  (set (keys (get @observed-elements elem))))

(defn do-observe [mutation]
  (let [observed @observed-elements]
    (doseq [m mutation]
      (when-let [cb (->> [(.-target m) (.-attributeName m)]
                         (get-in observed))]
        (cb m)))))

(def observer (js/MutationObserver. do-observe))

(defn new-value
  "Returns the current value of the mutated attribute."
  [m]
  (aget (.-target m) (.-attributeName m)))

(defn old-value
  "Returns the old (serialized) value of the mutated attribute as a string."
  [m]
  (.-oldValue m))

(def default-observe-opts
  {:childList false
   :attributes true
   :characterData false
   :subtree false
   :attributeOldValue true
   :characterDataOldValue false
   :attributeFilter []})

(defmethod on! :ns/change
  [elem attr callback]
  ;; We can either
  ;;
  ;;   1) create a new MutationObserver for each new callback and call
  ;;   `.disconnect` when observer has to be stopped. We still need to track
  ;;   the `elem -> MutationObserver` mapping and merge observers for the same elem.
  ;;   2) store a map of `[element, event] -> callback` and have a global
  ;;   MutationObserver.
  ;;
  ;; Here we implement the 2nd case.
  (swap! observed-elements assoc-in [elem (name attr)] callback)
  (->> (conj (observed-attributes elem) (name attr))
       (assoc default-observe-opts :attributeFilter)
       (clj->js)
       (.observe observer elem)))
