package org.example;

public class BottleNumber implements IBottleNumber{
    private int account;

    public int getAccount() {
        return account;
    }

    public void setAccount(int account) {
        this.account = account;
    }

    public BottleNumber(int account) {
        this.account = account;
    }

    @Override
    public String judgeIfPlural() {
        return account+" bottles";
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
