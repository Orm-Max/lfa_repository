
All generated strings fully conform to their respective regular expressions.

---

## Analysis of the Implementation

The parser correctly interprets the custom syntax used in the laboratory task:
- character classes inside `( )` without `|`,
- alternation with top-level `|`,
- immediate digit quantifiers after groups,
- spaces as visual separators only.

**Faced difficulties and solutions:**
- Distinguishing “digit after group = quantifier” from “digit as next literal” → solved by checking `wasGroup` flag **before** skipping spaces.
- Correct splitting of alternations while ignoring nested parentheses → implemented `splitByTopLevelPipe` with depth counter.
- Preventing extremely long strings → `MAX_REPEAT = 5` cap on `+` and `*`.
- Bonus trace requirement → every parsing decision is logged in `steps`.

The generated strings are always valid by construction because they are produced directly from the AST that mirrors the original regular expression.

---

## Conclusions

This laboratory work demonstrated the practical power of regular expressions as a generative mechanism for regular languages. By building a complete parser → AST → random generator pipeline, we showed that a regular expression can be treated as a compact representation of a right-linear grammar or a finite automaton (connection to Laboratories #1 and #2). The dynamic parsing approach (instead of hard-coded generation) proves that the same engine can process any regex of the supported syntax. The step-by-step trace (bonus) makes the internal logic transparent and educational. Overall, the lab reinforced the equivalence between regular expressions, regular grammars, finite automata, and lexical analyzers, while providing a reusable tool for generating test data for any regular pattern.

---

## References

1. Theme 1.pdf, Cojuhari Irina https://else.fcim.utm.md/course/view.php?id=98#section-1  
2. Aho, Sethi, Ullman – “Compilers: Principles, Techniques, and Tools” (Dragon Book), Chapter 3 – Lexical Analysis & Chapter 4 – Syntax Analysis  
3. Friedl, J. – “Mastering Regular Expressions”, 3rd Edition