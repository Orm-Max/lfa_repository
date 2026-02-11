import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FiniteAutomaton {
    Set<String> states;
    Set<String> alphabet;
    Map<String, Set<String>> transitions;
    String startState;
    Set<String> finalStates;

    public FiniteAutomaton(Set<String> states, Set<String> alphabet,
                           Map<String, Set<String>> transitions,
                           String startState, Set<String> finalStates) {
        this.states = states;
        this.alphabet = alphabet;
        this.transitions = transitions;
        this.startState = startState;
        this.finalStates = finalStates;
    }

    // Check if a string belongs to the language
    public boolean checkString(String input) {

        Set<String> currentStates = new HashSet<>();
        currentStates.add(startState);

        // Read input symbol by symbol
        for (char ch : input.toCharArray()) {

            Set<String> nextStates = new HashSet<>();

            for (String state : currentStates) {
                String key = state + "," + ch;

                if (transitions.containsKey(key)) {
                    nextStates.addAll(transitions.get(key));
                }
            }

            currentStates = nextStates;

            if (currentStates.isEmpty())
                return false;
        }

        // Check if we ended in a final state
        for (String state : currentStates) {
            if (finalStates.contains(state))
                return true;
        }

        return false;
    }
}
