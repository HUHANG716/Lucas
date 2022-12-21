package org.example;

public class NumberNagativeOne implements IBottleNumber{
    @Override
    public String judgeIfPlural() {
        return "99 bottles";
    }

    @Override
    public String action() {
        return "Take "+oneOrIt()+" down and pass it around, ";
    }

    @Override
    public String oneOrIt() {
        return "one";
    }
}
