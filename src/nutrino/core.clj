(ns nutrino.core
  (:require [clojure.set :as set]
            [clojure.core.unify :as unify]))

(def try-subst (unify/make-occurs-subst-fn unify/lvar?))

(defn fresh
  ([] (fresh "type"))
  ([seed]
   (symbol (str "?" seed "-" (rand-int 1000000000)))))

(defn typeof [expr]
  (if (contains? (meta expr) :type)
    (-> expr meta :type)
    ;; else
    (class expr)))

(declare infer-types)

(defn- infer-list-types [expr types bindings]
  (loop [in expr
         out []
         bindings bindings]
    (if (empty? in)
      [(seq out) bindings]
      ;; else
      (let [[annot bindings] (infer-types (first in) types bindings)]
        (recur (rest in) (conj out annot) bindings)))))

(defn infer-types [expr types bindings]
  (if (instance? clojure.lang.IObj expr)
    (let [[expr bindings]
          (cond
            (seq? expr) (infer-list-types expr types bindings)
            :else [expr bindings])
          [type bindings]
          (cond
            (symbol? expr) [(types expr) bindings]
            (seq? expr) (let [functype (concat ['->] (map typeof (rest expr)) [(fresh)])
                              bindings (-> (unify/unify functype (typeof (first expr)))
                                           (unify/flatten-bindings))]
                          [(try-subst (last functype) bindings) bindings]))]
      [(with-meta expr (-> (meta expr)
                           (merge {:type type}))) bindings])
    ;; else
    [expr bindings]))
