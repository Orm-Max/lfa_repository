import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int position;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    public ASTNode parse() {
        if (tokens.isEmpty()) {
            throw new ParserException("Input is empty.");
        }

        if (check(TokenType.STRING)) {
            return parseQuotedText();
        }

        if (check(TokenType.PERSON)) {
            return parsePersonStatement();
        }

        if (check(TokenType.EVENT)) {
            return parseEventStatement();
        }

        if (check(TokenType.PLACE)) {
            return parsePlaceStatement();
        }

        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.ARROW)) {
            return parseRelation();
        }

        throw new ParserException("Cannot parse input starting with token: " + peek());
    }

    private ASTNode parseQuotedText() {
        Token textToken = consume(TokenType.STRING, "Expected a quoted string.");
        ensureEndOfInput();
        return new QuotedTextNode(textToken.getValue());
    }

    private ASTNode parseRelation() {
        String left = consume(TokenType.IDENTIFIER, "Expected identifier before ->").getValue();
        consume(TokenType.ARROW, "Expected -> between identifiers.");
        String right = consume(TokenType.IDENTIFIER, "Expected identifier after ->").getValue();
        ensureEndOfInput();
        return new RelationNode(left, right);
    }

    private ASTNode parsePersonStatement() {
        String type = consume(TokenType.PERSON, "Expected PERSON.").getValue();
        String personName = consume(TokenType.IDENTIFIER, "Expected person name.").getValue();
        String action = consumeActionWord();
        String object = null;
        String year = null;

        if (check(TokenType.IDENTIFIER)) {
            object = consume(TokenType.IDENTIFIER, "Expected object name.").getValue();
        }

        if (match(TokenType.IN)) {
            year = consume(TokenType.YEAR, "Expected year after IN.").getValue();
        }

        ensureEndOfInput();
        return new ActionStatementNode(type, personName, personName, action, object, year);
    }

    private ASTNode parseEventStatement() {
        consume(TokenType.EVENT, "Expected EVENT.");
        String eventName = consume(TokenType.IDENTIFIER, "Expected event name.").getValue();
        consume(TokenType.COLON, "Expected : after event name.");

        if (check(TokenType.YEAR)) {
            String startYear = consume(TokenType.YEAR, "Expected start year.").getValue();
            consume(TokenType.ARROW, "Expected -> between years.");
            String endYear = consume(TokenType.YEAR, "Expected end year.").getValue();
            ensureEndOfInput();
            return new EventPeriodNode(eventName, startYear, endYear);
        }

        String subject = consume(TokenType.IDENTIFIER, "Expected subject after event name.").getValue();
        String action = consumeActionWord();
        String object = null;
        String year = null;

        if (check(TokenType.IDENTIFIER)) {
            object = consume(TokenType.IDENTIFIER, "Expected object name.").getValue();
        }

        if (match(TokenType.IN)) {
            year = consume(TokenType.YEAR, "Expected year after IN.").getValue();
        }

        ensureEndOfInput();
        return new ActionStatementNode("EVENT", eventName, subject, action, object, year);
    }

    private ASTNode parsePlaceStatement() {
        consume(TokenType.PLACE, "Expected PLACE.");
        String placeName = consume(TokenType.IDENTIFIER, "Expected place name.").getValue();
        consume(TokenType.FOUNDED, "Expected founded keyword.");
        String year = consume(TokenType.YEAR, "Expected year after founded.").getValue();
        consume(TokenType.BY, "Expected by keyword.");

        List<String> founders = new ArrayList<>();
        founders.add(consume(TokenType.IDENTIFIER, "Expected first founder name.").getValue());

        while (match(TokenType.AND)) {
            founders.add(consume(TokenType.IDENTIFIER, "Expected founder name after AND.").getValue());
        }

        ensureEndOfInput();
        return new PlaceFoundedNode(placeName, year, founders);
    }

    private String consumeActionWord() {
        if (check(TokenType.DEFEATED) || check(TokenType.BORN) || check(TokenType.DIED)
                || check(TokenType.RULED) || check(TokenType.FOUNDED)) {
            return advance().getValue();
        }

        if (check(TokenType.IDENTIFIER)) {
            return advance().getValue();
        }

        throw new ParserException("Expected action word, but found: " + peek());
    }

    private Token consume(TokenType expectedType, String message) {
        if (check(expectedType)) {
            return advance();
        }

        throw new ParserException(message + " Found: " + peek());
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().getType() == type;
    }

    private boolean checkNext(TokenType type) {
        if (position + 1 >= tokens.size()) {
            return false;
        }
        return tokens.get(position + 1).getType() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            position++;
        }
        return tokens.get(position - 1);
    }

    private boolean isAtEnd() {
        return position >= tokens.size();
    }

    private Token peek() {
        if (isAtEnd()) {
            return new Token(TokenType.UNKNOWN, "END");
        }
        return tokens.get(position);
    }

    private void ensureEndOfInput() {
        if (!isAtEnd()) {
            throw new ParserException("Unexpected token at the end: " + peek());
        }
    }

    public static class ParserException extends RuntimeException {
        public ParserException(String message) {
            super(message);
        }
    }
}
