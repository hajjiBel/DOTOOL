package tn.formation.calc;

public class Calculator {

    /** Prix TTC arrondi au centime. taux = 0.19 pour 19 %. */
    public double prixTtc(double ht, double taux) {
        if (ht < 0) {
            throw new IllegalArgumentException("Le prix HT doit être positif");
        }
        return Math.round(ht * (1 + taux) * 100.0) / 100.0;
    }

    public double diviser(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Division par zéro");
        }
        return a / b;
    }
}
