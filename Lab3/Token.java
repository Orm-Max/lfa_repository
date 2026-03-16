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