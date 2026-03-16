import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private String text;   // the input text
    private int pos;       // current position in the text

    public Lexer(String text) {
        this.text = text;
        this.pos = 0;
    }

    // ── main method: split the text into tokens ──────────────────────────────

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (pos < text.length()) {

            // skip spaces and tabs
            if (Character.isWhitespace(text.charAt(pos))) {
                pos++;
                continue;
            }

            // "->" arrow
            if (pos + 1 < text.length() && text.charAt(pos) == '-' && text.charAt(pos + 1) == '>') {
                tokens.add(new Token(TokenType.ARROW, "->"));
                pos += 2;
                continue;
            }

            // ":" colon
            if (text.charAt(pos) == ':') {
                tokens.add(new Token(TokenType.COLON, ":"));
                pos++;
                continue;
            }

            // "quoted string"
            if (text.charAt(pos) == '"') {
                tokens.add(readString());
                continue;
            }

            // number like 1066, 44, 1789
            if (Character.isDigit(text.charAt(pos))) {
                tokens.add(readNumber());
                continue;
            }

            // word: could be a keyword or an identifier
            if (Character.isLetter(text.charAt(pos)) || text.charAt(pos) == '_') {
                tokens.add(readWord());
                continue;
            }

            // anything else we don't recognise
            tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(text.charAt(pos))));
            pos++;
        }

        return tokens;
    }

    // ── helper: read a quoted string ─────────────────────────────────────────

    private Token readString() {
        pos++; // skip the opening "
        StringBuilder sb = new StringBuilder();

        while (pos < text.length() && text.charAt(pos) != '"') {
            sb.append(text.charAt(pos));
            pos++;
        }

        pos++; // skip the closing "
        return new Token(TokenType.STRING, sb.toString());
    }

    // ── helper: read a number and check if it looks like a year ──────────────

    private Token readNumber() {
        StringBuilder sb = new StringBuilder();

        while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
            sb.append(text.charAt(pos));
            pos++;
        }

        // also accept era suffix like BCE, BC, AD, CE
        if (pos < text.length() && Character.isLetter(text.charAt(pos))) {
            StringBuilder era = new StringBuilder();
            int savedPos = pos;

            while (pos < text.length() && Character.isLetter(text.charAt(pos))) {
                era.append(text.charAt(pos));
                pos++;
            }

            String suffix = era.toString().toUpperCase();
            if (suffix.equals("BCE") || suffix.equals("BC") || suffix.equals("AD") || suffix.equals("CE")) {
                sb.append(suffix);
            } else {
                // not an era suffix — put the letters back
                pos = savedPos;
            }
        }

        return new Token(TokenType.YEAR, sb.toString());
    }

    // ── helper: read a word and check the keyword table ──────────────────────

    private Token readWord() {
        StringBuilder sb = new StringBuilder();

        while (pos < text.length() && (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_')) {
            sb.append(text.charAt(pos));
            pos++;
        }

        String word = sb.toString();
        TokenType type = matchKeyword(word);

        return new Token(type, word);
    }

    // ── keyword table ─────────────────────────────────────────────────────────

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
}