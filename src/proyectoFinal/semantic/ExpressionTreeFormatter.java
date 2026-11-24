package proyectoFinal.semantic;

import java.util.ArrayList;
import java.util.List;
import proyectoFinal.parser.AstNode;

/** Extrae expresiones del árbol sintáctico para mostrarlas como árboles independientes. */
public final class ExpressionTreeFormatter {
    private ExpressionTreeFormatter() { }

    public static String format(AstNode root) {
        List<AstNode> expressions = new ArrayList<>();
        if (root != null) collectExpressionRoots(root, expressions);
        if (expressions.isEmpty()) return "No hay expresiones en el programa.";
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < expressions.size(); i++) {
            AstNode expression = expressions.get(i);
            output.append("Expresión ").append(i + 1).append(" (línea ")
                    .append(expression.getLine()).append("): ").append(expression.getValue()).append('\n');
            output.append(expression.toTreeString()).append('\n');
        }
        return output.toString();
    }

    private static void collectExpressionRoots(AstNode node, List<AstNode> output) {
        for (AstNode child : node.getChildren()) {
            if (isExpression(child)) output.add(child);
            else collectExpressionRoots(child, output);
        }
    }

    private static boolean isExpression(AstNode node) {
        return "Binary".equals(node.getKind()) || "Unary".equals(node.getKind())
                || "Literal".equals(node.getKind()) || "Identifier".equals(node.getKind())
                || "Grouping".equals(node.getKind());
    }
}
