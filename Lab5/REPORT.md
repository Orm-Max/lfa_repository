# Laboratory Work #5: Chomsky Normal Form

**Course:** Formal Languages & Finite Automata  
**Author:** Maxim Ormanji, FAF-242  
**Variant:** 17  
**Date:** April 23, 2026

---

## Theory

### What Is Chomsky Normal Form?

A context-free grammar (CFG) is said to be in **Chomsky Normal Form (CNF)** if every production rule takes exactly one of two forms:

- **A → BC** — a non-terminal expands into exactly two non-terminals
- **A → a** — a non-terminal expands into exactly one terminal symbol

Additionally, if the empty string **ε** belongs to the language, only the start symbol S may have the production **S → ε**, and S must not appear on the right-hand side of any rule.

Every context-free language (that does not consist solely of the empty string) has a grammar in CNF. This was proved by Noam Chomsky and the normal form is named after him. The significance of CNF lies in its uniformity: because every rule is either binary or terminal, parse trees are always full binary trees. This structure is what makes the CYK algorithm — which tests membership of a string in O(n³) time — so elegant, since it can iterate over all splits of a substring into exactly two halves.

### Why Normalize a Grammar?

Raw context-free grammars — as they arise naturally when specifying programming language syntax or modelling other formal systems — often include:

- **ε-productions** (A → ε): rules that derive the empty string, complicating analysis.
- **Unit productions** (A → B): renaming rules that add indirection without adding generative power.
- **Inaccessible symbols**: non-terminals that can never be reached from the start symbol.
- **Non-productive symbols**: non-terminals from which no terminal string can ever be derived.
- **Long productions** (A → BCD…): rules with three or more symbols on the right-hand side.

Normalization removes each of these in a defined order, producing a clean CNF grammar that is provably equivalent (generates the same language) and is amenable to algorithmic manipulation.

### Normalization Steps

The standard normalization pipeline is:

1. **Eliminate ε-productions.** Find all nullable non-terminals (those that can derive ε directly or indirectly). For every production containing a nullable symbol, add new productions that include all combinations where the nullable symbol is optionally omitted. Remove the ε-productions themselves.

2. **Eliminate unit productions (renamings).** A unit production A → B is replaced by copying all of B's productions directly into A, then removing A → B. This is repeated to fixpoint.

3. **Eliminate inaccessible symbols.** Perform a reachability analysis starting from the start symbol S. Any non-terminal that cannot be reached is removed along with all its productions.

4. **Eliminate non-productive symbols.** Iteratively identify non-terminals that can derive at least one terminal string. Any non-terminal that remains non-productive after fixpoint is removed, along with productions that reference it.

5. **Convert to CNF.** Two sub-steps: (a) replace terminals appearing in productions of length ≥ 2 with fresh non-terminals (e.g., introduce T_a → a and substitute T_a for every `a` that appears alongside other symbols); (b) binarize any production of length ≥ 3 by introducing fresh non-terminals to represent the tail of the production, reducing it to a binary rule.

---

## Objectives

1. Learn about Chomsky Normal Form (CNF).
2. Get familiar with the approaches for normalizing a context-free grammar.
3. Implement a method for normalizing an input grammar by the rules of CNF.
   1. The implementation must be encapsulated in a method with an appropriate signature (and ideally in an appropriate class).
   2. The implemented functionality must be executed and tested.
   3. **Bonus:** Make the function accept any grammar, not only the one from the student's variant.

**Variant 17 grammar:**

G = (V_N, V_T, P, S)  
V_N = {S, A, B, C, D, E}  
V_T = {a, b}

Productions P:

| # | Rule        | # | Rule        | # | Rule       |
|---|-------------|---|-------------|---|------------|
| 1 | S → aA      | 5 | A → BC      | 9 | C → ε      |
| 2 | S → AC      | 6 | A → aD      | 10| C → BA     |
| 3 | A → a       | 7 | B → b       | 11| E → aB     |
| 4 | A → ASC     | 8 | B → bA      | 12| D → abC    |

---

## Implementation Description

The implementation is contained in a single Java file `CNFConverter.java`. The grammar is represented as a `Map<String, List<List<String>>>`, mapping each non-terminal string to its list of productions, where each production is itself a `List<String>` of symbol strings. This representation makes it straightforward to add, remove, and iterate over productions programmatically. The five normalization steps are implemented as static methods (`eliminateEpsilon`, `eliminateRenamings`, `eliminateInaccessible`, `eliminateNonProductive`, `convertToCNF`) that mutate the shared grammar map in sequence.

The `isNonTerminal(String sym)` helper distinguishes non-terminals from terminals by checking whether the first character of the symbol string is an uppercase letter — a convention maintained throughout.

### Step 1 — Eliminate Epsilon Productions

`eliminateEpsilon()` begins by scanning every production for an empty list (the internal representation of ε). Any variable that directly produces ε is immediately added to the `nullable` set. A fixed-point loop then expands this set: if every symbol in some production of variable V is already nullable, then V is nullable too.

Once the nullable set is stable, `generateCombinations()` handles each production by enumerating every non-empty subset of nullable positions in the right-hand side using a bitmask. For a production [A, S, C] where C is nullable, the subsets `{C}` and `{A,S,C}` (omitting C, or omitting both A and C if A were also nullable) are all generated. Duplicate productions are filtered out, and the ε-production itself is removed from the result.

```java
// Enumerate all ways to omit nullable symbols from a production
int n = nullablePositions.size();
for (int mask = 1; mask < (1 << n); mask++) {
    Set<Integer> toRemove = new HashSet<>();
    for (int j = 0; j < n; j++) {
        if ((mask & (1 << j)) != 0) toRemove.add(nullablePositions.get(j));
    }
    List<String> newProd = new ArrayList<>();
    for (int i = 0; i < prod.size(); i++)
        if (!toRemove.contains(i)) newProd.add(prod.get(i));
    if (!newProd.isEmpty() && !results.contains(newProd)) results.add(newProd);
}
```

### Step 2 — Eliminate Renamings

`eliminateRenamings()` runs a fixed-point loop over all variables. A unit production is detected as a production whose `size() == 1` and whose sole symbol is a non-terminal. When found, all of the target variable's productions are copied directly into the current variable (deduplicating) and the unit production is removed. The loop repeats until no more unit productions remain.

### Step 3 — Eliminate Inaccessible Symbols

`eliminateInaccessible()` performs a BFS/DFS from S over the grammar's production rules. Starting with `accessible = {S}`, it repeatedly scans productions of every accessible variable and adds any non-terminal symbols found on right-hand sides. When the set stabilizes, any variable not in `accessible` is removed from the grammar map entirely via `grammar.keySet().retainAll(accessible)`.

### Step 4 — Eliminate Non-Productive Symbols

`eliminateNonProductive()` builds the productive set bottom-up. A variable is productive if it has at least one production where every non-terminal on the right-hand side is already known to be productive (terminals are vacuously productive). Variables that never become productive are removed, and any remaining production that references a non-productive non-terminal is also pruned.

```java
static boolean isProductiveProduction(List<String> prod, Set<String> productive) {
    for (String sym : prod)
        if (isNonTerminal(sym) && !productive.contains(sym)) return false;
    return true;
}
```

### Step 5 — Convert to CNF

`convertToCNF()` operates in two sub-steps.

**5a — Terminal isolation.** For every production of length ≥ 2, each terminal symbol is replaced by a fresh non-terminal. The naming convention is `"T" + sym.toUpperCase()`, so terminal `a` becomes non-terminal `TA`, and `b` becomes `TB`. The corresponding unit rules `TA → a` and `TB → b` are added to the grammar once each (via `terminalVarMap` to avoid duplicates).

**5b — Binarization.** A fixed-point loop iterates over all variables. Any production of length ≥ 3 is split: the first symbol becomes the left child, and the remaining tail is checked against a `seqCache` (a map from comma-joined tail key to fresh variable name). If the tail has been seen before, the existing auxiliary variable is reused; otherwise a new `X1`, `X2`, ... variable is created with that tail as its single production. The original long production is replaced by the binary rule `[first, restVar]`. The loop repeats until no production exceeds length 2.

```java
String restKey = String.join(",", rest);
String restVar = seqCache.get(restKey);
if (restVar == null) {
    restVar = "X" + newVarCounter++;
    seqCache.put(restKey, restVar);
    grammar.put(restVar, singletonList(new ArrayList<>(rest)));
}
List<String> binaryProd = List.of(first, restVar);
```

The `seqCache` optimization is important: it prevents the explosion of redundant auxiliary variables when the same tail sequence appears in multiple productions (as happens with `A → ASC` and `S → ASC` sharing the tail `[S, C]`).

---

## Results and Execution

### Original Grammar

```
S -> aA | AC
A -> a | ASC | BC | aD
B -> b | bA
C -> epsilon | BA
D -> abC
E -> aB
```

---

### Step 1: After Eliminating Epsilon Productions

Nullable variables: **{C}**

C is the only nullable variable (C → ε directly; no other variable has an all-nullable right-hand side).

For each production containing C, an additional production is generated with C omitted:

- `S → AC` gains `S → A` (C omitted)
- `A → ASC` gains `A → AS` (C omitted)
- `A → BC` gains `A → B` (C omitted)
- `D → abC` gains `D → ab` (C omitted)

The ε-production `C → ε` is then removed.

```
S -> aA | AC | A
A -> a | ASC | AS | BC | B | aD
B -> b | bA
C -> BA
D -> abC | ab
E -> aB
```

---

### Step 2: After Eliminating Renamings

Unit productions present: **S → A** and **A → B**.

Processing `S → A`: all of A's productions are copied into S, then `S → A` is removed. This introduces `S → B` (from A's productions), which is itself a unit production.

Processing `S → B` (discovered in the same pass): all of B's productions are copied into S, then `S → B` is removed.

Processing `A → B`: all of B's productions are copied into A, then `A → B` is removed.

```
S -> aA | AC | a | ASC | AS | BC | aD | b | bA
A -> a | ASC | AS | BC | aD | b | bA
B -> b | bA
C -> BA
D -> abC | ab
E -> aB
```

---

### Step 3: After Eliminating Inaccessible Symbols

Reachability analysis starting from S:

- S references: **A, C, B, D** (from its productions)
- A references: A, S, C, B, D (all already accessible)
- B references: A (already accessible)
- C references: B, A (already accessible)
- D references: C (already accessible)
- **E** is never referenced by any accessible variable.

Accessible symbols: **{S, A, B, C, D}**  
Inaccessible removed: **{E}**

```
S -> aA | AC | a | ASC | AS | BC | aD | b | bA
A -> a | ASC | AS | BC | aD | b | bA
B -> b | bA
C -> BA
D -> abC | ab
```

---

### Step 4: After Eliminating Non-Productive Symbols

Productive set built bottom-up:

- **B** productive (B → b, terminal string)
- **A** productive (A → a, terminal string)
- **S** productive (S → a, S → b)
- **C** productive (C → BA, both B and A productive)
- **D** productive (D → ab, terminal string)

All five remaining variables are productive. No variables or productions are removed.

```
S -> aA | AC | a | ASC | AS | BC | aD | b | bA
A -> a | ASC | AS | BC | aD | b | bA
B -> b | bA
C -> BA
D -> abC | ab
```

---

### Step 5: Chomsky Normal Form

**Sub-step 5a — Terminal isolation:**

Terminal `a` → introduced `TA`; terminal `b` → introduced `TB`.

Productions of length ≥ 2 that contain terminals have those terminals replaced:

| Original            | After 5a         |
|---------------------|------------------|
| S → aA              | S → TA A         |
| S → aD              | S → TA D         |
| S → bA              | S → TB A         |
| A → aD              | A → TA D         |
| A → bA              | A → TB A         |
| B → bA              | B → TB A         |
| D → abC             | D → TA TB C      |
| D → ab              | D → TA TB        |

Pure NT productions (AC, ASC, AS, BC, BA) are unchanged. Unit terminal productions (A → a, S → a, S → b, B → b) are unchanged (length 1).

**Sub-step 5b — Binarization:**

Long productions (length ≥ 3) after 5a:

| Variable | Long production | Split                        |
|----------|-----------------|------------------------------|
| S        | [A, S, C]       | S → A X1 ; X1 → S C         |
| A        | [A, S, C]       | A → A X1 (reuse X1)          |
| D        | [TA, TB, C]     | D → TA X2 ; X2 → TB C       |

`D → TA TB` has length 2 and is left as-is.

**Final CNF Grammar:**

```
S  -> TA A | AC | a | A X1 | AS | BC | TA D | b | TB A
A  -> a | A X1 | AS | BC | TA D | b | TB A
B  -> b | TB A
C  -> BA
D  -> TA X2 | TA TB
X1 -> SC
X2 -> TB C
TA -> a
TB -> b
```

Every production is now either **A → a** (single terminal) or **A → BC** (exactly two non-terminals) — the grammar is in valid CNF.

---

## Difficulties Faced

**Nullable set propagation.** Finding all nullable variables requires more than one pass: a variable can become nullable indirectly (if all symbols in one of its productions are nullable). The fix is a fixed-point loop that keeps expanding the nullable set until no new variables are added. For this particular grammar only C is nullable, so the indirect case does not trigger, but the general implementation handles it correctly.

**Combinatorial expansion when eliminating ε-productions.** For a production of length n containing k nullable positions, up to 2^k − 1 new productions can be generated (all non-empty subsets). The bitmask enumeration in `generateCombinations()` handles this cleanly without recursion, and a containment check prevents duplicate productions from accumulating.

**Unit production chaining.** After copying A's productions into S due to `S → A`, a new unit production `S → B` appeared (because A had `A → B` before step 2 was applied). This chain had to be resolved in the same fixed-point iteration. The while-loop structure of `eliminateRenamings()` ensures that newly exposed unit productions are always caught.

**Terminal vs. non-terminal naming collisions in CNF.** The naming scheme `T` + uppercase terminal (e.g., `TA` for terminal `a`) could in principle clash with an existing non-terminal if the grammar already contained a variable called `TA`. For Variant 17 this is not an issue since the original non-terminals are single uppercase letters, but a production-grade implementation would need a collision check before assigning these names.

**Shared tails during binarization.** Both `S → ASC` and `A → ASC` produce the same tail `[S, C]` during binarization. Without the `seqCache`, two distinct auxiliary variables would be created for the same sequence, inflating the grammar unnecessarily. The cache maps the comma-joined tail string (e.g., `"S,C"`) to the first variable created for it, allowing subsequent long productions with the same tail to reuse the existing auxiliary rule.

---

## Conclusions

This laboratory work demonstrated the full five-step normalization pipeline that transforms an arbitrary context-free grammar into Chomsky Normal Form. Each step eliminates one class of structural irregularity while preserving the generated language, and the order of the steps matters: for example, eliminating ε-productions before renamings ensures that unit productions created by the expansion (such as `S → A` arising from `S → AC` when C is nullable) are caught in step 2.

The implementation in `CNFConverter.java` achieves the bonus objective by operating on a generic grammar map rather than hardcoded rules. Any CFG whose symbols follow the uppercase/lowercase convention can be normalized by calling `setupGrammar()` with arbitrary productions and then running the five steps in sequence.

The most instructive aspect of this work was watching E disappear silently in step 3. E → aB is a well-formed, productive rule — it can derive strings like `ab`, `abA`, etc. — yet because nothing in S's reachable subgrammar ever references E, the entire rule is dead weight. This illustrates an important point: inaccessibility and non-productivity are orthogonal properties. A symbol can be productive but inaccessible (E in this grammar), or inaccessible and non-productive. The normalization pipeline handles both cases through separate, targeted steps.

The final CNF grammar for Variant 17 contains nine variables (S, A, B, C, D, X1, X2, TA, TB) and 26 productions, compared to the original six variables and twelve productions. The overhead is a known cost of CNF: the uniform binary structure that enables efficient parsing comes at the price of increased grammar size.

---

## References

1. Theme 5.pdf, Cojuhari Irina — https://else.fcim.utm.md/course/view.php?id=98#section-5  
2. Hopcroft, Motwani, Ullman — *Introduction to Automata Theory, Languages, and Computation*, 3rd ed.  
3. Sipser, M. — *Introduction to the Theory of Computation*, 3rd ed.
