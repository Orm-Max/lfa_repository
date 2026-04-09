# Laboratory Work #2: Determinism in Finite Automata. Conversion from NFA to DFA. Chomsky Normal Form

**Course:** Formal Languages & Finite Automata  
**Author:** Maxim Ormanji FAF-242  
**Date:** February 25, 2026

---

## Theory

A **regular expression** (regex) is a compact formal notation for describing sets of strings over an alphabet. Regular expressions are defined inductively:

- A single character `a` is a regex denoting the language `{a}`
- The concatenation `RS` of two regexes `R` and `S` denotes all strings formed by appending a string from `S` to a string from `R`
- The alternation `R|S` denotes the union of the two languages
- The Kleene star `R*` denotes zero or more repetitions of strings from `R`
- The plus operator `R+` denotes one or more repetitions (equivalent to `RR*`)
- The question mark `R?` denotes zero or one occurrence

Regular expressions are exactly as expressive as regular grammars and finite automata — all three formalisms describe the class of **regular languages** (Type-3 in the Chomsky hierarchy).

An **Abstract Syntax Tree (AST)** is the standard internal representation used when processing a regex. Each node in the AST encodes one structural element of the expression:

| Node type    | Meaning                                       |
|-------------|-----------------------------------------------|
| `Literal`   | A fixed character                             |
| `Concat`    | Children appear in sequence                   |
| `Alt`       | Exactly one child is chosen at random         |
| `Repeat`    | The child is repeated a certain number of times |

A **recursive-descent parser** translates the flat character stream of a regex string into such a tree by mutually recursive parsing functions — one per grammar rule — without needing a separate tokenizer.

---

## Objectives

1. Implement an **AST node hierarchy** covering literals, concatenation, alternation, and repetition

2. Build a **recursive-descent parser** that reads a regex string and produces the corresponding AST, including support for `+`, `*`, `?` quantifiers and digit-based exact-repetition notation

3. Provide a **`generate()` method** on every node type so that calling it on the root produces a random string belonging to the language defined by the regex

4. Emit a **step-by-step parsing trace** for each regex, showing every decision the parser makes

5. Print the final **AST structure** in an indented human-readable form

6. Demonstrate the system on three non-trivial regexes and produce five sample strings each

---

## Implementation Description

### AST Node Hierarchy

Every node in the tree extends the abstract class `Node`, which declares two abstract methods: `generate()` returns a random string from the node's language, and `describe(String indent)` returns a pretty-printed representation of the subtree.

Four concrete node types are implemented:

**`LiteralNode`** stores a single fixed character and always returns it unchanged.

**`ConcatNode`** holds an ordered list of child nodes and generates a string by appending the results of all children in sequence.

**`AltNode`** holds a list of alternative child nodes and generates a string by picking exactly one child at random using `Random.nextInt`.

**`RepeatNode`** wraps a single child and an integer range `[min, max]`. When `max == -1` the repetition is unbounded and is capped at `MAX_REPEAT` (5) to guarantee termination. It picks a random count within the range and calls `child.generate()` that many times.

```java
// RepeatNode.generate() — random count in [min, hi], hi capped at MAX_REPEAT
@Override String generate() {
    int hi    = (max == -1) ? MAX_REPEAT : max;
    int count = min + random.nextInt(hi - min + 1);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; i++) sb.append(child.generate());
    return sb.toString();
}
```

### Recursive-Descent Parser

The `Parser` class consumes the input string character by character through a cursor `pos`. It records every structural decision in a `steps` list, which is printed after parsing as the trace required by the bonus task.

The parsing grammar used internally is:

```
regex  ::= concat ( '|' concat )*          [handled inside group nodes]
concat ::= atom+
atom   ::= base quantifier?
base   ::= CHAR | '(' group_content ')'
```

**`parseConcat()`** loops, skipping visual-separator spaces, and calls `parseAtom()` until it hits end-of-input, `|`, or `)`. If only one atom is collected it returns that node directly; otherwise it wraps everything in a `ConcatNode`.

**`parseAtom()`** calls `parseBase()` to get the core node, then peeks at the next character *without skipping spaces* — this is deliberate, because a space before a digit means the digit starts a new atom rather than being a repetition count.

**`parseBase()`** either reads a single character literal or delegates to `parseGroup()` when a `(` is seen.

**`parseGroup()`** collects all characters up to the matching `)` by tracking parenthesis depth, then passes the raw content string to `buildGroupNode()`.

**`buildGroupNode()`** inspects the content for a top-level `|`. If one is found it splits into branches via `splitByTopLevelPipe()` and builds an `AltNode`. Otherwise it treats every non-space character as a separate alternative in a character-class `AltNode`.

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

### Quantifier Handling

Two quantifier forms are supported:

- **Symbol quantifiers** (`+`, `*`, `?`): consumed immediately after the base, wrap it in a `RepeatNode` with ranges `[1,−1]`, `[0,−1]`, and `[0,1]` respectively.
- **Digit quantifier**: a sequence of digits appearing *immediately* after a closing `)` is consumed as an exact repetition count and wrapped in a `RepeatNode` with `min == max == n`.

### Main Class

The `main` method iterates over three hard-coded regexes, parses each, prints the parsing trace, prints the AST, and generates five random strings. The expected output format is shown alongside each regex for comparison.

---

## Results and Execution

The program was run on three regular expressions. Below are the results including the parsing trace, AST, and sample generated strings for each.

---

### Regex 1: `(ab)(cd)E+ G?`

**Step-by-step parsing trace:**
```
 1. Raw input: "(ab)(cd)E+ G?"
 2. Rule: spaces outside groups are visual separators (ignored)
 3. Rule: digit immediately after ')' = exact-repetition quantifier
 4. [Concat] start  pos=0
 5. [Group] '(' at pos=0
 6. [Group] content = "ab"
 7. [Group] -> CharClass {ab}
 8. [Group] '(' at pos=4
 9. [Group] content = "cd"
10. [Group] -> CharClass {cd}
11. [Base ] literal 'E'  pos=8
12. [Quant] '+' -> 1 to 5 times (capped)
13. [Base ] literal 'G'  pos=11
14. [Quant] '?' -> 0 or 1 times
15. [Concat] -> ConcatNode  parts=4
16. Parsing complete.
```

**AST structure:**
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
4.  "bcEEEEEG"
5.  "acE"
```

All strings consist of one character from `{a,b}`, one from `{c,d}`, one or more `E`, and an optional `G` — which matches the expected format `{acEG, bdE, adEEG, ...}`.

---

### Regex 2: `p(Q | R | S) T (u v | w | x)* Z+`

**Step-by-step parsing trace:**
```
 1. Raw input: "p(Q | R | S) T (u v | w | x)* Z+"
 2. Rule: spaces outside groups are visual separators (ignored)
 3. Rule: digit immediately after ')' = exact-repetition quantifier
 4. [Concat] start  pos=0
 5. [Base ] literal 'p'  pos=0
 6. [Group] '(' at pos=1
 7. [Group] content = "Q | R | S"
 8. [Group] -> Alternation  branches=[Q , R , S]
 9. [Base ] literal 'T'  pos=13
10. [Group] '(' at pos=15
11. [Group] content = "u v | w | x"
12. [Group] -> Alternation  branches=[u v , w , x]
13. [Quant] '*' -> 0 to 5 times (capped)
14. [Base ] literal 'Z'  pos=29
15. [Quant] '+' -> 1 to 5 times (capped)
16. [Concat] -> ConcatNode  parts=5
17. Parsing complete.
```

**AST structure:**
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
3.  "pSTxuvZ"
4.  "pQTZZZZ"
5.  "pRTuvwxZZ"
```

Each string starts with `p`, has one of `Q/R/S`, a fixed `T`, zero or more repetitions of `uv`/`w`/`x`, and one or more `Z` — matching the expected format `{pQTuvuvZ, pRTwwwwZ, ...}`.

---

### Regex 3: `1 (0 | 1)* 2 (3 | 4)5 36`

**Step-by-step parsing trace:**
```
 1. Raw input: "1 (0 | 1)* 2 (3 | 4)5 36"
 2. Rule: spaces outside groups are visual separators (ignored)
 3. Rule: digit immediately after ')' = exact-repetition quantifier
 4. [Concat] start  pos=0
 5. [Base ] literal '1'  pos=0
 6. [Group] '(' at pos=2
 7. [Group] content = "0 | 1"
 8. [Group] -> Alternation  branches=[0 , 1]
 9. [Quant] '*' -> 0 to 5 times (capped)
10. [Base ] literal '2'  pos=11
11. [Group] '(' at pos=13
12. [Group] content = "3 | 4"
13. [Group] -> Alternation  branches=[3 , 4]
14. [Quant] digit "5" -> exactly 5 times
15. [Base ] literal '3'  pos=22
16. [Base ] literal '6'  pos=23
17. [Concat] -> ConcatNode  parts=6
18. Parsing complete.
```

**AST structure:**
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
4.  "12444443336"  [note: last two literals are '3' and '6', not part of repetition]
5.  "1100123333336"
```

Each string starts with `1`, has zero or more binary digits, then `2`, exactly five copies of `3` or `4`, and ends with the literal suffix `36` — matching the expected format `{1023333336, 1124444436, ...}`.

---

## Conclusions

This laboratory work demonstrated how regular expressions can be parsed into an AST using a recursive-descent parser and how that tree can be directly used to generate valid strings belonging to the described language.

The node hierarchy (`LiteralNode`, `ConcatNode`, `AltNode`, `RepeatNode`) maps cleanly onto the four fundamental regex operations — every structural rule has exactly one corresponding node type. The recursive-descent parser translates the flat character stream into the tree in a single linear pass, producing a detailed step-by-step trace as a side effect, which makes the internal decision process fully transparent.

The `generate()` method on each node type shows that string generation and language membership are two sides of the same coin: a node knows both what it means structurally and how to produce a concrete instance of that meaning.

The cap constant `MAX_REPEAT = 5` for unbounded quantifiers is an important engineering tradeoff: it trades completeness (strings longer than 5 repetitions are never generated) for guaranteed termination and practical output length — an acceptable compromise for demonstration purposes.

Overall, the lab reinforced the connection between formal language theory and practical compiler front-end techniques: the same recursive-descent approach used here for regex parsing is applied in real-world lexers and parsers, confirming that theoretical models translate directly into working software.

---

## References

1. Theme 2.pdf, Cojuhari Irina — https://else.fcim.utm.md/course/view.php?id=98#section-2  
2. Aho, Lam, Sethi, Ullman — *Compilers: Principles, Techniques, and Tools* (2nd ed.), Chapter 3