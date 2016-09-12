(ns seshat.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [seshat.core-test]))

(doo-tests 'seshat.core-test)
