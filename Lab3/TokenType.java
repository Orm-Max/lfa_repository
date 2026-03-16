public enum TokenType {
    // Who / what
    PERSON,
    EVENT,
    PLACE,

    // Actions
    BORN,
    DIED,
    RULED,
    FOUNDED,
    DEFEATED,

    // Time
    YEAR,

    // Connecting words
    IN,
    BY,
    AND,

    // Other
    ARROW,       // ->
    COLON,       // :
    IDENTIFIER,  // any name like Julius_Caesar
    STRING,      // "quoted text"
    UNKNOWN
}