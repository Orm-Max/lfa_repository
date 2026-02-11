import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) {

        // Define grammar
        Set<String> nonTerminals = new HashSet<>(Arrays.asList("S", "A", "B", "C"));
        Set<String> terminals = new HashSet<>(Arrays.asList("a", "b", "c", "d"));

        Map<String, List<String>> rules = new HashMap<>();
        rules.put("S", Arrays.asList("dA"));
        rules.put("A", Arrays.asList("aB", "bA"));
        rules.put("B", Arrays.asList("bC", "aB", "d"));
        rules.put("C", Arrays.asList("cB"));

        Grammar grammar = new Grammar(nonTerminals, terminals, rules, "S");

        // Generate strings
        List<String> generated = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String s = grammar.generateString(50);
            generated.add(s);
        }

        // Convert to FA
        FiniteAutomaton fa = grammar.toFiniteAutomaton();

        // Test generated strings
        System.out.println("Testing generated strings:");
        for (String s : generated) {
            System.out.println(s + " -> " + (fa.checkString(s) ? "accepted" : "not accepted"));
        }

        // Test invalid strings
        System.out.println("\nTesting invalid strings:");
        String[] invalid = {"abc", "ad", "ddd", "daaa", "xyz"};
        for (String s : invalid) {
            System.out.println(s + " -> " + (fa.checkString(s) ? "accepted" : "not accepted"));
        }
    }
}
