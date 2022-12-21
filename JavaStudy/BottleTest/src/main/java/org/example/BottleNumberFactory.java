package org.example;

public class BottleNumberFactory {
    public IBottleNumber build(int i){
        switch (i){
            case 1:return new NumberOne();
            case 0:return new NumberZero();
            case -1:return new NumberNagativeOne();
            default:return new BottleNumber(i);
        }
    }
}
