import java.util.*;

public class RegexGenerator {

    static final int MAX_REPEAT = 5;       // cap for + and * quantifiers
    static final Random random  = new Random();

    abstract static class Node {
        abstract String generate();
        abstract String describe(String indent);
    }

    // LiteralNode - a fixed single character, e.g. 'p', '1', 'E'
    static class LiteralNode extends Node {
        String ch;
        LiteralNode(String ch) { this.ch = ch; }

        @Override String generate() { return ch; }

        @Override String describe(String indent) {
            return indent + "Literal(\"" + ch + "\")";
        }
    }

    // ConcatNode - all children in sequence: child1 + child2 + child3 ...
    static class ConcatNode extends Node {
        List<Node> parts;
        ConcatNode(List<Node> parts) { this.parts = parts; }

        @Override String generate() {
            StringBuilder sb = new StringBuilder();
            for (Node n : parts) sb.append(n.generate());
            return sb.toString();
        }

        @Override String describe(String indent) {
            StringBuilder sb = new StringBuilder(indent + "Concat [\n");
            for (Node n : parts) sb.append(n.describe(indent + "  ")).append("\n");
            return sb.append(indent).append("]").toString();
        }
    }

    // AltNode - randomly pick one of the choices
    static class AltNode extends Node {
        List<Node> choices;
        AltNode(List<Node> choices) { this.choices = choices; }

        @Override String generate() {
            return choices.get(random.nextInt(choices.size())).generate();
        }

        @Override String describe(String indent) {
            StringBuilder sb = new StringBuilder(
                indent + "Alternation (pick 1 of " + choices.size() + ") [\n");
            for (Node n : choices) sb.append(n.describe(indent + "  ")).append("\n");
            return sb.append(indent).append("]").toString();
        }
    }

    // RepeatNode - repeat child between min and max times
    //   max == -1  →  unbounded, capped at MAX_REPEAT
    static class RepeatNode extends Node {
        Node child;
        int min, max;

        RepeatNode(Node child, int min, int max) {
            this.child = child;
            this.min   = min;
            this.max   = max;
        }

        @Override String generate() {
            int hi    = (max == -1) ? MAX_REPEAT : max;
            int count = min + random.nextInt(hi - min + 1);  // random in [min, hi]
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) sb.append(child.generate());
            return sb.toString();
        }

        @Override String describe(String indent) {
            String range;
            if      (max == -1)    range = min + " to " + MAX_REPEAT + " (capped)";
            else if (min == max)   range = "exactly " + min;
            else                   range = min + " to " + max;

            return indent + "Repeat(" + range + ") [\n"
                 + child.describe(indent + "  ") + "\n"
                 + indent + "]";
        }
    }


    static class Parser {

        final String input;           // original string, spaces kept
        int pos;
        final List<String> steps = new ArrayList<>();

        Parser(String raw) {
            this.input = raw;
            this.pos   = 0;
        }

        //  Low-level helpers 

        char peek() {
            return (pos < input.length()) ? input.charAt(pos) : '\0';
        }

        char consume() {
            return input.charAt(pos++);
        }

        // Skip spaces (used between atoms in concat, but NOT inside quantifier check)
        void skipSpaces() {
            while (pos < input.length() && input.charAt(pos) == ' ') pos++;
        }

        // Entry point
        Node parse() {
            steps.add("Raw input: \"" + input + "\"");
            steps.add("Rule: spaces outside groups are visual separators (ignored)");
            steps.add("Rule: digit immediately after ')' = exact-repetition quantifier");
            Node result = parseConcat();
            steps.add("Parsing complete.");
            return result;
        }

        //  concat: collect atoms (skip spaces between them)

        Node parseConcat() {
            steps.add("[Concat] start  pos=" + pos);
            List<Node> parts = new ArrayList<>();

            while (true) {
                skipSpaces();                // skip visual-separator spaces
                char c = peek();
                if (c == '\0' || c == '|' || c == ')') break;
                parts.add(parseAtom());
            }

            if (parts.isEmpty())   return new LiteralNode("");
            if (parts.size() == 1) return parts.get(0);

            steps.add("[Concat] -> ConcatNode  parts=" + parts.size());
            return new ConcatNode(parts);
        }

        //  atom: base + optional quantifier 
        Node parseAtom() {
            boolean wasGroup = (peek() == '(');
            Node base = parseBase();

            // Peek for quantifier WITHOUT skipping spaces first.
            // This is intentional: if a space precedes the digit,
            // the digit belongs to the next atom, not a quantifier.
            char q = peek();

            if (q == '+' || q == '*' || q == '?') {
                base = applySymbolQuantifier(base);
            } else if (wasGroup && Character.isDigit(q)) {
                // Digit IMMEDIATELY after ')' → exact repetition count
                base = applyDigitQuantifier(base);
            }

            return base;
        }

        //  base: literal character or (...) group 
        Node parseBase() {
            if (peek() == '(') {
                return parseGroup();
            } else {
                char c = consume();
                steps.add("[Base ] literal '" + c + "'  pos=" + (pos - 1));
                return new LiteralNode(String.valueOf(c));
            }
        }

        //  parseGroup: read content between ( and ) 
        Node parseGroup() {
            steps.add("[Group] '(' at pos=" + pos);
            consume(); // eat '('

            // Collect raw content until the matching ')'
            StringBuilder buf = new StringBuilder();
            int depth = 1;
            while (pos < input.length() && depth > 0) {
                char c = consume();
                if      (c == '(') { depth++; buf.append(c); }
                else if (c == ')') { depth--; if (depth > 0) buf.append(c); }
                else               {          buf.append(c); }
            }

            String content = buf.toString();
            steps.add("[Group] content = \"" + content + "\"");
            return buildGroupNode(content);
        }

        //  buildGroupNode: decide alternation vs char-class 
        Node buildGroupNode(String content) {
            if (content.contains("|")) {
                //  Alternation 
                List<String> branches = splitByTopLevelPipe(content);
                List<Node> choices = new ArrayList<>();
                for (String branch : branches) {
                    choices.add(parseBranch(branch));
                }
                steps.add("[Group] -> Alternation  branches=" + branches);
                return (choices.size() == 1) ? choices.get(0) : new AltNode(choices);

            } else {
                //  Character class 
                // Every non-space character is a separate alternative
                String stripped = content.replaceAll("\\s+", "");
                List<Node> choices = new ArrayList<>();
                for (char c : stripped.toCharArray()) {
                    choices.add(new LiteralNode(String.valueOf(c)));
                }
                steps.add("[Group] -> CharClass {" + stripped + "}");
                return (choices.size() == 1) ? choices.get(0) : new AltNode(choices);
            }
        }

        /** Split a string by '|', but only at depth-0 (skip nested groups) */
        List<String> splitByTopLevelPipe(String s) {
            List<String> result = new ArrayList<>();
            StringBuilder cur   = new StringBuilder();
            int depth = 0;

            for (char c : s.toCharArray()) {
                if      (c == '(')          { depth++;  cur.append(c); }
                else if (c == ')')          { depth--;  cur.append(c); }
                else if (c == '|' && depth == 0) {
                    result.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
            result.add(cur.toString()); // last branch
            return result;
        }

        /**
         * Parse a branch string (like " u v " or "w") as a literal sequence.
         * Spaces within a branch mean concatenation (so "u v" → "uv").
         */
        Node parseBranch(String branch) {
            // Strip surrounding and internal spaces → the multi-char literal
            String clean = branch.replaceAll("\\s+", "");
            if (clean.isEmpty())    return new LiteralNode("");
            if (clean.length() == 1) return new LiteralNode(clean);

            List<Node> parts = new ArrayList<>();
            for (char c : clean.toCharArray()) {
                parts.add(new LiteralNode(String.valueOf(c)));
            }
            return new ConcatNode(parts);
        }


        // Apply +, *, or ? quantifier symbol
        Node applySymbolQuantifier(Node base) {
            char q = consume();
            if (q == '?') {
                steps.add("[Quant] '?' -> 0 or 1 times");
                return new RepeatNode(base, 0, 1);
            } else if (q == '+') {
                steps.add("[Quant] '+' -> 1 to " + MAX_REPEAT + " times (capped)");
                return new RepeatNode(base, 1, -1);
            } else { // '*'
                steps.add("[Quant] '*' -> 0 to " + MAX_REPEAT + " times (capped)");
                return new RepeatNode(base, 0, -1);
            }
        }

        // Apply a digit after a group as exact repetition count, e.g. (3|4)5 → 5 times
        Node applyDigitQuantifier(Node base) {
            StringBuilder digits = new StringBuilder();
            int n = 0;
            while (Character.isDigit(peek())) {
                digits.append(peek());
                n = n * 10 + (consume() - '0');
            }
            steps.add("[Quant] digit \"" + digits + "\" -> exactly " + n + " times");
            return new RepeatNode(base, n, n);
        }
    }

    public static void main(String[] args) {
        String[] regexes = {
            "(ab)(cd)E+ G?",
            "p(Q | R | S) T (u v | w | x)* Z+",
            "1 (0 | 1)* 2 (3 | 4)5 36"
        };

        // Expected outputs from the task description (for reference):
        //   Regex 1:  {acEG, bdE, adEEG, ...}
        //   Regex 2:  {pQTuvuvZ, pRTwwwwZ, ...}
        //   Regex 3:  {1023333336, 1124444436, ...}
        String[] expected = {
            "{acEG, bdE, adEEG, ...}",
            "{pQTuvuvZ, pRTwwwwZ, ...}",
            "{1023333336, 1124444436, ...}"
        };

        for (int i = 0; i < regexes.length; i++) {

            System.out.println("\n" + "=".repeat(65));
            System.out.println("  REGEX " + (i + 1) + ":  " + regexes[i]);
            System.out.println("  Expected format: " + expected[i]);
            System.out.println("=".repeat(65));

            // Parse the regex into an AST
            Parser parser = new Parser(regexes[i]);
            Node   tree   = parser.parse();

            // BONUS: print the step-by-step parsing trace
            System.out.println("\n  [c] STEP-BY-STEP PARSING TRACE:");
            for (int j = 0; j < parser.steps.size(); j++) {
                System.out.printf("    %2d. %s%n", j + 1, parser.steps.get(j));
            }

            // Print the resulting AST
            System.out.println("\n  [b] AST STRUCTURE:");
            System.out.println(tree.describe("    "));

            // Generate 5 random valid strings
            System.out.println("\n  [a] GENERATED STRINGS  (5 samples):");
            for (int j = 1; j <= 5; j++) {
                System.out.println("      " + j + ".  \"" + tree.generate() + "\"");
            }
        }

        System.out.println("\n" + "=".repeat(65));
    }
}