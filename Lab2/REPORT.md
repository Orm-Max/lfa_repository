
**Analysis**:

- The original automaton is **non-deterministic** (NFA) — confirmed by `Is DFA? false` (q0 has two possible transitions on 'a').  
- The subset construction produced a **DFA** with 7 states (subsets of {q0,q1,q2,q3}).  
- Accepting states in the DFA are those subsets that contain q3.  
- The reverse conversion (automaton → grammar) produced a clean **right-linear regular grammar**.  
- All productions are of the form A → aB or A → a, which confirms it is a valid Type-3 grammar (right-linear variant).

---

## Conclusions

This laboratory work demonstrated the equivalence of regular grammars and finite automata in both directions:

- Subset construction (NFA → DFA) systematically eliminates non-determinism while preserving the accepted language.  
- Conversion from finite automaton to right-linear grammar is straightforward: each transition δ(p, a) = {q₁, q₂, …} generates productions p → aq₁ | aq₂ | …, and accepting transitions add p → a.  
- The resulting grammar was correctly classified as Type 3 (right-linear), confirming theoretical expectations.  

The implemented classes provide reusable components for working with regular languages: NFA/DFA manipulation, grammar generation, and type classification. These algorithms form the foundation for lexical analyzers in compilers, pattern matching engines, and many other practical systems in computer science.

---

## References

1. Theme 2.pdf, Cojuhari Irina https://else.fcim.utm.md/course/view.php?id=98#section-2 