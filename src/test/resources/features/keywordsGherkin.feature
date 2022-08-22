Feature: Guess the word

  The word guess game is a turn-based game for two players.
  The Maker makes a word for the Breaker to guess. The game
  is over when the Breaker guesses the Maker's word.
  
   Background:
    Given a global administrator named "Greg"
    
      Scenario Outline: Many additions
    Given I'm adding 
    When I add <a> and <b>
    Then the result is <c>

    Examples: Single digits
      | a | b | c  |
      | 1 | 2 | 3  |
      | 2 | 3 | 5 |

    Examples: Double digits
      | a  | b  | c  |
      | 10 | 20 | 30 |
      | 20 | 30 | 50 |
	    
	