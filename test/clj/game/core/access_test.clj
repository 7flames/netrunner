(ns game.core.access-test
  (:require [game.core :as core]
            [game.core-test :refer :all]
            [game.utils-test :refer :all]
            [game.macros-test :refer :all]
            [clojure.test :refer :all]))

(deftest rd-access
  (testing "Nothing in R&D, no upgrades"
    (do-game
      (new-game {:corp {:deck ["Hedge Fund"]
                        :hand [(qty "Hedge Fund" 5)]}})
      (core/click-draw state :corp nil)
      (take-credits state :corp)
      (run-empty-server state "R&D")
      (is (nil? (get-run)))
      (is (empty? (:prompt (get-runner))) "Runner has no access prompt")))
  (testing "Something in R&D, no upgrades"
    (do-game
      (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                        :hand ["Hedge Fund"]}})
      (take-credits state :corp)
      (run-empty-server state "R&D")
      (is (= ["No action"] (prompt-buttons :runner)))
      (click-prompt state :runner "No action")
      (is (nil? (get-run)))
      (is (empty? (:prompt (get-runner))) "Runner has no access prompt")))
  (testing "Nothing in R&D, an unrezzed upgrade"
    (do-game
      (new-game {:corp {:deck ["Hedge Fund"]
                        :hand [(qty "Hedge Fund" 4) "Bryan Stinson"]}})
      (core/click-draw state :corp nil)
      (play-from-hand state :corp "Bryan Stinson" "R&D")
      (take-credits state :corp)
      (run-empty-server state "R&D")
      (is (= ["Pay 5 [Credits] to trash" "No action"] (prompt-buttons :runner)))
      (click-prompt state :runner "No action")
      (is (nil? (get-run)))
      (is (empty? (:prompt (get-runner))) "Runner has no access prompt")))
  (testing "Something in R&D, an upgrade"
    (do-game
      (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                        :hand ["Hedge Fund" "Bryan Stinson"]}})
      (play-from-hand state :corp "Bryan Stinson" "R&D")
      (take-credits state :corp)
      (run-empty-server state "R&D")
      (is (= ["Card from deck" "Unrezzed upgrade"] (prompt-buttons :runner)))
      (click-prompt state :runner "Card from deck")
      (click-prompt state :runner "No action")
      (is (= ["Pay 5 [Credits] to trash" "No action"] (prompt-buttons :runner)))
      (click-prompt state :runner "No action")
      (is (nil? (get-run)))
      (is (empty? (:prompt (get-runner))) "Runner has no access prompt")))
  )

(deftest archives-access
  (testing "Nothing in archives"
    (do-game
      (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                        :hand ["Hedge Fund"]}})
      (take-credits state :corp)
      (run-empty-server state "Archives")
      (is (empty? (:prompt (get-runner))) "Runner has no access prompt")))
  (testing "only non-interactive cards"
    (do-game
      (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                        :hand ["Hedge Fund"]
                        :discard ["Hedge Fund" "Beanstalk Royalties"]}})
      (take-credits state :corp)
      (run-empty-server state "Archives")
      (is (empty? (:prompt (get-runner))) "Runner has no access prompt")))
  (testing "contains one agenda"
    (do-game
      (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                        :hand ["Hedge Fund"]
                        :discard ["Hostile Takeover"]}})
      (take-credits state :corp)
      (run-empty-server state "Archives")
      (is (= ["Steal"] (prompt-buttons :runner)))
      (click-prompt state :runner "Steal")
      (is (= 1 (:agenda-point (get-runner))))))
  (testing "contains multiple agendas"
    (do-game
      (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                        :hand ["Hedge Fund"]
                        :discard ["Hostile Takeover" "15 Minutes"]}})
      (take-credits state :corp)
      (run-empty-server state "Archives")
      (is (= ["Hostile Takeover" "15 Minutes"] (prompt-buttons :runner)))
      (click-prompt state :runner "Hostile Takeover")
      (click-prompt state :runner "Steal")
      (click-prompt state :runner "15 Minutes")
      (click-prompt state :runner "Steal")
      (is (nil? (get-run)))
      (is (empty? (:prompt (get-runner))))
      (is (= 2 (:agenda-point (get-runner))))))
  (testing "contains one access ability"
    (do-game
      (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                        :hand ["Hedge Fund"]
                        :discard ["Cyberdex Virus Suite"]}})
      (take-credits state :corp)
      (run-empty-server state "Archives")
      (is (prompt-is-type? state :runner :waiting))
      (click-prompt state :corp "Yes")))
  (testing "contains multiple access abilities"
    (do-game
      (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                        :hand ["Hedge Fund"]
                        :discard ["Cyberdex Virus Suite" "Shock!"]}})
      (take-credits state :corp)
      (run-empty-server state "Archives")
      (is (= ["Cyberdex Virus Suite" "Shock!"] (prompt-buttons :runner)))
      (click-prompt state :runner "Shock!")
      (click-prompt state :runner "Cyberdex Virus Suite")
      (is (prompt-is-type? state :runner :waiting))
      (click-prompt state :corp "Yes")))
  (testing "contains agendas and access abilities"
    (do-game
      (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                        :hand ["Hedge Fund"]
                        :discard ["Hostile Takeover" "Cyberdex Virus Suite"]}})
      (take-credits state :corp)
      (run-empty-server state "Archives")
      (is (= ["Hostile Takeover" "Cyberdex Virus Suite"] (prompt-buttons :runner)))
      (click-prompt state :runner "Cyberdex Virus Suite")
      (is (prompt-is-type? state :runner :waiting))
      (click-prompt state :corp "Yes")
      (is (= ["Hostile Takeover"] (prompt-buttons :runner)))
      (click-prompt state :runner "Hostile Takeover")
      (click-prompt state :runner "Steal")
      (is (= 1 (:agenda-point (get-runner))))))
  (testing "contains non-interactive cards, agendas, and access abilities"
    (do-game
      (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                        :hand ["Hedge Fund"]
                        :discard ["Hedge Fund" "Hostile Takeover" "Cyberdex Virus Suite"]}})
      (take-credits state :corp)
      (run-empty-server state "Archives")
      (is (= ["Hostile Takeover" "Cyberdex Virus Suite" "Everything else"] (prompt-buttons :runner)))
      (click-prompt state :runner "Cyberdex Virus Suite")
      (is (prompt-is-type? state :runner :waiting))
      (click-prompt state :corp "Yes")
      (is (= ["Hostile Takeover" "Everything else"] (prompt-buttons :runner)))
      (click-prompt state :runner "Hostile Takeover")
      (click-prompt state :runner "Steal")
      (is (= 1 (:agenda-point (get-runner))))
      (is (empty? (:prompt (get-corp))))
      (is (empty? (:prompt (get-runner))))
      (is (nil? (get-run)))))
  (testing "when access count is reduced"
    (testing "reduced by 1"
      (do-game
        (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                          :hand ["Bryan Stinson"]
                          :discard ["Hedge Fund" "Hostile Takeover"]}
                   :runner {:credits 10}})
        (play-from-hand state :corp "Bryan Stinson" "Archives")
        (core/rez state :corp (get-content state :archives 0))
        (take-credits state :corp)
        (run-empty-server state "Archives")
        (core/access-bonus state :corp :archives -1)
        (is (= ["Hostile Takeover" "Bryan Stinson" "Everything else"] (prompt-buttons :runner)))
        (click-prompt state :runner "Bryan Stinson")
        (click-prompt state :runner "No action")
        (is (= ["Hostile Takeover" "Everything else"] (prompt-buttons :runner)))
        (click-prompt state :runner "Everything else")
        (is (zero? (:agenda-point (get-runner))) "Runner doesn't access last card in Archives")
        (is (empty? (:prompt (get-corp))))
        (is (empty? (:prompt (get-runner))))
        (is (nil? (get-run)))))
    (testing "reduced by more than 1"
      (do-game
        (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                          :hand ["Bryan Stinson"]
                          :discard ["Hedge Fund" "Hostile Takeover" "Shock!"]}
                   :runner {:credits 10}})
        (play-from-hand state :corp "Bryan Stinson" "Archives")
        (core/rez state :corp (get-content state :archives 0))
        (take-credits state :corp)
        (run-empty-server state "Archives")
        (core/access-bonus state :corp :archives -2)
        (is (= ["Hostile Takeover" "Shock!" "Bryan Stinson" "Everything else"] (prompt-buttons :runner)))
        (click-prompt state :runner "Bryan Stinson")
        (click-prompt state :runner "No action")
        (is (= ["Hostile Takeover" "Shock!" "Everything else"] (prompt-buttons :runner)))
        (click-prompt state :runner "Everything else")
        (is (zero? (:agenda-point (get-runner))) "Runner didn't access Hostile Takeover")
        (is (zero? (count (:discard (get-runner)))) "Runner didn't access Shock!")
        (is (empty? (:prompt (get-corp))))
        (is (empty? (:prompt (get-runner))))
        (is (nil? (get-run))))))
  (testing "when access is limited to a single card, access only it"
    (do-game
      (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                        :hand ["Bryan Stinson"]
                        :discard ["Hostile Takeover" "Cyberdex Virus Suite"]}})
      (play-from-hand state :corp "Bryan Stinson" "Archives")
      (let [bryan (get-content state :archives 0)]
        (core/rez state :corp bryan)
        (take-credits state :corp)
        (run-on state "Archives")
        (core/set-only-card-to-access state :corp bryan))
      (run-continue state)
      (run-successful state)
      (is (= ["Pay 5 [Credits] to trash" "No action"] (prompt-buttons :runner)))
      (click-prompt state :runner "Pay 5 [Credits] to trash")
      (is (empty? (:prompt (get-corp))))
      (is (empty? (:prompt (get-runner))))
      (is (nil? (get-run)))))
  (testing "when a card is turned facedown mid-access"
    (do-game
      (new-game {:corp {:deck [(qty "Hedge Fund" 5)]
                        :hand ["Bryan Stinson"]
                        :discard ["Hostile Takeover" "Cyberdex Virus Suite" "Hedge Fund"]}})
      (play-from-hand state :corp "Bryan Stinson" "Archives")
      (core/rez state :corp (get-content state :archives 0))
      (take-credits state :corp)
      (run-on state "Archives")
      (run-continue state)
      (run-successful state)
      (is (= ["Hostile Takeover" "Cyberdex Virus Suite" "Bryan Stinson" "Everything else"] (prompt-buttons :runner)))
      (click-prompt state :runner "Hostile Takeover")
      (core/update! state :corp (dissoc (find-card "Hedge Fund" (:discard (get-corp))) :seen))
      (click-prompt state :runner "Steal")
      (is (= ["Cyberdex Virus Suite" "Bryan Stinson" "Facedown card in Archives"] (prompt-buttons :runner)))
      (click-prompt state :runner "Cyberdex Virus Suite")
      (click-prompt state :corp "No")
      (is (= ["Bryan Stinson" "Facedown card in Archives"] (prompt-buttons :runner)))
      (click-prompt state :runner "Facedown card in Archives")
      (is (last-log-contains? state "Runner accesses Hedge Fund from Archives."))
      (is (= ["Bryan Stinson"] (prompt-buttons :runner)))
      (click-prompt state :runner "Bryan Stinson")
      (is (= ["Pay 5 [Credits] to trash" "No action"] (prompt-buttons :runner)))
      (click-prompt state :runner "No action")
      (is (empty? (:prompt (get-corp))))
      (is (empty? (:prompt (get-runner))))
      (is (nil? (get-run))))))
