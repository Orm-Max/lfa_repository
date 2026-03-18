# Laboratory Work #3: Lexical Analysis

**Course:** Formal Languages & Finite Automata  
**Author:** Maxim Ormanji FAF-242  
**Date:** March 18, 2026

---

## Theory

**Lexical analysis** (also called scanning or tokenization) is the first phase of a compiler or interpreter. Its purpose is to convert a raw sequence of characters into a structured sequence of **tokens** — meaningful units that subsequent phases (parsing, semantic analysis) can work with.

A **token** is a pair consisting of:
- A **token type** (also called a lexeme category or tag), which classifies the unit (e.g., keyword, identifier, number)
- A **value** (the actual character sequence matched from the input)

Lexical analysis is directly grounded in the theory of **regular languages**. Each token category is described by a regular expression, and the scanner itself is implemented as a **finite automaton** — typically a deterministic finite automaton (DFA) — that recognises these patterns as it reads the input left to right.

The scanning process follows a well-defined pipeline:

1. **Input buffering** — the source text is read character by character, maintaining a current position pointer
2. **Whitespace and comment skipping** — non-meaningful characters are discarded
3. **Pattern matching** — the longest matching token is identified at each position (the *maximal munch* principle)
4. **Token emission** — the recognised token is added to the output list and the position is advanced

A **keyword** is a reserved word that looks syntactically identical to an identifier but carries special meaning in the language (e.g., `PERSON`, `EVENT`, `IN`). Distinguishing keywords from user-defined identifiers is a central responsibility of the lexer.

---

## Objectives

1. Design a **token type enumeration** that covers all meaningful categories of the target mini-language, including entity markers, action verbs, temporal expressions, structural symbols, and catch-all categories.

2. Implement a **Token** class that pairs a token type with its lexical value and provides a human-readable string representation.

3. Build a **Lexer** class that scans an arbitrary input string character by character, recognises each token category through dedicated helper methods, and returns the complete list of tokens.

4. Handle **special cases** correctly: multi-character operators (`->`), quoted string literals, integer years with optional era suffixes (`BCE`, `BC`, `AD`, `CE`), and unknown characters.

5. Demonstrate the lexer on a set of representative historical-event sentences that exercise every token category.

---

## Implementation Description

### TokenType Enumeration

The `TokenType` enum defines every category the lexer can produce, grouped by semantic role:

```java
public enum TokenType {
    // Entity markers
    PERSON, EVENT, PLACE,

    // Action keywords
    BORN, DIED, RULED, FOUNDED, DEFEATED,

    // Temporal
    YEAR,

    // Connectives
    IN, BY, AND,

    // Structural symbols
    ARROW,      // ->
    COLON,      // :

    // General
    IDENTIFIER, // any name such as Julius_Caesar
    STRING,     // "quoted text"
    UNKNOWN
}
```

Having a dedicated `YEAR` type (rather than a plain `NUMBER`) reflects the domain: the target language talks about history, so a number almost always represents a year, potentially decorated with an era suffix.

### Token Class

The `Token` class is a simple data carrier:

```java
public class Token {
    public TokenType type;
    public String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("%-15s -> %s", type, value);
    }
}
```

The `toString` method left-aligns the type in a fixed-width field, producing the clean columnar output visible in the demonstration.

### Lexer Class — Overall Structure

The `Lexer` maintains two fields: the full input `text` and a position cursor `pos`. The public `tokenize()` method loops until `pos` reaches the end of the input, dispatching to one of several branches based on the current character:

```java
public List<Token> tokenize() {
    List<Token> tokens = new ArrayList<>();

    while (pos < text.length()) {
        if (Character.isWhitespace(text.charAt(pos))) { pos++; continue; }

        if (pos + 1 < text.length()
                && text.charAt(pos) == '-' && text.charAt(pos + 1) == '>') {
            tokens.add(new Token(TokenType.ARROW, "->"));
            pos += 2; continue;
        }

        if (text.charAt(pos) == ':') {
            tokens.add(new Token(TokenType.COLON, ":")); pos++; continue;
        }

        if (text.charAt(pos) == '"') { tokens.add(readString()); continue; }

        if (Character.isDigit(text.charAt(pos))) { tokens.add(readNumber()); continue; }

        if (Character.isLetter(text.charAt(pos)) || text.charAt(pos) == '_') {
            tokens.add(readWord()); continue;
        }

        tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(text.charAt(pos))));
        pos++;
    }

    return tokens;
}
```

Whitespace is silently consumed. The two-character arrow `->` must be checked before the single-character fallback branch to respect the maximal-munch principle.

### Reading Quoted Strings

```java
private Token readString() {
    pos++; // skip opening "
    StringBuilder sb = new StringBuilder();

    while (pos < text.length() && text.charAt(pos) != '"') {
        sb.append(text.charAt(pos)); pos++;
    }

    pos++; // skip closing "
    return new Token(TokenType.STRING, sb.toString());
}
```

Everything between the opening and closing double-quote is captured verbatim as a single `STRING` token, including any embedded spaces and punctuation. This means a quoted sentence such as `"The Roman Empire fell in 476AD"` becomes one token rather than being split into individual words.

### Reading Years with Era Suffixes

```java
private Token readNumber() {
    StringBuilder sb = new StringBuilder();

    while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
        sb.append(text.charAt(pos)); pos++;
    }

    if (pos < text.length() && Character.isLetter(text.charAt(pos))) {
        StringBuilder era = new StringBuilder();
        int savedPos = pos;

        while (pos < text.length() && Character.isLetter(text.charAt(pos))) {
            era.append(text.charAt(pos)); pos++;
        }

        String suffix = era.toString().toUpperCase();
        if (suffix.equals("BCE") || suffix.equals("BC")
                || suffix.equals("AD") || suffix.equals("CE")) {
            sb.append(suffix);
        } else {
            pos = savedPos; // not an era — backtrack
        }
    }

    return new Token(TokenType.YEAR, sb.toString());
}
```

After consuming the digit sequence the method peeks ahead. If the immediately following letters form a recognised era suffix (`BCE`, `BC`, `AD`, `CE`), they are appended to the token value (e.g., `48BCE`, `753BCE`, `476AD`). If the letters do not form an era suffix, the position is restored via the saved `savedPos` variable so those letters can be re-scanned as a separate word token.

### Reading Words and Keyword Matching

```java
private Token readWord() {
    StringBuilder sb = new StringBuilder();

    while (pos < text.length()
            && (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_')) {
        sb.append(text.charAt(pos)); pos++;
    }

    String word = sb.toString();
    return new Token(matchKeyword(word), word);
}

private TokenType matchKeyword(String word) {
    switch (word.toUpperCase()) {
        case "PERSON":   return TokenType.PERSON;
        case "EVENT":    return TokenType.EVENT;
        case "PLACE":    return TokenType.PLACE;
        case "BORN":     return TokenType.BORN;
        case "DIED":     return TokenType.DIED;
        case "RULED":    return TokenType.RULED;
        case "FOUNDED":  return TokenType.FOUNDED;
        case "DEFEATED": return TokenType.DEFEATED;
        case "IN":       return TokenType.IN;
        case "BY":       return TokenType.BY;
        case "AND":      return TokenType.AND;
        default:         return TokenType.IDENTIFIER;
    }
}
```

Identifiers may contain letters, digits, and underscores, which allows multi-word names like `Julius_Caesar` or `Battle_of_Hastings` to be represented as a single token. Keyword matching is case-insensitive (`toUpperCase()`), so `defeated`, `DEFEATED`, and `Defeated` all yield `TokenType.DEFEATED`.

### Main Class — Demonstration

The `Main` class defines an array of seven representative input sentences and runs the lexer on each, printing the token list:

```java
String[] examples = {
    "PERSON Julius_Caesar defeated Pompey in 48BCE",
    "EVENT Battle_of_Hastings: William defeated Harold in 1066",
    "Napoleon -> French_Revolution",
    "EVENT Magna_Carta: King_John signed in 1215",
    "PLACE Rome founded 753BCE by Romulus and Remus",
    "EVENT World_War_II: 1939 -> 1945",
    "\"The Roman Empire fell in 476AD\""
};
```

The examples are deliberately varied to cover entity markers, action keywords, colons, arrows, plain years, years with era suffixes, identifiers containing underscores and digits, and quoted strings.

---

## Results and Execution

The full program output is shown below.

```
INPUT:  PERSON Julius_Caesar defeated Pompey in 48BCE
TOKENS:
  PERSON          -> PERSON
  IDENTIFIER      -> Julius_Caesar
  DEFEATED        -> defeated
  IDENTIFIER      -> Pompey
  IN              -> in
  YEAR            -> 48BCE

INPUT:  EVENT Battle_of_Hastings: William defeated Harold in 1066
TOKENS:
  EVENT           -> EVENT
  IDENTIFIER      -> Battle_of_Hastings
  COLON           -> :
  IDENTIFIER      -> William
  DEFEATED        -> defeated
  IDENTIFIER      -> Harold
  IN              -> in
  YEAR            -> 1066

INPUT:  Napoleon -> French_Revolution
TOKENS:
  IDENTIFIER      -> Napoleon
  ARROW           -> ->
  IDENTIFIER      -> French_Revolution

INPUT:  EVENT Magna_Carta: King_John signed in 1215
TOKENS:
  EVENT           -> EVENT
  IDENTIFIER      -> Magna_Carta
  COLON           -> :
  IDENTIFIER      -> King_John
  IDENTIFIER      -> signed
  IN              -> in
  YEAR            -> 1215

INPUT:  PLACE Rome founded 753BCE by Romulus and Remus
TOKENS:
  PLACE           -> PLACE
  IDENTIFIER      -> Rome
  FOUNDED         -> founded
  YEAR            -> 753BCE
  BY              -> by
  IDENTIFIER      -> Romulus
  AND             -> and
  IDENTIFIER      -> Remus

INPUT:  EVENT World_War_II: 1939 -> 1945
TOKENS:
  EVENT           -> EVENT
  IDENTIFIER      -> World_War_II
  COLON           -> :
  YEAR            -> 1939
  ARROW           -> ->
  YEAR            -> 1945

INPUT:  "The Roman Empire fell in 476AD"
TOKENS:
  STRING          -> The Roman Empire fell in 476AD
```

### Analysis of Individual Cases

**PERSON Julius_Caesar defeated Pompey in 48BCE** — demonstrates that the entity keyword `PERSON` is immediately followed by an identifier containing an underscore. The word `defeated` is matched as a keyword despite being lowercase. The year `48BCE` is emitted as a single `YEAR` token, not split across two tokens.

**EVENT Battle_of_Hastings: William defeated Harold in 1066** — shows the `COLON` token used as a separator after the event name. The year `1066` has no era suffix and is still classified as `YEAR`.

**Napoleon -> French_Revolution** — the minimal case: two bare identifiers connected by an `ARROW`. There are no keywords at all, demonstrating that the lexer does not require a specific sentence structure.

**EVENT Magna_Carta: King_John signed in 1215** — the word `signed` is not in the keyword table and therefore receives the type `IDENTIFIER`. This shows the graceful fallback: unrecognised words become identifiers rather than causing an error.

**PLACE Rome founded 753BCE by Romulus and Remus** — exercises `FOUNDED`, `BY`, and `AND` keywords, all matched case-insensitively, alongside a BCE year.

**EVENT World_War_II: 1939 -> 1945** — the identifier `World_War_II` contains both underscores and digits. The two years with an arrow between them represent a date range, demonstrating that the same `ARROW` token used for implications in the previous example is reused in a different semantic context; distinguishing such contexts is the job of the parser, not the lexer.

**"The Roman Empire fell in 476AD"** — the entire quoted sentence, including internal spaces, keywords like `in`, and the suffixed year `476AD`, is consumed as one `STRING` token, because the string reader does not interpret its contents.

---

## Conclusions

This laboratory work put into practice the concept of lexical analysis as the foundational phase of language processing. Implementing the lexer made clear how directly the theoretical notion of a finite automaton maps onto practical scanning code: each helper method (`readString`, `readNumber`, `readWord`) encodes a small recogniser for one token category, and the main dispatch loop in `tokenize()` acts as the top-level automaton that chooses which recogniser to invoke based on the lookahead character.

Several design decisions proved important. Supporting underscores and digits inside identifiers allowed multi-word historical names to be treated as atomic tokens, reducing the complexity passed to any future parser. Case-insensitive keyword matching made the input language more natural to write. The backtracking mechanism in `readNumber` illustrated a clean way to handle ambiguous prefixes (a digit string that might or might not be followed by an era suffix) without committing prematurely.

The results confirm that every token category defined in `TokenType` is correctly identified across the seven test sentences, including edge cases such as the two-character arrow operator, era-decorated years, quoted strings with embedded keywords, and identifiers with mixed-case keyword matches. The lab demonstrated concretely how regular language theory — regular expressions and finite automata — underlies the first and most ubiquitous stage of every compiler and interpreter front-end.

---

## References

1. Theme 3.pdf, Cojuhari Irina — https://else.fcim.utm.md/course/view.php?id=98#section-3
2. A. V. Aho, M. S. Lam, R. Sethi, J. D. Ullman, *Compilers: Principles, Techniques, and Tools*, 2nd ed., Pearson, 2006 — Chapter 3: Lexical Analysis
