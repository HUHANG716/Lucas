package org.example;

public class NumberOne implements IBottleNumber{
    @Override
    public String judgeIfPlural() {
        return "1 bottle";
    }

    @Override
    public String action() {
        return "Take "+oneOrIt()+" down and pass it around, ";
    }

    @Override
    public String oneOrIt() {
        return "it";
    }
}
