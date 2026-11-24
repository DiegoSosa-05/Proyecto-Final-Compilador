package proyectoFinal.semantic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import proyectoFinal.model.Diagnostic;
import proyectoFinal.model.Diagnostic.Category;
import proyectoFinal.parser.AstNode;

/** Verifica nombres, ámbitos y tipos sobre el árbol construido por el parser. */
public final class SemanticAnalyzer {
    private static final List<String> NUMERIC_TYPES = Arrays.asList("int", "float", "double");
    private final List<SymbolEntry> symbols = new ArrayList<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final List<Scope> scopes = new ArrayList<>();
    private int nextAddress = 1000;
    private int nextScopeId = 1;

    public SemanticResult analyze(AstNode root) {
        if (root != null) visit(root);
        return new SemanticResult(symbols, diagnostics);
    }

    private void visit(AstNode node) {
        switch (node.getKind()) {
            case "MainMethod":
                // El cuerpo principal crea el primer ámbito del programa.
                for (AstNode child : node.getChildren()) visitBlock(child, "main");
                break;
            case "Block":
                visitBlock(node, "bloque" + nextScopeId++);
                break;
            default:
                for (AstNode child : node.getChildren()) visit(child);
        }
    }

    private void visitBlock(AstNode block, String name) {
        Scope parent = currentScope();
        String fullName = parent == null ? name : parent.name + "/" + name;
        Scope scope = new Scope(fullName, parent);
        scopes.add(scope);
        for (AstNode statement : block.getChildren()) analyzeStatement(statement);
        scopes.remove(scopes.size() - 1);
    }

    private void analyzeStatement(AstNode node) {
        switch (node.getKind()) {
            case "Declaration": analyzeDeclaration(node); break;
            case "Assignment": analyzeAssignment(node); break;
            case "If": analyzeIf(node); break;
            case "While": analyzeWhile(node); break;
            case "Block": visitBlock(node, "bloque" + nextScopeId++); break;
            case "Print":
                if (!node.getChildren().isEmpty()) evaluate(node.getChildren().get(0));
                break;
            default:
                for (AstNode child : node.getChildren()) analyzeStatement(child);
        }
    }

    private void analyzeDeclaration(AstNode declaration) {
        String[] parts = declaration.getValue().split(" ", 2);
        String type = parts[0];
        String name = parts.length > 1 ? parts[1] : "";
        Scope scope = currentScope();
        if (scope == null) return;
        if (scope.variables.containsKey(name)) {
            report(declaration, "La variable '" + name + "' ya fue declarada en este ámbito.",
                    "Cambia el nombre o elimina la declaración duplicada.");
            if (!declaration.getChildren().isEmpty()) evaluate(declaration.getChildren().get(0));
            return;
        }

        String value = "sin inicializar";
        if (!declaration.getChildren().isEmpty()) {
            Value initial = evaluate(declaration.getChildren().get(0));
            // Conserva el símbolo aunque su inicializador tenga errores, evitando falsos
            // reportes de "no declarada" en las instrucciones posteriores.
            if (!initial.valid) {
                declare(scope, name, type, "desconocido");
                return;
            }
            if (!isAssignable(type, initial.type)) {
                report(declaration, "No se puede asignar un valor de tipo " + initial.type
                                + " a la variable '" + name + "' de tipo " + type + ".",
                        "Usa una expresión de tipo " + type + " o una conversión compatible.");
                declare(scope, name, type, "desconocido");
                return;
            }
            value = initial.known ? stringify(convert(initial.value, type)) : "desconocido";
        }
        declare(scope, name, type, value);
    }

    private void analyzeAssignment(AstNode assignment) {
        if (assignment.getChildren().isEmpty()) return;
        String name = assignment.getValue();
        Value value = evaluate(assignment.getChildren().get(0));
        SymbolEntry symbol = resolve(name);
        if (symbol == null) {
            report(assignment, "La variable '" + name + "' no está declarada.",
                    "Declara '" + name + "' antes de asignarle un valor.");
            return;
        }
        if (!value.valid) return;
        if (!isAssignable(symbol.getType(), value.type)) {
            report(assignment, "Asignación incompatible: '" + name + "' es " + symbol.getType()
                            + " y la expresión es " + value.type + ".",
                    "Asigna un valor compatible con el tipo " + symbol.getType() + ".");
            return;
        }
        symbol.setValue(value.known ? stringify(convert(value.value, symbol.getType())) : "desconocido");
    }

    private void analyzeIf(AstNode node) {
        if (node.getChildren().isEmpty()) return;
        Value condition = evaluate(node.getChildren().get(0));
        requireBoolean(condition, node, "La condición de if debe ser boolean.");
        if (node.getChildren().size() > 1) visitBlock(node.getChildren().get(1), "if" + nextScopeId++);
        if (node.getChildren().size() > 2) {
            AstNode alternative = node.getChildren().get(2);
            if ("Block".equals(alternative.getKind())) visitBlock(alternative, "else" + nextScopeId++);
            else analyzeStatement(alternative);
        }
    }

    private void analyzeWhile(AstNode node) {
        if (node.getChildren().size() < 2) return;
        Value condition = evaluate(node.getChildren().get(0));
        requireBoolean(condition, node, "La condición de while debe ser boolean.");
        visitBlock(node.getChildren().get(1), "while" + nextScopeId++);
    }

    private Value evaluate(AstNode expression) {
        switch (expression.getKind()) {
            case "Literal": return literal(expression);
            case "Identifier": return identifier(expression);
            case "Grouping": return expression.getChildren().isEmpty()
                    ? Value.invalid() : evaluate(expression.getChildren().get(0));
            case "Unary": return evaluateUnary(expression);
            case "Binary": return evaluateBinary(expression);
            default: return Value.invalid();
        }
    }

    private Value literal(AstNode node) {
        String text = node.getValue();
        try {
            if (text.startsWith("\"")) return Value.known("String", unquote(text));
            if ("true".equals(text) || "false".equals(text)) return Value.known("boolean", Boolean.valueOf(text));
            if (text.indexOf('.') >= 0) return Value.known("double", Double.valueOf(text));
            return Value.known("int", Integer.valueOf(text));
        } catch (NumberFormatException exception) {
            report(node, "El literal numérico está fuera del rango admitido.",
                    "Usa un valor numérico dentro del rango del tipo.");
            return Value.invalid();
        }
    }

    private Value identifier(AstNode node) {
        SymbolEntry symbol = resolve(node.getValue());
        if (symbol == null) {
            report(node, "La variable '" + node.getValue() + "' no está declarada.",
                    "Declara la variable antes de utilizarla.");
            return Value.invalid();
        }
        if ("sin inicializar".equals(symbol.getValue())) {
            report(node, "La variable '" + node.getValue() + "' se usa sin inicializar.",
                    "Asigna un valor antes de leer la variable.");
            return Value.invalid();
        }
        if ("desconocido".equals(symbol.getValue())) return Value.unknown(symbol.getType());
        return valueFromStored(symbol.getType(), symbol.getValue());
    }

    private Value evaluateUnary(AstNode node) {
        if (node.getChildren().isEmpty()) return Value.invalid();
        Value operand = evaluate(node.getChildren().get(0));
        if (!operand.valid) return operand;
        String op = node.getValue();
        if ("!".equals(op)) {
            if (!"boolean".equals(operand.type)) return operatorError(node, "! requiere un operando boolean.");
            return operand.known ? Value.known("boolean", !(Boolean) operand.value) : Value.unknown("boolean");
        }
        if (!isNumeric(operand.type)) return operatorError(node, "El operador unario " + op + " requiere un número.");
        if (!operand.known) return Value.unknown(operand.type);
        double number = asDouble(operand.value);
        Object result = "-".equals(op) ? -number : number;
        if ("int".equals(operand.type)) result = ((Number) result).intValue();
        else if ("float".equals(operand.type)) result = ((Number) result).floatValue();
        return Value.known(operand.type, result);
    }

    private Value evaluateBinary(AstNode node) {
        if (node.getChildren().size() < 2) return Value.invalid();
        Value left = evaluate(node.getChildren().get(0));
        Value right = evaluate(node.getChildren().get(1));
        if (!left.valid || !right.valid) return Value.invalid();
        String op = node.getValue();
        if ("&&".equals(op) || "||".equals(op)) {
            if (!"boolean".equals(left.type) || !"boolean".equals(right.type))
                return operatorError(node, op + " requiere dos operandos boolean.");
            if (!left.known || !right.known) return Value.unknown("boolean");
            boolean a = (Boolean) left.value, b = (Boolean) right.value;
            return Value.known("boolean", "&&".equals(op) ? a && b : a || b);
        }
        if ("==".equals(op) || "!=".equals(op)) return equality(node, left, right);
        if ("<".equals(op) || "<=".equals(op) || ">".equals(op) || ">=".equals(op))
            return relational(node, left, right);
        if ("+".equals(op) && ("String".equals(left.type) || "String".equals(right.type))) {
            if (!left.known || !right.known) return Value.unknown("String");
            return Value.known("String", stringify(left.value) + stringify(right.value));
        }
        if (!isNumeric(left.type) || !isNumeric(right.type))
            return operatorError(node, "El operador " + op + " requiere operandos numéricos.");
        return arithmetic(node, left, right);
    }

    private Value arithmetic(AstNode node, Value left, Value right) {
        String type = wider(left.type, right.type);
        if (!left.known || !right.known) return Value.unknown(type);
        double a = asDouble(left.value), b = asDouble(right.value), result;
        switch (node.getValue()) {
            case "+": result = a + b; break;
            case "-": result = a - b; break;
            case "*": result = a * b; break;
            case "/":
                if (b == 0.0) return operatorError(node, "No se puede dividir entre cero.");
                result = a / b;
                if ("int".equals(type)) return Value.known("int", (int) (a / b));
                break;
            case "%":
                if (b == 0.0) return operatorError(node, "No se puede obtener residuo entre cero.");
                result = a % b;
                break;
            default: return Value.invalid();
        }
        if ("int".equals(type)) return Value.known(type, (int) result);
        if ("float".equals(type)) return Value.known(type, (float) result);
        return Value.known(type, result);
    }

    private Value equality(AstNode node, Value left, Value right) {
        boolean compatible = (isNumeric(left.type) && isNumeric(right.type)) || left.type.equals(right.type);
        if (!compatible) return operatorError(node, "Los operandos de " + node.getValue() + " no son compatibles.");
        if (!left.known || !right.known) return Value.unknown("boolean");
        boolean equal = isNumeric(left.type) && isNumeric(right.type)
                ? asDouble(left.value) == asDouble(right.value) : left.value.equals(right.value);
        return Value.known("boolean", "==".equals(node.getValue()) ? equal : !equal);
    }

    private Value relational(AstNode node, Value left, Value right) {
        if (!isNumeric(left.type) || !isNumeric(right.type))
            return operatorError(node, "El operador relacional requiere operandos numéricos.");
        if (!left.known || !right.known) return Value.unknown("boolean");
        double a = asDouble(left.value), b = asDouble(right.value);
        boolean result;
        switch (node.getValue()) {
            case "<": result = a < b; break;
            case "<=": result = a <= b; break;
            case ">": result = a > b; break;
            default: result = a >= b;
        }
        return Value.known("boolean", result);
    }

    private void declare(Scope scope, String name, String type, String value) {
        String address = "M" + nextAddress;
        nextAddress += 4; // Celda didáctica fija de 4 bytes, no dirección real de la JVM.
        SymbolEntry entry = new SymbolEntry(name, type, value, scope.name, address);
        scope.variables.put(name, entry);
        symbols.add(entry);
    }

    private SymbolEntry resolve(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            SymbolEntry found = scopes.get(i).variables.get(name);
            if (found != null) return found;
        }
        return null;
    }
    private Scope currentScope() { return scopes.isEmpty() ? null : scopes.get(scopes.size() - 1); }
    private boolean isNumeric(String type) { return NUMERIC_TYPES.contains(type); }
    private String wider(String a, String b) {
        if ("double".equals(a) || "double".equals(b)) return "double";
        if ("float".equals(a) || "float".equals(b)) return "float";
        return "int";
    }
    private boolean isAssignable(String target, String source) {
        if (target.equals(source)) return true;
        if ("double".equals(target)) return "int".equals(source) || "float".equals(source);
        return "float".equals(target) && "int".equals(source);
    }
    private Object convert(Object value, String target) {
        if ("double".equals(target)) return asDouble(value);
        if ("float".equals(target)) return ((Number) value).floatValue();
        if ("int".equals(target)) return ((Number) value).intValue();
        return value;
    }
    private double asDouble(Object value) { return ((Number) value).doubleValue(); }
    private String stringify(Object value) { return String.valueOf(value); }
    private Value valueFromStored(String type, String value) {
        try {
            if ("int".equals(type)) return Value.known(type, Integer.valueOf(value));
            if ("float".equals(type)) return Value.known(type, Float.valueOf(value));
            if ("double".equals(type)) return Value.known(type, Double.valueOf(value));
            if ("boolean".equals(type)) return Value.known(type, Boolean.valueOf(value));
            return Value.known(type, value);
        } catch (NumberFormatException exception) { return Value.unknown(type); }
    }
    private String unquote(String text) {
        String body = text.substring(1, text.length() - 1);
        return body.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t")
                .replace("\\\"", "\"").replace("\\\\", "\\");
    }
    private void requireBoolean(Value condition, AstNode node, String message) {
        if (condition.valid && !"boolean".equals(condition.type))
            report(node, message, "Compara valores o usa true/false para producir una condición booleana.");
    }
    private Value operatorError(AstNode node, String message) {
        report(node, message, "Revisa los tipos de los operandos y el operador utilizado.");
        return Value.invalid();
    }
    private void report(AstNode node, String message, String suggestion) {
        diagnostics.add(new Diagnostic(Category.SEMANTICO, node.getLine(), node.getColumn(), message, suggestion));
    }

    private static final class Scope {
        private final String name;
        @SuppressWarnings("unused") private final Scope parent;
        private final Map<String, SymbolEntry> variables = new HashMap<>();
        private Scope(String name, Scope parent) { this.name = name; this.parent = parent; }
    }
    private static final class Value {
        private final String type;
        private final Object value;
        private final boolean known;
        private final boolean valid;
        private Value(String type, Object value, boolean known, boolean valid) {
            this.type = type; this.value = value; this.known = known; this.valid = valid;
        }
        private static Value known(String type, Object value) { return new Value(type, value, true, true); }
        private static Value unknown(String type) { return new Value(type, null, false, true); }
        private static Value invalid() { return new Value("?", null, false, false); }
    }
}
