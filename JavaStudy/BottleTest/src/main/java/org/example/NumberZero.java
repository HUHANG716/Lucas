package org.example;

public class NumberZero implements IBottleNumber{
    @Override
    public String judgeIfPlural() {
        return "No more bottles";
    }

    @Override
    public String action() {
        return "Go to the store and buy some more, ";
    }

    @Override
    public String oneOrIt() {
        return "one";
    }
}
