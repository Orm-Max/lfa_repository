import java.util.*;


// Grammar Class
class Grammar {
    Set<String> VN;
    Set<String> VT;
    Map<String, List<String>> productions;
    String start;

    public Grammar(Set<String> VN, Set<String> VT, Map<String, List<String>> productions, String start) {
        this.VN = new HashSet<>(VN);
        this.VT = new HashSet<>(VT);
        this.productions = productions;
        this.start = start;
    }

    // Split a string into grammar symbols (non-terminals or single-char terminals)
    private List<String> tokenize(String string) {
        List<String> tokens = new ArrayList<>();
        List<String> sortedVN = new ArrayList<>(VN);
        sortedVN.sort((a, b) -> b.length() - a.length()); // greedy longest match

        int i = 0;
        while (i < string.length()) {
            boolean matched = false;
            for (String nt : sortedVN) {
                if (string.startsWith(nt, i)) {
                    tokens.add(nt);
                    i += nt.length();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                tokens.add(String.valueOf(string.charAt(i)));
                i++;
            }
        }
        return tokens;
    }

    private boolean isNonTerminal(String sym) {
        return VN.contains(sym);
    }

    
    // Type 3 - Regular Grammar
    private Object[] isType3() {
        boolean rightLinear = true;
        boolean leftLinear  = true;

        for (Map.Entry<String, List<String>> entry : productions.entrySet()) {
            String lhs = entry.getKey();
            if (!VN.contains(lhs)) return new Object[]{false, null};

            for (String rhs : entry.getValue()) {
                if (rhs.equals("ε") || rhs.isEmpty()) continue;

                List<String> tokens = tokenize(rhs);
                List<Integer> ntPositions = new ArrayList<>();
                for (int i = 0; i < tokens.size(); i++) {
                    if (isNonTerminal(tokens.get(i))) ntPositions.add(i);
                }

                if (!ntPositions.isEmpty()) {
                    // Right-linear: exactly one NT at the end
                    if (ntPositions.size() != 1 || ntPositions.get(0) != tokens.size() - 1)
                        rightLinear = false;
                    for (int j = 0; j < ntPositions.get(0); j++)
                        if (isNonTerminal(tokens.get(j))) rightLinear = false;

                    // Left-linear: exactly one NT at the start
                    if (ntPositions.size() != 1 || ntPositions.get(0) != 0)
                        leftLinear = false;
                    for (int j = 1; j < tokens.size(); j++)
                        if (isNonTerminal(tokens.get(j))) leftLinear = false;
                }
            }
        }

        if (rightLinear && leftLinear) return new Object[]{true, "Type 3 (Regular - right-linear and left-linear)"};
        if (rightLinear)               return new Object[]{true, "Type 3 (Regular - right-linear)"};
        if (leftLinear)                return new Object[]{true, "Type 3 (Regular - left-linear)"};
        return new Object[]{false, null};
    }

    // Type 2 - Context-Free Grammar
    private boolean isType2() {
        for (String lhs : productions.keySet()) {
            if (!VN.contains(lhs)) return false;
        }
        return true;
    }

    // Type 1 - Context-Sensitive Grammar
    private boolean isType1() {
        for (Map.Entry<String, List<String>> entry : productions.entrySet()) {
            String lhs = entry.getKey();
            for (String rhs : entry.getValue()) {
                if ((rhs.equals("ε") || rhs.isEmpty()) && lhs.equals(start)) continue;
                if (rhs.length() < lhs.length()) return false;
            }
        }
        return true;
    }

    // Public: classify()
    public String classify() {
        Object[] result = isType3();
        if ((boolean) result[0]) return (String) result[1];
        if (isType2()) return "Type 2 (Context-Free)";
        if (isType1()) return "Type 1 (Context-Sensitive)";
        return "Type 0 (Unrestricted)";
    }
}


// FiniteAutomaton Class
class FiniteAutomaton {
    Set<String> states;
    Set<String> alphabet;
    Map<String, Map<String, Set<String>>> transMap; // state -> symbol -> next states
    String startState;
    Set<String> finalStates;

    public FiniteAutomaton(List<String> states, List<String> alphabet,
                           Map<String, Map<String, Set<String>>> transMap,
                           String startState, List<String> finalStates) {
        this.states      = new HashSet<>(states);
        this.alphabet    = new HashSet<>(alphabet);
        this.transMap    = transMap;
        this.startState  = startState;
        this.finalStates = new HashSet<>(finalStates);
    }

    public boolean isDeterministic() {
        for (Map<String, Set<String>> bySymbol : transMap.values()) {
            for (Set<String> nextStates : bySymbol.values()) {
                if (nextStates.size() > 1) return false;
            }
        }
        return true;
    }

    // Returns Object[]{dfaStates, dfaTransitions, dfaStart, dfaFinals}
    public Object[] toDFA() {
        List<Set<String>> dfaStates = new ArrayList<>();
        Map<String, Map<String, Set<String>>> dfaTransitions = new LinkedHashMap<>();
        Queue<Set<String>> queue = new LinkedList<>();

        Set<String> startSet = new HashSet<>(Collections.singletonList(startState));
        queue.add(startSet);
        dfaStates.add(startSet);

        while (!queue.isEmpty()) {
            Set<String> current = queue.poll();
            String currentKey = stateSetKey(current);
            dfaTransitions.putIfAbsent(currentKey, new LinkedHashMap<>());

            for (String symbol : alphabet) {
                Set<String> nextSet = new HashSet<>();
                for (String state : current) {
                    if (transMap.containsKey(state) && transMap.get(state).containsKey(symbol)) {
                        nextSet.addAll(transMap.get(state).get(symbol));
                    }
                }
                dfaTransitions.get(currentKey).put(symbol, nextSet);
                final Set<String> nextFinal = nextSet;
                boolean alreadyExists = dfaStates.stream().anyMatch(s -> s.equals(nextFinal));
                if (!alreadyExists) {
                    dfaStates.add(nextSet);
                    queue.add(nextSet);
                }
            }
        }

        List<Set<String>> dfaFinals = new ArrayList<>();
        for (Set<String> s : dfaStates) {
            for (String q : s) {
                if (finalStates.contains(q)) { dfaFinals.add(s); break; }
            }
        }

        return new Object[]{dfaStates, dfaTransitions, startSet, dfaFinals};
    }

    private String stateSetKey(Set<String> set) {
        List<String> sorted = new ArrayList<>(set);
        Collections.sort(sorted);
        return sorted.toString();
    }

    public Grammar toRegularGrammar() {
        Map<String, List<String>> productions = new HashMap<>();

        for (Map.Entry<String, Map<String, Set<String>>> outer : transMap.entrySet()) {
            String state = outer.getKey();
            for (Map.Entry<String, Set<String>> inner : outer.getValue().entrySet()) {
                String symbol = inner.getKey();
                for (String nextState : inner.getValue()) {
                    productions.computeIfAbsent(state, k -> new ArrayList<>())
                               .add(symbol + nextState);
                    if (finalStates.contains(nextState)) {
                        productions.get(state).add(symbol);
                    }
                }
            }
        }

        return new Grammar(states, alphabet, productions, startState);
    }
}

// Main Class
public class Main {

    private static void addTransition(Map<String, Map<String, Set<String>>> map,
                                      String state, String symbol, Set<String> nextStates) {
        map.computeIfAbsent(state, k -> new LinkedHashMap<>()).put(symbol, nextStates);
    }

    public static void main(String[] args) {

        // Automaton definition (Variant 17)
        List<String> states      = Arrays.asList("q0", "q1", "q2", "q3");
        List<String> alphabet    = Arrays.asList("a", "b", "c");
        String startState        = "q0";
        List<String> finalStates = Collections.singletonList("q3");

        Map<String, Map<String, Set<String>>> transitions = new LinkedHashMap<>();
        addTransition(transitions, "q0", "a", new HashSet<>(Arrays.asList("q0", "q1")));
        addTransition(transitions, "q1", "b", new HashSet<>(Collections.singletonList("q1")));
        addTransition(transitions, "q1", "a", new HashSet<>(Collections.singletonList("q2")));
        addTransition(transitions, "q2", "a", new HashSet<>(Collections.singletonList("q0")));
        addTransition(transitions, "q2", "b", new HashSet<>(Collections.singletonList("q3")));

        FiniteAutomaton fa = new FiniteAutomaton(states, alphabet, transitions, startState, finalStates);

        System.out.println("Is DFA? " + fa.isDeterministic());

        Object[] dfaResult = fa.toDFA();
        @SuppressWarnings("unchecked")
        List<Set<String>> dfaStates = (List<Set<String>>) dfaResult[0];
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Set<String>>> dfaTransitions = (Map<String, Map<String, Set<String>>>) dfaResult[1];
        @SuppressWarnings("unchecked")
        List<Set<String>> dfaFinals = (List<Set<String>>) dfaResult[3];

        System.out.println("\nDFA Transitions:");
        for (Map.Entry<String, Map<String, Set<String>>> outer : dfaTransitions.entrySet()) {
            for (Map.Entry<String, Set<String>> inner : outer.getValue().entrySet()) {
                if (!inner.getValue().isEmpty()) {
                    System.out.println("  " + outer.getKey() + " --" + inner.getKey() + "--> " + inner.getValue());
                }
            }
        }

        System.out.println("\nDFA States:");
        for (Set<String> s : dfaStates) {
            if (!s.isEmpty()) System.out.println("  " + s);
        }

        System.out.println("\nDFA Final States:");
        for (Set<String> s : dfaFinals) {
            System.out.println("  " + s);
        }

        Grammar grammar = fa.toRegularGrammar();

        System.out.println("\nRegular Grammar Productions:");
        for (Map.Entry<String, List<String>> entry : grammar.productions.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + String.join(" | ", entry.getValue()));
        }

        System.out.println("\nGrammar Classification: " + grammar.classify());
    }
}