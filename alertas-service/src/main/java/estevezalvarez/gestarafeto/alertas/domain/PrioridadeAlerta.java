package estevezalvarez.gestarafeto.alertas.domain;

public enum PrioridadeAlerta {
    BAIXA(1),
    MEDIA(2),
    ALTA(3);

    private final int peso;

    PrioridadeAlerta(int peso) {
        this.peso = peso;
    }

    public int getPeso() {
        return peso;
    }
}
