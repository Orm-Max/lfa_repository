import java.util.List;

public class Main {

    public static void main(String[] args) {

        // Some example historical event sentences
        String[] examples = {
            "PERSON Julius_Caesar born 100BCE in Rome",
            "PERSON Julius_Caesar defeated Pompey in 48BCE",
            "EVENT Battle_of_Hastings: William defeated Harold in 1066",
            "PERSON Napoleon_Bonaparte born 1769 in Corsica",
            "Napoleon -> French_Revolution",
            "EVENT Magna_Carta: King_John signed in 1215",
            "PLACE Rome founded 753BCE by Romulus and Remus",
            "PERSON Alexander_the_Great died 323BCE",
            "EVENT World_War_II: 1939 -> 1945",
            "\"The Roman Empire fell in 476AD\""
        };

        for (String sentence : examples) {
            System.out.println("INPUT:  " + sentence);
            System.out.println("TOKENS:");

            Lexer lexer = new Lexer(sentence);
            List<Token> tokens = lexer.tokenize();

            for (Token token : tokens) {
                System.out.println("  " + token);
            }

            System.out.println();
        }
    }
}