import java.util.*;


public class CNFConverter {

    // Grammar: maps each non-terminal to its list of productions.
    // Each production is a List<String> of symbols.
    static Map<String, List<List<String>>> grammar = new LinkedHashMap<>();

    // Counter for generating new variable names like X1, X2, ...
    static int newVarCounter = 1;

    public static void main(String[] args) {

        // Step 0: Set up the original grammar from Variant 17
        setupGrammar();
        System.out.println(" Original Grammar ");
        printGrammar();

        // Step 1: Eliminate epsilon productions
        eliminateEpsilon();
        System.out.println("\n Step 1: After Eliminating Epsilon Productions ");
        printGrammar();

        // Step 2: Eliminate renamings (unit productions: A -> B)
        eliminateRenamings();
        System.out.println("\n Step 2: After Eliminating Renamings ");
        printGrammar();

        // Step 3: Eliminate inaccessible symbols
        eliminateInaccessible();
        System.out.println("\n Step 3: After Eliminating Inaccessible Symbols ");
        printGrammar();

        // Step 4: Eliminate non-productive symbols
        eliminateNonProductive();
        System.out.println("\n Step 4: After Eliminating Non-Productive Symbols ");
        printGrammar();

        // Step 5: Convert to Chomsky Normal Form
        convertToCNF();
        System.out.println("\n Step 5: Chomsky Normal Form ");
        printGrammar();
    }

    // SETUP: Build the grammar using lists of symbol lists
    static void setupGrammar() {
        // S -> aA | AC
        addRule("S", prod("a", "A"));
        addRule("S", prod("A", "C"));

        // A -> a | ASC | BC | aD
        addRule("A", prod("a"));
        addRule("A", prod("A", "S", "C"));
        addRule("A", prod("B", "C"));
        addRule("A", prod("a", "D"));

        // B -> b | bA
        addRule("B", prod("b"));
        addRule("B", prod("b", "A"));

        // C -> epsilon | BA
        addRule("C", prod());           // epsilon = empty list
        addRule("C", prod("B", "A"));

        // D -> abC
        addRule("D", prod("a", "b", "C"));

        // E -> aB
        addRule("E", prod("a", "B"));
    }

    // Helper: create a production (list of symbols) from varargs
    static List<String> prod(String... symbols) {
        return new ArrayList<>(Arrays.asList(symbols));
    }

    // Helper: add a rule to the grammar
    static void addRule(String var, List<String> production) {
        grammar.computeIfAbsent(var, k -> new ArrayList<>()).add(production);
    }

    // Check if a symbol is a non-terminal (starts with uppercase letter)
    static boolean isNonTerminal(String sym) {
        return Character.isUpperCase(sym.charAt(0));
    }

    // Step 1: Eliminate Epsilon Productions
    static void eliminateEpsilon() {

        // 1a. Find all nullable non-terminals (those that can derive epsilon)
        Set<String> nullable = new LinkedHashSet<>();

        // First: find those that directly produce epsilon (empty production)
        for (String var : grammar.keySet()) {
            for (List<String> prod : grammar.get(var)) {
                if (prod.isEmpty()) {
                    nullable.add(var);
                }
            }
        }

        // Then keep expanding (e.g., if A -> BC where B and C are both nullable)
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String var : grammar.keySet()) {
                if (nullable.contains(var)) continue;
                for (List<String> prod : grammar.get(var)) {
                    // If every symbol in this production is nullable, so is var
                    if (!prod.isEmpty() && nullable.containsAll(prod)) {
                        nullable.add(var);
                        changed = true;
                    }
                }
            }
        }

        System.out.println("  Nullable variables: " + nullable);

        // 1b. For each production containing a nullable symbol,
        //     generate new productions where that symbol is omitted
        Map<String, List<List<String>>> newGrammar = new LinkedHashMap<>();

        for (String var : grammar.keySet()) {
            // Use a set of lists to avoid duplicate productions
            List<List<String>> newProds = new ArrayList<>();

            for (List<String> prod : grammar.get(var)) {
                List<List<String>> combos = generateCombinations(prod, nullable);
                for (List<String> combo : combos) {
                    if (!newProds.contains(combo)) {
                        newProds.add(combo);
                    }
                }
            }

            // Remove epsilon from final result
            newProds.remove(new ArrayList<>());

            newGrammar.put(var, newProds);
        }

        grammar = newGrammar;
    }

    // Generate all ways to omit nullable symbols from a production.
    // Example: prod = ["A","S","C"], nullable = {"C"} -> returns [["A","S","C"], ["A","S"]]
    static List<List<String>> generateCombinations(List<String> prod, Set<String> nullable) {
        List<List<String>> results = new ArrayList<>();
        results.add(new ArrayList<>(prod)); // always include the original

        // Find positions of nullable symbols in this production
        List<Integer> nullablePositions = new ArrayList<>();
        for (int i = 0; i < prod.size(); i++) {
            if (nullable.contains(prod.get(i))) {
                nullablePositions.add(i);
            }
        }

        // Generate all non-empty subsets of nullable positions to remove
        int n = nullablePositions.size();
        for (int mask = 1; mask < (1 << n); mask++) {
            Set<Integer> toRemove = new HashSet<>();
            for (int j = 0; j < n; j++) {
                if ((mask & (1 << j)) != 0) {
                    toRemove.add(nullablePositions.get(j));
                }
            }
            List<String> newProd = new ArrayList<>();
            for (int i = 0; i < prod.size(); i++) {
                if (!toRemove.contains(i)) {
                    newProd.add(prod.get(i));
                }
            }
            if (!newProd.isEmpty() && !results.contains(newProd)) {
                results.add(newProd);
            }
        }

        return results;
    }

    // Step 2: Eliminate Renamings (A -> B unit productions)
    static void eliminateRenamings() {

        boolean changed = true;
        while (changed) {
            changed = false;

            for (String var : grammar.keySet()) {
                List<List<String>> toAdd = new ArrayList<>();
                List<List<String>> toRemove = new ArrayList<>();

                for (List<String> prod : grammar.get(var)) {
                    // A unit production: exactly one symbol and it's a non-terminal
                    if (prod.size() == 1 && isNonTerminal(prod.get(0))) {
                        String target = prod.get(0);
                        toRemove.add(prod);

                        // Add all of the target's productions directly to var
                        if (grammar.containsKey(target)) {
                            for (List<String> targetProd : grammar.get(target)) {
                                if (!grammar.get(var).contains(targetProd)
                                        && !toAdd.contains(targetProd)) {
                                    toAdd.add(new ArrayList<>(targetProd));
                                    changed = true;
                                }
                            }
                        }
                    }
                }

                grammar.get(var).removeAll(toRemove);
                grammar.get(var).addAll(toAdd);
            }
        }
    }

    // Step 3: Eliminate Inaccessible Symbols
    static void eliminateInaccessible() {

        // Start from S and find every non-terminal reachable from it
        Set<String> accessible = new LinkedHashSet<>();
        accessible.add("S");

        boolean changed = true;
        while (changed) {
            changed = false;
            for (String var : new ArrayList<>(accessible)) {
                if (!grammar.containsKey(var)) continue;
                for (List<String> prod : grammar.get(var)) {
                    for (String sym : prod) {
                        if (isNonTerminal(sym) && !accessible.contains(sym)) {
                            accessible.add(sym);
                            changed = true;
                        }
                    }
                }
            }
        }

        Set<String> inaccessible = new LinkedHashSet<>(grammar.keySet());
        inaccessible.removeAll(accessible);

        System.out.println("  Accessible symbols:   " + accessible);
        System.out.println("  Inaccessible removed: " + inaccessible);

        grammar.keySet().retainAll(accessible);
    }

    // Step 4: Eliminate Non-Productive Symbols
    static void eliminateNonProductive() {

        Set<String> productive = new LinkedHashSet<>();

        // Keep expanding the productive set until stable
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String var : grammar.keySet()) {
                if (productive.contains(var)) continue;
                for (List<String> prod : grammar.get(var)) {
                    // A production is productive if every NT in it is already productive
                    if (isProductiveProduction(prod, productive)) {
                        productive.add(var);
                        changed = true;
                        break;
                    }
                }
            }
        }

        Set<String> nonProductive = new LinkedHashSet<>(grammar.keySet());
        nonProductive.removeAll(productive);

        System.out.println("  Productive symbols:       " + productive);
        System.out.println("  Non-productive removed:   " + nonProductive);

        // Remove non-productive variables from grammar
        grammar.keySet().retainAll(productive);

        // Also remove productions that reference non-productive variables
        for (String var : grammar.keySet()) {
            List<List<String>> toRemove = new ArrayList<>();
            for (List<String> prod : grammar.get(var)) {
                for (String sym : prod) {
                    if (isNonTerminal(sym) && nonProductive.contains(sym)) {
                        toRemove.add(prod);
                        break;
                    }
                }
            }
            grammar.get(var).removeAll(toRemove);
        }
    }

    // A production is productive if every non-terminal in it is already known productive
    static boolean isProductiveProduction(List<String> prod, Set<String> productive) {
        for (String sym : prod) {
            if (isNonTerminal(sym) && !productive.contains(sym)) {
                return false;
            }
        }
        return true; // all terminals are automatically "productive"
    }

    // Step 5: Convert to Chomsky Normal Form (CNF)
    static void convertToCNF() {

        // 5a: Replace terminals in long productions
        // terminalVarMap: "a" -> "TA",  "b" -> "TB"
        Map<String, String> terminalVarMap = new LinkedHashMap<>();

        Map<String, List<List<String>>> updatedGrammar = new LinkedHashMap<>();

        for (String var : grammar.keySet()) {
            List<List<String>> updatedProds = new ArrayList<>();

            for (List<String> prod : grammar.get(var)) {
                if (prod.size() <= 1) {
                    // A -> a  or  A -> epsilon: keep as-is
                    updatedProds.add(new ArrayList<>(prod));
                } else {
                    // Replace each terminal symbol with its new variable
                    List<String> newProd = new ArrayList<>();
                    for (String sym : prod) {
                        if (!isNonTerminal(sym)) {
                            // It's a terminal: get or create a variable for it
                            if (!terminalVarMap.containsKey(sym)) {
                                // "a" -> "TA", "b" -> "TB"
                                String newVar = "T" + sym.toUpperCase();
                                terminalVarMap.put(sym, newVar);
                            }
                            newProd.add(terminalVarMap.get(sym));
                        } else {
                            newProd.add(sym); // non-terminal stays as-is
                        }
                    }
                    updatedProds.add(newProd);
                }
            }

            updatedGrammar.put(var, updatedProds);
        }

        // Add the terminal replacement rules: TA -> a, TB -> b
        for (Map.Entry<String, String> entry : terminalVarMap.entrySet()) {
            List<List<String>> termProds = new ArrayList<>();
            termProds.add(prod(entry.getKey())); // e.g., ["a"]
            updatedGrammar.put(entry.getValue(), termProds);
        }

        grammar = updatedGrammar;

        // 5b: Break productions longer than 2 into binary productions
        // seqCache maps a sequence of symbols (as a joined string key) -> the
        // new variable we created to represent that sequence.
        // Example: "S,C" -> "X1"  means X1 -> SC was added
        Map<String, String> seqCache = new LinkedHashMap<>();

        boolean changed = true;
        while (changed) {
            changed = false;

            // Snapshot current variable names (grammar may grow during this loop)
            List<String> currentVars = new ArrayList<>(grammar.keySet());

            for (String var : currentVars) {
                List<List<String>> newProds = new ArrayList<>();

                for (List<String> prod : grammar.get(var)) {

                    if (prod.size() <= 2) {
                        // Already in CNF form (or a single terminal): keep it
                        newProds.add(new ArrayList<>(prod));

                    } else {
                        // Production is too long; split it:
                        // [A, S, C]  =>  first=A, rest=[S, C]
                        // Create new rule: X_new -> [S, C]
                        // Replace original with: [A, X_new]
                        changed = true;

                        String first = prod.get(0);
                        List<String> rest = prod.subList(1, prod.size());

                        // Build a string key for the rest, e.g., "S,C"
                        String restKey = String.join(",", rest);

                        // Reuse an existing variable if we already created one for this rest
                        String restVar = seqCache.get(restKey);

                        if (restVar == null) {
                            // Create a brand-new variable for this rest sequence
                            restVar = "X" + newVarCounter++;
                            seqCache.put(restKey, restVar);

                            // Add the new rule to the grammar
                            List<List<String>> newVarProds = new ArrayList<>();
                            newVarProds.add(new ArrayList<>(rest)); // e.g., X1 -> [S, C]
                            grammar.put(restVar, newVarProds);
                        }

                        // The current production becomes binary: [first, restVar]
                        List<String> binaryProd = new ArrayList<>();
                        binaryProd.add(first);
                        binaryProd.add(restVar);
                        newProds.add(binaryProd);
                    }
                }

                grammar.put(var, newProds);
            }
        }
    }

    // Helper: Print the grammar in a readable format
    static void printGrammar() {
        for (String var : grammar.keySet()) {
            List<String> prodStrings = new ArrayList<>();
            for (List<String> prod : grammar.get(var)) {
                if (prod.isEmpty()) {
                    prodStrings.add("epsilon");
                } else {
                    prodStrings.add(String.join("", prod));
                }
            }
            System.out.println("  " + var + " -> " + String.join(" | ", prodStrings));
        }
    }
}
