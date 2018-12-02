(ns nutrino.core-test
  (:require [midje.sweet :refer :all]
            [nutrino.core :refer :all]))

;; # Type Inference

;; The function `infer-types` takes an expression, a map from symbols
;; to their types and a map of type bindings (of type variables), and
;; returns a tuple `[expr bindings]` where `expr` is the original
;; expression, annotated with types, and `bindings` is the updated map
;; of type bindings.

;; Literal types are returned unchanged, since they cannot be given
;; meta fields.
(fact
 (infer-types 123 {} '{?foo bar}) => [123 '{?foo bar}])

;; A symbol is annotated with the type associated with it in the types
;; map.
(fact
 (let [[annotated bindings] (infer-types 'foo '{foo footype
                                                bar bartype} {})]
   annotated => 'foo
   (-> annotated meta :type) => 'footype))

;; `infer-types` preserves all other meta-fields.
(fact
 (let [[annotated bindings] (infer-types (with-meta 'foo {:x 3}) '{foo footype
                                                                   bar bartype} {})]
   (-> annotated meta :x) => 3))

;; A function application is handled as follows:
;; 1. All members of the sequence (the function and its arguments) go through `infer-types`. The bindings map is chained between them.
;; 2. A type signature is built for the function based on the arguments' inferred types. A fresh type variable is allocated for the return value.
;; 3. The type signature is unified with the function's (first element's) type signature.
(fact
 (let [[inferred bindings] (infer-types '(+ a b) '{+ (-> ?a ?a ?a)
                                                   a int32
                                                   b int32} {})]
   (typeof inferred) => 'int32
   (bindings '?a) => 'int32))

;; ## Helpers

;; The helper function `typeof` takes the output of `infer-types` and
;; returns the type. If there is a `:type` meta-field, it returns its
;; value.
(fact
 (typeof (with-meta 'foo {:type 'footype})) => 'footype)

;; If a variable has no `:type` meta-field, its class is returned.
(fact
 (typeof 3) => java.lang.Long)

;; The `fresh` function takes an optional seed name, and creates a
;; symbol of the form `?<seed>-<unique>`, where `<unique>` is a large
;; random decimal number.
(fact
 (fresh "foo") => '?foo-12345
 (provided
  (rand-int 1000000000) => 12345))

;; If no seed is given, the word `type` is used instead.
(fact
 (fresh) => '?type-12345
 (provided
  (rand-int 1000000000) => 12345))
