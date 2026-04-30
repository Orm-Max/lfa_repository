import java.util.List;

public interface ASTNode {
    String toTreeString(String indent);
}

class QuotedTextNode implements ASTNode {
    private final String text;

    public QuotedTextNode(String text) {
        this.text = text;
    }

    @Override
    public String toTreeString(String indent) {
        return indent + "QuotedTextNode\n"
                + indent + "  text: " + text + "\n";
    }
}

class RelationNode implements ASTNode {
    private final String left;
    private final String right;

    public RelationNode(String left, String right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toTreeString(String indent) {
        return indent + "RelationNode\n"
                + indent + "  left: " + left + "\n"
                + indent + "  right: " + right + "\n";
    }
}

class ActionStatementNode implements ASTNode {
    private final String containerType;
    private final String containerName;
    private final String subject;
    private final String action;
    private final String object;
    private final String year;

    public ActionStatementNode(String containerType, String containerName, String subject,
                               String action, String object, String year) {
        this.containerType = containerType;
        this.containerName = containerName;
        this.subject = subject;
        this.action = action;
        this.object = object;
        this.year = year;
    }

    @Override
    public String toTreeString(String indent) {
        StringBuilder builder = new StringBuilder();

        builder.append(indent).append("ActionStatementNode\n");
        builder.append(indent).append("  containerType: ").append(containerType).append("\n");
        builder.append(indent).append("  containerName: ").append(containerName).append("\n");
        builder.append(indent).append("  subject: ").append(subject).append("\n");
        builder.append(indent).append("  action: ").append(action).append("\n");

        if (object != null) {
            builder.append(indent).append("  object: ").append(object).append("\n");
        }

        if (year != null) {
            builder.append(indent).append("  year: ").append(year).append("\n");
        }

        return builder.toString();
    }
}

class EventPeriodNode implements ASTNode {
    private final String eventName;
    private final String startYear;
    private final String endYear;

    public EventPeriodNode(String eventName, String startYear, String endYear) {
        this.eventName = eventName;
        this.startYear = startYear;
        this.endYear = endYear;
    }

    @Override
    public String toTreeString(String indent) {
        return indent + "EventPeriodNode\n"
                + indent + "  eventName: " + eventName + "\n"
                + indent + "  startYear: " + startYear + "\n"
                + indent + "  endYear: " + endYear + "\n";
    }
}

class PlaceFoundedNode implements ASTNode {
    private final String placeName;
    private final String year;
    private final List<String> founders;

    public PlaceFoundedNode(String placeName, String year, List<String> founders) {
        this.placeName = placeName;
        this.year = year;
        this.founders = founders;
    }

    @Override
    public String toTreeString(String indent) {
        StringBuilder builder = new StringBuilder();

        builder.append(indent).append("PlaceFoundedNode\n");
        builder.append(indent).append("  placeName: ").append(placeName).append("\n");
        builder.append(indent).append("  year: ").append(year).append("\n");
        builder.append(indent).append("  founders: ").append(founders).append("\n");

        return builder.toString();
    }
}
