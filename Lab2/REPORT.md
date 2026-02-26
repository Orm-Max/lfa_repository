# Laboratory Work #2: Determinism in Finite Automata. Conversion from NDFA to DFA. Chomsky Hierarchy.

**Course:** Formal Languages & Finite Automata  
**Author:** Maxim Ormanji FAF-242  
**Date:** February 26, 2026

---

## Theory

A **finite automaton** is a mathematical model used to represent processes with a defined start and a set of possible end states. It is closely related to state machines, sharing similar structure and purpose. The word *finite* indicates that the modeled process has both a beginning and an ending.

Finite automata come in two varieties: **deterministic (DFA)** and **non-deterministic (NFA/NDFA)**. In a DFA, each state has exactly one transition per input symbol, making the next state always predictable. In an NDFA, a single state may have multiple transitions for the same symbol — or even none — leading to non-determinism. In systems theory, determinism characterizes how predictable a system is; when random variables are involved, the system is considered stochastic or non-deterministic.

Although NDFAs appear more powerful due to their flexibility, both models are equivalent in expressive power: any language recognized by an NDFA can also be recognized by a DFA. This equivalence is established through the **subset construction (powerset) algorithm**, which converts an NDFA into an equivalent DFA by grouping sets of NDFA states into single DFA states.

The **Chomsky hierarchy** classifies formal grammars into four types based on the form of their production rules:

- **Type 0 (Unrestricted):** No restrictions on production rules.
- **Type 1 (Context-Sensitive):** Each production rule satisfies |LHS| ≤ |RHS|, except for S → ε when S does not appear on any right-hand side.
- **Type 2 (Context-Free):** Every left-hand side is a single non-terminal.
- **Type 3 (Regular):** Productions are either right-linear (A → aB or A → a) or left-linear (A → Ba or A → a).

---

## Objectives

1. Provide a function in the `Grammar` class that classifies the grammar based on the Chomsky hierarchy.

2. Using Variant 17's finite automaton definition, implement the following:
   - **a.** Convert the finite automaton to a regular grammar.
   - **b.** Determine whether the FA is deterministic or non-deterministic.
   - **c.** Implement functionality to convert an NDFA to a DFA.

---

## Variant 17 — Finite Automaton Definition

The automaton used in this laboratory work is defined as follows:

- **States:** Q = {q0, q1, q2, q3}
- **Alphabet:** Σ = {a, b, c}
- **Start state:** q0
- **Final states:** F = {q3}
- **Transition function:**

---

## Implementation Description

### Grammar Class — Chomsky Hierarchy Classification

The `Grammar` class was extended with a `classify()` method that determines the type of the grammar according to the Chomsky hierarchy. Classification proceeds from the most restrictive (Type 3) to the least restrictive (Type 0), returning the first matching type.

The `isType3()` method tokenizes each right-hand side using greedy longest-match against the set of non-terminals, then checks whether all non-terminals appear either exclusively at the end (right-linear) or exclusively at the beginning (left-linear) of each production. A helper `tokenize()` method handles multi-character non-terminal symbols correctly.

```java
public String classify() {
    Object[] result = isType3();
    if ((boolean) result[0]) return (String) result[1];
    if (isType2()) return "Type 2 (Context-Free)";
    if (isType1()) return "Type 1 (Context-Sensitive)";
    return "Type 0 (Unrestricted)";
}
```

The `isType2()` check verifies that every left-hand side is a single non-terminal. The `isType1()` check verifies that no production shrinks the string (|RHS| ≥ |LHS|), except for S → ε when S is the start symbol.

### FiniteAutomaton Class — Determinism Check

The `isDeterministic()` method iterates over all transitions and checks whether any state-symbol pair leads to more than one next state:

```java
public boolean isDeterministic() {
    for (Map<String, Set<String>> bySymbol : transMap.values()) {
        for (Set<String> nextStates : bySymbol.values()) {
            if (nextStates.size() > 1) return false;
        }
    }
    return true;
}
```

For Variant 17, the transition `δ(q0, a) = {q0, q1}` immediately causes this method to return `false`.

### NDFA to DFA Conversion — Subset Construction

The `toDFA()` method implements the classic powerset (subset construction) algorithm. Each DFA state corresponds to a **set of NDFA states**. The algorithm begins with the start set `{q0}` and uses a queue to process reachable state sets:

```java
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
                if (transMap.containsKey(state) && transMap.get(state).containsKey(symbol))
                    nextSet.addAll(transMap.get(state).get(symbol));
            }
            dfaTransitions.get(currentKey).put(symbol, nextSet);
            if (dfaStates.stream().noneMatch(s -> s.equals(nextSet))) {
                dfaStates.add(nextSet);
                queue.add(nextSet);
            }
        }
    }
    // ...
}
```

A DFA state is a final state if it contains at least one NDFA final state. State sets are keyed by their sorted, stringified form (e.g., `[q0, q1]`) to ensure consistent lookup.

### FA to Regular Grammar Conversion

The `toRegularGrammar()` method produces a right-linear grammar directly from the automaton's transition map. For each transition `δ(state, symbol) = nextState`, the production `state → symbol nextState` is added. If `nextState` is a final state, an additional production `state → symbol` is added to allow the derivation to terminate:

```java
public Grammar toRegularGrammar() {
    Map<String, List<String>> productions = new HashMap<>();
    for (Map.Entry<String, Map<String, Set<String>>> outer : transMap.entrySet()) {
        String state = outer.getKey();
        for (Map.Entry<String, Set<String>> inner : outer.getValue().entrySet()) {
            String symbol = inner.getKey();
            for (String nextState : inner.getValue()) {
                productions.computeIfAbsent(state, k -> new ArrayList<>())
                           .add(symbol + nextState);
                if (finalStates.contains(nextState))
                    productions.get(state).add(symbol);
            }
        }
    }
    return new Grammar(states, alphabet, productions, startState);
}
```

---

## Results and Execution

The full program output is:

```
Is DFA? false

DFA Transitions:
  [q0] --a--> [q1, q0]
  [q0, q1] --a--> [q1, q2, q0]
  [q0, q1] --b--> [q1]
  [q0, q1, q2] --a--> [q1, q2, q0]
  [q0, q1, q2] --b--> [q1, q3]
  [q1] --a--> [q2]
  [q1] --b--> [q1]
  [q1, q3] --a--> [q2]
  [q1, q3] --b--> [q1]
  [q2] --a--> [q0]
  [q2] --b--> [q3]

DFA States:
  [q0]
  [q1, q0]
  [q1, q2, q0]
  [q1]
  [q1, q3]
  [q2]
  [q3]

DFA Final States:
  [q1, q3]
  [q3]

Regular Grammar Productions:
  q1 -> bq1 | aq2
  q2 -> aq0 | bq3 | b
  q0 -> aq1 | aq0

Grammar Classification: Type 3 (Regular - right-linear)
```

### Analysis of the NDFA → DFA Conversion

The original NDFA has 4 states. After applying the subset construction, the resulting DFA has **7 reachable states**: `[q0]`, `[q0, q1]`, `[q0, q1, q2]`, `[q1]`, `[q1, q3]`, `[q2]`, and `[q3]`. The empty set state (dead state) is not shown since it has no outgoing transitions that reach final states.

The two DFA final states are `[q1, q3]` and `[q3]` — both contain the NDFA final state `q3`.

The conversion is correct because every NDFA state-set is reachable and each DFA transition is computed as the union of all NDFA transitions from the constituent states. No non-determinism remains: for each DFA state and each input symbol, there is exactly one (possibly empty) target state.

### Analysis of the Regular Grammar

The converted grammar has three non-trivial non-terminals (`q0`, `q1`, `q2`) and the following productions:

- **q0 → aq1 | aq0** — from `q0`, reading `a` can lead to `q1` or stay in `q0`
- **q1 → bq1 | aq2** — from `q1`, reading `b` loops back; reading `a` moves to `q2`
- **q2 → aq0 | bq3 | b** — from `q2`, reading `a` returns to `q0`; reading `b` reaches final `q3` (generating both `bq3` and `b`)

All productions are right-linear (each right-hand side is either a terminal or a terminal followed by a single non-terminal), confirming the grammar is **Type 3 (Regular — right-linear)**.

---

## Conclusions

This laboratory work extended the previous implementation with three new capabilities: Chomsky hierarchy classification, determinism checking, and NDFA-to-DFA conversion via subset construction. The Variant 17 automaton was confirmed to be non-deterministic due to the transition `δ(q0, a) = {q0, q1}`, which produces two possible successor states for one symbol. The subset construction algorithm correctly resolved this by creating composite DFA states, expanding the state count from 4 to 7 while preserving the recognized language. The conversion to a regular grammar demonstrated once again the direct correspondence between finite automata and Type-3 grammars: each state becomes a non-terminal, each transition becomes a production rule, and transitions into final states produce terminal-only alternatives. The grammar classification confirmed that the derived grammar is right-linear, consistent with theory. Overall, the lab reinforced the practical equivalence between NDFAs and DFAs, and showed how abstract automaton-theoretic results translate directly into working code.

---

## References

1. Theme 2.pdf, Cojuhari Irina / Vasile Drumea — https://else.fcim.utm.md/course/view.php?id=98#section-2