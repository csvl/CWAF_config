package be.uclouvain.model;


public class Condition {

    public String condition;
    public boolean inverse;

    public static Condition and(Condition c1, Condition c2) {
        return new Condition("(" + c1.getCondition() + " AND " + c2.getCondition() + ")");
    }


    public static Condition or(Condition c1, Condition c2) {
        return new Condition("(" + c1.getCondition() + " OR " + c2.getCondition() + ")");
    }

    public static Condition not(Condition c) {
        return new Condition(c.getCondition(), !c.inverse);
    }

    public Condition(String condition) {
        this.condition = condition;
    }

    public Condition(String condition, boolean inverse) {
        this.condition = condition;
        this.inverse = inverse;
    }

    public String getCondition() {
        if (inverse) {
            return "NOT(" + condition + ")";
        } else {
            return condition;
        }
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void inverse() {
        this.inverse = !this.inverse;
    }

    public Condition not() {
        return not(this);
    }

    public Condition and(Condition c) {
        return and(this, c);
    }

    public Condition or(Condition c) {
        return or(this, c);
    }
}