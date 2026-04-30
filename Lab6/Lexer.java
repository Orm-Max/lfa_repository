import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    private final String input;
    private int position;
    private final List<TokenRule> rules;

    public Lexer(String input) {
        this.input = input;
        this.position = 0;
        this.rules = createRules();
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (position < input.length()) {
            String currentText = input.substring(position);
            Token matchedToken = null;
            int matchedLength = 0;

            for (TokenRule rule : rules) {
                Matcher matcher = rule.pattern.matcher(currentText);

                if (matcher.find() && matcher.start() == 0) {
                    String value = matcher.group();
                    matchedToken = new Token(rule.type, cleanValue(rule.type, value));
                    matchedLength = value.length();
                    break;
                }
            }

            if (matchedToken == null) {
                matchedToken = new Token(TokenType.UNKNOWN, String.valueOf(input.charAt(position)));
                matchedLength = 1;
            }

            if (matchedToken.getType() != TokenType.WHITESPACE) {
                tokens.add(matchedToken);
            }

            position += matchedLength;
        }

        return tokens;
    }

    private List<TokenRule> createRules() {
        List<TokenRule> tokenRules = new ArrayList<>();

        tokenRules.add(new TokenRule(TokenType.WHITESPACE, "\\s+"));
        tokenRules.add(new TokenRule(TokenType.ARROW, "->"));
        tokenRules.add(new TokenRule(TokenType.COLON, ":"));
        tokenRules.add(new TokenRule(TokenType.STRING, "\"[^\"]*\""));

        tokenRules.add(new TokenRule(TokenType.PERSON, "(?i)PERSON\\b"));
        tokenRules.add(new TokenRule(TokenType.EVENT, "(?i)EVENT\\b"));
        tokenRules.add(new TokenRule(TokenType.PLACE, "(?i)PLACE\\b"));

        tokenRules.add(new TokenRule(TokenType.BORN, "(?i)BORN\\b"));
        tokenRules.add(new TokenRule(TokenType.DIED, "(?i)DIED\\b"));
        tokenRules.add(new TokenRule(TokenType.RULED, "(?i)RULED\\b"));
        tokenRules.add(new TokenRule(TokenType.FOUNDED, "(?i)FOUNDED\\b"));
        tokenRules.add(new TokenRule(TokenType.DEFEATED, "(?i)DEFEATED\\b"));

        tokenRules.add(new TokenRule(TokenType.IN, "(?i)IN\\b"));
        tokenRules.add(new TokenRule(TokenType.BY, "(?i)BY\\b"));
        tokenRules.add(new TokenRule(TokenType.AND, "(?i)AND\\b"));

        tokenRules.add(new TokenRule(TokenType.YEAR, "\\d+(?:BCE|BC|AD|CE)?\\b"));
        tokenRules.add(new TokenRule(TokenType.IDENTIFIER, "[A-Za-z_][A-Za-z0-9_]*"));
        tokenRules.add(new TokenRule(TokenType.UNKNOWN, "."));

        return tokenRules;
    }

    private String cleanValue(TokenType type, String value) {
        if (type == TokenType.STRING) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    private static class TokenRule {
        private final TokenType type;
        private final Pattern pattern;

        private TokenRule(TokenType type, String regex) {
            this.type = type;
            this.pattern = Pattern.compile(regex);
        }
    }
}
