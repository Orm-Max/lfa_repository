import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Grammar {
    Set<String> nonTerminals;   // S, A, B, C ...
    Set<String> terminals;      // a, b, c, d ...
    Map<String, List<String>> rules;  // Production rules
    String startSymbol;         // Start symbol (S)
    Random random = new Random();

    public Grammar(Set<String> nonTerminals, Set<String> terminals,
                   Map<String, List<String>> rules, String startSymbol) {
        this.nonTerminals = nonTerminals;
        this.terminals = terminals;
        this.rules = rules;
        this.startSymbol = startSymbol;
    }

    // Generate a string from the grammar
    public String generateString(int maxSteps) {
        String current = startSymbol;
        int steps = 0;

        // Try to replace non-terminals with rules
        while (steps < maxSteps) {

            String foundNonTerminal = null;

            // Find first non-terminal in the string
            for (char ch : current.toCharArray()) {
                String symbol = String.valueOf(ch);
                if (nonTerminals.contains(symbol)) {
                    foundNonTerminal = symbol;
                    break;
                }
            }

            // If no non-terminals left then finished
            if (foundNonTerminal == null) {
                return current;
            }

            // Choose random rule for this non-terminal
            List<String> possibleRules = rules.get(foundNonTerminal);

            if (possibleRules == null || possibleRules.isEmpty())
                break;

            String chosenRule = possibleRules.get(random.nextInt(possibleRules.size()));

            // Replace only first occurrence
            current = current.replaceFirst(foundNonTerminal, chosenRule);

            steps++;
        }

        return current;
    }

    // Convert Grammar to Finite Automaton
    public FiniteAutomaton toFiniteAutomaton() {

        Set<String> states = new HashSet<>(nonTerminals);
        states.add("F"); // Final state

        Set<String> alphabet = new HashSet<>(terminals);

        Map<String, Set<String>> transitions = new HashMap<>();

        // Go through all rules
        for (String left : rules.keySet()) {
            for (String right : rules.get(left)) {

                // Case 1: A → a   (terminal only)
                if (right.length() == 1 && terminals.contains(right)) {
                    String key = left + "," + right;
                    transitions.putIfAbsent(key, new HashSet<>());
                    transitions.get(key).add("F");
                }

                // Case 2: A → aB
                else if (right.length() == 2) {
                    String terminal = String.valueOf(right.charAt(0));
                    String nextState = String.valueOf(right.charAt(1));

                    if (terminals.contains(terminal) && nonTerminals.contains(nextState)) {
                        String key = left + "," + terminal;
                        transitions.putIfAbsent(key, new HashSet<>());
                        transitions.get(key).add(nextState);
                    }
                }
            }
        }
        return new FiniteAutomaton(states, alphabet, transitions, startSymbol, Set.of("F"));
    }
}
