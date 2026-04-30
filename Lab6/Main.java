import java.util.List;

public class Main {
    public static void main(String[] args) {
        String[] examples = {
                "PERSON Julius_Caesar defeated Pompey in 48BCE",
                "EVENT Battle_of_Hastings: William defeated Harold in 1066",
                "Napoleon -> French_Revolution",
                "EVENT Magna_Carta: King_John signed in 1215",
                "PLACE Rome founded 753BCE by Romulus and Remus",
                "EVENT World_War_II: 1939 -> 1945",
                "\"The Roman Empire fell in 476AD\""
        };

        for (String example : examples) {
            System.out.println("INPUT:");
            System.out.println(example);
            System.out.println();

            Lexer lexer = new Lexer(example);
            List<Token> tokens = lexer.tokenize();

            System.out.println("TOKENS:");
            for (Token token : tokens) {
                System.out.println("  " + token);
            }

            Parser parser = new Parser(tokens);
            ASTNode ast = parser.parse();

            System.out.println();
            System.out.println("AST:");
            System.out.print(ast.toTreeString("  "));
            System.out.println("----------------------------------------");
        }
    }
}
