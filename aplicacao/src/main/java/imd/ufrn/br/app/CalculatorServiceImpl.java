package imd.ufrn.br.app;

import imd.ufrn.br.annotations.RemoteObject;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RemoteObject(name = "CalculatorService")
public class CalculatorServiceImpl {

    public CalculatorServiceImpl() {
        System.out.println("[" + LocalDateTime.now() + "] CalculatorServiceImpl instance created.");
    }
    public int add(int a, int b) {
        System.out.println("[" + LocalDateTime.now() + "] Executing CalculatorService.add(" + a + ", " + b + ")");
        return a + b;
    }

    public String echo(String message) {
        System.out.println("[" + LocalDateTime.now() + "] Executing CalculatorService.echo(\"" + message + "\")");
        if (message == null) {
            return "Echo: You sent null!";
        }
        return "Echo from server: " + message;
    }

    public ComplexData processData(ComplexData data) {
        System.out.println("[" + LocalDateTime.now() + "] Executing CalculatorService.processData(" + (data != null ? data.toString() : "null") + ")");
        if (data == null) {
            ComplexData errorData = new ComplexData("Error: Null input received", -1, false);
            return errorData;
        }
        data.setName("Processed: " + data.getName());
        data.setValue(data.getValue() * 10);
        data.setActive(!data.isActive());
        return data;
    }

    public String getStatus() {
        System.out.println("[" + LocalDateTime.now() + "] Executing CalculatorService.getStatus()");
        return "CalculatorService is UP and running at " + LocalDateTime.now();
    }

    public String greetAll(List<String> names) {
        System.out.println("[" + LocalDateTime.now() + "] Executing CalculatorService.greetAll(" + names + ")");
        if (names == null || names.isEmpty()) {
            return "Hello, an empty list of guests!";
        }
        return "Hello, " + String.join(", ", names) + "!";
    }

    public int sumArray(int[] numbers) {
        System.out.println("[" + LocalDateTime.now() + "] Executing CalculatorService.sumArray(" + Arrays.toString(numbers) + ")");
        if (numbers == null) {
            return 0;
        }
        return Arrays.stream(numbers).sum();
    }

    public void causeError() {
        System.out.println("[" + LocalDateTime.now() + "] Executing CalculatorService.causeError() - This will throw an exception.");
        throw new IllegalStateException("Intentional error from CalculatorService!");
    }
}