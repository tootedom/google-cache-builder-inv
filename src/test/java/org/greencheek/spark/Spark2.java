package org.greencheek.spark;

/**
 * Adapted from https://github.com/holman/spark
 *
 * whose license is:
 *
 *

 The MIT License

 Copyright (c) Zach Holman, http://zachholman.com

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 associated documentation files (the "Software"), to deal in the Software without restriction,
 including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial
 portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 */

import java.util.ArrayList;
import java.util.List;

/**
 * User: dominictootell
 * Date: 02/03/2013
 * Time: 17:25
 */
public class Spark2 {

    public static char MARKER = '|';
    public static short[] TICKS = new short[]{1, 2, 3, 4};
    public static int NUMBER_OF_TICKS = TICKS.length-1;

    public static void main(String[] args) throws Exception {

        System.out.println(graph(new int[]{101,250,47,150,6,90,389,28,300}).graphToString());

        System.out.println(graph(new int[]{1,1,3,2,3,4,2,1,4}).graphToString());
    }



    public static Spark2Result graph(int[] numbers) {
        return graph(numbers,MARKER);
    }

    public static Spark2Result graph(int[] numbers, char histogramCharacter) {

        short[] graph = new short[numbers.length];

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for(int num : numbers) {
            if(num<min) min = num;
            if(num>max) max = num;
        }

        int f=(( ((max-min)<<4)/(NUMBER_OF_TICKS) ));
        if(f<1) f = 1;

        int numberOfNumbers = numbers.length;
        for(int i =0;i< numberOfNumbers;i++) {
             graph[i] = TICKS[ (((numbers[i]-min)<<4)/f) ];
        }

        return new Spark2Result(graph,min,max,histogramCharacter);
    }


    public static class Spark2Result {
        private final int min;
        private final int max;
        private final short[] graph;
        private final char histoChar;

        public Spark2Result(short[] results,int min, int max, char histogramCharacter) {
            this.min = min;
            this.max = max;
            this.graph = results;
            this.histoChar = histogramCharacter;
        }

        public short[] getGraph() {
            return graph;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public String graphToString() {
            return graphToString(histoChar);
        }

        public String graphToString(char histoChar) {
            String[] plottedGraph = graphToStringArray();
            StringBuilder graph = new StringBuilder();
            String lineSep = System.getProperty("line.separator");

            for(String s : plottedGraph) {
               graph.append(s);
               graph.append(lineSep);
            }

            if(graph.length()>lineSep.length());
            return graph.substring(0,graph.length()-lineSep.length());
        }

        public String[] graphToStringArray() {
            return graphToStringArray(histoChar);
        }

        public String[] graphToStringArray(char histoChar) {
            List<char[]> graphAsChars = new ArrayList<char[]>(graph.length);
            int maxHeight = 0;
            for(short height : graph) {
                char[] bar = new char[height];
                for(int i=0;i<height;i++) {
                    bar[i] = histoChar;
                }
                graphAsChars.add(bar);
                if(height>maxHeight) maxHeight = height;
            }

            return graphToStringArray(graphAsChars,maxHeight);
        }


        private String[] graphToStringArray(List<char[]> graphAsCharArray, int maxHieght) {
            int lengthOfGraph = graphAsCharArray.size();
            List<StringBuilder> strings = new ArrayList<StringBuilder>(maxHieght);
            for(int i=0;i<maxHieght;i++) {
                strings.add(new StringBuilder(String.format("%1$" + lengthOfGraph + "s", "")));
            }

            int col = 0;
            for(char[] column: graphAsCharArray) {
                plotColumn(col,column,strings);
                col++;
            }

            String[] items = new String[maxHieght];
            int i=0;
            for(int reverse=maxHieght-1;reverse>=0;reverse--) {
                items[reverse] = strings.get(i).toString();
                i++;
            }

            return items;
        }

        private void plotColumn(int column,char[] columnChars,List<StringBuilder> strings) {
            int i =0;
            for(char c : columnChars) {
                StringBuilder row = strings.get(i);
                row.setCharAt(column,c);
                i++;
            }
        }


        public String toString() {
            return graphToString();
//            StringBuilder str = new StringBuilder(new String(graph));
//            str.append(':').append("min/max:").append(min).append('/').append(max);
//            return str.toString();
        }

    }


}
