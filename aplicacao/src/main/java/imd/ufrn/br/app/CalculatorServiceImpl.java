package imd.ufrn.br.app;

import imd.ufrn.br.annotations.HttpVerb;
import imd.ufrn.br.annotations.MethodMapping;
import imd.ufrn.br.annotations.RequestMapping;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RequestMapping(path = "/calculator")
public class CalculatorServiceImpl {

    @MethodMapping(path = "/add", verb = HttpVerb.POST)
    public int add(int a, int b) {
        return a + b;
    }

    @MethodMapping(path = "/echo", verb = HttpVerb.POST)
    public String echo(String message) {
        if (message == null) {
            return "Echo: You sent null!";
        }
        return "Echo from server: " + message;
    }

    @MethodMapping(path = "/process", verb = HttpVerb.POST)
    public ComplexData processData(ComplexData data) {
        if (data == null) {
            ComplexData errorData = new ComplexData("Error: Null input received", -1, false);
            return errorData;
        }
        data.setName("Processed: " + data.getName());
        data.setValue(data.getValue() * 10);
        data.setActive(!data.isActive());
        return data;
    }

    @MethodMapping(path = "/status", verb = HttpVerb.GET)
    public String getStatus() {
        return "CalculatorService is UP and running at " + LocalDateTime.now();
    }

    @MethodMapping(path = "/greet", verb = HttpVerb.POST)
    public String greetAll(List<String> names) {
        if (names == null || names.isEmpty()) {
            return "Hello, an empty list of guests!";
        }
        return "Hello, " + String.join(", ", names) + "!";
    }

    @MethodMapping(path = "/sum", verb = HttpVerb.POST)
    public int sumArray(int[] numbers) {
        if (numbers == null) {
            return 0;
        }
        return Arrays.stream(numbers).sum();
    }

    @MethodMapping(path = "/error", verb = HttpVerb.POST)
    public void causeError() {
        throw new IllegalStateException("Intentional error from CalculatorService!");
    }
}