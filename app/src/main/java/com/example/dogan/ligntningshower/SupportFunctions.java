package com.example.dogan.ligntningshower;

/**
 * Created by dogan on 03.04.2016.
 */
public class SupportFunctions {
    /**
     * Функция для округления float до нужной цифры.
     *
     * @param number Число для округления.
     * @param scale  Количество цифр после запятой
     * @author Oleg Zepp
     */
    private static float round(float number, int scale) {
        int pow = 10;
        for (int i = 1; i < scale; i++)
            pow *= 10;
        float tmp = number * pow;
        return (float) (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) / pow;
    }
}
