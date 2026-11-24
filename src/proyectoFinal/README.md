# Compilador educativo: especificación inicial

Esta carpeta contendrá un compilador educativo implementado por etapas. El lenguaje aceptado será un **subconjunto explícito de Java**; no se utilizará `javac` como analizador. `javac` podrá servir para compilar el propio proyecto, pero el reconocimiento léxico, sintáctico y semántico será nuestro.

## Estructura planeada

```text
proyectoFinal/
  app/       Coordinación del análisis y punto de inicio
  lexer/     Tokens y análisis léxico
  parser/    Gramática, parser y nodos del árbol sintáctico
  semantic/  Análisis de tipos, ámbitos y tablas semánticas
  model/     Errores, posiciones y resultado compartidos
  ui/        Interfaz gráfica Swing y vistas de resultados
```

Cada clase tendrá una responsabilidad concreta. Los comentarios explicarán las reglas y decisiones relevantes para facilitar su exposición y modificación.

## Gramática inicial (EBNF)

Los símbolos entre comillas son terminales. `*` significa cero o más repeticiones; `?`, opcional; `|`, alternativa.

```ebnf
programa       = clase ;
clase          = [ "public" ] "class" IDENTIFICADOR "{" metodoPrincipal "}" ;
metodoPrincipal = "public" "static" "void" "main" "(" "String" "[" "]" IDENTIFICADOR ")" bloque ;

bloque         = "{" sentencia* "}" ;
sentencia      = declaracion | asignacion | impresion | condicional | cicloMientras | bloque ;
declaracion    = tipo IDENTIFICADOR [ "=" expresion ] ";" ;
asignacion     = IDENTIFICADOR "=" expresion ";" ;
impresion      = "System" "." "out" "." "println" "(" [ expresion ] ")" ";" ;
condicional    = "if" "(" expresion ")" bloque [ "else" ( bloque | condicional ) ] ;
cicloMientras  = "while" "(" expresion ")" bloque ;
tipo           = "int" | "double" | "float" | "boolean" | "String" ;

expresion      = expresionOr ;
expresionOr    = expresionAnd { "||" expresionAnd } ;
expresionAnd   = expresionIgualdad { "&&" expresionIgualdad } ;
expresionIgualdad = expresionRelacional { ( "==" | "!=" ) expresionRelacional } ;
expresionRelacional = expresionAditiva { ( "<" | "<=" | ">" | ">=" ) expresionAditiva } ;
expresionAditiva = expresionMultiplicativa { ( "+" | "-" ) expresionMultiplicativa } ;
expresionMultiplicativa = expresionUnaria { ( "*" | "/" | "%" ) expresionUnaria } ;
expresionUnaria = ( "!" | "-" | "+" ) expresionUnaria | primaria ;
primaria       = ENTERO | DECIMAL | CADENA | "true" | "false" | IDENTIFICADOR
               | "(" expresion ")" ;
```

El lexer también reconocerá comentarios `//...` y `/*...*/` para ignorarlos durante el análisis, además de espacios y saltos de línea. Los operadores de dos caracteres (`&&`, `||`, `==`, `!=`, `<=`, `>=`) se reconocerán como un solo token. Se conservarán línea y columna desde el lexer para los reportes posteriores.

## Límites de esta primera versión

Se analizará un único archivo con una clase y el método `main`; las llaves de bloques son obligatorias. Se contemplan declaraciones, asignaciones, impresión, `if/else`, `while` y expresiones con precedencia. No se incluyen por ahora `for`, arreglos, métodos definidos por el usuario, `import`, objetos, llamadas arbitrarias ni inferencia de tipos. Estas limitaciones hacen que la gramática sea implementable y explicable en la evaluación.

## Reglas semánticas acordadas

- Los tipos base son `int`, `double`, `float`, `boolean` y `String`.
- Los ámbitos se abren y cierran con cada bloque `{ ... }`; una variable declarada dentro de un bloque no estará disponible fuera de él.
- Se reportan declaraciones duplicadas en el mismo ámbito y usos de variables no declaradas.
- `if` y `while` requieren una condición `boolean`.
- Aritmética: operandos numéricos; `+` también concatena si participa un `String`.
- Comparaciones relacionales requieren operandos numéricos y producen `boolean`. Igualdad requiere tipos compatibles. `&&`, `||` y `!` requieren booleanos.
- La asignación permite el mismo tipo y conversiones numéricas seguras (`int` a `float`/`double`, `float` a `double`); no permite conversiones estrechantes implícitas.
- La tabla de símbolos conservará nombre, tipo, valor conocido (o sin valor calculable) y ámbito. La tabla de direcciones usará direcciones simuladas consecutivas, claramente identificadas como didácticas.

## Reporte de errores

Cada diagnóstico llevará clasificación (léxico, sintáctico o semántico), línea y columna, descripción y sugerencia. El parser intentará sincronizarse al siguiente `;` o `}` tras errores de sentencia para continuar y reportar más problemas en una ejecución. Si el error impide determinar con precisión una ubicación o una sugerencia, se indicará de forma prudente en vez de inventar una corrección.

## Interfaz prevista

La interfaz Swing tendrá un área para escribir o pegar código, botones **Analizar** y **Generar tabla de tokens**, y vistas separadas para errores/reporte, tokens, árbol de derivación, árbol de expresiones, tabla de símbolos y direcciones.

## Etapa 2: análisis léxico

El paquete `lexer` contiene el reconocedor manual, los tipos de token, el resultado y un formateador de tabla. Cada token conserva su línea y columna inicial. El lexer omite espacios y comentarios, reconoce palabras reservadas, identificadores, literales, operadores y delimitadores, e informa caracteres inválidos, operadores lógicos incompletos, cadenas mal cerradas o escapes inválidos y comentarios de bloque sin cierre. Los diagnósticos usan el modelo compartido `model.Diagnostic` para integrarse después con las demás etapas.

## Etapa 3: análisis sintáctico

El paquete `parser` implementa un parser descendente recursivo para la gramática anterior. Produce nodos con tipo, valor, posición y nodos hijos para representar la estructura del programa y las expresiones; `AstNode.toTreeString()` permite mostrar el árbol con sangría. Las expresiones se analizan por niveles de precedencia (unarios, multiplicación, suma, relación, igualdad, `&&` y `||`). Ante errores dentro de un bloque, el parser intenta continuar en el siguiente punto de sincronización y cada diagnóstico incluye línea, columna, descripción y sugerencia.

## Etapa 4: análisis semántico

El paquete `semantic` recorre el árbol, crea ámbitos para `main` y cada bloque, y verifica declaraciones duplicadas, nombres no declarados, uso sin inicializar, asignaciones, condiciones y operadores. Evalúa expresiones constantes para registrar valores conocidos y marca como `desconocido` los valores que dependen de una ejecución. Cada variable recibe una dirección simbólica consecutiva `M1000`, `M1004`, etc.; son direcciones didácticas y usan un tamaño fijo de 4 bytes. `SymbolTableFormatter` prepara las tablas de símbolos y direcciones, y `ExpressionTreeFormatter` extrae los árboles de expresiones del árbol sintáctico.

## Etapa 5: interfaz e integración

La clase de inicio es `proyectoFinal.app.Main`. La ventana Swing separa editor con números de línea, controles de acción y pestañas de resultados; usa una paleta azul marino, turquesa y fondos claros para conservar contraste. **Analizar código** ejecuta lexer, parser y semántico, mientras que **Generar tabla de tokens** ejecuta el lexer y abre su tabla. Las demás pestañas muestran reporte general, diagnósticos con sugerencias, árboles, tabla de símbolos y direcciones. Para ejecutar desde la carpeta del proyecto después de compilar las fuentes: `java -cp bin proyectoFinal.app.Main`.
