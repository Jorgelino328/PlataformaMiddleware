package imd.ufrn.br.app;

public class ComplexData {
    private String name;
    private int value;
    private boolean active;

    public ComplexData() {
    }

    public ComplexData(String name, int value, boolean active) {
        this.name = name;
        this.value = value;
        this.active = active;
    }

    public String getName() { return name; }
    public int getValue() { return value; }
    public boolean isActive() { return active; }

    public void setName(String name) { this.name = name; }
    public void setValue(int value) { this.value = value; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "ComplexData{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", active=" + active +
                '}';
    }
}