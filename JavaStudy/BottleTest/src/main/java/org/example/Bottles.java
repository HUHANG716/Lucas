package org.example;

import java.util.ArrayList;


public class Bottles {

    private BottleNumberFactory bf = new BottleNumberFactory();

    public String verse(int bottleAccount) {
        IBottleNumber bottle = bf.build(bottleAccount);
        IBottleNumber nextBottle = bf.build(bottleAccount - 1);
        return bottle.judgeIfPlural() + " of beer on the wall, "
                + bottle.judgeIfPlural().toLowerCase() + " of beer.\n"
                + bottle.action() +
                nextBottle.judgeIfPlural().toLowerCase() + " of beer on the wall.";


    }


    public String verses(int i, int i1) {
        String finalResult = "";


        for (int a = i; a >= i1; a--) {
            finalResult = finalResult + verse(a);
            if (a != i1) {
                finalResult += "\n\n";
            }
        }
        return finalResult;
    }

    public String song() {
        return verses(99, 0);
    }
}

