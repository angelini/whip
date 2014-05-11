(ns whip.plugins.plugin)

(defprotocol Plugin
  (translate [p key] "Translates a keystroke into a handler fn"))
