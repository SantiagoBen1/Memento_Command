import java.time.Instant;
import java.util.*;

// ===== MEMENTO =====
// Snapshot inmutable que encapsula el estado de la calculadora.
class Memento {
    private final double state;

    public Memento(double state) {
        this.state = state;
    }

    public double getState() {
        return state;
    }
}

// ===== CALCULATOR (Originator) =====
// Originador que manipula el valor y produce/restaura mementos.
class Calculator {
    private double value;

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Memento save() {
        return new Memento(value);
    }

    public void restore(Memento m) {
        this.value = m.getState();
    }
}

// ===== COMMAND INTERFACE =====
interface Command {
    void execute();
}

// ===== BASE COMMAND =====
// Abstraccion comun para los comandos que aplican operaciones sobre la calculadora.
abstract class BaseCommand implements Command {
    protected Calculator calc;
    protected Memento backup;
    protected String label;
    protected Instant timestamp;

    public BaseCommand(Calculator calc, String label) {
        this.calc = calc;
        this.label = label;
        this.timestamp = Instant.now();
    }

    // Guarda el estado actual antes de ejecutar la operacion concreta.
    protected void saveBackup() {
        backup = calc.save();
    }

    // Restaura el valor previo utilizando el memento guardado.
    public void undo() {
        if (backup != null)
            calc.restore(backup);
    }

    public String getLabel() {
        return label;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

// ===== CONCRETE COMMANDS =====
// Implementaciones especificas de operaciones aritmeticas con soporte de undo/redo.
class AddCommand extends BaseCommand {
    private final double operand;

    public AddCommand(Calculator calc, double operand) {
        super(calc, "Add " + operand);
        this.operand = operand;
    }

    @Override
    public void execute() {
        saveBackup();
        calc.setValue(calc.getValue() + operand);
    }
}

class SubCommand extends BaseCommand {
    private final double operand;

    public SubCommand(Calculator calc, double operand) {
        super(calc, "Sub " + operand);
        this.operand = operand;
    }

    @Override
    public void execute() {
        saveBackup();
        calc.setValue(calc.getValue() - operand);
    }
}

class MulCommand extends BaseCommand {
    private final double operand;

    public MulCommand(Calculator calc, double operand) {
        super(calc, "Mul " + operand);
        this.operand = operand;
    }

    @Override
    public void execute() {
        saveBackup();
        calc.setValue(calc.getValue() * operand);
    }
}

class DivCommand extends BaseCommand {
    private final double operand;

    public DivCommand(Calculator calc, double operand) {
        super(calc, "Div " + operand);
        this.operand = operand;
    }

    @Override
    public void execute() {
        saveBackup();
        if (operand == 0)
            throw new ArithmeticException("División por cero");
        calc.setValue(calc.getValue() / operand);
    }
}

class ClearCommand extends BaseCommand {
    public ClearCommand(Calculator calc) {
        super(calc, "Clear");
    }

    @Override
    public void execute() {
        saveBackup();
        calc.setValue(0);
    }
}

// ===== HISTORY (Caretaker) =====
// Cuidador que mantiene pilas de undo y redo para gestionar comandos ejecutados.
class History {
    private final Stack<BaseCommand> undoStack = new Stack<>();
    private final Stack<BaseCommand> redoStack = new Stack<>();

    // Ejecuta un comando nuevo y lo registra en el historial.
    public void run(BaseCommand cmd) {
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
    }

    // Deshace el ultimo comando ejecutado, conservandolo para rehacer.
    public void undo() {
        if (undoStack.isEmpty()) return;
        BaseCommand cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
    }

    // Reaplica el comando mas reciente almacenado en redo.
    public void redo() {
        if (redoStack.isEmpty()) return;
        BaseCommand cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    // Construye una representacion legible del historial de undo.
    public String prettyUndoStack() {
        if (undoStack.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("Historial (últimos primero):\n");
        ListIterator<BaseCommand> it = undoStack.listIterator(undoStack.size());
        while (it.hasPrevious()) {
            BaseCommand cmd = it.previous();
            sb.append("- ").append(cmd.getLabel())
              .append(" [").append(cmd.getTimestamp()).append("]\n");
        }
        return sb.toString();
    }
}

// ===== MAIN (ConsoleCalc) =====
// Consola interactiva que delega en History el manejo de los comandos.
public class ConsoleCalc {
    public static void main(String[] args) {
        Calculator calc = new Calculator();
        History hist = new History();
        Scanner sc = new Scanner(System.in);

        System.out.println("Calc (Command + Memento) — escribe 'help' para ver comandos.\n");

        boolean running = true;
        while (running) {
            System.out.print("> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            try {
                switch (firstWord(line).toLowerCase()) {
                    case "help":
                        printHelp();
                        break;
                    case "val":
                        System.out.println("Valor = " + calc.getValue());
                        break;
                    case "hist":
                        String h = hist.prettyUndoStack();
                        System.out.print(h.isEmpty() ? "(historial vacío)\n" : h);
                        break;
                    case "undo":
                        if (!hist.canUndo()) { System.out.println("Nada que deshacer."); break; }
                        hist.undo();
                        System.out.println("Deshecho. Valor = " + calc.getValue());
                        break;
                    case "redo":
                        if (!hist.canRedo()) { System.out.println("Nada que rehacer."); break; }
                        hist.redo();
                        System.out.println("Rehecho. Valor = " + calc.getValue());
                        break;
                    case "clear":
                        hist.run(new ClearCommand(calc));
                        System.out.println("Clear. Valor = " + calc.getValue());
                        break;
                    case "+":
                    case "add":
                        hist.run(new AddCommand(calc, parseOperand(line)));
                        System.out.println("OK. Valor = " + calc.getValue());
                        break;
                    case "-":
                    case "sub":
                        hist.run(new SubCommand(calc, parseOperand(line)));
                        System.out.println("OK. Valor = " + calc.getValue());
                        break;
                    case "*":
                    case "mul":
                        hist.run(new MulCommand(calc, parseOperand(line)));
                        System.out.println("OK. Valor = " + calc.getValue());
                        break;
                    case "/":
                    case "div":
                        hist.run(new DivCommand(calc, parseOperand(line)));
                        System.out.println("OK. Valor = " + calc.getValue());
                        break;
                    case "exit":
                    case "quit":
                        running = false;
                        break;
                    default:
                        System.out.println("Comando no reconocido. Escribe 'help'.");
                }
            } catch (ArithmeticException ae) {
                System.out.println("Error: " + ae.getMessage());
            } catch (Exception e) {
                System.out.println("Error de entrada. Uso: '+ 5', '* 3', '/ 2', 'clear', 'undo', 'redo', 'val'.");
            }
        }

        System.out.println("Bye!");
    }

    // Muestra el listado de comandos disponibles.
    private static void printHelp() {
        System.out.println("Comandos:");
        System.out.println("  + n     | add n");
        System.out.println("  - n     | sub n");
        System.out.println("  * n     | mul n");
        System.out.println("  / n     | div n");
        System.out.println("  clear   | pone el valor en 0");
        System.out.println("  undo    | deshacer");
        System.out.println("  redo    | rehacer");
        System.out.println("  val     | mostrar valor actual");
        System.out.println("  hist    | mostrar historial (últimos primero)");
        System.out.println("  help    | ayuda");
        System.out.println("  exit    | salir");
    }

    // Devuelve el primer token del comando ingresado.
    private static String firstWord(String line) {
        int i = line.indexOf(' ');
        return (i < 0) ? line : line.substring(0, i);
    }

    // Extrae y parsea el operando numerico del comando textual.
    private static double parseOperand(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) throw new IllegalArgumentException("Falta operando.");
        return Double.parseDouble(parts[1]);
    }
}
