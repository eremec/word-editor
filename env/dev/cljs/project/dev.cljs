(ns ^:figwheel-no-load project.dev
  (:require
    [project.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
