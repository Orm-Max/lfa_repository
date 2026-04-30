# Laboratory Work 6: Parser and Abstract Syntax Tree

**Course:** Formal Languages & Finite Automata  
**Student:** Maxim Ormanji, FAF-242
**Topic Author:** Cretu Dumitru  
**Date:** April 28, 2026

---

## Theory

**Parsing** is the step that comes after lexical analysis.  
The lexer breaks the text into tokens, and the parser checks if those tokens follow the rules of the language.

The parser usually builds a tree representation of the input. One common form is the **Abstract Syntax Tree (AST)**.

An **AST** is a simplified tree that keeps the important structure of the sentence:

- what kind of statement we have
- what names appear in the sentence
- what action is described
- what year or relation is present

For example:

```text
PERSON Julius_Caesar defeated Pompey in 48BCE
```

can be understood as:

- statement type: `PERSON`
- subject: `Julius_Caesar`
- action: `defeated`
- object: `Pompey`
- year: `48BCE`

So instead of keeping only a flat token list, the parser builds a structure that is easier to analyze later.

---

## Objectives

1. Use `TokenType` to classify tokens.
2. Use regular expressions in the lexer.
3. Implement AST node classes.
4. Implement a simple parser.
5. Test the program on several example inputs.

---

## Grammar Used

For this laboratory work, the parser accepts a small historical mini-language.

```text
statement          -> person_statement
                  | event_statement
                  | place_statement
                  | relation_statement
                  | quoted_text

person_statement   -> PERSON IDENTIFIER action IDENTIFIER? IN YEAR?
event_statement    -> EVENT IDENTIFIER : YEAR -> YEAR
                  | EVENT IDENTIFIER : IDENTIFIER action IDENTIFIER? IN YEAR?
place_statement    -> PLACE IDENTIFIER FOUNDED YEAR BY IDENTIFIER (AND IDENTIFIER)*
relation_statement -> IDENTIFIER -> IDENTIFIER
quoted_text        -> STRING
```

This grammar is simple on purpose, because the goal of the lab is to understand the basic idea of parsing and AST creation.

---

## Implementation

### 1. TokenType

`TokenType.java` contains the categories used by the lexer and parser:

- `PERSON`, `EVENT`, `PLACE`
- `BORN`, `DIED`, `RULED`, `FOUNDED`, `DEFEATED`
- `IN`, `BY`, `AND`
- `ARROW`, `COLON`
- `YEAR`, `IDENTIFIER`, `STRING`, `WHITESPACE`, `UNKNOWN`

### 2. Lexer

The lexer uses **regular expressions** to recognize tokens.

Examples:

```java
new TokenRule(TokenType.PERSON, "(?i)PERSON\\b");
new TokenRule(TokenType.YEAR, "\\d+(?:BCE|BC|AD|CE)?\\b");
new TokenRule(TokenType.IDENTIFIER, "[A-Za-z_][A-Za-z0-9_]*");
```

Important idea:

- `(?i)` makes the keyword matching case-insensitive
- `\\d+(?:BCE|BC|AD|CE)?` recognizes years like `1066`, `48BCE`, `476AD`
- identifiers such as `Julius_Caesar` and `World_War_II` are accepted

The lexer reads the text from left to right and returns a list of tokens.

### 3. AST Nodes

The AST is represented with several beginner-friendly node classes, all grouped inside `ASTNode.java`:

- `QuotedTextNode`
- `RelationNode`
- `ActionStatementNode`
- `EventPeriodNode`
- `PlaceFoundedNode`

Each class implements:

```java
public interface ASTNode {
    String toTreeString(String indent);
}
```

This method prints the tree in a readable form.

### 4. Parser

The parser is implemented in `Parser.java`.

It works with a token list and a current position.

Main methods:

- `parse()` chooses what type of statement we have
- `parsePersonStatement()` parses a `PERSON ...` sentence
- `parseEventStatement()` parses an `EVENT ...` sentence
- `parsePlaceStatement()` parses a `PLACE ...` sentence
- `parseRelation()` parses `IDENTIFIER -> IDENTIFIER`

The parser uses helper methods like:

- `consume(...)` to take the expected token
- `match(...)` to optionally accept a token
- `check(...)` to look at the current token

If the sentence does not respect the grammar, the parser throws a `ParserException`.

---

## Source Code Example

Small example from the parser:

```java
private ASTNode parseRelation() {
    String left = consume(TokenType.IDENTIFIER, "Expected identifier before ->").getValue();
    consume(TokenType.ARROW, "Expected -> between identifiers.");
    String right = consume(TokenType.IDENTIFIER, "Expected identifier after ->").getValue();
    ensureEndOfInput();
    return new RelationNode(left, right);
}
```

This method shows a very simple parsing rule:

1. read the first identifier
2. read the arrow
3. read the second identifier
4. build a `RelationNode`

---

## Program Output

The program was tested on these examples:

```text
PERSON Julius_Caesar defeated Pompey in 48BCE
EVENT Battle_of_Hastings: William defeated Harold in 1066
Napoleon -> French_Revolution
EVENT Magna_Carta: King_John signed in 1215
PLACE Rome founded 753BCE by Romulus and Remus
EVENT World_War_II: 1939 -> 1945
"The Roman Empire fell in 476AD"
```

Example of output for one sentence:

```text
INPUT:
PERSON Julius_Caesar defeated Pompey in 48BCE

TOKENS:
  PERSON       -> PERSON
  IDENTIFIER   -> Julius_Caesar
  DEFEATED     -> defeated
  IDENTIFIER   -> Pompey
  IN           -> in
  YEAR         -> 48BCE

AST:
  ActionStatementNode
    containerType: PERSON
    containerName: Julius_Caesar
    subject: Julius_Caesar
    action: defeated
    object: Pompey
    year: 48BCE
```

---

## Conclusion

In this laboratory work, I extended the previous lexer into a complete small front-end:

- the **lexer** transforms text into tokens
- the **parser** checks the token order
- the **AST** stores the meaning in a tree structure

This lab helped me understand the difference between lexical analysis and syntactic analysis.

Lexical analysis answers:

- "What tokens do I have?"

Parsing answers:

- "How are these tokens organized?"

The final result is a simple but clear Java project that demonstrates parsing and AST construction in beginner level code.


---

## References

1. Formal Languages & Finite Automata course materials
2. Aho, Lam, Sethi, Ullman, *Compilers: Principles, Techniques, and Tools*
3. Abstract Syntax Tree (AST) concept in compiler design
