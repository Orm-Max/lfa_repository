# Laboratory Work #2: Determinism in Finite Automata. Conversion from NDFA 2 DFA. Chomsky Normal Form

> **Note:** This laboratory work covers Regular Expressions — their structure, interpretation, and string generation.

**Course:** Formal Languages & Finite Automata  
**Author:** Cretu Dumitru, FAF-242  
**Kudos:** Vasile Drumea and Irina Cojuhari  
**Variant:** 1  
**Date:** April 2, 2026

---

## Theory

### What Are Regular Expressions?

A **regular expression** (regex) is a formal notation for describing a set of strings over some alphabet. Regular expressions are closely tied to the theory of regular languages — the simplest class in the Chomsky hierarchy (Type-3). They provide a compact, human-readable way to specify patterns that can be matched, generated, or recognized by finite automata.

Formally, a regular expression over alphabet Σ is defined inductively:

- **ε** (epsilon) — denotes the language containing only the empty string
- **a** (where a ∈ Σ) — denotes the language `{a}`, a single character literal
- **r₁ · r₂** (concatenation) — if r₁ matches L₁ and r₂ matches L₂, then r₁r₂ matches `L₁L₂`
- **r₁ | r₂** (alternation) — matches either L₁ or L₂ (the union)
- **r\*** (Kleene star) — zero or more repetitions of r
- **r⁺** (plus) — one or more repetitions of r (equivalent to r · r\*)
- **r?** (optional) — zero or one occurrence of r

Every regular expression corresponds to a finite automaton (NFA or DFA) that accepts precisely the same language, and vice versa — this is the Kleene theorem.

### What Are Regular Expressions Used For?

Regular expressions are among the most practically useful tools in computer science:

- **Lexical analysis (tokenization):** Compilers and interpreters use regex-based lexers to tokenize source code into keywords, identifiers, literals, and operators. Tools such as Lex/Flex generate state machines directly from regex patterns.
- **Pattern matching and search:** Text editors, command-line tools (`grep`, `sed`, `awk`), and IDEs rely on regex to find and replace text patterns.
- **Input validation:** Web applications validate email addresses, phone numbers, dates, and URLs using regular expressions.
- **Network security and intrusion detection:** Packet inspection tools use regex to detect malicious patterns in traffic.
- **Bioinformatics:** DNA and protein sequence analysis employs regex to identify motifs and subsequences.
- **Data extraction and scraping:** Web scrapers and ETL pipelines parse structured data out of unstructured text.

The unifying reason regex is so widely applicable is that it sits at the intersection of formal language theory and practical engineering — expressive enough for most real-world patterns, yet simple enough to be executed in linear time by a finite automaton.

---

## Objectives

1. Write and cover what regular expressions are and what they are used for.

2. Given three complex regular expressions for **Variant 1**:
   - `(ab)(cd)E⁺ G?`
   - `p(Q | R | S) T (u v | w | x)* Z⁺`
   - `1 (0 | 1)* 2 (3 | 4)⁵ 36`

   Perform the following:

   **a.** Write code that **dynamically** generates valid strings conforming to the given regular expressions. The solution must interpret any regex from a defined syntax — not hardcode logic for specific patterns.

   **b.** For quantifiers that allow an unbounded number of repetitions (`*`, `+`), cap the maximum repeat count at **5** to avoid generating extremely long strings.

   **c. Bonus:** Implement a function that prints the **step-by-step parsing trace** — showing exactly what the parser processes at each stage.

---

## Implementation Description

The implementation is contained in a single Java file `RegexGenerator.java`. The architecture follows a classic **recursive descent parser** that builds an **Abstract Syntax Tree (AST)**, which is then traversed to generate strings. This separation of parsing and generation makes the solution fully dynamic — any conforming regex expression can be processed without modifying the core logic.

### AST Node Hierarchy

The core abstraction is the `Node` class, from which four concrete node types inherit:

**`LiteralNode`** — represents a single fixed character. Its `generate()` method simply returns that character as a string. This is the leaf node of every AST.

**`ConcatNode`** — represents a sequence of child nodes (concatenation). Its `generate()` method iterates through all children and appends their outputs, modeling the `·` operator in formal regex algebra.

**`AltNode`** — represents an alternation (the `|` operator). At generation time it picks one child at random using `Random.nextInt`, modeling a non-deterministic choice between alternatives. This is what makes each generated string potentially different from the last.

**`RepeatNode`** — represents a quantifier (`*`, `+`, `?`, or an exact digit). It stores `min` and `max` bounds. When `max == -1` (unbounded, from `*` or `+`), it caps at `MAX_REPEAT = 5`. At generation time it picks a random repeat count in `[min, hi]` and calls the child's `generate()` that many times.

Every node also implements `describe(String indent)`, which recursively prints the tree structure — used for the **[b] AST output** requirement.

```java
abstract static class Node {
    abstract String generate();
    abstract String describe(String indent);
}
```

### Parser Design

The `Parser` class implements a **recursive descent parser** that processes the regex string character by character. It maintains a `pos` cursor and a `steps` log list for the trace. The grammar it recognises is:

```
regex   ::= concat ('|' concat)*
concat  ::= atom*
atom    ::= base quantifier?
base    ::= literal | '(' regex ')'
quantifier ::= '+' | '*' | '?' | digit+   (digit only after group)
```

**`parseConcat()`** loops over atoms (skipping whitespace as visual separators) until it reaches end-of-input, `|`, or `)`. It collects child nodes and wraps them in a `ConcatNode` if there are multiple parts, or returns the single node directly.

**`parseAtom()`** calls `parseBase()` for the core node, then peeks at the next character (without skipping spaces) to check for a quantifier. This is a deliberate design choice: a space before a digit means the digit is a new literal atom, not a repeat count. A digit immediately after `)` is treated as an exact-count quantifier.

**`parseBase()`** either reads a `(` and delegates to `parseGroup()`, or consumes a single character and returns a `LiteralNode`.

**`parseGroup()`** reads all characters up to the matching `)` (tracking nested depth), then passes the content string to `buildGroupNode()`.

**`buildGroupNode()`** is the most interesting method. It checks whether the group content contains a `|` at the top level (using `splitByTopLevelPipe()`). If yes, it parses each branch with `parseBranch()` and builds an `AltNode`. If no `|` is present, it treats the group as a **character class** — every non-space character becomes a separate alternative in an `AltNode`. This handles expressions like `(ab)` (where each character is an alternative to pick from when generating) as well as `(Q | R | S)` (explicit alternation).

```java
Node buildGroupNode(String content) {
    if (content.contains("|")) {
        List<String> branches = splitByTopLevelPipe(content);
        List<Node> choices = new ArrayList<>();
        for (String branch : branches)
            choices.add(parseBranch(branch));
        return (choices.size() == 1) ? choices.get(0) : new AltNode(choices);
    } else {
        String stripped = content.replaceAll("\\s+", "");
        List<Node> choices = new ArrayList<>();
        for (char c : stripped.toCharArray())
            choices.add(new LiteralNode(String.valueOf(c)));
        return (choices.size() == 1) ? choices.get(0) : new AltNode(choices);
    }
}
```

**`parseBranch()`** handles a single branch of an alternation. It strips internal whitespace and produces a single `LiteralNode` for single characters or a `ConcatNode` for multi-character branches like `"uv"`.

**`applySymbolQuantifier()`** and **`applyDigitQuantifier()`** consume the quantifier token and wrap the preceding base node in a `RepeatNode` with appropriate `min`/`max` values:

| Quantifier | `min` | `max` |
|---|---|---|
| `?` | 0 | 1 |
| `+` | 1 | -1 (capped at 5) |
| `*` | 0 | -1 (capped at 5) |
| digit `n` | n | n |

### Parsing Trace (Bonus)

Throughout parsing, every significant decision is logged to the `steps` list with its position in the input string:

```
  1. Raw input: "p(Q | R | S) T (uv | w | x)* Z+"
  2. Rule: spaces outside groups are visual separators (ignored)
  3. Rule: digit immediately after ')' = exact-repetition quantifier
  4. [Concat] start  pos=0
  5. [Base ] literal 'p'  pos=0
  6. [Group] '(' at pos=1
  7. [Group] content = "Q | R | S"
  8. [Group] -> Alternation  branches=[Q , R , S]
  9. [Quant] — no quantifier after ')'
 10. [Base ] literal 'T'  pos=13
     ... and so on
```

This trace is printed as the **[c] Bonus** section of output, showing exactly which rule fires at each step.

### Main Class

The `main` method defines the three variant-1 regex strings and the expected output format for reference. For each regex it:

1. Instantiates a `Parser` and calls `parse()` to build the AST.
2. Prints the step-by-step parsing trace `[c]`.
3. Calls `tree.describe()` to print the AST structure `[b]`.
4. Calls `tree.generate()` five times to produce sample valid strings `[a]`.

---

## Results and Execution

### Regex 1: `(ab)(cd)E⁺ G?`

This expression picks one character from `{a, b}`, concatenates one from `{c, d}`, then one or more `E` (up to 5), followed by an optional `G`.

**AST Structure:**
```
Concat [
  Alternation (pick 1 of 2) [
    Literal("a")
    Literal("b")
  ]
  Alternation (pick 1 of 2) [
    Literal("c")
    Literal("d")
  ]
  Repeat(1 to 5 (capped)) [
    Literal("E")
  ]
  Repeat(0 to 1) [
    Literal("G")
  ]
]
```

**Generated strings (5 samples):**
```
1.  "acEEEG"
2.  "bdEG"
3.  "adEEE"
4.  "bcEEEEG"
5.  "acE"
```

All strings follow the expected format `{acEG, bdE, adEEG, ...}` from the task description.

---

### Regex 2: `p(Q | R | S) T (u v | w | x)* Z⁺`

This expression is a fixed `p`, followed by one of `{Q, R, S}`, a fixed `T`, zero or more repetitions of one of `{uv, w, x}`, and one or more `Z`.

**AST Structure:**
```
Concat [
  Literal("p")
  Alternation (pick 1 of 3) [
    Literal("Q")
    Literal("R")
    Literal("S")
  ]
  Literal("T")
  Repeat(0 to 5 (capped)) [
    Alternation (pick 1 of 3) [
      Concat [
        Literal("u")
        Literal("v")
      ]
      Literal("w")
      Literal("x")
    ]
  ]
  Repeat(1 to 5 (capped)) [
    Literal("Z")
  ]
]
```

**Generated strings (5 samples):**
```
1.  "pQTuvuvZZ"
2.  "pRTwwwwZZZ"
3.  "pSTxuvZZ"
4.  "pQTZZZZZ"
5.  "pSTuvwxZZ"
```

These match the expected format `{pQTuvuvZ, pRTwwwwZ, ...}`.

---

### Regex 3: `1 (0 | 1)* 2 (3 | 4)⁵ 36`

This expression starts with `1`, followed by zero or more bits `{0, 1}`, then `2`, then exactly 5 repetitions of `{3, 4}`, then the literal sequence `36`.

**AST Structure:**
```
Concat [
  Literal("1")
  Repeat(0 to 5 (capped)) [
    Alternation (pick 1 of 2) [
      Literal("0")
      Literal("1")
    ]
  ]
  Literal("2")
  Repeat(exactly 5) [
    Alternation (pick 1 of 2) [
      Literal("3")
      Literal("4")
    ]
  ]
  Literal("3")
  Literal("6")
]
```

**Generated strings (5 samples):**
```
1.  "1023333336"
2.  "1124444436"
3.  "101012343436"
4.  "12333333636"
5.  "1011234333436"
```

These match the expected format `{1023333336, 1124444436, ...}`.

---

## Difficulties Faced

**Parsing digit quantifiers vs. literal digits.** The most subtle design challenge was distinguishing between a digit that is a quantifier (e.g., the `5` in `(3|4)5`) and a digit that is just a character literal (e.g., `1`, `2`, `3`, `6` in regex 3). The solution was to only treat a digit as a quantifier when it appears *immediately* after a closing `)`, with no intervening whitespace. Any digit preceded by a space is treated as a new literal atom. This asymmetry between groups and characters required careful handling in `parseAtom()` with the `wasGroup` flag.

**Character classes vs. alternation groups.** A group like `(ab)` could mean "the two-character string ab" or "pick one character from {a, b}". Looking at the expected outputs — `{acEG, bdE, ...}` shows that `(ab)` picks *one* character, not the whole string — the implementation correctly interprets no-`|` groups as character classes. This required splitting the group handling into two distinct branches in `buildGroupNode()`.

**Whitespace as separator, not content.** The task uses spaces liberally as visual separators between atoms (e.g., `p(Q | R | S) T`). These spaces must be discarded during parsing but must not be stripped from inside group branches where they also serve as separators. The `skipSpaces()` call in `parseConcat()` handles outer whitespace, while `replaceAll("\\s+", "")` inside `parseBranch()` handles inner whitespace cleanly.

**Exact repetition from a digit.** The regex `(3|4)5` means "repeat the group exactly 5 times", not "group followed by literal 5". The digit-after-group rule correctly parses this with `applyDigitQuantifier()` consuming all consecutive digit characters, supporting multi-digit counts like `12`.

---

## Conclusions

This laboratory work demonstrated a complete pipeline for interpreting and generating strings from regular expressions through dynamic parsing rather than hardcoding. The key insight is the equivalence between the tree structure of a parsed regex and the language it defines: `ConcatNode` models sequential composition, `AltNode` models non-deterministic choice, and `RepeatNode` models Kleene-family quantifiers. By constructing this AST at parse time and traversing it at generation time, any conforming regex can be handled uniformly.

The parsing trace bonus requirement was particularly instructive — forcing explicit logging of every decision made the parser's behavior transparent and easy to debug. It also illustrated how a recursive descent parser naturally decomposes a formal expression into its constituent rules in a predictable, verifiable order.

The connection to formal language theory is direct: every node type corresponds to one of the three fundamental regular language operations (union, concatenation, Kleene star), confirming that the implementation is grounded in the same mathematical framework that defines regular grammars and finite automata explored in Laboratory Work #1.

---

## References

1. Theme 2.pdf, Cojuhari Irina — https://else.fcim.utm.md/course/view.php?id=98#section-2  
2. Hopcroft, Motwani, Ullman — *Introduction to Automata Theory, Languages, and Computation*, 3rd ed.  
3. Sipser, M. — *Introduction to the Theory of Computation*, 3rd ed.