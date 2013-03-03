package org.greencheek.annotations.axis;

import java.util.ArrayList;
import java.util.List;

/**
 * User: dominictootell
 * Date: 03/03/2013
 * Time: 16:44
 */
public class IterativeVerticalAxisCreator implements VerticalAxisCreator {

    public static void main(String[] args) {
        IterativeVerticalAxisCreator i = new IterativeVerticalAxisCreator();
        for(String s : i.verticalAxis(new int[]  { 2,3,10,2,3,40,9,1,67,54,20,3,40,30,100,598,88,76,4,53,10,98} ,2)) {
            System.out.println(s);
        }

    }
    @Override
    public String[] verticalAxis(int[] numbers, int increment) {

        List<String> rows = writeNumbersVertical(numbers, increment);

        return rows.toArray(new String[rows.size()]);
    }


    private static int getMaxValue(int[] numbers){
        int maxValue = numbers[0];
        for(int i=1;i<numbers.length;i++){
            if(numbers[i] > maxValue){
                maxValue = numbers[i];
            }
        }
        return maxValue;
    }

    public List<String> writeNumbersVertical(int[] number, int tick)
    {
        List<StringBuilder> rows = new ArrayList<StringBuilder>();




        int column =0;
        for(int num : number) {
            writeVerticalTR(rows, column, num, number.length);
            column++;
        }

        if(tick>1) {
            for(StringBuilder b : rows ) {
                int i = 1;
                int length = b.length();

                for(int j =0 ; j<length;j++) {
                    if(i%tick!=0) {
                        b.setCharAt(j,' ');
                    }
                    i++;
                }

            }
        }

        List<String> generatedRows = new ArrayList<String>(rows.size());
        for(StringBuilder s : rows) {
            generatedRows.add(s.toString());
        }

        return generatedRows;
    }

    private void writeVerticalTR(List<StringBuilder> rowsOfStrings,
                               int column,
                               int number, int stringLength) {
        char[] numberAsChars = String.valueOf(number).toCharArray();

        int rowNumber = 1;
        for(char c : numberAsChars) {
            StringBuilder stringRow;
            if(rowsOfStrings.size()<rowNumber) {
                stringRow = new StringBuilder(String.format("%1$" + stringLength + "s", ""));
//                if(rowNumber!=1) {
//                    for(int i=column;i>=0;i--) { stringRow.append(' ');}
//                }
                rowsOfStrings.add(stringRow);
            } else {
                stringRow = rowsOfStrings.get(rowNumber - 1);

            }

            stringRow.setCharAt(column, c);
            rowNumber++;
        }
    }
}
